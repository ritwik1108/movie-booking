package com.moviebooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.domain.booking.Booking;
import com.moviebooking.domain.booking.BookingStatus;
import com.moviebooking.domain.discount.DiscountCode;
import com.moviebooking.domain.discount.DiscountCodeType;
import com.moviebooking.domain.payment.Payment;
import com.moviebooking.domain.payment.PaymentStatus;
import com.moviebooking.domain.pricing.PricingRuleScope;
import com.moviebooking.domain.seat.SeatTier;
import com.moviebooking.domain.seathold.SeatHold;
import com.moviebooking.domain.seathold.SeatHoldStatus;
import com.moviebooking.domain.showseat.ShowSeat;
import com.moviebooking.domain.showseat.ShowSeatStatus;
import com.moviebooking.dto.auth.LoginRequest;
import com.moviebooking.dto.booking.ConfirmBookingRequest;
import com.moviebooking.dto.booking.HoldRequest;
import com.moviebooking.dto.catalog.*;
import com.moviebooking.dto.discount.DiscountCodeDto;
import com.moviebooking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class BookingConfirmationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DiscountCodeRepository discountCodeRepository;

    private String adminToken;
    private String customerToken;

    private Long showId;
    private List<Long> seatIds;

    @BeforeEach
    public void setup() throws Exception {
        // Authenticate admin
        LoginRequest adminLogin = new LoginRequest();
        adminLogin.setEmail("admin@movie.com");
        adminLogin.setPassword("admin123");

        String adminRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(adminRes).get("token").asText();

        // Authenticate customer
        LoginRequest customerLogin = new LoginRequest();
        customerLogin.setEmail("customer@movie.com");
        customerLogin.setPassword("customer123");

        String custRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerLogin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        customerToken = objectMapper.readTree(custRes).get("token").asText();

        setupShowAndSeats();
    }

    private void setupShowAndSeats() throws Exception {
        // City
        CityDto cityReq = CityDto.builder().name("Mumbai-South").build();
        String cityRes = mockMvc.perform(post("/api/v1/admin/cities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cityReq)))
                .andReturn().getResponse().getContentAsString();
        Long cityId = objectMapper.readTree(cityRes).get("id").asLong();

        // Theater
        TheaterDto theaterReq = TheaterDto.builder()
                .cityId(cityId)
                .name("PVR Sterling")
                .address("Fort, Mumbai")
                .build();
        String theaterRes = mockMvc.perform(post("/api/v1/admin/theaters")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(theaterReq)))
                .andReturn().getResponse().getContentAsString();
        Long theaterId = objectMapper.readTree(theaterRes).get("id").asLong();

        // Screen
        ScreenDto screenReq = ScreenDto.builder()
                .theaterId(theaterId)
                .name("Audi 1")
                .build();
        String screenRes = mockMvc.perform(post("/api/v1/admin/theaters/" + theaterId + "/screens")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(screenReq)))
                .andReturn().getResponse().getContentAsString();
        Long screenId = objectMapper.readTree(screenRes).get("id").asLong();

        // Seats (2 regular seats)
        BulkSeatCreationRequest bulkSeatsReq = new BulkSeatCreationRequest();
        BulkSeatCreationRequest.RowLayout rowA = new BulkSeatCreationRequest.RowLayout();
        rowA.setRowLabel("A");
        rowA.setNumSeats(2);
        rowA.setTier(SeatTier.REGULAR);
        bulkSeatsReq.setLayouts(Collections.singletonList(rowA));

        mockMvc.perform(post("/api/v1/admin/screens/" + screenId + "/seats")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkSeatsReq)));

        // Movie
        MovieDto movieReq = MovieDto.builder()
                .title("Interstellar")
                .durationMinutes(169)
                .language("English")
                .genre("Sci-Fi")
                .certification("UA")
                .build();
        String movieRes = mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieReq)))
                .andReturn().getResponse().getContentAsString();
        Long movieId = objectMapper.readTree(movieRes).get("id").asLong();

        // Global pricing
        PricingRuleDto pricingReq = PricingRuleDto.builder()
                .scope(PricingRuleScope.GLOBAL)
                .seatTier(SeatTier.REGULAR)
                .basePrice(new BigDecimal("200.00"))
                .weekendMultiplier(new BigDecimal("1.0"))
                .build();
        mockMvc.perform(post("/api/v1/admin/pricing-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pricingReq)));

        // Show
        ShowDto showReq = ShowDto.builder()
                .movieId(movieId)
                .screenId(screenId)
                .startTime(OffsetDateTime.now().plusDays(2))
                .endTime(OffsetDateTime.now().plusDays(2).plusHours(3))
                .build();
        String showRes = mockMvc.perform(post("/api/v1/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showReq)))
                .andReturn().getResponse().getContentAsString();
        showId = objectMapper.readTree(showRes).get("id").asLong();

        // Get seat IDs
        String seatsRes = mockMvc.perform(get("/api/v1/shows/" + showId + "/seats"))
                .andReturn().getResponse().getContentAsString();
        seatIds = new ArrayList<>();
        seatIds.add(objectMapper.readTree(seatsRes).get(0).get("seatId").asLong());
        seatIds.add(objectMapper.readTree(seatsRes).get(1).get("seatId").asLong());
    }

    @Test
    public void testConfirmBooking_HappyPath_NoCoupon() throws Exception {
        // 1. Hold Seats
        HoldRequest holdReq = new HoldRequest();
        holdReq.setSeatIds(seatIds);

        String holdRes = mockMvc.perform(post("/api/v1/shows/" + showId + "/holds")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long holdId = objectMapper.readTree(holdRes).get("holdId").asLong();

        // 2. Confirm Booking
        ConfirmBookingRequest confirmReq = ConfirmBookingRequest.builder()
                .holdId(holdId)
                .paymentToken("card_tok_123")
                .build();

        mockMvc.perform(post("/api/v1/bookings/confirm")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.subtotal").value(400.00))
                .andExpect(jsonPath("$.discountAmount").value(0.00))
                .andExpect(jsonPath("$.totalPaid").value(400.00))
                .andExpect(jsonPath("$.seatNames[0]").value("A1"))
                .andExpect(jsonPath("$.seatNames[1]").value("A2"));

        // 3. Verify DB states
        SeatHold hold = seatHoldRepository.findById(holdId).orElseThrow();
        assertEquals(SeatHoldStatus.CONSUMED, hold.getStatus());

        List<ShowSeat> showSeats = showSeatRepository.findByShowId(showId);
        for (ShowSeat ss : showSeats) {
            assertEquals(ShowSeatStatus.BOOKED, ss.getStatus());
        }
    }

    @Test
    public void testConfirmBooking_HappyPath_WithPercentageCoupon() throws Exception {
        // 1. Create Coupon (20% off)
        DiscountCodeDto couponDto = DiscountCodeDto.builder()
                .code("SAVE20")
                .type(DiscountCodeType.PERCENTAGE)
                .value(new BigDecimal("20.00"))
                .validFrom(OffsetDateTime.now().minusDays(1))
                .validTo(OffsetDateTime.now().plusDays(5))
                .maxUses(10)
                .minBookingAmount(new BigDecimal("300.00"))
                .build();

        mockMvc.perform(post("/api/v1/admin/discount-codes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(couponDto)))
                .andExpect(status().isCreated());

        // 2. Hold Seats
        HoldRequest holdReq = new HoldRequest();
        holdReq.setSeatIds(seatIds);
        String holdRes = mockMvc.perform(post("/api/v1/shows/" + showId + "/holds")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andReturn().getResponse().getContentAsString();
        Long holdId = objectMapper.readTree(holdRes).get("holdId").asLong();

        // 3. Confirm Booking with SAVE20
        ConfirmBookingRequest confirmReq = ConfirmBookingRequest.builder()
                .holdId(holdId)
                .paymentToken("card_tok_123")
                .discountCode("SAVE20")
                .build();

        mockMvc.perform(post("/api/v1/bookings/confirm")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.subtotal").value(400.00))
                .andExpect(jsonPath("$.discountAmount").value(80.00))
                .andExpect(jsonPath("$.totalPaid").value(320.00));

        // Verify coupon consumption count
        DiscountCode code = discountCodeRepository.findByCode("SAVE20").orElseThrow();
        assertEquals(1, code.getUsesConsumed());
    }

    @Test
    public void testConfirmBooking_PaymentDecline_ReleasesSeatsImmediately() throws Exception {
        // 1. Hold Seats
        HoldRequest holdReq = new HoldRequest();
        holdReq.setSeatIds(seatIds);
        String holdRes = mockMvc.perform(post("/api/v1/shows/" + showId + "/holds")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andReturn().getResponse().getContentAsString();
        Long holdId = objectMapper.readTree(holdRes).get("holdId").asLong();

        // 2. Confirm Booking with "fail_declined" payment token
        ConfirmBookingRequest confirmReq = ConfirmBookingRequest.builder()
                .holdId(holdId)
                .paymentToken("fail_declined")
                .build();

        mockMvc.perform(post("/api/v1/bookings/confirm")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PAYMENT_FAILED"));

        // 3. Verify DB states show that seats are immediately AVAILABLE again!
        SeatHold hold = seatHoldRepository.findById(holdId).orElseThrow();
        assertEquals(SeatHoldStatus.FAILED, hold.getStatus());

        List<ShowSeat> showSeats = showSeatRepository.findByShowId(showId);
        for (ShowSeat ss : showSeats) {
            assertEquals(ShowSeatStatus.AVAILABLE, ss.getStatus());
            assertNull(ss.getHeldByHold());
        }
    }

    @Test
    public void testConfirmBooking_CouponMinBookingAmountNotMet() throws Exception {
        // Create a Coupon requiring min 500.00 booking amount
        DiscountCodeDto couponDto = DiscountCodeDto.builder()
                .code("BIGSPENDER")
                .type(DiscountCodeType.FLAT)
                .value(new BigDecimal("100.00"))
                .validFrom(OffsetDateTime.now().minusDays(1))
                .validTo(OffsetDateTime.now().plusDays(5))
                .maxUses(10)
                .minBookingAmount(new BigDecimal("500.00"))
                .build();

        mockMvc.perform(post("/api/v1/admin/discount-codes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(couponDto)))
                .andExpect(status().isCreated());

        // Hold Seats (total cost = 400.00, below 500.00)
        HoldRequest holdReq = new HoldRequest();
        holdReq.setSeatIds(seatIds);
        String holdRes = mockMvc.perform(post("/api/v1/shows/" + showId + "/holds")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andReturn().getResponse().getContentAsString();
        Long holdId = objectMapper.readTree(holdRes).get("holdId").asLong();

        ConfirmBookingRequest confirmReq = ConfirmBookingRequest.builder()
                .holdId(holdId)
                .paymentToken("card_tok_123")
                .discountCode("BIGSPENDER")
                .build();

        // Should return HTTP 400 with MIN_BOOKING_AMOUNT_NOT_MET
        mockMvc.perform(post("/api/v1/bookings/confirm")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MIN_BOOKING_AMOUNT_NOT_MET"));
    }

    @Test
    public void testBookingHistoryAndOwnership() throws Exception {
        // 1. Hold and confirm a booking
        HoldRequest holdReq = new HoldRequest();
        holdReq.setSeatIds(seatIds);
        String holdRes = mockMvc.perform(post("/api/v1/shows/" + showId + "/holds")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andReturn().getResponse().getContentAsString();
        Long holdId = objectMapper.readTree(holdRes).get("holdId").asLong();

        ConfirmBookingRequest confirmReq = ConfirmBookingRequest.builder()
                .holdId(holdId)
                .paymentToken("card_tok_123")
                .build();
        String confirmRes = mockMvc.perform(post("/api/v1/bookings/confirm")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long bookingId = objectMapper.readTree(confirmRes).get("bookingId").asLong();

        // 2. Fetch history - should return the booking in a list
        mockMvc.perform(get("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookingId))
                .andExpect(jsonPath("$[0].movieTitle").value("Interstellar"))
                .andExpect(jsonPath("$[0].totalPaid").value(400.00));

        // 3. Fetch details - should return details
        mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.movieTitle").value("Interstellar"));

        // 4. Fetch details as a DIFFERENT user (admin@movie.com) - should return HTTP 403 Forbidden because they lack CUSTOMER role
        mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        // 5. Register and login a DIFFERENT customer
        com.moviebooking.dto.auth.RegisterRequest otherReq = new com.moviebooking.dto.auth.RegisterRequest();
        otherReq.setEmail("other@movie.com");
        otherReq.setPassword("other123");
        otherReq.setRole(com.moviebooking.domain.user.Role.CUSTOMER);

        String otherRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String otherToken = objectMapper.readTree(otherRes).get("token").asText();

        // 6. Fetch details as a DIFFERENT customer - should return HTTP 400 Bad Request (ownership block)
        mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED_BOOKING_ACCESS"));
    }
}
