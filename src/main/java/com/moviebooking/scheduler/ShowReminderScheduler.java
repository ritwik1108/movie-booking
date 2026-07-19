package com.moviebooking.scheduler;

import com.moviebooking.domain.booking.Booking;
import com.moviebooking.domain.booking.BookingStatus;
import com.moviebooking.repository.BookingRepository;
import com.moviebooking.repository.NotificationOutboxRepository;
import com.moviebooking.service.NotificationOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShowReminderScheduler {

    private final BookingRepository bookingRepository;
    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationOutboxService notificationOutboxService;

    @Scheduled(fixedRateString = "${app.reminder.poller-interval-ms:60000}")
    @Transactional
    public void sendUpcomingShowReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime threshold = now.plusHours(2);

        List<Booking> bookings = bookingRepository.findUpcomingBookings(BookingStatus.CONFIRMED, now, threshold);

        if (bookings.isEmpty()) {
            return;
        }

        log.info("Found {} upcoming bookings starting within 2 hours. Checking if reminders are needed.", bookings.size());

        for (Booking booking : bookings) {
            String pattern = "\"bookingId\":" + booking.getId();
            boolean alreadySent = notificationOutboxRepository.existsByNotification(
                    "SHOW_REMINDER", 
                    booking.getUser().getId(), 
                    pattern
            );

            if (!alreadySent) {
                Map<String, Object> payload = Map.of(
                        "bookingId", booking.getId(),
                        "movieTitle", booking.getShow().getMovie().getTitle(),
                        "screenName", booking.getShow().getScreen().getName(),
                        "showStartTime", booking.getShow().getStartTime(),
                        "message", "This is a reminder that your show is starting soon!"
                );

                notificationOutboxService.enqueueNotification(
                        "SHOW_REMINDER", 
                        booking.getUser(), 
                        payload
                );
            }
        }
    }
}
