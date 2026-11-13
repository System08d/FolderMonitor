package foldermonitor;

import java.awt.BasicStroke;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

// Основа для графиков «по линии на каждую выбранную папку» (5.2 и 5.3).
// История у папок разной длины: линия каждой папки начинается с её первого измерения в периоде,
// нулевых точек до этого не рисуем (иначе получился бы ложный взлёт с нуля).
// Цвет папки зависит от её номера в выборе, поэтому на всех графиках группы 5 он один и тот же.
public abstract class MultiLineChartBase implements ChartView {

    private static final int MAX_POINTS_WITH_MARKERS = 60;
    private static final Locale RU = Locale.forLanguageTag("ru");

    // Ряд одной папки по её измерениям за период (по времени); null, если построить нельзя
    protected abstract TimeSeries buildSeries(String name, List<Measurement> rows);

    protected abstract String axisLabel();

    // Единица в подсказке, с пробелом впереди: " ГБ", " %"
    protected abstract String unitText();

    // Значение, у которого рисуется пунктир (100 у индекса), или null
    protected abstract Double referenceValue();

    protected abstract double minAxisSpan();

    @Override
    public boolean forSeveralFolders() {
        return true;
    }

    @Override
    public final JComponent create(List<FolderHistory> histories, ChartPeriod period) {
        List<String> labels = FolderLabels.of(histories);

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        List<Integer> folderIndexOfSeries = new ArrayList<>();
        boolean anyLine = false;
        int maxPoints = 0;
        for (int i = 0; i < histories.size(); i++) {
            List<Measurement> rows = histories.get(i).root(period);
            if (rows.isEmpty()) {
                continue;
            }
            TimeSeries series = buildSeries(labels.get(i), rows);
            if (series == null || series.isEmpty()) {
                continue;
            }
            dataset.addSeries(series);
            folderIndexOfSeries.add(i);
            anyLine |= series.getItemCount() >= 2;
            maxPoints = Math.max(maxPoints, series.getItemCount());
        }
        if (dataset.getSeriesCount() == 0) {
            return ChartStyle.message("За выбранный период нет измерений ни у одной папки");
        }
        if (!anyLine) {
            return ChartStyle.message("Недостаточно данных: нужно минимум 2 измерения за период хотя бы у одной папки");
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null, dataset,
                true, true, false);
        ChartStyle.apply(chart);
        ChartStyle.styleLegend(chart);
        XYPlot plot = chart.getXYPlot();

        XYLineAndShapeRenderer line = new XYLineAndShapeRenderer(true, maxPoints <= MAX_POINTS_WITH_MARKERS);
        DecimalFormat number = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(RU));
        for (int s = 0; s < dataset.getSeriesCount(); s++) {
            line.setSeriesPaint(s, ChartStyle.sliceColor(folderIndexOfSeries.get(s)));
            line.setSeriesStroke(s, new BasicStroke(1.8f));
            line.setSeriesShape(s, new Ellipse2D.Double(-2.5, -2.5, 5, 5));
            line.setSeriesToolTipGenerator(s, new StandardXYToolTipGenerator(
                    "{0}, {1}: {2}" + unitText(), new SimpleDateFormat("yyyy-MM-dd HH:mm"), number));
        }
        plot.setRenderer(line);

        if (referenceValue() != null) {
            ChartStyle.referenceLine(plot, referenceValue());
        }

        NumberAxis y = (NumberAxis) plot.getRangeAxis();
        y.setAutoRangeIncludesZero(false);
        y.setAutoRangeMinimumSize(minAxisSpan());
        y.setStandardTickUnits(NumberAxis.createStandardTickUnits(RU));
        y.setLabel(axisLabel());

        DateAxis x = (DateAxis) plot.getDomainAxis();
        x.setLowerMargin(0.02);
        x.setUpperMargin(0.04);
        x.setStandardTickUnits(ChartStyle.dateTickUnits());

        return ChartStyle.panel(chart);
    }
}
