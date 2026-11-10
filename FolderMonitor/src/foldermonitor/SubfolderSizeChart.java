package foldermonitor;

import java.util.List;
import java.util.Map;

// Диаграмма 3.2: доли подпапок первого уровня в общем размере (ГБ). Топ-8 подпапок,
// всё остальное (файлы прямо в корне, мелкие подпапки) идёт в «Прочее».
public class SubfolderSizeChart extends DonutChartBase {

    @Override
    public String title() {
        return "Подпапки по размеру";
    }

    @Override
    public String description() {
        return "Какая доля размера приходится на каждую подпапку";
    }

    @Override
    protected Map<String, Double> slices(Measurement root, List<Measurement> subfolders) {
        return topWithOther(subfolders, m -> Fmt.gbValue(m.sizeBytes()), Fmt.gbValue(root.sizeBytes()));
    }

    @Override
    protected String unitText() {
        return " ГБ";
    }

    @Override
    protected boolean wholeNumbers() {
        return false;
    }

    @Override
    protected String centerText(double total) {
        return Fmt.gbText(total);
    }
}
