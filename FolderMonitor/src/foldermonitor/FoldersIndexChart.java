package foldermonitor;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;

// График 5.3: индекс роста размера нескольких папок. У каждой папки 100% = её размер в начале периода
// (первое ненулевое значение), так что видно, чей рост быстрее в процентах, как бы ни отличались размеры.
public class FoldersIndexChart extends MultiLineChartBase {

    @Override
    public String title() {
        return "Индекс роста папок";
    }

    @Override
    public String description() {
        return "У какой папки размер растёт быстрее в процентах";
    }

    @Override
    protected TimeSeries buildSeries(String name, List<Measurement> rows) {
        double base = GrowthMath.indexBase(rows, m -> (double) m.sizeBytes());
        if (base == 0) {
            return null;                    // размер всегда 0: индекс не посчитать
        }
        TimeSeries series = new TimeSeries(name);
        boolean started = false;
        for (Measurement m : rows) {
            if (!started && m.sizeBytes() == 0) {
                continue;                   // до первого ненулевого значения индекс не определён
            }
            started = true;
            Date date = Date.from(m.time().atZone(ZoneId.systemDefault()).toInstant());
            series.addOrUpdate(new Second(date), m.sizeBytes() / base * 100.0);
        }
        return series;
    }

    @Override
    protected String axisLabel() {
        return "% от начала периода";
    }

    @Override
    protected String unitText() {
        return " %";
    }

    @Override
    protected Double referenceValue() {
        return 100.0;
    }

    @Override
    protected double minAxisSpan() {
        return 2.0;
    }
}
