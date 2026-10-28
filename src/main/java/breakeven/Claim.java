package breakeven;

/**
 * An expense claim (class diagram: ReceiptClaim).
 * OOP [INHERITANCE]: extends Entity (id = reference number).
 * OOP [ENCAPSULATION]: all fields are private. The status can only change through the
 * named transitions approve() / flag() / clear() / resubmit(), so the rules cannot be bypassed.
 */
class Claim extends Entity {

    private String desc, claimant, receiptNo, date, event, category;
    private String notes = "", discrepancy = "", receiptFile = "";
    private double amount;
    private ClaimStatus status;

    Claim(String ref, String desc, String claimant, double amount, String category, String date,
          String receiptNo, String event, ClaimStatus status, String discrepancy) {
        super(ref);
        this.status = status;
        this.discrepancy = discrepancy == null ? "" : discrepancy;
        update(desc, claimant, amount, receiptNo, date, event, category, "", "");
    }

    // ---- getters ----
    String getRef()          { return getId(); }
    String getDesc()         { return desc; }
    String getClaimant()     { return claimant; }
    String getReceiptNo()    { return receiptNo; }
    String getDate()         { return date; }
    String getEvent()        { return event; }
    String getCategory()     { return category; }
    String getNotes()        { return notes; }
    String getDiscrepancy()  { return discrepancy; }
    String getReceiptFile()  { return receiptFile; }
    double getAmount()       { return amount; }
    ClaimStatus getStatus()  { return status; }
    boolean hasReceipt()     { return !receiptFile.isEmpty(); }

    /** OOP [ENCAPSULATION]: one validated entry point for editing the claim's content. */
    void update(String desc, String claimant, double amount, String receiptNo, String date,
                String event, String category, String notes, String receiptFile) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        this.desc = desc; this.claimant = claimant; this.amount = amount; this.receiptNo = receiptNo;
        this.date = date; this.event = event; this.category = category;
        this.notes = notes == null ? "" : notes;
        this.receiptFile = receiptFile == null ? "" : receiptFile;
    }

    // ---- status transitions (the only way status changes) ----
    void approve()                { status = ClaimStatus.APPROVED; discrepancy = ""; }
    void flag(String auditorNote) { status = ClaimStatus.FLAGGED; discrepancy = "Auditor note: " + auditorNote; }
    void clear()                  { status = ClaimStatus.CLEARED; }
    void resubmit(ClaimStatus s, String systemDiscrepancy) { status = s; discrepancy = systemDiscrepancy; }

    /** OOP [POLYMORPHISM]: Claim's own implementation of Exportable. */
    @Override
    public String toCsvRow() {
        return String.join(",", q(getRef()), q(desc), q(claimant), q(category), q(event), q(date),
                q(receiptNo), String.valueOf(amount), q(status.label()), q(receiptFile));
    }
}
