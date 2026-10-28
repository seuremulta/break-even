package breakeven;

import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static breakeven.Store.log;
import static breakeven.Ui.warn;

/**
 * Keeps a safe copy of every attached receipt so it cannot be lost if the original is moved or deleted
 * (the proposal's first failure point), and opens receipts for viewing.
 * OOP [ABSTRACTION]: callers say archive()/open(); the file handling is hidden here.
 * OOP [ENCAPSULATION]: the vault folder is private.
 */
final class ReceiptVault {

    private static final File DIR = new File("receipts");

    private ReceiptVault() {}

    /** Copies the file into ./receipts as "<ref>_<name>" and returns the stored path (or the original on failure). */
    static String archive(String ref, String src) {
        if (src == null || src.isEmpty()) return "";
        try {
            File s = new File(src);
            File parent = s.getAbsoluteFile().getParentFile();
            if (parent != null && parent.equals(DIR.getAbsoluteFile())) return src;   // already archived
            DIR.mkdirs();
            File dest = new File(DIR, ref + "_" + s.getName());
            Files.copy(s.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return dest.getPath();
        } catch (Exception e) {
            return src;
        }
    }

    /** Opens the claim's receipt in the system viewer and records the view in the audit log. */
    static void open(Component parent, Claim c) {
        if (!c.hasReceipt()) { warn(parent, "No receipt file is attached to " + c.getRef() + "."); return; }
        File f = new File(c.getReceiptFile());
        if (!f.exists()) { warn(parent, "Receipt file not found:\n" + c.getReceiptFile()); return; }
        try {
            if (!Desktop.isDesktopSupported()) throw new UnsupportedOperationException("Opening files is not supported here.");
            Desktop.getDesktop().open(f);
            log("Viewed receipt for " + c.getRef());
        } catch (Exception ex) {
            warn(parent, "Could not open the receipt: " + ex.getMessage());
        }
    }
}
