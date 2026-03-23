package com.hus.mim_backend.infrastructure.adapter.web.analytics;

import com.hus.mim_backend.application.analytics.dto.TrackHeartbeatRequest;
import com.hus.mim_backend.application.analytics.dto.TrackPageViewRequest;
import com.hus.mim_backend.application.analytics.usecase.RecordAnalyticsTrackingUseCase;
import com.hus.mim_backend.shared.api.ApiResponse;
import com.hus.mim_backend.shared.constants.ApiEndpoints;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiEndpoints.PUBLIC_ANALYTICS)
public class PublicAnalyticsController {
    private final RecordAnalyticsTrackingUseCase recordAnalyticsTrackingUseCase;

    public PublicAnalyticsController(RecordAnalyticsTrackingUseCase recordAnalyticsTrackingUseCase) {
        this.recordAnalyticsTrackingUseCase = recordAnalyticsTrackingUseCase;
    }

    @PostMapping(ApiEndpoints.ANALYTICS_PAGE_VIEW)
    public ResponseEntity<ApiResponse<Void>> trackPageView(
            @RequestBody TrackPageViewRequest request,
            Authentication authentication) {
        recordAnalyticsTrackingUseCase.recordPageView(request, isAuthenticated(authentication));
        return ResponseEntity.ok(ApiResponse.success(null, "Track page view successfully"));
    }

    @PostMapping(ApiEndpoints.ANALYTICS_HEARTBEAT)
    public ResponseEntity<ApiResponse<Void>> trackHeartbeat(
            @RequestBody TrackHeartbeatRequest request,
            Authentication authentication) {
        recordAnalyticsTrackingUseCase.recordHeartbeat(request, isAuthenticated(authentication));
        return ResponseEntity.ok(ApiResponse.success(null, "Track heartbeat successfully"));
    }

    private boolean isAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String principal = String.valueOf(authentication.getPrincipal());
        if (!StringUtils.hasText(principal)) {
            return false;
        }

        return !"anonymousUser".equalsIgnoreCase(principal.trim());
    }
}
