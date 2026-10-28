package breakeven;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import static breakeven.Store.*;
import static breakeven.Theme.*;
import static breakeven.Ui.*;

/**
 * Budget allocation per EVENT. Whatever is not allocated to an event sits in the contingency reserve
 * or stays unallocated, and both are shown as rows under the events.
 * Roles (OOP [POLYMORPHISM], via User permissions):
 *   Treasurer - canManageBudget(): creates / edits / deletes event allocations.
 *   Moderator - canSetBudget(): sets the semester total and contingency, draws on contingency, starts a new semester.
 * Past (closed) semesters are shown read-only.
 * OOP [INHERITANCE]: extends Page. Overrides refresh().
 */
class BudgetPage extends Page {
    private final DefaultTableModel em = model("Event", "Allocated", "Claimed", "Spent (Cleared)", "Remaining", "Utilization", "Remark");
    private final JTable et = table(em, 40);
    private final JLabel summary = new JLabel();
    private final JButton addBtn = btn("+ Event Allocation", true), editBtn = btn("Edit Allocation", false), delBtn = smallBtn("Delete Event"),
            totalBtn = btn("Set Total Budget", false), contBtn = btn("Set Contingency", false),
            drawBtn = btn("Draw from Contingency", false), semBtn = btn("Start New Semester", true);

