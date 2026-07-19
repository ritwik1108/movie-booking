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
public class BookingDto {
    private Long id;
    private String movieTitle;
    private String theaterName;
    private String screenName;
    private List<String> seatNames;
    private OffsetDateTime showStartTime;
    private BigDecimal totalPaid;
    private BookingStatus status;
    private OffsetDateTime createdAt;
}
