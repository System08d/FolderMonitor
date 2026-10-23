/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Settings {

    private static final String KEY_TRAY = "minimizeToTray";
    private static final String KEY_AUTOSTART = "autoStart";
    private static final String KEY_AUTOSTART_COMMAND = "autoStartCommand";
    private static final String KEY_INTERVAL_HOURS = "intervalHours";
    private static final String NODE_FOLDERS = "folders";

    // В Windows хранится в реестре: HKCU\Software\JavaSoft\Prefs\foldermonitor
    private final Preferences prefs = Preferences.userNodeForPackage(Settings.class);

    public boolean isMinimizeToTray() {
        return prefs.getBoolean(KEY_TRAY, true);
    }

    public void setMinimizeToTray(boolean value) {
        prefs.putBoolean(KEY_TRAY, value);
        flush();
    }

    public boolean isAutoStart() {
        return prefs.getBoolean(KEY_AUTOSTART, false);
    }

    public void setAutoStart(boolean value) {
        prefs.putBoolean(KEY_AUTOSTART, value);
        flush();
    }

    public String getAutoStartCommand() {
        return prefs.get(KEY_AUTOSTART_COMMAND, "");
    }

    public void setAutoStartCommand(String value) {
        prefs.put(KEY_AUTOSTART_COMMAND, value);
        flush();
    }

    // НОВОЕ: интервал сбора в часах. По умолчанию — раз в сутки.
    public int getIntervalHours() {
        return Math.max(1, prefs.getInt(KEY_INTERVAL_HOURS, 24));
    }

    public void setIntervalHours(int hours) {
        prefs.putInt(KEY_INTERVAL_HOURS, Math.max(1, hours));
        flush();
    }

    // НОВОЕ: список наблюдаемых папок
    public List<WatchedFolder> getFolders() {
        List<WatchedFolder> result = new ArrayList<>();
        Preferences node = prefs.node(NODE_FOLDERS);
        int count = node.getInt("count", 0);
        for (int i = 0; i < count; i++) {
            String path = node.get(i + ".path", "");
            if (path.isEmpty()) {
                continue;
            }
            try {
                result.add(new WatchedFolder(Paths.get(path), node.getBoolean(i + ".enabled", true)));
            } catch (InvalidPathException e) {
                AppLog.warn("Пропущен некорректный путь в настройках: " + path);
            }
        }
        return result;
    }

    // НОВОЕ: сохранить весь список (он маленький, поэтому проще записать целиком)
    public void saveFolders(List<WatchedFolder> folders) {
        Preferences node = prefs.node(NODE_FOLDERS);
        try {
            node.clear();   // удалить старые ключи
        } catch (BackingStoreException e) {
            AppLog.warn("Не удалось обновить настройки папок: " + e.getMessage());
            return;
        }
        node.putInt("count", folders.size());
        for (int i = 0; i < folders.size(); i++) {
            node.put(i + ".path", folders.get(i).path().toString());
            node.putBoolean(i + ".enabled", folders.get(i).enabled());
        }
        flush();
    }

    private void flush() {
        try {
            prefs.flush();   // записывает и дочерние узлы
        } catch (BackingStoreException e) {
            AppLog.warn("Не удалось сохранить настройки: " + e.getMessage());
        }
    }
}