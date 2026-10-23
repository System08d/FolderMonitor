/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MainFrame extends JFrame {

    public MainFrame(Settings settings, AppController controller) {
        super("Folder Monitor");
        setIconImage(TrayManager.createIcon());

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.windowCloseRequested();
            }
        });

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
            autoStartBox.setEnabled(false);
            new Thread(() -> {
                String error = changeAutoStart(settings, wanted);
                SwingUtilities.invokeLater(() -> {
                    autoStartBox.setEnabled(true);
                    if (error != null) {
                        autoStartBox.setSelected(!wanted);
                        JOptionPane.showMessageDialog(this,
                                "Не удалось изменить автозапуск:\n" + error,
                                "Автозапуск", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }, "autostart-change").start();
        });

        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        trayBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoStartBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        options.add(trayBox);
        options.add(autoStartBox);

        // ---- сборка окна: сверху временная панель папок, снизу галочки ----
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        content.add(new FoldersPanel(controller), BorderLayout.CENTER);
        content.add(options, BorderLayout.SOUTH);
        content.setPreferredSize(new Dimension(560, 480));

        setContentPane(content);
        pack();
        setLocationRelativeTo(null);
    }

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