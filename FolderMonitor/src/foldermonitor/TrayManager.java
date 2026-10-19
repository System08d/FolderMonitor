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
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

public class TrayManager {

    private final Runnable onOpen;   // что делать по "Открыть"
    private final Runnable onExit;   // что делать по "Выход"
    private TrayIcon icon;           // null, пока значок не установлен

    public TrayManager(Runnable onOpen, Runnable onExit) {
        this.onOpen = onOpen;
        this.onExit = onExit;
    }

    public static boolean isSupported() {
        return SystemTray.isSupported();
    }

    public boolean isInstalled() {
        return icon != null;
    }

    public void install() throws AWTException {
        if (icon != null || !SystemTray.isSupported()) {
            return;
        }
        PopupMenu menu = new PopupMenu();
        MenuItem open = new MenuItem("Открыть");
        open.addActionListener(e -> onOpen.run());
        MenuItem exit = new MenuItem("Выход");
        exit.addActionListener(e -> onExit.run());
        menu.add(open);
        menu.addSeparator();
        menu.add(exit);

        TrayIcon newIcon = new TrayIcon(createIcon(), "Folder Monitor", menu);
        newIcon.setImageAutoSize(true);                       // подогнать размер под трей
        newIcon.addActionListener(e -> onOpen.run());         // двойной щелчок по значку
        SystemTray.getSystemTray().add(newIcon);              // может выбросить AWTException
        icon = newIcon;
    }

    public void uninstall() {
        if (icon != null) {
            SystemTray.getSystemTray().remove(icon);
            icon = null;
        }
    }

    // Значок рисуем кодом, чтобы не возиться с файлами картинок:
    // жёлтая папка с синим графиком. Используется и для окна.
    public static Image createIcon() {
        int size = 64;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xE0A800));
        g.fillRoundRect(4, 10, 26, 16, 6, 6);        // язычок папки
        g.setColor(new Color(0xFFC83D));
        g.fillRoundRect(4, 18, 56, 38, 8, 8);        // корпус папки
        g.setColor(new Color(0x1F5FA8));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(new int[] { 14, 26, 36, 50 }, new int[] { 46, 34, 41, 26 }, 4);
        g.dispose();
        return image;
    }
}