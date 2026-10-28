package breakeven;

import java.awt.Color;

/**
 * OOP [ABSTRACTION]: anything that can be drawn as a coloured status "pill" in a table.
 * ClaimStatus and ReportStatus both implement it, so StatusRenderer works with either
 * without knowing which one it was given (OOP [POLYMORPHISM]).
 */
interface Badged {
    String label();
    Color background();
    Color foreground();

    /** Finds the badge whose label matches the text stored in a table cell, or null. */
    static Badged lookup(String label) {
        for (Badged b : ClaimStatus.values()) if (b.label().equals(label)) return b;
        for (Badged b : ReportStatus.values()) if (b.label().equals(label)) return b;
        return null;
    }
}
