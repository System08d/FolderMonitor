package foldermonitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

// Карточки с числами над графиком: текущий размер, изменение за неделю и за месяц,
// количество измерений. Показываются для одной папки.
public class StatCards extends JPanel {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public StatCards(FolderHistory history, ChartPeriod period) {
        super(new GridLayout(1, 4, 8, 0));
        setOpaque(false);

        Optional<Measurement> latest = history.latestRoot();
        if (latest.isEmpty()) {
            add(card("Текущий размер", "—", "измерений ещё нет", Theme.TEXT_DIM));
            add(card("За неделю", "—", "", Theme.TEXT_DIM));
            add(card("За месяц", "—", "", Theme.TEXT_DIM));
            add(card("Измерений", "0", "", Theme.TEXT_DIM));
            return;
        }

        Measurement now = latest.get();
        add(card("Текущий размер", Fmt.gb(now.sizeBytes()),
                "на " + DATE_TIME.format(now.time()), Theme.VALUE));
        add(deltaCard("За неделю", history, now, 7));
        add(deltaCard("За месяц", history, now, 30));
        add(card("Измерений", Fmt.count(history.root(period).size()),
                "за период, всего " + Fmt.count(history.root().size()), Theme.ACCENT));
    }

    // Изменение размера: последнее измерение минус измерение "N дней назад" (или ближайшее более раннее)
    private static JPanel deltaCard(String title, FolderHistory history, Measurement now, int days) {
        Optional<Measurement> before = history.rootAtOrBefore(now.time().minusDays(days));
        if (before.isEmpty()) {
            return card(title, "—", "пока мало истории", Theme.TEXT_DIM);
        }
        long delta = now.sizeBytes() - before.get().sizeBytes();
        Color color = delta > 0 ? Theme.VALUE : (delta < 0 ? Theme.WARN : Theme.TEXT);
        return card(title, Fmt.deltaGb(delta), "с " + DATE.format(before.get().time()), color);
    }

    private static JPanel card(String title, String value, String note, Color valueColor) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);
        panel.setBorder(HudBorder.of(title, 6));

        JLabel valueLabel = new JLabel(value, SwingConstants.LEFT);
        valueLabel.setFont(Theme.FONT_BIG);
        valueLabel.setForeground(valueColor);

        JLabel noteLabel = new JLabel(note, SwingConstants.LEFT);
        noteLabel.setFont(Theme.FONT_SMALL);
        noteLabel.setForeground(Theme.TEXT_DIM);

        panel.add(valueLabel, BorderLayout.CENTER);
        panel.add(noteLabel, BorderLayout.SOUTH);
        return panel;
    }
}
