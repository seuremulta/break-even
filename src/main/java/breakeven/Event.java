package breakeven;

/**
 * An event and the slice of the council budget allocated to it. This is now the one unit of budgeting
 * (it replaces both the old expense-category ceilings and the old "projected cost").
 * OOP [ENCAPSULATION]: the allocation rules live with the data they describe, so the Budget page,
 * the Dashboard and the Trends page cannot disagree about what "over-shot" means.
 */
class Event {

    private final String name;
    private double allocated;

    Event(String name, double allocated) { this.name = name; this.allocated = allocated; }

    String getName()      { return name; }
    double getAllocated() { return allocated; }

    void setAllocated(double a) {
        if (a < 0) throw new IllegalArgumentException("Allocation cannot be negative.");
        allocated = a;
    }

    double computeRemaining(double spent) { return allocated - spent; }

    double utilization(double spent) { return allocated == 0 ? 0 : spent / allocated; }

    double variance(double actual) { return actual - allocated; }

    String remark(double actual) {
        double v = variance(actual);
        return allocated == 0 ? "No allocation" : v > 0 ? "Over-shot" : v < 0 ? "Under-shot" : "On target";
    }
}
