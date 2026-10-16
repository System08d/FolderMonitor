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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ScanResult(FolderStats total, Map<String, FolderStats> subfolders) {

    public List<Measurement> toMeasurements(LocalDateTime time) {
        List<Measurement> list = new ArrayList<>();
        list.add(Measurement.ofFolder(time, total));
        for (Map.Entry<String, FolderStats> entry : subfolders.entrySet()) {
            list.add(Measurement.ofSubfolder(time, entry.getKey(), entry.getValue()));
        }
        return list;
    }
}