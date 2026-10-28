package foldermonitor;

import java.util.List;
import javax.swing.JComponent;

// Общий интерфейс графика. Каждый график — отдельный класс, который его реализует.
// Чтобы добавить новый график, достаточно написать класс и внести его в ChartRegistry.
public interface ChartView {

    // Название в списке графиков и в заголовке
    String title();

    // Одна короткая строка под названием на экране выбора
    String description();

    // false = график по одной папке; true = сравнение нескольких (группа 5)
    boolean forSeveralFolders();

    // Строит компонент с графиком. Вызывается в потоке окна; данные уже прочитаны.
    JComponent create(List<FolderHistory> histories, ChartPeriod period);
}
