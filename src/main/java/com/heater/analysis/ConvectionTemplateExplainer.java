package com.heater.analysis;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConvectionTemplateExplainer {

    private ConvectionTemplateExplainer() {}

    public static String explain(
            ConvectionResultsSummary summary,
            ConvectionAnalogies analogies,
            ConvectionLiterature literature
    ) {
        ConvectionSweepPoint ref = summary.referencePoint();
        StringBuilder sb = new StringBuilder();

        sb.append("## Speculative idea: chimney CO₂ capture (explain like I'm five)\n\n");
        sb.append("*Auto-generated **speculative** results — passive convection through giant CO₂-catching walls. ")
                .append("Not mixed with main thermal results. See [Glossary](#glossary) for simple definitions.*\n\n");

        sb.append("> **In one sentence:** ").append(analogies.oneLiner).append("\n\n");

        appendPictureThis(sb, analogies);
        appendWhatWeTried(sb, analogies);
        appendPlainEnglishResults(sb, analogies, ref);
        appendNumbers(sb, summary, ref);
        appendChartInterpretations(sb, summary, ref);
        appendLiteratureComparisons(sb, literature, ref);
        appendEnergyPolicy(sb, literature);
        appendConclusion(sb, summary, ref, literature);
        appendHonestLimits(sb, analogies);

        sb.append("### Generated at: ").append(summary.generatedAt()).append("\n\n");
        sb.append("### Sources\n\n");
        sb.append("- Physics: lumped buoyancy + porous-bed resistance (`src/main/java/com/heater/carbon/ConvectionCapturePhysics.java`)\n");
        sb.append("- Defaults: `config/passive_convection_capture.yaml`\n");
        sb.append("- Analogies: `config/convection_analogies.yaml`\n");
        sb.append("- Literature: `config/convection_references.yaml`\n");
        sb.append("- ").append(analogies.disclaimer).append("\n");

        return sb.toString();
    }

    private static void appendPictureThis(StringBuilder sb, ConvectionAnalogies analogies) {
        sb.append("### Picture this\n\n");
        sb.append("| Complicated word | Think of it like… |\n");
        sb.append("|------------------|-------------------|\n");
        Map<String, String> labels = Map.of(
                "waste_heat", "Waste heat",
                "convection", "Convection / chimney",
                "sorbent", "CO₂ capture sponge",
                "fan_savings", "Fans",
                "regeneration", "Regeneration"
        );
        for (Map.Entry<String, String> e : labels.entrySet()) {
            String text = analogies.concepts.get(e.getKey());
            if (text != null) {
                sb.append("| ").append(e.getValue()).append(" | ").append(text).append(" |\n");
            }
        }
        sb.append("\n");
    }

    private static void appendWhatWeTried(StringBuilder sb, ConvectionAnalogies analogies) {
        sb.append("### What the simulation tried (still experimental in real life)\n\n");
        for (int i = 0; i < analogies.steps.size(); i++) {
            sb.append(i + 1).append(". ").append(analogies.steps.get(i)).append("\n");
        }
        sb.append("\n");
    }

    private static void appendPlainEnglishResults(
            StringBuilder sb, ConvectionAnalogies analogies, ConvectionSweepPoint ref
    ) {
        sb.append("### Results in plain English\n\n");
        sb.append("| Question a kid might ask | What we found |\n");
        sb.append("|--------------------------|---------------|\n");
        for (ConvectionAnalogies.QuestionAnswer qa : analogies.kidQuestions) {
            sb.append("| ").append(qa.question()).append(" | ").append(qa.answer()).append(" |\n");
        }
        if (ref != null) {
            sb.append("| How much air does the chimney pull? | About **")
                    .append(String.format(Locale.US, "%.0f", ref.airflowM3S()))
                    .append(" m³/s** at our reference size |\n");
            sb.append("| How much fan electricity do we save? | About **")
                    .append(String.format(Locale.US, "%.2f", ref.fanSavedMw()))
                    .append(" MW** vs. fan-only baseline |\n");
            sb.append("| How much CO₂ per year (computer guess)? | **")
                    .append(String.format(Locale.US, "%.0f", ref.netCo2TonnesYr()))
                    .append(" tonnes net** after electricity penalty |\n");
        }
        sb.append("\n");
    }

    private static void appendNumbers(
            StringBuilder sb, ConvectionResultsSummary summary, ConvectionSweepPoint ref
    ) {
        sb.append("### Then the numbers\n\n");
        if (ref != null) {
            sb.append("**Reference hall** (~")
                    .append(String.format(Locale.US, "%.0f", ref.avgWasteHeatMw()))
                    .append(" MW waste heat, ")
                    .append(String.format(Locale.US, "%.0f", ref.chimneyHeightM()))
                    .append(" m chimney, ")
                    .append(String.format(Locale.US, "%.0f", ref.contactorAreaM2()))
                    .append(" m² contactors):\n\n");
            sb.append("| Metric | Value |\n|--------|-------|\n");
            sb.append("| Airflow | **").append(fmt(ref.airflowM3S())).append(" m³/s** |\n");
            sb.append("| Exhaust ΔT | **").append(fmt(ref.deltaTK())).append(" K** above ambient |\n");
            sb.append("| Fan baseline | **").append(fmt(ref.fanBaselineMw())).append(" MW** |\n");
            sb.append("| Fan with convection | **").append(fmt(ref.fanResidualMw())).append(" MW** |\n");
            sb.append("| Fan saved | **").append(fmt(ref.fanSavedMw())).append(" MW** |\n");
            sb.append("| Gross CO₂ captured | **").append(fmt(ref.grossCo2TonnesYr())).append(" t/yr** |\n");
            sb.append("| Net CO₂ (grid scenario) | **").append(fmt(ref.netCo2TonnesYr())).append(" t/yr** |\n\n");
        }

        for (String chart : summary.chartPaths()) {
            String name = chart.substring(chart.lastIndexOf('/') + 1).replace(".png", "").replace('_', ' ');
            sb.append("![")
                    .append(name)
                    .append("](")
                    .append(chart)
                    .append(")\n\n");
        }
    }

    private static void appendLiteratureComparisons(
            StringBuilder sb,
            ConvectionLiterature literature,
            ConvectionSweepPoint ref
    ) {
        sb.append("### How this compares to published research\n\n");
        if (!literature.comparisonIntro.isBlank()) {
            sb.append(literature.comparisonIntro.trim()).append("\n\n");
        }

        appendPaperGroup(sb, "arXiv & top reviews — net zero & negative emissions", literature.byCategory("arxiv_netzero"));
        appendPaperGroup(sb, "arXiv & reviews — DAC scaling", literature.byCategory("arxiv_dac"));
        appendPaperGroup(sb, "Peer-reviewed DAC & chimney physics", literature.byCategory("peer_reviewed"));
        appendPaperGroup(sb, "Nuclear + DAC comparisons (cited, not modeled)", literature.byCategory("nuclear_comparison"));

        sb.append("| Paper | What they report | How our sim compares |\n");
        sb.append("|-------|------------------|----------------------|\n");
        for (ConvectionLiterature.Paper p : literature.papers) {
            sb.append("| ").append(formatCite(p)).append(" | ").append(p.keyNumbers()).append(" | ")
                    .append(p.vsSim()).append(" |\n");
        }
        sb.append("\n");

        if (ref != null) {
            sb.append("**Our reference run vs. literature scale:**\n\n");
            sb.append("| Metric | This sim | Typical published DAC plant |\n");
            sb.append("|--------|----------|----------------------------|\n");
            sb.append("| Annual capture | **").append(fmt(ref.grossCo2TonnesYr())).append(" t/yr** | 0.9–1,000,000 t/yr (Keith 2018; McQueen 2020) |\n");
            sb.append("| Regen heat duty | **5.5 GJ/t-CO₂** (config default) | 0.5–18.75 GJ/t (Sabatino 2023 review) |\n");
            sb.append("| Air movement | **").append(fmt(ref.airflowM3S())).append(" m³/s** natural draft | Fan-driven contactors in commercial DAC |\n");
            sb.append("| Fan electricity saved | **").append(fmt(ref.fanSavedMw())).append(" MW** | Not reported — fans usually fully powered |\n\n");
        }

        if (!literature.plainEnglishGaps.isEmpty()) {
            sb.append("### What the papers do and do not cover\n\n");
            sb.append("| Question | Answer from literature |\n");
            sb.append("|----------|------------------------|\n");
            for (ConvectionAnalogies.QuestionAnswer qa : literature.plainEnglishGaps) {
                sb.append("| ").append(qa.question()).append(" | ").append(qa.answer()).append(" |\n");
            }
            sb.append("\n");
        }

        sb.append("#### Full references\n\n");
        for (ConvectionLiterature.Paper p : literature.papers) {
            sb.append("- ").append(p.authors()).append(" (").append(p.year()).append("). *")
                    .append(p.title()).append(".* ").append(p.venue()).append(".");
            if (!p.arxivId().isBlank()) {
                sb.append(" arXiv:").append(p.arxivId()).append(" — https://arxiv.org/abs/").append(p.arxivId());
            }
            if (!p.doi().isBlank()) {
                sb.append(" https://doi.org/").append(p.doi());
            } else if (!p.url().isBlank() && p.arxivId().isBlank()) {
                sb.append(" ").append(p.url());
            }
            sb.append("\n");
        }
        sb.append("\n");
    }

    private static void appendPaperGroup(StringBuilder sb, String heading, List<ConvectionLiterature.Paper> papers) {
        if (papers.isEmpty()) {
            return;
        }
        sb.append("**").append(heading).append(":** ");
        for (int i = 0; i < papers.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(formatCite(papers.get(i)));
        }
        sb.append("\n\n");
    }

    private static String formatCite(ConvectionLiterature.Paper p) {
        String label = p.authors() + " (" + p.year() + ")";
        if (!p.arxivId().isBlank()) {
            return "[" + label + "](https://arxiv.org/abs/" + p.arxivId() + ")";
        }
        if (!p.doi().isBlank()) {
            return "[" + label + "](https://doi.org/" + p.doi() + ")";
        }
        if (!p.url().isBlank()) {
            return "[" + label + "](" + p.url() + ")";
        }
        return label;
    }

    private static void appendEnergyPolicy(StringBuilder sb, ConvectionLiterature literature) {
        if (literature.energyPolicy.isBlank() && literature.nuclearComparisonsNote.isBlank()) {
            return;
        }
        sb.append("### Energy assumptions — no nuclear inputs\n\n");
        if (!literature.energyPolicy.isBlank()) {
            sb.append(literature.energyPolicy.trim()).append("\n\n");
        }
        if (!literature.nuclearComparisonsNote.isBlank()) {
            sb.append(literature.nuclearComparisonsNote.trim()).append("\n\n");
        }
    }

    private static void appendChartInterpretations(
            StringBuilder sb, ConvectionResultsSummary summary, ConvectionSweepPoint ref
    ) {
        sb.append("### Chart interpretations\n\n");

        List<ConvectionSweepPoint> heights = summary.bySweep("chimney_height");
        if (heights.size() >= 2) {
            ConvectionSweepPoint lo = heights.get(0);
            ConvectionSweepPoint hi = heights.get(heights.size() - 1);
            sb.append(String.format(Locale.US,
                    "**Airflow vs. chimney height** — Draft scales with stack height per buoyancy theory "
                            + "(Zandian & Ashjaee, 2013). Sweep spans **%.0f–%.0f m** → **%.0f–%.0f m³/s**, "
                            + "confirming that taller warm columns materially increase passive throughput — "
                            + "the same lever cooling towers use, repurposed for CO₂ contact.\n\n",
                    lo.chimneyHeightM(), hi.chimneyHeightM(), lo.airflowM3S(), hi.airflowM3S()));
        }

        List<ConvectionSweepPoint> areas = summary.bySweep("contactor_area");
        if (areas.size() >= 2) {
            ConvectionSweepPoint lo = areas.get(0);
            ConvectionSweepPoint hi = areas.get(areas.size() - 1);
            sb.append(String.format(Locale.US,
                    "**CO₂ vs. contactor area** — Capture rate tracks sorbent face area until mass-transfer or "
                            + "regeneration duty binds. **%,.0f–%,.0f m²** spans **%.0f–%.0f t/yr** gross — "
                            + "linear at this lumped fidelity, but still **orders of magnitude** below "
                            + "Mt-scale DAC literature (Keith 2018; McQueen 2021).\n\n",
                    lo.contactorAreaM2(), hi.contactorAreaM2(), lo.grossCo2TonnesYr(), hi.grossCo2TonnesYr()));
        }

        List<ConvectionSweepPoint> fractions = summary.bySweep("waste_fraction");
        if (fractions.size() >= 2) {
            ConvectionSweepPoint lo = fractions.get(0);
            ConvectionSweepPoint hi = fractions.get(fractions.size() - 1);
            sb.append(String.format(Locale.US,
                    "**Fan savings vs. waste-heat coupling** — Fraction of hall heat routed to the air chimney "
                            + "(**%.0f–%.0f%%** of **%.0f MW**) drives ΔT and draft. Fan duty falls from "
                            + "**%.2f to %.2f MW** — modest in absolute terms because baseline fan power is small "
                            + "at this contactor resistance, but directionally consistent with passive-air DAC concepts "
                            + "(Lackner 2018).\n\n",
                    lo.wasteHeatToAirFraction() * 100, hi.wasteHeatToAirFraction() * 100,
                    ref != null ? ref.avgWasteHeatMw() : lo.avgWasteHeatMw(),
                    lo.fanSavedMw(), hi.fanSavedMw()));
        }
    }

    private static void appendConclusion(
            StringBuilder sb,
            ConvectionResultsSummary summary,
            ConvectionSweepPoint ref,
            ConvectionLiterature literature
    ) {
        if (ref == null) {
            return;
        }

        double keithScaleRatio = ref.grossCo2TonnesYr() > 0 ? 900_000.0 / ref.grossCo2TonnesYr() : 0;
        double netPenaltyPct = ref.grossCo2TonnesYr() > 0
                ? 100.0 * (1.0 - ref.netCo2TonnesYr() / ref.grossCo2TonnesYr())
                : 0.0;

        List<ConvectionSweepPoint> heights = summary.bySweep("chimney_height");
        double maxAirflow = heights.stream().mapToDouble(ConvectionSweepPoint::airflowM3S).max().orElse(ref.airflowM3S());
        double minAirflow = heights.stream().mapToDouble(ConvectionSweepPoint::airflowM3S).min().orElse(ref.airflowM3S());

        sb.append("### Conclusion — synthesis and research positioning\n\n");
        sb.append(String.format(Locale.US,
                "> **Verdict:** Chimney convection is a **credible physics sketch**, not a deployment plan. "
                        + "At reference scale (**%.0f m** stack, **%,.0f m²** contactors, **%.0f MW** waste heat) "
                        + "passive draft moves **%.0f m³/s** (sweep range **%.0f–%.0f m³/s**), saves **%.2f MW** "
                        + "fan duty, and yields **%.0f t/yr** net CO₂ under grid scenario — roughly **%.0f× below** "
                        + "a 0.9 Mt/yr Keith-class plant, but aligned with the **side-gig philosophy**: use exhaust "
                        + "already on site before building new energy infrastructure.\n\n",
                ref.chimneyHeightM(), ref.contactorAreaM2(), ref.avgWasteHeatMw(),
                ref.airflowM3S(), minAirflow, maxAirflow, ref.fanSavedMw(),
                ref.netCo2TonnesYr(), keithScaleRatio));

        sb.append("#### Scholarly synthesis\n\n");
        sb.append("This module occupies a **deliberate gap** in the literature: Hamblin et al. (2024) integrate ")
                .append("liquid-loop DC waste heat; Lackner (2018) and Zandian & Ashjaee (2013) motivate passive airflow; ")
                .append("Keith (2018) and McQueen (2021) size Mt-scale fan-driven plants. No peer-reviewed study yet ")
                .append("combines **GPU-campus exhaust**, **chimney draft**, and **cyclic sorbent regeneration** at ")
                .append("hyperscale. Our lumped solver therefore functions as a **feasibility screen**: it asks whether ")
                .append("buoyancy and bed resistance permit useful airflow without claiming commercial LCO₂.\n\n");

        sb.append("#### What the sweep supports\n\n");
        sb.append(String.format(Locale.US,
                "- **Height sensitivity** — taller chimneys increase draft monotonically in our sweep (%.0f–%.0f m³/s)\n",
                minAirflow, maxAirflow));
        sb.append(String.format(Locale.US,
                "- **Contactor area dominates throughput** — **%.0f t/yr** gross at reference **%,.0f m²** face\n",
                ref.grossCo2TonnesYr(), ref.contactorAreaM2()));
        sb.append(String.format(Locale.US,
                "- **Regeneration penalty** — **%.0f%%** gross-to-net gap from heat-pump electricity (5.5 GJ/t duty, "
                        + "grid 0.39 kg/kWh) — consistent with solvent-DAC literature\n",
                netPenaltyPct));
        sb.append(String.format(Locale.US,
                "- **Fan substitution is real but small** — **%.2f MW** saved vs. **%.0f MW** hall heat — "
                        + "Opex lever, not primary climate story\n\n",
                ref.fanSavedMw(), ref.avgWasteHeatMw()));

        sb.append("#### What it does not support\n\n");
        sb.append("- **Campus-scale negative emissions** without orders-of-magnitude larger contactor area or multi-hall aggregation\n");
        sb.append("- **Replacing liquid-loop DAC** pursued by industry (Meta/X) — air-side path remains more speculative\n");
        sb.append("- **Nuclear or dedicated low-carbon heat** — we deliberately use **existing GPU exhaust** only (see energy policy above)\n\n");

        sb.append("#### Relationship to the main side gig\n\n");
        sb.append("The **primary** Data Center Heater Side Gig story routes **liquid** waste heat to DAC and community ")
                .append("loads (~**71 GWh/yr** per hall). Chimney convection is an **air-side appendix**: same regeneration ")
                .append("chemistry, different contactor physics. If passive draft reduces fan Opex and enables larger face ")
                .append("area at fixed electricity, it merits pilot study; if not, the liquid path remains the conservative ")
                .append("baseline (Shi et al., 2023; Hamblin et al., 2024).\n\n");

        sb.append("#### Closing synthesis\n\n");
        sb.append(String.format(Locale.US,
                "Treat this module as **structured speculation**: physics plausible, economics unproven, scale pre-commercial. "
                        + "It strengthens the broader thesis that hyperscale exhaust is **too large to ignore** — whether "
                        + "captured through pipes or chimneys — while keeping climate claims **proportionate** to **%.0f t/yr** "
                        + "net removal at reference geometry, not Mt-scale promises from the DAC literature.\n\n",
                ref.netCo2TonnesYr()));
    }

    private static void appendHonestLimits(StringBuilder sb, ConvectionAnalogies analogies) {
        sb.append("### Honest limits\n\n");
        for (String limit : analogies.honestLimits) {
            sb.append("- ").append(limit).append("\n");
        }
        sb.append("- ").append(analogies.disclaimer).append("\n\n");
    }

    private static String fmt(double v) {
        if (Math.abs(v) >= 100) return String.format(Locale.US, "%.0f", v);
        if (Math.abs(v) >= 10) return String.format(Locale.US, "%.1f", v);
        return String.format(Locale.US, "%.2f", v);
    }
}
