package com.heater.analysis;

import com.heater.config.ConfigLoader;
import com.heater.thermal.SimulationMetrics;
import com.heater.thermal.Simulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class HeatApplicationAnalyzer {

    private HeatApplicationAnalyzer() {}

    public static List<HeatApplicationPoint> analyzeAll(
            String applicationsPath,
            String referenceConfigPath,
            GpuProfile.GpuProfileRegistry registry,
            double simDurationS
    ) throws IOException {
        Map<String, Object> appsRoot = ConfigLoader.load(applicationsPath);
        Map<String, Object> refConfig = ConfigLoader.load(referenceConfigPath);
        GpuProfile refProfile = registry.referenceProfile();
        int refGpus = registry.referenceGpuCount();

        List<Map<String, Object>> scenarios = (List<Map<String, Object>>) appsRoot.get("application_scenarios");
        List<HeatApplicationPoint> results = new ArrayList<>();
        if (scenarios == null) return results;

        double olympicKwh = ConfigLoader.d(ConfigLoader.map(appsRoot, "olympic_pool"), "annual_heat_kwh", 180_000);
        double communityPoolKwh = ConfigLoader.d(ConfigLoader.map(appsRoot, "community_pool"), "annual_heat_kwh", 45_000);
        double racewayM3 = ConfigLoader.d(ConfigLoader.map(appsRoot, "aquaculture"), "raceway_volume_m3", 500);
        double wPerM3 = ConfigLoader.d(ConfigLoader.map(appsRoot, "aquaculture"), "maintenance_w_per_m3", 55);
        double fishKgM3Yr = ConfigLoader.d(ConfigLoader.map(appsRoot, "aquaculture"), "fish_kg_per_m3_year", 25);
        double homeKwh = ConfigLoader.d(ConfigLoader.map(appsRoot, "district_heat"), "us_home_annual_heat_kwh", 8000);
        double showerKwh = ConfigLoader.d(ConfigLoader.map(appsRoot, "homeless_shelter_shower"), "kwh_per_shower", 2.5);
        double haPerMwHeat = ConfigLoader.d(ConfigLoader.map(appsRoot, "algae_pond"), "hectares_per_mw_heat", 0.5);
        Map<String, Object> plasticCfg = ConfigLoader.map(appsRoot, "plastic_recycling");
        double tonnesPetPerGwh = ConfigLoader.d(plasticCfg, "tonnes_pet_per_gwh", 6500.0);
        double racewayKwhPerYear = wPerM3 * racewayM3 * 8760.0 / 1000.0;

        for (Map<String, Object> sc : scenarios) {
            String id = sc.get("id").toString();
            String label = sc.getOrDefault("label", id).toString();
            String robotPriority = sc.getOrDefault("robot_priority", "").toString();
            String routingNote = sc.getOrDefault("routing_note", "").toString();
            String configPath = sc.get("config").toString();
            Map<String, Object> scenarioConfig = ConfigLoader.load(configPath);

            FacilityScaler.ScaledFacility facility = FacilityScaler.scale(
                    mergeRouting(scenarioConfig, refConfig),
                    "nvidia_us_module",
                    refProfile,
                    refGpus,
                    registry,
                    FacilityScaler.ScaleMode.PROPORTIONAL,
                    simDurationS
            );

            Simulator sim = new Simulator(facility.config());
            SimulationMetrics metrics = sim.run(facility.scenario(), null);
            double annualFactor = simDurationS > 0 ? (365.0 * 86400.0 / simDurationS) : 0.0;

            double poolMwh = sim.state.energyPoolJ / 3_600_000_000.0 * annualFactor;
            double aquaMwh = sim.state.energyAquacultureJ / 3_600_000_000.0 * annualFactor;
            double algaeMwh = sim.state.energyAlgaeJ / 3_600_000_000.0 * annualFactor;
            double dacMwh = sim.state.energyDacJ / 3_600_000_000.0 * annualFactor;
            double plasticDirectMwh = sim.state.energyPlasticDirectJ / 3_600_000_000.0 * annualFactor;
            double plasticBoostMwh = sim.state.energyPlasticBoostJ / 3_600_000_000.0 * annualFactor;
            double plasticMwh = plasticDirectMwh + plasticBoostMwh;
            double totalMwh = poolMwh + aquaMwh + algaeMwh + dacMwh + plasticMwh;

            double netTonnes = metrics.climate().annualizedTonnesCo2e();
            double aquaRaceways = aquaMwh * 1000.0 / racewayKwhPerYear;
            double fishKg = aquaRaceways * racewayM3 * fishKgM3Yr;
            double algaeHaFromHeat = (algaeMwh / 8760.0) * haPerMwHeat;

            results.add(new HeatApplicationPoint(
                    id,
                    label,
                    robotPriority,
                    routingNote,
                    netTonnes,
                    poolMwh,
                    aquaMwh,
                    algaeMwh,
                    dacMwh,
                    plasticMwh,
                    totalMwh,
                    poolMwh * 1000.0 / olympicKwh,
                    poolMwh * 1000.0 / communityPoolKwh,
                    aquaRaceways,
                    fishKg,
                    algaeHaFromHeat,
                    plasticMwh / 1000.0 * tonnesPetPerGwh,
                    totalMwh * 1000.0 / homeKwh,
                    totalMwh * 1000.0 / showerKwh,
                    metrics.poolSatisfactionPct()
            ));
        }
        return results;
    }

    /** Keep plant sizing from reference; copy robot priority and load defs from scenario. */
    private static Map<String, Object> mergeRouting(Map<String, Object> scenario, Map<String, Object> reference) {
        Map<String, Object> merged = ConfigDeepCopy.copy(reference);
        if (scenario.containsKey("robot")) {
            merged.put("robot", ConfigDeepCopy.copy(scenario).get("robot"));
        }
        if (scenario.containsKey("loads")) {
            merged.put("loads", ConfigDeepCopy.copy(scenario).get("loads"));
        }
        return merged;
    }

    public static String formatPoint(HeatApplicationPoint p) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US,
                "**%s** — **%,.0f tonnes CO₂e/yr** net. **Robot priority:** %s. "
                        + "**Routed heat:** **%,.0f MWh/yr** "
                        + "(pools **%,.0f** · fisheries **%,.0f** · algae **%,.0f** · plastic **%,.0f** · DAC **%,.0f**).",
                p.label(), p.netCo2eTonnesPerYear(),
                p.robotPriority().isBlank() ? "see config YAML" : p.robotPriority(),
                p.heatTotalMwh(),
                p.heatPoolMwh(), p.heatAquacultureMwh(), p.heatAlgaeMwh(),
                p.heatPlasticMwh(), p.heatDacMwh()));
        if (!p.routingNote().isBlank()) {
            sb.append(" ").append(p.routingNote());
        }

        if (p.heatPoolMwh() > 0.5) {
            sb.append(String.format(Locale.US,
                    " Pool service: **%.1f olympic-pool equivalents** (%.1f community pools).",
                    p.olympicPoolsEquivalent(), p.communityPoolsEquivalent()));
        }
        if (p.heatAquacultureMwh() > 0.5) {
            sb.append(String.format(Locale.US,
                    " Fisheries: **%.1f raceways** (500 m³), **~%,.0f kg fish/yr** potential.",
                    p.aquacultureRacewaysEquivalent(), p.fishProductionKgPerYear()));
        }
        if (p.heatAlgaeMwh() > 0.5) {
            sb.append(String.format(Locale.US,
                    " Algae: **%.2f ha** thermal service (from **%,.0f MWh/yr** to ponds).",
                    p.algaeHectaresEquivalent(), p.heatAlgaeMwh()));
        }
        if (p.heatPlasticMwh() > 0.5) {
            sb.append(String.format(Locale.US,
                    " Plastic recycling: **~%,.0f tonnes PET/yr** thermal-service equivalent (from **%,.0f MWh/yr**).",
                    p.petTonnesEquivalent(), p.heatPlasticMwh()));
        }
        sb.append(String.format(Locale.US,
                " *Hypothetical redirect* (same MWh to community uses): **~%,.0f homes** heat, **%s**.",
                p.homesHeatedEquivalent(), formatHotShowers(p.hotShowersEquivalent())));
        return sb.toString();
    }

    public static String formatHotShowers(double showersPerYear) {
        if (showersPerYear >= 1_000_000) {
            return String.format(Locale.US,
                    "~%.1f million shelter hot showers/yr (~%,.0f/day)",
                    showersPerYear / 1_000_000.0, showersPerYear / 365.0);
        }
        if (showersPerYear >= 1_000) {
            return String.format(Locale.US,
                    "~%,.0f shelter hot showers/yr (~%,.0f/day)",
                    showersPerYear, showersPerYear / 365.0);
        }
        return String.format(Locale.US, "~%.0f shelter hot showers/yr", showersPerYear);
    }

    public static String formatHotShowersCompact(double showersPerYear) {
        if (showersPerYear >= 1_000_000) {
            return String.format(Locale.US, "%.1fM", showersPerYear / 1_000_000.0);
        }
        if (showersPerYear >= 1_000) {
            return String.format(Locale.US, "%.0fK", showersPerYear / 1_000.0);
        }
        return String.format(Locale.US, "%.0f", showersPerYear);
    }
}
