/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.awt.AWTException;
import java.awt.Frame;

public class AppController {

    private final Settings settings;
    private final Scheduler scheduler;
    private TrayManager tray;     // null, если трей не поддерживается или ещё не создан
    private MainFrame frame;      // null, пока окна нет (тогда оно не занимает память)

    public AppController(Settings settings, Scheduler scheduler) {
        this.settings = settings;
        this.scheduler = scheduler;
    }

    // Вызывается один раз при старте, в потоке окна
    public void start(boolean minimized) {
        applyTraySetting();
        // Если значка нет, то без окна приложение было бы невидимым — окно показываем в любом случае
        boolean hidden = minimized && tray != null && tray.isInstalled();
        if (!hidden) {
            openWindow();
        }
    }

    // Показать окно (создать заново, если его не было)
    public void openWindow() {
        if (frame == null) {
            frame = new MainFrame(settings, this);
        }
        frame.setVisible(true);
        frame.setState(Frame.NORMAL);   // если было свёрнуто на панель задач
        frame.toFront();
    }

    // Пользователь нажал на крестик
    public void windowCloseRequested() {
        if (tray != null && tray.isInstalled()) {
            closeWindow();     // прячемся в трей
        } else {
            exit();            // трея нет или он выключен — завершаем программу
        }
    }

    // Галочка "Сворачивать в трей" изменилась
    public void setMinimizeToTray(boolean value) {
        settings.setMinimizeToTray(value);
        applyTraySetting();
    }

    // Полный выход: останавливаем сбор (прерывая обход, если он идёт) и завершаем процесс
    public void exit() {
        closeWindow();
        if (tray != null) {
            tray.uninstall();
        }
        scheduler.stop();
        System.exit(0);
    }

    // Уничтожаем окно полностью, чтобы освободить память
    private void closeWindow() {
        if (frame != null) {
            frame.dispose();   // освобождает ресурсы окна (setVisible(false) этого не делает)
            frame = null;      // ссылки больше нет — сборщик мусора заберёт объект
            System.gc();       // подсказка сборщику мусора (не гарантия)
        }
    }

    // Привести значок в трее в соответствие с настройкой
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
                System.out.println("Не удалось добавить значок в трей: " + e.getMessage());
            }
        } else if (tray != null) {
            tray.uninstall();
        }
    }
}