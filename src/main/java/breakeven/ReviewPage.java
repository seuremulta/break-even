package breakeven;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static breakeven.ClaimDetails.claimDetail;
import static breakeven.Store.*;
import static breakeven.Theme.*;
import static breakeven.Ui.*;

/**
 * One page, two roles: the Auditor sees "Review Queue", the Moderator sees "Clearance".
 * OOP [POLYMORPHISM]: the title, queue and buttons come from the current User object
 * (reviewTitle(), reviewQueueStatus(), canReviewClaims()...), not from role-name comparisons.
 */
class ReviewPage extends Page {
    private final DefaultTableModel m = model("Ref No.", "Description", "Amount", "Claimant", "Status");
    private final JTable t = table(m, 40);
    private final List<Claim> shown = new ArrayList<>();
    private final JTextArea detail = new JTextArea();
    private final JButton approve = btn("Approve Claim", true), flag = btn("Flag Discrepancy", false), grant = btn("Grant Clearance", true);
    private final JLabel title = new JLabel(), sub = new JLabel();

    ReviewPage() {
        JPanel head = new JPanel(); head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS)); head.setOpaque(false);
        title.setFont(f(Font.PLAIN, 24)); sub.setFont(f(Font.PLAIN, 13)); sub.setForeground(MUTED);
        head.add(title); head.add(sub);
        add(head, BorderLayout.NORTH);
        detail.setEditable(false); detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detail.setBorder(new EmptyBorder(10, 12, 10, 12));
        JPanel actions = row(flag, approve, grant);
        actions.setBorder(new EmptyBorder(8, 0, 8, 8));
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE); body.add(scroll(detail), BorderLayout.CENTER); body.add(actions, BorderLayout.SOUTH);
        JPanel right = cardWith("Claim Detail & Allocation Check", null, body);
        JPanel left = cardWith("Queue", null, scroll(t));
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        sp.setResizeWeight(0.5); sp.setBorder(null); sp.setOpaque(false); sp.setDividerSize(12);
        add(sp, BorderLayout.CENTER);
        t.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Claim c = sel();
            detail.setText(c == null ? "Select a claim from the queue." : claimDetail(c));
            detail.setCaretPosition(0);
        });
        approve.addActionListener(e -> {
            Claim c = sel(); if (c == null) { msg(this, "Select a claim first."); return; }
            Store.approve(c); app().refreshAll();
        });
        flag.addActionListener(e -> {
            Claim c = sel(); if (c == null) { msg(this, "Select a claim first."); return; }
            String note = JOptionPane.showInputDialog(this, "Discrepancy note for the treasurer:");
            if (note == null || note.trim().isEmpty()) return;
            Store.flag(c, note.trim()); app().refreshAll();
        });
        grant.addActionListener(e -> {
            Claim c = sel(); if (c == null) { msg(this, "Select a claim first."); return; }
            String err = clearClaim(c);                       // OOP [ABSTRACTION]: rule lives in Store
            if (err != null) { warn(this, err); return; }
            app().refreshAll();
            msg(this, "Clearance granted for " + c.getRef() + ".\nBudget balance updated; reimbursement cleared for " + c.getClaimant() + ".");
        });
    }

    private Claim sel() { int r = t.getSelectedRow(); return r < 0 || r >= shown.size() ? null : shown.get(r); }

    @Override
    void refresh() {
        User u = current();
        title.setText(u.reviewTitle());
        sub.setText(u.reviewSubtitle());
        boolean open = editable();   // closed semesters are read-only
        approve.setVisible(u.canReviewClaims() && open); flag.setVisible(u.canReviewClaims() && open); grant.setVisible(u.canGrantClearance() && open);
        shown.clear(); m.setRowCount(0);
        List<Claim> l = new ArrayList<>(claims());
        l.sort(Comparator.comparing(Claim::getRef));
        for (Claim c : l) {
            if (c.getStatus() != u.reviewQueueStatus()) continue;
            shown.add(c);
            m.addRow(new Object[]{c.getRef(), c.getDesc(), peso(c.getAmount()), c.getClaimant(), c.getStatus().label()});
        }
        detail.setText(shown.isEmpty() ? "Nothing in the queue." : "Select a claim from the queue.");
    }
}
