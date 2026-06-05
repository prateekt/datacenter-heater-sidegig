package com.heater.analysis;

import com.heater.acoustic.*;
import com.heater.carbon.ConvectionCaptureConfig;
import com.heater.carbon.ConvectionCapturePhysics;
import com.heater.config.ConfigLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("unchecked")
public final class AcousticCaptureAnalyzer {

    private final Map<String, Object> sweepConfig;
    private final Map<String, Object> spectrumDefaults;
    private final Map<String, Object> equalizerDefaults;
    private final Map<String, Object> diffusionDefaults;
    private final Map<String, Object> convectionDefaults;

    public AcousticCaptureAnalyzer(
            String sweepPath,
            String spectrumPath,
            String equalizerPath,
            String diffusionPath,
            String convectionPath
    ) throws IOException {
        this.sweepConfig = ConfigLoader.load(sweepPath);
        this.spectrumDefaults = ConfigLoader.load(spectrumPath);
        this.equalizerDefaults = ConfigLoader.load(equalizerPath);
        this.diffusionDefaults = ConfigLoader.load(diffusionPath);
        this.convectionDefaults = ConfigLoader.load(convectionPath);
    }

    public AcousticResultsSummary runAll() throws IOException {
        AcousticResultsSummary summary = new AcousticResultsSummary();
        double qWaste = ConfigLoader.d(sweepConfig, "q_waste_avg_w", 39_523_125.0);
        double ambientTempC = ConfigLoader.d(sweepConfig, "ambient_temp_c", 22.0);

        AcousticSpectrumConfig spectrumCfg = AcousticSpectrumConfig.fromMap(spectrumDefaults);
        MechanicalEqualizerConfig refEq = MechanicalEqualizerConfig.fromMap(equalizerDefaults);
        MechanicalDiffusionConfig refDiff = MechanicalDiffusionConfig.fromMap(diffusionDefaults);
        ConvectionCaptureConfig convCfg = ConvectionCaptureConfig.fromYaml(convectionDefaults);

        FanNoiseSpectrum.SpectrumResult baseline = FanNoiseSpectrum.compute(spectrumCfg);
        double baselineDba = baseline.overallDba();

        MechanicalEqualizerConfig decoupledEq = copyEq(refEq);
        decoupledEq.thermalCouplingEnabled = false;
        decoupledEq.useChimneyDraft = false;
        var mseDecoupled = MechanicalEqualizerPhysics.solve(spectrumCfg, decoupledEq, null);

        MechanicalEqualizerConfig coupledEq = copyEq(refEq);
        coupledEq.thermalCouplingEnabled = true;
        coupledEq.useChimneyDraft = true;
        var draft = MechanicalEqualizerPhysics.resolveDraft(coupledEq, convCfg, qWaste, ambientTempC);
        var mseCoupled = MechanicalEqualizerPhysics.solve(spectrumCfg, coupledEq, draft);

        Random rng = new Random(42);
        MechanicalDiffusionConfig diffCfg = refDiff;
        double[] fanWave = FanNoiseSpectrum.synthesizeWaveform(spectrumCfg, rng);
        var mdmg = MechanicalDiffusionPhysics.denoise(diffCfg, fanWave, rng);

        summary.setReferenceRuns(new AcousticResultsSummary.AcousticReferenceRuns(
                baselineDba,
                mseDecoupled.metrics().fenceLineDba(),
                mseCoupled.metrics().fenceLineDba(),
                mdmg.spectralDistanceToTemplate(),
                mdmg.harmonicity(),
                mdmg.reverseSteps()
        ));

        AcousticSweepPoint ref = toMsePoint("reference", "Reference MSE (decoupled)", decoupledEq, mseDecoupled, baselineDba, 0);
        summary.setReferencePoint(ref);
        summary.addPoint(ref);

        summary.addPoint(toMsePoint("coupling_compare", "MSE chimney-coupled", coupledEq, mseCoupled, baselineDba, 0));

        for (double depth : doubleList(sweepConfig, "liner_depths_mm")) {
            MechanicalEqualizerConfig eq = copyEq(refEq);
            eq.linerDepthMm = depth;
            eq.thermalCouplingEnabled = false;
            var r = MechanicalEqualizerPhysics.solve(spectrumCfg, eq, null);
            summary.addPoint(toMsePoint("liner_depth", depth + " mm liner", eq, r, baselineDba, 0));
        }

        for (double flow : doubleList(sweepConfig, "water_flows_l_s")) {
            MechanicalEqualizerConfig eq = copyEq(refEq);
            eq.waterFlowLS = flow;
            eq.waterEnabled = flow > 0;
            eq.thermalCouplingEnabled = false;
            var r = MechanicalEqualizerPhysics.solve(spectrumCfg, eq, null);
            summary.addPoint(toMsePoint("water_flow", flow + " L/s water", eq, r, baselineDba, 0));
        }

        for (int steps : intList(sweepConfig, "diffusion_steps")) {
            MechanicalDiffusionConfig dc = copyDiff(refDiff);
            dc.reverseSteps = steps;
            var dr = MechanicalDiffusionPhysics.denoise(dc, fanWave, rng);
            summary.addPoint(new AcousticSweepPoint(
                    "diffusion_steps", steps + " reverse steps",
                    refEq.linerDepthMm, refEq.waterFlowLS, steps, "n/a",
                    baselineDba, baselineDba, 0,
                    0, 0, 0,
                    dr.spectralDistanceToTemplate(), dr.harmonicity(), 0
            ));
        }

        for (String mode : stringList(sweepConfig, "coupling_modes")) {
            MechanicalEqualizerConfig eq = copyEq(refEq);
            boolean coupled = "chimney_coupled".equals(mode);
            eq.thermalCouplingEnabled = coupled;
            eq.useChimneyDraft = coupled;
            var d = coupled
                    ? MechanicalEqualizerPhysics.resolveDraft(eq, convCfg, qWaste, ambientTempC)
                    : null;
            var r = MechanicalEqualizerPhysics.solve(spectrumCfg, eq, d);
            summary.addPoint(toMsePoint("coupling_mode", mode, eq, r, baselineDba, 0));
        }

        exportAudio(summary, spectrumCfg, decoupledEq, coupledEq, convCfg, qWaste, ambientTempC, refDiff, rng);

        return summary;
    }

