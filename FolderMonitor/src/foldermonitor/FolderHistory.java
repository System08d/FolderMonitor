package foldermonitor;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Вся прочитанная из CSV история одной папки. Живёт только пока открыт экран графика,
// после закрытия экрана объект уходит в сборщик мусора.
public final class FolderHistory {

    private final Path folder;
    private final List<Measurement> rows;   // все строки (корень и подпапки), по времени
    private final List<Measurement> root;   // только строки самой папки, по времени

    public FolderHistory(Path folder, List<Measurement> rows) {
        this.folder = folder;
        List<Measurement> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparing(Measurement::time));   // сортировка устойчивая
        this.rows = Collections.unmodifiableList(sorted);

        List<Measurement> onlyRoot = new ArrayList<>();
        for (Measurement m : sorted) {
            if (m.isFolderItself()) {
                onlyRoot.add(m);
            }
        }
        this.root = Collections.unmodifiableList(onlyRoot);
    }

    public Path folder() {
        return folder;
    }

    // Короткое имя для подписей: последнее звено пути (у корня диска — весь путь)
    public String displayName() {
        Path name = folder.getFileName();
        return name != null ? name.toString() : folder.toString();
    }

    // Все строки, включая подпапки (понадобятся для кольцевых диаграмм и рейтинга)
    public List<Measurement> rows() {
        return rows;
    }

    // Измерения самой папки за всё время
    public List<Measurement> root() {
        return root;
    }

    // Измерения самой папки за период
    public List<Measurement> root(ChartPeriod period) {
        List<Measurement> result = new ArrayList<>();
        for (Measurement m : root) {
            if (period.contains(m.time())) {
                result.add(m);
            }
        }
        return result;
    }

    public Optional<Measurement> latestRoot() {
        return root.isEmpty() ? Optional.empty() : Optional.of(root.get(root.size() - 1));
    }

    // Последнее измерение папки, сделанное не позже указанного момента
    public Optional<Measurement> rootAtOrBefore(LocalDateTime time) {
        for (int i = root.size() - 1; i >= 0; i--) {
            if (!root.get(i).time().isAfter(time)) {
                return Optional.of(root.get(i));
            }
        }
        return Optional.empty();
    }
}
