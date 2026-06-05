package com.heater;

import com.heater.analysis.HeatApplicationAnalyzer;
import com.heater.analysis.ThermalReport;
import com.heater.carbon.ClimateImpactCalculator;
import com.heater.config.ConfigLoader;
import com.heater.robot.RobotTaskLog;
import com.heater.thermal.ScenarioProfile;
import com.heater.thermal.ScenarioUtil;
import com.heater.thermal.SimulationMetrics;
import com.heater.thermal.Simulator;

import java.util.Locale;
import java.util.Map;

public final class App {

    private App() {}

    public static void main(String[] args) throws Exception {
        System.exit(run(args));
    }

    static int run(String[] args) throws Exception {
        String scenarioName = "winter_day";
        String configPath = null;
        Double duration = null;
        Double faultAt = null;
        boolean fast = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--scenario" -> scenarioName = args[++i];
                case "--config" -> configPath = args[++i];
                case "--duration" -> duration = Double.parseDouble(args[++i]);
                case "--fault-at" -> faultAt = Double.parseDouble(args[++i]);
                case "--fast" -> fast = true;
                default -> { }
            }
        }

        Map<String, Object> config = ConfigLoader.load(configPath);
        ScenarioProfile scenario = ScenarioUtil.fromConfig(config, scenarioName);
        if (duration != null) {
            scenario = new ScenarioProfile(
                    scenario.name(), duration, scenario.qWasteBase(),
                    scenario.qWastePeak(), scenario.outsideTemps()
            );
        } else if (fast) {
            scenario = new ScenarioProfile(
                    scenario.name(), 3600.0, scenario.qWasteBase(),
                    scenario.qWastePeak(), scenario.outsideTemps()
            );
        }

        Simulator sim = new Simulator(config);
        SimulationMetrics metrics = sim.run(scenario, faultAt);

        double avgMw = (scenario.qWasteBase() + scenario.qWastePeak()) / 2.0 / 1_000_000.0;
        ThermalReport thermal = ThermalReport.fromSimulator(sim, scenario, scenario.duration(), avgMw, 1);
        printReport(config, scenario, metrics, thermal);
        return metrics.primaryUnsafeTimeS() > 0 ? 1 : 0;
    }

    private static void printReport(
            Map<String, Object> config, ScenarioProfile scenario,
            SimulationMetrics metrics, ThermalReport thermal
    ) {
        ClimateImpactCalculator.ClimateReport c = metrics.climate();
        double gridKg = ConfigLoader.d(ConfigLoader.map(config, "climate"), "grid_co2_kg_per_kwh", 0.39);

        System.out.println("Scenario: " + scenario.name());
        System.out.printf("Duration: %.0f s (%.1f h)%n", scenario.duration(), scenario.duration() / 3600.0);
        System.out.println();
        System.out.println("--- Thermal outputs (grid-agnostic) ---");
        System.out.printf("Waste heat (avg):       %.1f MW%n", thermal.wasteHeatAvgMw());
        System.out.printf("Thermal service:        %,.1f MWh/yr (%.2f GWh/yr)%n",
                thermal.recoveredMwh(), thermal.annualizedRecoveredGwh());
        System.out.printf("Rejected to ambient:    %,.1f MWh/yr%n", thermal.rejectedMwh());
        System.out.printf("  DAC:                  %,.1f MWh/yr%n", thermal.dacMwh());
        System.out.printf("  Algae:                %,.1f MWh/yr%n", thermal.algaeMwh());
        System.out.printf("  Pool:                 %,.1f MWh/yr%n", thermal.poolMwh());
        System.out.printf("  Aquaculture:          %,.1f MWh/yr%n", thermal.aquacultureMwh());
        System.out.printf("Mean buffer temp:       %.1f C%n", thermal.meanBufferTempC());
        System.out.printf("Mean GPU loop out:      %.1f C%n", thermal.meanPrimaryTOutC());
        System.out.printf("Shelter showers equiv:  %s%n",
                HeatApplicationAnalyzer.formatHotShowers(thermal.recoveredMwh() * 1000.0 / 2.5));
        System.out.println();
        System.out.printf("--- Grid scenario (%.2f kg CO2/kWh) ---%n", gridKg);
        System.out.printf("DAC CO2 captured:       %,.1f kg%n", c.dacCo2Kg());
        System.out.printf("Algae CO2 fixed:        %,.1f kg%n", c.algaeCo2Kg());
        System.out.printf("Gross removal:          %,.1f kg%n", c.grossRemovalKg());
        System.out.printf("Heat-pump penalty:      %,.1f kg CO2e%n", c.operationalPenaltyKg());
        System.out.printf("Net CO2e removed:       %,.1f kg%n", c.netCo2eRemovedKg());
        System.out.printf("Annualized net:         %,.1f tonnes CO2e/yr%n", c.annualizedTonnesCo2e());
        System.out.println();
        System.out.println("--- System ---");
        System.out.printf(Locale.US, "Heat pump electricity:  %,.1f kWh%n", c.heatPumpElectricKwh());
        System.out.printf("CCS active time:        %.1f%%%n", c.ccsActivePct());
        System.out.printf("Primary unsafe time:    %.1f s%n", metrics.primaryUnsafeTimeS());
        System.out.printf("Pool satisfaction:      %.1f%%%n", metrics.poolSatisfactionPct());
        System.out.printf("House satisfaction:     %.1f%%%n", metrics.houseSatisfactionPct());
        System.out.println();
        System.out.println("--- Robot task log (last 15) ---");
        var events = metrics.robotEvents();
        int start = Math.max(0, events.size() - 15);
        for (int i = start; i < events.size(); i++) {
            RobotTaskLog e = events.get(i);
            String detail = e.detail().isEmpty() ? "" : " (" + e.detail() + ")";
            System.out.printf("  t=%8.0fs  %-18s  %s%s%n",
                    e.time(), e.event(), e.target().name().toLowerCase(), detail);
        }
    }
}
