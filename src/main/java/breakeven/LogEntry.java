package breakeven;

/**
 * One audit-log line (class diagram: AuditLog, with logID).
 * OOP [INHERITANCE]: extends Entity. OOP [ENCAPSULATION]: immutable - a log line can never be edited.
 */
class LogEntry extends Entity {

    private static int seq = 0;                       // gives every new entry a LOG-0001 style id
    private final String time, user, action;

    /** A brand-new entry: the id continues after the highest one seen so far. */
    LogEntry(String time, String user, String action) { this(nextId(), time, user, action); }

    /** An entry read back from the database. Moves the counter forward so new ids never repeat old ones. */
    LogEntry(String id, String time, String user, String action) {
        super(id);
        syncSeq(id);
        this.time = time; this.user = user; this.action = action;
    }

    private static String nextId() { return String.format("LOG-%04d", ++seq); }

    private static void syncSeq(String id) {
        try { seq = Math.max(seq, Integer.parseInt(id.substring(id.indexOf('-') + 1))); }
        catch (RuntimeException ignored) { /* unusual id: leave the counter alone */ }
    }

    String getTime()   { return time; }
    String getUser()   { return user; }
    String getAction() { return action; }

    @Override
    public String toCsvRow() { return String.join(",", q(getId()), q(time), q(user), q(action)); }
}
