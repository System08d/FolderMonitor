package foldermonitor;

import java.util.List;
import java.util.Map;

// Диаграмма 3.3: доли подпапок первого уровня по количеству файлов. Топ-8 подпапок,
// остальное идёт в «Прочее».
public class SubfolderCountChart extends DonutChartBase {

    @Override
    public String title() {
        return "Подпапки по числу файлов";
    }

    @Override
    public String description() {
        return "Какая доля файлов приходится на каждую подпапку";
    }

    @Override
    protected Map<String, Double> slices(Measurement root, List<Measurement> subfolders) {
        return topWithOther(subfolders, Measurement::fileCount, root.fileCount());
    }

    @Override
    protected String unitText() {
        return " файлов";
    }

    @Override
    protected boolean wholeNumbers() {
        return true;
    }

    @Override
    protected String centerText(double total) {
        return Fmt.count((long) total);
    }
}
