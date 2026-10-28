package breakeven;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static breakeven.Store.*;
import static breakeven.Theme.*;

/** Builds and shows the detail text for a single expense claim. */
final class ClaimDetails {

    private ClaimDetails() {}

    static String claimDetail(Claim c) {
        Event k = event(c.getEvent());
        StringBuilder sb = new StringBuilder();
        sb.append("Ref No.:       ").append(c.getRef()).append("\n");
        sb.append("Description:   ").append(c.getDesc()).append("\n");
        sb.append("Claimant:      ").append(c.getClaimant()).append("\n");
        sb.append("Amount:        ").append(peso(c.getAmount())).append("\n");
        sb.append("Date:          ").append(c.getDate()).append("\n");
        sb.append("Receipt OR No: ").append(c.getReceiptNo()).append("\n");
        sb.append("Receipt file:  ").append(c.hasReceipt() ? c.getReceiptFile() : "(none attached)").append("\n");
        sb.append("Event:         ").append(c.getEvent()).append("\n");
        sb.append("Category:      ").append(c.getCategory()).append("\n");
        sb.append("Status:        ").append(c.getStatus().label()).append("\n");
        if (!c.getNotes().isEmpty()) sb.append("Notes:         ").append(c.getNotes()).append("\n");
        if (!c.getDiscrepancy().isEmpty()) sb.append("Discrepancy:   ").append(c.getDiscrepancy()).append("\n");
        if (k != null) {
            boolean cleared = c.getStatus() == ClaimStatus.CLEARED;
            double rem = remaining(k), after = rem - (cleared ? 0 : c.getAmount());
            sb.append("\n--- Event allocation check: ").append(k.getName()).append(" ---\n");
            sb.append("Allocation ceiling: ").append(peso(k.getAllocated())).append("\n");
            sb.append("Spent (cleared):    ").append(peso(spent(k))).append("\n");
            sb.append("Remaining:          ").append(peso(rem)).append("\n");
            if (!cleared)
                sb.append("After this claim:   ").append(peso(after)).append(after < 0 ? "   ** OVER ALLOCATION **" : "").append("\n");
        }
        // The claim's paper trail, pulled from the audit log (proposal: "track the status of submitted claims")
        List<LogEntry> hist = historyFor(c);
        if (!hist.isEmpty()) {
            sb.append("\n--- History (audit trail) ---\n");
            for (LogEntry l : hist)
                sb.append(l.getTime()).append("  ").append(l.getUser()).append("\n    ").append(l.getAction()).append("\n");
        }
        return sb.toString();
    }

    static void showDetails(Component parent, Claim c) {
        JTextArea ta = new JTextArea(claimDetail(c));
        ta.setEditable(false); ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); ta.setBackground(FIELD);
        ta.setBorder(new EmptyBorder(8, 8, 8, 8));
        ta.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(560, 440));

        // OOP [POLYMORPHISM]: the claim's status and the user's role decide which buttons appear
        boolean canEdit = current().canSubmitClaims() && c.getStatus().canEdit() && editable();
        List<String> opts = new ArrayList<>();
        if (canEdit) opts.add("Edit & Resubmit");
        if (c.hasReceipt()) opts.add("Open Receipt");
        opts.add("Close");
        Object[] arr = opts.toArray();
        int r = JOptionPane.showOptionDialog(parent, sp, "Claim " + c.getRef(), JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, arr, arr[arr.length - 1]);
        if (r < 0) return;
        String pick = (String) arr[r];
        if (pick.equals("Edit & Resubmit")) new ExpenseDialog(c).setVisible(true);
        else if (pick.equals("Open Receipt")) ReceiptVault.open(parent, c);
    }
}
