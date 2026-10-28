package breakeven;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static breakeven.Theme.*;

/**
 * A small two-series bar chart drawn with Java2D (no external library).
 * OOP [INHERITANCE]: extends JComponent and overrides paintComponent().
 * OOP [ENCAPSULATION]: the data and colours are private; callers only use setData().
 */
class BarChart extends JComponent {
    private final String nameA, nameB;
    private final Color colA, colB;
    private List<String> labels = new ArrayList<>();
    private double[] a = new double[0], b = new double[0];

    BarChart(String nameA, Color colA, String nameB, Color colB) {
        this.nameA = nameA; this.colA = colA; this.nameB = nameB; this.colB = colB;
        setOpaque(false);
        setPreferredSize(new Dimension(300, 180));
    }

    void setData(List<String> labels, double[] a, double[] b) {
        this.labels = new ArrayList<>(labels); this.a = a.clone(); this.b = b.clone();
        repaint();
    }

    private static double niceCeil(double max) {
        if (max <= 0) return 1;
        double mag = Math.pow(10, Math.floor(Math.log10(max)));
        return Math.ceil(max / mag * 2) / 2 * mag;
    }

    private static String compact(double v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (v >= 1_000) return String.format("%.0fk", v / 1_000);
        return String.format("%.0f", v);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight(), left = 46, right = 12, top = 28, bottom = 26;
        int cw = w - left - right, ch = h - top - bottom;
        g.setFont(f(Font.PLAIN, 10));
        FontMetrics fm = g.getFontMetrics();

        // legend
        int lx = left;
        g.setColor(colA); g.fillRect(lx, 8, 10, 10); g.setColor(MUTED); g.drawString(nameA, lx + 14, 17);
        lx += 14 + fm.stringWidth(nameA) + 16;
        g.setColor(colB); g.fillRect(lx, 8, 10, 10); g.setColor(MUTED); g.drawString(nameB, lx + 14, 17);

        if (labels.isEmpty() || ch < 20) { g.setColor(MUTED); g.drawString("No data yet", left, top + 20); g.dispose(); return; }

        double max = 1;
        for (double v : a) max = Math.max(max, v);
        for (double v : b) max = Math.max(max, v);
        max = niceCeil(max);

        for (int i = 0; i <= 4; i++) {                       // grid + y labels
            int y = top + ch - ch * i / 4;
            g.setColor(new Color(0xEFECE5)); g.drawLine(left, y, left + cw, y);
            String s = compact(max * i / 4);
            g.setColor(MUTED); g.drawString(s, left - 6 - fm.stringWidth(s), y + 4);
        }
        int n = labels.size(), slot = cw / n, bw = Math.max(6, Math.min(34, slot / 3));
        for (int i = 0; i < n; i++) {
            int cx = left + slot * i + slot / 2;
            int ha = (int) (ch * a[i] / max), hb = (int) (ch * b[i] / max);
            g.setColor(colA); g.fillRect(cx - bw - 1, top + ch - ha, bw, ha);
            g.setColor(colB); g.fillRect(cx + 1, top + ch - hb, bw, hb);
            String s = labels.get(i);
            boolean cut = false;
            while (s.length() > 1 && fm.stringWidth(s) > slot - 4) { s = s.substring(0, s.length() - 1); cut = true; }
            if (cut) s += "..";
            g.setColor(TEXT); g.drawString(s, cx - fm.stringWidth(s) / 2, top + ch + 16);
        }
        g.dispose();
    }
}
