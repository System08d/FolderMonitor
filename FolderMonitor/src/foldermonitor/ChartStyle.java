package foldermonitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;

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

    // Сообщение вместо графика ("нет данных за период" и т.п.)
    public static JLabel message(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font(Theme.FONT_NORMAL.getName(), Font.PLAIN, Theme.FONT_NORMAL.getSize()));
        label.setForeground(Theme.TEXT_DIM);
        return label;
    }
}
