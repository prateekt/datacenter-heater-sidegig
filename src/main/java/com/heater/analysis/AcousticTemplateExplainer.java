package com.heater.analysis;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AcousticTemplateExplainer {

    private AcousticTemplateExplainer() {}

    public static String explain(
            AcousticResultsSummary summary,
            AcousticAnalogies analogies,
            AcousticLiterature literature
    ) {
        AcousticSweepPoint ref = summary.referencePoint();
        var runs = summary.referenceRuns();
        StringBuilder sb = new StringBuilder();

        sb.append("<a id=\"acoustic-speculative\"></a>\n\n");
        sb.append("## Speculative side gig: acoustic valorization (explain like I'm five)\n\n");
        sb.append("*Auto-generated **speculative** results — mechanical soundscape equalizer (MSE) and ")
                .append("mechanical diffusion music generator (MDMG). Not mixed with main thermal CO₂ results.*\n\n");

        sb.append("> **In one sentence:** ").append(analogies.oneLiner).append("\n\n");

        appendPictureThis(sb, analogies);
        appendWhatWeTried(sb, analogies);
        appendPlainEnglish(sb, analogies, ref, runs);
        appendNumbers(sb, ref, runs);
        appendCharts(sb, summary);
        appendAudio(sb, summary);
        appendScheme2(sb, runs);
        appendLiterature(sb, literature, ref, runs);
        appendConclusion(sb, ref, runs);
        appendHonestLimits(sb, analogies);

        sb.append("### Generated at: ").append(summary.generatedAt()).append("\n\n");
        sb.append("### Sources\n\n");
        sb.append("- Physics: `src/main/java/com/heater/acoustic/`\n");
        sb.append("- Defaults: `config/acoustic_spectrum.yaml`, `config/mechanical_equalizer.yaml`, `config/mechanical_diffusion.yaml`\n");
        sb.append("- Literature: `config/acoustic_references.yaml`\n");
        sb.append("- ").append(analogies.disclaimer).append("\n");

        return sb.toString();
    }

    private static void appendPictureThis(StringBuilder sb, AcousticAnalogies analogies) {
        sb.append("### Picture this\n\n");
        sb.append("| Complicated word | Think of it like… |\n");
        sb.append("|------------------|-------------------|\n");
        Map<String, String> labels = Map.of(
                "fan_noise", "Fan noise",
                "metamaterial", "Metamaterial liner",
                "water_soundscape", "Water soundscape",
                "organ_pipes", "Organ pipes",
                "mechanical_diffusion", "Mechanical diffusion"
        );
        for (Map.Entry<String, String> e : labels.entrySet()) {
            String text = analogies.concepts.get(e.getKey());
            if (text != null) {
                sb.append("| ").append(e.getValue()).append(" | ").append(text).append(" |\n");
            }
        }
        sb.append("\n");
    }

    private static void appendWhatWeTried(StringBuilder sb, AcousticAnalogies analogies) {
        sb.append("### What the simulation tried (still experimental in real life)\n\n");
        for (int i = 0; i < analogies.steps.size(); i++) {
            sb.append(i + 1).append(". ").append(analogies.steps.get(i)).append("\n");
        }
        sb.append("\n");
    }

    private static void appendPlainEnglish(
            StringBuilder sb,
            AcousticAnalogies analogies,
            AcousticSweepPoint ref,
            AcousticResultsSummary.AcousticReferenceRuns runs
    ) {
        sb.append("### Results in plain English\n\n");
        sb.append("| Question a kid might ask | What we found |\n");
        sb.append("|--------------------------|---------------|\n");
        for (AcousticAnalogies.QuestionAnswer qa : analogies.kidQuestions) {
            sb.append("| ").append(qa.question()).append(" | ").append(qa.answer()).append(" |\n");
        }
        if (runs != null) {
            sb.append("| How loud at the fence before? | About **")
                    .append(fmt(runs.baselineDba())).append(" dBA** |\n");
            sb.append("| After mechanical equalizer (no chimney)? | About **")
                    .append(fmt(runs.mseDecoupledDba())).append(" dBA** |\n");
            sb.append("| With chimney-coupled water/pipes? | About **")
                    .append(fmt(runs.mseCoupledDba())).append(" dBA** |\n");
        }
        if (ref != null) {
            sb.append("| Does the liner cost fan electricity? | About **")
                    .append(fmt(ref.addedFanPowerW() / 1000.0)).append(" kW** added at reference depth |\n");
        }
        sb.append("\n");
    }

    private static void appendNumbers(
            StringBuilder sb,
            AcousticSweepPoint ref,
            AcousticResultsSummary.AcousticReferenceRuns runs
    ) {
        sb.append("### Then the numbers — Scheme 1 (MSE)\n\n");
        if (ref != null && runs != null) {
            sb.append("| Metric | Baseline | MSE decoupled | MSE chimney-coupled |\n");
            sb.append("|--------|----------|---------------|---------------------|\n");
            sb.append("| Fence-line SPL | **").append(fmt(runs.baselineDba())).append(" dBA** | **")
                    .append(fmt(runs.mseDecoupledDba())).append(" dBA** | **")
                    .append(fmt(runs.mseCoupledDba())).append(" dBA** |\n");
            sb.append("| Soundscape quality index | — | **")
                    .append(fmt(ref.soundscapeQualityIndex())).append("** | (see coupling sweep) |\n");
            sb.append("| Added fan power (liner) | — | **")
                    .append(fmt(ref.addedFanPowerW() / 1000.0)).append(" kW** | — |\n\n");
        }

        sb.append("### Scheme 2 (MDMG) reference run\n\n");
        if (runs != null) {
            sb.append("| Metric | Value |\n|--------|-------|\n");
            sb.append("| Reverse denoising steps | **").append(runs.mdmgSteps()).append("** |\n");
            sb.append("| Spectral distance to template | **").append(fmt(runs.mdmgSpectralDistance())).append("** |\n");
            sb.append("| Harmonicity | **").append(fmt(runs.mdmgHarmonicity())).append("** |\n\n");
        }
    }

    private static void appendCharts(StringBuilder sb, AcousticResultsSummary summary) {
        for (String chart : summary.chartPaths()) {
            String name = chart.substring(chart.lastIndexOf('/') + 1).replace(".png", "");
            sb.append("![")
                    .append(name.replace('_', ' '))
                    .append("](")
                    .append(chart)
                    .append(")\n\n");
        }
    }

    private static void appendAudio(StringBuilder sb, AcousticResultsSummary summary) {
        sb.append("### Audio samples (synthetic)\n\n");
        sb.append("| Clip | Description |\n|------|-------------|\n");
        for (String path : summary.audioPaths()) {
            String file = path.substring(path.lastIndexOf('/') + 1);
            String desc = switch (file) {
                case "fan_noise_baseline.wav" -> "Unmitigated fan noise at the fence line";
                case "mse_perimeter_decoupled.wav" -> "Scheme 1 — metamaterial + water/pipes (decoupled)";
                case "mse_perimeter_coupled.wav" -> "Scheme 1 — with chimney-draft coupling";
                case "mdmg_output.wav" -> "Scheme 2 — mechanical diffusion output";
                default -> "Synthetic acoustic clip";
            };
            sb.append("| [").append(file).append("](").append(path).append(") | ").append(desc).append(" |\n");
        }
        sb.append("\n");
    }

    private static void appendScheme2(StringBuilder sb, AcousticResultsSummary.AcousticReferenceRuns runs) {
        sb.append("### Scheme 2 — software vs. mechanical diffusion\n\n");
        sb.append("| Software diffusion | Our mechanical cartoon |\n");
        sb.append("|--------------------|-------------------------|\n");
        sb.append("| Gaussian noise sample | Fan noise time series |\n");
        sb.append("| Forward noising steps | Damping + coupling scramble |\n");
        sb.append("| Neural denoiser | Fixed energy landscape (springs + template harmonics) |\n");
        sb.append("| Reverse steps | Overdamped Langevin iterations |\n\n");
        if (runs != null) {
            sb.append("> **Verdict (MDMG):** Ambitious structured speculation — inference physics only; ")
                    .append("training campus-scale hardware remains **unsolved** (see Whitelam 2024; McMahon 2024).\n\n");
        }
    }

    private static void appendLiterature(
            StringBuilder sb,
            AcousticLiterature literature,
            AcousticSweepPoint ref,
            AcousticResultsSummary.AcousticReferenceRuns runs
    ) {
        sb.append("### How this compares to published research\n\n");
        sb.append(literature.comparisonIntro).append("\n\n");
        sb.append("| Paper | What they report | How our sim compares |\n");
        sb.append("|-------|------------------|----------------------|\n");
        for (AcousticLiterature.Paper p : literature.papers) {
            sb.append("| [").append(p.authors()).append(" (").append(p.year()).append(")](").append(p.url()).append(") | ")
                    .append(p.keyNumbers()).append(" | ").append(p.vsSim()).append(" |\n");
        }
        sb.append("\n");

        sb.append("### What the papers do and do not cover\n\n");
        sb.append("| Question | Answer from literature |\n");
        sb.append("|----------|------------------------|\n");
        for (AcousticAnalogies.QuestionAnswer qa : literature.plainEnglishGaps) {
            sb.append("| ").append(qa.question()).append(" | ").append(qa.answer()).append(" |\n");
        }
        sb.append("\n");

        if (ref != null && runs != null) {
            sb.append("**Our reference run vs. literature anchors:**\n\n");
            sb.append("| Metric | This sim | Literature anchor |\n");
            sb.append("|--------|----------|-------------------|\n");
            sb.append("| Metamaterial attenuation | **").append(fmt(ref.reductionDba()))
                    .append(" dBA** fence-line shift | **~2.5 dBA** rack liner (Bell Labs SeMSA) |\n");
            sb.append("| Water masking | SQI **").append(fmt(ref.soundscapeQualityIndex()))
                    .append("** | Informational masking (Galbrun & Ali 2013) |\n");
            sb.append("| MDMG harmonicity | **").append(fmt(runs.mdmgHarmonicity()))
                    .append("** | Physical generative computing — lab scale only |\n\n");
        }
    }

    private static void appendConclusion(
            StringBuilder sb,
            AcousticSweepPoint ref,
            AcousticResultsSummary.AcousticReferenceRuns runs
    ) {
        sb.append("### Conclusion — synthesis and research positioning\n\n");
        sb.append("> **Verdict (MSE):** Credible near-term sketch — metamaterial rack liners are demonstrated; ")
                .append("water soundscapes improve perceived soundscape via informational masking. ")
                .append("Open questions: fan pressure-drop penalty, infrasound (&lt;20 Hz), perimeter scale-up.\n\n");
        sb.append("> **Verdict (MDMG):** Ambitious structured speculation — no published GPU-campus mechanical ")
                .append("music diffusion. Our simulator shows a **fixed-template** Langevin cartoon, not trainable hardware.\n\n");
        sb.append("#### Relationship to the main side gig\n\n");
        sb.append("Thermal side gigs route **heat** to DAC, algae, and community loads. Acoustic side gigs ask what ")
                .append("**sound and airflow** could become before treating them as pure nuisance — complementary to ")
                .append("chimney draft (`ConvectionCapturePhysics`) when thermally coupled.\n\n");
    }

    private static void appendHonestLimits(StringBuilder sb, AcousticAnalogies analogies) {
        sb.append("### Honest limits\n\n");
        for (String limit : analogies.honestLimits) {
            sb.append("- ").append(limit).append("\n");
        }
        sb.append("\n");
    }

    private static String fmt(double v) {
        return String.format(Locale.US, "%.2f", v);
    }
}
