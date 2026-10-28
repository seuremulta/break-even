package breakeven;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every SQL statement of the app lives here, so Store only deals with state and business rules.
 *
 * OOP [ABSTRACTION]: Store says "save this claim" / "load everything"; how that becomes SQL is hidden here.
 * OOP [ENCAPSULATION]: the on/off switch and the work helpers are private.
 *
 * Rules of thumb:
 *   - If MySQL cannot be reached when the app starts, the database is switched off for the whole session
 *     and the app runs in memory only (problem() explains why, so the UI can tell the user).
 *   - A save that fails later is printed to the console and never crashes the window.
 *   - Multi-step changes (e.g. starting a semester) run in one transaction: all of it is saved or none of it.
 */
final class Database {

    private Database() {}

    private static boolean enabled = true;
    private static String problem;

    /** Null when everything is fine, otherwise a user-readable reason the database is not being used. */
    static String problem() { return problem; }

    private static void disable(String why) {
        enabled = false;
        problem = why;
        System.err.println(">>> MYSQL DISABLED: " + why);
    }

    // ---------------------------------------------------------------- small helpers
    private interface Work { void run(Connection c) throws SQLException; }

    /** Runs one piece of SQL work. Returns false (and prints why) instead of throwing. */
    private static boolean exec(String what, Work w) {
        if (!enabled) return false;
        Connection c = DBConnection.getConnection();
        if (c == null) { System.err.println(">>> MYSQL: not connected, skipped [" + what + "]"); return false; }
        try { w.run(c); return true; }
        catch (SQLException e) { System.err.println(">>> MYSQL ERROR [" + what + "]: " + e.getMessage()); return false; }
    }

