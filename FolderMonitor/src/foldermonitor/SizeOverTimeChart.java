package foldermonitor;

// График 1.1: размер папки (ГБ) по времени.
public class SizeOverTimeChart extends LineChartBase {

    @Override
    public String title() {
        return "Размер по времени";
    }

    @Override
    public String description() {
        return "Как менялся размер папки (ГБ)";
    }

    @Override
    protected String seriesName() {
        return "Размер, ГБ";
    }

    @Override
    protected String axisLabel() {
        return "ГБ";
    }

    @Override
    protected String unitText() {
        return " ГБ";
    }

    @Override
    protected double valueOf(Measurement m) {
        return Fmt.gbValue(m.sizeBytes());
    }

    @Override
    protected boolean wholeNumbers() {
        return false;
    }

    @Override
    protected double minAxisSpan() {
        return 0.2;
    }
}
