package com.heater.analysis;

import java.util.ArrayList;
import java.util.List;

public final class ConvectionExplainerValidator {

    private ConvectionExplainerValidator() {}

    public record ValidationResult(boolean ok, List<String> warnings) {}

    public static ValidationResult validate(String markdown, ConvectionResultsSummary summary) {
        List<String> warnings = new ArrayList<>();
        String lower = markdown.toLowerCase();

        if (!markdown.contains("In one sentence")) {
            warnings.add("Missing one-sentence TL;DR");
        }
        if (!markdown.contains("Picture this")) {
            warnings.add("Missing Picture this analogy table");
        }
        if (!markdown.contains("plain English")) {
            warnings.add("Missing plain English results section");
        }
        if (!markdown.contains("published research")) {
            warnings.add("Missing literature comparison section");
        }
        if (!markdown.contains("Conclusion")) {
            warnings.add("Missing conclusion section");
        }
        if (!markdown.toLowerCase().contains("interpretation")) {
            warnings.add("Missing scholarly interpretation section");
        }
        if (!markdown.contains("doi.org") && !markdown.contains("proceedings.aiche.org")
                && !markdown.contains("arxiv.org")) {
            warnings.add("Missing academic citations (DOI, arXiv, or proceedings links)");
        }
        if (!markdown.toLowerCase().contains("no nuclear")) {
            warnings.add("Missing explicit no-nuclear energy policy statement");
        }
        if (!markdown.contains("speculative") && !markdown.contains("experimental")) {
            warnings.add("Missing speculative/experimental disclaimer");
        }

        long analogyRows = markdown.lines()
                .filter(l -> l.startsWith("| ") && !l.contains("Complicated word"))
                .count();
        if (analogyRows < 3) {
            warnings.add("Fewer than 3 analogy rows in Picture this table");
        }

        for (String chart : summary.chartPaths()) {
            if (!markdown.contains(chart)) {
                warnings.add("Missing chart embed: " + chart);
            }
        }

        if (summary.referencePoint() != null) {
            String airflow = String.format("%.0f", summary.referencePoint().airflowM3S());
            if (!markdown.contains(airflow)) {
                warnings.add("Reference airflow not found in markdown");
            }
        }

        if (lower.contains("tbd") || lower.contains("placeholder")) {
            warnings.add("Output contains placeholder text");
        }

        return new ValidationResult(warnings.isEmpty(), warnings);
    }
}
