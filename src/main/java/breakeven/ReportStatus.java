package breakeven;

import java.awt.Color;

/** OOP [POLYMORPHISM]: status of a liquidation report (class diagram: LiquidationReport.status). */
enum ReportStatus implements Badged {

    FILED(0xDDE9F9, 0x2A5DA8) {
        @Override boolean canDelete() { return true; }
    },
    REVIEWED(0xD3F5E3, 0x1B7F4B);     // reviewed by the moderator: locked, keeps the audit trail intact

    private final Color bg, fg;

    ReportStatus(int bg, int fg) { this.bg = new Color(bg); this.fg = new Color(fg); }

    @Override public String label()     { return name(); }
    @Override public Color background() { return bg; }
    @Override public Color foreground() { return fg; }

    boolean canDelete() { return false; }
}
