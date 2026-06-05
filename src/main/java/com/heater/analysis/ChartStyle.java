package com.heater.analysis;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.awt.*;

final class ChartStyle {

    static final Color BG = new Color(255, 255, 255);
    static final Color PLOT = new Color(248, 250, 252);
    static final Color GRID = new Color(226, 232, 240);
    static final Color TEXT = new Color(30, 41, 59);
    static final Color TEAL = new Color(13, 148, 136);
    static final Color GREEN = new Color(22, 163, 74);
    static final Color AMBER = new Color(245, 158, 11);
    static final Color SLATE = new Color(100, 116, 139);
    static final Color INDIGO = new Color(79, 70, 229);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font AXIS_TITLE_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font TICK_FONT = new Font("SansSerif", Font.PLAIN, 11);

    private ChartStyle() {}

    static void applyCategory(CategoryChart chart, Color barColor) {
        var s = chart.getStyler();
        s.setChartBackgroundColor(BG);
        s.setPlotBackgroundColor(PLOT);
        s.setPlotGridLinesColor(GRID);
        s.setPlotGridLinesVisible(true);
        s.setChartFontColor(TEXT);
        s.setAxisTickLabelsColor(TEXT);
        s.setChartTitleFont(TITLE_FONT);
        s.setAxisTitleFont(AXIS_TITLE_FONT);
        s.setAxisTickLabelsFont(TICK_FONT);
        s.setAvailableSpaceFill(0.15);
        s.setOverlapped(true);
        s.setSeriesColors(new Color[]{barColor});
        s.setLegendBackgroundColor(PLOT);
        s.setLegendBorderColor(GRID);
    }

    static void applyXy(XYChart chart, Color... seriesColors) {
        var s = chart.getStyler();
        s.setChartBackgroundColor(BG);
        s.setPlotBackgroundColor(PLOT);
        s.setPlotGridLinesColor(GRID);
        s.setPlotGridLinesVisible(true);
        s.setChartFontColor(TEXT);
        s.setAxisTickLabelsColor(TEXT);
        s.setChartTitleFont(TITLE_FONT);
        s.setAxisTitleFont(AXIS_TITLE_FONT);
        s.setAxisTickLabelsFont(TICK_FONT);
        s.setMarkerSize(8);
        s.setSeriesColors(seriesColors);
        s.setLegendPosition(Styler.LegendPosition.OutsideE);
        s.setLegendBackgroundColor(PLOT);
        s.setLegendBorderColor(GRID);
        s.setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
    }

    static void applyLineMarkers(XYChart chart) {
        chart.getSeriesMap().values().forEach(series -> {
            series.setMarker(SeriesMarkers.CIRCLE);
            series.setLineWidth(2.5f);
        });
    }
}
