package foldermonitor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

// Экран 3: график на всё окно, сверху карточки с числами.
// CSV читается в фоне, пока видна надпись "ЗАГРУЗКА". Экран создаётся заново при каждом открытии
// и убирается (ScreenHost.release) при выходе, поэтому данные не копятся в памяти.
public class ChartScreen extends JPanel {

    private final ChartView chart;
    private final ChartPeriod period;
    private final JPanel top = new JPanel(new BorderLayout(0, 10));
    private final JPanel center = new JPanel(new BorderLayout());

    public ChartScreen(ChartView chart, List<Path> folders, ChartPeriod period, DataProvider data,
            List<ChartView> alternatives, Consumer<ChartView> onSwitch, Runnable onBack) {
        super(new BorderLayout(0, 10));
        this.chart = chart;
        this.period = period;
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // ---- заголовок ----
        JLabel title = new JLabel(("График · " + chart.title() + " · " + period.label()).toUpperCase());
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.ACCENT);
        JLabel what = new JLabel(folders.size() == 1 ? folders.get(0).toString()
                : "Сравнение папок: " + folders.size());
        what.setFont(Theme.FONT_SMALL);
        what.setForeground(Theme.TEXT_DIM);
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(what);
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);

        // ---- график (пока загрузка) ----
        center.setOpaque(false);
        center.setBorder(HudBorder.of(chart.title(), 4));
        center.add(ChartStyle.message("ЗАГРУЗКА…"), BorderLayout.CENTER);

        // ---- кнопки ----
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        ThemedButton back = new ThemedButton("Назад");
        back.addActionListener(e -> onBack.run());
        ThemedButton change = new ThemedButton("Сменить график");
        change.addActionListener(e -> showChartMenu(change, alternatives, onSwitch));
        change.setEnabled(alternatives.size() > 1);
        buttons.add(back);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(change);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        load(folders, data);
    }

    // Чтение CSV в фоновом потоке, чтобы окно не зависало
    private void load(List<Path> folders, DataProvider data) {
        new SwingWorker<List<FolderHistory>, Void>() {
            @Override
            protected List<FolderHistory> doInBackground() throws Exception {
                List<FolderHistory> result = new ArrayList<>();
                for (Path folder : folders) {
                    result.add(data.load(folder));
                }
                return result;
            }

            @Override
            protected void done() {
                try {
                    showChart(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    AppLog.error("Не удалось прочитать историю для графика", e.getCause());
                    showText("Не удалось прочитать данные: " + e.getCause().getMessage());
                }
            }
        }.execute();
    }

    private void showChart(List<FolderHistory> histories) {
        try {
            if (histories.size() == 1) {
                top.add(new StatCards(histories.get(0), period), BorderLayout.CENTER);
            }
            center.removeAll();
            center.add(chart.create(histories, period), BorderLayout.CENTER);
        } catch (RuntimeException e) {
            AppLog.error("Ошибка при построении графика " + chart.title(), e);
            showText("Не удалось построить график: " + e);
            return;
        }
        revalidate();
        repaint();
    }

    private void showText(String text) {
        center.removeAll();
        JLabel label = ChartStyle.message(text);
        label.setForeground(Theme.WARN);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        center.add(label, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // Меню со списком графиков: выбор сразу открывает другой график с тем же периодом
    private void showChartMenu(ThemedButton anchor, List<ChartView> alternatives,
            Consumer<ChartView> onSwitch) {
        JPopupMenu menu = new JPopupMenu();
        for (ChartView other : alternatives) {
            JMenuItem item = new JMenuItem((other == chart ? "• " : "  ") + other.title());
            item.setEnabled(other != chart);
            item.addActionListener(e -> onSwitch.accept(other));
            menu.add(item);
        }
        menu.show(anchor, 0, -menu.getPreferredSize().height);   // над кнопкой, она внизу окна
    }
}
