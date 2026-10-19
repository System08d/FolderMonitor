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
import java.time.Duration;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    // Аргумент запуска "сразу свёрнутым в трей" (его же пишет AutoStart в реестр)
    public static final String MINIMIZED_ARG = "--minimized";

    // true = быстрые интервалы для проверки; false = боевые значения.
    // ПЕРЕД СБОРКОЙ .jar ДЛЯ АВТОЗАПУСКА ПОСТАВЬТЕ false!
    private static final boolean TEST_MODE = false;

    private static final Duration INTERVAL =
            TEST_MODE ? Duration.ofMinutes(1) : Duration.ofHours(24);   // раз в сутки
    private static final Duration CHECK_PERIOD =
            TEST_MODE ? Duration.ofSeconds(10) : Duration.ofMinutes(5);

    // После автозапуска первый сбор откладываем, чтобы не грузить диск сразу после входа в Windows
    private static final Duration STARTUP_DELAY =
            TEST_MODE ? Duration.ZERO : Duration.ofMinutes(2);

    // Временно: список папок задан в коде (на этапе 5 будет из настроек)
    private static final String[] FOLDERS = { "C:\\temp\\my program" };

    public static void main(String[] args) {
        boolean minimized = hasArg(args, MINIMIZED_ARG);
        useSystemLookAndFeel();

        Path dataDir = AppPaths.dataDir();
        StatsStorage storage;
        try {
            storage = new StatsStorage(dataDir);
        } catch (IOException e) {
            showMessageAndExit("Не удалось открыть папку данных:\n" + e.getMessage(),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Второй экземпляр не нужен: два сборщика писали бы в одни и те же файлы
        if (!SingleInstance.tryLock(dataDir)) {
            if (minimized) {
                System.exit(0);   // запуск из автозапуска — молча уходим
            }
            showMessageAndExit("Folder Monitor уже запущен.\n"
                    + "Найдите его значок в системном трее (возле часов).",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Settings settings = new Settings();

        Scheduler scheduler = new Scheduler(storage, INTERVAL, CHECK_PERIOD);
        for (String name : FOLDERS) {
            scheduler.addFolder(Paths.get(name));
        }
        scheduler.start(minimized ? STARTUP_DELAY : Duration.ZERO);

        AutoStart.syncOnStart(settings);

        AppController controller = new AppController(settings, scheduler);
        SwingUtilities.invokeLater(() -> controller.start(minimized));
        // main() на этом заканчивается, но программа живёт: её держат потоки
        // планировщика и окна. Завершить её можно только через controller.exit().
    }

    private static boolean hasArg(String[] args, String wanted) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(wanted)) {
                return true;
            }
        }
        return false;
    }

    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // не страшно: останется стандартный вид Java
        }
    }

    private static void showMessageAndExit(String text, int messageType) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, text, "Folder Monitor", messageType);
            System.exit(0);
        });
    }
}