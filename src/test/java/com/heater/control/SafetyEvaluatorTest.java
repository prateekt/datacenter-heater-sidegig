package com.heater.control;

import com.heater.model.RouterState;
import com.heater.model.SensorSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafetyEvaluatorTest {

    private SensorSnapshot base() {
        return new SensorSnapshot(
                50, 12, 30, 35, 30, 25, 20, 22, 5, 5, true,
                RouterState.CONNECTED, 200_000, 45, 0.01
        );
    }

    @Test
    void primaryTripDisablesSecondary() {
        var s = new SensorSnapshot(
                66, 12, 30, 35, 30, 25, 20, 22, 5, 5, true,
                RouterState.CONNECTED, 200_000, 45, 0
        );
        SafetyBounds b = SafetyEvaluator.evaluate(s, limits(), 0);
        assertTrue(b.forceFullReject);
        assertFalse(b.allowSecondary);
        assertFalse(b.allowCcsValve);
    }

    @Test
    void clampRespectsCcsValve() {
        SafetyBounds b = new SafetyBounds();
        b.forceFullReject = true;
        b.rejectFractionMin = 1.0;
        b.allowSecondary = false;
        b.allowCcsValve = false;
        var c = SafetyEvaluator.clamp(0.2, 0.8, true, true, true, true, true, b);
        assertFalse(c.ccsValve());
        assertEquals(0, c.secondaryPumpSpeed(), 1e-6);
    }

    private SafetyLimits limits() {
        return new SafetyLimits(63, 65, 8, 25, 120);
    }
}