    private void exportAudio(
            AcousticResultsSummary summary,
            AcousticSpectrumConfig spectrumCfg,
            MechanicalEqualizerConfig decoupledEq,
            MechanicalEqualizerConfig coupledEq,
            ConvectionCaptureConfig convCfg,
            double qWaste,
            double ambientTempC,
            MechanicalDiffusionConfig diffCfg,
            Random rng
    ) throws IOException {
        Path audioDir = Path.of("docs/audio");
        Path figuresDir = Path.of("docs/figures");

        double[] baseline = FanNoiseSpectrum.synthesizeWaveform(spectrumCfg, rng);
        WavExporter.writeMono(audioDir.resolve("fan_noise_baseline.wav"), baseline, spectrumCfg.sampleRateHz);
        summary.addAudio("docs/audio/fan_noise_baseline.wav");
        SpectrogramGenerator.write(figuresDir.resolve("acoustic_spectrogram_baseline.png"), baseline, spectrumCfg.sampleRateHz, "baseline");

        var mseDec = MechanicalEqualizerPhysics.solve(spectrumCfg, decoupledEq, null);
        double[] mseWave = MechanicalEqualizerPhysics.synthesizePerimeterWaveform(spectrumCfg, mseDec, rng);
        WavExporter.writeMono(audioDir.resolve("mse_perimeter_decoupled.wav"), mseWave, spectrumCfg.sampleRateHz);
        summary.addAudio("docs/audio/mse_perimeter_decoupled.wav");
        SpectrogramGenerator.write(figuresDir.resolve("acoustic_spectrogram_mse.png"), mseWave, spectrumCfg.sampleRateHz, "mse");

        var draft = MechanicalEqualizerPhysics.resolveDraft(coupledEq, convCfg, qWaste, ambientTempC);
        var mseCou = MechanicalEqualizerPhysics.solve(spectrumCfg, coupledEq, draft);
        double[] mseCouWave = MechanicalEqualizerPhysics.synthesizePerimeterWaveform(spectrumCfg, mseCou, rng);
        WavExporter.writeMono(audioDir.resolve("mse_perimeter_coupled.wav"), mseCouWave, spectrumCfg.sampleRateHz);
        summary.addAudio("docs/audio/mse_perimeter_coupled.wav");

        var mdmg = MechanicalDiffusionPhysics.denoise(diffCfg, baseline, rng);
        WavExporter.writeMono(audioDir.resolve("mdmg_output.wav"), mdmg.outputWaveform(), spectrumCfg.sampleRateHz);
        summary.addAudio("docs/audio/mdmg_output.wav");
        SpectrogramGenerator.write(figuresDir.resolve("acoustic_spectrogram_mdmg.png"), mdmg.outputWaveform(), spectrumCfg.sampleRateHz, "mdmg");
    }

