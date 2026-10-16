/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StatsStorage {

    private static final String SEP = ";";
    private static final String HEADER =
            "Дата;Объект;Размер_байт;Файлов;Папок;Изображений;Видео;Прочих";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path dataDir;

    public StatsStorage(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);   // создаст папку, если её нет
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

        if (!Files.exists(file) || Files.size(file) == 0) {
            lines.add("\uFEFF" + HEADER);   // \uFEFF = BOM, чтобы Excel понял UTF-8
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

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {   // i = 1: первую строку (заголовок) пропускаем
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            try {
                result.add(parseLine(line));
            } catch (RuntimeException e) {
                System.out.println("Пропущена повреждённая строка " + (i + 1)
                        + " в " + file.getFileName() + ": " + e.getMessage());
            }
        }
        return result;
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
        return new Measurement(
                LocalDateTime.parse(p[0], TIME_FORMAT),
                p[1],
                Long.parseLong(p[2]),
                Long.parseLong(p[3]),
                Long.parseLong(p[4]),
                parseOptional(p[5]),
                parseOptional(p[6]),
                parseOptional(p[7]));
    }

    private static long parseOptional(String s) {
        return s.isEmpty() ? Measurement.NO_DATA : Long.parseLong(s);
    }
}