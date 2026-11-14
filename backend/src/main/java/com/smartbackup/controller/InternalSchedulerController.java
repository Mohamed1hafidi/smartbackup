package com.smartbackup.controller;

import com.smartbackup.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint interne appelé par le Cloudflare Worker Cron toutes les 10 min.
 *
 * <p>Permet de :
 * <ul>
 *   <li>Réveiller le backend Render (keep-alive)</li>
 *   <li>Déclencher l'exécution des sauvegardes planifiées dues</li>
 * </ul>
 *
 * <p>Protégé par un secret partagé {@code X-Scheduler-Secret} défini
 * dans la variable d'environnement {@code SCHEDULER_SECRET}.
 * Si la variable est vide, l'endpoint est désactivé en production.
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class InternalSchedulerController {

    private final ScheduleService scheduleService;

    @Value("${app.scheduler.secret:}")
    private String schedulerSecret;

    /**
     * POST /api/internal/scheduler/trigger
     *
     * <p>Appelé par le Cloudflare Worker.
     * Vérifie le secret, puis exécute les sauvegardes dues.
     */
    @PostMapping("/scheduler/trigger")
    public ResponseEntity<Map<String, Object>> triggerScheduler(
            @RequestHeader(value = "X-Scheduler-Secret", required = false) String incomingSecret
    ) {
        // ── Vérification du secret ──────────────────────────
        if (schedulerSecret != null && !schedulerSecret.isBlank()) {
            if (!schedulerSecret.equals(incomingSecret)) {
                log.warn("[InternalScheduler] Secret invalide — tentative rejetée.");
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Unauthorized"));
            }
        }

        // ── Exécution des schedules dus ──────────────────────
        log.info("[InternalScheduler] Déclenchement externe reçu (Cloudflare Worker).");
        int executed = scheduleService.executeDueSchedules();
        log.info("[InternalScheduler] {} sauvegarde(s) planifiée(s) exécutée(s).", executed);

        return ResponseEntity.ok(Map.of(
                "triggered", true,
                "schedulesExecuted", executed,
                "timestamp", java.time.Instant.now().toString()
        ));
    }
}
