package com.lexicalanalyzer.ui;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.ArrayList;
import java.util.List;

/**
 * Live "tokens per source line" chart -- it grows line by line as the replay
 * reaches each line, and shows where the dense (or erroneous) code is.
 */
public class TokensPerLineChartView extends LineChart<Number, Number> {

    private final XYChart.Series<Number, Number> series = new XYChart.Series<>();
    private final List<XYChart.Data<Number, Number>> points = new ArrayList<>();

    public TokensPerLineChartView() {
        super(new NumberAxis(), new NumberAxis());
        setTitle(null);
        setAnimated(false);
        setLegendVisible(false);
        setCreateSymbols(false);
        getStyleClass().add("token-rate-chart");

        ((NumberAxis) getXAxis()).setLabel("Source line");
        ((NumberAxis) getYAxis()).setLabel("Tokens on line");
        ((NumberAxis) getYAxis()).setMinorTickVisible(false);
        ((NumberAxis) getYAxis()).setForceZeroInRange(true);

        getData().add(series);
    }

    public void reset() {
        points.clear();
        series.getData().clear();
    }

    /** Records one more token found on {@code line} (1-based). */
    public void addToken(int line) {
        while (points.size() < line) {
            XYChart.Data<Number, Number> p = new XYChart.Data<>(points.size() + 1, 0);
            points.add(p);
            series.getData().add(p);
        }
        XYChart.Data<Number, Number> p = points.get(line - 1);
        p.setYValue(p.getYValue().intValue() + 1);
    }
}
