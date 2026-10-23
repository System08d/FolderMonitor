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
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {

    public enum State { WAITING, SCANNING, ERROR, STOPPED }

    // Снимок состояния папки для окна. lastRun == null означает "данных о последнем измерении нет".
    public record FolderStatus(State state, Instant lastRun, String message) { }

    // Всё, что планировщик знает об одной папке
    private static class FolderEntry {
        volatile boolean enabled;
        volatile Instant lastRun;            // null = ещё не загружено из CSV; EPOCH = измерений не было
        volatile State state = State.WAITING;
        volatile String message = "";        // текст последней ошибки
        volatile boolean hadError;           // была ли ошибка (чтобы не повторять её в логе)

        FolderEntry(boolean enabled) {
            this.enabled = enabled;
        }
    }

    private static final DateTimeFormatter FULL_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsStorage storage;
    private final FolderScanner scanner = new FolderScanner();
    private final Duration checkPeriod;
    private volatile Duration interval;

    private final ConcurrentSkipListMap<Path, FolderEntry> entries = new ConcurrentSkipListMap<>();
    private final ScheduledExecutorService executor;

    public Scheduler(StatsStorage storage, Duration interval, Duration checkPeriod) {
        this.storage = storage;
        this.interval = interval;
        this.checkPeriod = checkPeriod;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "folder-monitor-scan");
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
    }

    // ---- вызывается из окна и из Main ----

    // false, если такая папка уже есть в списке
    public boolean addFolder(Path folder, boolean enabled) {
        return entries.putIfAbsent(normalize(folder), new FolderEntry(enabled)) == null;
    }

    public void removeFolder(Path folder) {
        entries.remove(normalize(folder));   // идущий обход заметит это и остановится
    }

    public void setEnabled(Path folder, boolean enabled) {
        FolderEntry entry = entries.get(normalize(folder));
        if (entry != null) {
            entry.enabled = enabled;
        }
    }

    public void setInterval(Duration newInterval) {
        this.interval = newInterval;
    }

    public List<Path> getFolders() {
        return new ArrayList<>(entries.keySet());
    }

    public List<WatchedFolder> getFolderConfigs() {
        List<WatchedFolder> list = new ArrayList<>();
        for (Map.Entry<Path, FolderEntry> e : entries.entrySet()) {
            list.add(new WatchedFolder(e.getKey(), e.getValue().enabled));
        }
        return list;
    }

    public FolderStatus getStatus(Path folder) {
        FolderEntry entry = entries.get(normalize(folder));
        if (entry == null) {
            return new FolderStatus(State.STOPPED, null, "");
        }
        Instant last = entry.lastRun;
        if (last != null && last.equals(Instant.EPOCH)) {
            last = null;
        }
        State state = entry.enabled ? entry.state : State.STOPPED;
        return new FolderStatus(state, last, entry.message);
    }

    public void start(Duration initialDelay) {
        executor.scheduleWithFixedDelay(
                this::checkAll, initialDelay.toMillis(), checkPeriod.toMillis(), TimeUnit.MILLISECONDS);
    }

    // Проверить папки прямо сейчас (в потоке планировщика, поэтому наложения нет)
    public void checkSoon() {
        try {
            executor.execute(this::checkAll);
        } catch (RejectedExecutionException e) {
            // планировщик уже остановлен — ничего не делаем
        }
    }

    public void stop() {
        executor.shutdownNow();   // прерывает и обход, если он идёт
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ---- всё ниже выполняется в фоновом потоке ----

    private void checkAll() {
        // Ошибка в задаче по расписанию молча остановила бы все будущие запуски,
        // поэтому всё тело завёрнуто в try/catch.
        try {
            for (Map.Entry<Path, FolderEntry> e : entries.entrySet()) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                checkFolder(e.getKey(), e.getValue());
            }
        } catch (Throwable t) {
            AppLog.error("Неожиданная ошибка планировщика", t);
        }
    }

    private void checkFolder(Path folder, FolderEntry entry) {
        // выключена или уже убрана из списка
        if (!entry.enabled || entries.get(folder) != entry) {
            return;
        }
        try {
            Instant last = entry.lastRun;
            if (last == null) {   // первая проверка этой папки после запуска программы
                last = loadLastRun(folder);
                entry.lastRun = last;
                if (last.equals(Instant.EPOCH)) {
                    AppLog.info(folder + ": измерений ещё не было");
                } else {
                    AppLog.info(folder + ": последнее измерение " + format(last)
                            + ", следующее не раньше " + format(last.plus(interval)));
                }
            }
            if (!isDue(last, Instant.now())) {
                return;
            }

            AppLog.info(folder + ": начат сбор");
            entry.state = State.SCANNING;
            long started = System.nanoTime();

            // обход остановится, если папку выключили или убрали из списка
            ScanResult result = scanner.scan(folder,
                    () -> !entry.enabled || entries.get(folder) != entry);

            Instant finished = Instant.now();
            storage.append(folder, result.toMeasurements(
                    LocalDateTime.ofInstant(finished, ZoneId.systemDefault())));

            boolean recovered = entry.hadError;
            entry.lastRun = finished;
            entry.hadError = false;
            entry.message = "";
            entry.state = State.WAITING;

            long millis = (System.nanoTime() - started) / 1_000_000;
            long skipped = result.total().getSkippedCount();
            AppLog.info(folder + ": готово за " + millis + " мс, подходящих подпапок: "
                    + result.subfolders().size()
                    + (skipped > 0 ? ", пропущено (нет доступа): " + skipped : "")
                    + (recovered ? " [папка снова доступна]" : ""));

        } catch (InterruptedIOException e) {
            entry.state = State.WAITING;
            AppLog.info(folder + ": сбор прерван, данные не записаны");
        } catch (IOException e) {
            fail(folder, entry, e.getMessage() != null ? e.getMessage() : e.toString(), null);
        } catch (RuntimeException e) {
            fail(folder, entry, "внутренняя ошибка: " + e, e);
        }
    }

    // Запомнить ошибку. В лог она попадает один раз (когда появилась или изменилась),
    // а не при каждом повторе. Новая строка в CSV НЕ пишется: никаких нулей.
    private void fail(Path folder, FolderEntry entry, String text, Throwable cause) {
        boolean changed = !entry.hadError || !text.equals(entry.message);
        entry.hadError = true;
        entry.state = State.ERROR;
        entry.message = text;
        if (changed) {
            String line = folder + ": ошибка — " + text
                    + ". Данные не записаны, повтор при следующей проверке";
            if (cause != null) {
                AppLog.error(line, cause);
            } else {
                AppLog.warn(line);
            }
        }
    }

    private boolean isDue(Instant last, Instant now) {
        return last.isAfter(now) || !now.isBefore(last.plus(interval));
    }

    private Instant loadLastRun(Path folder) {
        try {
            return storage.lastTime(folder)
                    .map(t -> t.atZone(ZoneId.systemDefault()).toInstant())
                    .orElse(Instant.EPOCH);
        } catch (IOException e) {
            AppLog.warn(folder + ": не удалось прочитать историю: " + e.getMessage());
            return Instant.EPOCH;
        }
    }

    private static Path normalize(Path folder) {
        return folder.toAbsolutePath().normalize();
    }

    private static String format(Instant instant) {
        return FULL_TIME.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }
}