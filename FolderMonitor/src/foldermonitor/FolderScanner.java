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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;

public class FolderScanner {

    // Пороги для подпапок первого уровня (500 МБ считаем как 500 * 1024 * 1024)
    public static final long MIN_SUBFOLDER_BYTES = 500L * 1024 * 1024;
    public static final long MIN_SUBFOLDER_FILES = 500;

    public ScanResult scan(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException("Папка не найдена или недоступна: " + root);
        }

        ScanVisitor visitor = new ScanVisitor(root);
        Files.walkFileTree(root, visitor);

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

    // "Посетитель" вынесен во вложенный класс: ему нужны собственные поля
    private static class ScanVisitor extends SimpleFileVisitor<Path> {

        private final Path root;
        private final FolderStats total = new FolderStats();
        private final Map<String, FolderStats> subfolders = new LinkedHashMap<>();
        private FolderStats current; // подпапка 1-го уровня, в которой мы сейчас; null = мы в корне

        ScanVisitor(Path root) {
            this.root = root;
        }

        // Куда писать числа: в текущую подпапку или, если мы прямо в корне, в общий итог
        private FolderStats target() {
            return current != null ? current : total;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (dir.equals(root)) {
                return FileVisitResult.CONTINUE;
            }
            if (root.equals(dir.getParent())) {
                // вошли в подпапку первого уровня
                total.addFolder();
                current = new FolderStats();
                subfolders.put(dir.getFileName().toString(), current);
            } else {
                // более глубокая папка: считаем её внутри текущей подпапки
                target().addFolder();
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
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
            target().addSkipped();
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            if (current != null && root.equals(dir.getParent())) {
                // вышли из подпапки первого уровня: переносим её числа в общий итог
                total.addAll(current);
                current = null;
            }
            return FileVisitResult.CONTINUE;
        }
    }
}