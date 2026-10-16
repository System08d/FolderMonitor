/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

public class FolderStats {

    private static final double BYTES_IN_GB = 1024.0 * 1024.0 * 1024.0;

    private long sizeBytes;
    private long folderCount;
    private long imageCount;
    private long videoCount;
    private long otherCount;
    private long skippedCount;

    public void addImage(long size) {
        imageCount++;
        sizeBytes += size;
    }

    public void addVideo(long size) {
        videoCount++;
        sizeBytes += size;
    }

    public void addOther(long size) {
        otherCount++;
        sizeBytes += size;
    }

    public void addFolder() {
        folderCount++;
    }

    public void addSkipped() {
        skippedCount++;
    }

    public void addAll(FolderStats other) {
        sizeBytes += other.sizeBytes;
        folderCount += other.folderCount;
        imageCount += other.imageCount;
        videoCount += other.videoCount;
        otherCount += other.otherCount;
        skippedCount += other.skippedCount;
    }

    public long getSizeBytes() { return sizeBytes; }
    public long getFolderCount() { return folderCount; }
    public long getImageCount() { return imageCount; }
    public long getVideoCount() { return videoCount; }
    public long getOtherCount() { return otherCount; }
    public long getSkippedCount() { return skippedCount; }

    public long getFileCount() {
        return imageCount + videoCount + otherCount;
    }

    public String getSizeGbText() {
        return String.format("%.1f", sizeBytes / BYTES_IN_GB);
    }

    @Override
    public String toString() {
        return "Размер: " + getSizeGbText() + " ГБ (" + sizeBytes + " байт)\n"
                + "Файлов: " + getFileCount() + "\n"
                + "Папок: " + folderCount + "\n"
                + "Изображений: " + imageCount + "\n"
                + "Видео: " + videoCount + "\n"
                + "Прочих файлов: " + otherCount + "\n"
                + "Пропущено (нет доступа): " + skippedCount;
    }
}