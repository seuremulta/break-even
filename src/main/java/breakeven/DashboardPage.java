package breakeven;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import static breakeven.ClaimDetails.showDetails;
import static breakeven.Store.*;
import static breakeven.Theme.*;
import static breakeven.Ui.*;

/** OOP [INHERITANCE]: extends Page. Overrides refresh() (OOP [POLYMORPHISM]). */
class DashboardPage extends Page {
    private final JLabel availL = new JLabel(), availSub = new JLabel(), totalL = new JLabel(), totalSub = new JLabel(),
            pendL = new JLabel(), pendSub = new JLabel();
    private final DefaultTableModel m = model("Ref No.", "Description", "Category", "Amount", "OIC", "Status");
    private final JTable table = table(m, 52);
    private final List<Claim> shown = new ArrayList<>();
    private final JComboBox<String> filter = new JComboBox<>(new String[]{"All", "Cleared", "Pending", "Approved", "Flagged"});
    private final JPanel catBox = new JPanel();
    private final JButton newBtn = btn("+ New Expense", true), delBtn = smallBtn("Delete Claim");

    DashboardPage() {
        JButton refresh = btn("Refresh", false);
        newBtn.addActionListener(e -> new ExpenseDialog(null).setVisible(true));
        refresh.addActionListener(e -> app().refreshAll());
        JPanel north = new JPanel(new BorderLayout(0, 18)); north.setOpaque(false);
        north.add(pageHeader("Financial Overview", "Where every claim checks out.", row(newBtn, refresh)), BorderLayout.NORTH);
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); stats.setOpaque(false);
        stats.add(statCard("Total Budget", totalL, totalSub, TEXT)); stats.add(Box.createHorizontalStrut(16));
        stats.add(statCard("Available Balance", availL, availSub, GREEN)); stats.add(Box.createHorizontalStrut(16));
        stats.add(statCard("Pending Clearance", pendL, pendSub, ORANGE));
        north.add(stats, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        filter.setFont(f(Font.PLAIN, 12));
        filter.addActionListener(e -> refresh());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int r = table.getSelectedRow();
                if (e.getClickCount() == 2 && r >= 0 && r < shown.size()) showDetails(app(), shown.get(r));
            }
        });
        int[] w = {90, 300, 150, 110, 110, 100};
        for (int i = 0; i < w.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        delBtn.addActionListener(e -> deleteClaim());
        JPanel fr = row(delBtn, filter);
        JPanel left = cardWith("Recent Expenses  (double-click a row for details)", fr, scroll(table));
        catBox.setLayout(new BoxLayout(catBox, BoxLayout.Y_AXIS)); catBox.setBackground(Color.WHITE);
        catBox.setBorder(new EmptyBorder(8, 16, 8, 16));
        JPanel rightBody = new JPanel(new BorderLayout()); rightBody.setBackground(Color.WHITE);
        rightBody.add(catBox, BorderLayout.NORTH);
        JPanel right = cardWith("Budget by Event", null, scroll(rightBody));
        right.setPreferredSize(new Dimension(340, 10));
        JPanel center = new JPanel(new BorderLayout(16, 0)); center.setOpaque(false);
        center.add(left, BorderLayout.CENTER); center.add(right, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);
    }

    private void deleteClaim() {
        int r = table.getSelectedRow();
        if (r < 0) { msg(this, "Select a claim row first."); return; }
        Claim c = shown.get(r);
        // OOP [POLYMORPHISM]: the status object decides whether deletion is allowed (no string compare)
        if (!c.getStatus().canDelete()) {
            warn(this, c.getRef() + " is " + c.getStatus().label() + " and has already been through review, so it can't be deleted.\nOnly PENDING or FLAGGED claims can be removed.");
            return;
        }
        if (!confirm(this, "Delete claim " + c.getRef() + " (" + c.getDesc() + ", " + peso(c.getAmount()) + ")?\nThis cannot be undone.", "Delete claim")) return;
        removeClaim(c);
        log("Deleted claim " + c.getRef() + " (" + peso(c.getAmount()) + ")");
        app().refreshAll();
    }

    /** One row of the "Budget by Event" panel: name + percentage, a thin bar, and two amounts underneath. */
    private static JPanel block(String name, String pctText, Color pctColor, double frac, Color barColor, String left, String right) {
        JPanel r1 = new JPanel(new BorderLayout()); r1.setOpaque(false);
        JLabel n = new JLabel(name); n.setFont(f(Font.PLAIN, 13));
        JLabel p = new JLabel(pctText); p.setFont(f(Font.PLAIN, 12)); p.setForeground(pctColor);
        r1.add(n, BorderLayout.WEST); r1.add(p, BorderLayout.EAST);
        JProgressBar pb = new JProgressBar(0, 1000);
        pb.setUI(new BasicProgressBarUI());
        pb.setValue((int) Math.max(0, Math.min(1000, frac * 1000))); pb.setBorderPainted(false);
        pb.setForeground(barColor); pb.setBackground(new Color(0xE9E6DF));
        pb.setPreferredSize(new Dimension(10, 6));
        JPanel r2 = new JPanel(new BorderLayout()); r2.setOpaque(false);
        JLabel a = new JLabel(left); a.setFont(f(Font.PLAIN, 12));
        JLabel b = new JLabel(right); b.setFont(f(Font.PLAIN, 12)); b.setForeground(MUTED);
        r2.add(a, BorderLayout.WEST); r2.add(b, BorderLayout.EAST);
        JPanel blk = new JPanel(new BorderLayout(0, 4)); blk.setOpaque(false);
        blk.setBorder(new EmptyBorder(8, 0, 8, 0));
        blk.add(r1, BorderLayout.NORTH); blk.add(pb, BorderLayout.CENTER); blk.add(r2, BorderLayout.SOUTH);
        return blk;
    }

    @Override
    void refresh() {
        double total = budget().getTotalAmount();
        totalL.setText(peso(total)); totalSub.setText(semester().getLabel());
        availL.setText(peso(available()));
        availSub.setText(String.format("%.2f%% remaining", total == 0 ? 0 : available() / total * 100));
        pendL.setText(peso(pendingTotal())); pendSub.setText(pendingCount() + " claims awaiting");
        boolean canEditClaims = current().canSubmitClaims() && editable();   // closed semesters are read-only
        newBtn.setVisible(canEditClaims);
        delBtn.setVisible(canEditClaims);

        String flt = ((String) filter.getSelectedItem()).toUpperCase();
        shown.clear(); m.setRowCount(0);
        List<Claim> sorted = new ArrayList<>(claims());
        sorted.sort((a, b) -> b.getRef().compareTo(a.getRef()));
        for (Claim c : sorted) {
            if (!flt.equals("ALL") && !c.getStatus().label().equals(flt)) continue;
            shown.add(c);
            m.addRow(new Object[]{c.getRef(), "<html>" + esc(c.getDesc()) + "<br><span style='color:#999999'>" + c.getDate() + "</span></html>",
                    c.getCategory(), peso(c.getAmount()), c.getClaimant(), c.getStatus().label()});
        }
        catBox.removeAll();
        for (Event e : events()) {
            double sp = spent(e), pct = e.utilization(sp);
            boolean over = pct > 1 || (e.getAllocated() == 0 && sp > 0);
            catBox.add(block(e.getName(), String.format("%.0f%%", pct * 100), over ? Color.RED : GREEN, pct, over ? Color.RED : GREEN,
                    peso(sp), "/ " + peso(e.getAllocated())));
        }
        if (events().isEmpty()) {
            JLabel none = new JLabel("No event allocations yet."); none.setFont(f(Font.PLAIN, 12)); none.setForeground(MUTED);
            none.setBorder(new EmptyBorder(8, 0, 8, 0));
            catBox.add(none);
        }
        // the rest of the budget: what is not allocated to any event
        double tot = budget().getTotalAmount();
        double cs = tot == 0 ? 0 : budget().getContingency() / tot, us = tot == 0 ? 0 : budget().unallocated() / tot;
        catBox.add(block("Contingency Fund", String.format("%.0f%% of total", cs * 100), MUTED, cs, new Color(0xD9A441), peso(budget().getContingency()), "reserved"));
        catBox.add(block("Unallocated", String.format("%.0f%% of total", us * 100), MUTED, us, new Color(0xB9CFC4), peso(budget().unallocated()), "free to allocate"));
        catBox.revalidate(); catBox.repaint();
    }
}
