package breakeven;

/**
 * OOP [ABSTRACTION]: a pure interface. The export code only needs to know "this thing can
 * turn itself into one CSV row"; it does not care whether it is a claim, a log entry or a report.
 * OOP [POLYMORPHISM]: Ui.exportCsv() calls toCsvRow() on a List of Exportable and each class answers in its own way.
 */
interface Exportable {
    String toCsvRow();
}
