package com.heater.model;

public enum LoadTarget {
    NONE,
    HOUSE,
    CARBON_CAPTURE,
    ALGAE,
    POOL,
    AQUACULTURE,
    PLASTIC_RECYCLING,
    BUFFER;

    public static LoadTarget fromString(String value) {
        if (value == null) {
            return NONE;
        }
        return switch (value.toLowerCase()) {
            case "house" -> HOUSE;
            case "carbon_capture" -> CARBON_CAPTURE;
            case "algae" -> ALGAE;
            case "pool" -> POOL;
            case "aquaculture", "fishery", "fisheries" -> AQUACULTURE;
            case "plastic_recycling", "plastic" -> PLASTIC_RECYCLING;
            case "buffer" -> BUFFER;
            case "none" -> NONE;
            default -> NONE;
        };
    }
}
