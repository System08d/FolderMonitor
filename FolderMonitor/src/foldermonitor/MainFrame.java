package foldermonitor;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

// Окно приложения. Само ничего не рисует: внутри ScreenHost со сменяющимися экранами.
// Главный экран создаётся здесь, остальные (выбор папки, опции, график) создаёт ChartFlow.
public class MainFrame extends JFrame {

    private final ScreenHost host = new ScreenHost();

    public MainFrame(Settings settings, AppController controller) {
        super("Folder Monitor");
        setIconImage(TrayManager.createIcon());

        // Крестик НЕ закрывает окно сам: решение принимает AppController
        // (спрятаться в трей или завершить программу)
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.windowCloseRequested();
            }
        });

        ChartFlow flow = new ChartFlow(host, controller);
        host.setScreen(Screen.MAIN, new MainScreen(settings, controller, flow::start));
        host.show(Screen.MAIN);   // окно всегда открывается на главном экране

        host.setPreferredSize(new Dimension(1000, 680));
        setContentPane(host);
        setMinimumSize(new Dimension(800, 560));
        pack();
        setLocationRelativeTo(null);   // по центру экрана
    }
}
