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
import java.io.InterruptedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class FolderScanner {

    // Пороги для подпапок первого уровня (500 МБ считаем как 500 * 1024 * 1024)
    public static final long MIN_SUBFOLDER_BYTES = 500L * 1024 * 1024;
    public static final long MIN_SUBFOLDER_FILES = 500;

    public ScanResult scan(Path root) throws IOException {
        return scan(root, () -> false);
    }

    // cancelled — функция "пора остановиться?" (её вызывает обход по ходу работы)
    public ScanResult scan(Path root, BooleanSupplier cancelled) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException("Папка не найдена или недоступна: " + root);
        }

        ScanVisitor visitor = new ScanVisitor(root, cancelled);
        Files.walkFileTree(root, visitor);

        // Прервали (выход из программы, "Стоп", папку убрали) — неполные числа не отдаём
        if (visitor.isStopRequested()) {
            throw new InterruptedIOException("Сбор прерван");
        }
        // Саму папку не удалось прочитать — это ошибка, а не "пустая папка"
        if (visitor.rootFailed) {
            throw new IOException("Нет доступа к папке: " + root);
        }
        // Папка пропала во время обхода (диск отключили) — данные были бы неполными
        if (!Files.isDirectory(root)) {
            throw new IOException("Папка стала недоступна во время сбора: " + root);
        }

        return new ScanResult(visitor.total, filterBig(visitor.subfolders));
    }

    private Map<String, FolderStats> filterBig(Map<String, FolderStats> all) {
        Map<String, FolderStats> result = new LinkedHashMap<>();
        for (Map.Entry<String, FolderStats> entry : all.entrySet()) {
            FolderStats s = entry.getValue();
            if (s.getSizeBytes() > MIN_SUBFOLDER_BYTES
                    || s.getFileCount() > MIN_SUBFOLDER_FILES) {
                result.put(entry.getKey(), s);
            }
        }
        return result;
    }

    private static class ScanVisitor extends SimpleFileVisitor<Path> {

        private final Path root;
        private final BooleanSupplier cancelled;
        private final FolderStats total = new FolderStats();
        private final Map<String, FolderStats> subfolders = new LinkedHashMap<>();
        private FolderStats current;   // подпапка 1-го уровня, в которой мы сейчас; null = в корне
        private boolean rootFailed;    // не удалось прочитать саму наблюдаемую папку

        ScanVisitor(Path root, BooleanSupplier cancelled) {
            this.root = root;
            this.cancelled = cancelled;
        }

        boolean isStopRequested() {
            return Thread.currentThread().isInterrupted() || cancelled.getAsBoolean();
        }

        private FolderStats target() {
            return current != null ? current : total;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (isStopRequested()) {
                return FileVisitResult.TERMINATE;
            }
            if (dir.equals(root)) {
                return FileVisitResult.CONTINUE;
            }
            if (root.equals(dir.getParent())) {
                total.addFolder();
                current = new FolderStats();
                subfolders.put(dir.getFileName().toString(), current);
            } else {
                target().addFolder();
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (isStopRequested()) {
                return FileVisitResult.TERMINATE;
            }
            if (attrs.isRegularFile()) {
                long size = attrs.size();
                String name = file.getFileName().toString();
                FolderStats t = target();

                if (FileTypes.isImage(name)) {
                    t.addImage(size);
                } else if (FileTypes.isVideo(name)) {
                    t.addVideo(size);
                } else {
                    t.addOther(size);
                }
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            if (file.equals(root)) {   // не открылась сама наблюдаемая папка
                rootFailed = true;
                return FileVisitResult.TERMINATE;
            }
            target().addSkipped();
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            if (current != null && root.equals(dir.getParent())) {
                total.addAll(current);
                current = null;
            }
            return FileVisitResult.CONTINUE;
        }
    }
}