package foldermonitor;

import java.nio.file.Path;
import java.util.List;

// Сценарий просмотра графиков: выбор папки -> опции -> график.
// Помнит выбор пользователя, пока окно открыто, и создаёт экраны заново при каждом показе
// (старые уходят в сборщик мусора, в памяти ничего лишнего не остаётся).
public class ChartFlow {

    private final ScreenHost host;
    private final AppController controller;

    private List<Path> folders = List.of();
    private ChartView chart;                  // последний выбранный график
    private ChartPeriod period = ChartPeriod.all();     // по умолчанию — всё время

    public ChartFlow(ScreenHost host, AppController controller) {
        this.host = host;
        this.controller = controller;
    }

    // Кнопка "Показать графики" на главном экране
    public void start() {
        showFolderSelect();
    }

    private void showFolderSelect() {
        List<DataProvider.KnownFolder> known =
                controller.getDataProvider().knownFolders(controller.getFolders());
        host.setScreen(Screen.SELECT_FOLDER, new FolderSelectScreen(known, folders,
                this::onFoldersChosen, this::backToMain));
        host.show(Screen.SELECT_FOLDER);
        host.release(Screen.OPTIONS);
        host.release(Screen.CHART);
    }

    private void onFoldersChosen(List<Path> selected) {
        folders = selected;
        showOptions();
    }

    private void showOptions() {
        List<ChartView> available = ChartRegistry.forFolderCount(folders.size());
        host.setScreen(Screen.OPTIONS, new OptionsScreen(folders, available, chart, period,
                this::showChart, this::showFolderSelect));
        host.show(Screen.OPTIONS);
    }

    private void showChart(ChartView selected, ChartPeriod selectedPeriod) {
        chart = selected;
        period = selectedPeriod;
        host.setScreen(Screen.CHART, new ChartScreen(selected, folders, selectedPeriod,
                controller.getDataProvider(), ChartRegistry.forFolderCount(folders.size()),
                other -> showChart(other, period), this::backFromChart));
        host.show(Screen.CHART);
    }

    private void backFromChart() {
        showOptions();
        host.release(Screen.CHART);   // данные графика освобождаются
    }

    private void backToMain() {
        host.show(Screen.MAIN);
        host.release(Screen.SELECT_FOLDER);
        host.release(Screen.OPTIONS);
        host.release(Screen.CHART);
    }
}
