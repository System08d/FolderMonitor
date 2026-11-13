package foldermonitor;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;

// График 5.2: динамика размера (ГБ) нескольких папок, по линии на папку.
public class FoldersSizeLinesChart extends MultiLineChartBase {

    @Override
    public String title() {
        return "Динамика размера папок";
    }

    @Override
    public String description() {
        return "Размер выбранных папок по времени, линии";
    }

    @Override
    protected TimeSeries buildSeries(String name, List<Measurement> rows) {
        TimeSeries series = new TimeSeries(name);
        for (Measurement m : rows) {
            Date date = Date.from(m.time().atZone(ZoneId.systemDefault()).toInstant());
            series.addOrUpdate(new Second(date), Fmt.gbValue(m.sizeBytes()));
        }
        return series;
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
    protected Double referenceValue() {
        return null;
    }

    @Override
    protected double minAxisSpan() {
        return 0.2;
    }
}
