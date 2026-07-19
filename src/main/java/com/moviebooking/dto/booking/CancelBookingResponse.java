package com.moviebooking.dto.booking;

import com.moviebooking.domain.booking.BookingStatus;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingResponse {
    private Long bookingId;
    private BookingStatus status;
    private BigDecimal totalPaid;
    private BigDecimal refundAmount;
    private BigDecimal refundPercentage;
}
