# GitHub Actions — Secrets à configurer
# ============================================================
# Aller dans : GitHub → Repo → Settings → Secrets and variables → Actions
# Cliquer "New repository secret" pour chaque ligne ci-dessous.
# ============================================================
# Ce fichier est un guide de référence — NE PAS y mettre de vraies valeurs.
# ============================================================

# ── Render (Backend) ─────────────────────────────────────────
# Comment trouver : Render → Service → Settings → Deploy Hooks → Create Hook
RENDER_DEPLOY_HOOK_URL=https://api.render.com/deploy/srv-XXXX?key=YYYY

# URL publique du backend Render (après premier déploiement)
RENDER_BACKEND_URL=https://smartbackup-backend-XXXX.onrender.com

# ── Vercel (Frontend) ────────────────────────────────────────
# Comment trouver le token : https://vercel.com/account/tokens → Create
VERCEL_TOKEN=your_vercel_personal_access_token

# Comment trouver : Vercel → Settings → General → Team ID
# (ou votre User ID si compte personnel)
VERCEL_ORG_ID=team_XXXX

# Comment trouver : Vercel → Project → Settings → General → Project ID
VERCEL_PROJECT_ID=prj_XXXX

# ── Optionnel : pour les tests CI ────────────────────────────
# Si vous voulez que le CI vérifie le déploiement automatiquement
SCHEDULER_SECRET=same_value_as_on_render
