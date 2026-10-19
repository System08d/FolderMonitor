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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {

    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FULL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsStorage storage;
    private final FolderScanner scanner = new FolderScanner();
    private final Duration interval;      // как часто измерять папку (например, 24 часа)
    private final Duration checkPeriod;   // как часто просыпаться и проверять (например, 5 минут)

    // К этим коллекциям будут обращаться разные потоки (потом ещё и окно),
    // поэтому используем "concurrent"-версии: они безопасны при одновременной работе
    private final Set<Path> folders = ConcurrentHashMap.newKeySet();
    private final Map<Path, Instant> lastRun = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executor;

    public Scheduler(StatsStorage storage, Duration interval, Duration checkPeriod) {
        this.storage = storage;
        this.interval = interval;
        this.checkPeriod = checkPeriod;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "folder-monitor-scan");
            thread.setPriority(Thread.MIN_PRIORITY);   // пониженный приоритет
            return thread;
        });
    }

    public void addFolder(Path folder) {
        folders.add(folder.toAbsolutePath().normalize());
    }

    public void removeFolder(Path folder) {
        Path key = folder.toAbsolutePath().normalize();
        folders.remove(key);
        lastRun.remove(key);
    }

        // ИЗМЕНЕНО: первая проверка через initialDelay (при автозапуске не грузим диск сразу после входа в Windows)
    public void start(Duration initialDelay) {
        executor.scheduleWithFixedDelay(
                this::checkAll, initialDelay.toMillis(), checkPeriod.toMillis(), TimeUnit.MILLISECONDS);
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
        // Важно: если задача по расписанию выбросит ошибку, Java молча ПЕРЕСТАНЕТ
        // запускать её в дальнейшем. Для программы, живущей неделями, это недопустимо,
        // поэтому всё тело завёрнуто в try/catch.
        try {
            for (Path folder : folders) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                checkFolder(folder);
            }
        } catch (Throwable t) {
            log("Неожиданная ошибка планировщика: " + t);
        }
    }

    private void checkFolder(Path folder) {
        try {
            Instant last = lastRun.get(folder);
            if (last == null) {   // первая проверка этой папки после запуска программы
                last = loadLastRun(folder);
                lastRun.put(folder, last);
                if (last.equals(Instant.EPOCH)) {
                    log(folder + ": измерений ещё не было");
                } else {
                    log(folder + ": последнее измерение " + format(last)
                            + ", следующее не раньше " + format(last.plus(interval)));
                }
            }
            if (!isDue(last, Instant.now())) {
                return;
            }

            log(folder + ": начат сбор");
            long started = System.nanoTime();
            ScanResult result = scanner.scan(folder);

            Instant finished = Instant.now();
            storage.append(folder, result.toMeasurements(
                    LocalDateTime.ofInstant(finished, ZoneId.systemDefault())));
            lastRun.put(folder, finished);

            long millis = (System.nanoTime() - started) / 1_000_000;
            log(folder + ": готово за " + millis + " мс, подходящих подпапок: "
                    + result.subfolders().size());

        } catch (InterruptedIOException e) {
            log(folder + ": сбор прерван, данные не записаны");
        } catch (IOException e) {
            log(folder + ": ошибка — " + e.getMessage() + ". Повтор при следующей проверке");
        } catch (RuntimeException e) {
            log(folder + ": внутренняя ошибка — " + e);
        }
    }

    // Пора ли измерять? (также срабатывает, если "последнее измерение" оказалось в будущем —
    // например, часы компьютера перевели назад)
    private boolean isDue(Instant last, Instant now) {
        return last.isAfter(now) || !now.isBefore(last.plus(interval));
    }

    private Instant loadLastRun(Path folder) {
        try {
            return storage.lastTime(folder)
                    .map(t -> t.atZone(ZoneId.systemDefault()).toInstant())
                    .orElse(Instant.EPOCH);   // EPOCH = 1970 год = "измерений не было"
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    private static String format(Instant instant) {
        return FULL_TIME.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    private static void log(String text) {
        System.out.println("[" + LocalTime.now().format(LOG_TIME) + "] " + text);
    }
}