    /** Same as exec, but all-or-nothing. */
    private static boolean tx(String what, Work w) {
        if (!enabled) return false;
        Connection c = DBConnection.getConnection();
        if (c == null) { System.err.println(">>> MYSQL: not connected, skipped [" + what + "]"); return false; }
        try {
            c.setAutoCommit(false);
            w.run(c);
            c.commit();
            return true;
        } catch (SQLException e) {
            try { c.rollback(); } catch (SQLException ignored) {}
            System.err.println(">>> MYSQL ERROR [" + what + "] (rolled back): " + e.getMessage());
            return false;
        } finally {
            try { c.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    // ---------------------------------------------------------------- startup
    /** Tries to reach MySQL once. If it fails the database stays off for this session. */
    static boolean connect() {
        if (DBConnection.getConnection() != null) return true;
        disable("Could not reach the MySQL database (is the XAMPP MySQL service running?).\n"
                + "BreakEven will run with demo data in memory only, and nothing you do will be saved.");
        return false;
    }

    /**
     * Loads every semester (with its budget, events, claims and reports) and the audit log.
     * Returns false if the data could not be read; the database is then switched off so that demo data
     * can never be written on top of whatever is stored there.
     */
    static boolean loadAll(List<Semester> semesters, List<LogEntry> logs) {
        Connection c = DBConnection.getConnection();
        if (c == null) { disable("Lost the connection to MySQL while loading."); return false; }
        try {
            Map<String, Semester> byId = new LinkedHashMap<>();
            Map<String, Boolean> closed = new LinkedHashMap<>();

            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT s.id, s.label, s.is_closed, b.id AS bid, b.total_amount, b.contingency "
                         + "FROM semesters s JOIN budgets b ON b.semester_id = s.id")) {
                while (rs.next()) {
                    Budget b = new Budget(rs.getString("bid"), rs.getDouble("total_amount"));
                    b.setContingency(rs.getDouble("contingency"));
                    String id = rs.getString("id");
                    byId.put(id, new Semester(id, rs.getString("label"), b));
                    closed.put(id, rs.getBoolean("is_closed"));
                }
            }

            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT semester_id, name, allocated FROM events ORDER BY seq")) {
                while (rs.next()) {
                    Semester s = byId.get(rs.getString("semester_id"));
                    if (s != null) s.getBudget().addEvent(new Event(rs.getString("name"), rs.getDouble("allocated")));
                }
            }

            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT ref, semester_id, description, claimant, amount, category, `date`, receipt_no, "
                         + "event_name, status, discrepancy, notes, receipt_file FROM claims ORDER BY LENGTH(ref), ref")) {
                while (rs.next()) {
                    Semester s = byId.get(rs.getString("semester_id"));
                    if (s == null) continue;
                    Claim cl = new Claim(rs.getString("ref"), rs.getString("description"), rs.getString("claimant"),
                            rs.getDouble("amount"), rs.getString("category"), rs.getString("date"), rs.getString("receipt_no"),
                            rs.getString("event_name"), ClaimStatus.valueOf(rs.getString("status")), rs.getString("discrepancy"));
                    cl.update(cl.getDesc(), cl.getClaimant(), cl.getAmount(), cl.getReceiptNo(), cl.getDate(), cl.getEvent(),
                            cl.getCategory(), rs.getString("notes"), rs.getString("receipt_file"));   // notes + receipt file
                    s.addClaim(cl);
                }
            }

            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, semester_id, event_name, `count`, total, projected, filed_by, `date`, "
                         + "status, reviewed_by FROM reports ORDER BY LENGTH(id), id")) {
                while (rs.next()) {
                    Semester s = byId.get(rs.getString("semester_id"));
                    if (s == null) continue;
                    Report r = new Report(rs.getString("id"), rs.getString("event_name"), rs.getInt("count"), rs.getDouble("total"),
                            rs.getDouble("projected"), rs.getString("filed_by"), rs.getString("date"));
                    if ("REVIEWED".equals(rs.getString("status"))) r.review(rs.getString("reviewed_by") == null ? "" : rs.getString("reviewed_by"));
                    s.addReport(r);
                }
            }

            // close semesters last: a closed semester refuses new claims and reports
            for (Map.Entry<String, Semester> e : byId.entrySet()) if (closed.get(e.getKey())) e.getValue().close();
            semesters.addAll(byId.values());
            semesters.sort(Comparator.comparingInt(s -> numberOf(s.getId())));

            List<LogEntry> loaded = new ArrayList<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, `timestamp`, `user`, action FROM logs")) {
                while (rs.next()) {
                    String ts = rs.getString("timestamp");
                    if (ts != null && ts.length() > 19) ts = ts.substring(0, 19);   // drop ".0" some drivers add
                    loaded.add(new LogEntry(rs.getString("id"), ts, rs.getString("user"), rs.getString("action")));
                }
            }
            loaded.sort(Comparator.comparingInt((LogEntry l) -> numberOf(l.getId())).reversed());   // newest first
            logs.addAll(loaded);
            return true;
        } catch (SQLException | RuntimeException e) {
            semesters.clear(); logs.clear();
            disable("The saved data could not be read (" + e.getMessage() + ").\n"
                    + "If you updated BreakEven, re-run database/schema.sql. BreakEven will run with demo data in memory only.");
            return false;
        }
    }

    /** "SEM-12" -> 12, "LOG-0007" -> 7; used to sort ids numerically. */
    private static int numberOf(String id) {
        try { return Integer.parseInt(id.substring(id.indexOf('-') + 1)); } catch (RuntimeException e) { return 0; }
    }

    // ---------------------------------------------------------------- saves (insert or update)
    private static void upsertSemester(Connection c, Semester s) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO semesters (id, label, is_closed) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE label = VALUES(label), is_closed = VALUES(is_closed)")) {
            ps.setString(1, s.getId()); ps.setString(2, s.getLabel()); ps.setBoolean(3, s.isClosed());
            ps.executeUpdate();
        }
    }

    private static void upsertBudget(Connection c, String semesterId, Budget b) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO budgets (id, semester_id, total_amount, contingency) VALUES (?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE total_amount = VALUES(total_amount), contingency = VALUES(contingency)")) {
            ps.setString(1, b.getBudgetId()); ps.setString(2, semesterId);
            ps.setDouble(3, b.getTotalAmount()); ps.setDouble(4, b.getContingency());
            ps.executeUpdate();
        }
    }

    private static void upsertEvent(Connection c, String semesterId, Event e) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO events (semester_id, name, allocated) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE allocated = VALUES(allocated)")) {
            ps.setString(1, semesterId); ps.setString(2, e.getName()); ps.setDouble(3, e.getAllocated());
            ps.executeUpdate();
        }
    }

    /** Saves a semester that was just opened. If an older one was closed to make room, both go in one transaction. */
    static void saveNewSemester(Semester closedOne, Semester fresh) {
        tx("start semester " + fresh.getId(), c -> {
            if (closedOne != null) upsertSemester(c, closedOne);
            upsertSemester(c, fresh);
            upsertBudget(c, fresh.getId(), fresh.getBudget());
        });
    }

    static void saveBudget(String semesterId, Budget b) {
        exec("budget " + b.getBudgetId(), c -> upsertBudget(c, semesterId, b));
    }

    static void saveEvent(String semesterId, Event e) {
        exec("event " + e.getName(), c -> upsertEvent(c, semesterId, e));
    }

    /** Drawing from contingency changes the budget AND an event, so both are written together. */
    static void saveBudgetAndEvent(String semesterId, Budget b, Event e) {
        tx("contingency draw into " + e.getName(), c -> { upsertBudget(c, semesterId, b); upsertEvent(c, semesterId, e); });
    }

    static void deleteEvent(String semesterId, String name) {
        exec("delete event " + name, c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM events WHERE semester_id = ? AND name = ?")) {
                ps.setString(1, semesterId); ps.setString(2, name); ps.executeUpdate();
            }
        });
    }

    static void saveClaim(String semesterId, Claim cl) {
        exec("claim " + cl.getRef(), c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO claims (ref, semester_id, description, claimant, amount, category, `date`, receipt_no, event_name, "
                            + "status, discrepancy, notes, receipt_file) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE description = VALUES(description), claimant = VALUES(claimant), "
                            + "amount = VALUES(amount), category = VALUES(category), `date` = VALUES(`date`), "
                            + "receipt_no = VALUES(receipt_no), event_name = VALUES(event_name), status = VALUES(status), "
                            + "discrepancy = VALUES(discrepancy), notes = VALUES(notes), receipt_file = VALUES(receipt_file)")) {
                ps.setString(1, cl.getRef()); ps.setString(2, semesterId); ps.setString(3, cl.getDesc());
                ps.setString(4, cl.getClaimant()); ps.setDouble(5, cl.getAmount()); ps.setString(6, cl.getCategory());
                ps.setString(7, cl.getDate()); ps.setString(8, cl.getReceiptNo()); ps.setString(9, cl.getEvent());
                ps.setString(10, cl.getStatus().name()); ps.setString(11, cl.getDiscrepancy());
                ps.setString(12, cl.getNotes()); ps.setString(13, cl.getReceiptFile());
                ps.executeUpdate();
            }
        });
    }

    static void deleteClaim(String ref) {
        exec("delete claim " + ref, c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM claims WHERE ref = ?")) {
                ps.setString(1, ref); ps.executeUpdate();
            }
        });
    }

    static void saveReport(String semesterId, Report r) {
        exec("report " + r.getId(), c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO reports (id, semester_id, event_name, `count`, total, projected, filed_by, `date`, status, reviewed_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE event_name = VALUES(event_name), `count` = VALUES(`count`), total = VALUES(total), "
                            + "projected = VALUES(projected), filed_by = VALUES(filed_by), `date` = VALUES(`date`), "
                            + "status = VALUES(status), reviewed_by = VALUES(reviewed_by)")) {
                ps.setString(1, r.getId()); ps.setString(2, semesterId); ps.setString(3, r.getEvent());
                ps.setInt(4, r.getCount()); ps.setDouble(5, r.getTotal()); ps.setDouble(6, r.getProjected());
                ps.setString(7, r.getFiledBy()); ps.setString(8, r.getDate());
                ps.setString(9, r.getStatus().name()); ps.setString(10, r.getReviewedBy());
                ps.executeUpdate();
            }
        });
    }

    static void deleteReport(String id) {
        exec("delete report " + id, c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM reports WHERE id = ?")) {
                ps.setString(1, id); ps.executeUpdate();
            }
        });
    }

    static void saveLog(LogEntry l) {
        exec("log " + l.getId(), c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO logs (id, `timestamp`, `user`, action) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, l.getId()); ps.setString(2, l.getTime()); ps.setString(3, l.getUser()); ps.setString(4, l.getAction());
                ps.executeUpdate();
            }
        });
    }
}
