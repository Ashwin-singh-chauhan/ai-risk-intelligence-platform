package com.deloitte.erip.incident;

public enum IncidentSeverity {
    LOW(10), MEDIUM(25), HIGH(45), CRITICAL(70);

    private final int weight;

    IncidentSeverity(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
