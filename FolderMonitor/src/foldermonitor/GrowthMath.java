package foldermonitor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

// Расчёты роста по дням. Чистая арифметика без JFreeChart и Swing, поэтому легко проверяется.
//
// Главное правило: если за какой-то день измерения нет (компьютер был выключен, обход не удался),
// берём последнее известное значение. В CSV при этом ничего не дописывается, подстановка делается
// только здесь, в момент расчёта.
public final class GrowthMath {

    private GrowthMath() { }

    // Значение показателя на конец дня
    public record DayValue(LocalDate day, double value) { }

    // Значения по одному на каждый день от первого до последнего измерения. rows: измерения самой папки,
    // отсортированные по времени. Если в дне несколько измерений, берём последнее. День без измерения
    // получает значение предыдущего дня. До первого измерения и после последнего дней нет.
    public static List<DayValue> dailyValues(List<Measurement> rows, ToDoubleFunction<Measurement> metric) {
        List<DayValue> result = new ArrayList<>();
        if (rows.isEmpty()) {
            return result;
        }
        LocalDate first = rows.get(0).time().toLocalDate();
        LocalDate last = rows.get(rows.size() - 1).time().toLocalDate();
        double current = metric.applyAsDouble(rows.get(0));
        int next = 0;
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            while (next < rows.size() && !rows.get(next).time().toLocalDate().isAfter(day)) {
                current = metric.applyAsDouble(rows.get(next));
                next++;
            }
            result.add(new DayValue(day, current));
        }
        return result;
    }

    // Прирост за день: значение этого дня минус значение предыдущего. У первого дня прироста нет.
    public static List<DayValue> growth(List<DayValue> daily) {
        List<DayValue> result = new ArrayList<>();
        for (int i = 1; i < daily.size(); i++) {
            result.add(new DayValue(daily.get(i).day(), daily.get(i).value() - daily.get(i - 1).value()));
        }
        return result;
    }

    // Скользящее среднее за последние windowDays дней (включая текущий). В самом начале, пока
    // накоплено меньше дней, усредняем то, что есть.
    public static List<DayValue> movingAverage(List<DayValue> series, int windowDays) {
        List<DayValue> result = new ArrayList<>();
        for (int i = 0; i < series.size(); i++) {
            int from = Math.max(0, i - windowDays + 1);
            double sum = 0;
            for (int k = from; k <= i; k++) {
                sum += series.get(k).value();
            }
            result.add(new DayValue(series.get(i).day(), sum / (i - from + 1)));
        }
        return result;
    }

    // Имя и число (подпапка и её прирост)
    public record NamedValue(String name, double value) { }

    // Рейтинг по приросту: для каждой подпапки конец минус начало, по убыванию (самый быстрый рост первым).
    // Подпапки, которой в какой-то момент нет в данных (например, она ещё не дошла до порога размера),
    // в этот момент приписывается 0. Это принятое правило для пропущенных значений.
    public static List<NamedValue> ranking(Map<String, Double> start, Map<String, Double> end) {
        Set<String> names = new HashSet<>(start.keySet());
        names.addAll(end.keySet());
        List<NamedValue> result = new ArrayList<>();
        for (String name : names) {
            result.add(new NamedValue(name, end.getOrDefault(name, 0.0) - start.getOrDefault(name, 0.0)));
        }
        // при равном приросте порядок по имени, чтобы список не прыгал от запуска к запуску
        result.sort(Comparator.comparingDouble(NamedValue::value).reversed()
                .thenComparing(NamedValue::name));
        return result;
    }

    // Оставить только дни, входящие в период
    public static List<DayValue> inPeriod(List<DayValue> series, ChartPeriod period) {
        List<DayValue> result = new ArrayList<>();
        for (DayValue v : series) {
            if (period.contains(v.day().atStartOfDay())) {
                result.add(v);
            }
        }
        return result;
    }

    // База индекса роста: первое ненулевое значение. Если значение в начале периода равно нулю,
    // делить на него нельзя, поэтому берём первое, которое не ноль. Нет такого: возвращаем 0.
    public static double indexBase(List<Measurement> rows, ToDoubleFunction<Measurement> metric) {
        for (Measurement m : rows) {
            double v = metric.applyAsDouble(m);
            if (v != 0) {
                return v;
            }
        }
        return 0;
    }
}
