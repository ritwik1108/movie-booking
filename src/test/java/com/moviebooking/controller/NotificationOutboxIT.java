package com.moviebooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.domain.notification.NotificationOutbox;
import com.moviebooking.domain.notification.NotificationOutboxStatus;
import com.moviebooking.domain.pricing.PricingRuleScope;
import com.moviebooking.domain.seat.SeatTier;
import com.moviebooking.domain.showseat.ShowSeat;
import com.moviebooking.domain.user.User;
import com.moviebooking.dto.auth.LoginRequest;
import com.moviebooking.dto.booking.ConfirmBookingRequest;
import com.moviebooking.dto.booking.HoldRequest;
import com.moviebooking.dto.catalog.*;
import com.moviebooking.repository.NotificationOutboxRepository;
import com.moviebooking.repository.ShowSeatRepository;
import com.moviebooking.repository.UserRepository;
import com.moviebooking.scheduler.NotificationOutboxPoller;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class NotificationOutboxIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationOutboxPoller notificationOutboxPoller;

    private String adminToken;
    private String customerToken;

    private Long showId;
    private List<Long> seatIds;

    @BeforeEach
    public void setup() throws Exception {
        notificationOutboxRepository.deleteAll();

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
        CityDto cityReq = CityDto.builder().name("Delhi").build();
        String cityRes = mockMvc.perform(post("/api/v1/admin/cities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cityReq)))
                .andReturn().getResponse().getContentAsString();
        Long cityId = objectMapper.readTree(cityRes).get("id").asLong();

        TheaterDto theaterReq = TheaterDto.builder()
                .cityId(cityId)
                .name("PVR Select CityWalk")
                .address("Saket, New Delhi")
                .build();
        String theaterRes = mockMvc.perform(post("/api/v1/admin/theaters")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(theaterReq)))
                .andReturn().getResponse().getContentAsString();
        Long theaterId = objectMapper.readTree(theaterRes).get("id").asLong();

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

        ShowDto showReq = ShowDto.builder()
                .movieId(movieId)
                .screenId(screenId)
                .startTime(OffsetDateTime.now().plusDays(1))
                .endTime(OffsetDateTime.now().plusDays(1).plusHours(3))
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

    @Test
    public void testOutboxFlow_ConfirmAndCancel() throws Exception {
        // 1. Hold seats
        HoldRequest holdReq = new HoldRequest();
        holdReq.setSeatIds(seatIds);
        String holdRes = mockMvc.perform(post("/api/v1/shows/" + showId + "/holds")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(holdReq)))
                .andReturn().getResponse().getContentAsString();
        Long holdId = objectMapper.readTree(holdRes).get("holdId").asLong();

        // 2. Confirm booking
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

        // 3. Verify BOOKING_CONFIRMED event is transactionally enqueued
        List<NotificationOutbox> outboxList = notificationOutboxRepository.findAll();
        assertEquals(1, outboxList.size());
        NotificationOutbox confirmEvent = outboxList.get(0);
        assertEquals("BOOKING_CONFIRMED", confirmEvent.getType());
        assertEquals(NotificationOutboxStatus.PENDING, confirmEvent.getStatus());
        assertEquals(0, confirmEvent.getAttemptCount());

        // 4. Manually run the poller to process the outbox message
        notificationOutboxPoller.pollAndDispatch();

        // 5. Verify the event status is updated to SENT
        NotificationOutbox updatedConfirmEvent = notificationOutboxRepository.findById(confirmEvent.getId()).orElseThrow();
        assertEquals(NotificationOutboxStatus.SENT, updatedConfirmEvent.getStatus());
        assertEquals(1, updatedConfirmEvent.getAttemptCount());
        assertNotNull(updatedConfirmEvent.getSentAt());

        // 6. Cancel booking
        mockMvc.perform(post("/api/v1/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // 7. Verify BOOKING_CANCELLED event is enqueued
        List<NotificationOutbox> outboxList2 = notificationOutboxRepository.findAll();
        assertEquals(2, outboxList2.size());
        NotificationOutbox cancelEvent = outboxList2.stream()
                .filter(e -> e.getType().equals("BOOKING_CANCELLED"))
                .findFirst().orElseThrow();
        assertEquals(NotificationOutboxStatus.PENDING, cancelEvent.getStatus());

        // 8. Run poller to dispatch the cancellation email
        notificationOutboxPoller.pollAndDispatch();

        // 9. Verify sent status
        NotificationOutbox updatedCancelEvent = notificationOutboxRepository.findById(cancelEvent.getId()).orElseThrow();
        assertEquals(NotificationOutboxStatus.SENT, updatedCancelEvent.getStatus());
        assertEquals(1, updatedCancelEvent.getAttemptCount());
    }

    @Test
    public void testOutboxRetryFailureFlow() {
        User user = userRepository.findByEmail("customer@movie.com").orElseThrow();

        // Enqueue a failing notification
        NotificationOutbox record = NotificationOutbox.builder()
                .type("RETRY_TEST_FAIL")
                .recipientUser(user)
                .payloadJson("{\"message\":\"fail_notification\"}")
                .status(NotificationOutboxStatus.PENDING)
                .attemptCount(0)
                .build();
        notificationOutboxRepository.save(record);

        // 1st Poll attempt
        notificationOutboxPoller.pollAndDispatch();
        NotificationOutbox attempt1 = notificationOutboxRepository.findById(record.getId()).orElseThrow();
        assertEquals(NotificationOutboxStatus.PENDING, attempt1.getStatus());
        assertEquals(1, attempt1.getAttemptCount());

        // 2nd Poll attempt
        notificationOutboxPoller.pollAndDispatch();
        NotificationOutbox attempt2 = notificationOutboxRepository.findById(record.getId()).orElseThrow();
        assertEquals(NotificationOutboxStatus.PENDING, attempt2.getStatus());
        assertEquals(2, attempt2.getAttemptCount());

        // 3rd Poll attempt
        notificationOutboxPoller.pollAndDispatch();
        NotificationOutbox attempt3 = notificationOutboxRepository.findById(record.getId()).orElseThrow();
        // Since attemptCount hits 3, it should set status to FAILED
        assertEquals(NotificationOutboxStatus.FAILED, attempt3.getStatus());
        assertEquals(3, attempt3.getAttemptCount());
    }
}
