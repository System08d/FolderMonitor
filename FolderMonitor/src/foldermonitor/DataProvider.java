package foldermonitor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Слой данных для графиков. CSV читается только когда пользователь открыл график,
// и ничего не кэшируется: объект FolderHistory держит экран, пока он открыт.
public class DataProvider {

    // Папка, у которой есть история. watched = false: из наблюдения убрана, история осталась.
    public record KnownFolder(Path path, boolean watched) { }

    private final StatsStorage storage;

    public DataProvider(StatsStorage storage) {
        this.storage = storage;
    }

    // Все папки, по которым можно показать график: наблюдаемые с историей + убранные с историей
    public List<KnownFolder> knownFolders(Collection<Path> watched) {
        List<KnownFolder> result = new ArrayList<>();
        Set<Path> seen = new HashSet<>();

        for (Path folder : watched) {
            Path path = normalize(folder);
            if (storage.hasHistory(path) && seen.add(path)) {
                try {
                    storage.rememberPath(path);   // чтобы папка не пропала из списка после "Убрать"
                } catch (IOException e) {
                    AppLog.warn("Не удалось запомнить путь папки " + path + ": " + e.getMessage());
                }
                result.add(new KnownFolder(path, true));
            }
        }
        for (Path folder : storage.knownFolders()) {
            Path path = normalize(folder);
            if (seen.add(path)) {
                result.add(new KnownFolder(path, false));
            }
        }
        result.sort(Comparator.comparing(k -> k.path().toString().toLowerCase()));
        return result;
    }

    // Читает CSV целиком. Вызывать НЕ из потока окна (файл может быть большим).
    public FolderHistory load(Path folder) throws IOException {
        Path path = normalize(folder);
        return new FolderHistory(path, storage.read(path));
    }

    private static Path normalize(Path folder) {
        return folder.toAbsolutePath().normalize();
    }
}
