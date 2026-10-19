/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MainFrame extends JFrame {

    public MainFrame(Settings settings, AppController controller) {
        super("Folder Monitor");
        setIconImage(TrayManager.createIcon());

        // Крестик НЕ закрывает окно сам: решение принимает AppController
        // (спрятаться в трей или завершить программу)
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.windowCloseRequested();
            }
        });

        JLabel info = new JLabel("<html>Приложение работает, данные собираются в фоне.<br>"
                + "Папки и графики появятся на следующих этапах.</html>");

        // ---- галочка "Сворачивать в трей" ----
        JCheckBox trayBox = new JCheckBox("Сворачивать в трей при закрытии окна");
        if (TrayManager.isSupported()) {
            trayBox.setSelected(settings.isMinimizeToTray());
        } else {
            trayBox.setEnabled(false);
            trayBox.setToolTipText("Системный трей в этой системе недоступен");
        }
        trayBox.addActionListener(e -> controller.setMinimizeToTray(trayBox.isSelected()));

        // ---- галочка "Запускать при включении компьютера" ----
        JCheckBox autoStartBox = new JCheckBox("Запускать при включении компьютера");
        if (AutoStart.isSupported()) {
            autoStartBox.setSelected(settings.isAutoStart());
        } else {
            autoStartBox.setEnabled(false);
            autoStartBox.setToolTipText("Работает только в собранном .jar под Windows "
                    + "(при запуске из NetBeans недоступно)");
        }
        autoStartBox.addActionListener(e -> {
            boolean wanted = autoStartBox.isSelected();
            autoStartBox.setEnabled(false);   // на время операции
            // PowerShell запускается долго (доли секунды), поэтому делаем это не в потоке окна
            new Thread(() -> {
                String error = changeAutoStart(settings, wanted);
                SwingUtilities.invokeLater(() -> {   // вернулись в поток окна
                    autoStartBox.setEnabled(true);
                    if (error != null) {
                        autoStartBox.setSelected(!wanted);   // откатываем галочку
                        JOptionPane.showMessageDialog(this,
                                "Не удалось изменить автозапуск:\n" + error,
                                "Автозапуск", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }, "autostart-change").start();
        });

        // ---- раскладка: элементы друг под другом, выровненные по левому краю ----
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setPreferredSize(new Dimension(460, 180));
        for (Component c : new Component[] { info, trayBox, autoStartBox }) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        panel.add(info);
        panel.add(Box.createVerticalStrut(16));
        panel.add(trayBox);
        panel.add(autoStartBox);

        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);   // по центру экрана
    }

    // Возвращает null при успехе или текст ошибки
    private static String changeAutoStart(Settings settings, boolean wanted) {
        try {
            if (wanted) {
                AutoStart.enable(settings);
            } else {
                AutoStart.disable(settings);
            }
            return null;
        } catch (IOException e) {
            return e.getMessage();
        }
    }
}