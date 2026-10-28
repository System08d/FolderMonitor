package foldermonitor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Период, за который показываем график. from/to == null означает "без ограничения".
// Границы включительно и считаются по календарным дням.
public record ChartPeriod(Kind kind, LocalDate from, LocalDate to) {

    public enum Kind { DAYS_7, DAYS_30, DAYS_90, ALL, CUSTOM }

    // По умолчанию — всё время
    public static ChartPeriod all() {
        return new ChartPeriod(Kind.ALL, null, null);
    }

    // "7 дней" = сегодня и 6 предыдущих дней; то же для 30 и 90
    public static ChartPeriod last(Kind kind) {
        int days = switch (kind) {
            case DAYS_7 -> 7;
            case DAYS_30 -> 30;
            case DAYS_90 -> 90;
            default -> throw new IllegalArgumentException("Не относительный период: " + kind);
        };
        return new ChartPeriod(kind, LocalDate.now().minusDays(days - 1), null);
    }

    public static ChartPeriod custom(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Начало периода позже конца");
        }
        return new ChartPeriod(Kind.CUSTOM, from, to);
    }

    public boolean contains(LocalDateTime time) {
        LocalDate day = time.toLocalDate();
        if (from != null && day.isBefore(from)) {
            return false;
        }
        return to == null || !day.isAfter(to);
    }

    public String label() {
        return switch (kind) {
            case DAYS_7 -> "7 дней";
            case DAYS_30 -> "30 дней";
            case DAYS_90 -> "90 дней";
            case ALL -> "всё время";
            case CUSTOM -> from + " — " + to;
        };
    }
}