    private static AcousticSweepPoint toMsePoint(
            String sweepId,
            String label,
            MechanicalEqualizerConfig eq,
            MechanicalEqualizerPhysics.MseResult r,
            double baselineDba,
            double spectralDistance
    ) {
        var m = r.metrics();
        return new AcousticSweepPoint(
                sweepId, label,
                eq.linerDepthMm, eq.waterFlowLS, 0,
                eq.thermalCouplingEnabled ? "chimney_coupled" : "decoupled",
                baselineDba, m.fenceLineDba(), m.reductionDba(),
                m.soundscapeQualityIndex(), m.tonalProminenceDb(),
                r.attenuation().addedFanPowerW(),
                spectralDistance, 0, r.volumeFlowM3S()
        );
    }

    private static MechanicalEqualizerConfig copyEq(MechanicalEqualizerConfig src) {
        MechanicalEqualizerConfig c = MechanicalEqualizerConfig.fromMap(Map.of("mechanical_equalizer", eqMap(src)));
        return c;
    }

    private static Map<String, Object> eqMap(MechanicalEqualizerConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", c.enabled);
        m.put("liner_depth_mm", c.linerDepthMm);
        m.put("resonator_cell_count", c.resonatorCellCount);
        m.put("max_attenuation_db_per_band", c.maxAttenuationDbPerBand);
        m.put("pressure_drop_pa_per_mm", c.pressureDropPaPerMm);
        m.put("fan_efficiency", c.fanEfficiency);
        m.put("air_density_kg_m3", c.airDensityKgM3);
        m.put("baseline_fan_airflow_kg_s", c.baselineFanAirflowKgS);
        m.put("water_feature", Map.of(
                "enabled", c.waterEnabled, "type", c.waterType,
                "flow_l_s", c.waterFlowLS, "jet_height_m", c.jetHeightM));
        m.put("organ_pipes", Map.of("enabled", c.organPipesEnabled, "pipe_count", c.pipeCount, "fundamental_hz", c.pipeFundamentalHz));
        m.put("aeolian", Map.of("enabled", c.aeolianEnabled, "element_count", c.aeolianElementCount));
        m.put("thermal_coupling", Map.of(
                "enabled", c.thermalCouplingEnabled, "use_chimney_draft", c.useChimneyDraft,
                "waste_heat_to_air_fraction", c.wasteHeatToAirFraction, "chimney_height_m", c.chimneyHeightM));
        return m;
    }

    private static MechanicalDiffusionConfig copyDiff(MechanicalDiffusionConfig src) {
        return MechanicalDiffusionConfig.fromMap(Map.of("mechanical_diffusion", diffMap(src)));
    }

    private static Map<String, Object> diffMap(MechanicalDiffusionConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", c.enabled);
        m.put("oscillator_count", c.oscillatorCount);
        m.put("reverse_steps", c.reverseSteps);
        m.put("dt_s", c.dtS);
        m.put("damping", c.damping);
        m.put("coupling_strength", c.couplingStrength);
        m.put("noise_schedule_start", c.noiseScheduleStart);
        m.put("noise_schedule_end", c.noiseScheduleEnd);
        m.put("sample_rate_hz", c.sampleRateHz);
        m.put("clip_duration_s", c.clipDurationS);
        m.put("template", Map.of(
                "pink_noise_weight", c.pinkNoiseWeight,
                "harmonic_frequencies_hz", java.util.Arrays.stream(c.harmonicFrequenciesHz).boxed().toList(),
                "harmonic_amplitudes", java.util.Arrays.stream(c.harmonicAmplitudes).boxed().toList()));
        return m;
    }

    private static List<Double> doubleList(Map<String, Object> root, String key) {
        Object v = root.get(key);
        if (v instanceof List<?> list) {
            return list.stream().map(o -> ((Number) o).doubleValue()).toList();
        }
        return List.of();
    }

    private static List<Integer> intList(Map<String, Object> root, String key) {
        Object v = root.get(key);
        if (v instanceof List<?> list) {
            return list.stream().map(o -> ((Number) o).intValue()).toList();
        }
        return List.of();
    }

    private static List<String> stringList(Map<String, Object> root, String key) {
        return ConfigLoader.stringList(root, key);
    }
}
