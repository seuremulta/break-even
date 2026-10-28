package breakeven;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static breakeven.Theme.TS;
import static breakeven.Theme.peso;

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
        if (old != null) {
            old.close();
            saveSemesterToDB(old);
        }
        int n = semesters.size() + 1;
        Semester s = new Semester("SEM-" + n, label, new Budget("BUD-" + n, total));
        semesters.add(s);
        viewing = s;
        saveSemesterToDB(s);
        saveBudgetToDB(s.getBudget(), s.getId());
        return s;
    }

    // ---------------------------------------------------------------- mutation (controlled)
    static void addClaim(Claim c)       { viewing.addClaim(c); saveClaimToDB(c); }
    static void removeClaim(Claim c)    { viewing.removeClaim(c); deleteClaimFromDB(c.getRef()); }
    static void addEvent(Event e)       { viewing.requireOpen(); budget().addEvent(e); saveEventToDB(e); }
    static void removeEvent(Event e)    { viewing.requireOpen(); budget().removeEvent(e); deleteEventFromDB(e.getName()); }
    static void addReport(Report r)     { viewing.addReport(r); saveReportToDB(r); }
    static void removeReport(Report r)  { viewing.removeReport(r); deleteReportFromDB(r.getId()); }

    static void saveEvent(Event e)      { saveEventToDB(e); }

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
        saveLogToDB(entry);
    }

    // ---------------------------------------------------------------- review workflow
    static void approve(Claim c) {
        c.approve();
        saveClaimToDB(c);
        log("Approved claim " + c.getRef() + " (" + peso(c.getAmount()) + ")");
    }

    static void flag(Claim c, String note) {
        c.flag(note);
        saveClaimToDB(c);
        log("Flagged " + c.getRef() + ": " + note);
    }

    static String clearClaim(Claim c) {
        if (!editable()) return "This semester is closed, so claims can no longer be cleared.";
        Event k = event(c.getEvent());
        if (k == null) return "Cannot clear: \"" + c.getEvent() + "\" has no budget allocation.";
        if (c.getAmount() > remaining(k))
            return "Cannot clear: amount exceeds the remaining allocation for " + k.getName() + " (" + peso(remaining(k)) + ").";
        c.clear();
        saveClaimToDB(c);
        log("Granted clearance for " + c.getRef() + " (" + peso(c.getAmount()) + "), budget balance updated");
        return null;
    }

    // ---------------------------------------------------------------- DATABASE PERSISTENCE HELPERS
    private static void saveSemesterToDB(Semester s) {
        if (s == null) return;
        String sql = "INSERT INTO semesters (id, label, is_closed) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE label=?, is_closed=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            ps.setString(1, s.getId());
            ps.setString(2, s.getLabel());
            ps.setBoolean(3, s.isClosed());
            ps.setString(4, s.getLabel());
            ps.setBoolean(5, s.isClosed());
            ps.executeUpdate();
            System.out.println(">>> MYSQL SUCCESS [Semester Saved]: " + s.getId());
        } catch (SQLException e) { System.err.println(">>> MYSQL ERROR [Semester]: " + e.getMessage()); }
    }

    private static void saveBudgetToDB(Budget b, String semesterId) {
        if (b == null) return;
        String sql = "INSERT INTO budgets (id, semester_id, total_amount, contingency) VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE semester_id=?, total_amount=?, contingency=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            ps.setString(1, b.getBudgetId());
            ps.setString(2, semesterId);
            ps.setDouble(3, b.getTotalAmount());
            ps.setDouble(4, b.getContingency());
            ps.setString(5, semesterId);
            ps.setDouble(6, b.getTotalAmount());
            ps.setDouble(7, b.getContingency());
            ps.executeUpdate();
            System.out.println(">>> MYSQL SUCCESS [Budget Saved]: " + b.getBudgetId());
        } catch (SQLException e) { System.err.println(">>> MYSQL ERROR [Budget]: " + e.getMessage()); }
    }

    static void saveClaimToDB(Claim c) {
        if (c == null) return;
        String sql = "INSERT INTO claims (ref, description, claimant, amount, category, date, receipt_no, event_name, status, discrepancy, notes, receipt_file) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE description=?, claimant=?, amount=?, category=?, date=?, receipt_no=?, event_name=?, status=?, discrepancy=?, notes=?, receipt_file=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            ps.setString(1, c.getRef());
            ps.setString(2, c.getDesc());
            ps.setString(3, c.getClaimant());
            ps.setDouble(4, c.getAmount());
            ps.setString(5, c.getCategory());
            ps.setString(6, c.getDate());
            ps.setString(7, c.getReceiptNo());
            ps.setString(8, c.getEvent());
            ps.setString(9, c.getStatus().name());
            ps.setString(10, c.getDiscrepancy());
            ps.setString(11, c.getNotes());
            ps.setString(12, c.getReceiptFile());

            ps.setString(13, c.getDesc());
            ps.setString(14, c.getClaimant());
            ps.setDouble(15, c.getAmount());
            ps.setString(16, c.getCategory());
            ps.setString(17, c.getDate());
            ps.setString(18, c.getReceiptNo());
            ps.setString(19, c.getEvent());
            ps.setString(20, c.getStatus().name());
            ps.setString(21, c.getDiscrepancy());
            ps.setString(22, c.getNotes());
            ps.setString(23, c.getReceiptFile());
            ps.executeUpdate();
            System.out.println(">>> MYSQL SUCCESS [Claim Saved]: " + c.getRef());
        } catch (SQLException e) { System.err.println(">>> MYSQL ERROR [Claim Save Failed]: " + e.getMessage()); }
    }

    private static void deleteClaimFromDB(String ref) {
        String sql = "DELETE FROM claims WHERE ref = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            ps.setString(1, ref);
            ps.executeUpdate();
            System.out.println(">>> MYSQL SUCCESS [Claim Deleted]: " + ref);
        } catch (SQLException e) { System.err.println(">>> MYSQL ERROR [Delete Claim]: " + e.getMessage()); }
    }

    static void saveEventToDB(Event e) {
        if (e == null) return;
        String sql = "INSERT INTO events (name, allocated) VALUES (?, ?) ON DUPLICATE KEY UPDATE allocated=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            ps.setString(1, e.getName());
            ps.setDouble(2, e.getAllocated());
            ps.setDouble(3, e.getAllocated());
            ps.executeUpdate();
            System.out.println(">>> MYSQL SUCCESS [Event Saved]: " + e.getName());
        } catch (SQLException ex) { System.err.println(">>> MYSQL ERROR [Event Save Failed]: " + ex.getMessage()); }
    }

    private static void deleteEventFromDB(String name) {
        String sql = "DELETE FROM events WHERE name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            ps.setString(1, name);
            ps.executeUpdate();
            System.out.println(">>> MYSQL SUCCESS [Event Deleted]: " + name);
        } catch (SQLException ex) { System.err.println(">>> MYSQL ERROR [Delete Event]: " + ex.getMessage()); }
    }

    private static void saveReportToDB(Report r) {
        if (r == null) return;
        String sql = "INSERT INTO reports (id, event_name, count, total, projected, filed_by, date, reviewed_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE event_name=?, count=?, total=?, projected=?, filed_by=?, date=?, reviewed_by=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            ps.setString(1, r.getId());
            ps.setString(2, r.getEvent());
            ps.setInt(3, r.getCount());
            ps.setDouble(4, r.getTotal());
            ps.setDouble(5, r.getProjected());
            ps.setString(6, r.getFiledBy());
            ps.setString(7, r.getDate());
            ps.setString(8, r.getReviewedBy());

            ps.setString(9, r.getEvent());
            ps.setInt(10, r.getCount());
            ps.setDouble(11, r.getTotal());
            ps.setDouble(12, r.getProjected());
            ps.setString(13, r.getFiledBy());
            ps.setString(14, r.getDate());
            ps.setString(15, r.getReviewedBy());
            ps.executeUpdate();
            System.out.println(">>> MYSQL SUCCESS [Report Saved]: " + r.getId());
        } catch (SQLException ex) { System.err.println(">>> MYSQL ERROR [Report]: " + ex.getMessage()); }
    }

    private static void deleteReportFromDB(String id) {
        String sql = "DELETE FROM reports WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            ps.setString(1, id);
            ps.executeUpdate();
            System.out.println(">>> MYSQL SUCCESS [Report Deleted]: " + id);
        } catch (SQLException ex) { System.err.println(">>> MYSQL ERROR [Delete Report]: " + ex.getMessage()); }
    }

    private static void saveLogToDB(LogEntry entry) {
        if (entry == null) return;
        String sql = "INSERT INTO logs (id, timestamp, user, action) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            ps.setString(1, entry.getId());
            ps.setString(2, entry.getTime());
            ps.setString(3, entry.getUser());
            ps.setString(4, entry.getAction());
            ps.executeUpdate();
            System.out.println(">>> MYSQL SUCCESS [Log Saved]: " + entry.getId());
        } catch (SQLException ex) { System.err.println(">>> MYSQL ERROR [Log]: " + ex.getMessage()); }
    }

    // ---------------------------------------------------------------- demo seed data
    private static Claim mk(String ref, String desc, String by, double amt, String type, String date, String or,
                            String ev, ClaimStatus st, String disc) {
        return new Claim(ref, desc, by, amt, type, date, or, ev, st, disc);
    }

    static void seed() {
        String VE = "Venue & Equipment", FR = "Food & Refreshments", PM = "Printing & Materials",
                TR = "Transportation", HF = "Honoraria & Fees";

        Semester old = startSemester("A.Y. 2025-2026 - 2nd Sem", 420000);
        Budget ob = old.getBudget();
        ob.setContingency(40000);
        saveBudgetToDB(ob, old.getId());

        Event ev1 = new Event("Intramurals", 60000);
        Event ev2 = new Event("General Assembly", 25000);
        ob.addEvent(ev1); saveEventToDB(ev1);
        ob.addEvent(ev2); saveEventToDB(ev2);

        Claim c1 = mk("EXP-0071", "Referee Fees - Intramurals", "C. Refalbor", 12000, HF, "2025-03-10", "OR-2025-0201", "Intramurals", ClaimStatus.CLEARED, "");
        Claim c2 = mk("EXP-0072", "Water & Snacks - Intramurals", "S. Remulta", 8400, FR, "2025-03-11", "OR-2025-0205", "Intramurals", ClaimStatus.CLEARED, "");
        Claim c3 = mk("EXP-0073", "Meeting Snacks - General Assembly", "V. Po", 3200, FR, "2025-03-14", "OR-2025-0214", "General Assembly", ClaimStatus.CLEARED, "");
        old.addClaim(c1); saveClaimToDB(c1);
        old.addClaim(c2); saveClaimToDB(c2);
        old.addClaim(c3); saveClaimToDB(c3);

        Report lr = new Report("LR-001", "Intramurals", 2, 20400, 60000, "F. Fernandes", "2025-03-20");
        lr.review("Prof. D. Santos");
        old.addReport(lr); saveReportToDB(lr);

        log("Granted clearance for EXP-0071");
        log("Granted clearance for EXP-0072");
        log("Granted clearance for EXP-0073");

        Semester curSem = startSemester("A.Y. 2026-2027 - 1st Sem", 485000);
        Budget b = budget();
        b.setContingency(60000);
        saveBudgetToDB(b, curSem.getId());

        Event e1 = new Event("Acquaintance Party", 80000);
        Event e2 = new Event("Foundation Week", 30000);
        Event e3 = new Event("Leadership Training", 40000);
        b.addEvent(e1); saveEventToDB(e1);
        b.addEvent(e2); saveEventToDB(e2);
        b.addEvent(e3); saveEventToDB(e3);

        addClaim(mk("EXP-0091", "Stage Rental - Acquaintance Party", "C. Refalbor", 28500, VE, "2025-09-02", "OR-2025-0455", "Acquaintance Party", ClaimStatus.CLEARED, ""));
        addClaim(mk("EXP-0090", "Catering - ACQ", "S. Remulta", 14200, FR, "2025-08-28", "OR-2025-0441", "Acquaintance Party", ClaimStatus.PENDING, ""));
        addClaim(mk("EXP-0089", "Printing - ACQ Pins (x12)", "V. Po", 6800, PM, "2025-08-25", "OR-2025-0430", "Acquaintance Party", ClaimStatus.CLEARED, ""));
        addClaim(mk("EXP-0088", "Sound System Rental - ACQ", "N. Mahipus", 18000, VE, "2025-08-20", "OR-2025-0412", "Acquaintance Party", ClaimStatus.FLAGGED, "Auditor note: receipt total differs from quotation"));

        String[] d = {"Transportation", "Snacks for Volunteers", "Certificates", "Venue Decorations", "Registration Supplies", "Banner Printing"};
        String[] t = {TR, FR, PM, VE, PM, PM};
        double[] a = {3200, 3800, 3500, 4000, 4500, 5000};
        for (int i = 0; i < 6; i++)
            addClaim(mk(String.format("EXP-%04d", 82 + i), d[i] + " - Foundation Week", "C. Refalbor", a[i],
                    t[i], "2025-08-1" + (i + 1), "OR-2025-03" + (10 + i), "Foundation Week", ClaimStatus.PENDING, ""));

        log("Flagged EXP-0088: receipt total differs from quotation");
        log("Approved claim EXP-0089");
        log("Granted clearance for EXP-0091");
    }
}