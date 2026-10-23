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
import java.awt.FlowLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;

// ВРЕМЕННАЯ панель: чтобы проверить логику этапа 5. На этапе 6 заменится вашим дизайном.
public class FoldersPanel extends JPanel {

    private static final int[] HOURS = { 1, 3, 6, 12, 24, 48, 168 };
    private static final String[] HOURS_TEXT = {
        "1 час", "3 часа", "6 часов", "12 часов", "24 часа (раз в сутки)", "2 суток", "7 суток" };
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AppController controller;
    private final DefaultListModel<Path> model = new DefaultListModel<>();
    private final JList<Path> list = new JList<>(model);
    private final Timer refreshTimer;

    public FoldersPanel(AppController controller) {
        super(new BorderLayout(0, 8));
        this.controller = controller;

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new FolderRenderer());
        reloadList();

        JButton addButton = new JButton("Добавить…");
        JButton removeButton = new JButton("Убрать");
        JButton startButton = new JButton("Старт");
        JButton stopButton = new JButton("Стоп");
        addButton.addActionListener(e -> addFolder());
        removeButton.addActionListener(e -> removeSelected());
        startButton.addActionListener(e -> setSelectedEnabled(true));
        stopButton.addActionListener(e -> setSelectedEnabled(false));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.add(addButton);
        buttons.add(removeButton);
        buttons.add(startButton);
        buttons.add(stopButton);

        JComboBox<String> interval = new JComboBox<>(HOURS_TEXT);
        interval.setSelectedIndex(indexForHours(controller.getIntervalHours()));   // до addActionListener!
        interval.addActionListener(e ->
                controller.setIntervalHours(HOURS[interval.getSelectedIndex()]));

        JPanel intervalRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        intervalRow.add(new JLabel("Собирать данные раз в:"));
        intervalRow.add(interval);

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(buttons, BorderLayout.NORTH);
        top.add(intervalRow, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);

        // раз в 2 секунды перерисовываем список, чтобы менялись статусы
        refreshTimer = new Timer(2000, e -> list.repaint());
    }

    // Таймер работает только пока панель на экране. dispose() окна вызывает removeNotify(),
    // и таймер останавливается, поэтому окно не "залипает" в памяти.
    @Override
    public void addNotify() {
        super.addNotify();
        refreshTimer.start();
    }

    @Override
    public void removeNotify() {
        refreshTimer.stop();
        super.removeNotify();
    }

    private void reloadList() {
        model.clear();
        for (Path folder : controller.getFolders()) {
            model.addElement(folder);
        }
    }

    private void addFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите папку для наблюдения");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path folder = chooser.getSelectedFile().toPath();
        if (!Files.isDirectory(folder)) {
            JOptionPane.showMessageDialog(this, "Это не папка или она недоступна.",
                    "Добавить папку", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!controller.addFolder(folder)) {
            JOptionPane.showMessageDialog(this, "Эта папка уже в списке.",
                    "Добавить папку", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        reloadList();
        list.setSelectedValue(folder.toAbsolutePath().normalize(), true);
    }

    private void removeSelected() {
        Path folder = list.getSelectedValue();
        if (folder == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Убрать папку из наблюдения?\n" + folder
                        + "\n\nСама папка и накопленная история не удаляются.",
                "Убрать папку", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            controller.removeFolder(folder);
            reloadList();
        }
    }

    private void setSelectedEnabled(boolean enabled) {
        Path folder = list.getSelectedValue();
        if (folder != null) {
            controller.setFolderEnabled(folder, enabled);
            list.repaint();
        }
    }

    private static int indexForHours(int hours) {
        for (int i = 0; i < HOURS.length; i++) {
            if (HOURS[i] == hours) {
                return i;
            }
        }
        return 4;   // 24 часа
    }

    private static String statusText(Scheduler.FolderStatus s) {
        String last = s.lastRun() == null
                ? "—"
                : TIME.format(LocalDateTime.ofInstant(s.lastRun(), ZoneId.systemDefault()));
        return switch (s.state()) {
            case STOPPED -> "Остановлено. Последнее измерение: " + last;
            case SCANNING -> "Идёт сбор данных…";
            case ERROR -> "Ошибка: " + s.message() + " (последнее измерение: " + last + ")";
            case WAITING -> "Работает. Последнее измерение: " + last;
        };
    }

    // В HTML-тексте метки символы & < > нужно экранировать (в путях бывает "&")
    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // Рисует строку списка: путь жирным, ниже статус. Статус берётся в момент отрисовки.
    private class FolderRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                boolean selected, boolean focus) {
            super.getListCellRendererComponent(l, value, index, selected, focus);
            Path folder = (Path) value;
            setText("<html><b>" + escape(folder.toString()) + "</b><br>"
                    + escape(statusText(controller.getStatus(folder))) + "</html>");
            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            return this;
        }
    }
}