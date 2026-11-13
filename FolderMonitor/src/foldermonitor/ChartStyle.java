package foldermonitor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;

// Единый тёмный стиль всех графиков JFreeChart. Цвета и шрифты берутся из Theme.
// Любой новый график после создания JFreeChart вызывает apply(chart) и panel(chart).
public final class ChartStyle {

    private ChartStyle() { }

    // Тёмный фон, тонкая сетка, моноширинные шрифты
    public static void apply(JFreeChart chart) {
        chart.setBackgroundPaint(Theme.BG);
        chart.setBorderVisible(false);
        chart.setAntiAlias(true);
        chart.setTextAntiAlias(true);

        Plot plot = chart.getPlot();
        plot.setBackgroundPaint(Theme.BG);
        plot.setOutlinePaint(Theme.GRID);
        plot.setOutlineStroke(new BasicStroke(1f));

        if (plot instanceof XYPlot xy) {
            xy.setDomainGridlinePaint(Theme.GRID);
            xy.setRangeGridlinePaint(Theme.GRID);
            xy.setDomainGridlineStroke(new BasicStroke(1f));
            xy.setRangeGridlineStroke(new BasicStroke(1f));
            styleAxis(xy.getDomainAxis());
            styleAxis(xy.getRangeAxis());
        } else if (plot instanceof CategoryPlot cp) {
            cp.setRangeGridlinePaint(Theme.GRID);
            cp.setRangeGridlineStroke(new BasicStroke(1f));
            cp.setDomainGridlinesVisible(false);
            styleAxis(cp.getDomainAxis());
            styleAxis(cp.getRangeAxis());
        }
    }

    private static void styleAxis(Axis axis) {
        axis.setLabelFont(Theme.FONT_SMALL);
        axis.setLabelPaint(Theme.TEXT_DIM);
        axis.setTickLabelFont(Theme.FONT_SMALL);
        axis.setTickLabelPaint(Theme.TEXT_DIM);
        axis.setAxisLinePaint(Theme.BORDER);
        axis.setTickMarkPaint(Theme.BORDER);
    }

    // Деления оси времени. У каждого шага свой формат подписи: секундам нужны секунды, дням не нужно
    // время. JFreeChart сам берёт самый мелкий шаг, при котором подписи не слипаются, и подпись
    // всегда различает соседние деления. Один общий формат (как раньше) этого не умел и давал
    // "05.11 16:51" два раза подряд при периоде в пару минут.
    public static TickUnits dateTickUnits() {
        SimpleDateFormat seconds = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat minutes = new SimpleDateFormat("dd.MM HH:mm");
        SimpleDateFormat days = new SimpleDateFormat("dd.MM.yy");
        SimpleDateFormat months = new SimpleDateFormat("MM.yyyy");
        SimpleDateFormat years = new SimpleDateFormat("yyyy");

        TickUnits units = new TickUnits();
        for (int n : new int[] {1, 5, 10, 30}) {
            units.add(new DateTickUnit(DateTickUnitType.SECOND, n, seconds));
        }
        for (int n : new int[] {1, 5, 10, 30}) {
            units.add(new DateTickUnit(DateTickUnitType.MINUTE, n, minutes));
        }
        for (int n : new int[] {1, 3, 6, 12}) {
            units.add(new DateTickUnit(DateTickUnitType.HOUR, n, minutes));
        }
        for (int n : new int[] {1, 2, 7, 14}) {
            units.add(new DateTickUnit(DateTickUnitType.DAY, n, days));
        }
        for (int n : new int[] {1, 3, 6}) {
            units.add(new DateTickUnit(DateTickUnitType.MONTH, n, months));
        }
        for (int n : new int[] {1, 5, 10, 25}) {
            units.add(new DateTickUnit(DateTickUnitType.YEAR, n, years));
        }
        return units;
    }

    // Обёртка для показа в окне: без контекстного меню и зума мышью, растягивается на всё место
    public static ChartPanel panel(JFreeChart chart) {
        ChartPanel panel = new ChartPanel(chart);
        panel.setBackground(Theme.BG);
        panel.setPopupMenu(null);
        panel.setMouseZoomable(false);
        // Без этого при большом окне JFreeChart растягивает готовую картинку, а не рисует заново
        panel.setMinimumDrawWidth(10);
        panel.setMinimumDrawHeight(10);
        panel.setMaximumDrawWidth(Integer.MAX_VALUE);
        panel.setMaximumDrawHeight(Integer.MAX_VALUE);
        return panel;
    }

