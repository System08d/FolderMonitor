package foldermonitor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

// График 5.1: текущий размер всех выбранных папок, столбцы. «Текущий» = последнее измерение
// в выбранном периоде. Если у папки в периоде измерений нет, показываем 0 (принятое правило).
public class FoldersSizeChart implements ChartView {

    private static final Locale RU = Locale.forLanguageTag("ru");

    @Override
    public String title() {
        return "Размер папок";
    }

    @Override
    public String description() {
        return "Текущий размер выбранных папок, столбцы";
    }

    @Override
    public boolean forSeveralFolders() {
        return true;
    }

    @Override
    public JComponent create(List<FolderHistory> histories, ChartPeriod period) {
        List<String> labels = ChartStyle.shortNames(FolderLabels.of(histories));

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        boolean anyData = false;
        for (int i = 0; i < histories.size(); i++) {
            List<Measurement> rows = histories.get(i).root(period);
            double gb = 0;
            if (!rows.isEmpty()) {
                gb = Fmt.gbValue(rows.get(rows.size() - 1).sizeBytes());
                anyData = true;
            }
            dataset.addValue(gb, "Размер, ГБ", labels.get(i));
        }
        if (!anyData) {
            return ChartStyle.message("За выбранный период нет измерений ни у одной папки");
        }

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset,
                PlotOrientation.VERTICAL, false, true, false);
        ChartStyle.apply(chart);
        CategoryPlot plot = chart.getCategoryPlot();

        // цвет папки: по её номеру в выборе, одинаковый на всех графиках группы 5
        BarRenderer renderer = ChartStyle.barRenderer(ChartStyle::sliceColor);
        ChartStyle.barLabels(renderer, new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(RU)), " ГБ");
        plot.setRenderer(renderer);

        NumberAxis y = (NumberAxis) plot.getRangeAxis();
        y.setStandardTickUnits(NumberAxis.createStandardTickUnits(RU));
        y.setLabel("ГБ");

        return ChartStyle.withCaption("Размер на последнее измерение в периоде", ChartStyle.panel(chart));
    }
}
