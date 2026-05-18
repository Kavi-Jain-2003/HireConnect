package com.hireconnect.analytics.service;

import com.hireconnect.analytics.dto.AnalyticsSummary;
import java.util.Map;

public interface AnalyticsService {

    int getJobViewCount(Long jobId, String authHeader);

    int getAppCountByJob(Long jobId, String authHeader);

    double getViewToApplyRatio(Long jobId, String authHeader);

    double getTimeToHire(Long jobId, String authHeader);

    AnalyticsSummary getPipelineStats(Long recruiterId, String authHeader);

    AnalyticsSummary getPlatformStats(String authHeader);

    Map<String, Long> getTopJobCategories(String authHeader);
}
