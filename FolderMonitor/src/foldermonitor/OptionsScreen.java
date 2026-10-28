package foldermonitor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

// Экран 2: тип графика и период. По умолчанию период — всё время.
public class OptionsScreen extends JPanel {

    private final JList<ChartView> chartList;
    private final JRadioButton days7 = radio("7 дней");
    private final JRadioButton days30 = radio("30 дней");
    private final JRadioButton days90 = radio("90 дней");
    private final JRadioButton allTime = radio("Всё время");
    private final JRadioButton custom = radio("Свой период");
    private final JTextField fromField = new JTextField(10);
    private final JTextField toField = new JTextField(10);
    private final JLabel error = new JLabel(" ");

    public OptionsScreen(List<Path> folders, List<ChartView> charts, ChartView preselectedChart,
            ChartPeriod preselectedPeriod, BiConsumer<ChartView, ChartPeriod> onShow, Runnable onBack) {
        super(new BorderLayout(0, 12));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("ЭКРАН 2 · ТИП ГРАФИКА И ПЕРИОД");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.ACCENT);

        JLabel what = new JLabel(folders.size() == 1 ? folders.get(0).toString()
                : "Сравнение папок: " + folders.size());
        what.setFont(Theme.FONT_SMALL);
        what.setForeground(Theme.TEXT_DIM);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.NORTH);
        header.add(what, BorderLayout.SOUTH);

        // ---- левая панель: список графиков ----
        DefaultListModel<ChartView> model = new DefaultListModel<>();
        charts.forEach(model::addElement);
        chartList = new JList<>(model);
        chartList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chartList.setCellRenderer(new ChartRenderer());
        if (preselectedChart != null && charts.contains(preselectedChart)) {
            chartList.setSelectedValue(preselectedChart, true);
        } else if (!charts.isEmpty()) {
            chartList.setSelectedIndex(0);
        }
        JScrollPane scroll = new JScrollPane(chartList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.PANEL);

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setBorder(HudBorder.of("График"));
        if (charts.isEmpty()) {
            left.add(ChartStyle.message("<html><center>Для такого выбора папок<br>"
                    + "графиков пока нет.</center></html>"), BorderLayout.CENTER);
        } else {
            left.add(scroll, BorderLayout.CENTER);
        }

        // ---- правая панель: период ----
        ButtonGroup group = new ButtonGroup();
        for (JRadioButton b : List.of(days7, days30, days90, allTime, custom)) {
            group.add(b);
        }
        styleField(fromField);
        styleField(toField);
        LocalDate today = LocalDate.now();
        fromField.setText(today.minusDays(30).toString());
        toField.setText(today.toString());
        applyPeriod(preselectedPeriod);
        custom.addActionListener(e -> updateCustomEnabled());
        for (JRadioButton b : List.of(days7, days30, days90, allTime)) {
            b.addActionListener(e -> updateCustomEnabled());
        }
        updateCustomEnabled();

        JPanel customRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        customRow.setOpaque(false);
        customRow.add(label("с"));
        customRow.add(fromField);
        customRow.add(label("по"));
        customRow.add(toField);
        JLabel format = label("формат ГГГГ-ММ-ДД");
        format.setForeground(Theme.TEXT_DIM);
        error.setFont(Theme.FONT_SMALL);
        error.setForeground(Theme.WARN);

        JPanel periodBox = new JPanel();
        periodBox.setOpaque(false);
        periodBox.setLayout(new BoxLayout(periodBox, BoxLayout.Y_AXIS));
        for (JRadioButton b : List.of(days7, days30, days90, allTime, custom)) {
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            periodBox.add(b);
        }
        periodBox.add(Box.createVerticalStrut(6));
        periodBox.add(aligned(customRow));
        periodBox.add(aligned(format));
        periodBox.add(Box.createVerticalStrut(6));
        periodBox.add(aligned(error));

        // BorderLayout.NORTH: строки не растягиваются по высоте, а прижимаются к верху
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.setBorder(HudBorder.of("Период"));
        right.add(periodBox, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));
        center.setOpaque(false);
        center.add(left);
        center.add(right);

        // ---- кнопки ----
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        ThemedButton back = new ThemedButton("Назад");
        back.addActionListener(e -> onBack.run());
        ThemedButton show = new ThemedButton("Показать график");
        show.setEnabled(!charts.isEmpty());
        show.addActionListener(e -> {
            ChartView chart = chartList.getSelectedValue();
            ChartPeriod period = readPeriod();
            if (chart != null && period != null) {
                onShow.accept(chart, period);
            }
        });
        buttons.add(back);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(show);

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    // Возвращает выбранный период или null, если свой период введён неверно (тогда показана ошибка)
    private ChartPeriod readPeriod() {
        error.setText(" ");
        if (days7.isSelected()) {
            return ChartPeriod.last(ChartPeriod.Kind.DAYS_7);
        }
        if (days30.isSelected()) {
            return ChartPeriod.last(ChartPeriod.Kind.DAYS_30);
        }
        if (days90.isSelected()) {
            return ChartPeriod.last(ChartPeriod.Kind.DAYS_90);
        }
        if (allTime.isSelected()) {
            return ChartPeriod.all();
        }
        try {
            LocalDate from = LocalDate.parse(fromField.getText().trim());
            LocalDate to = LocalDate.parse(toField.getText().trim());
            if (from.isAfter(to)) {
                error.setText("Начало периода позже конца");
                return null;
            }
            return ChartPeriod.custom(from, to);
        } catch (DateTimeParseException e) {
            error.setText("Дата должна быть в формате ГГГГ-ММ-ДД");
            return null;
        }
    }

    private void applyPeriod(ChartPeriod period) {
        switch (period.kind()) {
            case DAYS_7 -> days7.setSelected(true);
            case DAYS_30 -> days30.setSelected(true);
            case DAYS_90 -> days90.setSelected(true);
            case CUSTOM -> {
                custom.setSelected(true);
                fromField.setText(period.from().toString());
                toField.setText(period.to().toString());
            }
            default -> allTime.setSelected(true);
        }
    }

    private void updateCustomEnabled() {
        fromField.setEnabled(custom.isSelected());
        toField.setEnabled(custom.isSelected());
    }

    private static JRadioButton radio(String text) {
        JRadioButton button = new JRadioButton(text);
        button.setOpaque(false);
        button.setForeground(Theme.TEXT);
        button.setFont(Theme.FONT_NORMAL);
        button.setFocusPainted(false);
        return button;
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.FONT_NORMAL);
        label.setForeground(Theme.TEXT);
        return label;
    }

    private static void styleField(JTextField field) {
        field.setFont(Theme.FONT_NORMAL);
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.ACCENT);
    }

    // BoxLayout: чтобы всё прижималось влево
    private static JPanel aligned(Component component) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(component);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private static class ChartRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                boolean selected, boolean focus) {
            super.getListCellRendererComponent(l, value, index, selected, focus);
            ChartView chart = (ChartView) value;
            setFont(Theme.FONT_NORMAL);
            setText("<html><b>" + chart.title() + "</b><br>" + chart.description() + "</html>");
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            if (selected) {
                setBackground(Theme.PANEL_PRESSED);
                setForeground(Theme.ACCENT);
            } else {
                setBackground(Theme.PANEL);
                setForeground(Theme.TEXT);
            }
            return this;
        }
    }
}
