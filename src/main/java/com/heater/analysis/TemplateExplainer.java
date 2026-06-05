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

        sb.append("## Thermal results: hyperscale waste-heat potential\n\n");
        sb.append("*Auto-generated **output-side** results for **Data Center Heater Side Gig** — ")
                .append("how much heat hyperscale AI halls produce, what temperatures are available, ")
                .append("and which downstream processes can use it before dissipation. ")
                .append("Grid-dependent carbon accounting is in the [appendix](#appendix-grid-dependent-carbon-scenario).*\n\n");

        appendThesis(sb);
        appendExecutiveSummary(sb, summary, scale, registry);
        appendHowToReadOutputs(sb);
        appendThermalEnvelope(sb, summary, registry);
        appendThermalChartSection(sb, summary, "gpu_count_ramp", "thermal_service_vs_gpu_count.png",
                "Chart 1 — Thermal service scales with GPU count",
                "Proportional plant growth",
                "Each doubling of GPUs (with scaled plant) roughly doubles **GWh/yr delivered** until equipment limits bind.");
        appendThermalChartSection(sb, summary, "gpu_generation", "thermal_by_generation.png",
                "Chart 2 — Hotter generations, same hall",
                "Blackwell → Rubin thermal envelope",
                "Same 25,000-GPU hall delivers more **GWh/yr** as chip TDP rises.");
        appendThermalChartSection(sb, summary, "saturation", "thermal_saturation_gpu.png",
                "Chart 3 — Thermal saturation at fixed plant",
                "Oversized heat, fixed downstream plant",
                "Pasting more GPUs onto a hall **without** scaling capture plant hits a **thermal service plateau**.");
        appendThermalChartSection(sb, summary, "multi_hall", "thermal_multi_hall.png",
                "Chart 4 — Multi-hall campus rollout",
                "NVIDIA-scale campus expansion",
                "Ten halls ≈ 250k GPUs — cumulative **GWh/yr** scales linearly when each hall is provisioned.");
        appendThermalLoadSplitSection(sb, summary);
        appendGpuTimelineSection(sb);

        appendHeatApplicationsSection(sb, summary);

        sb.append("### Results at a glance\n\n");
        sb.append("| Scenario | GPUs | Chip | Halls | **Thermal (GWh/yr)** | Net CO₂e (t/yr, grid scenario) |\n");
        sb.append("|----------|------|------|-------|----------------------|-------------------------------|\n");
        appendResultRow(sb, summary, "gpu_count_ramp", 5000, null, "AI lab");
        appendResultRow(sb, summary, "gpu_count_ramp", 25000, null, "One hall (H100)");
        appendResultRow(sb, summary, "gpu_generation", 25000, "B200_LC", "One hall (B200)");
        appendResultRow(sb, summary, "multi_hall", 10, "B200_LC", "10-hall campus");
        appendForecastRow(sb, summary, "2026", "Rubin hall");

        sb.append("\n### Scenario narratives\n\n");
        appendThermalNarrative(sb, summary, "Lab footprint (~5k H100)", "gpu_count_ramp", 5000, null);
        appendThermalNarrative(sb, summary, "Single Colossus-class hall (25k B200)", "gpu_generation", 25000, "B200_LC");
        appendThermalNarrative(sb, summary, "Regional campus (10 halls)", "multi_hall", 10, null);
        appendThermalNarrative(sb, summary, "Rubin-era hall (forecast)", "forecast_timeline", 2026, null);

        appendConclusionSection(sb, summary, scale, registry);
        appendGridScenarioAppendix(sb, summary, scale, registry);

        sb.append("### FAQ\n\n");
        sb.append("**Why lead with GWh, not CO₂?** Waste heat is a **physical output** of compute — it exists whether the grid is coal, gas, solar, nuclear, or geothermal. GWh and temperature grades are grid-agnostic.\n\n");
        sb.append("**When does grid carbon matter?** When you ask whether DAC **net-removes** CO₂ after heat-pump electricity — see the [grid appendix](#appendix-grid-dependent-carbon-scenario).\n\n");
        sb.append("**Pools, fisheries, showers vs. DAC?** Same exhaust, different router priority — a **policy choice** about where to send thermal service before dissipation.\n\n");

        sb.append("### Generated at: ").append(summary.generatedAt()).append("\n\n");
        sb.append("### Sources\n\n");
        sb.append("- Hall sizing: ServeTheHome xAI Colossus; Introl B200; SemiAnalysis NVL72\n");
        sb.append("- Thermal grades: `config/thermal_grades.yaml`; heat analogies: `config/heat_applications.yaml`\n");
        sb.append("- Grid scenario: U.S. grid 0.39 kg CO₂/kWh, PUE 1.15 (`config/nvidia_us_expansion.yaml`)\n");
        sb.append("- Forecast SKUs: public GTC roadmaps — not NVIDIA confidential data\n");

        return sb.toString();
    }

    private static void appendThesis(StringBuilder sb) {
        sb.append("### Thesis\n\n");
        sb.append("> We do not assume clean electricity. We quantify the **output-side thermodynamic potential** ")
                .append("of hyperscale AI data centers: how much heat is produced, what temperatures are available, ")
                .append("and which downstream processes can use it before it is dissipated.\n\n");
    }

    private static void appendHowToReadOutputs(StringBuilder sb) {
        sb.append("### How to read the outputs\n\n");
        sb.append("| Output | Use for |\n|--------|--------|\n");
        sb.append("| **MW waste heat** | Continuous thermal exhaust from GPU operations (grid-agnostic) |\n");
        sb.append("| **GWh/yr thermal service** | Heat actually delivered to downstream loads before rejection |\n");
        sb.append("| **Temperature grades** | Which processes can physically use the available heat |\n");
        sb.append("| **MWh by load** | DAC, algae, pools, fisheries, showers (translation metrics) |\n");
        sb.append("| **Tonnes CO₂e/yr** | *Grid scenario only* — see appendix |\n\n");
    }

    private static void appendThermalEnvelope(
            StringBuilder sb, ResultsSummary summary, GpuProfile.GpuProfileRegistry registry
    ) {
        SweepPoint ref = findByProfile(summary, "gpu_generation", registry.referenceProfileId());
        sb.append("### Reference hall — thermal envelope\n\n");
        sb.append(String.format(Locale.US,
                "**%,d %s GPUs** · **~%.0f MW** average waste heat · U.S. Southwest · 7-day sim, annualized\n\n",
                registry.referenceGpuCount(),
                registry.referenceProfile().displayName(),
                registry.referenceProfile().avgWasteHeatMw(registry.referenceGpuCount())));
        if (ref != null) {
            ThermalReport t = ref.thermal();
            sb.append("| Output | Reference hall |\n|--------|----------------|\n");
            sb.append(String.format(Locale.US,
                    "| Waste heat | **%.0f MW** (%.0f GWh/yr input) |\n",
                    ref.avgWasteHeatMw(), t.wasteHeatAnnualGwh()));
            sb.append(String.format(Locale.US,
                    "| Thermal service delivered | **%.1f GWh/yr** |\n", t.annualizedRecoveredGwh()));
            sb.append(String.format(Locale.US,
                    "| Rejected to ambient | **%.1f GWh/yr** |\n", t.rejectedMwh() / 1000.0));
            sb.append(String.format(Locale.US,
                    "| Mean buffer temp | **%.1f °C** |\n", t.meanBufferTempC()));
            sb.append(String.format(Locale.US,
                    "| Mean GPU loop out | **%.1f °C** |\n", t.meanPrimaryTOutC()));
            sb.append(String.format(Locale.US,
                    "| Mean algae pond | **%.1f °C** |\n\n", t.meanAlgaeTempC()));
        }
        sb.append("**Temperature grades** (see `config/thermal_grades.yaml`): GPU loop 40–65°C · buffer 35–55°C · ")
                .append("DAC regeneration ~90°C (heat pump) · algae 25–30°C · aquaculture ~22°C · showers ~42°C.\n\n");
        sb.append("| Source | Finding |\n|--------|--------|\n");
        sb.append("| [ServeTheHome / Supermicro](https://www.servethehome.com/inside-100000-nvidia-gpu-xai-colossus-cluster-supermicro-helped-build-for-elon-musk/) | **~25,000 GPUs per compute hall** |\n");
        sb.append("| [Introl B200 guide](https://introl.com/blog/nvidia-b200-vs-gb200-deployment-guide) | **~160–224 GPUs/MW** (B200 HGX) |\n\n");
    }

    private static void appendExecutiveSummary(
            StringBuilder sb, ResultsSummary summary, ClimateAnalogies scale,
            GpuProfile.GpuProfileRegistry registry
    ) {
        SweepPoint b200 = findByProfile(summary, "gpu_generation", "B200_LC");

        sb.append("### Executive summary\n\n");
        if (b200 != null) {
            ThermalReport t = b200.thermal();
            sb.append(String.format(Locale.US,
                    "> **TL;DR** — One Colossus-class hall throws off **~%.0f MW** of waste heat and delivers "
                            + "**~%.0f GWh/yr** of usable thermal service before rejection — enough for "
                            + "**~%.0f million shelter hot showers/yr** or colocated DAC/algae loads.\n\n",
                    b200.avgWasteHeatMw(), t.annualizedRecoveredGwh(),
                    showerMillions(summary)));
        } else {
            sb.append("> **TL;DR** — Hyperscale AI halls produce continuous waste heat that can be routed to ")
                    .append("downstream loads before dissipation.\n\n");
        }

        sb.append("#### The question\n\n");
        sb.append("- Hyperscalers are building **~25,000-GPU liquid-cooled halls** (documented at xAI Colossus)\n");
        sb.append(String.format(Locale.US,
                "- Each hall runs **~%.0f MW of waste heat** 24/7 — usually dumped to ambient\n",
                registry.referenceProfile().avgWasteHeatMw(registry.referenceGpuCount())));
        sb.append("- **What downstream processes** can use that exhaust **before it is wasted**?\n\n");

        if (b200 != null) {
            ThermalReport t = b200.thermal();
            sb.append("#### The answer — reference hall (25k B200)\n\n");
            sb.append(String.format(Locale.US,
                    "| | |\n|---|---|\n"
                            + "| **Waste heat** | **%.0f MW** continuous exhaust |\n"
                            + "| **Thermal service** | **%.1f GWh/yr** delivered to loads |\n"
                            + "| **Load split** | DAC **%.0f** · algae **%.0f** · rejected **%.0f GWh/yr** |\n"
                            + "| **Mean delivery temp** | Buffer **%.1f°C** · GPU loop **%.1f°C** |\n\n",
                    b200.avgWasteHeatMw(), t.annualizedRecoveredGwh(),
                    t.dacMwh() / 1000.0, t.algaeMwh() / 1000.0, t.rejectedMwh() / 1000.0,
                    t.meanBufferTempC(), t.meanPrimaryTOutC()));

            List<HeatApplicationPoint> apps = summary.applications();
            HeatApplicationPoint dacApp = apps.stream()
                    .filter(a -> "dac_priority".equals(a.scenarioId())).findFirst().orElse(null);
            if (dacApp != null) {
                sb.append("#### Downstream equivalents\n\n");
                sb.append("- ").append(HeatApplicationAnalyzer.formatHotShowers(dacApp.hotShowersEquivalent())).append("\n");
                sb.append(String.format(Locale.US,
                        "- **~%,.0f homes**-worth of annual heat · details in [Secondary heat applications](#secondary-heat-applications)\n\n",
                        dacApp.homesHeatedEquivalent()));
            }

            sb.append("#### Grid scenario footnote\n\n");
            sb.append(String.format(Locale.US,
                    "If this heat powers DAC on today's U.S. grid: **~%,.0f tonnes CO₂e/yr** net removed — "
                            + "see [appendix](#appendix-grid-dependent-carbon-scenario).\n\n",
                    b200.annualizedNetTonnes()));
        }
    }

    private static double showerMillions(ResultsSummary summary) {
        return summary.applications().stream()
                .filter(a -> "dac_priority".equals(a.scenarioId()))
                .findFirst()
                .map(a -> a.hotShowersEquivalent() / 1_000_000.0)
                .orElse(0.0);
    }

    private static void appendThermalChartSection(
            StringBuilder sb, ResultsSummary summary,
            String sweepId, String chartFile, String title, String subtitle, String lesson
    ) {
        sb.append("### ").append(title).append("\n\n");
        sb.append("*").append(subtitle).append("*\n\n");
        sb.append("![").append(title).append("](docs/figures/").append(chartFile).append(")\n\n");
        sb.append("*Y-axis: thermal service delivered (GWh/yr annualized from simulation)*\n\n");
        sb.append("**Read:** ").append(lesson).append("\n\n");
        List<SweepPoint> pts = summary.bySweep(sweepId);
        if (!pts.isEmpty()) {
            SweepPoint h = pts.get(Math.min(pts.size() / 2, pts.size() - 1));
            sb.append(String.format(Locale.US,
                    "**Highlighted point:** %s → **%.1f GWh/yr** thermal service at **%.0f MW** waste heat.\n\n",
                    h.label(), h.thermal().annualizedRecoveredGwh(), h.avgWasteHeatMw()));
        }
    }

    private static void appendThermalLoadSplitSection(StringBuilder sb, ResultsSummary summary) {
        sb.append("### Chart 5 — Thermal load split (reference hall)\n\n");
        sb.append("![Thermal load split](docs/figures/thermal_load_split.png)\n\n");
        sb.append("*Stacked annual thermal service by downstream load (DAC priority routing).*\n\n");
    }

    private static void appendCo2ChartSection(
            StringBuilder sb, ResultsSummary summary, ClimateAnalogies scale,
            String sweepId, String chartFile, String title, String subtitle, String lesson
    ) {
        sb.append("#### ").append(title).append("\n\n");
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

    private static void appendGridScenarioAppendix(
            StringBuilder sb, ResultsSummary summary, ClimateAnalogies scale,
            GpuProfile.GpuProfileRegistry registry
    ) throws IOException {
        sb.append("<a id=\"appendix-grid-dependent-carbon-scenario\"></a>\n\n");
        sb.append("## Appendix: Grid-dependent carbon scenario\n\n");
        sb.append("> **Assumption:** U.S. grid **0.39 kg CO₂/kWh**, facility **PUE 1.15**. ")
                .append("This layer answers *net climate impact* — not whether waste heat exists.\n\n");

        appendWorthItSection(sb, summary, scale, registry);

        appendCo2ChartSection(sb, summary, scale, "gpu_count_ramp", "co2_vs_gpu_count.png",
                "CO₂ vs. GPU count", "Grid scenario", "Net tonnes scale with GPUs when plant scales.");
        appendCo2ChartSection(sb, summary, scale, "gpu_generation", "co2_vs_gpu_generation.png",
                "CO₂ vs. GPU generation", "Grid scenario", "Hotter chips → more net removal at same hall size.");
        appendCo2ChartSection(sb, summary, scale, "saturation", "co2_saturation_gpu.png",
                "CO₂ saturation", "Grid scenario", "Fixed DAC plant → CO₂ plateau as heat rises.");
        appendCo2ChartSection(sb, summary, scale, "multi_hall", "co2_multi_hall.png",
                "CO₂ multi-hall", "Grid scenario", "Campus-scale cumulative net removal.");
        appendGrossNetSection(sb, summary, scale);
        sb.append(scale.electrificationNote()).append("\n\n");
    }

    private static void appendGrossNetSection(StringBuilder sb, ResultsSummary summary, ClimateAnalogies scale) {
        sb.append("#### Gross vs. net CO₂ (heat-pump grid penalty)\n\n");
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
        sb.append("**Input-side thermal envelope** — watts per GPU to the coolant loop (TDP + rack overhead). ")
                .append("† = public roadmap forecast. Drives output GWh regardless of grid mix.\n\n");
    }

    private static void appendWorthItSection(
            StringBuilder sb, ResultsSummary summary, ClimateAnalogies scale,
            GpuProfile.GpuProfileRegistry registry
    ) throws IOException {
        OperationalCarbon ops = OperationalCarbon.fromConfig();
        sb.append("### Operational CO₂ recovery (grid scenario)\n\n");
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

    private static void appendResultRow(StringBuilder sb, ResultsSummary summary,
            String sweepId, int key, String profileId, String label) {
        SweepPoint p = resolvePoint(summary, sweepId, key, profileId);
        if (p == null) return;
        sb.append(String.format(Locale.US,
                "| %s | %,d | %s | %d | **%.1f** | %,.0f (grid scenario) |\n",
                label, p.gpuCount(), p.profileName(), p.halls(),
                p.thermal().annualizedRecoveredGwh(), p.annualizedNetTonnes()));
    }

    private static void appendForecastRow(StringBuilder sb, ResultsSummary summary,
            String year, String label) {
        SweepPoint p = summary.bySweep("forecast_timeline").stream()
                .filter(pt -> pt.label().startsWith(year)).findFirst().orElse(null);
        if (p == null) return;
        sb.append(String.format(Locale.US,
                "| %s | %,d | %s | %d | **%.1f** | %,.0f (grid scenario) |\n",
                label, p.gpuCount(), p.profileName(), p.halls(),
                p.thermal().annualizedRecoveredGwh(), p.annualizedNetTonnes()));
    }

    private static void appendThermalNarrative(StringBuilder sb, ResultsSummary summary,
            String title, String sweepId, int key, String profileId) {
        SweepPoint p = resolvePoint(summary, sweepId, key, profileId);
        if (p == null) return;
        ThermalReport t = p.thermal();
        sb.append(String.format(Locale.US,
                "**%s** — **%.1f GWh/yr** thermal service at **%.0f MW** waste heat "
                        + "(DAC **%.0f** · algae **%.0f** · rejected **%.0f GWh/yr**). "
                        + "Grid scenario: **%,.0f tonnes CO₂e/yr** net removed.\n\n",
                title, t.annualizedRecoveredGwh(), p.avgWasteHeatMw(),
                t.dacMwh() / 1000.0, t.algaeMwh() / 1000.0, t.rejectedMwh() / 1000.0,
                p.annualizedNetTonnes()));
    }

    private static SweepPoint resolvePoint(ResultsSummary summary, String sweepId, int key, String profileId) {
        if (profileId != null) return findByProfile(summary, sweepId, profileId);
        if (key >= 2020) {
            return summary.bySweep(sweepId).stream()
                    .filter(pt -> pt.label().startsWith(String.valueOf(key))).findFirst().orElse(null);
        }
        if ("multi_hall".equals(sweepId)) {
            return summary.bySweep(sweepId).stream().filter(pt -> pt.halls() == key).findFirst().orElse(null);
        }
        return findPoint(summary, sweepId, key);
    }

    private static SweepPoint findByProfile(ResultsSummary summary, String sweepId, String profileId) {
        return summary.bySweep(sweepId).stream()
                .filter(p -> p.profileId().equals(profileId)).findFirst().orElse(null);
    }

    private static SweepPoint findPoint(ResultsSummary summary, String sweepId, int key) {
        return summary.bySweep(sweepId).stream()
                .filter(p -> p.gpuCount() == key || p.halls() == key).findFirst().orElse(null);
    }

    private static void appendConclusionSection(
            StringBuilder sb, ResultsSummary summary, ClimateAnalogies scale,
            GpuProfile.GpuProfileRegistry registry
    ) {
        SweepPoint b200 = findByProfile(summary, "gpu_generation", "B200_LC");
        if (b200 == null) return;

        ThermalReport t = b200.thermal();
        SweepPoint campus10 = summary.bySweep("multi_hall").stream()
                .filter(p -> p.halls() == 10).findFirst().orElse(null);
        SweepPoint h100 = findByProfile(summary, "gpu_generation", "H100_SXM");
        HeatApplicationPoint dacApp = summary.applications().stream()
                .filter(a -> "dac_priority".equals(a.scenarioId())).findFirst().orElse(null);

        double thermalSaturationPct = thermalSaturationUpliftPercent(summary);
        double thermalUpliftPct = h100 != null && h100.thermal().annualizedRecoveredGwh() > 0
                ? 100.0 * (b200.thermal().annualizedRecoveredGwh() - h100.thermal().annualizedRecoveredGwh())
                        / h100.thermal().annualizedRecoveredGwh()
                : 0.0;

        sb.append("### Conclusion — significance, limits, and what's worth it\n\n");
        sb.append(String.format(Locale.US,
                "> **Verdict:** Routing hyperscale exhaust before dissipation is **worth doing** — "
                        + "**~%.0f MW** and **~%.0f GWh/yr** of deliverable thermal service per Colossus-class hall, "
                        + "regardless of whether the grid is coal, gas, solar, nuclear, or geothermal.\n\n",
                b200.avgWasteHeatMw(), t.annualizedRecoveredGwh()));

        sb.append("#### What is significant\n\n");
        sb.append(String.format(Locale.US,
                "- **%.0f MW** continuous waste heat — a physical output of compute, not a grid assumption\n",
                b200.avgWasteHeatMw()));
        sb.append(String.format(Locale.US,
                "- **%.1f GWh/yr** thermal service delivered from one hall — DAC, algae, or community loads\n",
                t.annualizedRecoveredGwh()));
        if (campus10 != null) {
            sb.append(String.format(Locale.US,
                    "- **%.0f GWh/yr at 10 halls** — campus-scale thermal budget for colocated industry\n",
                    campus10.thermal().annualizedRecoveredGwh()));
        }
        if (thermalUpliftPct > 1) {
            sb.append(String.format(Locale.US,
                    "- **+%.0f%% thermal service** H100 → B200 at same 25k footprint — hotter silicon = more output GWh\n",
                    thermalUpliftPct));
        }
        if (thermalSaturationPct < 5) {
            sb.append("- **Plant saturation is real** — past ~1.3× heat, GWh delivered barely moves without scaling downstream plant\n");
        }
        if (dacApp != null) {
            sb.append("- **").append(HeatApplicationAnalyzer.formatHotShowers(dacApp.hotShowersEquivalent()))
                    .append("** from the same exhaust — enormous community heat potential\n");
        }
        sb.append("\n");

        sb.append("#### What is not significant\n\n");
        sb.append("- **Debating grid cleanliness to prove heat exists** — the exhaust is there either way\n");
        sb.append("- **National climate salvation from one hall** — see grid appendix for tonne-scale limits\n");
        sb.append("- **Assuming more GPUs automatically add service** — without proportional plant, **GWh plateaus** (Chart 3)\n");
        sb.append("- **Treating CO₂ charts as the primary output** — they are a **labeled grid scenario**, not thermodynamics\n\n");

        sb.append("#### What's worth it? — decision guide\n\n");
        sb.append("| If your goal is… | Worth it? | Simulation says… |\n");
        sb.append("|------------------|-----------|------------------|\n");
        sb.append(String.format(Locale.US,
                "| Use waste heat before dumping to ambient | **Yes** | **%.1f GWh/yr** deliverable per hall |\n",
                t.annualizedRecoveredGwh()));
        if (dacApp != null) {
            sb.append(String.format(Locale.US,
                    "| Shelter showers / community heat near campus | **Yes — trade-off** | **%s** possible; routing choice sets DAC vs. showers |\n",
                    HeatApplicationAnalyzer.formatHotShowers(dacApp.hotShowersEquivalent())));
        }
        sb.append("| Size Blackwell / Rubin halls with matched downstream plant | **Yes** | Hotter generations raise **GWh/hall** when plant scales |\n");
        sb.append("| Prove the hall is carbon-neutral | **No** | Grid scenario still shows net emitter — see appendix |\n");
        sb.append("| Replace national mitigation strategy | **No** | Output story is **per-campus thermodynamics**, not U.S. inventory |\n\n");

        sb.append("#### Bottom line\n\n");
        sb.append("**Significant:** tens of MW and tens of GWh/yr of routable exhaust, temperature grades for real downstream loads, ")
                .append("and clear saturation lessons for plant sizing. ");
        sb.append("**Not significant:** grid mix as a prerequisite, national CO₂ %, or carbon-neutral claims. ");
        sb.append("**Worth it?** **Yes** to extract value from unavoidable exhaust; climate tonnes are a **separate question** under explicit grid assumptions.\n\n");
    }

    private static double thermalSaturationUpliftPercent(ResultsSummary summary) {
        List<SweepPoint> pts = summary.bySweep("saturation");
        if (pts.size() < 2) return 0.0;
        double base = pts.stream().filter(p -> p.label().contains("1.0x")).findFirst()
                .map(p -> p.thermal().annualizedRecoveredGwh()).orElse(pts.get(0).thermal().annualizedRecoveredGwh());
        double max = pts.stream().mapToDouble(p -> p.thermal().annualizedRecoveredGwh()).max().orElse(base);
        if (base <= 0) return 0.0;
        return 100.0 * (max - base) / base;
    }

    private static void appendHeatApplicationsSection(StringBuilder sb, ResultsSummary summary) {
        List<HeatApplicationPoint> apps = summary.applications();
        if (apps.isEmpty()) return;

        sb.append("<a id=\"secondary-heat-applications\"></a>\n\n");
        sb.append("### Secondary heat applications — pools, fisheries, showers, community heat\n\n");
        sb.append("The same **~34 MW** waste-heat stream can be routed to **DAC**, **heated pools**, **aquaculture raceways**, **algae**, or **shelter hot showers** ")
                .append("(MVP: one path at a time). Metrics translate delivered MWh into real-world equivalents ")
                .append("(olympic pool ~180 MWh/yr; shelter hot shower ~2.5 kWh; U.S. home ~8 MWh/yr heat).\n\n");

        sb.append("| Priority scenario | Heat (MWh/yr) | Hot showers/yr | Net CO₂e (t/yr, grid) | Olympic pools | Raceways | Homes equiv. |\n");
        sb.append("|-------------------|---------------|----------------|------------------------|---------------|----------|-------------|\n");
        for (HeatApplicationPoint p : apps) {
            sb.append(String.format(Locale.US,
                    "| %s | **%,.0f** | **%s** | %,.0f | %.1f | %.1f | %,.0f |\n",
                    p.label(), p.heatTotalMwh(),
                    HeatApplicationAnalyzer.formatHotShowersCompact(p.hotShowersEquivalent()),
                    p.netCo2eTonnesPerYear(),
                    p.olympicPoolsEquivalent(), p.aquacultureRacewaysEquivalent(),
                    p.homesHeatedEquivalent()));
        }
        sb.append("\n*Hot showers: dignified **8-min shelter/mobile unit** shower (~60 L warmed to 42°C, ~2.5 kWh each). "
                + "Illustrates community heat potential — not a modeled load in the simulator yet.*\n\n");
        sb.append("\n");

        HeatApplicationPoint dac = apps.stream().filter(a -> "dac_priority".equals(a.scenarioId())).findFirst().orElse(null);
        HeatApplicationPoint community = apps.stream().filter(a -> "community_heat".equals(a.scenarioId())).findFirst().orElse(null);
        if (dac != null && community != null) {
            double co2Trade = dac.netCo2eTonnesPerYear() - community.netCo2eTonnesPerYear();
            sb.append(String.format(Locale.US,
                    "**Trade-off (community vs. DAC priority):** ~%,.0f fewer tonnes CO₂e removed per year, "
                            + "but **%,.0f MWh/yr** to pools/fisheries and **~%,.0f homes** heat equivalent — "
                            + "a campus **amenity + food + district heat** story alongside partial climate clawback.\n\n",
                    co2Trade, community.heatPoolMwh() + community.heatAquacultureMwh(),
                    community.homesHeatedEquivalent()));
        }

        for (HeatApplicationPoint p : apps) {
            sb.append("- ").append(HeatApplicationAnalyzer.formatPoint(p)).append("\n");
        }
        sb.append("\n");
    }
}
