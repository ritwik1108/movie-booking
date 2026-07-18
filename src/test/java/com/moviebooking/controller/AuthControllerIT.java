package com.moviebooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.domain.user.Role;
import com.moviebooking.dto.auth.LoginRequest;
import com.moviebooking.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testRegisterAndLogin_Success() throws Exception {
        String email = "testuser" + System.currentTimeMillis() + "@test.com";

        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setEmail(email);
        registerReq.setPassword("password123");
        registerReq.setRole(Role.CUSTOMER);

        // 1. Register customer
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        // 2. Login customer
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(email);
        loginReq.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    public void testRegisterDuplicateEmail_Conflict() throws Exception {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setEmail("admin@movie.com"); // Pre-seeded admin user
        registerReq.setPassword("password123");
        registerReq.setRole(Role.CUSTOMER);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    public void testRoleGuard_CustomerAccessForbiddenOnAdminEndpoint() throws Exception {
        String email = "customer" + System.currentTimeMillis() + "@test.com";

        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setEmail(email);
        registerReq.setPassword("password123");
        registerReq.setRole(Role.CUSTOMER);

        // Register customer and get token
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        // Attempt to hit admin endpoint (GET /api/v1/admin/cities) with customer token
        mockMvc.perform(get("/api/v1/admin/cities")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }
}
