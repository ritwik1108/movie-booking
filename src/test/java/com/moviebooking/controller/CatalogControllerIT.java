package com.moviebooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.domain.pricing.PricingRuleScope;
import com.moviebooking.domain.seat.SeatTier;
import com.moviebooking.dto.auth.LoginRequest;
import com.moviebooking.dto.catalog.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CatalogControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    public void setup() throws Exception {
        // Authenticate as pre-seeded admin user
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("admin@movie.com");
        loginReq.setPassword("admin123");

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        adminToken = objectMapper.readTree(response).get("token").asText();
    }

    @Test
    public void testFullCatalogWorkflow() throws Exception {
        // 1. Create a City
        CityDto cityReq = CityDto.builder().name("Mumbai").build();
        String cityResponse = mockMvc.perform(post("/api/v1/admin/cities")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cityReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Mumbai"))
                .andReturn().getResponse().getContentAsString();
        Long cityId = objectMapper.readTree(cityResponse).get("id").asLong();

        // 2. Create a Theater in City
        TheaterDto theaterReq = TheaterDto.builder()
                .cityId(cityId)
                .name("PVR Phoenix")
                .address("Lower Parel, Mumbai")
                .build();
        String theaterResponse = mockMvc.perform(post("/api/v1/admin/theaters")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(theaterReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long theaterId = objectMapper.readTree(theaterResponse).get("id").asLong();

        // 3. Create a Screen in Theater
        ScreenDto screenReq = ScreenDto.builder()
                .name("Audi 1")
                .build();
        String screenResponse = mockMvc.perform(post("/api/v1/admin/theaters/" + theaterId + "/screens")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(screenReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long screenId = objectMapper.readTree(screenResponse).get("id").asLong();

        // 4. Create seats in Screen (Bulk layout)
        BulkSeatCreationRequest bulkSeatsReq = new BulkSeatCreationRequest();
        BulkSeatCreationRequest.RowLayout rowA = new BulkSeatCreationRequest.RowLayout();
        rowA.setRowLabel("A");
        rowA.setNumSeats(5);
        rowA.setTier(SeatTier.REGULAR);
        bulkSeatsReq.setLayouts(Collections.singletonList(rowA));

        mockMvc.perform(post("/api/v1/admin/screens/" + screenId + "/seats")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkSeatsReq)))
                .andExpect(status().isCreated());

        // 5. Create a Movie
        MovieDto movieReq = MovieDto.builder()
                .title("Inception")
                .durationMinutes(148)
                .language("English")
                .genre("Sci-Fi")
                .certification("UA")
                .build();
        String movieResponse = mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long movieId = objectMapper.readTree(movieResponse).get("id").asLong();

        // 6. Create global pricing rule for SeatTier.REGULAR
        PricingRuleDto pricingReq = PricingRuleDto.builder()
                .scope(PricingRuleScope.GLOBAL)
                .seatTier(SeatTier.REGULAR)
                .basePrice(new BigDecimal("150.00"))
                .weekendMultiplier(new BigDecimal("1.2"))
                .build();
        mockMvc.perform(post("/api/v1/admin/pricing-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pricingReq)))
                .andExpect(status().isCreated());

        // 7. Create a Show
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startTime = now.plusDays(1);
        OffsetDateTime endTime = startTime.plusHours(2).plusMinutes(30);

        ShowDto showReq = ShowDto.builder()
                .movieId(movieId)
                .screenId(screenId)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        String showResponse = mockMvc.perform(post("/api/v1/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(showReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long showId = objectMapper.readTree(showResponse).get("id").asLong();

        // 8. Overlap Test - should fail 400 Bad Request
        ShowDto overlapShowReq = ShowDto.builder()
                .movieId(movieId)
                .screenId(screenId)
                .startTime(startTime.plusMinutes(30))
                .endTime(endTime.plusHours(1))
                .build();
        mockMvc.perform(post("/api/v1/admin/shows")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlapShowReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("SHOW_OVERLAP"));

        // 9. Customer Browsing Movies
        mockMvc.perform(get("/api/v1/movies")
                        .param("cityId", cityId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Inception"));

        // 10. Customer Browsing Shows
        mockMvc.perform(get("/api/v1/movies/" + movieId + "/shows")
                        .param("cityId", cityId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(showId));

        // 11. Customer getting Seat Map for Show
        mockMvc.perform(get("/api/v1/shows/" + showId + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].tier").value("REGULAR"))
                .andExpect(jsonPath("$[0].price").isNotEmpty());
    }
}
