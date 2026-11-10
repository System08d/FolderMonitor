package foldermonitor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieToolTipGenerator;
import org.jfree.chart.plot.CenterTextMode;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.general.DefaultPieDataset;

// Основа для кольцевых диаграмм «из чего состоит папка» на ПОСЛЕДНЮЮ дату измерения в периоде.
// Всё общее (выбор даты, «топ 8 + Прочее», цвета, легенда, число в центре, подсказки)
// написано здесь. Конкретная диаграмма только говорит, какие у неё доли.
public abstract class DonutChartBase implements ChartView {

    private static final int MAX_NAMED_SLICES = 8;       // больше именованных долей кольцо не читается
    private static final int MAX_NAME_LENGTH = 28;       // длинные имена папок сокращаем в легенде
    private static final String OTHER = "Прочее";
    private static final Locale RU = Locale.forLanguageTag("ru");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ---- то, чем отличаются диаграммы ----

    // Вернуть доли по последнему измерению: имя доли → значение (в единицах unitText()).
    // Если построить нельзя, бросить NoSlices с текстом для пользователя.
    protected abstract Map<String, Double> slices(Measurement root, List<Measurement> subfolders)
            throws NoSlices;

    // Единица в легенде и подсказке, с пробелом впереди: " ГБ", " файлов"
    protected abstract String unitText();

    // true: значения целые (файлы), false: одна цифра после запятой (ГБ)
    protected abstract boolean wholeNumbers();

    // Текст в центре кольца: сумма всех долей, готовая к показу
    protected abstract String centerText(double total);

    // Нечего показывать: сообщение вместо диаграммы
    protected static final class NoSlices extends Exception {
        public NoSlices(String message) {
            super(message);
        }
    }

    // ---- помощники для наследников ----

    // Топ-8 подпапок по значению, остальное и всё, что не вошло в подпапки, уходит в «Прочее».
    // rootValue: значение самой папки; «Прочее» = rootValue − сумма показанных подпапок.
    protected static Map<String, Double> topWithOther(List<Measurement> subfolders,
            java.util.function.ToDoubleFunction<Measurement> value, double rootValue) {
        List<Measurement> sorted = new ArrayList<>(subfolders);
        sorted.sort(Comparator.comparingDouble(value).reversed());

        Map<String, Double> result = new LinkedHashMap<>();
        double shown = 0;
        for (Measurement m : sorted) {
            double v = value.applyAsDouble(m);
            if (v <= 0 || result.size() >= MAX_NAMED_SLICES) {
                continue;
            }
            result.put(m.name(), v);
            shown += v;
        }
        double other = rootValue - shown;
        if (other > 0) {
            String key = OTHER;
            while (result.containsKey(key)) {
                key = key + "*";     // подпапка с таким же именем, как у «Прочее»: не склеиваем
            }
            result.put(key, other);
        }
        return result;
    }

    // ---- общая часть ----

    @Override
    public boolean forSeveralFolders() {
        return false;
    }

    @Override
    public final JComponent create(List<FolderHistory> histories, ChartPeriod period) {
        FolderHistory history = histories.get(0);
        List<Measurement> rows = history.root(period);
        if (rows.isEmpty()) {
            return ChartStyle.message("За выбранный период нет измерений");
        }
        Measurement last = rows.get(rows.size() - 1);

        // подпапки того же прохода: у них то же время, что у строки самой папки
        List<Measurement> subfolders = new ArrayList<>();
        for (Measurement m : history.rows()) {
            if (!m.isFolderItself() && m.time().equals(last.time())) {
                subfolders.add(m);
            }
        }

        Map<String, Double> slices;
        try {
            slices = slices(last, subfolders);
        } catch (NoSlices e) {
            return ChartStyle.message(e.getMessage());
        }
        double total = 0;
        for (double v : slices.values()) {
            total += v;
        }
        if (slices.isEmpty() || total <= 0) {
            return ChartStyle.message("В последнем измерении нет данных для этой диаграммы");
        }

        // Короткие имена для легенды; если после сокращения имена совпали, оставляем полные
        Map<String, String> shortNames = new LinkedHashMap<>();
        for (String name : slices.keySet()) {
            shortNames.put(name, shorten(name));
        }
        boolean clash = shortNames.values().stream().distinct().count() != shortNames.size();

        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        for (Map.Entry<String, Double> e : slices.entrySet()) {
            dataset.setValue(clash ? e.getKey() : shortNames.get(e.getKey()), e.getValue());
        }

        RingPlot plot = new RingPlot(dataset);
        styleRing(plot);

        int colorIndex = 0;
        for (Object keyObject : dataset.getKeys()) {
            String key = (String) keyObject;
            boolean isOther = key.startsWith(OTHER);
            plot.setSectionPaint(key, isOther ? ChartStyle.otherSliceColor()
                    : ChartStyle.sliceColor(colorIndex++));
        }

        NumberFormat numbers = new DecimalFormat(wholeNumbers() ? "#,##0" : "0.0",
                DecimalFormatSymbols.getInstance(RU));
        NumberFormat percents = new DecimalFormat("0.0%", DecimalFormatSymbols.getInstance(RU));
        plot.setToolTipGenerator(new StandardPieToolTipGenerator(
                "{0}: {1}" + unitText() + " ({2})", numbers, percents));
        plot.setLegendLabelGenerator(new StandardPieSectionLabelGenerator(
                "{0}: {1}" + unitText() + " ({2})", numbers, percents));
        plot.setCenterTextMode(CenterTextMode.FIXED);
        plot.setCenterText(centerText(total));
        plot.setCenterTextFont(Theme.FONT_BIG);
        plot.setCenterTextColor(Theme.VALUE);

        // заголовка нет (он в рамке панели), легенда справа вместо подписей на самом кольце:
        // подписи на кольце наезжают друг на друга, когда доли маленькие
        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        ChartStyle.apply(chart);
        LegendTitle legend = chart.getLegend();
        legend.setPosition(RectangleEdge.RIGHT);
        legend.setBackgroundPaint(Theme.BG);
        legend.setItemFont(Theme.FONT_NORMAL);
        legend.setItemPaint(Theme.TEXT);

        ChartPanel panel = ChartStyle.panel(chart);

        JLabel caption = new JLabel("Данные на " + DATE_TIME.format(last.time()), SwingConstants.LEFT);
        caption.setFont(Theme.FONT_SMALL);
        caption.setForeground(Theme.TEXT_DIM);
        caption.setBorder(new EmptyBorder(6, 8, 0, 8));

        JPanel result = new JPanel(new BorderLayout());
        result.setBackground(Theme.BG);
        result.add(caption, BorderLayout.NORTH);
        result.add(panel, BorderLayout.CENTER);
        return result;
    }

    private static String shorten(String name) {
        if (name.length() <= MAX_NAME_LENGTH) {
            return name;
        }
        int keep = MAX_NAME_LENGTH - 1;
        int head = keep * 2 / 3;
        return name.substring(0, head) + "…" + name.substring(name.length() - (keep - head));
    }

    // Настройки кольца, общие для всех диаграмм
    private static void styleRing(RingPlot plot) {
        plot.setBackgroundPaint(Theme.BG);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setLabelGenerator(null);               // подписей на кольце нет, всё в легенде
        plot.setSectionOutlinesVisible(false);
        plot.setSeparatorsVisible(true);
        plot.setSeparatorPaint(Theme.BG);           // тёмные щели между долями
        plot.setSeparatorStroke(new BasicStroke(2f));
        plot.setSectionDepth(0.38);                 // толщина кольца
        plot.setInteriorGap(0.06);
        plot.setStartAngle(90);
    }
}
