package com.hus.mim_backend.application.analytics.usecase;

import com.hus.mim_backend.application.analytics.dto.AdminAnalyticsOverviewResponse;

public interface QueryAdminAnalyticsUseCase {
    AdminAnalyticsOverviewResponse getOverview(int months, int onlineWindowMinutes);
}
