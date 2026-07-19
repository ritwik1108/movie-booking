package com.moviebooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.domain.discount.DiscountCode;
import com.moviebooking.domain.discount.DiscountCodeType;
import com.moviebooking.domain.pricing.PricingRuleScope;
import com.moviebooking.domain.seat.SeatTier;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CouponConcurrencyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DiscountCodeRepository discountCodeRepository;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private String adminToken;
    private String customerToken;

    private Long showId;
    private Long seatId1;
    private Long seatId2;

    @BeforeEach
    public void setup() throws Exception {
        // Clear repositories to ensure a clean state
        bookingRepository.deleteAll();
        seatHoldRepository.deleteAll();
        discountCodeRepository.deleteAll();

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
        CityDto cityReq = CityDto.builder().name("Delhi").build();
        String cityRes = mockMvc.perform(post("/api/v1/admin/cities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cityReq)))
                .andReturn().getResponse().getContentAsString();
        Long cityId = objectMapper.readTree(cityRes).get("id").asLong();

        // Theater
        TheaterDto theaterReq = TheaterDto.builder()
                .cityId(cityId)
                .name("PVR Select CityWalk")
                .address("Saket, Delhi")
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
                .name("Audi 2")
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
                .title("Inception")
                .durationMinutes(148)
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
                .basePrice(new BigDecimal("250.00"))
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
                .endTime(OffsetDateTime.now().plusDays(2).plusHours(2).plusMinutes(30))
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
        seatId1 = objectMapper.readTree(seatsRes).get(0).get("seatId").asLong();
        seatId2 = objectMapper.readTree(seatsRes).get(1).get("seatId").asLong();
    }

    @Test
    public void testConcurrentCouponUsage_OnlyOneSucceeds() throws Exception {
        // 1. Create Coupon with maxUses = 1
        DiscountCodeDto couponDto = DiscountCodeDto.builder()
                .code("LIMIT1")
                .type(DiscountCodeType.FLAT)
                .value(new BigDecimal("50.00"))
                .validFrom(OffsetDateTime.now().minusDays(1))
                .validTo(OffsetDateTime.now().plusDays(5))
                .maxUses(1)
                .build();

        mockMvc.perform(post("/api/v1/admin/discount-codes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(couponDto)))
                .andExpect(status().isCreated());

        // 2. Hold Seat 1 (User 1 hold)
        HoldRequest holdReq1 = new HoldRequest();
        holdReq1.setSeatIds(Collections.singletonList(seatId1));
        String holdRes1 = mockMvc.perform(post("/api/v1/shows/" + showId + "/holds")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq1)))
                .andReturn().getResponse().getContentAsString();
        Long holdId1 = objectMapper.readTree(holdRes1).get("holdId").asLong();

        // 3. Hold Seat 2 (User 1 hold - using same customer token for simplicity)
        HoldRequest holdReq2 = new HoldRequest();
        holdReq2.setSeatIds(Collections.singletonList(seatId2));
        String holdRes2 = mockMvc.perform(post("/api/v1/shows/" + showId + "/holds")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq2)))
                .andReturn().getResponse().getContentAsString();
        Long holdId2 = objectMapper.readTree(holdRes2).get("holdId").asLong();

        // 4. Concurrent Confirm Requests
        ConfirmBookingRequest confirmReq1 = ConfirmBookingRequest.builder()
                .holdId(holdId1)
                .paymentToken("card_tok_1")
                .discountCode("LIMIT1")
                .build();

        ConfirmBookingRequest confirmReq2 = ConfirmBookingRequest.builder()
                .holdId(holdId2)
                .paymentToken("card_tok_2")
                .discountCode("LIMIT1")
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                int status = mockMvc.perform(post("/api/v1/bookings/confirm")
                                .header("Authorization", "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(confirmReq1)))
                        .andReturn().getResponse().getStatus();
                if (status == 200) {
                    successCount.incrementAndGet();
                } else if (status == 400) {
                    failureCount.incrementAndGet();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                int status = mockMvc.perform(post("/api/v1/bookings/confirm")
                                .header("Authorization", "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(confirmReq2)))
                        .andReturn().getResponse().getStatus();
                if (status == 200) {
                    successCount.incrementAndGet();
                } else if (status == 400) {
                    failureCount.incrementAndGet();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishLatch.countDown();
            }
        });

        // Trigger simultaneous execution
        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        // Assertions: Exactly 1 confirmation must succeed and exactly 1 must fail due to limit constraints!
        assertEquals(1, successCount.get());
        assertEquals(1, failureCount.get());

        // Verify DB usage consumed is exactly 1
        DiscountCode code = discountCodeRepository.findByCode("LIMIT1").orElseThrow();
        assertEquals(1, code.getUsesConsumed());
    }
}
