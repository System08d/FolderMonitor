/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;

// Плоская кнопка с тонкой рамкой. Все кнопки приложения создаём через неё,
// тогда стиль кнопок меняется в одном месте (финальный вид — этап 7).
public class ThemedButton extends JButton {

    public ThemedButton(String text) {
        super(text);
        setFont(Theme.FONT_NORMAL);
        // Рисовать всё будем сами в paintComponent, стандартное оформление Windows отключаем
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);   // чтобы кнопка знала, что над ней курсор
        setBorder(BorderFactory.createEmptyBorder(7, 18, 7, 18));   // отступы вокруг текста
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();   // копия, чтобы не испортить настройки Swing
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            ButtonModel model = getModel();
            Color fill;
            Color line;
            Color text;
            if (!isEnabled()) {
                fill = Theme.BG;
                line = Theme.GRID;
                text = Theme.TEXT_DIM;
            } else if (model.isPressed()) {
                fill = Theme.PANEL_PRESSED;
                line = Theme.ACCENT;
                text = Theme.ACCENT;
            } else if (model.isRollover() || isFocusOwner()) {
                fill = Theme.PANEL_HOVER;
                line = Theme.ACCENT;   // подсветка рамки при наведении и при фокусе с клавиатуры
                text = Theme.ACCENT;
            } else {
                fill = Theme.PANEL;
                line = Theme.BORDER;
                text = Theme.ACCENT;
            }

            g2.setColor(fill);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(line);
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);   // рамка толщиной 1 px

            String label = getText();
            if (label != null && !label.isEmpty()) {
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(label)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(text);
                g2.drawString(label, x, y);
            }
        } finally {
            g2.dispose();
        }
    }
}