package com.heater.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SweepRunnerTest {

    @Test
    void proportionalGpuRampIsMonotonicInNetCo2() throws Exception {
        SweepRunner runner = new SweepRunner(
                "config/nvidia_us_expansion.yaml",
                "src/test/resources/scalability_sweep_fast.yaml");
        ResultsSummary summary = runner.runAll();
        List<SweepPoint> ramp = summary.bySweep("gpu_count_ramp");
        assertTrue(ramp.size() >= 2);
        for (int i = 1; i < ramp.size(); i++) {
            assertTrue(ramp.get(i).annualizedNetTonnes() >= ramp.get(i - 1).annualizedNetTonnes() * 0.95,
                    "Net CO2 should grow with GPU count");
            assertTrue(ramp.get(i).thermal().annualizedRecoveredGwh()
                            >= ramp.get(i - 1).thermal().annualizedRecoveredGwh() * 0.95,
                    "Thermal GWh should grow with GPU count");
        }
    }
}
