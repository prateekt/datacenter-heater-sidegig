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
        sb.append("## Acoustic side gig (speculative)\n\n");
        sb.append("*A computer experiment — not mixed with main thermal CO₂ results. Nothing here is built at scale yet.*\n\n");

        appendOverview(sb, analogies, runs);
        appendFanOrchestra(sb, analogies, summary, runs);
        appendMse(sb, ref, runs, summary);
        appendMdmg(sb, runs, summary);
        appendAudio(sb, summary);
        appendCharts(sb, summary);
        appendResearch(sb, literature, ref, runs, analogies);
        appendFooter(sb, summary, analogies);

        return sb.toString();
    }

    private static void appendOverview(
            StringBuilder sb,
            AcousticAnalogies analogies,
            AcousticResultsSummary.AcousticReferenceRuns runs
    ) {
        sb.append("### At a glance\n\n");
        sb.append("**The question:** Data centers are loud because cooling fans never stop. ")
                .append("What if that hum became structured music — using physics, not speakers?\n\n");
        sb.append("**Jump to:** ");
        sb.append("[Overview](#acoustic-overview) · ");
        sb.append("[1 · Fan orchestra](#acoustic-fan-orchestra) · ");
        sb.append("[2 · MSE equalizer](#acoustic-mse) · ");
        sb.append("[3 · MDMG diffusion](#acoustic-mdmg) · ");
        sb.append("[Audio](#acoustic-audio) · ");
        sb.append("[Charts](#acoustic-charts) · ");
        sb.append("[Research](#acoustic-research)\n\n");

        sb.append("<a id=\"acoustic-overview\"></a>\n\n");
        sb.append("**Three layers — read top to bottom:**\n\n");
        sb.append("| Step | Name | What it does (plain English) |\n");
        sb.append("|------|------|------------------------------|\n");
        sb.append("| **1** | **Fan orchestra** | Each cooling fan spins a tiny [Pickaso Rotary Bow](https://www.pickasobow.com/products/pickaso-rotary-bow) on a string — **15,000 sustained voices** at Colossus scale |\n");
        sb.append("| **2** | **MSE** (Mechanical Soundscape Equalizer) | Honeycomb panels + water trickle **shape** the sound at the fence — no computer |\n");
        sb.append("| **3** | **MDMG** (Mechanical Diffusion Music Generator) | Coupled springs **refine** the orchestra rumble toward smoother music — like AI diffusion, but in metal |\n\n");

        sb.append("```\n");
        sb.append("GPU fans  →  Fan orchestra (15k bowed strings)  →  MSE (panels + water)  →  Fence line\n");
        sb.append("                              ↓\n");
        sb.append("                         MDMG (spring refinement)\n");
        sb.append("```\n\n");

        if (runs != null) {
            sb.append("**Reference run headline numbers:**\n\n");
            sb.append("- **").append(String.format(Locale.US, "%,d", runs.activeInstrumentCount()))
                    .append("** bowed-string instruments (1 per fan)\n");
            sb.append("- **").append(fmt(runs.baselineDba())).append(" dBA** at the fence before treatment\n");
            sb.append("- **").append(fmt(runs.mseDecoupledDba())).append(" dBA** after MSE (decoupled)\n");
            sb.append("- MDMG harmonicity **").append(fmt(runs.mdmgHarmonicity()))
                    .append("** on fan-orchestra input\n\n");
        }

        sb.append("> ").append(analogies.oneLiner).append("\n\n");
    }

    private static void appendFanOrchestra(
            StringBuilder sb,
            AcousticAnalogies analogies,
            AcousticResultsSummary summary,
            AcousticResultsSummary.AcousticReferenceRuns runs
    ) {
        sb.append("---\n\n");
        sb.append("<a id=\"acoustic-fan-orchestra\"></a>\n\n");
        sb.append("### 1 · Fan orchestra — the music source\n\n");

        var p = analogies.pickaso;
        sb.append("**The idea:** Mount a Pickaso-style rotary bow on every cooling fan. ")
                .append("The fan shaft spins the bow wheel; the string never stops vibrating while GPUs run.\n\n");

        if (p != null && !p.heroImage().isBlank()) {
            sb.append("| Pickaso product (real) | Tremobow demo frame |\n");
            sb.append("|------------------------|---------------------|\n");
            sb.append("| ![").append(p.productName()).append("](").append(p.heroImage()).append(") | ");
            if (!p.tremobowImage().isBlank()) {
                sb.append("![Tremobow](").append(p.tremobowImage()).append(") |\n\n");
            } else {
                sb.append("— |\n\n");
            }
            sb.append("Video: [Introducing the Pickaso Rotary Bow](").append(p.videoUrl()).append(")\n\n");
        }

        sb.append("**Pickaso → data center (one cell per fan):**\n\n");
        sb.append("| On a guitar | On a cooling fan |\n");
        sb.append("|-------------|------------------|\n");
        sb.append("| Motor spins elastic bow wheel | Fan shaft drives bow wheel |\n");
        sb.append("| Swappable covers (Tremobow, Vase, Curved) | Same three cover presets, rotated by voice |\n");
        sb.append("| Endless sustain | Continuous bowing while fans run |\n");
        sb.append("| One guitar | **15,000 cells** = 2,500 racks × 6 fans |\n\n");

        sb.append("**Scale (from [`config/acoustic_spectrum.yaml`](config/acoustic_spectrum.yaml)):**\n\n");
        sb.append("- **15,000** instruments · **~490 Hz** blade-passing tremolo · **150 m** fence distance\n\n");

        for (String chart : summary.chartPaths()) {
            if (chart.endsWith("fan_orchestra_cell.png") || chart.endsWith("fan_orchestra_at_scale.png")) {
                appendChartImage(sb, chart, captionFor(chart));
            }
        }

        if (runs != null) {
            sb.append("**Orchestra metrics (this run):**\n\n");
            sb.append("| Metric | Value | In plain English |\n");
            sb.append("|--------|-------|------------------|\n");
            sb.append("| Active instruments | **")
                    .append(String.format(Locale.US, "%,d", runs.activeInstrumentCount()))
                    .append("** | One bow cell per fan |\n");
            sb.append("| Musical content | **").append(fmt(runs.musicalContentDb()))
                    .append(" dB** | Energy in bow partials (200 Hz–2 kHz) |\n");
            sb.append("| Sustain index | **").append(fmt(runs.sustainIndex()))
                    .append("** | How \"bow-like\" the waveform is (0–1) |\n");
            sb.append("| Tremolo depth | **").append(fmt(runs.tremoloDepthDb()))
                    .append(" dB** | Blade-passing pulse on bow pressure |\n\n");
        }

        appendCompactSweep(sb, summary, "instrumented_fraction",
                "What if only some fans had bow cells?", "% fans instrumented");
        appendCompactSweep(sb, summary, "rack_count", "What if the hall were smaller or larger?", "Racks");
        appendCompactSweep(sb, summary, "bow_cover", "Which Pickaso cover sounds brightest?", "Cover");
    }

    private static void appendMse(
            StringBuilder sb,
            AcousticSweepPoint ref,
            AcousticResultsSummary.AcousticReferenceRuns runs,
            AcousticResultsSummary summary
    ) {
        sb.append("---\n\n");
        sb.append("<a id=\"acoustic-mse\"></a>\n\n");
        sb.append("### 2 · MSE — Mechanical Soundscape Equalizer\n\n");

        sb.append("**What it does:** Passive hardware at the fence line — no AI, no speakers.\n\n");
        sb.append("- **Metamaterial liner** — thin honeycomb panels that absorb the harshest fan tones (costs fan power)\n");
        sb.append("- **Water soundscape** — warm exhaust pushes water over rocks for stream-like masking\n");
        sb.append("- **Organ pipes** (optional) — chimney draft blows across tuned pipes\n\n");
        sb.append("The fan-orchestra waveform is mixed in **first**, then liners and water shape the result.\n\n");

        if (ref != null && runs != null) {
            sb.append("**Results:**\n\n");
            sb.append("| | Baseline (fans only) | MSE decoupled | MSE + chimney coupling |\n");
            sb.append("|---|------------------------|---------------|------------------------|\n");
            sb.append("| Fence SPL | **").append(fmt(runs.baselineDba())).append(" dBA** | **")
                    .append(fmt(runs.mseDecoupledDba())).append(" dBA** | **")
                    .append(fmt(runs.mseCoupledDba())).append(" dBA** |\n");
            sb.append("| Soundscape quality | — | **")
                    .append(fmt(ref.soundscapeQualityIndex())).append("** | see chart |\n");
            sb.append("| Extra fan power (liner) | — | **")
                    .append(fmt(ref.addedFanPowerW() / 1000.0)).append(" kW** | — |\n\n");
        }

        sb.append("> **Verdict:** Metamaterial rack liners are real (Bell Labs ~2.5 dBA). Water masking improves *perceived* peace. Open questions: fan pressure drop, infrasound, campus scale-up.\n\n");

        appendChartsMatching(sb, summary, "acoustic_spl_vs_liner", "acoustic_sqi_vs_water", "acoustic_coupling");
    }

    private static void appendMdmg(
            StringBuilder sb,
            AcousticResultsSummary.AcousticReferenceRuns runs,
            AcousticResultsSummary summary
    ) {
        sb.append("---\n\n");
        sb.append("<a id=\"acoustic-mdmg\"></a>\n\n");
        sb.append("### 3 · MDMG — Mechanical Diffusion Music Generator\n\n");

        sb.append("**What it does:** Refines the **fan-orchestra waveform** toward smoother music using coupled springs — ")
                .append("a mechanical cartoon of AI diffusion (Langevin dynamics instead of a neural network).\n\n");
        sb.append("| Software diffusion | Our MDMG cartoon |\n");
        sb.append("|--------------------|------------------|\n");
        sb.append("| Start from noise | Start from fan-orchestra clip |\n");
        sb.append("| Neural denoiser | Fixed spring energy landscape |\n");
        sb.append("| Reverse steps | Overdamped Langevin iterations |\n\n");

        if (runs != null) {
            sb.append("**Results (reference run):**\n\n");
            sb.append("| Metric | Value |\n");
            sb.append("|--------|-------|\n");
            sb.append("| Input | Fan-orchestra aggregate (15k voices) |\n");
            sb.append("| Reverse steps | **").append(runs.mdmgSteps()).append("** |\n");
            sb.append("| Spectral distance | **").append(fmt(runs.mdmgSpectralDistance())).append("** |\n");
            sb.append("| Harmonicity | **").append(fmt(runs.mdmgHarmonicity())).append("** |\n\n");
        }

        sb.append("For tier comparison (MDMG v1 / v2 / Java-LDM), see the [MDMG benchmark](#mdmg-benchmark) section below.\n\n");
        sb.append("> **Verdict:** Speculative — no one has built campus-scale mechanical music hardware. ")
                .append("This sim shows inference physics only.\n\n");

        appendChartsMatching(sb, summary, "acoustic_mdmg_vs_steps");
    }

    private static void appendAudio(StringBuilder sb, AcousticResultsSummary summary) {
        sb.append("---\n\n");
        sb.append("<a id=\"acoustic-audio\"></a>\n\n");
        sb.append("### Listen — synthetic audio clips\n\n");

        sb.append("**Step 1 — Fan orchestra**\n");
        appendAudioLink(sb, summary, "fan_orchestra_15k.wav",
                "15,000 bowed strings (aggregate)");
        appendAudioLink(sb, summary, "fan_orchestra_vs_raw_fan.wav",
                "Orchestra minus raw fan bed");
        appendAudioLink(sb, summary, "fan_noise_baseline.wav",
                "Raw fan noise at fence (before treatment)");

        sb.append("\n**Step 2 — MSE**\n");
        appendAudioLink(sb, summary, "mse_perimeter_decoupled.wav",
                "Orchestra + metamaterial + water (decoupled)");
        appendAudioLink(sb, summary, "mse_perimeter_coupled.wav",
                "Same + chimney-draft coupling");

        sb.append("\n**Step 3 — MDMG**\n");
        appendAudioLink(sb, summary, "mdmg_output.wav",
                "MDMG refines fan-orchestra input");

        sb.append("\n");
    }

    private static void appendAudioLink(StringBuilder sb, AcousticResultsSummary summary, String file, String desc) {
        for (String path : summary.audioPaths()) {
            if (path.endsWith(file)) {
                sb.append("- [").append(file).append("](").append(path).append(") — ").append(desc).append("\n");
                return;
            }
        }
    }

    private static void appendCharts(StringBuilder sb, AcousticResultsSummary summary) {
        sb.append("---\n\n");
        sb.append("<a id=\"acoustic-charts\"></a>\n\n");
        sb.append("### Charts & sweeps\n\n");
        sb.append("All figures regenerate with `./gradlew generateAcousticFigures`.\n\n");

        sb.append("**Fan orchestra**\n\n");
        for (String chart : summary.chartPaths()) {
            if (isOrchestraAnalyticsChart(chart)) {
                appendChartImage(sb, chart, captionFor(chart));
            }
        }

        sb.append("**MSE & MDMG**\n\n");
        for (String chart : summary.chartPaths()) {
            if (!isOrchestraAnalyticsChart(chart)
                    && !chart.endsWith("fan_orchestra_cell.png")
                    && !chart.endsWith("fan_orchestra_at_scale.png")) {
                appendChartImage(sb, chart, captionFor(chart));
            }
        }
    }

    private static void appendResearch(
            StringBuilder sb,
            AcousticLiterature literature,
            AcousticSweepPoint ref,
            AcousticResultsSummary.AcousticReferenceRuns runs,
            AcousticAnalogies analogies
    ) {
        sb.append("---\n\n");
        sb.append("<a id=\"acoustic-research\"></a>\n\n");
        sb.append("### Research, limits & verdicts\n\n");

        sb.append("**Honest limits**\n\n");
        for (String limit : analogies.honestLimits) {
            sb.append("- ").append(limit).append("\n");
        }
        sb.append("\n");

        if (ref != null && runs != null) {
            sb.append("**How our numbers compare to published work:**\n\n");
            sb.append("| Topic | This sim | Literature |\n");
            sb.append("|-------|----------|------------|\n");
            sb.append("| Metamaterial liner | **").append(fmt(ref.reductionDba()))
                    .append(" dBA** shift | ~2.5 dBA (Bell Labs rack liner) |\n");
            sb.append("| Water masking | SQI **").append(fmt(ref.soundscapeQualityIndex()))
                    .append("** | Informational masking (Galbrun & Ali 2013) |\n");
            sb.append("| MDMG harmonicity | **").append(fmt(runs.mdmgHarmonicity()))
                    .append("** | Lab-scale physical computing only |\n\n");
        }

        sb.append("<details>\n<summary><strong>Full literature table (click to expand)</strong></summary>\n\n");
        sb.append(literature.comparisonIntro).append("\n\n");
        sb.append("| Paper | What they report | How our sim compares |\n");
        sb.append("|-------|------------------|----------------------|\n");
        for (AcousticLiterature.Paper paper : literature.papers) {
            sb.append("| [").append(paper.authors()).append(" (").append(paper.year()).append(")](")
                    .append(paper.url()).append(") | ")
                    .append(paper.keyNumbers()).append(" | ").append(paper.vsSim()).append(" |\n");
        }
        sb.append("\n</details>\n\n");

        sb.append("**Relationship to the main repo:** Thermal side gigs route *heat* to DAC, algae, and recycling. ")
                .append("This acoustic module asks what *sound* could become — complementary, not a replacement.\n\n");
        sb.append("- ").append(analogies.disclaimer).append("\n\n");
    }

    private static void appendFooter(StringBuilder sb, AcousticResultsSummary summary, AcousticAnalogies analogies) {
        sb.append("<details>\n<summary><strong>Config & code paths (click to expand)</strong></summary>\n\n");
        sb.append("- Physics: `src/main/java/com/heater/acoustic/`\n");
        sb.append("- Config: `config/acoustic_spectrum.yaml`, `config/mechanical_equalizer.yaml`, `config/mechanical_diffusion.yaml`\n");
        sb.append("- Literature: `config/acoustic_references.yaml`\n");
        sb.append("- Generated: ").append(summary.generatedAt()).append("\n\n");
        sb.append("</details>\n\n");
    }

    private static void appendCompactSweep(
            StringBuilder sb,
            AcousticResultsSummary summary,
            String sweepId,
            String question,
            String scenarioLabel
    ) {
        List<AcousticSweepPoint> pts = summary.bySweep(sweepId);
        if (pts.isEmpty()) return;
        sb.append("<details>\n<summary><strong>").append(question).append("</strong></summary>\n\n");
        sb.append("| ").append(scenarioLabel).append(" | Fence SPL | Tonal prominence |\n");
        sb.append("|").append("-".repeat(scenarioLabel.length() + 2)).append("|-----------|------------------|\n");
        for (AcousticSweepPoint p : pts) {
            sb.append("| ").append(p.label()).append(" | **")
                    .append(fmt(p.fenceLineDba())).append(" dBA** | **")
                    .append(fmt(p.tonalProminenceDb())).append(" dB** |\n");
        }
        sb.append("\n</details>\n\n");
    }

    private static void appendChartsMatching(StringBuilder sb, AcousticResultsSummary summary, String... keywords) {
        for (String chart : summary.chartPaths()) {
            for (String kw : keywords) {
                if (chart.contains(kw)) {
                    appendChartImage(sb, chart, captionFor(chart));
                    break;
                }
            }
        }
    }

    private static boolean isOrchestraAnalyticsChart(String chart) {
        return chart.contains("orchestra") || chart.contains("fan_orchestra");
    }

    private static void appendChartImage(StringBuilder sb, String chart, String caption) {
        sb.append("*").append(caption).append("*\n\n");
        sb.append("![").append(caption).append("](").append(chart).append(")\n\n");
    }

    private static String captionFor(String chart) {
        String name = chart.substring(chart.lastIndexOf('/') + 1).replace(".png", "");
        return switch (name) {
            case "fan_orchestra_cell" -> "One fan cell: shaft → bow wheel → string → fence";
            case "fan_orchestra_at_scale" -> "Scale: 1 Pickaso cell vs 15,000 fan-orchestra cells";
            case "acoustic_orchestra_instrumented_fraction" -> "More instrumented fans → more tonal bow energy";
            case "acoustic_orchestra_rack_count" -> "Bigger hall → louder fence line";
            case "acoustic_orchestra_bow_covers" -> "Pickaso cover presets vs tonal prominence";
            case "acoustic_orchestra_bow_covers_spl" -> "Pickaso cover presets vs fence SPL";
            case "acoustic_spl_vs_liner_depth" -> "MSE: fence SPL vs metamaterial liner depth";
            case "acoustic_sqi_vs_water_flow" -> "MSE: soundscape quality vs water flow";
            case "acoustic_mdmg_vs_steps" -> "MDMG: quality vs reverse denoising steps";
            case "acoustic_coupling_comparison" -> "Baseline vs MSE decoupled vs chimney-coupled";
            default -> name.replace('_', ' ');
        };
    }

    private static String fmt(double v) {
        return String.format(Locale.US, "%.2f", v);
    }
}
