package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.awt.BasicStroke;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
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

// Основа для графиков «одно число одной папки по времени» (размер, файлы, папки): линия с точками
// и полупрозрачной заливкой под ней. Всё общее (сборка графика, оси, подсказки, проверки данных)
// написано здесь один раз. Конкретный график только отвечает на несколько вопросов: как называется,
// какое число брать из измерения и как подписать ось.
public abstract class LineChartBase implements ChartView {

    private static final int MAX_POINTS_WITH_MARKERS = 60;
    private static final Locale RU = Locale.forLanguageTag("ru");   // десятичная запятая на всех осях

    // ---- то, чем отличаются графики друг от друга ----

    // Название ряда данных (в подсказках и внутри JFreeChart)
    protected abstract String seriesName();

    // Подпись вертикальной оси: "ГБ", "файлов", "папок"
    protected abstract String axisLabel();

    // Единица в подсказке над точкой, с пробелом впереди: " ГБ", " файлов"
    protected abstract String unitText();

    // Какое число измерения нанести на график
    protected abstract double valueOf(Measurement m);

    // true: значения целые (файлы, папки): деления оси тоже целые, без "1000,5 файлов"
    protected abstract boolean wholeNumbers();

    // Минимальная высота оси. Если значение почти не менялось, ось не сжимается в линию:
    // 0,2 для гигабайт, 5 для штук.
    protected abstract double minAxisSpan();

    // ---- общая часть ----

    @Override
    public boolean forSeveralFolders() {
        return false;
    }

    @Override
    public final JComponent create(List<FolderHistory> histories, ChartPeriod period) {
        List<Measurement> rows = histories.get(0).root(period);
        if (rows.isEmpty()) {
            return ChartStyle.message("За выбранный период нет измерений");
        }
        if (rows.size() < 2) {
            return ChartStyle.message("Недостаточно данных: нужно минимум 2 измерения за период");
        }

        TimeSeries series = new TimeSeries(seriesName());
        for (Measurement m : rows) {
            Date date = Date.from(m.time().atZone(ZoneId.systemDefault()).toInstant());
            series.addOrUpdate(new Second(date), valueOf(m));
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
                "{1}: {2}" + unitText(), new SimpleDateFormat("yyyy-MM-dd HH:mm"), valueFormat()));
        plot.setRenderer(0, line);

        XYAreaRenderer area = new XYAreaRenderer(XYAreaRenderer.AREA);
        area.setSeriesPaint(0, ChartStyle.translucent(Theme.ACCENT, 45));
        area.setOutline(false);
        plot.setDataset(1, dataset);
        plot.setRenderer(1, area);

        // Вертикальная ось не обязана начинаться с нуля, иначе небольшой рост не виден.
        // Деления выбирает сам JFreeChart, число знаков у каждого шага своё, поэтому подписи
        // не повторяются (общий формат "одна цифра" давал 40,5 40,5 40,5 40,4 ...).
        NumberAxis y = (NumberAxis) plot.getRangeAxis();
        y.setAutoRangeIncludesZero(false);
        y.setAutoRangeMinimumSize(minAxisSpan());
        y.setStandardTickUnits(wholeNumbers()
                ? NumberAxis.createIntegerTickUnits(RU)
                : NumberAxis.createStandardTickUnits(RU));
        y.setLabel(axisLabel());

        // Ось времени: у каждого шага деления свой формат подписи (секунды, минуты, часы, дни),
        // так что подписи не повторяются ни при периоде в пару минут, ни при периоде в годы.
        DateAxis x = (DateAxis) plot.getDomainAxis();
        x.setLowerMargin(0.02);
        x.setUpperMargin(0.04);
        x.setStandardTickUnits(ChartStyle.dateTickUnits());

        return ChartStyle.panel(chart);
    }

    // Число в подсказке: целое с пробелами между разрядами или одна цифра после запятой
    private NumberFormat valueFormat() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(RU);
        return new DecimalFormat(wholeNumbers() ? "#,##0" : "0.0", symbols);
    }
}
