package com.smartbackup.service;

import com.smartbackup.entity.NotificationType;
import com.smartbackup.entity.User;
import com.smartbackup.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final UserRepository userRepository;
    private final TaskExecutor notificationTaskExecutor;
    private final boolean enabled;
    private final String from;
    private final String frontendUrl;
    private final int maxEmailsPerUserPerDay;
    private final Map<UUID, DailyCounter> counters = new ConcurrentHashMap<>();

    public EmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            UserRepository userRepository,
            @Qualifier("notificationTaskExecutor") TaskExecutor notificationTaskExecutor,
            @Value("${app.notifications.email-enabled:false}") boolean enabled,
            @Value("${app.notifications.from:no-reply@smartbackup.local}") String from,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
            @Value("${app.notifications.max-emails-per-user-per-day:5}") int maxEmailsPerUserPerDay
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.userRepository = userRepository;
        this.notificationTaskExecutor = notificationTaskExecutor;
        this.enabled = enabled;
        this.from = from;
        this.frontendUrl = frontendUrl;
        this.maxEmailsPerUserPerDay = maxEmailsPerUserPerDay;
    }

    public void send(UUID userId, NotificationType type, String message) {
        if (!enabled) {
            return;
        }

        try {
            notificationTaskExecutor.execute(() -> sendNow(userId, type, message));
        } catch (RuntimeException exception) {
            log.warn("Impossible de mettre en file l'email de notification pour {}", userId, exception);
        }
    }

    private void sendNow(UUID userId, NotificationType type, String message) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Email de notification ignoré : JavaMailSender non configuré");
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isActive()) {
            return;
        }

        if (!allowEmail(userId)) {
            log.info("Limite quotidienne d'emails atteinte pour l'utilisateur {}", userId);
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(user.getEmail());
        mail.setSubject(subjectFor(type));
        mail.setText(message + System.lineSeparator() + System.lineSeparator()
                + "Consultez vos notifications : " + frontendUrl);

        try {
            mailSender.send(mail);
            log.info("Email de notification {} envoyé à l'utilisateur {}", type, userId);
        } catch (MailException exception) {
            log.error("Échec de l'envoi de l'email de notification à l'utilisateur {}",
                    userId, exception);
        }
    }

    private boolean allowEmail(UUID userId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        DailyCounter counter = counters.compute(userId, (key, existing) -> {
            if (existing == null || !existing.date().equals(today)) {
                return new DailyCounter(today, new AtomicInteger());
            }
            return existing;
        });
        return counter.count().incrementAndGet() <= maxEmailsPerUserPerDay;
    }

    private String subjectFor(NotificationType type) {
        return switch (type) {
            case BACKUP_SUCCESS -> "SmartBackup - Sauvegarde réussie";
            case BACKUP_FAILED -> "SmartBackup - Sauvegarde échouée";
            case RESTORE_SUCCESS -> "SmartBackup - Restauration réussie";
            case RESTORE_FAILED -> "SmartBackup - Restauration échouée";
            case SCHEDULE_ACTIVATED -> "SmartBackup - Programmation activée";
            case SCHEDULE_DEACTIVATED -> "SmartBackup - Programmation désactivée";
        };
    }

    private record DailyCounter(LocalDate date, AtomicInteger count) {
    }
}
