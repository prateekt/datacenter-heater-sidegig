package com.heater.control;

import com.heater.model.RouterState;
import com.heater.model.SensorSnapshot;

public final class SafetyEvaluator {

    private SafetyEvaluator() {}

    public static SafetyBounds evaluate(SensorSnapshot sensors, SafetyLimits limits, double robotTaskElapsed) {
        SafetyBounds bounds = new SafetyBounds();

        if (sensors.primaryTOut() >= limits.primaryTTrip()) {
            bounds.forceFullReject = true;
            bounds.rejectFractionMin = 0.8;
            bounds.secondaryPumpMax = 0.2;
            bounds.alarm = "primary_overtemp";
        }

        if (sensors.primaryTOut() >= limits.primaryTMax()) {
            bounds.forceFullReject = true;
            bounds.rejectFractionMin = 1.0;
            bounds.allowSecondary = false;
            bounds.allowPoolValve = false;
            bounds.allowHouseValve = false;
            bounds.allowCcsValve = false;
            bounds.allowAlgaeValve = false;
            bounds.allowPlasticValve = false;
            bounds.alarm = "primary_trip";
        }

        if (sensors.primaryMdot() < limits.primaryMdotMin()) {
            bounds.forceFullReject = true;
            bounds.rejectFractionMin = 1.0;
            bounds.allowSecondary = false;
            bounds.allowPoolValve = false;
            bounds.allowHouseValve = false;
            bounds.allowCcsValve = false;
            bounds.allowAlgaeValve = false;
            bounds.allowPlasticValve = false;
            bounds.alarm = "primary_low_flow";
        }

        if (sensors.hxDeltaT() > limits.secondaryDtMax()) {
            bounds.forceFullReject = true;
            bounds.rejectFractionMin = 0.9;
            bounds.secondaryPumpMax = 0.0;
            bounds.allowSecondary = false;
            bounds.alarm = "sensor_fault_hx";
        }

        if (!sensors.loadConnected() && sensors.robotState() == RouterState.CONNECTED) {
            bounds.forceFullReject = true;
            bounds.rejectFractionMin = Math.max(bounds.rejectFractionMin, 0.5);
            if (bounds.alarm == null) bounds.alarm = "load_disconnected";
        }

        if (sensors.robotState() == RouterState.FAULT) {
            bounds.forceFullReject = true;
            bounds.rejectFractionMin = 1.0;
            bounds.allowSecondary = false;
            bounds.allowCcsValve = false;
            bounds.allowAlgaeValve = false;
            bounds.allowPlasticValve = false;
            if (bounds.alarm == null) bounds.alarm = "robot_fault";
        }

        if (robotTaskElapsed > limits.robotTaskTimeout()) {
            bounds.forceFullReject = true;
            bounds.rejectFractionMin = 1.0;
            bounds.allowSecondary = false;
            bounds.allowCcsValve = false;
            bounds.allowAlgaeValve = false;
            bounds.allowPlasticValve = false;
            if (bounds.alarm == null) bounds.alarm = "robot_task_timeout";
        }

        return bounds;
    }

    public static ActuatorClamp clamp(
            double rejectFraction,
            double secondaryPumpSpeed,
            boolean poolValve,
            boolean houseValve,
            boolean ccsValve,
            boolean algaeValve,
            boolean plasticValve,
            SafetyBounds bounds
    ) {
        if (bounds.forceFullReject) {
            rejectFraction = Math.max(rejectFraction, bounds.rejectFractionMin);
        }
        rejectFraction = Math.max(bounds.rejectFractionMin, Math.min(bounds.rejectFractionMax, rejectFraction));

        if (!bounds.allowSecondary) {
            secondaryPumpSpeed = 0.0;
        } else {
            secondaryPumpSpeed = Math.min(secondaryPumpSpeed, bounds.secondaryPumpMax);
        }

        if (!bounds.allowPoolValve) poolValve = false;
        if (!bounds.allowHouseValve) houseValve = false;
        if (!bounds.allowCcsValve) ccsValve = false;
        if (!bounds.allowAlgaeValve) algaeValve = false;
        if (!bounds.allowPlasticValve) plasticValve = false;

        return new ActuatorClamp(rejectFraction, secondaryPumpSpeed, poolValve, houseValve, ccsValve, algaeValve, plasticValve);
    }

    public record ActuatorClamp(
            double rejectFraction,
            double secondaryPumpSpeed,
            boolean poolValve,
            boolean houseValve,
            boolean ccsValve,
            boolean algaeValve,
            boolean plasticValve
    ) {}
}
