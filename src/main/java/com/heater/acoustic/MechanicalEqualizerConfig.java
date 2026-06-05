package com.heater.acoustic;

@SuppressWarnings("unchecked")
public final class MechanicalEqualizerConfig {

    public boolean enabled = true;
    public double linerDepthMm = 15;
    public int resonatorCellCount = 48;
    public double maxAttenuationDbPerBand = 12.0;
    public double pressureDropPaPerMm = 8.0;
    public double fanEfficiency = 0.65;
    public double airDensityKgM3 = 1.2;
    public double baselineFanAirflowKgS = 800.0;

    public boolean waterEnabled = true;
    public String waterType = "upward_jet_fountain";
    public double waterFlowLS = 12.0;
    public double jetHeightM = 0.8;

    public boolean organPipesEnabled = true;
    public int pipeCount = 16;
    public double pipeFundamentalHz = 220.0;

    public boolean aeolianEnabled = true;
    public int aeolianElementCount = 24;

    public boolean thermalCouplingEnabled = false;
    public boolean useChimneyDraft = false;
    public double wasteHeatToAirFraction = 0.35;
    public double chimneyHeightM = 120;

    public static MechanicalEqualizerConfig fromMap(java.util.Map<String, Object> root) {
        java.util.Map<String, Object> m = com.heater.config.ConfigLoader.map(root, "mechanical_equalizer");
        MechanicalEqualizerConfig c = new MechanicalEqualizerConfig();
        c.enabled = bool(m, "enabled", true);
        c.linerDepthMm = com.heater.config.ConfigLoader.d(m, "liner_depth_mm", c.linerDepthMm);
        c.resonatorCellCount = (int) com.heater.config.ConfigLoader.d(m, "resonator_cell_count", c.resonatorCellCount);
        c.maxAttenuationDbPerBand = com.heater.config.ConfigLoader.d(m, "max_attenuation_db_per_band", c.maxAttenuationDbPerBand);
        c.pressureDropPaPerMm = com.heater.config.ConfigLoader.d(m, "pressure_drop_pa_per_mm", c.pressureDropPaPerMm);
        c.fanEfficiency = com.heater.config.ConfigLoader.d(m, "fan_efficiency", c.fanEfficiency);
        c.airDensityKgM3 = com.heater.config.ConfigLoader.d(m, "air_density_kg_m3", c.airDensityKgM3);
        c.baselineFanAirflowKgS = com.heater.config.ConfigLoader.d(m, "baseline_fan_airflow_kg_s", c.baselineFanAirflowKgS);

        java.util.Map<String, Object> water = com.heater.config.ConfigLoader.map(m, "water_feature");
        c.waterEnabled = bool(water, "enabled", true);
        c.waterType = str(water, "type", c.waterType);
        c.waterFlowLS = com.heater.config.ConfigLoader.d(water, "flow_l_s", c.waterFlowLS);
        c.jetHeightM = com.heater.config.ConfigLoader.d(water, "jet_height_m", c.jetHeightM);

        java.util.Map<String, Object> pipes = com.heater.config.ConfigLoader.map(m, "organ_pipes");
        c.organPipesEnabled = bool(pipes, "enabled", true);
        c.pipeCount = (int) com.heater.config.ConfigLoader.d(pipes, "pipe_count", c.pipeCount);
        c.pipeFundamentalHz = com.heater.config.ConfigLoader.d(pipes, "fundamental_hz", c.pipeFundamentalHz);

        java.util.Map<String, Object> aeolian = com.heater.config.ConfigLoader.map(m, "aeolian");
        c.aeolianEnabled = bool(aeolian, "enabled", true);
        c.aeolianElementCount = (int) com.heater.config.ConfigLoader.d(aeolian, "element_count", c.aeolianElementCount);

        java.util.Map<String, Object> thermal = com.heater.config.ConfigLoader.map(m, "thermal_coupling");
        c.thermalCouplingEnabled = bool(thermal, "enabled", false);
        c.useChimneyDraft = bool(thermal, "use_chimney_draft", false);
        c.wasteHeatToAirFraction = com.heater.config.ConfigLoader.d(thermal, "waste_heat_to_air_fraction", c.wasteHeatToAirFraction);
        c.chimneyHeightM = com.heater.config.ConfigLoader.d(thermal, "chimney_height_m", c.chimneyHeightM);
        return c;
    }

    private static boolean bool(java.util.Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        return v instanceof Boolean b ? b : def;
    }

    private static String str(java.util.Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v == null ? def : v.toString();
    }
}
