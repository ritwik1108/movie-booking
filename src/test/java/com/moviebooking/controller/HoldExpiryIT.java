package com.moviebooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.domain.pricing.PricingRuleScope;
import com.moviebooking.domain.seat.SeatTier;
import com.moviebooking.domain.seathold.SeatHold;
import com.moviebooking.domain.seathold.SeatHoldStatus;
import com.moviebooking.domain.showseat.ShowSeat;
import com.moviebooking.domain.showseat.ShowSeatStatus;
import com.moviebooking.dto.auth.LoginRequest;
import com.moviebooking.dto.booking.HoldRequest;
import com.moviebooking.dto.catalog.*;
import com.moviebooking.repository.SeatHoldRepository;
import com.moviebooking.repository.ShowSeatRepository;
import com.moviebooking.scheduler.HoldExpirySweeper;
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
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class HoldExpiryIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private HoldExpirySweeper holdExpirySweeper;

    private String adminToken;
    private String customerToken;

    private Long showId;
    private Long seatId;

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

        setupShowAndSeat();
    }

    private void setupShowAndSeat() throws Exception {
        // City
        CityDto cityReq = CityDto.builder().name("Pune").build();
        String cityRes = mockMvc.perform(post("/api/v1/admin/cities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cityReq)))
                .andReturn().getResponse().getContentAsString();
        Long cityId = objectMapper.readTree(cityRes).get("id").asLong();

        // Theater
        TheaterDto theaterReq = TheaterDto.builder()
                .cityId(cityId)
                .name("PVR Nitesh")
                .address("Koregaon Park, Pune")
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
                .name("Audi 3")
                .build();
        String screenRes = mockMvc.perform(post("/api/v1/admin/theaters/" + theaterId + "/screens")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(screenReq)))
                .andReturn().getResponse().getContentAsString();
        Long screenId = objectMapper.readTree(screenRes).get("id").asLong();

        // Seats (1 regular seat)
        BulkSeatCreationRequest bulkSeatsReq = new BulkSeatCreationRequest();
        BulkSeatCreationRequest.RowLayout rowA = new BulkSeatCreationRequest.RowLayout();
        rowA.setRowLabel("A");
        rowA.setNumSeats(1);
        rowA.setTier(SeatTier.REGULAR);
        bulkSeatsReq.setLayouts(Collections.singletonList(rowA));

        mockMvc.perform(post("/api/v1/admin/screens/" + screenId + "/seats")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkSeatsReq)));

        // Movie
        MovieDto movieReq = MovieDto.builder()
                .title("Dunkirk")
                .durationMinutes(106)
                .language("English")
                .genre("War")
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
                .basePrice(new BigDecimal("120.00"))
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
                .endTime(OffsetDateTime.now().plusDays(2).plusHours(2))
                .build();
        String showRes = mockMvc.perform(post("/api/v1/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showReq)))
                .andReturn().getResponse().getContentAsString();
        showId = objectMapper.readTree(showRes).get("id").asLong();

        // Get seat ID
        String seatsRes = mockMvc.perform(get("/api/v1/shows/" + showId + "/seats"))
                .andReturn().getResponse().getContentAsString();
        seatId = objectMapper.readTree(seatsRes).get(0).get("seatId").asLong();
    }

    @Test
    public void testLazyExpiry_SelfHealsSeatMap() throws Exception {
        // 1. Hold Seat
        HoldRequest holdReq = new HoldRequest();
        holdReq.setSeatIds(Collections.singletonList(seatId));

        String holdRes = mockMvc.perform(post("/api/v1/shows/" + showId + "/holds")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long holdId = objectMapper.readTree(holdRes).get("holdId").asLong();

        // Verify it is HELD
        mockMvc.perform(get("/api/v1/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("HELD"));

        // 2. Backdate the hold in DB to simulate expiration
        SeatHold hold = seatHoldRepository.findById(holdId).orElseThrow();
        hold.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        seatHoldRepository.save(hold);

        // 3. Query seat map - should self-heal and return AVAILABLE
        mockMvc.perform(get("/api/v1/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        // 4. Verify DB state is updated to EXPIRED
        SeatHold updatedHold = seatHoldRepository.findById(holdId).orElseThrow();
        assertEquals(SeatHoldStatus.EXPIRED, updatedHold.getStatus());
    }

    @Test
    public void testExpirySweeper_BackgroundCleanup() throws Exception {
        // 1. Hold Seat
        HoldRequest holdReq = new HoldRequest();
        holdReq.setSeatIds(Collections.singletonList(seatId));

        String holdRes = mockMvc.perform(post("/api/v1/shows/" + showId + "/holds")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long holdId = objectMapper.readTree(holdRes).get("holdId").asLong();

        // 2. Backdate the hold in DB to simulate expiration
        SeatHold hold = seatHoldRepository.findById(holdId).orElseThrow();
        hold.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        seatHoldRepository.save(hold);

        // 3. Trigger sweeper manually
        holdExpirySweeper.sweepExpiredHolds();

        // 4. Verify DB states
        SeatHold updatedHold = seatHoldRepository.findById(holdId).orElseThrow();
        assertEquals(SeatHoldStatus.EXPIRED, updatedHold.getStatus());

        ShowSeat seat = showSeatRepository.findByHeldByHold(hold).stream().findFirst().orElse(null);
        // ShowSeat should have been freed (status AVAILABLE, heldByHold null)
        // Since it's freed, findByHeldByHold(hold) should be empty now.
        assertEquals(null, seat);
    }
}
