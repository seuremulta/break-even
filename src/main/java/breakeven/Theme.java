package breakeven;

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

/** Colours, fonts and small formatting helpers shared by the whole UI. */
final class Theme {

    private Theme() {}   // OOP [ABSTRACTION]: pure helper class - nobody should create a Theme object

    static final Color BG = new Color(0xFAF8F4), SIDEBAR = new Color(0xF7F4EE), GREEN = new Color(0x245844),
            BORDER_C = new Color(0xE3DFD6), TEXT = new Color(0x1F2421), MUTED = new Color(0x77797A),
            ORANGE = new Color(0xD9780F), FIELD = new Color(0xF7F5F0), SEL = new Color(0xE8F0EB);

    static final String PESO = "\u20B1";

    static final DecimalFormat FMT = new DecimalFormat("#,##0.00");

    static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static String peso(double v) { return PESO + FMT.format(v); }

    static String php(double v) { return "PHP " + FMT.format(v); }

    static Font f(int style, int size) { return new Font("SansSerif", style, size); }

    static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }

    static Double num(String s) {
        try { double v = Double.parseDouble(s.replace(",", "").replace(PESO, "").trim()); return v >= 0 ? v : null; }
        catch (Exception e) { return null; }
    }
}
