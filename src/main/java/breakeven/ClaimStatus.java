package breakeven;

import java.awt.Color;

/**
 * OOP [POLYMORPHISM]: replaces the old "PENDING"/"APPROVED"... strings and the scattered
 * status.equals(...) checks. Every constant carries its own behaviour by overriding the
 * methods below, so callers just ask the status (c.getStatus().canDelete()) instead of
 * branching on its name.
 * OOP [ENCAPSULATION]: the colours and rules of each status live here and nowhere else.
 */
enum ClaimStatus implements Badged {

    PENDING(0xFDF1C2, 0x8A6100) {
        @Override boolean isAwaiting() { return true; }
        @Override boolean canDelete()  { return true; }
    },
    APPROVED(0xDDE9F9, 0x2A5DA8) {
        @Override boolean isAwaiting() { return true; }
        @Override boolean isLiquidationEligible() { return true; }
    },
    FLAGGED(0xFBD9DB, 0xB3261E) {
        @Override boolean canDelete() { return true; }
        @Override boolean canEdit()   { return true; }                  // treasurer may correct & resubmit
        @Override boolean countsTowardActual() { return false; }
    },
    CLEARED(0xD3F5E3, 0x1B7F4B) {
        @Override boolean isLiquidationEligible() { return true; }
    };

    private final Color bg, fg;

    ClaimStatus(int bg, int fg) { this.bg = new Color(bg); this.fg = new Color(fg); }

    // ---- Badged (abstraction) ----
    @Override public String label()      { return name(); }
    @Override public Color background()  { return bg; }
    @Override public Color foreground()  { return fg; }

    // ---- default behaviour; constants above override only what differs ----
    boolean isAwaiting()            { return false; }   // still waiting on audit / clearance
    boolean canDelete()             { return false; }   // only PENDING or FLAGGED may be removed
    boolean canEdit()               { return false; }   // only FLAGGED may be edited & resubmitted
    boolean countsTowardActual()    { return true; }    // FLAGGED claims do not count toward event actuals
    boolean isLiquidationEligible() { return false; }   // APPROVED / CLEARED go into liquidation reports
}
