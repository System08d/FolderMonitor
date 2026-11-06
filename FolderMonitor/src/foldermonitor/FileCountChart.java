package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

// График 1.2: количество файлов по времени (всего, включая вложенные папки).
public class FileCountChart extends LineChartBase {

    @Override
    public String title() {
        return "Количество файлов по времени";
    }

    @Override
    public String description() {
        return "Сколько файлов в папке, включая вложенные";
    }

    @Override
    protected String seriesName() {
        return "Файлов";
    }

    @Override
    protected String axisLabel() {
        return "файлов";
    }

    @Override
    protected String unitText() {
        return " файлов";
    }

    @Override
    protected double valueOf(Measurement m) {
        return m.fileCount();
    }

    @Override
    protected boolean wholeNumbers() {
        return true;
    }

    @Override
    protected double minAxisSpan() {
        return 5;
    }
}

