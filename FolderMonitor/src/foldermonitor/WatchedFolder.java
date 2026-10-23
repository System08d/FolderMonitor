/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

import java.nio.file.Path;

// Настройка одной наблюдаемой папки: путь и включён ли сбор
public record WatchedFolder(Path path, boolean enabled) { }