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
import java.time.Duration;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static final String MINIMIZED_ARG = "--minimized";

    // true = быстрые интервалы для проверки; false = боевые значения.
    // ПЕРЕД СБОРКОЙ .jar ДЛЯ ПОСТОЯННОЙ РАБОТЫ ПОСТАВЬТЕ false!
    private static final boolean TEST_MODE = true;

    private static final Duration CHECK_PERIOD =
            TEST_MODE ? Duration.ofSeconds(10) : Duration.ofMinutes(5);

    private static final Duration STARTUP_DELAY =
            TEST_MODE ? Duration.ZERO : Duration.ofMinutes(2);

    // Интервал сбора: в тестовом режиме всегда 1 минута, иначе — из настроек (в часах)
    public static Duration intervalFor(int hours) {
        return TEST_MODE ? Duration.ofMinutes(1) : Duration.ofHours(hours);
    }

    public static void main(String[] args) {
        boolean minimized = hasArg(args, MINIMIZED_ARG);
        useSystemLookAndFeel();

        Path dataDir = AppPaths.dataDir();
        AppLog.init(AppPaths.logDirFor(dataDir));
        Thread.setDefaultUncaughtExceptionHandler((thread, error) ->
                AppLog.error("Необработанная ошибка в потоке " + thread.getName(), error));

        StatsStorage storage;
        try {
            storage = new StatsStorage(dataDir);
        } catch (IOException e) {
            AppLog.error("Не удалось открыть папку данных " + dataDir, e);
            showMessageAndExit("Не удалось открыть папку данных:\n" + e.getMessage(),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!SingleInstance.tryLock(dataDir)) {
            AppLog.info("Повторный запуск: программа уже работает, вторая копия закрыта");
            if (minimized) {
                System.exit(0);
            }
            showMessageAndExit("Folder Monitor уже запущен.\n"
                    + "Найдите его значок в системном трее (возле часов).",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Settings settings = new Settings();
        List<WatchedFolder> folders = settings.getFolders();
        AppLog.info("=== Запуск Folder Monitor" + (minimized ? " (свёрнутым)" : "")
                + (TEST_MODE ? " [ТЕСТОВЫЙ РЕЖИМ]" : "")
                + ", данные: " + dataDir + ", папок в списке: " + folders.size() + " ===");

        Scheduler scheduler = new Scheduler(
                storage, intervalFor(settings.getIntervalHours()), CHECK_PERIOD);
        for (WatchedFolder folder : folders) {
            scheduler.addFolder(folder.path(), folder.enabled());
        }
        scheduler.start(minimized ? STARTUP_DELAY : Duration.ZERO);

        AutoStart.syncOnStart(settings);

        AppController controller = new AppController(settings, scheduler);
        SwingUtilities.invokeLater(() -> controller.start(minimized));
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