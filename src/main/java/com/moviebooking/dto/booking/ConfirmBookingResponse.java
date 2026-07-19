package com.moviebooking.dto.booking;

import com.moviebooking.domain.booking.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmBookingResponse {
    private Long bookingId;
    private String movieTitle;
    private String theaterName;
    private String screenName;
    private List<String> seatNames;
    private OffsetDateTime showStartTime;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalPaid;
    private BookingStatus status;
}
