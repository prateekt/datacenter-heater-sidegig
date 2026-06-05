package com.heater.analysis;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class TemplateExplainer {

    private TemplateExplainer() {}

    public static String explain(ResultsSummary summary, String gpuProfilesPath) throws IOException {
        GpuProfile.GpuProfileRegistry registry = GpuProfile.load(gpuProfilesPath);
        ClimateAnalogies scale = ClimateAnalogies.loadDefault();
        StringBuilder sb = new StringBuilder();

        sb.append("## Scalability: GPUs, heat, and CO₂\n\n");
        sb.append("*Auto-generated simulation results for **Data Center Heater Side Gig** — ")
                .append("waste-heat-driven DAC at NVIDIA-scale U.S. AI halls. ")
                .append("Charts use **metric tonnes CO₂e/year**; prose adds scale analogies.*\n\n");

        sb.append("### Executive summary\n\n");
        sb.append("NVIDIA and hyperscale partners are building **~25,000-GPU liquid-cooled halls** ")
                .append("(documented at xAI Colossus). Each hall throws off **tens of MW of waste heat** 24/7. ")
                .append("This simulation asks: if that heat powers **direct air capture (DAC)** colocated on campus, ")
                .append("how much CO₂ comes back out of the atmosphere — and what fraction of the **hall's own GPU electricity emissions** does that recuperate?\n\n");

        SweepPoint b200 = findByProfile(summary, "gpu_generation", "B200_LC");
        if (b200 != null) {
            sb.append("**Reference answer (25k B200 liquid, DAC priority):** ")
                    .append(scale.formatTonnes(b200.annualizedNetTonnes())).append(" net removed; ");
            OperationalCarbon ops = OperationalCarbon.fromConfig();
            OperationalCarbon.RecoveryAnalysis rec = ops.forHall(
                    registry.require("B200_LC"), b200.gpuCount(), registry, b200.annualizedNetTonnes());
            sb.append(String.format(Locale.US, "**%.0f%% recovery** of operational GPU-grid CO₂. ", rec.recoveryPercent()));
            sb.append(scale.scaleNarrative(b200.annualizedNetTonnes())).append("\n\n");
        }

        sb.append("### How to read the metrics\n\n");
        sb.append("| Metric | Use for |\n|--------|--------|\n");
        sb.append("| **Tonnes CO₂e/year** | Engineering, reporting, charts — the primary unit |\n");
        sb.append("| **% operational recovery** | Fair \"worth it?\" test vs. the hall's own GPU grid emissions |\n");
        sb.append("| **Cars / farms / homes** | Intuition only — see electrification note below |\n\n");
        sb.append(scale.electrificationNote()).append("\n\n");

        sb.append("### Reference hall — scale and sources\n\n");
        sb.append(String.format(Locale.US,
                "**%,d %s GPUs** · **~%.0f MW** average waste heat · U.S. Southwest climate · 7-day sim, annualized\n\n",
                registry.referenceGpuCount(),
                registry.referenceProfile().displayName(),
                registry.referenceProfile().avgWasteHeatMw(registry.referenceGpuCount())));
        sb.append("| Source | Finding |\n|--------|--------|\n");
        sb.append("| [ServeTheHome / Supermicro](https://www.servethehome.com/inside-100000-nvidia-gpu-xai-colossus-cluster-supermicro-helped-build-for-elon-musk/) | **~25,000 GPUs per compute hall** (4 halls → 100k H100) |\n");
        sb.append("| [Introl B200 guide](https://introl.com/blog/nvidia-b200-vs-gb200-deployment-guide) | **~160–224 GPUs/MW** (B200 HGX, 8-GPU racks) |\n");
        sb.append("| [SemiAnalysis NVL72](https://semianalysis.substack.com/p/gb200-hardware-architecture-and-component) | **72 GPUs @ ~120 kW/rack** |\n\n");

        appendChartSection(sb, summary, scale, "gpu_count_ramp", "co2_vs_gpu_count.png",
                "Chart 1 — Removal scales with GPU count",
                "Proportional plant growth",
                "Each doubling of GPUs (with scaled DAC) roughly doubles net removal until equipment limits bind.");
        appendChartSection(sb, summary, scale, "gpu_generation", "co2_vs_gpu_generation.png",
                "Chart 2 — Hotter generations, same hall",
                "Blackwell → Rubin thermal envelope",
                "Same 25,000-GPU hall removes more CO₂ as TDP rises — relevant for Blackwell and Vera Rubin planning.");
        appendChartSection(sb, summary, scale, "saturation", "co2_saturation_gpu.png",
                "Chart 3 — Saturation at fixed DAC capacity",
                "Oversized heat, fixed capture plant",
                "Pasting more GPUs onto a hall **without** scaling DAC hits a plateau — capex must match heat.");
        appendChartSection(sb, summary, scale, "multi_hall", "co2_multi_hall.png",
                "Chart 4 — Multi-hall campus rollout",
                "NVIDIA-scale campus expansion",
                "Ten halls ≈ 250k GPUs — where regional climate impact becomes policy-visible.");
        appendGrossNetSection(sb, summary, scale);
        appendGpuTimelineSection(sb);

        appendWorthItSection(sb, summary, scale, registry);

        sb.append("### Results at a glance\n\n");
        sb.append("| Scenario | GPUs | Chip | Halls | **Net CO₂e (t/yr)** | Scale intuition |\n");
        sb.append("|----------|------|------|-------|---------------------|------------------|\n");
        appendResultRow(sb, summary, scale, "gpu_count_ramp", 5000, null, "AI lab");
        appendResultRow(sb, summary, scale, "gpu_count_ramp", 25000, null, "One hall (H100)");
        appendResultRow(sb, summary, scale, "gpu_generation", 25000, "B200_LC", "One hall (B200)");
        appendResultRow(sb, summary, scale, "multi_hall", 10, "B200_LC", "10-hall campus");
        appendForecastRow(sb, summary, scale, "2026", "Rubin hall");

        sb.append("\n### Scenario narratives\n\n");
        appendNarrative(sb, summary, scale, "Lab footprint (~5k H100)", "gpu_count_ramp", 5000, null);
        appendNarrative(sb, summary, scale, "Single Colossus-class hall (25k B200)", "gpu_generation", 25000, "B200_LC");
        appendNarrative(sb, summary, scale, "Regional campus (10 halls)", "multi_hall", 10, null);
        appendNarrative(sb, summary, scale, "Rubin-era hall (forecast)", "forecast_timeline", 2026, null);

        sb.append("### FAQ\n\n");
        sb.append("**Why tonnes on charts, not cars?** Tonnes are the engineering and reporting unit. Cars are a fading proxy as transport electrifies — we keep them in prose with an EV caveat.\n\n");
        sb.append("**What is the right \"worth it?\" metric?** **% operational recovery** — how much of the hall's own GPU-grid CO₂ DAC gives back. Not global cars off the road.\n\n");
        sb.append("**Does a greener grid hurt this story?** GPU **operational** CO₂ drops; **waste heat** does not. DAC becomes *more* valuable per MWh of heat, but heat-pump electricity penalty also shrinks.\n\n");
        sb.append("**Agriculture comparison — serious or gimmick?** USDA cover-crop programs sequester **~0.3–0.8 t CO₂e/acre/year**. Our reference hall matches **~75,000 acres** of high-performing cover crop — real land programs, not a substitute for cutting GPU power.\n\n");
        sb.append("**NVIDIA-specific takeaway:** Blackwell and Rubin halls run hotter → **more DAC potential per hall** if capture plant scales with silicon. Saturation chart shows **DAC capex must track heat**.\n\n");

        sb.append("### Generated at: ").append(summary.generatedAt()).append("\n\n");
        sb.append("### Sources\n\n");
        sb.append("- Hall sizing: ServeTheHome xAI Colossus; Introl B200; SemiAnalysis NVL72\n");
        sb.append("- Analogies: EPA (transport, national inventory), USDA NRCS (cover crops), EIA (homes)\n");
        sb.append("- Forecast SKUs: public GTC roadmaps — not NVIDIA confidential data\n");

        return sb.toString();
    }

    private static void appendChartSection(
            StringBuilder sb, ResultsSummary summary, ClimateAnalogies scale,
            String sweepId, String chartFile, String title, String subtitle, String lesson
    ) {
        sb.append("### ").append(title).append("\n\n");
        sb.append("*").append(subtitle).append("*\n\n");
        sb.append("![").append(title).append("](docs/figures/").append(chartFile).append(")\n\n");
        sb.append("*").append(scale.chartSubtitleTonnes()).append("*\n\n");
        sb.append("**Read:** ").append(lesson).append("\n\n");
        List<SweepPoint> pts = summary.bySweep(sweepId);
        if (!pts.isEmpty()) {
            SweepPoint h = pts.get(Math.min(pts.size() / 2, pts.size() - 1));
            sb.append("**Highlighted point:** ").append(h.label()).append(" → ");
            sb.append(scale.scaleNarrative(h.annualizedNetTonnes())).append("\n\n");
        }
    }

    private static void appendGrossNetSection(StringBuilder sb, ResultsSummary summary, ClimateAnalogies scale) {
        sb.append("### Chart 5 — Gross vs. net (heat-pump electricity penalty)\n\n");
        sb.append("![Gross vs net](docs/figures/gross_vs_net_co2.png)\n\n");
        sb.append("*").append(scale.chartSubtitleTonnes()).append("*\n\n");
        List<SweepPoint> pts = summary.bySweep("gpu_count_ramp");
        if (!pts.isEmpty()) {
            SweepPoint p = pts.get(pts.size() - 1);
            sb.append(String.format(Locale.US,
                    "At **%,d GPUs**: **%,.0f tonnes** gross captured vs **%,.0f tonnes** net — "
                            + "the gap is grid CO₂ from heat-pump electricity (~%.0f%% of gross).\n\n",
                    p.gpuCount(), p.annualizedGrossTonnes(), p.annualizedNetTonnes(),
                    100.0 * (1.0 - p.annualizedNetTonnes() / p.annualizedGrossTonnes())));
        }
    }

    private static void appendGpuTimelineSection(StringBuilder sb) {
        sb.append("### Chart 6 — Waste heat per GPU by generation\n\n");
        sb.append("![GPU waste heat timeline](docs/figures/gpu_tdp_timeline.png)\n\n");
        sb.append("Watts per GPU to the coolant loop (TDP + rack overhead). † = public roadmap forecast.\n\n");
    }

    private static void appendWorthItSection(
            StringBuilder sb, ResultsSummary summary, ClimateAnalogies scale,
            GpuProfile.GpuProfileRegistry registry
    ) throws IOException {
        OperationalCarbon ops = OperationalCarbon.fromConfig();
        sb.append("### Operational CO₂ recovery — the primary \"worth it?\" metric\n\n");
        sb.append("For NVIDIA-scale infrastructure, compare DAC removal to **CO₂ from powering the same GPUs** ")
                .append("(average waste heat × PUE 1.15 × U.S. grid 0.39 kg/kWh). This stays valid as transport electrifies.\n\n");
        sb.append("| Scenario | GPU ops CO₂ (t/yr) | DAC net (t/yr) | **Recovery** | Net balance (t/yr) |\n");
        sb.append("|----------|-------------------|-----------------|--------------|-------------------|\n");

        appendRecoveryRow(sb, summary, ops, registry, "gpu_generation", "B200_LC", "25k B200 reference");
        appendRecoveryRow(sb, summary, ops, registry, "gpu_generation", "H100_SXM", "25k H100");
        appendRecoveryRow(sb, summary, ops, registry, "gpu_count_ramp", 5000, "H100_SXM", "5k H100 lab");
        appendRecoveryRowMulti(sb, summary, ops, registry, 10);

        SweepPoint ref = findByProfile(summary, "gpu_generation", "B200_LC");
        if (ref != null) {
            OperationalCarbon.RecoveryAnalysis r = ops.forHall(
                    registry.require("B200_LC"), ref.gpuCount(), registry, ref.annualizedNetTonnes());
            sb.append("\n**Reference hall:** ").append(ops.explainRecovery(r, scale)).append("\n\n");
            sb.append("**Strategic framing for NVIDIA:** Waste-heat DAC is **colocated carbon clawback** on heat already paid for — ")
                    .append("~one quarter of operational CO₂ today, rising if grid greens and DAC scales with Blackwell/Rubin thermals. ")
                    .append("Not a license to build; a way to **extract value from unavoidable exhaust**.\n\n");
        }
    }

    private static void appendRecoveryRow(StringBuilder sb, ResultsSummary summary, OperationalCarbon ops,
            GpuProfile.GpuProfileRegistry registry, String sweepId, String profileId, String label) throws IOException {
        SweepPoint p = findByProfile(summary, sweepId, profileId);
        if (p == null) return;
        writeRecoveryRow(sb, ops, registry, p, label);
    }

    private static void appendRecoveryRow(StringBuilder sb, ResultsSummary summary, OperationalCarbon ops,
            GpuProfile.GpuProfileRegistry registry, String sweepId, int gpus, String profileId, String label) throws IOException {
        SweepPoint p = summary.bySweep(sweepId).stream()
                .filter(pt -> pt.gpuCount() == gpus && pt.profileId().equals(profileId))
                .findFirst().orElse(null);
        if (p == null) return;
        writeRecoveryRow(sb, ops, registry, p, label);
    }

    private static void appendRecoveryRowMulti(StringBuilder sb, ResultsSummary summary, OperationalCarbon ops,
            GpuProfile.GpuProfileRegistry registry, int halls) throws IOException {
        SweepPoint p = summary.bySweep("multi_hall").stream().filter(pt -> pt.halls() == halls).findFirst().orElse(null);
        if (p == null) return;
        GpuProfile profile = registry.require(p.profileId());
        int totalGpus = p.gpuCount() * p.halls();
        OperationalCarbon.RecoveryAnalysis r = ops.forHall(profile, totalGpus, registry, p.annualizedNetTonnes());
        sb.append(String.format(Locale.US,
                "| %d halls × 25k B200 | %,.0f | %,.0f | **%.0f%%** | %,.0f |\n",
                halls, r.operationalCo2Tonnes(), r.netRemovedTonnes(), r.recoveryPercent(), r.netBalanceTonnes()));
    }

    private static void writeRecoveryRow(StringBuilder sb, OperationalCarbon ops,
            GpuProfile.GpuProfileRegistry registry, SweepPoint p, String label) throws IOException {
        OperationalCarbon.RecoveryAnalysis r = ops.forHall(
                registry.require(p.profileId()), p.gpuCount(), registry, p.annualizedNetTonnes());
        sb.append(String.format(Locale.US,
                "| %s | %,.0f | %,.0f | **%.0f%%** | %,.0f |\n",
                label, r.operationalCo2Tonnes(), r.netRemovedTonnes(), r.recoveryPercent(), r.netBalanceTonnes()));
    }

    private static void appendResultRow(StringBuilder sb, ResultsSummary summary, ClimateAnalogies scale,
            String sweepId, int key, String profileId, String label) {
        SweepPoint p;
        if ("multi_hall".equals(sweepId)) {
            p = summary.bySweep(sweepId).stream().filter(pt -> pt.halls() == key).findFirst().orElse(null);
        } else if (profileId != null) {
            p = findByProfile(summary, sweepId, profileId);
        } else {
            p = findPoint(summary, sweepId, key);
        }
        if (p == null) return;
        double acres = p.annualizedNetTonnes() / 0.5;
        sb.append(String.format(Locale.US,
                "| %s | %,d | %s | %d | **%,.0f** | ~%,.0f acres cover-crop equiv.; %s |\n",
                label, p.gpuCount(), p.profileName(), p.halls(), p.annualizedNetTonnes(), acres,
                scale.formatCars(scale.iceCarsFromTonnes(p.annualizedNetTonnes())) + " gasoline cars (legacy proxy)"));
    }

    private static void appendForecastRow(StringBuilder sb, ResultsSummary summary, ClimateAnalogies scale,
            String year, String label) {
        SweepPoint p = summary.bySweep("forecast_timeline").stream()
                .filter(pt -> pt.label().startsWith(year)).findFirst().orElse(null);
        if (p == null) return;
        double acres = p.annualizedNetTonnes() / 0.5;
        sb.append(String.format(Locale.US,
                "| %s | %,d | %s | %d | **%,.0f** | ~%,.0f acres cover-crop equiv.; %s |\n",
                label, p.gpuCount(), p.profileName(), p.halls(), p.annualizedNetTonnes(), acres,
                scale.formatCars(scale.iceCarsFromTonnes(p.annualizedNetTonnes())) + " gasoline cars (legacy proxy)"));
    }

    private static void appendNarrative(StringBuilder sb, ResultsSummary summary, ClimateAnalogies scale,
            String title, String sweepId, int key, String profileId) {
        SweepPoint p = profileId != null ? findByProfile(summary, sweepId, profileId)
                : key >= 2020 ? summary.bySweep(sweepId).stream().filter(pt -> pt.label().startsWith(String.valueOf(key))).findFirst().orElse(null)
                : findPoint(summary, sweepId, key);
        if (p == null && key == 10) {
            p = summary.bySweep(sweepId).stream().filter(pt -> pt.halls() == 10).findFirst().orElse(null);
        }
        if (p == null) return;
        sb.append("**").append(title).append("** — ").append(scale.scaleNarrative(p.annualizedNetTonnes()));
        sb.append(" *(MVP: single heat path; parallel DAC+algae would raise totals.)*\n\n");
    }

    private static SweepPoint findByProfile(ResultsSummary summary, String sweepId, String profileId) {
        return summary.bySweep(sweepId).stream()
                .filter(p -> p.profileId().equals(profileId)).findFirst().orElse(null);
    }

    private static SweepPoint findPoint(ResultsSummary summary, String sweepId, int key) {
        return summary.bySweep(sweepId).stream()
                .filter(p -> p.gpuCount() == key || p.halls() == key).findFirst().orElse(null);
    }
}
