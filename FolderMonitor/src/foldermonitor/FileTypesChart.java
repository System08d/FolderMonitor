package foldermonitor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Диаграмма 3.1: из каких файлов состоит папка: изображения, видео, прочее (по количеству файлов).
public class FileTypesChart extends DonutChartBase {

    @Override
    public String title() {
        return "Типы файлов";
    }

    @Override
    public String description() {
        return "Изображения, видео и прочее (по числу файлов)";
    }

    @Override
    protected Map<String, Double> slices(Measurement root, List<Measurement> subfolders) throws NoSlices {
        if (!root.hasTypes()) {
            throw new NoSlices("В этом измерении нет данных по типам файлов");
        }
        Map<String, Double> result = new LinkedHashMap<>();
        addIfPositive(result, "Изображения", root.imageCount());
        addIfPositive(result, "Видео", root.videoCount());
        addIfPositive(result, "Прочее", root.otherCount());
        return result;
    }

    private static void addIfPositive(Map<String, Double> map, String name, long value) {
        if (value > 0) {
            map.put(name, (double) value);
        }
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
