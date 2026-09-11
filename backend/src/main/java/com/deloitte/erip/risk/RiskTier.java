package com.deloitte.erip.risk;

public enum RiskTier {
    LOW, MEDIUM, HIGH, CRITICAL;

    public static RiskTier fromScore(double score) {
        if (score >= 80) return CRITICAL;
        if (score >= 60) return HIGH;
        if (score >= 35) return MEDIUM;
        return LOW;
    }
}
