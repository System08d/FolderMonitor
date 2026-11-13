package foldermonitor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JComponent;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

// График 4.1: рейтинг подпапок по приросту за период. Горизонтальные столбики, сверху тот,
// кто вырос сильнее всех. Рост красится бирюзовым, убыль красным, но знак "+"/"-" есть и в подписи,
// так что цвет не единственный признак.
public class SubfolderGrowthChart implements ChartView {

    private static final int TOP = 10;
    private static final Locale RU = Locale.forLanguageTag("ru");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public String title() {
        return "Рейтинг подпапок по приросту";
    }

    @Override
    public String description() {
        return "Какие подпапки выросли сильнее всего за период (топ 10)";
    }

    @Override
    public boolean forSeveralFolders() {
        return false;
    }

    @Override
    public JComponent create(List<FolderHistory> histories, ChartPeriod period) {
        FolderHistory history = histories.get(0);
        List<Measurement> rows = history.root(period);
        if (rows.isEmpty()) {
            return ChartStyle.message("За выбранный период нет измерений");
        }
        if (rows.size() < 2) {
            return ChartStyle.message("Недостаточно данных: нужно минимум 2 измерения за период");
        }
        LocalDateTime from = rows.get(0).time();
        LocalDateTime to = rows.get(rows.size() - 1).time();

        // размеры подпапок (ГБ) в первом и в последнем измерении периода
        Map<String, Double> start = subfolderSizes(history, from);
        Map<String, Double> end = subfolderSizes(history, to);
        List<GrowthMath.NamedValue> ranking = GrowthMath.ranking(start, end);
        if (ranking.isEmpty()) {
            return ChartStyle.message("В измерениях за период нет подпапок крупнее порога");
        }
        if (ranking.size() > TOP) {
            ranking = ranking.subList(0, TOP);
        }

        List<String> names = new ArrayList<>();
        for (GrowthMath.NamedValue v : ranking) {
            names.add(v.name());
        }
        List<String> shown = ChartStyle.shortNames(names);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < ranking.size(); i++) {
            dataset.addValue(ranking.get(i).value(), "Прирост, ГБ", shown.get(i));
        }

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset,
                PlotOrientation.HORIZONTAL, false, true, false);
        ChartStyle.apply(chart);
        CategoryPlot plot = chart.getCategoryPlot();

        final List<GrowthMath.NamedValue> items = ranking;
        BarRenderer renderer = ChartStyle.barRenderer(
                i -> items.get(i).value() >= 0 ? Theme.ACCENT : Theme.WARN);
        DecimalFormat signed = new DecimalFormat("+0.0;-0.0", DecimalFormatSymbols.getInstance(RU));
        ChartStyle.barLabels(renderer, signed, " ГБ");
        plot.setRenderer(renderer);

        NumberAxis y = (NumberAxis) plot.getRangeAxis();
        y.setStandardTickUnits(NumberAxis.createStandardTickUnits(RU));
        y.setLabel("Прирост, ГБ");

        return ChartStyle.withCaption("Прирост с " + DATE_TIME.format(from) + " по "
                + DATE_TIME.format(to) + ". Подпапка, которой нет в измерении, считается как 0.",
                ChartStyle.panel(chart));
    }

    // Подпапки одного прохода (строки с тем же временем, что у строки самой папки): имя → размер в ГБ
    private static Map<String, Double> subfolderSizes(FolderHistory history, LocalDateTime time) {
        Map<String, Double> result = new HashMap<>();
        for (Measurement m : history.rows()) {
            if (!m.isFolderItself() && m.time().equals(time)) {
                result.put(m.name(), Fmt.gbValue(m.sizeBytes()));
            }
        }
        return result;
    }
}
