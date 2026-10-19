/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class StatsStorage {

    private static final String SEP = ";";
    private static final String HEADER =
            "Дата;Объект;Размер_байт;Файлов;Папок;Изображений;Видео;Прочих";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path dataDir;

    public StatsStorage(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);
        if (!Files.isWritable(dataDir)) {
            throw new IOException("Нет прав на запись в папку данных: " + dataDir);
        }
        this.dataDir = dataDir;
    }

    // Имя CSV-файла для наблюдаемой папки: <имя папки>_<короткий код пути>.csv
    public Path fileFor(Path watchedFolder) {
        Path full = watchedFolder.toAbsolutePath().normalize();
        Path last = full.getFileName();   // у корня диска (C:\) его нет
        String baseName = (last != null) ? last.toString() : "disk";
        baseName = baseName.replaceAll("[^\\p{L}\\p{N}._-]", "_");
        String hash = Integer.toHexString(full.toString().toLowerCase(Locale.ROOT).hashCode());
        return dataDir.resolve(baseName + "_" + hash + ".csv");
    }

    // Дописать строки в конец файла (файл целиком не перезаписывается)
    public void append(Path watchedFolder, List<Measurement> rows) throws IOException {
        Path file = fileFor(watchedFolder);
        List<String> lines = new ArrayList<>();

        boolean isNewFile = !Files.exists(file) || Files.size(file) == 0;
        if (isNewFile) {
            lines.add("\uFEFF" + HEADER);   // \uFEFF = BOM, чтобы Excel понял UTF-8
        } else if (!endsWithNewline(file)) {
            // ИЗМЕНЕНО: прошлая запись оборвалась на середине строки.
            // Пустая строка закроет её, и новые данные не склеятся с мусором.
            lines.add("");
        }
        for (Measurement m : rows) {
            lines.add(toLine(m));
        }

        Files.write(file, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // Прочитать всю историю папки. Если файла ещё нет — пустой список.
    public List<Measurement> read(Path watchedFolder) throws IOException {
        Path file = fileFor(watchedFolder);
        List<Measurement> result = new ArrayList<>();
        if (!Files.exists(file)) {
            return result;
        }

        // ИЗМЕНЕНО: читаем построчно, а не readAllLines (тот падает на битых байтах)
        try (BufferedReader reader = openReader(file)) {
            reader.readLine();   // первая строка — заголовок, пропускаем
            int lineNumber = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    result.add(parseLine(line));
                } catch (RuntimeException e) {
                    System.out.println("Пропущена повреждённая строка " + lineNumber
                            + " в " + file.getFileName() + ": " + e.getMessage());
                }
            }
        }
        return result;
    }

    // НОВОЕ: время последнего измерения (или пусто, если измерений нет).
    // Читает файл потоком и не хранит строки в памяти — подходит для фоновой работы.
    public Optional<LocalDateTime> lastTime(Path watchedFolder) throws IOException {
        Path file = fileFor(watchedFolder);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        LocalDateTime last = null;
        try (BufferedReader reader = openReader(file)) {
            reader.readLine();   // заголовок
            String line;
            while ((line = reader.readLine()) != null) {
                int end = line.indexOf(SEP);
                if (end < 0) {
                    continue;
                }
                try {
                    LocalDateTime t = LocalDateTime.parse(line.substring(0, end), TIME_FORMAT);
                    if (last == null || t.isAfter(last)) {
                        last = t;
                    }
                } catch (RuntimeException e) {
                    // повреждённая строка — пропускаем
                }
            }
        }
        return Optional.ofNullable(last);
    }

    // НОВОЕ: читатель, который заменяет битые байты знаком «�» вместо ошибки
    private static BufferedReader openReader(Path file) throws IOException {
        return new BufferedReader(
                new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8));
    }

    // НОВОЕ: заканчивается ли файл переводом строки (смотрим только последний байт)
    private static boolean endsWithNewline(Path file) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            long size = channel.size();
            if (size == 0) {
                return true;
            }
            channel.position(size - 1);
            ByteBuffer buffer = ByteBuffer.allocate(1);
            channel.read(buffer);
            return buffer.get(0) == '\n';
        }
    }

    private static String toLine(Measurement m) {
        String images = m.hasTypes() ? String.valueOf(m.imageCount()) : "";
        String videos = m.hasTypes() ? String.valueOf(m.videoCount()) : "";
        String others = m.hasTypes() ? String.valueOf(m.otherCount()) : "";
        return String.join(SEP,
                TIME_FORMAT.format(m.time()),
                m.name().replace(';', ','),
                String.valueOf(m.sizeBytes()),
                String.valueOf(m.fileCount()),
                String.valueOf(m.folderCount()),
                images, videos, others);
    }

    private static Measurement parseLine(String line) {
        String[] p = line.split(SEP, -1);   // -1: не терять пустые поля в конце строки
        if (p.length != 8) {
            throw new IllegalArgumentException("ожидалось 8 полей, найдено " + p.length);
        }
        Measurement m = new Measurement(
                LocalDateTime.parse(p[0], TIME_FORMAT),
                p[1],
                Long.parseLong(p[2]),
                Long.parseLong(p[3]),
                Long.parseLong(p[4]),
                parseOptional(p[5]),
                parseOptional(p[6]),
                parseOptional(p[7]));

        // ИЗМЕНЕНО: проверка целостности. Если последнее число обрезалось,
        // сумма не сойдётся, и строка отбрасывается.
        if (m.hasTypes()
                && m.fileCount() != m.imageCount() + m.videoCount() + m.otherCount()) {
            throw new IllegalArgumentException("не сходится количество файлов");
        }
        return m;
    }

    private static long parseOptional(String s) {
        return s.isEmpty() ? Measurement.NO_DATA : Long.parseLong(s);
    }
}