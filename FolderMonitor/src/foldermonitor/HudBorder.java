package foldermonitor;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.border.AbstractBorder;

// Рамка панели в стиле HUD: тонкая линия в 1 px и маленький заголовок ЗАГЛАВНЫМИ буквами
// в левом верхнем углу, который "разрывает" линию (как "ANALYSIS" и "ZONE 1" на образце).
public class HudBorder extends AbstractBorder {

    private static final int TITLE_HEIGHT = 18;

    private final String title;
    private final int pad;

    private HudBorder(String title, int pad) {
        this.title = title.toUpperCase();
        this.pad = pad;
    }

    public static HudBorder of(String title) {
        return new HudBorder(title, 10);
    }

    public static HudBorder of(String title, int pad) {
        return new HudBorder(title, pad);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(TITLE_HEIGHT + pad, pad + 1, pad + 1, pad + 1);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        Insets own = getBorderInsets(c);
        insets.set(own.top, own.left, own.bottom, own.right);
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(Theme.FONT_SMALL);
            FontMetrics fm = g2.getFontMetrics();

            int lineY = y + TITLE_HEIGHT / 2;
            g2.setColor(Theme.BORDER);
            g2.drawRect(x, lineY, width - 1, height - 1 - TITLE_HEIGHT / 2);

            int textX = x + 12;
            int textWidth = fm.stringWidth(title);
            g2.setColor(Theme.BG);   // стираем кусок линии под заголовком
            g2.fillRect(textX - 5, y, textWidth + 10, TITLE_HEIGHT);
            g2.setColor(Theme.ACCENT);
            g2.drawString(title, textX, y + (TITLE_HEIGHT - fm.getHeight()) / 2 + fm.getAscent());
        } finally {
            g2.dispose();
        }
    }
}
