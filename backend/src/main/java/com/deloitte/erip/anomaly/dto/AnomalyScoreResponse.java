package com.deloitte.erip.anomaly.dto;

public record AnomalyScoreResponse(
        double anomalyScore,
        boolean isAnomaly,
        String modelVersion
) {
}
