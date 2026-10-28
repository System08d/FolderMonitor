package foldermonitor;

import java.util.Locale;

// Форматирование чисел для экрана. Размер везде показываем как 9,4 ГБ
// (одна цифра после запятой, запятая как разделитель), независимо от языка Windows.
public final class Fmt {

    private static final double BYTES_IN_GB = 1024.0 * 1024.0 * 1024.0;

    private Fmt() { }

    // "9,4"
    public static String gbNumber(long bytes) {
        return oneDecimal(bytes / BYTES_IN_GB);
    }

    // "9,4 ГБ"
    public static String gb(long bytes) {
        return gbNumber(bytes) + " ГБ";
    }

    // "+0,4 ГБ" / "-1,2 ГБ" / "0,0 ГБ" (если после округления получился ноль, знак не пишем)
    public static String deltaGb(long deltaBytes) {
        String number = oneDecimal(Math.abs(deltaBytes) / BYTES_IN_GB);
        if (number.equals("0,0")) {
            return "0,0 ГБ";
        }
        return (deltaBytes > 0 ? "+" : "-") + number + " ГБ";
    }

    // "12 345"
    public static String count(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }

    private static String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value).replace('.', ',');
    }
}
