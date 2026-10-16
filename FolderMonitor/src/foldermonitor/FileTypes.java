/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.util.Set;

public class FileTypes {

    private static final Set<String> IMAGES = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tif", "tiff", "heic");

    private static final Set<String> VIDEOS = Set.of(
            "mp4", "mov", "avi", "mkv", "wmv", "webm", "m4v", "flv");

    public static boolean isImage(String fileName) {
        return IMAGES.contains(getExtension(fileName));
    }

    public static boolean isVideo(String fileName) {
        return VIDEOS.contains(getExtension(fileName));
    }

    private static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }
}