    BudgetPage() {
        add(pageHeader("Budget Allocation", "The budget is split per event. What is not allocated sits in contingency or stays unallocated.", null), BorderLayout.NORTH);
        summary.setFont(f(Font.PLAIN, 12)); summary.setForeground(MUTED);
        JPanel bar = new JPanel(new BorderLayout(0, 8)); bar.setOpaque(false);
        bar.add(summary, BorderLayout.NORTH);
        bar.add(row(totalBtn, contBtn, drawBtn, semBtn), BorderLayout.CENTER);
        JPanel center = new JPanel(new BorderLayout(0, 12)); center.setOpaque(false);
        center.add(bar, BorderLayout.NORTH);
        center.add(cardWith("Budget by Event", row(delBtn, editBtn, addBtn), scroll(et)), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        addBtn.addActionListener(e -> addAllocation());
        editBtn.addActionListener(e -> editAllocation());
        delBtn.addActionListener(e -> deleteEvent());
        totalBtn.addActionListener(e -> setTotal());
        contBtn.addActionListener(e -> setContingency());
        drawBtn.addActionListener(e -> drawContingency());
        semBtn.addActionListener(e -> startNewSemester());
    }

    private Event selectedEvent() {
        int r = et.getSelectedRow();
        if (r < 0 || r >= events().size()) { msg(this, "Select an event row first (not the Contingency / Unallocated rows)."); return null; }
        return events().get(r);
    }

    // ---------------------------------------------------------------- Treasurer: allocations per event
    private void addAllocation() {
        String name = JOptionPane.showInputDialog(this, "Event name:");
        if (name == null || name.trim().isEmpty()) return;
        String in = JOptionPane.showInputDialog(this, "Budget to allocate to \"" + name.trim() + "\" (PHP):");
        if (in == null) return;
        Double v = num(in);
        if (v == null) { warn(this, "Enter a valid amount."); return; }
        Event e = event(name.trim());
        if (e != null && v < spent(e)) { warn(this, "Allocation cannot be lower than what is already spent (" + peso(spent(e)) + ")."); return; }
        if (!budget().canAllocate(e, v)) {
            warn(this, "Not enough unallocated budget. You can allocate at most " + peso(budget().unallocated() + (e == null ? 0 : e.getAllocated())) + ".");
            return;
        }
        if (e == null) { addEvent(new Event(name.trim(), v)); log("Event allocation created: " + name.trim() + " (" + peso(v) + ")"); }
        else { log("Changed " + e.getName() + " allocation " + peso(e.getAllocated()) + " -> " + peso(v)); e.setAllocated(v); }
        app().refreshAll();
    }

    private void editAllocation() {
        Event e = selectedEvent();
        if (e == null) return;
        String in = JOptionPane.showInputDialog(this, "New allocation for " + e.getName() + " (PHP):", String.valueOf(e.getAllocated()));
        if (in == null) return;
        Double v = num(in);
        if (v == null) { warn(this, "Enter a valid positive amount."); return; }
        if (v < spent(e)) { warn(this, "Allocation cannot be lower than what is already spent (" + peso(spent(e)) + ")."); return; }
        // OOP [ABSTRACTION]: Budget hides how the "fits in the semester budget" rule is computed
        if (!budget().canAllocate(e, v)) {
            warn(this, "Not enough unallocated budget. You can allocate at most " + peso(budget().unallocated() + e.getAllocated()) + " to this event.");
            return;
        }
        log("Changed " + e.getName() + " allocation " + peso(e.getAllocated()) + " -> " + peso(v));
        e.setAllocated(v);   // OOP [ENCAPSULATION]: goes through the validated setter
        app().refreshAll();
    }

    private void deleteEvent() {
        Event e = selectedEvent();
        if (e == null) return;
        int n = claimsFor(e.getName()).size();
        if (n > 0) {
            warn(this, "\"" + e.getName() + "\" still has " + n + " claim(s) logged against it.\nDelete or move those claims first, then remove the event.");
            return;
        }
        if (!confirm(this, "Delete the event \"" + e.getName() + "\" and release its " + peso(e.getAllocated()) + " back to unallocated?", "Delete event")) return;
        removeEvent(e);
        log("Deleted event allocation: " + e.getName() + " (" + peso(e.getAllocated()) + ")");
        app().refreshAll();
    }

    // ---------------------------------------------------------------- Moderator: semester-level budget
    private void setTotal() {
        Budget b = budget();
        String in = JOptionPane.showInputDialog(this, "Total council budget for " + semester().getLabel() + " (PHP):", String.valueOf(b.getTotalAmount()));
        if (in == null) return;
        Double v = num(in);
        if (v == null || v <= 0) { warn(this, "Enter a valid amount greater than zero."); return; }
        if (!b.canSetTotal(v)) { warn(this, "The total cannot be lower than what is already committed to events and contingency (" + peso(b.committed()) + ")."); return; }
        log("Changed total budget for " + semester().getLabel() + " " + peso(b.getTotalAmount()) + " -> " + peso(v));
        b.setTotalAmount(v);
        app().refreshAll();
    }

    private void setContingency() {
        Budget b = budget();
        String in = JOptionPane.showInputDialog(this, "Contingency reserve (PHP):", String.valueOf(b.getContingency()));
        if (in == null) return;
        Double v = num(in);
        if (v == null) { warn(this, "Enter a valid amount."); return; }
        if (!b.canSetContingency(v)) { warn(this, "Contingency can be at most " + peso(b.getTotalAmount() - b.totalAllocated()) + " (total minus event allocations)."); return; }
        log("Changed contingency " + peso(b.getContingency()) + " -> " + peso(v));
        b.setContingency(v);
        app().refreshAll();
    }

    private void drawContingency() {
        Budget b = budget();
        Event e = selectedEvent();
        if (e == null) return;
        if (b.getContingency() <= 0) { msg(this, "There is no contingency left to draw from."); return; }
        String in = JOptionPane.showInputDialog(this, "Move how much from contingency (" + peso(b.getContingency()) + " left) to \"" + e.getName() + "\" (PHP)?");
        if (in == null) return;
        Double v = num(in);
        if (v == null || v <= 0) { warn(this, "Enter a valid amount greater than zero."); return; }
        if (v > b.getContingency()) { warn(this, "Only " + peso(b.getContingency()) + " is left in contingency."); return; }
        b.drawContingency(e, v);
        log("Drew " + peso(v) + " from contingency into " + e.getName() + " (now " + peso(e.getAllocated()) + ")");
        app().refreshAll();
    }

    private void startNewSemester() {
        Semester open = currentSemester();
        String label = JOptionPane.showInputDialog(this, "Name of the new semester\n(e.g. A.Y. 2026-2027 - 2nd Sem):");
        if (label == null || label.trim().isEmpty()) return;
        for (Semester s : semesters())
            if (s.getLabel().equalsIgnoreCase(label.trim())) { warn(this, "A semester with that name already exists."); return; }
        String in = JOptionPane.showInputDialog(this, "Total council budget for " + label.trim() + " (PHP):", String.valueOf(open.getBudget().getTotalAmount()));
        if (in == null) return;
        Double v = num(in);
        if (v == null || v <= 0) { warn(this, "Enter a valid amount greater than zero."); return; }
        int notCleared = 0;
        if (!open.isClosed()) for (Claim c : open.getClaims()) if (c.getStatus() != ClaimStatus.CLEARED) notCleared++;
        String note = open.isClosed() ? "" : "This closes \"" + open.getLabel() + "\". It stays viewable but becomes read-only.\n"
                + (notCleared > 0 ? notCleared + " claim(s) there are not cleared yet and will stay locked in that semester.\n" : "");
        if (!confirm(this, note + "Start \"" + label.trim() + "\" with a total budget of " + peso(v) + "?", "Start new semester")) return;
        if (!open.isClosed()) log("Closed semester " + open.getLabel());
        startSemester(label.trim(), v);
        log("Started semester " + label.trim() + " with a total budget of " + peso(v));
        app().refreshAll();
    }

    // ---------------------------------------------------------------- display
    @Override
    void refresh() {
        User u = current();
        boolean open = editable();
        boolean treasurer = u.canManageBudget() && open, moderator = u.canSetBudget() && open;
        addBtn.setVisible(treasurer); editBtn.setVisible(treasurer); delBtn.setVisible(treasurer);
        totalBtn.setVisible(moderator); contBtn.setVisible(moderator); drawBtn.setVisible(moderator);
        semBtn.setVisible(u.canSetBudget());

        Budget b = budget();
        summary.setText(semester().getLabel() + (open ? "" : "  [CLOSED - read only]")
                + "     Total " + peso(b.getTotalAmount()) + "   |   Events " + peso(b.totalAllocated())
                + "   |   Contingency " + peso(b.getContingency()) + "   |   Unallocated " + peso(b.unallocated()));
        em.setRowCount(0);
        for (Event e : events()) {
            double sp = spent(e);
            em.addRow(new Object[]{e.getName(), peso(e.getAllocated()), peso(actual(e)), peso(sp), peso(e.computeRemaining(sp)),
                    String.format("%.1f%%", e.utilization(sp) * 100), e.remark(actual(e))});
        }
        double total = b.getTotalAmount();
        em.addRow(new Object[]{"Contingency Fund", peso(b.getContingency()), "-", "-", "-",
                String.format("%.1f%% of budget", total == 0 ? 0 : b.getContingency() / total * 100), "Reserved"});
        em.addRow(new Object[]{"Unallocated", peso(b.unallocated()), "-", "-", "-",
                String.format("%.1f%% of budget", total == 0 ? 0 : b.unallocated() / total * 100), "Free to allocate"});
    }
}