    // Полупрозрачная версия цвета (для заливки под линией)
    public static Color translucent(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    // Легенда над графиком в общем стиле (для графиков с несколькими линиями)
    public static void styleLegend(JFreeChart chart) {
        LegendTitle legend = chart.getLegend();
        if (legend == null) {
            return;
        }
        legend.setPosition(RectangleEdge.TOP);
        legend.setBackgroundPaint(Theme.BG);
        legend.setItemFont(Theme.FONT_NORMAL);
        legend.setItemPaint(Theme.TEXT);
    }

    // Пунктирная горизонтальная линия на заданном значении оси (100% у индекса, 0 у скорости)
    public static void referenceLine(XYPlot plot, double value) {
        Stroke dashed = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[] {4f, 4f}, 0f);
        plot.addRangeMarker(new ValueMarker(value, Theme.TEXT_DIM, dashed));
    }

    // Столбики: цвет каждого берётся по номеру категории (так у папки один и тот же цвет на всех
    // графиках группы 5), без градиента и теней, не слишком толстые.
    public static BarRenderer barRenderer(IntFunction<Paint> colorOfCategory) {
        BarRenderer renderer = new BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {
                return colorOfCategory.apply(column);
            }
        };
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        renderer.setMaximumBarWidth(0.12);
        return renderer;
    }

    // Подписи значений над столбиками и подсказка при наведении. unit с пробелом впереди: " ГБ"
    public static void barLabels(BarRenderer renderer, NumberFormat format, String unit) {
        renderer.setSeriesItemLabelGenerator(0, new StandardCategoryItemLabelGenerator("{2}", format));
        renderer.setSeriesItemLabelsVisible(0, true);
        renderer.setSeriesItemLabelPaint(0, Theme.TEXT);
        renderer.setSeriesItemLabelFont(0, Theme.FONT_SMALL);
        renderer.setSeriesToolTipGenerator(0,
                new StandardCategoryToolTipGenerator("{1}: {2}" + unit, format));
    }

    // Тонкая строка пояснения над графиком («Данные на …», «Прирост с … по …»)
    public static JPanel withCaption(String text, JComponent content) {
        JLabel caption = new JLabel(text, SwingConstants.LEFT);
        caption.setFont(Theme.FONT_SMALL);
        caption.setForeground(Theme.TEXT_DIM);
        caption.setBorder(new EmptyBorder(6, 8, 0, 8));
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG);
        panel.add(caption, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    // Длинное имя папки сокращаем до 28 символов серединой: начало…конец
    public static String shortName(String name) {
        final int max = 28;
        if (name.length() <= max) {
            return name;
        }
        int keep = max - 1;
        int head = keep * 2 / 3;
        return name.substring(0, head) + "…" + name.substring(name.length() - (keep - head));
    }

    // Короткие имена для списка; если после сокращения какие-то совпали, возвращаем все полные
    public static List<String> shortNames(List<String> names) {
        List<String> result = new ArrayList<>();
        for (String name : names) {
            result.add(shortName(name));
        }
        if (result.stream().distinct().count() != result.size()) {
            return new ArrayList<>(names);
        }
        return result;
    }

    // Цвета долей на кольцевых диаграммах. Подобраны так, чтобы соседние доли различались
    // на тёмном фоне; красный не используем, он зарезервирован под отрицательный прирост.
    private static final Color[] SLICE_COLORS = {
        Theme.ACCENT,                 // бирюзовый
        Theme.VALUE,                  // салатовый
        new Color(0x3FA7E0),          // синий
        new Color(0xE5A93C),          // янтарный
        new Color(0x9B7BE8),          // фиолетовый
        new Color(0xE06FA8),          // розовый
        new Color(0x5CC8D6),          // голубой
        new Color(0xC77B4A)           // медный
    };

    public static Color sliceColor(int index) {
        return SLICE_COLORS[index % SLICE_COLORS.length];
    }

    // «Прочее» всегда приглушённого серо-зелёного цвета, чтобы не путалось с именованными долями
    public static Color otherSliceColor() {
        return new Color(0x4A6B63);
    }

    // Сообщение вместо графика ("нет данных за период" и т.п.)
    public static JLabel message(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font(Theme.FONT_NORMAL.getName(), Font.PLAIN, Theme.FONT_NORMAL.getSize()));
        label.setForeground(Theme.TEXT_DIM);
        return label;
    }
}
