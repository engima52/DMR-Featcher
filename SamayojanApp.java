import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class SamayojanApp extends JFrame {

    private static final File DB_FILE = new File("samayojan_local.db");
    private static final File CONFIG_FILE = new File("samayojan_config.properties");

    private LocalDatabase db = new LocalDatabase();
    private final Properties config = new Properties();

    private final List<CIFMasterRecord> cifMasterList = new ArrayList<>();
    private final Map<String, CIFMasterRecord> cifLookupMap = new HashMap<>();
    private final Map<String, CIFMasterRecord> acctToCifMap = new HashMap<>();

    // UI Controls
    private JTextField pathField;
    private JTextField fileFilterField;
    private JCheckBox missingAsZeroChk;
    private JLabel syncBadgeLabel;
    private JLabel statusLabel;

    // KPI Labels
    private JLabel kpiTotalLbl, kpiEligLbl, kpiNotEligLbl, kpiCheckLbl, kpiDatesLbl;

    // DMR Fetcher Controls
    private JTextField dmrInputField;
    private JCheckBox hideZeroDmrChk;
    private JLabel dmrFarmerHeaderLbl;
    private JLabel dmrStatusBadgeLbl;
    private ScrollableCardPanel dmrCardsContainer;
    private CIFMasterRecord currentFetchedCif = null;

    // Table Controls
    private JTextField tableSearchField;
    private JComboBox<String> sheetSelectorCombo;
    private String activeStatusFilter = "ALL";
    private JTable mainTable;
    private DefaultTableModel tableModel;

    // Theme Colors
    private static final Color SLATE_900 = new Color(15, 23, 42);
    private static final Color SLATE_800 = new Color(30, 41, 59);
    private static final Color SLATE_700 = new Color(51, 65, 85);
    private static final Color SLATE_200 = new Color(203, 213, 225);
    private static final Color SLATE_100 = new Color(241, 245, 249);
    private static final Color SLATE_50  = new Color(248, 250, 252);
    private static final Color BLUE_600  = new Color(37, 99, 235);
    private static final Color GREEN_700 = new Color(21, 128, 61);
    private static final Color GREEN_100 = new Color(220, 252, 231);
    private static final Color RED_700   = new Color(185, 28, 28);
    private static final Color RED_100   = new Color(254, 226, 226);
    private static final Color AMBER_700 = new Color(180, 83, 9);
    private static final Color AMBER_100 = new Color(254, 243, 199);

    public SamayojanApp() {
        setTitle("DCCB Harpalpur (00003) — Standalone Samayojan & DMR Fetcher System");
        setSize(1460, 860);
        setMinimumSize(new Dimension(1180, 720));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadConfig();
        initUI();
        loadLocalDatabaseOnStartup();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 12, 10, 12));
        root.setBackground(SLATE_100);

        JPanel northContainer = new JPanel(new BorderLayout(8, 8));
        northContainer.setBackground(SLATE_100);
        northContainer.add(buildTopSyncBar(), BorderLayout.NORTH);
        northContainer.add(buildKpiRow(), BorderLayout.SOUTH);
        root.add(northContainer, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.63);
        splitPane.setDividerSize(8);
        splitPane.setBorder(null);

        splitPane.setLeftComponent(buildLeftTableWorkspace());
        splitPane.setRightComponent(buildRightDmrFetcherPanel());
        root.add(splitPane, BorderLayout.CENTER);

        root.add(buildFooterBar(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildTopSyncBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 8));
        bar.setBackground(SLATE_900);
        bar.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("JILA SAHAKARI KENDRIYA BANK MARYADIT — BRANCH HARPALPUR (00003)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Color.WHITE);
        syncBadgeLabel = new JLabel("Local DB: 0 Dates Loaded | Ready for 1-Click Delta Sync");
        syncBadgeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        syncBadgeLabel.setForeground(new Color(148, 163, 184));
        titleBox.add(title);
        titleBox.add(syncBadgeLabel);
        bar.add(titleBox, BorderLayout.WEST);

        JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        ctrlPanel.setOpaque(false);

        JLabel pathLbl = new JLabel("Reports Folder:");
        pathLbl.setForeground(Color.WHITE);
        pathLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ctrlPanel.add(pathLbl);

        pathField = new JTextField(config.getProperty("reportsPath", ""), 18);
        pathField.setFont(new Font("Consolas", Font.PLAIN, 12));
        ctrlPanel.add(pathField);

        JButton browseBtn = createButton("Browse...", SLATE_700, Color.WHITE);
        browseBtn.addActionListener(e -> chooseReportsFolder());
        ctrlPanel.add(browseBtn);

        JLabel filterLbl = new JLabel("File Filter:");
        filterLbl.setForeground(Color.WHITE);
        filterLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ctrlPanel.add(filterLbl);

        fileFilterField = new JTextField(config.getProperty("fileFilter", ReportParser.DEFAULT_FILE_FILTER), 6);
        fileFilterField.setFont(new Font("Consolas", Font.BOLD, 12));
        ctrlPanel.add(fileFilterField);

        JButton syncBtn = createButton("1-Click Sync New Dates", BLUE_600, Color.WHITE);
        syncBtn.addActionListener(e -> runIncrementalSync(false));
        ctrlPanel.add(syncBtn);

        JButton demoBtn = createButton("Generate Home Test Data", AMBER_700, Color.WHITE);
        demoBtn.addActionListener(e -> generateHomeTestData());
        ctrlPanel.add(demoBtn);

        JButton resetDbBtn = createButton("Reset DB", RED_700, Color.WHITE);
        resetDbBtn.addActionListener(e -> resetDatabase());
        ctrlPanel.add(resetDbBtn);

        bar.add(ctrlPanel, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 5, 10, 0));
        row.setBackground(SLATE_100);

        kpiTotalLbl   = createKpiCard(row, "TOTAL FARMERS (ALL)", "0", SLATE_800, "ALL");
        kpiEligLbl    = createKpiCard(row, "ELIGIBLE (CLICK TO FILTER)", "0", GREEN_700, "ELIGIBLE");
        kpiNotEligLbl = createKpiCard(row, "NOT ELIGIBLE (UNPAID)", "0", RED_700, "NOT ELIGIBLE");
        kpiCheckLbl   = createKpiCard(row, "NEEDS TO CHECK", "0", AMBER_700, "NEEDS TO CHECK");
        kpiDatesLbl   = createKpiCard(row, "SYNCED DATES IN DB", "0", BLUE_600, null);

        return row;
    }

    private JLabel createKpiCard(JPanel parent, String title, String value, Color accent, final String filterKey) {
        JPanel card = new JPanel(new BorderLayout(4, 2));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(SLATE_200, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel tLbl = new JLabel(title);
        tLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tLbl.setForeground(SLATE_700);

        JLabel vLbl = new JLabel(value);
        vLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        vLbl.setForeground(accent);

        card.add(tLbl, BorderLayout.NORTH);
        card.add(vLbl, BorderLayout.CENTER);

        if (filterKey != null) {
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    activeStatusFilter = filterKey;
                    refreshTable();
                }
            });
        }
        parent.add(card);
        return vLbl;
    }

    private JPanel buildLeftTableWorkspace() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(SLATE_200, 1),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel topFilterBar = new JPanel(new BorderLayout(8, 6));
        topFilterBar.setBackground(Color.WHITE);

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftControls.setBackground(Color.WHITE);
        leftControls.add(new JLabel("Sheet View:"));

        sheetSelectorCombo = new JComboBox<>(new String[]{
            "1. ALL (Master CIF View)",
            "2. SB (Savings Accounts)",
            "3. KHARIF KIND",
            "4. KHARIF CASH",
            "5. RABI KIND",
            "6. RABI CASH"
        });
        sheetSelectorCombo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sheetSelectorCombo.addActionListener(e -> refreshTable());
        leftControls.add(sheetSelectorCombo);

        missingAsZeroChk = new JCheckBox("Missing Intermediate Date = Rs. 0 (Cleared)", true);
        missingAsZeroChk.setBackground(Color.WHITE);
        missingAsZeroChk.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        missingAsZeroChk.addActionListener(e -> {
            recomputeAndRefreshUI();
            if (currentFetchedCif != null) renderDmrFetcherCard(currentFetchedCif.cif);
        });
        leftControls.add(missingAsZeroChk);

        topFilterBar.add(leftControls, BorderLayout.WEST);

        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setBackground(Color.WHITE);
        tableSearchField = new JTextField();
        tableSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableSearchField.addActionListener(e -> refreshTable());
        searchBox.add(new JLabel("Search / Bulk Filter: "), BorderLayout.WEST);
        searchBox.add(tableSearchField, BorderLayout.CENTER);

        JButton findBtn = createButton("Filter", SLATE_800, Color.WHITE);
        findBtn.addActionListener(e -> refreshTable());
        searchBox.add(findBtn, BorderLayout.EAST);

        topFilterBar.add(searchBox, BorderLayout.CENTER);
        panel.add(topFilterBar, BorderLayout.NORTH);

        String[] cols = {
            "SL", "12-Digit CIF", "Farmer Name", "SB Account (Today)",
            "Kharif Kind", "Kharif Cash", "Rabi Kind", "Rabi Cash",
            "Status", "Exact Audit Reason", "CBS Override"
        };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 10;
            }
        };

        mainTable = new JTable(tableModel) {
            @Override
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int r = rowAtPoint(p), c = columnAtPoint(p);
                if (r >= 0 && c >= 0) {
                    Object val = getValueAt(r, c);
                    return val != null ? val.toString() : null;
                }
                return null;
            }
        };
        mainTable.setRowHeight(28);
        mainTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        mainTable.setGridColor(SLATE_200);
        mainTable.setShowGrid(true);
        mainTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setBackground(SLATE_800);
                lbl.setForeground(Color.WHITE);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                lbl.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(SLATE_700, 1),
                    new EmptyBorder(6, 6, 6, 6)
                ));
                lbl.setOpaque(true);
                return lbl;
            }
        };
        mainTable.getTableHeader().setDefaultRenderer(headerRenderer);
        mainTable.getTableHeader().setPreferredSize(new Dimension(100, 32));

        DefaultTableCellRenderer zebraRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : SLATE_50);
                    c.setForeground(SLATE_900);
                }
                return c;
            }
        };
        for (int i = 0; i < cols.length; i++) {
            if (i != 8) mainTable.getColumnModel().getColumn(i).setCellRenderer(zebraRenderer);
        }
        mainTable.getColumnModel().getColumn(8).setCellRenderer(new StatusBadgeRenderer());

        TableColumnModel cm = mainTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(35);
        cm.getColumn(0).setMaxWidth(45);
        cm.getColumn(1).setPreferredWidth(95);
        cm.getColumn(2).setPreferredWidth(120);
        cm.getColumn(3).setPreferredWidth(115);
        cm.getColumn(4).setPreferredWidth(110);
        cm.getColumn(5).setPreferredWidth(90);
        cm.getColumn(6).setPreferredWidth(90);
        cm.getColumn(7).setPreferredWidth(90);
        cm.getColumn(8).setPreferredWidth(85);
        cm.getColumn(9).setPreferredWidth(210);
        cm.getColumn(10).setPreferredWidth(95);

        JComboBox<String> overrideCombo = new JComboBox<>(new String[]{
            "Auto", "Cleared <= 01/04/2026", "Cleared Late", "Old Loan Unpaid"
        });
        cm.getColumn(10).setCellEditor(new DefaultCellEditor(overrideCombo));

        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 10 && e.getFirstRow() >= 0) {
                int row = e.getFirstRow();
                final String cif = String.valueOf(tableModel.getValueAt(row, 1));
                String val = String.valueOf(tableModel.getValueAt(row, 10));
                if ("Auto".equals(val) || val.isEmpty()) {
                    db.manualOverrides.remove(cif);
                } else {
                    db.manualOverrides.put(cif, val);
                }
                saveLocalDatabase();
                recomputeAndRefreshUI();
                SwingUtilities.invokeLater(() -> {
                    if (currentFetchedCif != null && currentFetchedCif.cif.equals(cif)) {
                        renderDmrFetcherCard(cif);
                    }
                });
            }
        });

        mainTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && mainTable.getSelectedRow() >= 0) {
                String cif = String.valueOf(mainTable.getValueAt(mainTable.getSelectedRow(), 1));
                renderDmrFetcherCard(cif);
            }
        });

        panel.add(new JScrollPane(mainTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRightDmrFetcherPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BLUE_600, 2),
            new EmptyBorder(10, 12, 10, 12)
        ));

        JPanel topBox = new JPanel(new BorderLayout(6, 6));
        topBox.setBackground(Color.WHITE);

        JLabel hdr = new JLabel("QUICK DMR FETCHER (ENTER SB A/C FOR CBS COPY-PASTE)");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 13));
        hdr.setForeground(BLUE_600);
        topBox.add(hdr, BorderLayout.NORTH);

        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setBackground(Color.WHITE);

        dmrInputField = new JTextField();
        dmrInputField.setFont(new Font("Consolas", Font.BOLD, 15));
        dmrInputField.addActionListener(e -> fetchDmrByInput());
        inputRow.add(dmrInputField, BorderLayout.CENTER);

        JButton fetchBtn = createButton("Fetch DMRs", BLUE_600, Color.WHITE);
        fetchBtn.addActionListener(e -> fetchDmrByInput());
        inputRow.add(fetchBtn, BorderLayout.EAST);

        topBox.add(inputRow, BorderLayout.CENTER);

        JPanel subRow = new JPanel(new BorderLayout());
        subRow.setBackground(Color.WHITE);

        hideZeroDmrChk = new JCheckBox("Show Only Active DMRs (Today Bal > Rs. 0)", false);
        hideZeroDmrChk.setBackground(Color.WHITE);
        hideZeroDmrChk.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hideZeroDmrChk.addActionListener(e -> {
            if (currentFetchedCif != null) renderDmrFetcherCard(currentFetchedCif.cif);
        });
        subRow.add(hideZeroDmrChk, BorderLayout.WEST);

        JButton copyAllBtn = createButton("Copy All Active DMRs", GREEN_700, Color.WHITE);
        copyAllBtn.addActionListener(e -> copyAllDmrToClipboard());
        subRow.add(copyAllBtn, BorderLayout.EAST);

        topBox.add(subRow, BorderLayout.SOUTH);
        panel.add(topBox, BorderLayout.NORTH);

        JPanel centerBox = new JPanel(new BorderLayout(6, 6));
        centerBox.setBackground(Color.WHITE);

        JPanel farmerBanner = new JPanel(new BorderLayout(6, 4));
        farmerBanner.setBackground(SLATE_100);
        farmerBanner.setBorder(new EmptyBorder(8, 10, 8, 10));

        dmrFarmerHeaderLbl = new JLabel("Enter an SB Account No. above or click any row in the table.");
        dmrFarmerHeaderLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dmrFarmerHeaderLbl.setForeground(SLATE_900);
        dmrStatusBadgeLbl = new JLabel("");
        dmrStatusBadgeLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));

        farmerBanner.add(dmrFarmerHeaderLbl, BorderLayout.CENTER);
        farmerBanner.add(dmrStatusBadgeLbl, BorderLayout.EAST);
        centerBox.add(farmerBanner, BorderLayout.NORTH);

        dmrCardsContainer = new ScrollableCardPanel();
        dmrCardsContainer.setLayout(new BoxLayout(dmrCardsContainer, BoxLayout.Y_AXIS));
        dmrCardsContainer.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(dmrCardsContainer);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        centerBox.add(scroll, BorderLayout.CENTER);

        panel.add(centerBox, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFooterBar() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(SLATE_100);

        statusLabel = new JLabel("Ready. Standalone Local Database active (No Excel dependency required).");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(SLATE_800);
        footer.add(statusLabel, BorderLayout.WEST);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setOpaque(false);

        JButton exportExcelBtn = createButton("Export 6-Sheet Styled Excel (.xls)", GREEN_700, Color.WHITE);
        exportExcelBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        exportExcelBtn.addActionListener(e -> exportSixSheetExcel());
        rightBtns.add(exportExcelBtn);

        footer.add(rightBtns, BorderLayout.EAST);
        return footer;
    }

    private JButton createButton(String text, final Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(bg.darker(), 1),
            new EmptyBorder(6, 12, 6, 12)
        ));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void fetchDmrByInput() {
        String q = dmrInputField.getText().trim();
        if (q.isEmpty()) return;

        CIFMasterRecord match = acctToCifMap.get(q);
        if (match == null) match = cifLookupMap.get(q);

        if (match == null) {
            for (CIFMasterRecord cm : cifMasterList) {
                if (cm.cif.endsWith(q) ||
                    cm.sb.stream().anyMatch(a -> a.account.endsWith(q) || a.account.contains(q)) ||
                    cm.allDmr.stream().anyMatch(a -> a.account.endsWith(q) || a.account.contains(q))) {
                    match = cm;
                    break;
                }
            }
        }

        if (match == null) {
            JOptionPane.showMessageDialog(this,
                "No Farmer / CIF found matching Account or CIF: " + q,
                "Account Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        renderDmrFetcherCard(match.cif);
    }

    private void renderDmrFetcherCard(String cif) {
        final CIFMasterRecord cm = cifLookupMap.get(cif);
        if (cm == null) return;
        currentFetchedCif = cm;

        dmrFarmerHeaderLbl.setText(cm.name + "  |  CIF: " + cm.cif);
        dmrStatusBadgeLbl.setText("  " + cm.finalStatus + "  ");
        dmrStatusBadgeLbl.setOpaque(true);
        if ("ELIGIBLE".equals(cm.finalStatus)) {
            dmrStatusBadgeLbl.setBackground(GREEN_100);
            dmrStatusBadgeLbl.setForeground(GREEN_700);
        } else if ("NOT ELIGIBLE".equals(cm.finalStatus)) {
            dmrStatusBadgeLbl.setBackground(RED_100);
            dmrStatusBadgeLbl.setForeground(RED_700);
        } else {
            dmrStatusBadgeLbl.setBackground(AMBER_100);
            dmrStatusBadgeLbl.setForeground(AMBER_700);
        }

        dmrCardsContainer.removeAll();

        JPanel reasonCard = new JPanel(new BorderLayout(8, 4));
        reasonCard.setBackground(new Color(255, 251, 235));
        reasonCard.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(252, 211, 77), 1),
            new EmptyBorder(8, 10, 8, 10)
        ));

        JTextArea reasonArea = new JTextArea("Audit Remark: " + cm.finalReason);
        reasonArea.setEditable(false);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setOpaque(false);
        reasonArea.setFont(new Font("Segoe UI", Font.BOLD, 11));
        reasonArea.setForeground(SLATE_900);
        reasonCard.add(reasonArea, BorderLayout.CENTER);

        JButton copyCifBtn = createButton("Copy CIF", SLATE_700, Color.WHITE);
        copyCifBtn.addActionListener(e -> copyToClipboard(cm.cif, "12-Digit CIF " + cm.cif));
        JPanel cifBtnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        cifBtnWrap.setOpaque(false);
        cifBtnWrap.add(copyCifBtn);
        reasonCard.add(cifBtnWrap, BorderLayout.EAST);

        dmrCardsContainer.add(reasonCard);
        dmrCardsContainer.add(Box.createVerticalStrut(8));

        for (EnrichedAccount sbAcc : cm.sb) {
            dmrCardsContainer.add(buildSingleAccountCopyCard("SAVINGS (SBCHQ)", sbAcc, true));
            dmrCardsContainer.add(Box.createVerticalStrut(6));
        }

        boolean hideZero = hideZeroDmrChk.isSelected();
        int shownDmr = 0;
        for (EnrichedAccount dmr : cm.allDmr) {
            if (hideZero && dmr.balToday <= 0.0) continue;
            dmrCardsContainer.add(buildSingleAccountCopyCard(dmr.category, dmr, false));
            dmrCardsContainer.add(Box.createVerticalStrut(6));
            shownDmr++;
        }

        if (shownDmr == 0) {
            JLabel noneLbl = new JLabel("No DMR accounts to display (or all DMR balances are Rs. 0.00).");
            noneLbl.setForeground(SLATE_700);
            noneLbl.setBorder(new EmptyBorder(10, 6, 10, 6));
            dmrCardsContainer.add(noneLbl);
        }

        dmrCardsContainer.revalidate();
        dmrCardsContainer.repaint();
    }

    private JPanel buildSingleAccountCopyCard(final String categoryTitle, final EnrichedAccount acc, boolean isSb) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(isSb ? new Color(240, 249, 255) : Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(isSb ? new Color(147, 197, 253) : SLATE_200, 1),
            new EmptyBorder(8, 10, 8, 10)
        ));

        JPanel topRow = new JPanel(new BorderLayout(6, 0));
        topRow.setOpaque(false);

        JLabel titleAndAcctLbl = new JLabel(categoryTitle + " :  " + acc.account);
        titleAndAcctLbl.setFont(new Font("Consolas", Font.BOLD, 14));
        titleAndAcctLbl.setForeground(SLATE_900);
        topRow.add(titleAndAcctLbl, BorderLayout.WEST);

        JLabel todayBalLbl = new JLabel("Today: Rs. " + SamayojanModels.AMT_FMT.format(acc.balToday));
        todayBalLbl.setFont(new Font("Consolas", Font.BOLD, 14));
        todayBalLbl.setForeground((!isSb && acc.balToday > SamayojanModels.MARGIN_RS) ? RED_700 : GREEN_700);
        topRow.add(todayBalLbl, BorderLayout.EAST);

        card.add(topRow, BorderLayout.NORTH);

        JPanel botRow = new JPanel(new BorderLayout(6, 0));
        botRow.setOpaque(false);

        String aprInfo = acc.existedOnApr1
            ? ("01/04/2026 Bal: Rs." + SamayojanModels.AMT_FMT.format(acc.balApr1))
            : ("New After 01/04 (1st Seen: " + SamayojanModels.formatDate(acc.firstSeenDate) + ")");
        String clrInfo = acc.clearedDate != null ? ("  |  Cleared: " + SamayojanModels.formatDate(acc.clearedDate)) : "";

        JLabel histLbl = new JLabel(aprInfo + clrInfo);
        histLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        histLbl.setForeground(SLATE_700);
        botRow.add(histLbl, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnGroup.setOpaque(false);

        JButton copyAccBtn = createButton("Copy A/c: " + acc.account, SLATE_800, Color.WHITE);
        copyAccBtn.addActionListener(e -> copyToClipboard(acc.account, categoryTitle + " A/c " + acc.account));
        btnGroup.add(copyAccBtn);

        JButton copyBalBtn = createButton("Copy Bal", BLUE_600, Color.WHITE);
        copyBalBtn.addActionListener(e -> copyToClipboard(SamayojanModels.AMT_FMT.format(acc.balToday), "Balance " + SamayojanModels.AMT_FMT.format(acc.balToday)));
        btnGroup.add(copyBalBtn);

        botRow.add(btnGroup, BorderLayout.EAST);
        card.add(botRow, BorderLayout.SOUTH);

        return card;
    }

    private void copyAllDmrToClipboard() {
        if (currentFetchedCif == null) {
            JOptionPane.showMessageDialog(this, "Fetch a farmer's SB account first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (EnrichedAccount dmr : currentFetchedCif.allDmr) {
            if (hideZeroDmrChk.isSelected() && dmr.balToday <= 0.0) continue;
            sb.append(dmr.category).append("\t")
              .append(dmr.account).append("\t")
              .append(SamayojanModels.AMT_FMT.format(dmr.balToday)).append("\n");
        }
        if (sb.length() == 0) {
            JOptionPane.showMessageDialog(this, "No active DMR accounts to copy.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        copyToClipboard(sb.toString().trim(), "All DMR Accounts & Today Balances");
    }

    private void copyToClipboard(String value, String label) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
        statusLabel.setText("Copied to Clipboard: " + label + " -> [" + value + "]");
    }

    private void chooseReportsFolder() {
        JFileChooser chooser = new JFileChooser(pathField.getText().trim().isEmpty() ? "." : pathField.getText().trim());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            saveConfig();
            runIncrementalSync(false);
        }
    }

    private void runIncrementalSync(final boolean silentIfNoFolder) {
        final String rootPathStr = pathField.getText().trim();
        if (rootPathStr.isEmpty()) {
            if (!silentIfNoFolder) {
                JOptionPane.showMessageDialog(this,
                    "Please click 'Browse...' to select your 'reports' folder, or click 'Generate Home Test Data' to test right now.",
                    "Reports Folder Not Set", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        final Path rootDir = Paths.get(rootPathStr);
        if (!Files.exists(rootDir)) {
            if (!silentIfNoFolder) {
                JOptionPane.showMessageDialog(this, "Folder does not exist: " + rootPathStr, "Error", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        saveConfig();
        final String filterPattern = fileFilterField.getText().trim();
        statusLabel.setText("Scanning reports/YYYYMMDD/ for new un-imported dates...");

        new Thread(() -> {
            try {
                final Set<String> newDatesFound = ReportParser.syncNewDatesFromFolder(rootDir, filterPattern, db);

                if (newDatesFound.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        recomputeAndRefreshUI();
                        statusLabel.setText("Database is already 100% up-to-date (" + db.loadedDates.size() + " dates in Local DB).");
                        if (!silentIfNoFolder) {
                            JOptionPane.showMessageDialog(this,
                                "All date folders in '" + rootPathStr + "' are already synced!\nTotal Synced Dates: " + db.loadedDates.size(),
                                "Already Up-To-Date", JOptionPane.INFORMATION_MESSAGE);
                        }
                    });
                    return;
                }

                saveLocalDatabase();

                SwingUtilities.invokeLater(() -> {
                    recomputeAndRefreshUI();
                    if (currentFetchedCif != null) renderDmrFetcherCard(currentFetchedCif.cif);
                    else if (!cifMasterList.isEmpty()) renderDmrFetcherCard(cifMasterList.get(0).cif);
                    statusLabel.setText("1-Click Delta Sync Complete! Added " + newDatesFound.size() + " new date(s): " + newDatesFound);
                    if (!silentIfNoFolder) {
                        JOptionPane.showMessageDialog(this,
                            "Delta Sync Complete!\nNewly Imported Date(s): " + newDatesFound +
                            "\nTotal Dates in Local DB: " + db.loadedDates.size() +
                            "\nTotal Farmers (CIFs): " + cifMasterList.size(),
                            "1-Click Sync Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Sync Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Sync Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void recomputeAndRefreshUI() {
        boolean missingAsZero = missingAsZeroChk != null && missingAsZeroChk.isSelected();
        EligibilityEngine.recompute(db, missingAsZero, cifMasterList, cifLookupMap, acctToCifMap);

        int elig = 0, notElig = 0, check = 0;
        for (CIFMasterRecord c : cifMasterList) {
            if ("ELIGIBLE".equals(c.finalStatus)) elig++;
            else if ("NOT ELIGIBLE".equals(c.finalStatus)) notElig++;
            else check++;
        }
        kpiTotalLbl.setText(String.valueOf(cifMasterList.size()));
        kpiEligLbl.setText(String.valueOf(elig));
        kpiNotEligLbl.setText(String.valueOf(notElig));
        kpiCheckLbl.setText(String.valueOf(check));
        kpiDatesLbl.setText(String.valueOf(db.loadedDates.size()));

        if (!db.loadedDates.isEmpty()) {
            List<String> sorted = new ArrayList<>(db.loadedDates);
            Collections.sort(sorted);
            syncBadgeLabel.setText("Local DB: " + sorted.size() + " Date(s) [" +
                SamayojanModels.formatDate(sorted.get(0)) + " -> " +
                SamayojanModels.formatDate(sorted.get(sorted.size() - 1)) + "] | Total Accounts: " + db.accounts.size());
        } else {
            syncBadgeLabel.setText("Local DB: 0 Dates Loaded | Ready for 1-Click Delta Sync");
        }

        refreshTable();
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        String rawSearch = tableSearchField.getText().trim().toUpperCase();
        String[] bulkTerms = rawSearch.isEmpty() ? new String[0] : rawSearch.split("[\\s,;]+");
        int sheetIdx = sheetSelectorCombo.getSelectedIndex();

        int sl = 1;
        for (CIFMasterRecord cm : cifMasterList) {
            if (!"ALL".equals(activeStatusFilter) && !activeStatusFilter.equals(cm.finalStatus)) continue;
            if (sheetIdx == 1 && cm.sb.isEmpty()) continue;
            if (sheetIdx == 2 && cm.kharifKind.isEmpty()) continue;
            if (sheetIdx == 3 && cm.kharifCash.isEmpty()) continue;
            if (sheetIdx == 4 && cm.rabiKind.isEmpty()) continue;
            if (sheetIdx == 5 && cm.rabiCash.isEmpty()) continue;

            if (bulkTerms.length > 0 && !matchesAnyTerm(cm, bulkTerms)) continue;

            tableModel.addRow(new Object[]{
                sl++,
                cm.cif,
                cm.name,
                SamayojanModels.formatShortSummary(cm.sb),
                SamayojanModels.formatShortSummary(cm.kharifKind),
                SamayojanModels.formatShortSummary(cm.kharifCash),
                SamayojanModels.formatShortSummary(cm.rabiKind),
                SamayojanModels.formatShortSummary(cm.rabiCash),
                cm.finalStatus,
                cm.finalReason,
                cm.override
            });
        }
    }

    private boolean matchesAnyTerm(CIFMasterRecord cm, String[] terms) {
        for (String t : terms) {
            if (t.isEmpty()) continue;
            if (cm.cif.contains(t) || cm.name.toUpperCase().contains(t)) return true;
            for (EnrichedAccount a : cm.sb) if (a.account.contains(t)) return true;
            for (EnrichedAccount a : cm.allDmr) if (a.account.contains(t)) return true;
        }
        return false;
    }

    private void generateHomeTestData() {
        try {
            Path demoRoot = Paths.get("reports").toAbsolutePath();
            pathField.setText(demoRoot.toString());
            int stage = ReportParser.generateDemoFiles(demoRoot);
            if (stage == 1) {
                runIncrementalSync(false);
            } else if (stage == 2) {
                JOptionPane.showMessageDialog(this,
                    "Created new date folder 'reports/20261110'!\nNow importing ONLY 20261110 via 1-Click Delta Sync...",
                    "Simulating Next Day (30th)", JOptionPane.INFORMATION_MESSAGE);
                runIncrementalSync(false);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Demo folders (20260401, 20260518, 20261109, 20261110) are already synced.",
                    "Demo Data Ready", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error generating demo data: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadLocalDatabaseOnStartup() {
        if (DB_FILE.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DB_FILE))) {
                db = (LocalDatabase) ois.readObject();
                recomputeAndRefreshUI();
                if (!cifMasterList.isEmpty()) renderDmrFetcherCard(cifMasterList.get(0).cif);
                statusLabel.setText("Loaded Local Database (" + db.loadedDates.size() + " dates, " + db.accounts.size() + " accounts).");
            } catch (Throwable e) {
                db = new LocalDatabase();
                DB_FILE.delete();
                statusLabel.setText("Old cache cleared. Starting fresh Local Database.");
            }
        }
        try {
            if (!pathField.getText().trim().isEmpty() && Files.exists(Paths.get(pathField.getText().trim()))) {
                runIncrementalSync(true);
            }
        } catch (Throwable ignored) {}
    }

    private void saveLocalDatabase() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DB_FILE))) {
            oos.writeObject(db);
        } catch (Exception e) {
            System.err.println("Could not save local DB: " + e.getMessage());
        }
    }

    private void resetDatabase() {
        int res = JOptionPane.showConfirmDialog(this,
            "Reset local database (samayojan_local.db) and clear all imported dates?",
            "Confirm Reset", JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            db = new LocalDatabase();
            if (DB_FILE.exists()) DB_FILE.delete();
            recomputeAndRefreshUI();
            dmrCardsContainer.removeAll();
            dmrCardsContainer.repaint();
            statusLabel.setText("Local Database reset.");
        }
    }

    private void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                config.load(fis);
            } catch (Exception ignored) {}
        }
    }

    private void saveConfig() {
        config.setProperty("reportsPath", pathField.getText().trim());
        config.setProperty("fileFilter", fileFilterField.getText().trim());
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            config.store(fos, "DCCB 00003 Samayojan Settings");
        } catch (Exception ignored) {}
    }

    private void exportSixSheetExcel() {
        if (cifMasterList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No data in database to export.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser(".");
        chooser.setSelectedFile(new File("Samayojan_00003_Harpalpur_6Sheets.xls"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File outFile = chooser.getSelectedFile();
        try {
            ExcelExporter.exportSixSheets(outFile, cifMasterList);
            JOptionPane.showMessageDialog(this, "Exported 6-Sheet Styled Excel Workbook:\n" + outFile.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static class ScrollableCardPanel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 64; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String st = String.valueOf(value);
            c.setHorizontalAlignment(CENTER);
            c.setFont(c.getFont().deriveFont(Font.BOLD));
            c.setOpaque(true);
            if (!isSelected) {
                if ("ELIGIBLE".equals(st)) {
                    c.setBackground(GREEN_100);
                    c.setForeground(GREEN_700);
                } else if ("NOT ELIGIBLE".equals(st)) {
                    c.setBackground(RED_100);
                    c.setForeground(RED_700);
                } else {
                    c.setBackground(AMBER_100);
                    c.setForeground(AMBER_700);
                }
            }
            return c;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new SamayojanApp().setVisible(true);
        });
    }
}
