package com.hus.mim_backend.infrastructure.adapter.web.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnalyticsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicTrackingEndpointShouldAllowAnonymous() throws Exception {
        mockMvc.perform(post("/api/public/analytics/page-view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "visitorId": "visitor-security-test",
                                  "routeKey": "HOME",
                                  "path": "/"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void adminOverviewShouldRejectAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = { "PERM_ADMIN_DASHBOARD_VIEW" })
    void adminOverviewShouldAllowAuthorizedPermission() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .param("months", "12")
                        .param("onlineWindowMinutes", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.kpis").exists())
                .andExpect(jsonPath("$.data.monthlyTraffic").isArray());
    }

    @Test
    @WithMockUser(authorities = { "PERM_MODERATION_POSTS_VIEW" })
    void adminOverviewShouldRejectInsufficientPermission() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview"))
                .andExpect(status().isForbidden());
    }
}
