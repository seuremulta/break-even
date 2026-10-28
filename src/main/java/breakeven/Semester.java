package breakeven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One semester of the council's books: its own budget (total, contingency, event allocations),
 * its own claims and its own liquidation reports.
 * OOP [ENCAPSULATION]: the lists are private and exposed read-only. Once a semester is closed it
 * refuses every change, so past semesters stay a trustworthy record.
 */
class Semester {

    private final String id, label;
    private final Budget budget;
    private final List<Claim> claims = new ArrayList<>();
    private final List<Report> reports = new ArrayList<>();
    private boolean closed;

    Semester(String id, String label, Budget budget) { this.id = id; this.label = label; this.budget = budget; }

    String getId()      { return id; }
    String getLabel()   { return label; }
    Budget getBudget()  { return budget; }
    boolean isClosed()  { return closed; }
    void close()        { closed = true; }

    List<Claim> getClaims()   { return Collections.unmodifiableList(claims); }
    List<Report> getReports() { return Collections.unmodifiableList(reports); }

    void addClaim(Claim c)      { requireOpen(); claims.add(c); }
    void removeClaim(Claim c)   { requireOpen(); claims.remove(c); }
    void addReport(Report r)    { requireOpen(); reports.add(r); }
    void removeReport(Report r) { requireOpen(); reports.remove(r); }

    void requireOpen() { if (closed) throw new IllegalStateException(label + " is closed and read-only."); }

    @Override public String toString() { return label; }
}
