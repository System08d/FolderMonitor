/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class Main {

    // ВПИШИТЕ СВОЮ ПАПКУ (сначала лучше тестовую, см. ниже)
    private static final String FOLDER = "C:\\Users\\oleksandr.dan\\Downloads";

    public static void main(String[] args) {
        Path root = Paths.get(FOLDER);
        FolderScanner scanner = new FolderScanner();

        try {
            System.out.println("Сканирую: " + root);
            long start = System.nanoTime();
            ScanResult result = scanner.scan(root);
            long millis = (System.nanoTime() - start) / 1_000_000;

            System.out.println("=== Вся папка ===");
            System.out.println(result.total());

            System.out.println("=== Подпапки первого уровня (прошли порог) ===");
            if (result.subfolders().isEmpty()) {
                System.out.println("Подходящих подпапок нет");
            }
            for (Map.Entry<String, FolderStats> entry : result.subfolders().entrySet()) {
                FolderStats s = entry.getValue();
                System.out.println(entry.getKey() + ": " + s.getSizeGbText() + " ГБ, файлов "
                        + s.getFileCount() + ", папок " + s.getFolderCount());
            }
            System.out.println("Время обхода: " + millis + " мс");

            // ---- Этап 3: сохранение и чтение ----
            StatsStorage storage = new StatsStorage(AppPaths.dataDir());
            storage.append(root, result.toMeasurements(LocalDateTime.now()));
            System.out.println("Файл данных: " + storage.fileFor(root));

            List<Measurement> history = storage.read(root);
            System.out.println("Всего строк в истории: " + history.size());
            if (!history.isEmpty()) {
                System.out.println("Последняя строка: " + history.get(history.size() - 1));
            }
        } catch (IOException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }
}