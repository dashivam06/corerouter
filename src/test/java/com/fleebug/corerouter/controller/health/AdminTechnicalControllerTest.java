package com.fleebug.corerouter.controller.health;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
public class AdminTechnicalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testHealth_WithAdminRole_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/admin/technical/health"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testHealth_WithUserRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/technical/health"))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.message").value("Forbidden: Admin role required"));
    }

    @Test
    public void testHealth_NoAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/technical/health"))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.message").value("Authentication token is missing"));
    }
}
