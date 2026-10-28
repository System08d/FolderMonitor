package foldermonitor;

import java.util.ArrayList;
import java.util.List;

// Список всех графиков приложения. Новый график добавляется одной строкой в ALL.
public final class ChartRegistry {

    private static final List<ChartView> ALL = List.of(
            new SizeOverTimeChart()
    );

    private ChartRegistry() { }

    // Графики, которые подходят для выбранного количества папок
    public static List<ChartView> forFolderCount(int folderCount) {
        List<ChartView> result = new ArrayList<>();
        for (ChartView chart : ALL) {
            if (chart.forSeveralFolders() == (folderCount > 1)) {
                result.add(chart);
            }
        }
        return result;
    }
}
