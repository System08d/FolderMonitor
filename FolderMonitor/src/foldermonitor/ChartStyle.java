package foldermonitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
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
