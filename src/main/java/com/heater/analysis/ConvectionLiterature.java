package com.heater.analysis;

import com.heater.config.ConfigLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class ConvectionLiterature {

    public final String comparisonIntro;
    public final String energyPolicy;
    public final String nuclearComparisonsNote;
    public final List<Paper> papers;
    public final List<ConvectionAnalogies.QuestionAnswer> plainEnglishGaps;

    public record Paper(
            String id,
            String authors,
            int year,
            String title,
            String venue,
            String arxivId,
            String doi,
            String url,
            String category,
            String keyNumbers,
            String vsSim
    ) {}

    private ConvectionLiterature(
            String comparisonIntro,
            String energyPolicy,
            String nuclearComparisonsNote,
            List<Paper> papers,
            List<ConvectionAnalogies.QuestionAnswer> plainEnglishGaps
    ) {
        this.comparisonIntro = comparisonIntro;
        this.energyPolicy = energyPolicy;
        this.nuclearComparisonsNote = nuclearComparisonsNote;
        this.papers = papers;
        this.plainEnglishGaps = plainEnglishGaps;
    }

    public static ConvectionLiterature load(String path) throws IOException {
        Map<String, Object> root = ConfigLoader.load(path);
        String intro = String.valueOf(root.getOrDefault("comparison_intro", "")).trim();
        String energyPolicy = String.valueOf(root.getOrDefault("energy_policy", "")).trim();
        String nuclearNote = String.valueOf(root.getOrDefault("nuclear_comparisons_note", "")).trim();

        List<Paper> papers = new ArrayList<>();
        Object papersObj = root.get("papers");
        if (papersObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    papers.add(new Paper(
                            str(m, "id"),
                            str(m, "authors"),
                            intVal(m, "year"),
                            str(m, "title"),
                            str(m, "venue"),
                            str(m, "arxiv_id"),
                            str(m, "doi"),
                            str(m, "url"),
                            str(m, "category"),
                            str(m, "key_numbers"),
                            str(m, "vs_sim")
                    ));
                }
            }
        }

        List<ConvectionAnalogies.QuestionAnswer> gaps = new ArrayList<>();
        Object gapsObj = root.get("plain_english_gaps");
        if (gapsObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    gaps.add(new ConvectionAnalogies.QuestionAnswer(
                            str(m, "q"),
                            str(m, "a")
                    ));
                }
            }
        }

        return new ConvectionLiterature(intro, energyPolicy, nuclearNote, papers, gaps);
    }

    public List<Paper> byCategory(String category) {
        return papers.stream().filter(p -> category.equals(p.category())).toList();
    }

    private static String str(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString();
    }

    private static int intVal(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : 0;
    }
}
