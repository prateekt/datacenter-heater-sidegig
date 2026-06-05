package com.heater.model;

public record SensorSnapshot(
        double primaryTOut,
        double primaryMdot,
        double secondaryTIn,
        double secondaryTOut,
        double bufferTemp,
        double poolTemp,
        double houseTemp,
        double algaeTemp,
        double ambientTemp,
        double hxDeltaT,
        boolean loadConnected,
        RouterState robotState,
        double qWaste,
        double ccsSourceTemp,
        double co2Rate
) {
    public static SensorSnapshot fromSystem(SystemState state, RouterState robotState) {
        boolean loadConnected = state.pool.connected
                || state.aquaculture.connected
                || state.house.connected
                || state.carbonCapture.connected
                || state.algae.connected
                || state.plasticRecycling.connected;
        return new SensorSnapshot(
                state.primary.tOut,
                state.primary.mdot,
                state.buffer.temperature,
                state.hx.tSecondaryOut,
                state.buffer.temperature,
                state.pool.temperature,
                state.house.temperature,
                state.algae.temperature,
                state.ambientTemp,
                Math.abs(state.hx.tSecondaryOut - state.buffer.temperature),
                loadConnected,
                robotState,
                state.primary.qWaste,
                state.buffer.temperature,
                state.carbonCapture.currentCaptureRateKgS + state.algae.currentGrowthRateKgS
        );
    }
}
