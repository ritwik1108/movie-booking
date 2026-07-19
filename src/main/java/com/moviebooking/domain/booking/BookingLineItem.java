package com.moviebooking.domain.booking;

import com.moviebooking.domain.seat.SeatTier;
import com.moviebooking.domain.showseat.ShowSeat;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "booking_line_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_seat_id", nullable = false)
    private ShowSeat showSeat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatTier tier;

    @Column(name = "price_charged", nullable = false)
    private BigDecimal priceCharged;
}
