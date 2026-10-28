package breakeven;

/**
 * OOP [INHERITANCE]: common parent of Claim, Report and LogEntry. Each of them is an
 * identifiable record with an id that can be exported, so that shared part is written once here.
 * OOP [ABSTRACTION]: abstract class - it cannot be created on its own and it forces every
 * subclass to implement toCsvRow() (inherited from Exportable).
 * OOP [ENCAPSULATION]: the id is private and final; it can be read but never changed.
 */
abstract class Entity implements Exportable {

    private final String id;

    protected Entity(String id) { this.id = id; }

    String getId() { return id; }

    /** Quotes a value for CSV. Shared by all subclasses. */
    protected static String q(String s) {
        return "\"" + (s == null ? "" : s).replace("\"", "\"\"") + "\"";
    }
}
