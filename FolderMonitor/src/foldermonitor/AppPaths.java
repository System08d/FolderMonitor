/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

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

    public static Path dataDir() {
        return appDir().resolve("data");
    }
}