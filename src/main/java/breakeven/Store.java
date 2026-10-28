package breakeven;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static breakeven.Theme.TS;
import static breakeven.Theme.peso;

/**
 * The app's state (users, semesters, audit log), its budget maths and the review workflow.
 * OOP [ENCAPSULATION]: all state is private; the rest of the app reads it through the methods below.
 * OOP [ABSTRACTION]: every database operation is delegated to Database, so Store contains no SQL.
 */
final class Store {

    private Store() {}

    // ---------------------------------------------------------------- state (all private)
    private static final List<User> users = Arrays.asList(
            new Treasurer("treasurer", "treasurer123", "F. Fernandes"),
            new Auditor("auditor", "auditor123", "A. Reyes"),
            new Moderator("moderator", "moderator123", "Prof. D. Santos"));

    private static final List<String> EXPENSE_TYPES = Arrays.asList(
            "Venue & Equipment", "Food & Refreshments", "Printing & Materials", "Transportation", "Honoraria & Fees", "Miscellaneous");

    private static final List<Semester> semesters = new ArrayList<>();
    private static final List<LogEntry> logs = new ArrayList<>();

    private static Semester viewing;
    private static User current;
    private static App app;

    // ---------------------------------------------------------------- read-only access
    static List<User> users()          { return Collections.unmodifiableList(users); }
    static List<Semester> semesters()  { return Collections.unmodifiableList(semesters); }
    static List<String> expenseTypes() { return EXPENSE_TYPES; }
    static Semester semester()         { return viewing; }
    static Budget budget()             { return viewing == null ? null : viewing.getBudget(); }
    static List<Event> events()        { return budget() == null ? Collections.emptyList() : budget().getEvents(); }
    static List<Claim> claims()        { return viewing == null ? Collections.emptyList() : viewing.getClaims(); }
    static List<Report> reports()      { return viewing == null ? Collections.emptyList() : viewing.getReports(); }
    static List<LogEntry> logs()       { return Collections.unmodifiableList(logs); }

    static User current()          { return current; }
    static void setCurrent(User u) { current = u; }
    static App app()               { return app; }
    static void setApp(App a)      { app = a; }

    // ---------------------------------------------------------------- semesters
    static Semester currentSemester() {
        Semester s = latestOpen();
        return s != null ? s : (semesters.isEmpty() ? null : semesters.get(semesters.size() - 1));
    }

    private static Semester latestOpen() {
        for (int i = semesters.size() - 1; i >= 0; i--) if (!semesters.get(i).isClosed()) return semesters.get(i);
        return null;
    }

    static void viewSemester(Semester s) { viewing = s; }

    static boolean editable() { return viewing != null && !viewing.isClosed(); }

    static Semester startSemester(String label, double total) {
        Semester old = latestOpen();
        if (old != null) old.close();
        int n = semesters.size() + 1;
        Semester s = new Semester("SEM-" + n, label, new Budget("BUD-" + n, total));
        semesters.add(s);
        viewing = s;
        Database.saveNewSemester(old, s);                     // closing the old one + opening the new one: one transaction
        return s;
    }

    // ---------------------------------------------------------------- mutation (controlled)
    static void addClaim(Claim c)       { viewing.addClaim(c); saveClaim(c); }
    static void removeClaim(Claim c)    { viewing.removeClaim(c); Database.deleteClaim(c.getRef()); }
    static void addEvent(Event e)       { viewing.requireOpen(); budget().addEvent(e); saveEvent(e); }
    static void removeEvent(Event e)    { viewing.requireOpen(); budget().removeEvent(e); Database.deleteEvent(viewing.getId(), e.getName()); }
    static void addReport(Report r)     { viewing.addReport(r); saveReport(r); }
    static void removeReport(Report r)  { viewing.removeReport(r); Database.deleteReport(r.getId()); }

    // ---- "I changed an existing object, store it" (pages call these after editing a claim / event / budget / report)
    static void saveClaim(Claim c)      { Database.saveClaim(viewing.getId(), c); }
    static void saveEvent(Event e)      { Database.saveEvent(viewing.getId(), e); }
    static void saveReport(Report r)    { Database.saveReport(viewing.getId(), r); }
    static void saveBudget()            { Database.saveBudget(viewing.getId(), budget()); }
    static void saveBudgetAndEvent(Event e) { Database.saveBudgetAndEvent(viewing.getId(), budget(), e); }

    // ---------------------------------------------------------------- lookups
    static User authenticate(String username, String password) {
        for (User u : users) if (u.authenticate(username, password)) return u;
        return null;
    }

    static Event event(String n) { return budget() == null ? null : budget().find(n); }

