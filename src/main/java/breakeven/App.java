package breakeven;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static breakeven.Store.*;
import static breakeven.Theme.*;
import static breakeven.Ui.*;

/**
 * The main window.
 * OOP [INHERITANCE]: extends JFrame.
 * OOP [POLYMORPHISM]: the page map holds every screen as a Page, and refreshAll()/navigate() call
 * refresh() on whichever concrete page it gets. The sidebar is built from User.navItems(), so each role
 * (Treasurer / Auditor / Moderator) supplies its own menu with no role-name checks in this class.
 * OOP [ENCAPSULATION]: widgets and maps are private; other classes only use refreshAll().
 */
class App extends JFrame {
    private final CardLayout rootCards = new CardLayout(), pageCards = new CardLayout();
    private final JPanel root = new JPanel(rootCards), content = new JPanel(pageCards), navBox = new JPanel();
    private final JLabel userName = new JLabel(), userRole = new JLabel();
    private final JButton sideNew = btn("+ New Expense", true);
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, JButton> navBtns = new LinkedHashMap<>();
    private final LoginPanel login;
    private final JComboBox<Semester> semBox = new JComboBox<>();
    private final JLabel semState = new JLabel();
    private boolean syncing;   // true while the semester box is being refilled by code (ignore its events)

    App() {
        super("BreakEven - Finance and Audit Tracking Platform");
        Page review = new ReviewPage();                       // one page, shown as "Review Queue" or "Clearance"
        pages.put("Dashboard", new DashboardPage()); pages.put("Budget Allocation", new BudgetPage());
        pages.put("Review Queue", review); pages.put("Clearance", review);
        pages.put("Event History", new EventHistoryPage());   // new: auditor
        pages.put("Liquidation", new LiquidationPage()); pages.put("Variance Trends", new TrendsPage());   // new: moderator
        pages.put("Audit Log", new AuditPage());
        content.setBackground(BG);
        for (Map.Entry<String, Page> e : pages.entrySet())
            if (!e.getKey().equals("Clearance")) content.add(e.getValue(), key(e.getKey()));
        login = new LoginPanel(this::signIn);
        root.add(login, "login");
        root.add(mainPanel(), "main");
        setContentPane(root);
        setSize(1320, 820); setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private static String key(String label) { return label.equals("Clearance") ? "Review Queue" : label; }

    /** Asks every distinct page to redraw itself (polymorphic refresh()). */
    void refreshAll() {
        syncSemesterBox();
        for (Page p : new LinkedHashSet<>(pages.values())) p.refresh();
    }

    /** Keeps the sidebar's semester picker, its OPEN/CLOSED tag and the "+ New Expense" button in step with the data. */
    private void syncSemesterBox() {
        if (semester() == null) return;
        syncing = true;
        semBox.removeAllItems();
        List<Semester> l = new ArrayList<>(semesters());
        Collections.reverse(l);                                  // newest first
        for (Semester s : l) semBox.addItem(s);
        semBox.setSelectedItem(semester());
        syncing = false;
        boolean closed = semester().isClosed();
        semState.setText(closed ? "CLOSED - read only" : "OPEN - current semester");
        semState.setForeground(closed ? new Color(0xB3261E) : GREEN);
        if (current() != null) sideNew.setVisible(current().canSubmitClaims() && editable());
    }

    private JPanel mainPanel() {
        JPanel main = new JPanel(new BorderLayout()); main.setBackground(BG);
        JPanel side = new JPanel(new BorderLayout()); side.setBackground(SIDEBAR);
        side.setPreferredSize(new Dimension(212, 10));
        side.setBorder(new MatteBorder(0, 0, 0, 1, BORDER_C));

        JPanel brand = new JPanel(new BorderLayout(10, 0)); brand.setOpaque(false); brand.setBorder(new EmptyBorder(14, 12, 14, 12));
        JLabel logo = new JLabel("B", SwingConstants.CENTER); logo.setOpaque(true); logo.setBackground(GREEN); logo.setForeground(Color.WHITE);
        logo.setFont(f(Font.BOLD, 16)); logo.setPreferredSize(new Dimension(30, 30));
        JPanel nm = new JPanel(new GridLayout(2, 1)); nm.setOpaque(false);
        JLabel b1 = new JLabel("Breakeven"); b1.setFont(f(Font.BOLD, 13));
        JLabel b2 = new JLabel("v1.0.0"); b2.setFont(f(Font.PLAIN, 11)); b2.setForeground(MUTED);
        nm.add(b1); nm.add(b2);
        brand.add(logo, BorderLayout.WEST); brand.add(nm, BorderLayout.CENTER);

        JPanel org = new JPanel(new GridLayout(4, 1, 0, 3)); org.setOpaque(false);
        org.setBorder(new CompoundBorder(new MatteBorder(1, 0, 1, 0, BORDER_C), new EmptyBorder(12, 12, 12, 12)));
        JLabel o1 = new JLabel("ORGANIZATION"), o2 = new JLabel("CCS Student Council");
        o1.setFont(f(Font.PLAIN, 10)); o1.setForeground(MUTED); o2.setFont(f(Font.PLAIN, 12));
        semBox.setFont(f(Font.PLAIN, 11)); semBox.setBackground(FIELD); semBox.setToolTipText("Switch semester");
        semState.setFont(f(Font.BOLD, 10));
        semBox.addActionListener(e -> {                          // switching semester re-points every page at that semester's data
            if (syncing) return;
            Semester s = (Semester) semBox.getSelectedItem();
            if (s != null && s != semester()) { viewSemester(s); refreshAll(); }
        });
        org.add(o1); org.add(o2); org.add(semBox); org.add(semState);
        JPanel topSide = new JPanel(new BorderLayout()); topSide.setOpaque(false);
        topSide.add(brand, BorderLayout.NORTH); topSide.add(org, BorderLayout.CENTER);

        navBox.setLayout(new BoxLayout(navBox, BoxLayout.Y_AXIS)); navBox.setOpaque(false);
        navBox.setBorder(new EmptyBorder(8, 0, 8, 0));
        JPanel navWrap = new JPanel(new BorderLayout()); navWrap.setOpaque(false);
        navWrap.add(topSide, BorderLayout.NORTH); navWrap.add(navBox, BorderLayout.CENTER);

        JPanel foot = new JPanel(new BorderLayout(0, 8)); foot.setOpaque(false);
        foot.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, BORDER_C), new EmptyBorder(12, 12, 12, 12)));
        JPanel who = new JPanel(new GridLayout(2, 1)); who.setOpaque(false);
        userName.setFont(f(Font.PLAIN, 12)); userRole.setFont(f(Font.PLAIN, 11)); userRole.setForeground(MUTED);
        who.add(userName); who.add(userRole);
        JLabel active = new JLabel("ACTIVE", SwingConstants.CENTER); active.setOpaque(true);
        active.setBackground(new Color(0xD3F5E3)); active.setForeground(new Color(0x1B7F4B)); active.setFont(f(Font.BOLD, 9));
        active.setBorder(new EmptyBorder(2, 6, 2, 6));
        JPanel wr = new JPanel(new BorderLayout()); wr.setOpaque(false);
        wr.add(who, BorderLayout.CENTER);
        JPanel ap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4)); ap.setOpaque(false); ap.add(active);
        wr.add(ap, BorderLayout.EAST);
        JButton out = btn("Sign out", false);
        out.addActionListener(e -> { log("Signed out"); setCurrent(null); login.reset(); rootCards.show(root, "login"); });
        sideNew.addActionListener(e -> new ExpenseDialog(null).setVisible(true));
        JPanel btns = new JPanel(new GridLayout(2, 1, 0, 6)); btns.setOpaque(false);
        btns.add(sideNew); btns.add(out);
        foot.add(wr, BorderLayout.CENTER); foot.add(btns, BorderLayout.SOUTH);

        side.add(navWrap, BorderLayout.CENTER); side.add(foot, BorderLayout.SOUTH);
        main.add(side, BorderLayout.WEST); main.add(content, BorderLayout.CENTER);
        return main;
    }

    void signIn(User x) {
        setCurrent(x);
        viewSemester(currentSemester());                      // always start on the open semester
        log("Signed in");
        userName.setText(x.getName()); userRole.setText(x.getRole());
        sideNew.setVisible(x.canSubmitClaims());
        navBox.removeAll(); navBtns.clear();
        for (String s : x.navItems()) {                       // OOP [POLYMORPHISM]: each role supplies its own menu
            JButton b = new JButton(s);
            b.setUI(new BasicButtonUI());
            b.setHorizontalAlignment(SwingConstants.LEFT); b.setFont(f(Font.PLAIN, 13));
            b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            b.addActionListener(e -> navigate(s));
            navBtns.put(s, b);
            JPanel w = new JPanel(new BorderLayout()); w.setOpaque(false);
            w.setBorder(new EmptyBorder(2, 4, 2, 4)); w.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            w.add(b); navBox.add(w);
        }
        navBox.revalidate();
        refreshAll();
        navigate("Dashboard");
        rootCards.show(root, "main");
        String note = x.loginNotice();                        // OOP [POLYMORPHISM]: role-specific notification
        if (note != null) SwingUtilities.invokeLater(() -> msg(this, note));
    }

    private void navigate(String label) {
        pageCards.show(content, key(label));
        pages.get(label).refresh();
        for (Map.Entry<String, JButton> e : navBtns.entrySet()) {
            boolean on = e.getKey().equals(label);
            JButton b = e.getValue();
            b.setOpaque(true);
            b.setBackground(on ? SEL : SIDEBAR);
            b.setForeground(on ? GREEN : TEXT);
            b.setBorder(on ? new CompoundBorder(new LineBorder(TEXT, 1, true), new EmptyBorder(8, 10, 8, 10)) : new EmptyBorder(9, 11, 9, 11));
        }
    }
}
