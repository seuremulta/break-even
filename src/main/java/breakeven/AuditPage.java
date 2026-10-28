package breakeven;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;

import static breakeven.Store.*;
import static breakeven.Ui.*;

/** OOP [INHERITANCE]: extends Page. Overrides refresh() (OOP [POLYMORPHISM]). */
class AuditPage extends Page {
    private final DefaultTableModel m = model("Log ID", "Timestamp", "Reviewer / User", "Action");
    private final JTable t = table(m, 36);
    private final JButton exp = btn("Export Audit Log (.csv)", true);

    AuditPage() {
        add(pageHeader("Audit Log", "Every review action, flag and clearance - timestamped with identity.", row(exp)), BorderLayout.NORTH);
        add(cardWith("Trail", null, scroll(t)), BorderLayout.CENTER);
        t.getColumnModel().getColumn(0).setPreferredWidth(90);
        t.getColumnModel().getColumn(1).setPreferredWidth(160);
        t.getColumnModel().getColumn(2).setPreferredWidth(220);
        t.getColumnModel().getColumn(3).setPreferredWidth(480);
        // OOP [POLYMORPHISM]: exportCsv() just calls toCsvRow() on each Exportable; LogEntry supplies its own row
        exp.addActionListener(e -> exportCsv(this, "BreakEven_AuditLog.csv", "log_id,timestamp,user,action", logs()));
    }

    @Override
    void refresh() {
        exp.setVisible(current().canExportAuditLog());
        m.setRowCount(0);
        for (LogEntry l : logs()) m.addRow(new Object[]{l.getId(), l.getTime(), l.getUser(), l.getAction()});
    }
}
