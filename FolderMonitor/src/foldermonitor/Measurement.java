/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.time.LocalDateTime;

public record Measurement(
        LocalDateTime time,
        String name,          // "." = сама папка, иначе имя подпапки
        long sizeBytes,
        long fileCount,
        long folderCount,
        long imageCount,      // для подпапок = NO_DATA
        long videoCount,
        long otherCount) {

    public static final String ROOT_NAME = ".";
    public static final long NO_DATA = -1;

    public static Measurement ofFolder(LocalDateTime time, FolderStats s) {
        return new Measurement(time, ROOT_NAME,
                s.getSizeBytes(), s.getFileCount(), s.getFolderCount(),
                s.getImageCount(), s.getVideoCount(), s.getOtherCount());
    }

    public static Measurement ofSubfolder(LocalDateTime time, String name, FolderStats s) {
        return new Measurement(time, name,
                s.getSizeBytes(), s.getFileCount(), s.getFolderCount(),
                NO_DATA, NO_DATA, NO_DATA);
    }

    public boolean isFolderItself() {
        return ROOT_NAME.equals(name);
    }

    public boolean hasTypes() {
        return imageCount != NO_DATA;
    }
}