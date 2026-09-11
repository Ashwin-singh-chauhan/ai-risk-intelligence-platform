package com.deloitte.erip.asset;

public enum Exposure {
    INTERNAL(1), DMZ(2), PUBLIC(3);

    private final int weight;

    Exposure(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
