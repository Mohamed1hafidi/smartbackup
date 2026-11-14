/**
 * SmartBackup — Cloudflare Worker Cron
 * 
 * Objectif : Maintenir le backend Render actif (éviter la mise en veille
 *            après 15 min d'inactivité sur le plan gratuit) ET déclencher
 *            les sauvegardes planifiées que Spring @Scheduled ne peut pas
 *            exécuter si le serveur dort.
 *
 * Déploiement :
 *   1. Créer un Worker dans le dashboard Cloudflare
 *   2. Coller ce code
 *   3. Ajouter un Cron Trigger : "*/10 * * * *" (toutes les 10 min)
 *   4. Ajouter une variable d'environnement BACKEND_URL = https://your-app.onrender.com
 *   5. (Optionnel) Ajouter SCHEDULER_SECRET pour sécuriser l'endpoint
 */

export default {
  /**
   * Cron handler : appelé par le Cron Trigger Cloudflare
   */
  async scheduled(event, env, ctx) {
    const backendUrl = env.BACKEND_URL || 'https://your-smartbackup.onrender.com';
    const secret    = env.SCHEDULER_SECRET || '';

    console.log(`[SmartBackup Worker] Ping ${backendUrl} at ${new Date().toISOString()}`);

    // 1. Keep-alive : ping /actuator/health
    try {
      const healthRes = await fetch(`${backendUrl}/actuator/health`, {
        method: 'GET',
        headers: { 'User-Agent': 'SmartBackup-CF-Worker/1.0' },
        signal: AbortSignal.timeout(10_000),
      });
      console.log(`[Health] status=${healthRes.status}`);
    } catch (err) {
      console.error(`[Health] FAILED: ${err.message}`);
    }

    // 2. Déclencher les sauvegardes planifiées
    //    Le backend vérifie l'heure et exécute les schedules dus.
    try {
      const triggerRes = await fetch(`${backendUrl}/api/internal/scheduler/trigger`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Scheduler-Secret': secret,
          'User-Agent': 'SmartBackup-CF-Worker/1.0',
        },
        signal: AbortSignal.timeout(30_000),
      });
      const body = await triggerRes.text();
      console.log(`[Scheduler] status=${triggerRes.status} body=${body.slice(0, 200)}`);
    } catch (err) {
      console.error(`[Scheduler] FAILED: ${err.message}`);
    }
  },

  /**
   * HTTP handler (optionnel — permet de tester manuellement le Worker)
   */
  async fetch(request, env, ctx) {
    return new Response(
      JSON.stringify({ status: 'SmartBackup Worker active', time: new Date().toISOString() }),
      { headers: { 'Content-Type': 'application/json' } }
    );
  },
};
