package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.awt.AWTException;
import java.awt.Frame;
import java.nio.file.Path;
import java.util.List;

public class AppController {

    private final Settings settings;
    private final Scheduler scheduler;
    private final DataProvider data;
    private TrayManager tray;
    private MainFrame frame;

    public AppController(Settings settings, Scheduler scheduler, StatsStorage storage) {
        this.settings = settings;
        this.scheduler = scheduler;
        this.data = new DataProvider(storage);
    }

    // НОВОЕ: чтение истории для графиков
    public DataProvider getDataProvider() {
        return data;
    }

    // ================== жизнь окна и трея ==================

    public void start(boolean minimized) {
        applyTraySetting();
        boolean hidden = minimized && tray != null && tray.isInstalled();
        if (!hidden) {
            openWindow();
        }
    }

        public void openWindow() {
        if (frame == null) {
            Theme.installLookAndFeel();   // НОВОЕ: при первом открытии окна
            frame = new MainFrame(settings, this);
        }
        frame.setVisible(true);
        frame.setState(Frame.NORMAL);
        frame.toFront();
    }

    public void windowCloseRequested() {
        if (tray != null && tray.isInstalled()) {
            closeWindow();
        } else {
            exit();
        }
    }

    public void setMinimizeToTray(boolean value) {
        settings.setMinimizeToTray(value);
        applyTraySetting();
    }

    public void exit() {
        closeWindow();
        if (tray != null) {
            tray.uninstall();
        }
        scheduler.stop();
        AppLog.info("Выход из программы");
        System.exit(0);
    }

    // ================== папки и интервал (НОВОЕ) ==================

    public List<Path> getFolders() {
        return scheduler.getFolders();
    }

    public Scheduler.FolderStatus getStatus(Path folder) {
        return scheduler.getStatus(folder);
    }

    // false, если такая папка уже есть
    public boolean addFolder(Path folder) {
        if (!scheduler.addFolder(folder, true)) {
            return false;
        }
        saveFolders();
        AppLog.info("Добавлена папка: " + folder);
        scheduler.checkSoon();   // первый сбор сразу, а не через несколько минут
        return true;
    }

    public void removeFolder(Path folder) {
        scheduler.removeFolder(folder);
        saveFolders();
        AppLog.info("Папка убрана из наблюдения (история в data сохранена): " + folder);
    }

    public void setFolderEnabled(Path folder, boolean enabled) {
        scheduler.setEnabled(folder, enabled);
        saveFolders();
        AppLog.info((enabled ? "Сбор запущен: " : "Сбор остановлен: ") + folder);
        if (enabled) {
            scheduler.checkSoon();
        }
    }

    public int getIntervalHours() {
        return settings.getIntervalHours();
    }

    public void setIntervalHours(int hours) {
        settings.setIntervalHours(hours);
        scheduler.setInterval(Main.intervalFor(hours));
        AppLog.info("Интервал сбора: " + hours + " ч");
        scheduler.checkSoon();
    }

    private void saveFolders() {
        settings.saveFolders(scheduler.getFolderConfigs());
    }

    // ================== внутреннее ==================

    private void closeWindow() {
        if (frame != null) {
            frame.dispose();
            frame = null;
            System.gc();
        }
    }

    private void applyTraySetting() {
        if (!TrayManager.isSupported()) {
            return;
        }
        if (settings.isMinimizeToTray()) {
            if (tray == null) {
                tray = new TrayManager(this::openWindow, this::exit);
            }
            try {
                tray.install();
            } catch (AWTException e) {
                AppLog.warn("Не удалось добавить значок в трей: " + e.getMessage());
            }
        } else if (tray != null) {
            tray.uninstall();
        }
    }
}
