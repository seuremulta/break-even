package breakeven;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;

import static breakeven.Store.*;
import static breakeven.Theme.*;
import static breakeven.Ui.*;

/**
 * The expense-claim form (also used to edit & resubmit a flagged claim).
 * OOP [INHERITANCE]: extends JDialog. OOP [ENCAPSULATION]: all form state is private.
 */
class ExpenseDialog extends JDialog {
    private final PH desc = new PH("e.g. Stage Rental - Event Name"), claimant = new PH("Officer name"),
            amount = new PH("0.00"), receipt = new PH("OR-2025-XXXX"), date = new PH("yyyy-mm-dd");
    private final JComboBox<String> event = new JComboBox<>(), category = new JComboBox<>();
    private final JTextArea notes = new JTextArea(3, 20);
    private final JButton attach = btn("Attach photo/PDF...", false);
    private String attachedPath = "";
    private final Claim edit;

    ExpenseDialog(Claim edit) {
        super(app(), edit == null ? "Log Expense Claim" : "Edit & Resubmit Claim", true);
        this.edit = edit;
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel(edit == null ? "Log Expense Claim" : "Resubmit Claim");
        title.setFont(f(Font.BOLD, 17));
        JLabel ref = new JLabel("Auto-ref: " + (edit == null ? nextRef() : edit.getRef()));
        ref.setFont(f(Font.PLAIN, 12)); ref.setForeground(MUTED);
        JPanel top = new JPanel(new GridLayout(2, 1)); top.setOpaque(false);
        top.add(title); top.add(ref);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; g.anchor = GridBagConstraints.WEST; g.insets = new Insets(0, 6, 8, 6);
        p.add(top, g);

        for (Event e : events()) event.addItem(e.getName());
        for (String t : expenseTypes()) category.addItem(t);
        for (JComboBox<String> cb : Arrays.asList(event, category)) { cb.setFont(f(Font.PLAIN, 13)); cb.setBackground(FIELD); }
        notes.setFont(f(Font.PLAIN, 13)); notes.setLineWrap(true); notes.setWrapStyleWord(true);
        notes.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane ns = new JScrollPane(notes);
        ns.setBorder(new LineBorder(BORDER_C)); ns.getViewport().setBackground(FIELD);
        date.setText(LocalDate.now().toString());
        attach.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Receipt (png, jpg, pdf)", "png", "jpg", "jpeg", "pdf"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                attachedPath = fc.getSelectedFile().getAbsolutePath();
                attach.setText(fc.getSelectedFile().getName());
            }
        });

        put(p, "Description", desc, 0, 1, 2);
        put(p, "Claimant", claimant, 0, 2, 1);   put(p, "Amount (" + PESO + ")", amount, 1, 2, 1);
        put(p, "Receipt / OR No.", receipt, 0, 3, 1); put(p, "Date", date, 1, 3, 1);
        put(p, "Charge to Event", event, 0, 4, 1);  put(p, "Receipt File", attach, 1, 4, 1);
        put(p, "Expense Type", category, 0, 5, 2);
        put(p, "Notes / Remarks", ns, 0, 6, 2);

        if (edit != null) {
            desc.setText(edit.getDesc()); claimant.setText(edit.getClaimant()); amount.setText(String.valueOf(edit.getAmount()));
            receipt.setText(edit.getReceiptNo()); date.setText(edit.getDate()); event.setSelectedItem(edit.getEvent());
            category.setSelectedItem(edit.getCategory()); notes.setText(edit.getNotes()); attachedPath = edit.getReceiptFile();
            if (!attachedPath.isEmpty()) attach.setText(new File(attachedPath).getName());
        }

        JButton submit = btn(edit == null ? "Submit Claim" : "Resubmit Claim", true), cancel = btn("Cancel", false);
        submit.addActionListener(e -> submit());
        cancel.addActionListener(e -> dispose());
        JPanel bp = new JPanel(new BorderLayout(10, 0)); bp.setOpaque(false);
        bp.add(submit, BorderLayout.CENTER); bp.add(cancel, BorderLayout.EAST);
        g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 7; g.gridwidth = 2; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.insets = new Insets(10, 6, 0, 6);
        p.add(bp, g);

        setContentPane(p);
        setPreferredSize(new Dimension(560, 640));
        pack();
        setLocationRelativeTo(app());
    }

    private static void put(JPanel p, String lab, JComponent c, int x, int y, int w) {
        JPanel b = new JPanel(new BorderLayout(0, 4)); b.setOpaque(false);
        JLabel l = new JLabel(lab.toUpperCase()); l.setFont(f(Font.BOLD, 11));
        b.add(l, BorderLayout.NORTH); b.add(c, BorderLayout.CENTER);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x; g.gridy = y; g.gridwidth = w; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(5, 6, 5, 6);
        p.add(b, g);
    }

    private void submit() {
        String d = desc.getText().trim(), cl = claimant.getText().trim(), rc = receipt.getText().trim(),
                dt = date.getText().trim(), ev = event.getSelectedItem() == null ? "" : event.getSelectedItem().toString().trim();
        if (d.isEmpty() || cl.isEmpty() || rc.isEmpty() || ev.isEmpty()) { warn(this, "Description, claimant, receipt/OR number and event are required.\n(Events are created under Budget Allocation first.)"); return; }
        Double amt = num(amount.getText());
        if (amt == null || amt <= 0) { warn(this, "Enter a valid amount greater than zero."); return; }
        try { LocalDate.parse(dt); } catch (Exception ex) { warn(this, "Date must be in yyyy-mm-dd format."); return; }

        Event target = event(ev);
        if (target == null) { warn(this, "That event has no budget allocation yet. Create it under Budget Allocation first."); return; }
        String type = (String) category.getSelectedItem();
        double rem = remaining(target);
        ClaimStatus status = ClaimStatus.PENDING;
        String disc = "";
        if (amt > rem) {   // automatic discrepancy calculation
            disc = "System: exceeds remaining " + target.getName() + " allocation (" + peso(rem) + ") by " + peso(amt - rem);
            int ok = JOptionPane.showConfirmDialog(this, disc.substring(8) + ".\nThe claim will be flagged automatically. Submit anyway?",
                    "Allocation exceeded", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
            status = ClaimStatus.FLAGGED;
        }
        String ref = edit == null ? nextRef() : edit.getRef();
        String stored = ReceiptVault.archive(ref, attachedPath);     // keep a safe copy of the receipt
        Claim x;
        if (edit == null) {
            x = new Claim(ref, d, cl, amt, type, dt, rc, ev, status, disc);
            x.update(d, cl, amt, rc, dt, ev, type, notes.getText().trim(), stored);
            addClaim(x);
            log("Submitted claim " + x.getRef() + " (" + peso(amt) + ")");
        } else {
            x = edit;
            x.update(d, cl, amt, rc, dt, ev, type, notes.getText().trim(), stored);   // OOP [ENCAPSULATION]: validated update
            x.resubmit(status, disc);
            log("Resubmitted claim " + x.getRef() + " (" + peso(amt) + ")");
        }
        if (status == ClaimStatus.FLAGGED) log("System flagged " + x.getRef() + ": " + disc.substring(8));
        dispose();
        app().refreshAll();
    }
}

