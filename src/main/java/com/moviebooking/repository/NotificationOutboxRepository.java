package com.moviebooking.repository;

import com.moviebooking.domain.notification.NotificationOutbox;
import com.moviebooking.domain.notification.NotificationOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    List<NotificationOutbox> findByStatusAndAttemptCountLessThanOrderByCreatedAtAsc(
            NotificationOutboxStatus status,
            int maxAttempts
    );
}
