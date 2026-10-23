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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class AutoStart {

    private static final String RUN_KEY =
            "HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String VALUE_NAME = "FolderMonitor";
    private static final String[] JVM_OPTIONS = { "-Xmx256m", "-XX:+UseSerialGC" };

    // Автозапуск возможен только в Windows и только из собранного .jar
    public static boolean isSupported() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") && AppPaths.jarFile() != null;
    }

    // Включить автозапуск. Выбрасывает IOException, если не получилось.
    public static void enable(Settings settings) throws IOException {
        String command = buildCommand();
        String script = "$ErrorActionPreference = 'Stop'; "
                + "Set-ItemProperty -Path '" + RUN_KEY + "' -Name '" + VALUE_NAME
                + "' -Value '" + command.replace("'", "''") + "'";
        runPowerShell(script);
        settings.setAutoStart(true);
        settings.setAutoStartCommand(command);
                AppLog.info("Автозапуск включён: " + command);
    }

    // Выключить автозапуск (если записи нет — это тоже успех)
    public static void disable(Settings settings) throws IOException {
        String script = "$ErrorActionPreference = 'Stop'; "
                + "if (Get-ItemProperty -Path '" + RUN_KEY + "' -Name '" + VALUE_NAME
                + "' -ErrorAction SilentlyContinue) { "
                + "Remove-ItemProperty -Path '" + RUN_KEY + "' -Name '" + VALUE_NAME + "' }; "
                + "exit 0";
        runPowerShell(script);
        settings.setAutoStart(false);
        settings.setAutoStartCommand("");
                AppLog.info("Автозапуск выключен");
    }

    // При старте программы: если автозапуск включён, но путь к Java или к .jar изменился
    // (обновили Java, перенесли программу), обновляем запись. Делается в фоне и только
    // при реальном изменении, поэтому обычный старт ничего не тратит.
    public static void syncOnStart(Settings settings) {
        if (!isSupported() || !settings.isAutoStart()) {
            return;
        }
        String wanted;
        try {
            wanted = buildCommand();
        } catch (IOException e) {
            return;
        }
        if (wanted.equals(settings.getAutoStartCommand())) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                enable(settings);
                                AppLog.info("Запись автозапуска обновлена (изменился путь к программе)");
            } catch (IOException e) {
                AppLog.warn("Не удалось обновить автозапуск: " + e.getMessage());
            }
        }, "autostart-sync");
        thread.setDaemon(true);
        thread.start();
    }

    private static String buildCommand() throws IOException {
        Path jar = AppPaths.jarFile();
        if (jar == null) {
            throw new IOException("Программа запущена не из .jar-файла");
        }
        Path javaw = Paths.get(System.getProperty("java.home"), "bin", "javaw.exe");
        if (!Files.isRegularFile(javaw)) {
            throw new IOException("Не найден файл " + javaw);
        }
        StringBuilder sb = new StringBuilder();
        sb.append('"').append(javaw).append('"');
        for (String option : JVM_OPTIONS) {
            sb.append(' ').append(option);
        }
        sb.append(" -jar \"").append(jar).append("\" ").append(Main.MINIMIZED_ARG);
        return sb.toString();
    }

    private static void runPowerShell(String script) throws IOException {
        String encoded = Base64.getEncoder()
                .encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
        Process process = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
                "-OutputFormat", "Text", "-EncodedCommand", encoded)
                .redirectErrorStream(true)   // ошибки смешиваем с обычным выводом
                .start();
        process.getOutputStream().close();   // на вход ничего не подаём

        byte[] output;
        try (InputStream in = process.getInputStream()) {
            output = in.readAllBytes();
        }
        try {
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("PowerShell не ответил за 30 секунд");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Операция прервана");
        }
        if (process.exitValue() != 0) {
            String text = new String(output, nativeCharset()).trim();
            if (text.length() > 400) {
                text = text.substring(0, 400) + "...";
            }
            throw new IOException("PowerShell вернул код " + process.exitValue() + ": " + text);
        }
    }

    // Кодировка консоли Windows (нужна только чтобы прочитать текст ошибки)
    private static Charset nativeCharset() {
        try {
            return Charset.forName(System.getProperty("native.encoding"));
        } catch (RuntimeException e) {
            return Charset.defaultCharset();
        }
    }
}