package breakeven;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

import static breakeven.Theme.*;

/**
 * The sign-in screen: a single centered card on a soft background.
 * To restyle the login page, this is the only file you need to edit.
 */
class LoginPanel extends JPanel {   // OOP [INHERITANCE]: a JPanel with its own look; helper widgets below are private nested subclasses

    private final Consumer<User> onLogin;
    private final JTextField user = new JTextField();
    private final JPasswordField pass = new JPasswordField();
    private final JLabel error = new JLabel(" ");

    LoginPanel(Consumer<User> onLogin) {
        this.onLogin = onLogin;
        setLayout(new GridBagLayout());
        setBackground(BG);
        add(buildCard());
    }

    /** Called after sign-out so the next person starts with a clean form. */
    void reset() {
        user.setText(""); pass.setText(""); error.setText(" ");
        user.requestFocusInWindow();
    }

    /** Soft decorative circles behind the card. */
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x24, 0x58, 0x44, 22));
        g.fillOval(-140, -140, 420, 420);
        g.fillOval(getWidth() - 260, getHeight() - 240, 440, 440);
        g.setColor(new Color(0x24, 0x58, 0x44, 14));
        g.fillOval(getWidth() - 180, -90, 240, 240);
        g.dispose();
    }

    // ---------------------------------------------------------------- the card
    private JPanel buildCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0, 0, 0, 14));                      // soft shadow
                g.fillRoundRect(3, 6, getWidth() - 6, getHeight() - 6, 24, 24);
                g.setColor(Color.WHITE);
                g.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 6, 24, 24);
                g.setColor(BORDER_C);
                g.drawRoundRect(0, 0, getWidth() - 7, getHeight() - 7, 24, 24);
                g.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(38, 44, 44, 50));
        card.add(Box.createRigidArea(new Dimension(340, 0)));

        // --- brand header
        JComponent badge = new JComponent() {
            { setPreferredSize(new Dimension(52, 52)); setMaximumSize(new Dimension(52, 52)); }
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(GREEN);
                g.fillRoundRect(0, 0, 52, 52, 16, 16);
                g.setColor(Color.WHITE); g.setFont(f(Font.BOLD, 28));
                FontMetrics fm = g.getFontMetrics();
                g.drawString("B", (52 - fm.stringWidth("B")) / 2, (52 + fm.getAscent() - fm.getDescent()) / 2);
                g.dispose();
            }
        };
        JLabel title = new JLabel("BreakEven");
        title.setFont(f(Font.BOLD, 28)); title.setForeground(GREEN);
        JLabel tag = new JLabel("Where every claim checks out.");
        tag.setFont(f(Font.PLAIN, 13)); tag.setForeground(MUTED);
        center(badge); center(title); center(tag);
        card.add(badge); card.add(Box.createVerticalStrut(14));
        card.add(title); card.add(Box.createVerticalStrut(4));
        card.add(tag); card.add(Box.createVerticalStrut(30));

        // --- fields
        JLabel showHide = new JLabel("Show");
        showHide.setFont(f(Font.BOLD, 12)); showHide.setForeground(GREEN);
        showHide.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pass.setEchoChar('\u2022');
        showHide.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean hidden = pass.getEchoChar() != 0;
                pass.setEchoChar(hidden ? (char) 0 : '\u2022');
                showHide.setText(hidden ? "Hide" : "Show");
            }
        });

        card.add(labelRow("USERNAME")); card.add(Box.createVerticalStrut(6));
        card.add(new Field(user, null));
        card.add(Box.createVerticalStrut(16));
        card.add(labelRow("PASSWORD")); card.add(Box.createVerticalStrut(6));
        card.add(new Field(pass, showHide));

        error.setFont(f(Font.PLAIN, 12)); error.setForeground(new Color(0xB3261E));
        JPanel errRow = new JPanel(new BorderLayout());
        errRow.setOpaque(false); errRow.add(error, BorderLayout.WEST);
        errRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        center(errRow);
        card.add(Box.createVerticalStrut(8)); card.add(errRow); card.add(Box.createVerticalStrut(6));

        // --- sign in
        RoundButton signIn = new RoundButton("Sign in", true);
        signIn.setPreferredSize(new Dimension(10, 46));
        signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        center(signIn);
        signIn.addActionListener(e -> attempt());
        user.addActionListener(e -> pass.requestFocusInWindow());
        pass.addActionListener(e -> attempt());
        KeyAdapter clear = new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) { error.setText(" "); }
        };
        user.addKeyListener(clear); pass.addKeyListener(clear);
        card.add(signIn);
        card.add(Box.createVerticalStrut(26));

        // --- demo account shortcuts
        JLabel quick = new JLabel("DEMO ACCOUNTS  -  click to fill in");
        quick.setFont(f(Font.BOLD, 10)); quick.setForeground(MUTED);
        center(quick);
        card.add(quick); card.add(Box.createVerticalStrut(8));
        JPanel chips = new JPanel(new GridLayout(1, Store.users().size(), 8, 0));
        chips.setOpaque(false);
        chips.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        center(chips);
        for (User u : Store.users()) {          // OOP [POLYMORPHISM]: Treasurer, Auditor, Moderator all handled as "User"
            RoundButton chip = new RoundButton(u.getRole(), false);
            chip.addActionListener(e -> {
                user.setText(u.getUsername()); pass.setText(u.demoPassword());
                error.setText(" "); pass.requestFocusInWindow();
            });
            chips.add(chip);
        }
        card.add(chips);
        card.add(Box.createVerticalStrut(26));

        JLabel foot = new JLabel("CCS Student Council  \u00B7  A.Y. 2026-2027");
        foot.setFont(f(Font.PLAIN, 11)); foot.setForeground(MUTED);
        center(foot);
        card.add(foot);
        return card;
    }

    private static JPanel labelRow(String text) {
        JLabel l = new JLabel(text);
        l.setFont(f(Font.BOLD, 11)); l.setForeground(TEXT);
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false); row.add(l, BorderLayout.WEST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        center(row);
        return row;
    }

    private static void center(JComponent c) { c.setAlignmentX(Component.CENTER_ALIGNMENT); }

    private void attempt() {
        String u = user.getText().trim(), p = new String(pass.getPassword());
        User x = Store.authenticate(u, p);   // OOP [ENCAPSULATION]: the login screen never sees the stored passwords
        if (x != null) {
            error.setText(" "); pass.setText("");
            onLogin.accept(x);
            return;
        }
        error.setText("Incorrect username or password.");
        pass.setText(""); pass.requestFocusInWindow();
    }

    // ---------------------------------------------------------------- small widgets
    /** Rounded input box with a green outline while focused. */
    private static class Field extends JPanel {   // OOP [INHERITANCE]: custom-painted rounded input box
        private boolean focused;

        Field(JTextField input, JComponent trailing) {
            setLayout(new BorderLayout(8, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(0, 14, 0, 14));
            input.setBorder(null); input.setOpaque(false);
            input.setFont(f(Font.PLAIN, 14)); input.setForeground(TEXT); input.setCaretColor(GREEN);
            input.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
            add(input, BorderLayout.CENTER);
            if (trailing != null) add(trailing, BorderLayout.EAST);
            setPreferredSize(new Dimension(10, 46));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(FIELD);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g.setColor(focused ? GREEN : BORDER_C);
            g.setStroke(new BasicStroke(focused ? 2f : 1f));
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
            g.dispose();
        }
    }

    /** Rounded button with a hover effect. Primary = solid green, otherwise outlined chip. */
    private static class RoundButton extends JButton {   // OOP [INHERITANCE]: custom-painted button, overrides paintComponent()
        private final boolean primary;
        private boolean hover;

        RoundButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setUI(new BasicButtonUI());
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(f(primary ? Font.BOLD : Font.PLAIN, primary ? 14 : 12));
            setForeground(primary ? Color.WHITE : GREEN);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = primary ? 14 : 12;
            if (primary) {
                g.setColor(hover ? new Color(0x1B4434) : GREEN);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            } else {
                g.setColor(hover ? SEL : Color.WHITE);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                g.setColor(hover ? GREEN : BORDER_C);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }
}