    static List<Claim> claimsFor(String ev) {
        List<Claim> l = new ArrayList<>();
        for (Claim c : claims()) if (c.getEvent().equalsIgnoreCase(ev)) l.add(c);
        l.sort(Comparator.comparing(Claim::getRef));
        return l;
    }

    /**
     * Other claims, in ANY semester, that carry the same receipt / OR number (used to warn about double claims).
     * Case, spaces and dashes are ignored, so "OR-2026-0455" and "or 2026 0455" count as the same receipt.
     * 'except' is left out of the result (pass the claim being edited, or null for a new claim).
     */
    static List<Claim> claimsWithReceipt(String receiptNo, Claim except) {
        String key = receiptKey(receiptNo);
        List<Claim> l = new ArrayList<>();
        if (key.isEmpty()) return l;
        for (Semester sem : semesters)
            for (Claim c : sem.getClaims())
                if (c != except && receiptKey(c.getReceiptNo()).equals(key)) l.add(c);
        l.sort(Comparator.comparing(Claim::getRef));
        return l;
    }

    private static String receiptKey(String s) { return s == null ? "" : s.replaceAll("[^A-Za-z0-9]", "").toLowerCase(); }

    static List<Claim> eligible(String ev) {
        List<Claim> l = new ArrayList<>();
        for (Claim c : claimsFor(ev)) if (c.getStatus().isLiquidationEligible()) l.add(c);
        return l;
    }

    static List<LogEntry> historyFor(Claim c) {
        List<LogEntry> l = new ArrayList<>();
        for (LogEntry e : logs) if (e.getAction().contains(c.getRef())) l.add(e);
        return l;
    }

    // ---------------------------------------------------------------- budget maths
    static double spent(Event e) {
        double s = 0;
        for (Claim c : claims()) if (c.getEvent().equalsIgnoreCase(e.getName()) && c.getStatus() == ClaimStatus.CLEARED) s += c.getAmount();
        return s;
    }

    static double remaining(Event e) { return e.computeRemaining(spent(e)); }

    static double totalSpent() { double s = 0; for (Event e : events()) s += spent(e); return s; }

    static double available() { return budget() == null ? 0 : budget().computeAvailableBalance(Store::spent); }

    static double pendingTotal() { double s = 0; for (Claim c : claims()) if (c.getStatus().isAwaiting()) s += c.getAmount(); return s; }

    static int pendingCount() { int n = 0; for (Claim c : claims()) if (c.getStatus().isAwaiting()) n++; return n; }

    static double actual(Event e) {
        double s = 0;
        for (Claim c : claims()) if (c.getEvent().equalsIgnoreCase(e.getName()) && c.getStatus().countsTowardActual()) s += c.getAmount();
        return s;
    }

    // ---------------------------------------------------------------- ids
    static String nextRef() {
        int max = 0;
        for (Semester s : semesters) for (Claim c : s.getClaims()) max = Math.max(max, Integer.parseInt(c.getRef().substring(4)));
        return String.format("EXP-%04d", max + 1);
    }

    static String nextReportId() {
        int max = 0;
        for (Semester s : semesters) for (Report r : s.getReports()) max = Math.max(max, Integer.parseInt(r.getId().substring(3)));
        return String.format("LR-%03d", max + 1);
    }

    // ---------------------------------------------------------------- audit log
    static void log(String action) {
        String timeStr = LocalDateTime.now().format(TS);
        String userStr = current == null ? "system" : current.signature();
        LogEntry entry = new LogEntry(timeStr, userStr, action);
        logs.add(0, entry);
        Database.saveLog(entry);
    }

    // ---------------------------------------------------------------- review workflow
    static void approve(Claim c) {
        c.approve();
        saveClaim(c);
        log("Approved claim " + c.getRef() + " (" + peso(c.getAmount()) + ")");
    }

    static void flag(Claim c, String note) {
        c.flag(note);
        saveClaim(c);
        log("Flagged " + c.getRef() + ": " + note);
    }

    static String clearClaim(Claim c) {
        if (!editable()) return "This semester is closed, so claims can no longer be cleared.";
        Event k = event(c.getEvent());
        if (k == null) return "Cannot clear: \"" + c.getEvent() + "\" has no budget allocation.";
        if (c.getAmount() > remaining(k))
            return "Cannot clear: amount exceeds the remaining allocation for " + k.getName() + " (" + peso(remaining(k)) + ").";
        c.clear();
        saveClaim(c);
        log("Granted clearance for " + c.getRef() + " (" + peso(c.getAmount()) + "), budget balance updated");
        return null;
    }

