package com.heater.acoustic;

import com.heater.carbon.ConvectionCaptureConfig;
import com.heater.carbon.ConvectionCapturePhysics;

/**
 * Orchestrates Scheme 1 — Mechanical Soundscape Equalizer (MSE).
 */
public final class MechanicalEqualizerPhysics {

    public record MseResult(
            double[] baselineLw,
            double[] perimeterLw,
            SoundscapeMetrics.MetricsResult metrics,
            MetamaterialBank.AttenuationResult attenuation,
            WaterSoundscape.WaterResult water,
            OrganPipeArray.PipeResult pipes,
            AeolianElements.AeolianResult aeolian,
            double volumeFlowM3S,
            boolean thermallyCoupled
    ) {}

    private MechanicalEqualizerPhysics() {}

    public static MseResult solve(
            AcousticSpectrumConfig spectrumCfg,
            MechanicalEqualizerConfig eqCfg,
            ConvectionCapturePhysics.DraftResult draft
    ) {
        FanNoiseSpectrum.SpectrumResult baseline = FanNoiseSpectrum.compute(spectrumCfg);
        double bpf = baseline.bladePassingFrequencyHz();

        MetamaterialBank.AttenuationResult atten = MetamaterialBank.compute(eqCfg, bpf);
        double[] afterAtten = ThirdOctaveBands.subtractAttenuation(baseline.lwPerBand(), atten.attenuationDbPerBand());

        double airflowBoost = 1.0;
        double volumeFlow = 0.0;
        double perimeterWind = 2.0;

        if (eqCfg.thermalCouplingEnabled && draft != null && draft.volumeFlowM3S() > 0) {
            volumeFlow = draft.volumeFlowM3S();
            airflowBoost = 1.0 + Math.min(2.0, volumeFlow / 100.0);
            perimeterWind = 2.0 + volumeFlow / 80.0;
        }

        WaterSoundscape.WaterResult water = WaterSoundscape.compute(eqCfg, airflowBoost);
        OrganPipeArray.PipeResult pipes = OrganPipeArray.compute(eqCfg, volumeFlow, afterAtten, bpf);
        AeolianElements.AeolianResult aeolian = AeolianElements.compute(eqCfg, perimeterWind);

        double[] combined = ThirdOctaveBands.copy(afterAtten);
        combined = ThirdOctaveBands.addIncoherent(combined, water.lwPerBand());
        combined = ThirdOctaveBands.addIncoherent(combined, pipes.lwPerBand());
        combined = ThirdOctaveBands.addIncoherent(combined, aeolian.lwPerBand());

        SoundscapeMetrics.MetricsResult metrics = SoundscapeMetrics.compute(
                baseline.lwPerBand(), combined, baseline.tonalProminenceDb());

        return new MseResult(
                baseline.lwPerBand(),
                combined,
                metrics,
                atten,
                water,
                pipes,
                aeolian,
                volumeFlow,
                eqCfg.thermalCouplingEnabled
        );
    }

    public static ConvectionCapturePhysics.DraftResult resolveDraft(
            MechanicalEqualizerConfig eqCfg,
            ConvectionCaptureConfig convCfg,
            double qWasteW,
            double ambientTempC
    ) {
        if (!eqCfg.thermalCouplingEnabled || !eqCfg.useChimneyDraft) {
            return null;
        }
        if (convCfg != null) {
            convCfg.wasteHeatToAirFraction = eqCfg.wasteHeatToAirFraction;
            convCfg.chimneyHeightM = eqCfg.chimneyHeightM;
            return ConvectionCapturePhysics.solve(convCfg, qWasteW, ambientTempC);
        }
        return null;
    }

    /** Synthesize perimeter waveform by mixing attenuated fan noise with water/pipe tones. */
    public static double[] synthesizePerimeterWaveform(
            AcousticSpectrumConfig spectrumCfg,
            MseResult result,
            java.util.Random rng
    ) {
        double[] fan = FanNoiseSpectrum.synthesizeWaveform(spectrumCfg, rng);
        int n = fan.length;
        double[] out = new double[n];
        double attenFactor = Math.pow(10.0, -result.metrics().reductionDba() / 20.0);
        for (int i = 0; i < n; i++) {
            out[i] = fan[i] * Math.max(0.1, attenFactor);
        }
        double tStep = 1.0 / spectrumCfg.sampleRateHz;
        double waterAmp = result.water().overallDba() > -100
                ? Math.pow(10.0, (result.water().overallDba() - 60.0) / 20.0) * 0.3 : 0;
        double pipeAmp = result.pipes().musicalTonalContentDb() > -100
                ? Math.pow(10.0, (result.pipes().musicalTonalContentDb() - 60.0) / 20.0) * 0.2 : 0;

        for (int i = 0; i < n; i++) {
            double t = i * tStep;
            out[i] += waterAmp * (Math.sin(2 * Math.PI * 800 * t) + 0.5 * rng.nextGaussian());
            out[i] += pipeAmp * Math.sin(2 * Math.PI * 220 * t) * Math.sin(2 * Math.PI * 3 * t);
        }
        normalize(out, 0.9);
        return out;
    }

    private static void normalize(double[] x, double peak) {
        double max = 0.0;
        for (double v : x) max = Math.max(max, Math.abs(v));
        if (max > 1e-12) {
            double s = peak / max;
            for (int i = 0; i < x.length; i++) x[i] *= s;
        }
    }
}
