package com.hus.mim_backend.infrastructure.adapter.web.admin;

import com.hus.mim_backend.application.analytics.dto.AdminAnalyticsOverviewResponse;
import com.hus.mim_backend.application.analytics.usecase.QueryAdminAnalyticsUseCase;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import com.hus.mim_backend.shared.constants.RbacPermissions;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiEndpoints.ADMIN_ANALYTICS)
@PreAuthorize("hasAuthority('PERM_" + RbacPermissions.ADMIN_DASHBOARD_VIEW + "') or hasRole('ADMIN')")
public class AdminAnalyticsController {
    private final QueryAdminAnalyticsUseCase queryAdminAnalyticsUseCase;

    public AdminAnalyticsController(QueryAdminAnalyticsUseCase queryAdminAnalyticsUseCase) {
        this.queryAdminAnalyticsUseCase = queryAdminAnalyticsUseCase;
    }

    @GetMapping(ApiEndpoints.ANALYTICS_OVERVIEW)
    public ResponseEntity<ApiResponse<AdminAnalyticsOverviewResponse>> getOverview(
            @RequestParam(defaultValue = "12") int months,
            @RequestParam(defaultValue = "10") int onlineWindowMinutes) {
        AdminAnalyticsOverviewResponse overview = queryAdminAnalyticsUseCase.getOverview(months, onlineWindowMinutes);
        return ResponseEntity.ok(ApiResponse.success(overview, "Get admin analytics overview successfully"));
    }
}