    // ---------------------------------------------------------------- startup
    /**
     * Called once by Main. Loads the saved data from MySQL; if the database is empty it is filled with the demo data,
     * and if MySQL cannot be reached the demo data is kept in memory only (Database.problem() says why).
     */
    static void init() {
        if (!Database.connect() || !Database.loadAll(semesters, logs) || semesters.isEmpty()) {
            seed();
            return;
        }
        viewing = currentSemester();
    }

    // ---------------------------------------------------------------- demo seed data
    private static Claim mk(String ref, String desc, String by, double amt, String type, String date, String or,
                            String ev, ClaimStatus st, String disc) {
        return new Claim(ref, desc, by, amt, type, date, or, ev, st, disc);
    }

    private static void seed() {
        String VE = "Venue & Equipment", FR = "Food & Refreshments", PM = "Printing & Materials",
                TR = "Transportation", HF = "Honoraria & Fees";

        // ---- last semester (closed once the new one starts)
        Semester old = startSemester("A.Y. 2025-2026 - 2nd Sem", 420000);
        Budget ob = old.getBudget();
        ob.setContingency(40000);
        saveBudget();

        addEvent(new Event("Intramurals", 60000));
        addEvent(new Event("General Assembly", 25000));

        addClaim(mk("EXP-0071", "Referee Fees - Intramurals", "C. Refalbor", 12000, HF, "2026-03-10", "OR-2026-0201", "Intramurals", ClaimStatus.CLEARED, ""));
        addClaim(mk("EXP-0072", "Water & Snacks - Intramurals", "S. Remulta", 8400, FR, "2026-03-11", "OR-2026-0205", "Intramurals", ClaimStatus.CLEARED, ""));
        addClaim(mk("EXP-0073", "Meeting Snacks - General Assembly", "V. Po", 3200, FR, "2026-03-14", "OR-2026-0214", "General Assembly", ClaimStatus.CLEARED, ""));

        Report lr = new Report("LR-001", "Intramurals", 2, 20400, 60000, "F. Fernandes", "2026-03-20");
        lr.review("Prof. D. Santos");
        addReport(lr);

        log("Granted clearance for EXP-0071");
        log("Granted clearance for EXP-0072");
        log("Granted clearance for EXP-0073");

        // ---- this semester (open)
        startSemester("A.Y. 2026-2027 - 1st Sem", 485000);
        Budget b = budget();
        b.setContingency(60000);
        saveBudget();

        addEvent(new Event("Acquaintance Party", 80000));
        addEvent(new Event("Foundation Week", 30000));
        addEvent(new Event("Leadership Training", 40000));

        addClaim(mk("EXP-0091", "Stage Rental - Acquaintance Party", "C. Refalbor", 28500, VE, "2026-09-02", "OR-2026-0455", "Acquaintance Party", ClaimStatus.CLEARED, ""));
        addClaim(mk("EXP-0090", "Catering - ACQ", "S. Remulta", 14200, FR, "2026-08-28", "OR-2026-0441", "Acquaintance Party", ClaimStatus.PENDING, ""));
        addClaim(mk("EXP-0089", "Printing - ACQ Pins (x12)", "V. Po", 6800, PM, "2026-08-25", "OR-2026-0430", "Acquaintance Party", ClaimStatus.CLEARED, ""));
        addClaim(mk("EXP-0088", "Sound System Rental - ACQ", "N. Mahipus", 18000, VE, "2026-08-20", "OR-2026-0412", "Acquaintance Party", ClaimStatus.FLAGGED, "Auditor note: receipt total differs from quotation"));

        String[] d = {"Transportation", "Snacks for Volunteers", "Certificates", "Venue Decorations", "Registration Supplies", "Banner Printing"};
        String[] t = {TR, FR, PM, VE, PM, PM};
        double[] a = {3200, 3800, 3500, 4000, 4500, 5000};
        for (int i = 0; i < 6; i++)
            addClaim(mk(String.format("EXP-%04d", 82 + i), d[i] + " - Foundation Week", "C. Refalbor", a[i],
                    t[i], "2026-08-1" + (i + 1), "OR-2026-03" + (10 + i), "Foundation Week",
                    i == 0 ? ClaimStatus.APPROVED : ClaimStatus.PENDING, ""));   // EXP-0082 waits in the Moderator's Clearance queue

        // a filed (not yet reviewed) report, so the Moderator has something to review too
        addReport(new Report("LR-002", "Acquaintance Party", 2, 35300, 80000, "F. Fernandes", "2026-09-05"));

        log("Flagged EXP-0088: receipt total differs from quotation");
        log("Approved claim EXP-0089");
        log("Granted clearance for EXP-0089");
        log("Granted clearance for EXP-0091");
        log("Approved claim EXP-0082");
        log("Filed liquidation report LR-002 for Acquaintance Party (2 claims, " + peso(35300) + ")");
    }
}
