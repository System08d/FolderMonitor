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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class AppLog {

    private static final long MAX_BYTES = 1024 * 1024;   // 1 МБ на файл
    private static final int KEEP_FILES = 3;             // основной + 2 старых
    private static final String FILE_NAME = "folder-monitor.log";
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static Path dir;   // null, пока лог не инициализирован: тогда пишем только в консоль

    private AppLog() { }       // объекты создавать не нужно: класс только со static-методами

    public static synchronized void init(Path logDir) {
        try {
            Files.createDirectories(logDir);
            dir = logDir;
        } catch (IOException e) {
            System.out.println("Не удалось создать папку логов " + logDir + ": " + e.getMessage());
        }
    }

    public static void info(String text) {
        write("INFO ", text);
    }

    public static void warn(String text) {
        write("WARN ", text);
    }

    public static void error(String text, Throwable cause) {
        if (cause == null) {
            write("ERROR", text);
            return;
        }
        StringWriter trace = new StringWriter();
        cause.printStackTrace(new PrintWriter(trace));
        write("ERROR", text + System.lineSeparator() + trace.toString().trim());
    }

    // synchronized: пишут разные потоки (планировщик, окно), строки не должны перемешиваться
    private static synchronized void write(String level, String text) {
        String line = TIME.format(LocalDateTime.now()) + " " + level + " " + text;
        System.out.println(line);   // консоль NetBeans; у javaw консоли нет, это не мешает
        if (dir == null) {
            return;
        }
        try {
            Path file = dir.resolve(FILE_NAME);
            rotateIfNeeded(file);
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException e) {
            // Лог записать не удалось. Из-за этого программа падать не должна.
        }
    }

    // folder-monitor.log -> .1.log -> .2.log, самый старый удаляется
    private static void rotateIfNeeded(Path file) throws IOException {
        if (!Files.exists(file) || Files.size(file) < MAX_BYTES) {
            return;
        }
        Files.deleteIfExists(numbered(KEEP_FILES - 1));
        for (int i = KEEP_FILES - 2; i >= 1; i--) {
            Path from = numbered(i);
            if (Files.exists(from)) {
                Files.move(from, numbered(i + 1), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Files.move(file, numbered(1), StandardCopyOption.REPLACE_EXISTING);
    }

    private static Path numbered(int index) {
        return dir.resolve("folder-monitor." + index + ".log");
    }
}