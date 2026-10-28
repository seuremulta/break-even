package breakeven;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static breakeven.ClaimDetails.showDetails;
import static breakeven.Store.*;
import static breakeven.Theme.*;
import static breakeven.Ui.*;

/**
 * NEW (proposal, Auditor: "View claim and receipt history per event").
 * Pick an event, see every claim and whether its receipt is on file, and see the review trail of the selected claim.
 * OOP [INHERITANCE]: extends Page. Overrides refresh() (OOP [POLYMORPHISM]).
 */
class EventHistoryPage extends Page {
    private final JComboBox<String> evBox = new JComboBox<>();
    private boolean loading;
    private final DefaultTableModel cm = model("Ref No.", "Description", "Category", "Date", "OR No.", "Amount", "Receipt", "Status"),
            hm = model("Log ID", "Timestamp", "Reviewer / User", "Action");
    private final JTable ct = table(cm, 36), ht = table(hm, 32);
    private final List<Claim> shown = new ArrayList<>();
    private final JLabel sum = new JLabel(" ");
    private final JButton detailBtn = btn("View Details", false), openBtn = btn("Open Receipt", false),
            expBtn = btn("Export Claims (.csv)", false);

    EventHistoryPage() {
        add(pageHeader("Event History", "Claim and receipt history per event, with the review trail behind each claim.", null), BorderLayout.NORTH);
        evBox.setFont(f(Font.PLAIN, 13)); evBox.setPreferredSize(new Dimension(240, 32));
        sum.setFont(f(Font.PLAIN, 12)); sum.setForeground(MUTED);
        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); ctrl.setOpaque(false);
        ctrl.add(new JLabel("Event:")); ctrl.add(evBox);
        JPanel bar = new JPanel(new BorderLayout()); bar.setOpaque(false);
        bar.add(ctrl, BorderLayout.WEST); bar.add(row(detailBtn, openBtn, expBtn), BorderLayout.EAST);
        JPanel top = new JPanel(new BorderLayout(0, 6)); top.setOpaque(false);
        top.add(bar, BorderLayout.NORTH); top.add(sum, BorderLayout.SOUTH);
        JPanel grid = new JPanel(new GridLayout(2, 1, 0, 16)); grid.setOpaque(false);
        grid.add(cardWith("Claims & Receipts  (double-click a row for details)", null, scroll(ct)));
        grid.add(cardWith("Review Trail of Selected Claim", null, scroll(ht)));
        JPanel center = new JPanel(new BorderLayout(0, 12)); center.setOpaque(false);
        center.add(top, BorderLayout.NORTH); center.add(grid, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        int[] w = {80, 260, 130, 90, 110, 100, 150, 90};
        for (int i = 0; i < w.length; i++) ct.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        ht.getColumnModel().getColumn(0).setPreferredWidth(80);
        ht.getColumnModel().getColumn(1).setPreferredWidth(150);
        ht.getColumnModel().getColumn(2).setPreferredWidth(200);
        ht.getColumnModel().getColumn(3).setPreferredWidth(420);

        evBox.addActionListener(e -> { if (!loading) reload(); });
        ct.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) showTrail(); });
        ct.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && sel() != null) showDetails(app(), sel());
            }
        });
        detailBtn.addActionListener(e -> { Claim c = sel(); if (c == null) msg(this, "Select a claim first."); else showDetails(app(), c); });
        openBtn.addActionListener(e -> { Claim c = sel(); if (c == null) msg(this, "Select a claim first."); else ReceiptVault.open(this, c); });
        expBtn.addActionListener(e -> {
            if (shown.isEmpty()) { msg(this, "No claims to export for this event."); return; }
            String ev = (String) evBox.getSelectedItem();
            exportCsv(this, "EventHistory_" + ev.replaceAll("\\W+", "_") + ".csv",
                    "ref,description,claimant,category,event,date,or_no,amount,status,receipt_file", shown);
        });
    }

    private Claim sel() { int r = ct.getSelectedRow(); return r < 0 || r >= shown.size() ? null : shown.get(r); }

    private void showTrail() {
        hm.setRowCount(0);
        Claim c = sel();
        if (c == null) return;
        for (LogEntry l : historyFor(c)) hm.addRow(new Object[]{l.getId(), l.getTime(), l.getUser(), l.getAction()});
    }

    private void reload() {
        shown.clear(); cm.setRowCount(0); hm.setRowCount(0);
        String ev = (String) evBox.getSelectedItem();
        if (ev == null) { sum.setText(" "); return; }
        double tot = 0; int withReceipt = 0;
        for (Claim c : claimsFor(ev)) {
            shown.add(c);
            if (c.hasReceipt()) withReceipt++;
            tot += c.getAmount();
            cm.addRow(new Object[]{c.getRef(), c.getDesc(), c.getCategory(), c.getDate(), c.getReceiptNo(), peso(c.getAmount()),
                    c.hasReceipt() ? new File(c.getReceiptFile()).getName() : "No file attached", c.getStatus().label()});
        }
        sum.setText(shown.size() + " claim(s)    Total: " + peso(tot) + "    Receipts on file: " + withReceipt
                + "    Missing receipt file: " + (shown.size() - withReceipt));
    }

    @Override
    void refresh() {
        loading = true;
        Object sel = evBox.getSelectedItem();
        evBox.removeAllItems();
        for (Event e : events()) evBox.addItem(e.getName());
        if (sel != null) evBox.setSelectedItem(sel);
        loading = false;
        reload();
    }
}
