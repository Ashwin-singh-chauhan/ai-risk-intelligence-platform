package com.deloitte.erip.asset;

public enum Criticality {
    LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4);

    private final int weight;

    Criticality(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
