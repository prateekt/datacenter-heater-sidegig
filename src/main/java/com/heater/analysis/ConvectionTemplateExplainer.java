package com.heater.analysis;

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
        appendLiteratureComparisons(sb, literature, ref);
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

        sb.append("| Paper | What they report | How our sim compares |\n");
        sb.append("|-------|------------------|----------------------|\n");
        for (ConvectionLiterature.Paper p : literature.papers) {
            String cite = p.authors() + " (" + p.year() + ")";
            if (!p.doi().isBlank()) {
                cite = "[" + cite + "](https://doi.org/" + p.doi() + ")";
            } else if (!p.url().isBlank()) {
                cite = "[" + cite + "](" + p.url() + ")";
            }
            sb.append("| ").append(cite).append(" | ").append(p.keyNumbers()).append(" | ")
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
            if (!p.doi().isBlank()) {
                sb.append(" https://doi.org/").append(p.doi());
            } else if (!p.url().isBlank()) {
                sb.append(" ").append(p.url());
            }
            sb.append("\n");
        }
        sb.append("\n");
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
