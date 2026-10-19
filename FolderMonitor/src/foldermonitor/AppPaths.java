/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppPaths {

    // Папка приложения: рядом с .jar, а при запуске из NetBeans — папка проекта
    public static Path appDir() {
        try {
            Path location = Paths.get(AppPaths.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(location)) {   // запущено из .jar-файла
                return location.getParent();
            }
        } catch (Exception e) {
            // не получилось определить — используем запасной вариант ниже
        }
        return Paths.get(System.getProperty("user.dir"));
    }

    // НОВОЕ: если рядом с приложением писать нельзя (например, Program Files),
    // данные лежат в %LOCALAPPDATA%\FolderMonitor\data
    public static Path dataDir() {
        Path preferred = appDir().resolve("data");
        if (canWriteTo(preferred)) {
            return preferred;
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            return Paths.get(localAppData, "FolderMonitor", "data");
        }
        return Paths.get(System.getProperty("user.home"), ".foldermonitor", "data");
    }

    // Настоящая проверка: пробуем создать и сразу удалить пустой временный файл
    // (внутри папки данных самого приложения, не в наблюдаемых папках)
    private static boolean canWriteTo(Path dir) {
        try {
            Files.createDirectories(dir);
            Path probe = Files.createTempFile(dir, "probe", ".tmp");
            Files.delete(probe);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
    
        // НОВОЕ: путь к самому .jar-файлу или null, если запущено не из .jar (например, из NetBeans)
    public static Path jarFile() {
        try {
            Path location = Paths.get(AppPaths.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(location)) {
                return location;
            }
        } catch (Exception e) {
            // не получилось определить — вернём null
        }
        return null;
    }
    
}
