package breakeven;

import javax.swing.*;

import static breakeven.Store.*;
import static breakeven.Theme.f;

/** Entry point. Sets the look, loads the saved data (or the demo data) and opens the window. */
public class Main {

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        UIManager.put("OptionPane.messageFont", f(java.awt.Font.PLAIN, 13));
        UIManager.put("OptionPane.buttonFont", f(java.awt.Font.PLAIN, 13));
        init();
        SwingUtilities.invokeLater(() -> {
            App a = new App(); setApp(a); a.setVisible(true);
            String problem = Database.problem();              // MySQL unreachable or unreadable: say so instead of failing silently
            if (problem != null) Ui.warn(a, problem);
        });
    }
}
