package foldermonitor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

// Главный экран: список наблюдаемых папок, управление сбором, интервал, галочки, кнопка графиков.
// Заменил временные FoldersPanel и прежний MainScreen.
public class MainScreen extends JPanel {

    private static final int[] HOURS = { 1, 3, 6, 12, 24, 48, 168 };
    private static final String[] HOURS_TEXT = {
        "1 час", "3 часа", "6 часов", "12 часов", "24 часа (раз в сутки)", "2 суток", "7 суток" };
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AppController controller;
    private final DefaultListModel<Path> model = new DefaultListModel<>();
    private final JList<Path> list = new JList<>(model);
    private final Timer refreshTimer;

    public MainScreen(Settings settings, AppController controller, Runnable onShowCharts) {
        super(new BorderLayout(0, 12));
        this.controller = controller;
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("FOLDER MONITOR");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.ACCENT);

        // ---------- панель папок ----------
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new FolderRenderer());
        list.setBackground(Theme.PANEL);                     // фон не зависит от того, подключился ли FlatLaf
        ToolTipManager.sharedInstance().registerComponent(list);   // подсказка с полным путём и статусом
        reloadList();
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.PANEL);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);   // длинный путь обрезается, а не прокручивается

        ThemedButton addButton = new ThemedButton("Добавить…");
        ThemedButton removeButton = new ThemedButton("Убрать");
        ThemedButton startButton = new ThemedButton("Старт");
        ThemedButton stopButton = new ThemedButton("Стоп");
        addButton.addActionListener(e -> addFolder());
        removeButton.addActionListener(e -> removeSelected());
        startButton.addActionListener(e -> setSelectedEnabled(true));
        stopButton.addActionListener(e -> setSelectedEnabled(false));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(addButton);
        buttons.add(removeButton);
        buttons.add(Box.createHorizontalStrut(12));
        buttons.add(startButton);
        buttons.add(stopButton);

        JComboBox<String> interval = new JComboBox<>(HOURS_TEXT);
        interval.setSelectedIndex(indexForHours(controller.getIntervalHours()));   // до addActionListener!
        interval.addActionListener(e ->
                controller.setIntervalHours(HOURS[interval.getSelectedIndex()]));
        JLabel intervalLabel = new JLabel("Собирать данные раз в:");
        intervalLabel.setFont(Theme.FONT_NORMAL);
        intervalLabel.setForeground(Theme.TEXT);
        JPanel intervalRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        intervalRow.setOpaque(false);
        intervalRow.add(intervalLabel);
        intervalRow.add(interval);

        JPanel foldersTop = new JPanel(new BorderLayout(0, 8));
        foldersTop.setOpaque(false);
        foldersTop.add(buttons, BorderLayout.NORTH);
        foldersTop.add(intervalRow, BorderLayout.SOUTH);

        JPanel folders = new JPanel(new BorderLayout(0, 8));
        folders.setOpaque(false);
        folders.setBorder(HudBorder.of("Наблюдаемые папки"));
        folders.add(foldersTop, BorderLayout.NORTH);
        folders.add(scroll, BorderLayout.CENTER);

        // ---------- галочка "Сворачивать в трей" ----------
        JCheckBox trayBox = new JCheckBox("Сворачивать в трей при закрытии окна");
        if (TrayManager.isSupported()) {
            trayBox.setSelected(settings.isMinimizeToTray());
        } else {
            trayBox.setEnabled(false);
            trayBox.setToolTipText("Системный трей в этой системе недоступен");
        }
        trayBox.addActionListener(e -> controller.setMinimizeToTray(trayBox.isSelected()));

        // ---------- галочка "Запускать при включении компьютера" ----------
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
            // PowerShell запускается долго (доли секунды), поэтому не в потоке окна
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
        styleCheckBox(trayBox);
        styleCheckBox(autoStartBox);

        JPanel options = new JPanel();
        options.setOpaque(false);
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setBorder(HudBorder.of("Работа в фоне"));
        trayBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoStartBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        options.add(trayBox);
        options.add(autoStartBox);

        // ---------- кнопка "Показать графики" ----------
        ThemedButton chartsButton = new ThemedButton("Показать графики");
        chartsButton.addActionListener(e -> onShowCharts.run());
        JPanel buttonCell = new JPanel(new BorderLayout());   // чтобы кнопка не растянулась на всю высоту
        buttonCell.setOpaque(false);
        buttonCell.add(chartsButton, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setOpaque(false);
        bottom.add(options, BorderLayout.CENTER);
        bottom.add(buttonCell, BorderLayout.EAST);

        add(title, BorderLayout.NORTH);
        add(folders, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // Раз в 2 секунды перерисовываем список, чтобы менялись статусы. Таймер работает
        // только пока экран ВИДЕН (когда открыты графики или окно скрыто, он стоит).
        refreshTimer = new Timer(2000, e -> list.repaint());
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    list.repaint();
                    refreshTimer.start();
                } else {
                    refreshTimer.stop();
                }
            }
        });
    }

    // ================== папки ==================

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

    // ================== вспомогательное ==================

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

    // В HTML-подсказке символы & < > нужно экранировать (в путях бывает "&")
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // Галочка в цветах темы: прозрачный фон, светлый текст
    private static void styleCheckBox(JCheckBox box) {
        box.setOpaque(false);
        box.setForeground(Theme.TEXT);
        box.setFont(Theme.FONT_NORMAL);
        box.setFocusPainted(false);
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

    // Строка списка: сверху путь (слева) и текущий размер (справа), ниже статус.
    // Цвет статуса: ошибка красным, идёт сбор — подсвечен, остановлено — приглушено.
    // Размер берётся из последнего измерения, поэтому он виден и у остановленной папки.
    private class FolderRenderer extends JPanel implements ListCellRenderer<Path> {
        private final JLabel path = new JLabel();
        private final JLabel size = new JLabel();
        private final JLabel status = new JLabel();

        FolderRenderer() {
            super(new BorderLayout(0, 2));
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            path.setFont(Theme.FONT_BOLD);
            size.setFont(Theme.FONT_BOLD);
            status.setFont(Theme.FONT_SMALL);

            JPanel head = new JPanel(new BorderLayout(12, 0));
            head.setOpaque(false);
            head.add(path, BorderLayout.CENTER);
            head.add(size, BorderLayout.EAST);
            add(head, BorderLayout.NORTH);
            add(status, BorderLayout.SOUTH);
        }

        // Если путь не помещается, обрезаем его СЛЕВА ("…ents/Projects/final"): имя папки важнее начала пути.
        // Полный путь остаётся в подсказке.
        private String fitPath(JList<?> l, String text, String sizeText) {
            if (l.getWidth() <= 0) {
                return text;
            }
            int reserved = 16 + 12 + size.getFontMetrics(size.getFont()).stringWidth(sizeText);   // поля, зазор, размер
            int available = l.getWidth() - reserved;
            FontMetrics fm = path.getFontMetrics(path.getFont());
            if (fm.stringWidth(text) <= available) {
                return text;
            }
            int cut = 1;
            while (cut < text.length() - 1 && fm.stringWidth("…" + text.substring(cut)) > available) {
                cut++;
            }
            return "…" + text.substring(cut);
        }

        // Ширину строки определяет окно, а не длина текста: иначе длинный путь растягивает список
        // и размер справа уезжает за край. Лишнее обрезается многоточием.
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(0, super.getPreferredSize().height);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Path> l, Path folder,
                int index, boolean selected, boolean focus) {
            Scheduler.FolderStatus s = controller.getStatus(folder);
            boolean hasSize = s.sizeBytes() >= 0;
            String sizeText = hasSize ? Fmt.gb(s.sizeBytes()) : "—";
            size.setText(sizeText);
            path.setText(fitPath(l, folder.toString(), sizeText));
            status.setText(statusText(s));
            setToolTipText("<html>" + escape(folder.toString()) + "<br>" + escape(statusText(s)) + "</html>");
            setBackground(selected ? Theme.PANEL_PRESSED : Theme.PANEL);
            path.setForeground(selected ? Theme.ACCENT : Theme.TEXT);
            size.setForeground(!hasSize ? Theme.TEXT_DIM : (selected ? Theme.ACCENT : Theme.VALUE));
            status.setForeground(switch (s.state()) {
                case ERROR -> Theme.WARN;
                case SCANNING -> Theme.VALUE;
                case STOPPED -> Theme.TEXT_DIM;
                case WAITING -> Theme.ACCENT_DIM;
            });
            return this;
        }
    }
}
