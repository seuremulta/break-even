package breakeven;

/**
 * One audit-log line (class diagram: AuditLog, with logID).
 * OOP [INHERITANCE]: extends Entity. OOP [ENCAPSULATION]: immutable - a log line can never be edited.
 */
class LogEntry extends Entity {

    private static int seq = 0;                       // gives every entry a LOG-0001 style id
    private final String time, user, action;

    LogEntry(String time, String user, String action) {
        super(String.format("LOG-%04d", ++seq));
        this.time = time; this.user = user; this.action = action;
    }

    String getTime()   { return time; }
    String getUser()   { return user; }
    String getAction() { return action; }

    @Override
    public String toCsvRow() { return String.join(",", q(getId()), q(time), q(user), q(action)); }
}
