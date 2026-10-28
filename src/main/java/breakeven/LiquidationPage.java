package breakeven;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static breakeven.Store.*;
import static breakeven.Theme.*;
import static breakeven.Ui.*;

/** OOP [INHERITANCE]: extends Page. Overrides refresh() (OOP [POLYMORPHISM]). */
class LiquidationPage extends Page {
    private final JComboBox<String> evBox = new JComboBox<>();
    private boolean loading;
    private final DefaultTableModel m = model("Ref No.", "Description", "Category", "Date", "OR No.", "Amount", "Status"),
            rm = model("Report ID", "Event", "Claims", "Total", "Allocated", "Filed By", "Date", "Status");
    private final JTable t = table(m, 38), rt = table(rm, 36);
    private final JLabel sum = new JLabel(" ");
    private final JButton fileBtn = btn("File Liquidation Report", true), exportBtn = btn("Export Report (.txt)", false),
            delRepBtn = smallBtn("Delete Report"), reviewBtn = btn("Mark Reviewed", true), expRepBtn = btn("Export Reports (.csv)", false);

    LiquidationPage() {
        add(pageHeader("Liquidation", "Approved claims compiled into one report per event.", null), BorderLayout.NORTH);
        evBox.setFont(f(Font.PLAIN, 13)); evBox.setPreferredSize(new Dimension(240, 32));
        sum.setFont(f(Font.PLAIN, 12)); sum.setForeground(MUTED);
        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)); ctrl.setOpaque(false);
        ctrl.add(new JLabel("Event:")); ctrl.add(evBox);
        JPanel ctrlRight = row(fileBtn, exportBtn);
        JPanel bar = new JPanel(new BorderLayout()); bar.setOpaque(false);
        bar.add(ctrl, BorderLayout.WEST); bar.add(ctrlRight, BorderLayout.EAST);
        JPanel top = new JPanel(new BorderLayout(0, 6)); top.setOpaque(false);
        top.add(bar, BorderLayout.NORTH); top.add(sum, BorderLayout.SOUTH);
        JPanel grid = new JPanel(new GridLayout(2, 1, 0, 16)); grid.setOpaque(false);
        grid.add(cardWith("Claims Included in Report", null, scroll(t)));
        grid.add(cardWith("Filed Liquidation Reports", row(delRepBtn, reviewBtn, expRepBtn), scroll(rt)));
        JPanel center = new JPanel(new BorderLayout(0, 12)); center.setOpaque(false);
        center.add(top, BorderLayout.NORTH); center.add(grid, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        evBox.addActionListener(e -> { if (!loading) reload(); });
        fileBtn.addActionListener(e -> {
            String ev = (String) evBox.getSelectedItem();
            List<Claim> el = ev == null ? new ArrayList<>() : eligible(ev);
            if (el.isEmpty()) { warn(this, "No approved or cleared claims for this event yet."); return; }
            double total = 0;
            for (Claim c : el) total += c.getAmount();
            Report r = new Report(nextReportId(), ev, el.size(), total, event(ev).getAllocated(), current().getName(), LocalDate.now().toString());
            addReport(r);
            log("Filed liquidation report " + r.getId() + " for " + ev + " (" + r.getCount() + " claims, " + peso(r.getTotal()) + ")");
            app().refreshAll();
            msg(this, "Liquidation report " + r.getId() + " filed.");
        });
        reviewBtn.addActionListener(e -> {
            int r = rt.getSelectedRow();
            if (r < 0) { msg(this, "Select a filed report first."); return; }
            Report rep = reports().get(r);
            if (!rep.review(current().getName())) { msg(this, rep.getId() + " has already been reviewed."); return; }
            saveReport(rep);
            log("Reviewed liquidation report " + rep.getId() + " (" + rep.getEvent() + ")");
            app().refreshAll();
        });
        delRepBtn.addActionListener(e -> {
            int r = rt.getSelectedRow();
            if (r < 0) { msg(this, "Select a filed report first."); return; }
            Report rep = reports().get(r);
            // OOP [POLYMORPHISM]: the report's status decides - reviewed reports are locked for the audit trail
            if (!rep.getStatus().canDelete()) { warn(this, rep.getId() + " has been reviewed by the moderator and can no longer be deleted."); return; }
            if (!confirm(this, "Delete report " + rep.getId() + " for \"" + rep.getEvent() + "\"?\nYou can file a new one afterwards.", "Delete report")) return;
            removeReport(rep);
            log("Deleted liquidation report " + rep.getId() + " (" + rep.getEvent() + ")");
            app().refreshAll();
        });
        exportBtn.addActionListener(e -> {
            String ev = (String) evBox.getSelectedItem();
            if (ev == null) return;
            export(this, "Liquidation_" + ev.replaceAll("\\W+", "_") + ".txt", reportText(ev));
        });
        expRepBtn.addActionListener(e -> exportCsv(this, "BreakEven_LiquidationReports.csv",
                "report_id,event,claims,total,allocated,filed_by,date,status,reviewed_by", reports()));
    }

    static String reportText(String ev) {
        StringBuilder sb = new StringBuilder();
        sb.append("LIQUIDATION REPORT - ").append(ev).append("\nCCS Student Council, " + semester().getLabel() + "\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(TS)).append("\n\n");
        sb.append(String.format("%-10s %-38s %-18s %-12s %14s%n", "REF", "DESCRIPTION", "CATEGORY", "OR NO.", "AMOUNT"));
        double tot = 0;
        for (Claim c : eligible(ev)) {
            String d = c.getDesc();
            sb.append(String.format("%-10s %-38s %-18s %-12s %14s%n", c.getRef(),
                    d.length() > 37 ? d.substring(0, 37) : d, c.getCategory(), c.getReceiptNo(), FMT.format(c.getAmount())));
            tot += c.getAmount();
        }
        double proj = event(ev).getAllocated();
        sb.append("\nTotal liquidated: ").append(php(tot)).append("\nAllocated budget: ").append(php(proj));
        sb.append("\nVariance:         ").append(php(tot - proj)).append("\n");
        return sb.toString();
    }

    private void reload() {
        m.setRowCount(0);
        String ev = (String) evBox.getSelectedItem();
        if (ev == null) { sum.setText(" "); return; }
        double tot = 0;
        for (Claim c : eligible(ev)) {
            m.addRow(new Object[]{c.getRef(), c.getDesc(), c.getCategory(), c.getDate(), c.getReceiptNo(), peso(c.getAmount()), c.getStatus().label()});
            tot += c.getAmount();
        }
        int excl = 0;
        for (Claim c : claimsFor(ev)) if (!c.getStatus().isLiquidationEligible()) excl++;
        double proj = event(ev).getAllocated();
        sum.setText("Allocated: " + peso(proj) + "    Actual: " + peso(tot) + "    Variance: " + (tot - proj > 0 ? "+" : "") + peso(tot - proj)
                + "    (" + excl + " pending/flagged claim(s) excluded)");
    }

    @Override
    void refresh() {
        User u = current();
        boolean open = editable();   // closed semesters are read-only
        fileBtn.setVisible(u.canFileReports() && open);
        delRepBtn.setVisible(u.canFileReports() && open);
        reviewBtn.setVisible(u.canReviewReports() && open);
        loading = true;
        Object sel = evBox.getSelectedItem();
        evBox.removeAllItems();
        for (Event e : events()) evBox.addItem(e.getName());
        if (sel != null) evBox.setSelectedItem(sel);
        loading = false;
        reload();
        rm.setRowCount(0);
        for (Report r : reports())
            rm.addRow(new Object[]{r.getId(), r.getEvent(), r.getCount(), peso(r.getTotal()), peso(r.getProjected()), r.getFiledBy(), r.getDate(), r.getStatus().label()});
    }
}
