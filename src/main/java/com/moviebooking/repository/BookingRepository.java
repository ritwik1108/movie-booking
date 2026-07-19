package com.moviebooking.repository;

import com.moviebooking.domain.booking.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Booking b WHERE b.status = :status AND b.show.startTime >= :now AND b.show.startTime <= :threshold")
    List<Booking> findUpcomingBookings(
            @org.springframework.data.repository.query.Param("status") com.moviebooking.domain.booking.BookingStatus status,
            @org.springframework.data.repository.query.Param("now") java.time.OffsetDateTime now,
            @org.springframework.data.repository.query.Param("threshold") java.time.OffsetDateTime threshold
    );
}
