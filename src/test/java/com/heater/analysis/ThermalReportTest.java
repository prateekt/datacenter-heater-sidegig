package com.heater.analysis;

import com.heater.config.ConfigLoader;
import com.heater.thermal.ScenarioUtil;
import com.heater.thermal.Simulator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ThermalReportTest {

    @Test
    void fromSimulatorAnnualizesPerLoadEnergy() throws Exception {
        Map<String, Object> config = ConfigLoader.load("config/nvidia_us_expansion.yaml");
        Simulator sim = new Simulator(config);
        var scenario = ScenarioUtil.fromConfig(config, "nvidia_us_module");
        sim.run(scenario, null);

        ThermalReport report = ThermalReport.fromSimulator(sim, scenario, 3600.0, 34.0, 1);
        assertTrue(report.recoveredMwh() > 0);
        assertTrue(report.dacMwh() >= 0);
        assertTrue(report.meanBufferTempC() > 0);
        double loadSum = report.poolMwh() + report.aquacultureMwh()
                + report.houseMwh() + report.algaeMwh() + report.dacMwh();
        assertEquals(report.recoveredMwh(), loadSum, Math.max(1.0, report.recoveredMwh() * 0.05));
    }
}
