package breakeven;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import static breakeven.Theme.*;

/**
 * Text field with grey placeholder text.
 * OOP [INHERITANCE]: extends JTextField and only overrides how it is painted.
 * OOP [ENCAPSULATION]: the hint is private.
 */
class PH extends JTextField {
    private final String hint;

    PH(String hint) {
        this.hint = hint;
        setFont(f(Font.PLAIN, 13)); setBackground(FIELD);
        setBorder(new CompoundBorder(new LineBorder(BORDER_C), new EmptyBorder(8, 10, 8, 10)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty() && !isFocusOwner()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(0xB5B5B5)); g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(hint, getInsets().left, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }
}
