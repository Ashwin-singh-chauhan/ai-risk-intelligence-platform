package com.deloitte.erip.event;

public enum EventSeverity {
    INFO(1), LOW(3), MEDIUM(8), HIGH(15), CRITICAL(25);

    private final int weight;

    EventSeverity(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
