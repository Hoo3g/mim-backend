package com.hus.mim_backend.application.analytics.usecase;

import com.hus.mim_backend.application.analytics.dto.TrackHeartbeatRequest;
import com.hus.mim_backend.application.analytics.dto.TrackPageViewRequest;

public interface RecordAnalyticsTrackingUseCase {
    void recordPageView(TrackPageViewRequest request, boolean authenticated);

    void recordHeartbeat(TrackHeartbeatRequest request, boolean authenticated);
}
