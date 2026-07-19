package com.moviebooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.dto.auth.LoginRequest;
import com.moviebooking.dto.refund.RefundPolicyDto;
import com.moviebooking.dto.refund.RefundRuleDto;
import com.moviebooking.repository.RefundPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RefundPolicyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefundPolicyRepository refundPolicyRepository;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    public void setup() throws Exception {
        refundPolicyRepository.deleteAll();

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
    }

    @Test
    public void testRefundPolicyCRUD_AsAdmin() throws Exception {
        // 1. Create Refund Policy
        RefundPolicyDto policyDto = RefundPolicyDto.builder()
                .name("Super Premium Refund Policy")
                .build();

        String createRes = mockMvc.perform(post("/api/v1/admin/refund-policies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(policyDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Super Premium Refund Policy"))
                .andReturn().getResponse().getContentAsString();
        Long policyId = objectMapper.readTree(createRes).get("id").asLong();

        // 2. Add Refund Rule
        RefundRuleDto ruleDto = RefundRuleDto.builder()
                .hoursBeforeMin(12)
                .hoursBeforeMax(48)
                .refundPercentage(new BigDecimal("75.00"))
                .build();

        mockMvc.perform(post("/api/v1/admin/refund-policies/" + policyId + "/rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ruleDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.hoursBeforeMin").value(12))
                .andExpect(jsonPath("$.hoursBeforeMax").value(48))
                .andExpect(jsonPath("$.refundPercentage").value(75.00));

        // 3. Get all policies
        mockMvc.perform(get("/api/v1/admin/refund-policies")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Super Premium Refund Policy"))
                .andExpect(jsonPath("$[0].rules", hasSize(1)))
                .andExpect(jsonPath("$[0].rules[0].refundPercentage").value(75.00));

        // 4. Delete rule
        // First we extract the rule ID
        String listRes = mockMvc.perform(get("/api/v1/admin/refund-policies")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString();
        Long ruleId = objectMapper.readTree(listRes).get(0).get("rules").get(0).get("id").asLong();

        mockMvc.perform(delete("/api/v1/admin/refund-policies/" + policyId + "/rules/" + ruleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify rule is deleted
        mockMvc.perform(get("/api/v1/admin/refund-policies")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rules", hasSize(0)));

        // 5. Delete Policy
        mockMvc.perform(delete("/api/v1/admin/refund-policies/" + policyId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify policy list is empty
        mockMvc.perform(get("/api/v1/admin/refund-policies")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testRefundPolicyCRUD_AsCustomer_Forbidden() throws Exception {
        RefundPolicyDto policyDto = RefundPolicyDto.builder()
                .name("Customer Policy Attempt")
                .build();

        mockMvc.perform(post("/api/v1/admin/refund-policies")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(policyDto)))
                .andExpect(status().isForbidden());
    }
}
