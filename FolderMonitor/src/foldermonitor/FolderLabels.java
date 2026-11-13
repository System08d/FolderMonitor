package foldermonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Подписи папок для графиков группы 5. Обычно это короткое имя ("Documents"). Если у двух
// выбранных папок имя одинаковое (C:\a\Documents и D:\b\Documents), оба подписываются полным путём,
// иначе их не отличить.
public final class FolderLabels {

    private FolderLabels() { }

    public static List<String> of(List<FolderHistory> histories) {
        Map<String, Integer> counts = new HashMap<>();
        for (FolderHistory h : histories) {
            counts.merge(h.displayName(), 1, Integer::sum);
        }
        List<String> labels = new ArrayList<>();
        for (FolderHistory h : histories) {
            labels.add(counts.get(h.displayName()) > 1 ? h.folder().toString() : h.displayName());
        }
        return labels;
    }
}
