package breakeven;

import java.util.List;

/**
 * Base class of every signed-in person (class diagram: User -> Treasurer / Auditor / Moderator).
 *
 * OOP [ABSTRACTION]: abstract class. getRole(), navItems() and loginNotice() have no body here;
 * each role must define them. The rest of the app talks to "a User" and never to a role string.
 * OOP [ENCAPSULATION]: credentials are private. Passwords are only checked via authenticate().
 * OOP [POLYMORPHISM]: the permission methods return false by default and each role overrides
 * the ones it is allowed to use. This replaces every old current.role.equals("Treasurer") check.
 */
abstract class User {

    private final String username, password, name;

    protected User(String username, String password, String name) {
        this.username = username; this.password = password; this.name = name;
    }

    String getUsername() { return username; }
    String getName()     { return name; }

    boolean authenticate(String u, String p) { return username.equals(u) && password.equals(p); }

    /** DEMO ONLY: lets the login screen's quick-fill chips work. Delete when the database login arrives. */
    String demoPassword() { return password; }

    /** Text shown in the audit log, e.g. "A. Reyes (Auditor)". */
    String signature() { return name + " (" + getRole() + ")"; }

    // ---- each role MUST define these (abstraction) ----
    abstract String getRole();
    abstract List<String> navItems();       // which sidebar pages this role sees
    abstract String loginNotice();          // message right after sign-in, or null

    // ---- permissions: default = no; roles override (polymorphism) ----
    boolean canSubmitClaims()     { return false; }
    boolean canManageBudget()     { return false; }   // allocate budget to events
    boolean canSetBudget()        { return false; }   // semester total, contingency, start a new semester
    boolean canFileReports()      { return false; }
    boolean canReviewClaims()     { return false; }   // approve / flag
    boolean canGrantClearance()   { return false; }
    boolean canReviewReports()    { return false; }
    boolean canExportAuditLog()   { return false; }

    // ---- the Review/Clearance page adapts itself to the role ----
    ClaimStatus reviewQueueStatus() { return null; }
    String reviewTitle()            { return ""; }
    String reviewSubtitle()         { return ""; }
}
