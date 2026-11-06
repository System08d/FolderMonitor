/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package foldermonitor;

/**
 *
 * @author oleksandr.dan
 */

// График 1.3: количество папок по времени (всех вложенных, саму наблюдаемую папку не считаем).
public class FolderCountChart extends LineChartBase {

    @Override
    public String title() {
        return "Количество папок по времени";
    }

    @Override
    public String description() {
        return "Сколько папок внутри, включая вложенные";
    }

    @Override
    protected String seriesName() {
        return "Папок";
    }

    @Override
    protected String axisLabel() {
        return "папок";
    }

    @Override
    protected String unitText() {
        return " папок";
    }

    @Override
    protected double valueOf(Measurement m) {
        return m.folderCount();
    }

    @Override
    protected boolean wholeNumbers() {
        return true;
    }

    @Override
    protected double minAxisSpan() {
        return 5;
    }
}

