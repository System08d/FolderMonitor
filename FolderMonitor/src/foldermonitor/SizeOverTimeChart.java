package foldermonitor;

import java.awt.BasicStroke;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

// График 1.1: размер папки (ГБ) по времени. Линия + полупрозрачная заливка под ней.
public class SizeOverTimeChart implements ChartView {

    private static final double BYTES_IN_GB = 1024.0 * 1024.0 * 1024.0;
    private static final int MAX_POINTS_WITH_MARKERS = 60;

    @Override
    public String title() {
        return "Размер по времени";
    }

    @Override
    public String description() {
        return "Как менялся размер папки (ГБ)";
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

        TimeSeries series = new TimeSeries("Размер, ГБ");
        for (Measurement m : rows) {
            Date date = Date.from(m.time().atZone(ZoneId.systemDefault()).toInstant());
            series.addOrUpdate(new Second(date), m.sizeBytes() / BYTES_IN_GB);
        }
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        // без заголовка, легенды и ссылок: заголовок есть в рамке панели
        JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null, dataset,
                false, true, false);
        ChartStyle.apply(chart);
        XYPlot plot = chart.getXYPlot();

        // слой 0: линия (рисуется сверху); слой 1: заливка под линией
        XYLineAndShapeRenderer line = new XYLineAndShapeRenderer(true,
                rows.size() <= MAX_POINTS_WITH_MARKERS);
        line.setSeriesPaint(0, Theme.ACCENT);
        line.setSeriesStroke(0, new BasicStroke(1.6f));
        line.setSeriesShape(0, new Ellipse2D.Double(-2.5, -2.5, 5, 5));
        line.setSeriesToolTipGenerator(0, new StandardXYToolTipGenerator(
                "{1}: {2} ГБ", new SimpleDateFormat("yyyy-MM-dd HH:mm"), oneDecimal()));
        plot.setRenderer(0, line);

        XYAreaRenderer area = new XYAreaRenderer(XYAreaRenderer.AREA);
        area.setSeriesPaint(0, ChartStyle.translucent(Theme.ACCENT, 45));
        area.setOutline(false);
        plot.setDataset(1, dataset);
        plot.setRenderer(1, area);

        // ось размера не обязана начинаться с нуля, иначе небольшой рост не виден
        NumberAxis y = (NumberAxis) plot.getRangeAxis();
        y.setAutoRangeIncludesZero(false);
        y.setAutoRangeMinimumSize(0.2);
        y.setNumberFormatOverride(oneDecimal());
        y.setLabel("ГБ");

        DateAxis x = (DateAxis) plot.getDomainAxis();
        x.setLowerMargin(0.02);
        x.setUpperMargin(0.04);
        // подписи дат не зависят от языка системы; для очень короткого периода добавляем время
        boolean shortSpan = Duration.between(rows.get(0).time(), rows.get(rows.size() - 1).time())
                .compareTo(Duration.ofDays(2)) < 0;
        x.setDateFormatOverride(new SimpleDateFormat(shortSpan ? "dd.MM HH:mm" : "dd.MM.yy"));

        return ChartStyle.panel(chart);
    }

    // 9,4 с запятой, как на всех экранах
    private static DecimalFormat oneDecimal() {
        return new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("ru")));
    }
}
