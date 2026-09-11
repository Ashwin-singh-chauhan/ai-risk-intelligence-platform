package com.deloitte.erip.anomaly.dto;

public record AnomalyScoreRequest(
        String eventType,
        String severity,
        Integer sourcePort,
        Integer destinationPort,
        String protocol,
        int hourOfDay,
        int dayOfWeek,
        double assetCriticalityWeight,
        double assetExposureWeight
) {
}
