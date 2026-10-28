package breakeven;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import static breakeven.Theme.*;

/**
 * Draws a status as a coloured pill.
 * OOP [INHERITANCE]: extends DefaultTableCellRenderer.
 * OOP [POLYMORPHISM]: it asks a Badged (ClaimStatus or ReportStatus) for its colours, so it never
 * needs a switch over status names and works for any future status type.
 */
class StatusRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
        String s = String.valueOf(v);
        Badged badge = Badged.lookup(s);
        Color bg = badge != null ? badge.background() : new Color(0xEEEEEE);
        Color fg = badge != null ? badge.foreground() : new Color(0x555555);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(sel ? SEL : Color.WHITE);
        p.setBorder(new MatteBorder(0, 0, 1, 0, new Color(0xEFECE5)));
        JLabel l = new JLabel(s);
        l.setOpaque(true); l.setBackground(bg); l.setForeground(fg); l.setFont(f(Font.BOLD, 11));
        l.setBorder(new CompoundBorder(new LineBorder(fg.brighter(), 1), new EmptyBorder(2, 8, 2, 8)));
        GridBagConstraints g = new GridBagConstraints();
        g.anchor = GridBagConstraints.WEST; g.insets = new Insets(0, 8, 0, 0);
        p.add(l, g);
        return p;
    }
}
