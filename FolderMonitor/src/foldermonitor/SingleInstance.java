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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class SingleInstance {

    // static-поля держат файл открытым до конца работы программы.
    // Блокировку снимет сама Windows при завершении процесса, даже при аварии.
    private static FileChannel channel;
    private static FileLock lock;

    // true = мы единственный экземпляр, false = программа уже запущена
    public static boolean tryLock(Path dataDir) {
        try {
            channel = FileChannel.open(dataDir.resolve("app.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = channel.tryLock();   // null, если файл уже заблокирован другим процессом
            if (lock == null) {
                channel.close();
                return false;
            }
            return true;
        } catch (OverlappingFileLockException e) {
            return false;
        } catch (IOException e) {
            // Не смогли проверить — лучше работать, чем отказаться запускаться
            System.out.println("Не удалось проверить повторный запуск: " + e.getMessage());
            return true;
        }
    }
}