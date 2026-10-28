package breakeven;

/**
 * A filed liquidation report (class diagram: LiquidationReport, with status).
 * OOP [INHERITANCE]: extends Entity. OOP [ENCAPSULATION]: status changes only through review().
 */
class Report extends Entity {

    private final String event, filedBy, date;
    private final int count;
    private final double total, projected;
    private ReportStatus status = ReportStatus.FILED;
    private String reviewedBy = "";

    Report(String id, String event, int count, double total, double projected, String filedBy, String date) {
        super(id);
        this.event = event; this.count = count; this.total = total;
        this.projected = projected; this.filedBy = filedBy; this.date = date;
    }

    String getEvent()        { return event; }
    String getFiledBy()      { return filedBy; }
    String getDate()         { return date; }
    int getCount()           { return count; }
    double getTotal()        { return total; }
    double getProjected()    { return projected; }
    ReportStatus getStatus() { return status; }
    String getReviewedBy()   { return reviewedBy; }

    /** Moderator sign-off. Returns false if it was already reviewed. */
    boolean review(String by) {
        if (status == ReportStatus.REVIEWED) return false;
        status = ReportStatus.REVIEWED; reviewedBy = by;
        return true;
    }

    @Override
    public String toCsvRow() {
        return String.join(",", q(getId()), q(event), String.valueOf(count), String.valueOf(total),
                String.valueOf(projected), q(filedBy), q(date), q(status.label()), q(reviewedBy));
    }
}
