/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Settings {

    private static final String KEY_TRAY = "minimizeToTray";
    private static final String KEY_AUTOSTART = "autoStart";
    private static final String KEY_AUTOSTART_COMMAND = "autoStartCommand";

    // В Windows хранится в реестре: HKCU\Software\JavaSoft\Prefs\foldermonitor
    private final Preferences prefs = Preferences.userNodeForPackage(Settings.class);

    // По умолчанию трей включён: приложение задумано как фоновое
    public boolean isMinimizeToTray() {
        return prefs.getBoolean(KEY_TRAY, true);
    }

    public void setMinimizeToTray(boolean value) {
        prefs.putBoolean(KEY_TRAY, value);
        flush();
    }

    // По умолчанию автозапуск выключен: без вашего согласия систему не меняем
    public boolean isAutoStart() {
        return prefs.getBoolean(KEY_AUTOSTART, false);
    }

    public void setAutoStart(boolean value) {
        prefs.putBoolean(KEY_AUTOSTART, value);
        flush();
    }

    // Команда, которую мы последний раз записали в автозапуск ("" = ничего не записано)
    public String getAutoStartCommand() {
        return prefs.get(KEY_AUTOSTART_COMMAND, "");
    }

    public void setAutoStartCommand(String value) {
        prefs.put(KEY_AUTOSTART_COMMAND, value);
        flush();
    }

    private void flush() {
        try {
            prefs.flush();   // записать на диск сразу, а не когда-нибудь
        } catch (BackingStoreException e) {
            System.out.println("Не удалось сохранить настройки: " + e.getMessage());
        }
    }
}