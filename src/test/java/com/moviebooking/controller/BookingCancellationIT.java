package com.moviebooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.domain.booking.Booking;
import com.moviebooking.domain.booking.BookingStatus;
import com.moviebooking.domain.pricing.PricingRuleScope;
import com.moviebooking.domain.refund.Refund;
import com.moviebooking.domain.refund.RefundPolicy;
import com.moviebooking.domain.refund.RefundRule;
import com.moviebooking.domain.seat.SeatTier;
import com.moviebooking.domain.show.Show;
import com.moviebooking.domain.showseat.ShowSeat;
import com.moviebooking.domain.showseat.ShowSeatStatus;
import com.moviebooking.dto.auth.LoginRequest;
import com.moviebooking.dto.booking.ConfirmBookingRequest;
import com.moviebooking.dto.booking.HoldRequest;
import com.moviebooking.dto.catalog.*;
import com.moviebooking.dto.refund.RefundPolicyDto;
import com.moviebooking.dto.refund.RefundRuleDto;
import com.moviebooking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class BookingCancellationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private RefundPolicyRepository refundPolicyRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private ShowRepository showRepository;

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
        setupRefundPolicy();
    }

    private void setupShowAndSeats() throws Exception {
        CityDto cityReq = CityDto.builder().name("Bangalore").build();
        String cityRes = mockMvc.perform(post("/api/v1/admin/cities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cityReq)))
                .andReturn().getResponse().getContentAsString();
        Long cityId = objectMapper.readTree(cityRes).get("id").asLong();

        TheaterDto theaterReq = TheaterDto.builder()
                .cityId(cityId)
                .name("PVR Forum")
                .address("Koramangala, Bangalore")
                .build();
        String theaterRes = mockMvc.perform(post("/api/v1/admin/theaters")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(theaterReq)))
                .andReturn().getResponse().getContentAsString();
        Long theaterId = objectMapper.readTree(theaterRes).get("id").asLong();

        ScreenDto screenReq = ScreenDto.builder()
                .theaterId(theaterId)
                .name("Audi 4")
                .build();
        String screenRes = mockMvc.perform(post("/api/v1/admin/theaters/" + theaterId + "/screens")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(screenReq)))
                .andReturn().getResponse().getContentAsString();
        Long screenId = objectMapper.readTree(screenRes).get("id").asLong();

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

        MovieDto movieReq = MovieDto.builder()
                .title("Oppenheimer")
                .durationMinutes(180)
                .language("English")
                .genre("Drama")
                .certification("UA")
                .build();
        String movieRes = mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieReq)))
                .andReturn().getResponse().getContentAsString();
        Long movieId = objectMapper.readTree(movieRes).get("id").asLong();

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

        String seatsRes = mockMvc.perform(get("/api/v1/shows/" + showId + "/seats"))
                .andReturn().getResponse().getContentAsString();
        seatIds = new ArrayList<>();
        seatIds.add(objectMapper.readTree(seatsRes).get(0).get("seatId").asLong());
        seatIds.add(objectMapper.readTree(seatsRes).get(1).get("seatId").asLong());
    }

    private void setupRefundPolicy() throws Exception {
        // Create refund policy
        RefundPolicyDto policyDto = RefundPolicyDto.builder()
                .name("Standard Cancellation Policy")
                .build();
        String policyRes = mockMvc.perform(post("/api/v1/admin/refund-policies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(policyDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long policyId = objectMapper.readTree(policyRes).get("id").asLong();

        // Rule 1: >= 24 hours before show time -> 100% refund
        RefundRuleDto rule1 = RefundRuleDto.builder()
                .hoursBeforeMin(24)
                .hoursBeforeMax(null)
                .refundPercentage(new BigDecimal("100.00"))
                .build();
        mockMvc.perform(post("/api/v1/admin/refund-policies/" + policyId + "/rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rule1)))
                .andExpect(status().isCreated());

        // Rule 2: 2 to 24 hours before show time -> 50% refund
        RefundRuleDto rule2 = RefundRuleDto.builder()
                .hoursBeforeMin(2)
                .hoursBeforeMax(24)
                .refundPercentage(new BigDecimal("50.00"))
                .build();
        mockMvc.perform(post("/api/v1/admin/refund-policies/" + policyId + "/rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rule2)))
                .andExpect(status().isCreated());

        // Rule 3: 0 to 2 hours before show time -> 0% refund
        RefundRuleDto rule3 = RefundRuleDto.builder()
                .hoursBeforeMin(0)
                .hoursBeforeMax(2)
                .refundPercentage(new BigDecimal("0.00"))
                .build();
        mockMvc.perform(post("/api/v1/admin/refund-policies/" + policyId + "/rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rule3)))
                .andExpect(status().isCreated());
    }

    private Long createAndConfirmBooking() throws Exception {
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
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(confirmRes).get("bookingId").asLong();
    }

    @Test
    public void testCancelBooking_100PercentRefund() throws Exception {
        Long bookingId = createAndConfirmBooking();

        // 100% refund should apply because show is in 2 days (48 hours >= 24)
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundPercentage").value(100.00))
                .andExpect(jsonPath("$.refundAmount").value(400.00));

        // Verify DB states: Seats released, booking cancelled
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());

        List<ShowSeat> seats = showSeatRepository.findByShowId(showId);
        for (ShowSeat ss : seats) {
            assertEquals(ShowSeatStatus.AVAILABLE, ss.getStatus());
            assertNull(ss.getHeldByHold());
        }

        // Verify Refund record
        Refund refund = refundRepository.findAll().stream()
                .filter(r -> r.getBooking().getId().equals(bookingId))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("400.00"), refund.getAmount().setScale(2));
    }

    @Test
    public void testCancelBooking_50PercentRefund() throws Exception {
        Long bookingId = createAndConfirmBooking();

        // Backdate the show's start time to be exactly 5 hours from now (simulating 5 hours before show start)
        Show show = showRepository.findById(showId).orElseThrow();
        show.setStartTime(OffsetDateTime.now().plusHours(5));
        showRepository.save(show);

        // 50% refund should apply (5 hours is between 2 and 24 hours)
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundPercentage").value(50.00))
                .andExpect(jsonPath("$.refundAmount").value(200.00));
    }

    @Test
    public void testCancelBooking_0PercentRefund() throws Exception {
        Long bookingId = createAndConfirmBooking();

        // Backdate show to be exactly 1 hour from now
        Show show = showRepository.findById(showId).orElseThrow();
        show.setStartTime(OffsetDateTime.now().plusHours(1));
        showRepository.save(show);

        // 0% refund should apply (1 hour is between 0 and 2 hours)
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundPercentage").value(0.00))
                .andExpect(jsonPath("$.refundAmount").value(0.00));
    }

    @Test
    public void testCancelBooking_ShowAlreadyStarted_Throws400() throws Exception {
        Long bookingId = createAndConfirmBooking();

        // Backdate show to be in the past (already started)
        Show show = showRepository.findById(showId).orElseThrow();
        show.setStartTime(OffsetDateTime.now().minusHours(1));
        showRepository.save(show);

        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SHOW_ALREADY_STARTED"));
    }

    @Test
    public void testCancelBooking_OtherCustomer_Throws400() throws Exception {
        Long bookingId = createAndConfirmBooking();

        // Register and login another customer
        com.moviebooking.dto.auth.RegisterRequest otherReq = new com.moviebooking.dto.auth.RegisterRequest();
        otherReq.setEmail("othercustomer@movie.com");
        otherReq.setPassword("other123");
        otherReq.setRole(com.moviebooking.domain.user.Role.CUSTOMER);

        String otherRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String otherToken = objectMapper.readTree(otherRes).get("token").asText();

        // Cancel request from different customer should be rejected with HTTP 400 Bad Request
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED_BOOKING_ACCESS"));
    }
}
