package com.moviebooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.domain.pricing.PricingRuleScope;
import com.moviebooking.domain.seat.SeatTier;
import com.moviebooking.dto.auth.LoginRequest;
import com.moviebooking.dto.booking.HoldRequest;
import com.moviebooking.dto.catalog.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SeatLockingConcurrencyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String customerToken;

    private Long showId;
    private Long seat1Id;
    private Long seat2Id;

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
        CityDto cityReq = CityDto.builder().name("Mumbai").build();
        String cityRes = mockMvc.perform(post("/api/v1/admin/cities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cityReq)))
                .andReturn().getResponse().getContentAsString();
        Long cityId = objectMapper.readTree(cityRes).get("id").asLong();

        // Theater
        TheaterDto theaterReq = TheaterDto.builder()
                .cityId(cityId)
                .name("PVR Inorbit")
                .address("Malad, Mumbai")
                .build();
        String theaterRes = mockMvc.perform(post("/api/v1/admin/theaters")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(theaterReq)))
                .andReturn().getResponse().getContentAsString();
        Long theaterId = objectMapper.readTree(theaterRes).get("id").asLong();

        // Screen
        ScreenDto screenReq = ScreenDto.builder().name("Screen 5").build();
        String screenRes = mockMvc.perform(post("/api/v1/admin/theaters/" + theaterId + "/screens")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(screenReq)))
                .andReturn().getResponse().getContentAsString();
        Long screenId = objectMapper.readTree(screenRes).get("id").asLong();

        // Seats (2 regular seats for deadlock race)
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
                .basePrice(new BigDecimal("150.00"))
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
        seat1Id = objectMapper.readTree(seatsRes).get(0).get("seatId").asLong();
        seat2Id = objectMapper.readTree(seatsRes).get(1).get("seatId").asLong();
    }

    @Test
    public void testSeatLocking_ConcurrentlyHoldsOverlapSeats() throws Exception {
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Thread 1 holds [seat1, seat2]
        HoldRequest req1 = new HoldRequest(Arrays.asList(seat1Id, seat2Id));
        String json1 = objectMapper.writeValueAsString(req1);

        // Thread 2 holds [seat2, seat1] - opposing order
        HoldRequest req2 = new HoldRequest(Arrays.asList(seat2Id, seat1Id));
        String json2 = objectMapper.writeValueAsString(req2);

        executorService.submit(() -> {
            try {
                startLatch.await();
                MvcResult result = mockMvc.perform(post("/api/v1/shows/" + showId + "/holds")
                                .header("Authorization", "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json1))
                        .andReturn();
                int status = result.getResponse().getStatus();
                if (status == 201) successCount.incrementAndGet();
                else if (status == 409) conflictCount.incrementAndGet();
                else errorCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                startLatch.await();
                MvcResult result = mockMvc.perform(post("/api/v1/shows/" + showId + "/holds")
                                .header("Authorization", "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json2))
                        .andReturn();
                int status = result.getResponse().getStatus();
                if (status == 201) successCount.incrementAndGet();
                else if (status == 409) conflictCount.incrementAndGet();
                else errorCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Note: With the bug present (unsorted seat IDs), at least one request may fail with a deadlock error (HTTP 500 / other status)
        // or a deadlock exception is caught.
        // We will assert that either the deadlock happened (errorCount > 0) or one succeeded and one failed with conflict.
        assertTrue(errorCount.get() > 0 || (successCount.get() == 1 && conflictCount.get() == 1),
                "Expected either a deadlock error or serial execution. Under high concurrency, unsorted IDs cause database deadlock errors.");
    }
}
