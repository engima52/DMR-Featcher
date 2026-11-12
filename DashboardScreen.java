import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DashboardScreen extends JPanel {
    private AppContext ctx;
    private JLabel kpiTotalLbl, kpiEligLbl, kpiNotEligLbl, kpiCheckLbl, kpiDatesLbl;
    private JComboBox<String> sheetSelectorCombo;
    private JTextField dashSearchField;
    private String activeStatusFilter = "ALL";
    private JTable masterTable;
    private DefaultTableModel masterTableModel;

    public DashboardScreen(final AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(14, 14));
        setOpaque(false);

        JPanel kpiRow = new JPanel(new GridLayout(1, 5, 14, 0));
        kpiRow.setOpaque(false);
        kpiTotalLbl   = createPastelKpiCard(kpiRow, "TOTAL FARMERS",      "0", FlutterUI.PASTEL_GOLD,   FlutterUI.PASTEL_LILAC, "ALL",           "ALL");
        kpiEligLbl    = createPastelKpiCard(kpiRow, "ELIGIBLE FARMERS",   "0", FlutterUI.PASTEL_MINT_1, FlutterUI.PASTEL_MINT_2,"OK",            "ELIGIBLE");
        kpiNotEligLbl = createPastelKpiCard(kpiRow, "NOT ELIGIBLE",       "0", FlutterUI.PASTEL_ROSE_1, FlutterUI.PASTEL_ROSE_2,"!",             "NOT ELIGIBLE");
        kpiCheckLbl   = createPastelKpiCard(kpiRow, "NEEDS TO CHECK",     "0", FlutterUI.PASTEL_AMBER,  FlutterUI.PASTEL_GOLD,  "?",             "NEEDS TO CHECK");
        kpiDatesLbl   = createPastelKpiCard(kpiRow, "SYNCED DATES IN DB", "0", FlutterUI.PASTEL_SKY_1,  FlutterUI.PASTEL_SKY_2, "DB",            null);
        add(kpiRow, BorderLayout.NORTH);

        FlutterUI.RoundedCard tableCard = new FlutterUI.RoundedCard(Color.WHITE);
        tableCard.setLayout(new BorderLayout(10, 12));
        tableCard.setBorder(new EmptyBorder(16, 18, 16, 18));

        String[] cols = {
            "SL", "12-Digit CIF", "Farmer Name", "SB Account (Today)",
            "Kharif Kind", "Kharif Cash", "Rabi Kind", "Rabi Cash",
            "Status", "Exact Audit Reason", "CBS Override"
        };
        masterTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 10; }
        };
        masterTable = FlutterUI.createStyledTable(masterTableModel, 8);

        JPanel filterBar = new JPanel(new BorderLayout(10, 0));
        filterBar.setOpaque(false);

        JPanel leftFlt = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftFlt.setOpaque(false);
        JLabel viewLbl = new JLabel("Portfolio View:");
        viewLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        leftFlt.add(viewLbl);

        sheetSelectorCombo = new JComboBox<>(new String[]{
            "1. ALL (Master CIF View)", "2. SB (Savings Accounts)",
            "3. KHARIF KIND", "4. KHARIF CASH", "5. RABI KIND", "6. RABI CASH"
        });
        sheetSelectorCombo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sheetSelectorCombo.addActionListener(e -> refreshTable());
        leftFlt.add(sheetSelectorCombo);
        filterBar.add(leftFlt, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);
        dashSearchField = new JTextField();
        dashSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dashSearchField.addActionListener(e -> refreshTable());
        searchPanel.add(new JLabel("Search Farmer / CIF / A/c: "), BorderLayout.WEST);
        searchPanel.add(dashSearchField, BorderLayout.CENTER);

        JPanel searchBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchBtns.setOpaque(false);
        FlutterUI.FlutterButton findBtn = new FlutterUI.FlutterButton("Filter", FlutterUI.SLATE_800, Color.WHITE);
        findBtn.addActionListener(e -> refreshTable());
        searchBtns.add(findBtn);

        FlutterUI.FlutterButton inspectBtn = new FlutterUI.FlutterButton("Inspect 360°", FlutterUI.INDIGO_600, Color.WHITE);
        inspectBtn.addActionListener(e -> {
            int row = masterTable.getSelectedRow();
            if (row >= 0) ctx.openIn360Screen(String.valueOf(masterTable.getValueAt(row, 1)));
        });
        searchBtns.add(inspectBtn);
        searchPanel.add(searchBtns, BorderLayout.EAST);
        filterBar.add(searchPanel, BorderLayout.CENTER);
        tableCard.add(filterBar, BorderLayout.NORTH);

        TableColumnModel cm = masterTable.getColumnModel();
        cm.getColumn(0).setMaxWidth(48);
        cm.getColumn(1).setPreferredWidth(100);
        cm.getColumn(2).setPreferredWidth(130);
        cm.getColumn(8).setPreferredWidth(95);
        cm.getColumn(9).setPreferredWidth(240);

        JComboBox<String> overrideCombo = new JComboBox<>(new String[]{
            "Auto", "Cleared <= 01/04/2026", "Cleared Late", "Old Loan Unpaid"
        });
        cm.getColumn(10).setCellEditor(new DefaultCellEditor(overrideCombo));

        masterTableModel.addTableModelListener(e -> {
            if (e.getColumn() == 10 && e.getFirstRow() >= 0) {
                int r = e.getFirstRow();
                String cif = String.valueOf(masterTableModel.getValueAt(r, 1));
                String val = String.valueOf(masterTableModel.getValueAt(r, 10));
                if ("Auto".equals(val) || val.isEmpty()) ctx.db.manualOverrides.remove(cif);
                else ctx.db.manualOverrides.put(cif, val);
                ctx.saveDatabase();
                SwingUtilities.invokeLater(ctx::recomputeAndNotify);
            }
        });

        masterTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && masterTable.getSelectedRow() >= 0) {
                    ctx.openIn360Screen(String.valueOf(masterTable.getValueAt(masterTable.getSelectedRow(), 1)));
                }
            }
        });

        JScrollPane scroll = new JScrollPane(masterTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        tableCard.add(scroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        ctx.addRefreshListener(this::updateKpisAndTable);
    }

    private JLabel createPastelKpiCard(JPanel parent, String title, String value, Color c1, Color c2, String badge, final String filterKey) {
        FlutterUI.PastelGradientCard card = new FlutterUI.PastelGradientCard(c1, c2, badge);
        card.setLayout(new BorderLayout(4, 6));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        JLabel tLbl = new JLabel(title);
        tLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tLbl.setForeground(FlutterUI.SLATE_700);

        JLabel vLbl = new JLabel(value);
        vLbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
        vLbl.setForeground(FlutterUI.SLATE_900);

        card.add(tLbl, BorderLayout.NORTH);
        card.add(vLbl, BorderLayout.CENTER);

        if (filterKey != null) {
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    activeStatusFilter = filterKey;
                    refreshTable();
                }
            });
        }
        parent.add(card);
        return vLbl;
    }

    private void updateKpisAndTable() {
        int elig = 0, notElig = 0, check = 0;
        for (CIFMasterRecord c : ctx.cifMasterList) {
            if ("ELIGIBLE".equals(c.finalStatus)) elig++;
            else if ("NOT ELIGIBLE".equals(c.finalStatus)) notElig++;
            else check++;
        }
        kpiTotalLbl.setText(String.valueOf(ctx.cifMasterList.size()));
        kpiEligLbl.setText(String.valueOf(elig));
        kpiNotEligLbl.setText(String.valueOf(notElig));
        kpiCheckLbl.setText(String.valueOf(check));
        kpiDatesLbl.setText(String.valueOf(ctx.db.loadedDates.size()));
        refreshTable();
    }

    private void refreshTable() {
        masterTableModel.setRowCount(0);
        String q = dashSearchField.getText().trim().toUpperCase();
        int sheetIdx = sheetSelectorCombo.getSelectedIndex();
        int sl = 1;
        for (CIFMasterRecord cm : ctx.cifMasterList) {
            if (!"ALL".equals(activeStatusFilter) && !activeStatusFilter.equals(cm.finalStatus)) continue;
            if (sheetIdx == 1 && cm.sb.isEmpty()) continue;
            if (sheetIdx == 2 && cm.kharifKind.isEmpty()) continue;
            if (sheetIdx == 3 && cm.kharifCash.isEmpty()) continue;
            if (sheetIdx == 4 && cm.rabiKind.isEmpty()) continue;
            if (sheetIdx == 5 && cm.rabiCash.isEmpty()) continue;
            if (!q.isEmpty()) {
                boolean m = cm.cif.contains(q) || cm.name.toUpperCase().contains(q) ||
                    cm.sb.stream().anyMatch(a -> a.account.contains(q)) ||
                    cm.allDmr.stream().anyMatch(a -> a.account.contains(q));
                if (!m) continue;
            }
            masterTableModel.addRow(new Object[]{
                sl++, cm.cif, cm.name,
                SamayojanModels.formatShortSummary(cm.sb),
                SamayojanModels.formatShortSummary(cm.kharifKind),
                SamayojanModels.formatShortSummary(cm.kharifCash),
                SamayojanModels.formatShortSummary(cm.rabiKind),
                SamayojanModels.formatShortSummary(cm.rabiCash),
                cm.finalStatus, cm.finalReason, cm.override
            });
        }
    }
}