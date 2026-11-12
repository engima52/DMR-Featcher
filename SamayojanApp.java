import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class SamayojanApp extends JFrame {
    private AppContext ctx;
    private final CardLayout screenCardLayout = new CardLayout();
    private final JPanel screenContainer = new JPanel(screenCardLayout);
    private final List<FlutterUI.NavRailButton> navButtons = new ArrayList<>();
    private final JLabel topSyncPill = new JLabel("Local DB: 0 Dates | Ready");
    private final JLabel footerStatusLbl = new JLabel("Ready. Standalone Local Database active.");

    private Single360Screen single360Screen;
    private SyncSettingsScreen syncSettingsScreen;

    public SamayojanApp() {
        setTitle("DCCB Harpalpur (00003) — Modular Multi-Screen Samayojan Studio");
        setSize(1480, 880);
        setMinimumSize(new Dimension(1200, 740));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        ctx = new AppContext(this);
        ctx.setStatusUpdater(footerStatusLbl::setText);

        DashboardScreen dashboardScreen = new DashboardScreen(ctx);
        BulkDmrScreen bulkDmrScreen = new BulkDmrScreen(ctx);
        single360Screen = new Single360Screen(ctx);
        syncSettingsScreen = new SyncSettingsScreen(ctx);

        ctx.setJumpTo360Handler(cif -> {
            single360Screen.renderFarmer(cif);
            switchScreen("SCREEN_360", 2);
        });
        ctx.addRefreshListener(this::updateTopPill);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(FlutterUI.BG_CANVAS);
        root.add(buildAppBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(FlutterUI.BG_CANVAS);
        body.add(buildNavRail(), BorderLayout.WEST);

        screenContainer.setBackground(FlutterUI.BG_CANVAS);
        screenContainer.setBorder(new EmptyBorder(14, 16, 14, 16));
        screenContainer.add(dashboardScreen,    "SCREEN_DASHBOARD");
        screenContainer.add(bulkDmrScreen,      "SCREEN_BULK");
        screenContainer.add(single360Screen,    "SCREEN_360");
        screenContainer.add(syncSettingsScreen, "SCREEN_SYNC");
        body.add(screenContainer, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
        switchScreen("SCREEN_DASHBOARD", 0);
        ctx.loadDatabaseOnStartup();
    }

    private JPanel buildAppBar() {
        JPanel appBar = new JPanel(new BorderLayout(12, 0));
        appBar.setBackground(FlutterUI.SLATE_900);
        appBar.setBorder(new EmptyBorder(12, 18, 12, 18));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);
        JLabel title = new JLabel("DCCB HARPALPUR (BRANCH 00003) — SAMAYOJAN ENTERPRISE STUDIO");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Color.WHITE);
        topSyncPill.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        topSyncPill.setForeground(new Color(148, 163, 184));
        left.add(title);
        left.add(topSyncPill);
        appBar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        FlutterUI.FlutterButton b1 = new FlutterUI.FlutterButton("Open Bulk SB Paste Studio", FlutterUI.INDIGO_600, Color.WHITE);
        b1.addActionListener(e -> switchScreen("SCREEN_BULK", 1));
        FlutterUI.FlutterButton b2 = new FlutterUI.FlutterButton("1-Click Sync New Dates", FlutterUI.BLUE_600, Color.WHITE);
        b2.addActionListener(e -> ctx.runIncrementalSync(false));
        FlutterUI.FlutterButton b3 = new FlutterUI.FlutterButton("Generate Home Test Data", FlutterUI.AMBER_700, Color.WHITE);
        b3.addActionListener(e -> syncSettingsScreen.generateHomeTestData());
        FlutterUI.FlutterButton b4 = new FlutterUI.FlutterButton("Export Full 6-Sheet Excel", FlutterUI.EMERALD_700, Color.WHITE);
        b4.addActionListener(e -> ctx.exportExcelList(ctx.cifMasterList, "Samayojan_00003_Full_Branch_6Sheets.xls"));
        right.add(b1); right.add(b2); right.add(b3); right.add(b4);
        appBar.add(right, BorderLayout.EAST);
        return appBar;
    }

    private JPanel buildNavRail() {
        JPanel rail = new JPanel();
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.setBackground(FlutterUI.RAIL_BG);
        rail.setPreferredSize(new Dimension(215, 100));
        rail.setBorder(new EmptyBorder(16, 10, 16, 10));

        addNav(rail, "1. Branch Dashboard",  "Master 6-Sheet & KPIs",     "SCREEN_DASHBOARD", 0);
        rail.add(Box.createVerticalStrut(8));
        addNav(rail, "2. Bulk DMR Studio",   "Paste SB List & Copy DMRs", "SCREEN_BULK",      1);
        rail.add(Box.createVerticalStrut(8));
        addNav(rail, "3. Single DMR & 360°", "1-Click Copy & Timeline",   "SCREEN_360",       2);
        rail.add(Box.createVerticalStrut(8));
        addNav(rail, "4. Sync & DB Center",  "Folder Path & File Filter", "SCREEN_SYNC",      3);
        rail.add(Box.createVerticalGlue());

        final JCheckBox chk = new JCheckBox("<html><body style='width:145px;color:#cbd5e1;font-size:10px;'>Treat Missing Intermediate Date as Rs. 0 (Cleared)</body></html>", true);
        chk.setOpaque(false);
        chk.addActionListener(e -> { ctx.missingAsZero = chk.isSelected(); ctx.recomputeAndNotify(); });
        rail.add(chk);
        return rail;
    }

    private void addNav(JPanel rail, String t, String s, final String key, final int idx) {
        FlutterUI.NavRailButton btn = new FlutterUI.NavRailButton(t, s);
        btn.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { switchScreen(key, idx); } });
        navButtons.add(btn);
        rail.add(btn);
    }

    private void switchScreen(String key, int idx) {
        screenCardLayout.show(screenContainer, key);
        for (int i = 0; i < navButtons.size(); i++) navButtons.get(i).setSelected(i == idx);
    }

    private void updateTopPill() {
        if (ctx.db.loadedDates.isEmpty()) topSyncPill.setText("Local DB: 0 Dates | Ready");
        else {
            List<String> s = new ArrayList<>(ctx.db.loadedDates);
            Collections.sort(s);
            topSyncPill.setText("Local DB: " + s.size() + " Date(s) [" + SamayojanModels.formatDate(s.get(0)) + " -> " + SamayojanModels.formatDate(s.get(s.size() - 1)) + "] | Accounts: " + ctx.db.accounts.size());
        }
    }

    private JPanel buildFooter() {
        JPanel f = new JPanel(new BorderLayout());
        f.setBackground(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(new LineBorder(FlutterUI.SLATE_200, 1), new EmptyBorder(8, 16, 8, 16)));
        footerStatusLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        f.add(footerStatusLbl, BorderLayout.WEST);
        return f;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new SamayojanApp().setVisible(true);
        });
    }
}