package breakeven;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static breakeven.Theme.BG;

/**
 * OOP [ABSTRACTION]: the common shape of every screen. A page is a JPanel that knows how to refresh();
 * how it does so is left to each page.
 * OOP [INHERITANCE]: all 7 pages extend this and inherit the shared look (background, padding, layout).
 * OOP [POLYMORPHISM]: App calls page.refresh() on any Page and the right screen updates itself.
 */
abstract class Page extends JPanel {
    Page() { setBackground(BG); setBorder(new EmptyBorder(24, 32, 24, 32)); setLayout(new BorderLayout(0, 18)); }
    abstract void refresh();
}
