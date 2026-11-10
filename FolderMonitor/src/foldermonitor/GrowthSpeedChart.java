package foldermonitor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

// График 2.1: скорость роста размера. Тонкая линия: прирост за каждый день. Толстая линия:
// скользящее среднее за 7 дней. Кнопка переключает единицу: ГБ в день или ГБ в неделю.
// «ГБ в неделю» это та же скорость, умноженная на 7 (сколько прибавилось бы за неделю при таком темпе).
public class GrowthSpeedChart implements ChartView {

    private static final int WINDOW_DAYS = 7;
    private static final int MAX_POINTS_WITH_MARKERS = 60;
    private static final Locale RU = Locale.forLanguageTag("ru");

    @Override
    public String title() {
        return "Скорость роста";
    }

    @Override
    public String description() {
        return "Прирост размера в ГБ в день или в неделю, со скользящим средним";
    }

    @Override
    public boolean forSeveralFolders() {
        return false;
    }

    @Override
    public JComponent create(List<FolderHistory> histories, ChartPeriod period) {
        FolderHistory history = histories.get(0);
        if (history.root().isEmpty()) {
            return ChartStyle.message("Нет измерений");
        }

        // Считаем по всей истории, чтобы у первого дня периода был прирост относительно дня до него,
        // и только потом оставляем дни периода.
        List<GrowthMath.DayValue> sizes = GrowthMath.dailyValues(history.root(),
                m -> Fmt.gbValue(m.sizeBytes()));
        List<GrowthMath.DayValue> growth = GrowthMath.growth(sizes);
        List<GrowthMath.DayValue> average = GrowthMath.movingAverage(growth, WINDOW_DAYS);
        List<GrowthMath.DayValue> shownGrowth = GrowthMath.inPeriod(growth, period);
        List<GrowthMath.DayValue> shownAverage = GrowthMath.inPeriod(average, period);

        if (shownGrowth.size() < 2) {
            return ChartStyle.message("Недостаточно данных: нужен период минимум из 3 дней с измерениями");
        }
        return new SpeedPanel(shownGrowth, shownAverage);
    }

    // Панель: сверху строка с кнопкой единицы, под ней график. По кнопке график строится заново.
    private static final class SpeedPanel extends JPanel {

        private final List<GrowthMath.DayValue> growth;
        private final List<GrowthMath.DayValue> average;
        private final ThemedButton unitButton = new ThemedButton("");
        private boolean weekly = false;
        private JComponent chartComponent;

        SpeedPanel(List<GrowthMath.DayValue> growth, List<GrowthMath.DayValue> average) {
            super(new BorderLayout());
            this.growth = growth;
            this.average = average;
            setBackground(Theme.BG);

            JLabel note = new JLabel("Среднее за " + WINDOW_DAYS + " дней. Дни без измерений: берём прошлое значение.");
            note.setFont(Theme.FONT_SMALL);
            note.setForeground(Theme.TEXT_DIM);
            unitButton.addActionListener(e -> {
                weekly = !weekly;
                rebuild();
            });

            JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            bar.setOpaque(false);
            bar.setBorder(new EmptyBorder(6, 8, 6, 8));
            bar.add(unitButton);
            bar.add(note);
            add(bar, BorderLayout.NORTH);
            rebuild();
        }

        private void rebuild() {
            unitButton.setText(weekly ? "Единица: ГБ в неделю" : "Единица: ГБ в день");
            if (chartComponent != null) {
                remove(chartComponent);
            }
            chartComponent = buildChart(weekly ? 7.0 : 1.0, weekly ? "ГБ/нед" : "ГБ/день");
            add(chartComponent, BorderLayout.CENTER);
            revalidate();
            repaint();
        }

        private JComponent buildChart(double factor, String unit) {
            TimeSeries dayLine = new TimeSeries("Прирост за день");
            for (GrowthMath.DayValue v : growth) {
                dayLine.addOrUpdate(day(v), v.value() * factor);
            }
            TimeSeries avgLine = new TimeSeries("Среднее за " + WINDOW_DAYS + " дней");
            for (GrowthMath.DayValue v : average) {
                avgLine.addOrUpdate(day(v), v.value() * factor);
            }
            TimeSeriesCollection dataset = new TimeSeriesCollection();
            dataset.addSeries(dayLine);
            dataset.addSeries(avgLine);

            JFreeChart chart = ChartFactory.createTimeSeriesChart(null, null, null, dataset,
                    true, true, false);
            ChartStyle.apply(chart);
            ChartStyle.styleLegend(chart);
            XYPlot plot = chart.getXYPlot();

            DecimalFormat number = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(RU));
            XYLineAndShapeRenderer line = new XYLineAndShapeRenderer(true,
                    growth.size() <= MAX_POINTS_WITH_MARKERS);
            line.setSeriesPaint(0, Theme.ACCENT_DIM);          // дневной прирост: тоньше и тише
            line.setSeriesStroke(0, new BasicStroke(1.2f));
            line.setSeriesPaint(1, Theme.ACCENT);              // среднее: основная линия
            line.setSeriesStroke(1, new BasicStroke(2.4f));
            line.setSeriesShapesVisible(1, false);             // точки только у дневной линии
            for (int i = 0; i < 2; i++) {
                line.setSeriesShape(i, new Ellipse2D.Double(-2.5, -2.5, 5, 5));
                line.setSeriesToolTipGenerator(i, new StandardXYToolTipGenerator(
                        "{0}, {1}: {2} " + unit, new SimpleDateFormat("yyyy-MM-dd"), number));
            }
            plot.setRenderer(line);

            // нулевая линия: выше неё размер растёт, ниже убывает
            ChartStyle.referenceLine(plot, 0.0);

            NumberAxis y = (NumberAxis) plot.getRangeAxis();
            y.setAutoRangeIncludesZero(true);
            y.setAutoRangeMinimumSize(0.02);
            y.setStandardTickUnits(NumberAxis.createStandardTickUnits(RU));
            y.setLabel(unit);

            DateAxis x = (DateAxis) plot.getDomainAxis();
            x.setLowerMargin(0.02);
            x.setUpperMargin(0.04);
            x.setStandardTickUnits(ChartStyle.dateTickUnits());

            return ChartStyle.panel(chart);
        }

        private static Day day(GrowthMath.DayValue v) {
            return new Day(v.day().getDayOfMonth(), v.day().getMonthValue(), v.day().getYear());
        }
    }
}
