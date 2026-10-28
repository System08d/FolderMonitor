package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import javax.swing.UIManager;

// Все цвета и шрифты приложения в одном месте.
// Значения ВРЕМЕННЫЕ (взяты из описания стиля в roadmap), финальные уточним на этапе 7.
// Чтобы поменять внешний вид, достаточно поменять числа здесь.
public final class Theme {

    private Theme() { }   // объекты не создаются: только static-поля

    // ---- цвета ----
    public static final Color BG            = new Color(0x071411);   // фон окна: почти чёрный сине-зелёный
    public static final Color PANEL         = new Color(0x0C1F1A);   // фон панелей и кнопок
    public static final Color PANEL_HOVER   = new Color(0x11302A);   // кнопка под курсором
    public static final Color PANEL_PRESSED = new Color(0x0F3B31);   // кнопка нажата
    public static final Color BORDER        = new Color(0x1F6B58);   // тонкая рамка (приглушённая)
    public static final Color GRID          = new Color(0x123029);   // сетка графиков, неактивные рамки
    public static final Color ACCENT        = new Color(0x1FE5B5);   // основной неоновый акцент
    public static final Color ACCENT_DIM    = new Color(0x2E8B6E);   // второй, приглушённый акцент
    public static final Color WARN          = new Color(0xE5484D);   // красный: ошибки, отрицательный прирост
    public static final Color VALUE         = new Color(0xB8F05C);   // подсветка чисел
    public static final Color TEXT          = new Color(0xA9CFC6);   // обычный текст
    public static final Color TEXT_DIM      = new Color(0x6F9A8F);   // второстепенный текст

    // ---- шрифты (моноширинные; размеры не мельче 12 пт, чтобы числа читались) ----
    private static final String MONO = detectMono();

    public static final Font FONT_SMALL  = new Font(MONO, Font.PLAIN, 12);   // подписи осей
    public static final Font FONT_NORMAL = new Font(MONO, Font.PLAIN, 13);   // обычный текст, кнопки
    public static final Font FONT_BOLD   = new Font(MONO, Font.BOLD, 13);
    public static final Font FONT_TITLE  = new Font(MONO, Font.BOLD, 14);    // заголовки панелей
    public static final Font FONT_BIG    = new Font(MONO, Font.BOLD, 22);    // крупные числа на карточках

    // НОВОЕ: тёмная тема FlatLaf в цветах приложения. Вызывать один раз в начале main,
    // до создания любых окон. Окна, диалоги, списки и галочки становятся тёмными сами.
        private static boolean lookAndFeelInstalled;

    // Включает тёмную тему. Вызывается перед созданием ПЕРВОГО окна или диалога, а не при старте
    // программы: пока приложение сидит в трее, FlatLaf и Swing в память не загружаются.
    public static synchronized void installLookAndFeel() {
        if (lookAndFeelInstalled) {
            return;   // повторные вызовы ничего не делают
        }
        lookAndFeelInstalled = true;
        try {
            Map<String, String> extra = new HashMap<>();
            extra.put("@background", hex(BG));
            extra.put("@foreground", hex(TEXT));
            extra.put("@accentColor", hex(ACCENT));
            FlatLaf.setGlobalExtraDefaults(extra);
            FlatDarkLaf.setup();

            // Плоский вид без скруглений, рамки и шрифт из Theme
            UIManager.put("defaultFont", FONT_NORMAL);
            UIManager.put("Component.arc", 0);
            UIManager.put("Button.arc", 0);
            UIManager.put("TextComponent.arc", 0);
            UIManager.put("CheckBox.arc", 0);
            UIManager.put("ScrollBar.thumbArc", 0);
            UIManager.put("Component.borderColor", BORDER);
            UIManager.put("Component.focusedBorderColor", ACCENT);
            UIManager.put("List.background", PANEL);
            UIManager.put("TextField.background", PANEL);
            UIManager.put("ComboBox.background", PANEL);
        } catch (Exception | LinkageError e) {
            // Например, нет папки lib рядом с .jar. Окно откроется в стандартном виде, причина будет в логе.
            AppLog.error("Не удалось включить тёмную тему FlatLaf, будет стандартный вид", e);
        }
    }

    private static String hex(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
    }

    // Consolas есть в Windows 7 и новее. Если её нет, Java вернёт другой шрифт, и берём стандартный моноширинный.
    private static String detectMono() {
        Font probe = new Font("Consolas", Font.PLAIN, 12);
        return "Consolas".equalsIgnoreCase(probe.getFamily()) ? "Consolas" : Font.MONOSPACED;
    }
}
