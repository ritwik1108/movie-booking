package com.moviebooking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.domain.notification.NotificationOutbox;
import com.moviebooking.domain.notification.NotificationOutboxStatus;
import com.moviebooking.domain.user.User;
import com.moviebooking.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationOutboxService {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueNotification(String type, User recipient, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            NotificationOutbox outbox = NotificationOutbox.builder()
                    .type(type)
                    .recipientUser(recipient)
                    .payloadJson(payloadJson)
                    .status(NotificationOutboxStatus.PENDING)
                    .attemptCount(0)
                    .build();
            notificationOutboxRepository.save(outbox);
            log.info("Enqueued notification of type {} for user {}", type, recipient.getEmail());
        } catch (Exception e) {
            log.error("Failed to enqueue notification of type {}", type, e);
            throw new RuntimeException("Notification enqueue failed", e);
        }
    }
}
