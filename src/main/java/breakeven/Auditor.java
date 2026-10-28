package breakeven;

import java.util.Arrays;
import java.util.List;

/** OOP [INHERITANCE]: an Auditor IS-A User. Reviews claims, approves or flags them, checks event history. */
class Auditor extends User {

    Auditor(String username, String password, String name) { super(username, password, name); }

    @Override String getRole() { return "Auditor"; }

    @Override List<String> navItems() {
        return Arrays.asList("Dashboard", "Budget Allocation", "Review Queue", "Event History", "Liquidation", "Audit Log");
    }

    @Override String loginNotice() {
        int n = 0;
        for (Claim c : Store.claims()) if (c.getStatus() == ClaimStatus.PENDING) n++;
        return n == 0 ? null : n + " pending claim(s) are waiting in your review queue.";
    }

    @Override boolean canReviewClaims() { return true; }

    @Override ClaimStatus reviewQueueStatus() { return ClaimStatus.PENDING; }
    @Override String reviewTitle()    { return "Audit Review Queue"; }
    @Override String reviewSubtitle() { return "Pending claims - cross-check against allocations, then approve or flag."; }
}
