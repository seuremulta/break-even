package breakeven;

import java.util.Arrays;
import java.util.List;

/** OOP [INHERITANCE]: a Moderator IS-A User. Grants clearance, sets the semester budget, reviews reports, exports the audit log, watches trends. */
class Moderator extends User {

    Moderator(String username, String password, String name) { super(username, password, name); }

    @Override String getRole() { return "Moderator"; }

    @Override List<String> navItems() {
        return Arrays.asList("Dashboard", "Budget Allocation", "Clearance", "Liquidation", "Variance Trends", "Audit Log");
    }

    @Override String loginNotice() {
        int claimsWaiting = 0, reportsWaiting = 0;
        for (Claim c : Store.claims()) if (c.getStatus() == ClaimStatus.APPROVED) claimsWaiting++;
        for (Report r : Store.reports()) if (r.getStatus() == ReportStatus.FILED) reportsWaiting++;
        if (claimsWaiting == 0 && reportsWaiting == 0) return null;
        return claimsWaiting + " auditor-approved claim(s) await clearance.\n" + reportsWaiting + " liquidation report(s) await your review.";
    }

    @Override boolean canGrantClearance() { return true; }
    @Override boolean canSetBudget()      { return true; }
    @Override boolean canReviewReports()  { return true; }
    @Override boolean canExportAuditLog() { return true; }

    @Override ClaimStatus reviewQueueStatus() { return ClaimStatus.APPROVED; }
    @Override String reviewTitle()    { return "Financial Clearance"; }
    @Override String reviewSubtitle() { return "Auditor-approved claims awaiting your clearance."; }
}
