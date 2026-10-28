package breakeven;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static breakeven.Store.*;
import static breakeven.Theme.*;
import static breakeven.Ui.*;

/**
 * NEW (proposal, Moderator: "Monitor council-wide budget utilization and variance trends";
 * key feature: "see where their planning consistently over- or under-shoots").
 * Shows whichever semester is selected in the sidebar.
 * OOP [INHERITANCE]: extends Page. Overrides refresh() (OOP [POLYMORPHISM]).
 */
class TrendsPage extends Page {
    private final JLabel utilL = new JLabel(), utilSub = new JLabel(), clearL = new JLabel(), clearSub = new JLabel(),
            varL = new JLabel(), varSub = new JLabel(), tendency = new JLabel(" ");
    private final BarChart evChart = new BarChart("Allocated", new Color(0xB9CFC4), "Spent (cleared)", GREEN);
    private final BarChart typeChart = new BarChart("Claimed", new Color(0xB9CFC4), "Cleared", ORANGE);
    private final DefaultTableModel m = model("Event", "Allocated", "Actual", "Variance", "Variance %", "Remark");
    private final JTable t = table(m, 34);

    TrendsPage() {
        JPanel north = new JPanel(new BorderLayout(0, 18)); north.setOpaque(false);
        north.add(pageHeader("Variance Trends", "Council-wide budget utilization and where planning over- or under-shoots.", null), BorderLayout.NORTH);
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); stats.setOpaque(false);
        stats.add(statCard("Budget Utilization", utilL, utilSub, GREEN)); stats.add(Box.createHorizontalStrut(16));
        stats.add(statCard("Cleared Spending", clearL, clearSub, TEXT)); stats.add(Box.createHorizontalStrut(16));
        stats.add(statCard("Net Planning Variance", varL, varSub, ORANGE));
        north.add(stats, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);

        tendency.setFont(f(Font.PLAIN, 12)); tendency.setForeground(MUTED);
        JPanel charts = new JPanel(new GridLayout(1, 2, 16, 0)); charts.setOpaque(false);
        charts.add(cardWith("Allocation vs Cleared Spending by Event", null, evChart));
        charts.add(cardWith("Spending by Expense Type", null, typeChart));
        JPanel grid = new JPanel(new GridLayout(2, 1, 0, 16)); grid.setOpaque(false);
        grid.add(charts);
        grid.add(cardWith("Variance per Event", tendency, scroll(t)));
        add(grid, BorderLayout.CENTER);
    }

    @Override
    void refresh() {
        double total = budget().getTotalAmount(), spentAll = totalSpent();
        utilL.setText(String.format("%.1f%%", total == 0 ? 0 : spentAll / total * 100));
        utilSub.setText("of the semester budget cleared");
        clearL.setText(peso(spentAll)); clearSub.setText("of " + peso(budget().totalAllocated()) + " allocated to events");

        int n = events().size();
        List<String> el = new ArrayList<>();
        double[] ea = new double[n], es = new double[n];
        double sumAlloc = 0, sumAct = 0; int over = 0, under = 0, on = 0, i = 0;
        m.setRowCount(0);
        for (Event e : events()) {
            double a = actual(e), v = e.variance(a);
            el.add(e.getName()); ea[i] = e.getAllocated(); es[i] = spent(e); i++;
            sumAlloc += e.getAllocated(); sumAct += a;
            if (e.getAllocated() > 0) { if (v > 0) over++; else if (v < 0) under++; else on++; }
            m.addRow(new Object[]{e.getName(), peso(e.getAllocated()), peso(a), (v > 0 ? "+" : "") + peso(v),
                    e.getAllocated() == 0 ? "-" : String.format("%+.1f%%", v / e.getAllocated() * 100), e.remark(a)});
        }
        evChart.setData(el, ea, es);

        List<String> tl = new ArrayList<>(expenseTypes());
        double[] claimed = new double[tl.size()], cleared = new double[tl.size()];
        for (Claim c : claims()) {
            int k = tl.indexOf(c.getCategory());
            if (k < 0) continue;
            if (c.getStatus().countsTowardActual()) claimed[k] += c.getAmount();
            if (c.getStatus() == ClaimStatus.CLEARED) cleared[k] += c.getAmount();
        }
        typeChart.setData(tl, claimed, cleared);

        double net = sumAct - sumAlloc;
        varL.setText((net > 0 ? "+" : "") + peso(net));
        varSub.setText(over + " over-shot, " + under + " under-shot, " + on + " on target");
        tendency.setText(over > under ? "Planning tends to OVER-SHOOT" : under > over ? "Planning tends to UNDER-SHOOT" : "No consistent over/under-shoot");
    }
}
