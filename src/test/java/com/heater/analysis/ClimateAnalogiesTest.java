package com.heater.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClimateAnalogiesTest {

    @Test
    void shareNeverShowsMisleadingZeroPercent() throws Exception {
        ClimateAnalogies scale = ClimateAnalogies.loadDefault();
        String small = scale.formatUsEmissionsShare(5_317);
        assertFalse(small.contains("0.00%"), "Should not show rounded-zero percent: " + small);
        assertTrue(small.contains("1 in"), small);

        String reference = scale.formatUsEmissionsShare(37_776);
        assertFalse(reference.contains("0.00%"), reference);
        assertTrue(reference.contains("thousand tonnes"), reference);
        assertTrue(reference.contains("1 in"), reference);
    }

    @Test
    void scaleNarrativeOmitsZeroPercentages() throws Exception {
        ClimateAnalogies scale = ClimateAnalogies.loadDefault();
        String narrative = scale.scaleNarrative(5_317);
        assertFalse(narrative.contains("0.00%"), narrative);
        assertFalse(narrative.contains("0.000%"), narrative);
    }
}
