package foldermonitor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

// Экран 1: выбор папки (или нескольких, для сравнения), графики которой хотим посмотреть.
// В списке и наблюдаемые папки, и убранные из наблюдения, у которых осталась история.
public class FolderSelectScreen extends JPanel {

    private final JList<DataProvider.KnownFolder> list;

    public FolderSelectScreen(List<DataProvider.KnownFolder> folders, List<Path> preselected,
            Consumer<List<Path>> onNext, Runnable onBack) {
        super(new BorderLayout(0, 12));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("ЭКРАН 1 · ВЫБОР ПАПКИ");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.ACCENT);

        DefaultListModel<DataProvider.KnownFolder> model = new DefaultListModel<>();
        folders.forEach(model::addElement);
        list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setCellRenderer(new Renderer());
        for (int i = 0; i < model.size(); i++) {
            if (preselected.contains(model.get(i).path())) {
                list.addSelectionInterval(i, i);
            }
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.PANEL);

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.setBorder(HudBorder.of("Папки с историей"));
        JLabel hint = new JLabel("<html>Одна папка: графики по ней. Несколько (Ctrl или Shift): "
                + "сравнение папок.</html>");
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.TEXT_DIM);
        body.add(hint, BorderLayout.NORTH);
        if (folders.isEmpty()) {
            JLabel empty = ChartStyle.message("<html><center>Истории пока нет.<br>"
                    + "Добавьте папку на главном экране и дождитесь первого измерения.</center></html>");
            body.add(empty, BorderLayout.CENTER);
        } else {
            body.add(scroll, BorderLayout.CENTER);
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        ThemedButton back = new ThemedButton("Назад");
        back.addActionListener(e -> onBack.run());
        ThemedButton next = new ThemedButton("Далее");
        next.setEnabled(!list.isSelectionEmpty());
        next.addActionListener(e -> {
            List<Path> chosen = new ArrayList<>();
            for (DataProvider.KnownFolder k : list.getSelectedValuesList()) {
                chosen.add(k.path());
            }
            onNext.accept(chosen);
        });
        list.addListSelectionListener(e -> next.setEnabled(!list.isSelectionEmpty()));
        buttons.add(back);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(next);

        add(title, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private static class Renderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                boolean selected, boolean focus) {
            super.getListCellRendererComponent(l, value, index, selected, focus);
            DataProvider.KnownFolder folder = (DataProvider.KnownFolder) value;
            setText(folder.path().toString()
                    + (folder.watched() ? "" : "   [не наблюдается, есть история]"));
            setFont(Theme.FONT_NORMAL);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
            if (selected) {
                setBackground(Theme.PANEL_PRESSED);
                setForeground(Theme.ACCENT);
            } else {
                setBackground(Theme.PANEL);
                setForeground(folder.watched() ? Theme.TEXT : Theme.TEXT_DIM);
            }
            return this;
        }
    }
}
