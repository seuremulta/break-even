package breakeven;

import java.util.Arrays;
import java.util.List;

/** OOP [INHERITANCE]: a Treasurer IS-A User. Logs expenses, manages the budget, files liquidation reports. */
class Treasurer extends User {

    Treasurer(String username, String password, String name) { super(username, password, name); }

    @Override String getRole() { return "Treasurer"; }

    @Override List<String> navItems() { return Arrays.asList("Dashboard", "Budget Allocation", "Liquidation"); }

    @Override String loginNotice() {
        int n = 0;
        for (Claim c : Store.claims()) if (c.getStatus() == ClaimStatus.FLAGGED) n++;
        return n == 0 ? null : n + " claim(s) were flagged and need your correction.\nOpen them from the dashboard (double-click) to edit and resubmit.";
    }

    @Override boolean canSubmitClaims() { return true; }
    @Override boolean canManageBudget() { return true; }
    @Override boolean canFileReports()  { return true; }
}
