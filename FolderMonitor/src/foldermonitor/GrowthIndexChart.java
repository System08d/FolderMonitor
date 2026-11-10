package foldermonitor;

import java.awt.BasicStroke;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;
import javax.swing.JComponent;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

// График 1.4: «индекс роста». Размер и число файлов на одной оси, оба показателя в процентах
// от значения в начале периода (там 100%). Видно, что растёт быстрее.
public class GrowthIndexChart implements ChartView {

    private static final int MAX_POINTS_WITH_MARKERS = 60;
    private static final Locale RU = Locale.forLanguageTag("ru");

    @Override
    public String title() {
        return "Индекс роста";
    }

    @Override
    public String description() {
        return "Размер и число файлов в % от начала периода";
    }

    @Override
    public boolean forSeveralFolders() {
        return false;
    }

    @Override
    public JComponent create(List<FolderHistory> histories, ChartPeriod period) {
        List<Measurement> rows = histories.get(0).root(period);
        if (rows.isEmpty()) {
            return ChartStyle.message("За выбранный период нет измерений");
        }
        if (rows.size() < 2) {
            return ChartStyle.message("Недостаточно данных: нужно минимум 2 измерения за период");
        }

        TimeSeries size = series("Размер", rows, m -> (double) m.sizeBytes());
        TimeSeries files = series("Файлы", rows, m -> (double) m.fileCount());
        if (size == null && files == null) {
            return ChartStyle.message("Все значения за период равны нулю, индекс не посчитать");
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        if (size != null) {
            dataset.addSeries(size);
        }
        if (files != null) {
            dataset.addSeries(files);
        }

        // легенда нужна: на графике две линии, их надо различать (и по цвету, и по подписи)
        JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null, dataset,
                true, true, false);
        ChartStyle.apply(chart);
        ChartStyle.styleLegend(chart);
        XYPlot plot = chart.getXYPlot();

        XYLineAndShapeRenderer line = new XYLineAndShapeRenderer(true,
                rows.size() <= MAX_POINTS_WITH_MARKERS);
        DecimalFormat percent = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(RU));
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            boolean isSize = "Размер".equals(dataset.getSeriesKey(i).toString());
            line.setSeriesPaint(i, isSize ? Theme.ACCENT : Theme.VALUE);
            line.setSeriesStroke(i, new BasicStroke(1.8f));
            line.setSeriesShape(i, new Ellipse2D.Double(-2.5, -2.5, 5, 5));
            line.setSeriesToolTipGenerator(i, new StandardXYToolTipGenerator(
                    "{0}, {1}: {2} %", new SimpleDateFormat("yyyy-MM-dd HH:mm"), percent));
        }
        plot.setRenderer(line);

        // пунктир на 100%: «столько было в начале периода»
        ChartStyle.referenceLine(plot, 100.0);

        NumberAxis y = (NumberAxis) plot.getRangeAxis();
        y.setAutoRangeIncludesZero(false);
        y.setAutoRangeMinimumSize(2.0);
        y.setStandardTickUnits(NumberAxis.createStandardTickUnits(RU));
        y.setLabel("% от начала периода");

        DateAxis x = (DateAxis) plot.getDomainAxis();
        x.setLowerMargin(0.02);
        x.setUpperMargin(0.04);
        x.setStandardTickUnits(ChartStyle.dateTickUnits());

        return ChartStyle.panel(chart);
    }

    // Ряд в процентах от базы. Точки до первого ненулевого значения пропускаем (индекс там не определён).
    // null, если у показателя за весь период нет ненулевых значений.
    private static TimeSeries series(String name, List<Measurement> rows, ToDoubleFunction<Measurement> metric) {
        double base = GrowthMath.indexBase(rows, metric);
        if (base == 0) {
            return null;
        }
        TimeSeries series = new TimeSeries(name);
        boolean started = false;
        for (Measurement m : rows) {
            double v = metric.applyAsDouble(m);
            if (!started && v == 0) {
                continue;
            }
            started = true;
            Date date = Date.from(m.time().atZone(ZoneId.systemDefault()).toInstant());
            series.addOrUpdate(new Second(date), v / base * 100.0);
        }
        return series;
    }
}
