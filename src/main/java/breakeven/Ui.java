package breakeven;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static breakeven.Store.*;
import static breakeven.Theme.*;

/** Reusable Swing building blocks: buttons, cards, tables, dialogs and file export. */
final class Ui {

    private Ui() {}   // OOP [ABSTRACTION]: static toolbox of reusable widgets, never instantiated

    static JButton btn(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setUI(new BasicButtonUI());
        b.setFont(f(Font.PLAIN, 13));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(primary ? GREEN : new Color(0xF4F2EC));
        b.setForeground(primary ? Color.WHITE : TEXT);
        b.setBorder(new CompoundBorder(new LineBorder(primary ? GREEN : TEXT, 1, true), new EmptyBorder(8, 16, 8, 16)));
        return b;
    }

    static JButton smallBtn(String text) {
        JButton b = btn(text, false);
        b.setBorder(new CompoundBorder(new LineBorder(new Color(0xB3261E), 1, true), new EmptyBorder(4, 12, 4, 12)));
        b.setForeground(new Color(0xB3261E));
        return b;
    }

    static boolean confirm(Component p, String m, String title) {
        return JOptionPane.showConfirmDialog(p, m, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    static JPanel card() {
        JPanel c = new JPanel();
        c.setBackground(Color.WHITE);
        c.setBorder(new CompoundBorder(new LineBorder(BORDER_C), new EmptyBorder(14, 16, 14, 16)));
        return c;
    }

    static JPanel cardWith(String title, JComponent right, JComponent body) {
        JPanel c = new JPanel(new BorderLayout());
        c.setBackground(Color.WHITE);
        c.setBorder(new LineBorder(BORDER_C));
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(Color.WHITE);
        h.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER_C), new EmptyBorder(12, 16, 12, 16)));
        JLabel l = new JLabel(title);
        l.setFont(f(Font.PLAIN, 15));
        h.add(l, BorderLayout.WEST);
        if (right != null) h.add(right, BorderLayout.EAST);
        c.add(h, BorderLayout.NORTH);
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    static JPanel pageHeader(String title, String sub, JComponent right) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        JPanel l = new JPanel();
        l.setLayout(new BoxLayout(l, BoxLayout.Y_AXIS));
        l.setOpaque(false);
        JLabel t = new JLabel(title); t.setFont(f(Font.PLAIN, 24)); t.setForeground(TEXT);
        JLabel s = new JLabel(sub); s.setFont(f(Font.PLAIN, 13)); s.setForeground(MUTED);
        l.add(t); l.add(s);
        h.add(l, BorderLayout.WEST);
        if (right != null) h.add(right, BorderLayout.EAST);
        return h;
    }

    static JPanel row(JComponent... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.setOpaque(false);
        for (JComponent c : cs) p.add(c);
        return p;
    }

    static JScrollPane scroll(JComponent c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(null);
        s.getViewport().setBackground(Color.WHITE);
        return s;
    }

    static void msg(Component p, String m) { JOptionPane.showMessageDialog(p, m, "BreakEven", JOptionPane.INFORMATION_MESSAGE); }

    static void warn(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Please check", JOptionPane.WARNING_MESSAGE); }

    static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
    }

    static JTable table(DefaultTableModel m, int rowH) {
        JTable t = new JTable(m);
        t.setRowHeight(rowH); t.setShowGrid(false); t.setIntercellSpacing(new Dimension(0, 0));
        t.setFont(f(Font.PLAIN, 13)); t.setForeground(TEXT); t.setBackground(Color.WHITE);
        t.setSelectionBackground(SEL); t.setSelectionForeground(TEXT); t.setFillsViewportHeight(true);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false); h.setPreferredSize(new Dimension(10, 38));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean fo, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);
                l.setText(String.valueOf(v).toUpperCase());
                l.setFont(f(Font.PLAIN, 11)); l.setForeground(MUTED); l.setBackground(Color.WHITE);
                l.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER_C), new EmptyBorder(0, 8, 0, 8)));
                return l;
            }
        });
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean fo, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, s, false, r, c);
                l.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, new Color(0xEFECE5)), new EmptyBorder(0, 8, 0, 8)));
                return l;
            }
        });
        for (int i = 0; i < m.getColumnCount(); i++)
            if (m.getColumnName(i).equals("Status")) t.getColumnModel().getColumn(i).setCellRenderer(new StatusRenderer());
        return t;
    }

    static void export(Component parent, String name, String content) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(name));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        try {
            java.nio.file.Files.write(fc.getSelectedFile().toPath(), content.getBytes(StandardCharsets.UTF_8));
            log("Exported " + name);
            msg(parent, "Saved to " + fc.getSelectedFile().getAbsolutePath());
            app().refreshAll();
        } catch (IOException ex) { warn(parent, "Could not save file: " + ex.getMessage()); }
    }

    /** Saves several Exportable records as one CSV file. */
    static void exportCsv(Component parent, String fileName, String header, List<? extends Exportable> rows) {
        StringBuilder sb = new StringBuilder(header).append('\n');
        for (Exportable r : rows) sb.append(r.toCsvRow()).append('\n');   // OOP [POLYMORPHISM]: each record formats itself
        export(parent, fileName, sb.toString());
    }

    /** The big number cards on the dashboard and trends pages. */
    static JPanel statCard(String title, JLabel val, JLabel sub, Color vc) {
        JPanel c = card();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setPreferredSize(new Dimension(300, 102));
        JLabel t = new JLabel(title.toUpperCase()); t.setFont(f(Font.PLAIN, 11)); t.setForeground(MUTED);
        val.setFont(f(Font.PLAIN, 24)); val.setForeground(vc);
        sub.setFont(f(Font.PLAIN, 12)); sub.setForeground(vc == TEXT ? MUTED : vc);
        c.add(t); c.add(Box.createVerticalStrut(6)); c.add(val); c.add(sub);
        return c;
    }
}
