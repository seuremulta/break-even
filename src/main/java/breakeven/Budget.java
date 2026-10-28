package breakeven;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * One semester's budget (class diagram: Budget). A fixed total is split into:
 *   - an allocation per Event,
 *   - a contingency reserve,
 *   - whatever is left, which is "unallocated" (never stored, always computed).
 * OOP [ENCAPSULATION]: the event list is private and read-only; the total and the contingency can only
 * change through validated setters, so the three parts can never add up to more than the total.
 * OOP [ABSTRACTION]: callers ask "can this fit?" without knowing how it is worked out.
 */
class Budget {

    private static final double EPS = 0.005;

    private final String budgetId;
    private double totalAmount, contingency;
    private final List<Event> events = new ArrayList<>();

    Budget(String budgetId, double totalAmount) { this.budgetId = budgetId; this.totalAmount = totalAmount; }

    String getBudgetId()       { return budgetId; }
    double getTotalAmount()    { return totalAmount; }
    double getContingency()    { return contingency; }
    List<Event> getEvents()    { return Collections.unmodifiableList(events); }
    void addEvent(Event e)     { events.add(e); }
    void removeEvent(Event e)  { events.remove(e); }

    Event find(String name) {
        for (Event e : events) if (e.getName().equalsIgnoreCase(name)) return e;
        return null;
    }

    double totalAllocated() { double s = 0; for (Event e : events) s += e.getAllocated(); return s; }
    double committed()      { return totalAllocated() + contingency; }
    double unallocated()    { return totalAmount - committed(); }

    /** Would setting event e (null = a brand-new event) to newValue keep events + contingency within the total? */
    boolean canAllocate(Event e, double newValue) {
        double others = 0;
        for (Event o : events) if (o != e) others += o.getAllocated();
        return others + newValue + contingency <= totalAmount + EPS;
    }

    boolean canSetTotal(double v)       { return v >= committed() - EPS; }
    boolean canSetContingency(double v) { return totalAllocated() + v <= totalAmount + EPS; }

    void setTotalAmount(double v) {
        if (!canSetTotal(v)) throw new IllegalArgumentException("Total cannot be lower than what is already committed.");
        totalAmount = v;
    }

    void setContingency(double v) {
        if (v < 0 || !canSetContingency(v)) throw new IllegalArgumentException("Contingency does not fit in the budget.");
        contingency = v;
    }

    /** Moves money from the contingency reserve into an event's allocation. The total never changes. */
    void drawContingency(Event e, double amount) {
        if (amount <= 0 || amount > contingency + EPS) throw new IllegalArgumentException("Not enough contingency.");
        contingency -= amount;
        e.setAllocated(e.getAllocated() + amount);
    }

    /** Class diagram: Budget.computeAvailableBalance(). The caller says how "spent" is measured per event. */
    double computeAvailableBalance(ToDoubleFunction<Event> spentOf) {
        double s = 0;
        for (Event e : events) s += spentOf.applyAsDouble(e);
        return totalAmount - s;
    }
}
