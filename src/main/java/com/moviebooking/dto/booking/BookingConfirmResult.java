package com.moviebooking.dto.booking;

import com.moviebooking.domain.booking.Booking;

public record BookingConfirmResult(boolean success, Booking booking, String errorMessage) {}
