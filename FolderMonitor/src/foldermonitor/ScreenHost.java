/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.awt.CardLayout;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;

// «Стопка карточек»: в каждый момент на экране виден один экран, остальные спрятаны.
// Это содержимое окна (MainFrame).
public class ScreenHost extends JPanel {

    private final CardLayout layout = new CardLayout();
    private final Map<Screen, JComponent> components = new EnumMap<>(Screen.class);
    private Screen current;   // какой экран виден сейчас (null, если ни один)

    public ScreenHost() {
        setLayout(layout);
        setBackground(Theme.BG);
    }

    // Добавить экран. Если экран с таким id уже был, он заменяется новым:
    // так экран графика можно создавать заново при каждом открытии, а старый уходит в сборщик мусора.
    public void setScreen(Screen id, JComponent component) {
        JComponent old = components.put(id, component);
        if (old != null) {
            remove(old);
        }
        add(component, id.name());
        if (id == current) {
            layout.show(this, id.name());   // заменили видимый экран: сразу показываем новый
        }
        revalidate();
        repaint();
    }

    // Показать экран
    public void show(Screen id) {
        if (!components.containsKey(id)) {
            throw new IllegalArgumentException("Экран не зарегистрирован: " + id);
        }
        layout.show(this, id.name());
        current = id;
    }

    // Убрать экран совсем и освободить память (понадобится для экрана графика)
    public void release(Screen id) {
        JComponent old = components.remove(id);
        if (old != null) {
            remove(old);
            if (id == current) {
                current = null;
            }
            revalidate();
            repaint();
        }
    }

    public Screen getCurrent() {
        return current;
    }
}