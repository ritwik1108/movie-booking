package com.moviebooking.scheduler;

import com.moviebooking.domain.notification.NotificationOutbox;
import com.moviebooking.domain.notification.NotificationOutboxStatus;
import com.moviebooking.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxPoller {

    private final NotificationOutboxRepository notificationOutboxRepository;

    @Scheduled(fixedRateString = "${app.outbox.poller-interval-ms:5000}")
    public void pollAndDispatch() {
        List<NotificationOutbox> pending = notificationOutboxRepository
                .findByStatusAndAttemptCountLessThanOrderByCreatedAtAsc(NotificationOutboxStatus.PENDING, 3);

        if (pending.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox notification events to process", pending.size());

        for (NotificationOutbox record : pending) {
            try {
                dispatch(record);
            } catch (Exception e) {
                log.error("Error processing outbox record ID {}", record.getId(), e);
            }
        }
    }

    @Transactional
    public void dispatch(NotificationOutbox record) {
        // Increment attempts
        record.setAttemptCount(record.getAttemptCount() + 1);

        try {
            // Mock notification sending
            log.info("[NOTIFICATION DISPATCH] Sending email of type '{}' to recipient '{}' with payload: {}",
                    record.getType(), record.getRecipientUser().getEmail(), record.getPayloadJson());

            // If the payload contains "fail_notification", simulate a dispatch failure for retry testing
            if (record.getPayloadJson().contains("fail_notification")) {
                throw new RuntimeException("Simulated notification gateway error");
            }

            record.setStatus(NotificationOutboxStatus.SENT);
            record.setSentAt(OffsetDateTime.now());
        } catch (Exception e) {
            log.warn("Failed to dispatch notification ID {} (attempt {}): {}", 
                    record.getId(), record.getAttemptCount(), e.getMessage());
            
            if (record.getAttemptCount() >= 3) {
                record.setStatus(NotificationOutboxStatus.FAILED);
            } else {
                record.setStatus(NotificationOutboxStatus.PENDING);
            }
        }

        notificationOutboxRepository.save(record);
    }
}
