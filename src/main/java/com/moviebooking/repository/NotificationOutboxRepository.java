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

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(n) > 0 FROM NotificationOutbox n WHERE n.type = :type AND n.recipientUser.id = :userId AND n.payloadJson LIKE %:pattern%")
    boolean existsByNotification(
            @org.springframework.data.repository.query.Param("type") String type,
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("pattern") String pattern
    );
}
