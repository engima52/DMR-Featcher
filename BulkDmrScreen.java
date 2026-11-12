import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.util.*;
import java.util.List;

public class BulkDmrScreen extends JPanel {
    private AppContext ctx;
    private JTextArea bulkSbPasteArea;
    private JLabel bulkStatsBadge;
    private JCheckBox bulkOnlyActiveChk, bulkFlatModeChk;
    private JTable bulkTable;
    private DefaultTableModel bulkTableModel;
    private FlutterUI.ScrollableCardPanel bulkQuickCardsPanel;
    private JLabel bulkSelectedFarmerLbl;

    private final List<CIFMasterRecord> bulkMatchedCifs = new ArrayList<>();
    private final List<BulkDmrRow> bulkFlatRows = new ArrayList<>();

    public BulkDmrScreen(final AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(12, 12));
        setOpaque(false);

        FlutterUI.RoundedCard leftPasteCard = new FlutterUI.RoundedCard(Color.WHITE);
        leftPasteCard.setLayout(new BorderLayout(8, 8));
        leftPasteCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        leftPasteCard.setPreferredSize(new Dimension(290, 100));

        JPanel pasteHdr = new JPanel(new GridLayout(2, 1, 0, 2));
        pasteHdr.setOpaque(false);
        JLabel pTitle = new JLabel("1. PASTE BULK SB ACCOUNTS");
        pTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pTitle.setForeground(FlutterUI.INDIGO_600);
        JLabel pSub = new JLabel("Paste column from Excel (1 per line)");
        pSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pSub.setForeground(FlutterUI.SLATE_500);
        pasteHdr.add(pTitle);
        pasteHdr.add(pSub);
        leftPasteCard.add(pasteHdr, BorderLayout.NORTH);

        bulkSbPasteArea = new JTextArea("500010000001\n500010000003\n500010000004");
        bulkSbPasteArea.setFont(new Font("Consolas", Font.BOLD, 13));
        bulkSbPasteArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane pasteScroll = new JScrollPane(bulkSbPasteArea);
        pasteScroll.setBorder(new LineBorder(FlutterUI.SLATE_200, 1));
        leftPasteCard.add(pasteScroll, BorderLayout.CENTER);

        JPanel pasteFooter = new JPanel(new GridLayout(3, 1, 0, 6));
        pasteFooter.setOpaque(false);
        bulkStatsBadge = new JLabel("Pasted: 0 | Matched: 0 | Missing: 0");
        bulkStatsBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        pasteFooter.add(bulkStatsBadge);

        JPanel btnRow1 = new JPanel(new GridLayout(1, 2, 6, 0));
        btnRow1.setOpaque(false);
        FlutterUI.FlutterButton clipPasteBtn = new FlutterUI.FlutterButton("Paste Clipboard", FlutterUI.SLATE_800, Color.WHITE);
        clipPasteBtn.addActionListener(e -> pasteClipboard());
        FlutterUI.FlutterButton clearBulkBtn = new FlutterUI.FlutterButton("Clear List", FlutterUI.ROSE_700, Color.WHITE);
        clearBulkBtn.addActionListener(e -> { bulkSbPasteArea.setText(""); processBulkSbList(); });
        btnRow1.add(clipPasteBtn);
        btnRow1.add(clearBulkBtn);
        pasteFooter.add(btnRow1);

        FlutterUI.FlutterButton processBulkBtn = new FlutterUI.FlutterButton("Fetch DMRs for Bulk List", FlutterUI.INDIGO_600, Color.WHITE);
        processBulkBtn.addActionListener(e -> processBulkSbList());
        pasteFooter.add(processBulkBtn);
        leftPasteCard.add(pasteFooter, BorderLayout.SOUTH);
        add(leftPasteCard, BorderLayout.WEST);

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setResizeWeight(0.64);
        centerSplit.setDividerSize(8);
        centerSplit.setBorder(null);
        centerSplit.setOpaque(false);

        FlutterUI.RoundedCard midTableCard = new FlutterUI.RoundedCard(Color.WHITE);
        midTableCard.setLayout(new BorderLayout(8, 8));
        midTableCard.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel midTopBar = new JPanel(new BorderLayout(8, 4));
        midTopBar.setOpaque(false);
        JLabel midTitle = new JLabel("2. BULK MATCHED DMR RESULTS (EXACT PASTED ORDER)");
        midTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        midTopBar.add(midTitle, BorderLayout.WEST);

        JPanel modeToggles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        modeToggles.setOpaque(false);
        bulkFlatModeChk = new JCheckBox("Flat DMR Rows", true);
        bulkFlatModeChk.setOpaque(false);
        bulkFlatModeChk.addActionListener(e -> processBulkSbList());
        bulkOnlyActiveChk = new JCheckBox("Only Active DMRs (> Rs. 0)", false);
        bulkOnlyActiveChk.setOpaque(false);
        bulkOnlyActiveChk.addActionListener(e -> processBulkSbList());
        modeToggles.add(bulkFlatModeChk);
        modeToggles.add(bulkOnlyActiveChk);
        midTopBar.add(modeToggles, BorderLayout.EAST);
        midTableCard.add(midTopBar, BorderLayout.NORTH);

        String[] bulkCols = {
            "SL", "Pasted SB A/c", "12-Digit CIF", "Farmer Name",
            "DMR Category", "DMR Account No", "01/04 Bal", "Cleared Date", "Today Balance", "Status"
        };
        bulkTableModel = new DefaultTableModel(bulkCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        bulkTable = FlutterUI.createStyledTable(bulkTableModel, 9);
        bulkTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && bulkTable.getSelectedRow() >= 0) {
                renderBulkRightCards(String.valueOf(bulkTable.getValueAt(bulkTable.getSelectedRow(), 2)));
            }
        });
        midTableCard.add(new JScrollPane(bulkTable), BorderLayout.CENTER);
        centerSplit.setLeftComponent(midTableCard);

        FlutterUI.RoundedCard rightCopyCard = new FlutterUI.RoundedCard(Color.WHITE);
        rightCopyCard.setLayout(new BorderLayout(8, 8));
        rightCopyCard.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel bulkActionBox = new JPanel(new GridLayout(4, 1, 0, 6));
        bulkActionBox.setOpaque(false);
        JLabel actHdr = new JLabel("3. 1-CLICK BULK COPY & EXCEL EXPORT");
        actHdr.setFont(new Font("Segoe UI", Font.BOLD, 13));
        actHdr.setForeground(FlutterUI.EMERALD_700);
        bulkActionBox.add(actHdr);

        FlutterUI.FlutterButton copyBulkTableBtn = new FlutterUI.FlutterButton("Copy All Bulk DMRs (Paste into Excel)", FlutterUI.BLUE_600, Color.WHITE);
        copyBulkTableBtn.addActionListener(e -> copyBulkResults(false));
        bulkActionBox.add(copyBulkTableBtn);

        FlutterUI.FlutterButton copyEligDmrOnlyBtn = new FlutterUI.FlutterButton("Copy ONLY Eligible DMR A/c + Today Bal", FlutterUI.INDIGO_600, Color.WHITE);
        copyEligDmrOnlyBtn.addActionListener(e -> copyBulkResults(true));
        bulkActionBox.add(copyEligDmrOnlyBtn);

        FlutterUI.FlutterButton exportBulkExcelBtn = new FlutterUI.FlutterButton("Export Pasted List to 6-Sheet Excel (.xls)", FlutterUI.EMERALD_700, Color.WHITE);
        exportBulkExcelBtn.addActionListener(e -> ctx.exportExcelList(bulkMatchedCifs, "Samayojan_00003_Bulk_Filtered_6Sheets.xls"));
        bulkActionBox.add(exportBulkExcelBtn);
        rightCopyCard.add(bulkActionBox, BorderLayout.NORTH);

        JPanel inspectorBox = new JPanel(new BorderLayout(6, 6));
        inspectorBox.setOpaque(false);
        bulkSelectedFarmerLbl = new JLabel("Click any row in the Bulk Table to copy individual DMRs:");
        bulkSelectedFarmerLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        inspectorBox.add(bulkSelectedFarmerLbl, BorderLayout.NORTH);

        bulkQuickCardsPanel = new FlutterUI.ScrollableCardPanel();
        bulkQuickCardsPanel.setLayout(new BoxLayout(bulkQuickCardsPanel, BoxLayout.Y_AXIS));
        bulkQuickCardsPanel.setBackground(Color.WHITE);
        JScrollPane cardScroll = new JScrollPane(bulkQuickCardsPanel);
        cardScroll.setBorder(null);
        cardScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        inspectorBox.add(cardScroll, BorderLayout.CENTER);

        rightCopyCard.add(inspectorBox, BorderLayout.CENTER);
        centerSplit.setRightComponent(rightCopyCard);
        add(centerSplit, BorderLayout.CENTER);

        ctx.addRefreshListener(this::processBulkSbList);
    }

    private void pasteClipboard() {
        try {
            String clip = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (clip != null) {
                bulkSbPasteArea.setText(bulkSbPasteArea.getText().trim().isEmpty() ? clip.trim() : bulkSbPasteArea.getText().trim() + "\n" + clip.trim());
                processBulkSbList();
            }
        } catch (Exception ignored) {}
    }

    public void processBulkSbList() {
        bulkTableModel.setRowCount(0);
        bulkMatchedCifs.clear();
        bulkFlatRows.clear();

        String raw = bulkSbPasteArea.getText().trim();
        if (raw.isEmpty()) {
            bulkStatsBadge.setText("Pasted: 0 | Matched: 0 | Missing: 0");
            return;
        }
        String[] tokens = raw.split("[\\r\\n\\t,; ]+");
        List<String> pastedList = new ArrayList<>();
        for (String t : tokens) if (!t.trim().isEmpty()) pastedList.add(t.trim());

        boolean onlyActive = bulkOnlyActiveChk.isSelected();
        boolean flatMode = bulkFlatModeChk.isSelected();
        int matched = 0, missing = 0, sl = 1;
        Set<String> seenCifs = new LinkedHashSet<>();

        for (String q : pastedList) {
            CIFMasterRecord match = ctx.resolveFarmer(q);
            if (match == null) {
                missing++;
                bulkFlatRows.add(new BulkDmrRow(q, "-", "NOT FOUND IN DB", "NOT FOUND", "-", 0, "NOT FOUND"));
                bulkTableModel.addRow(new Object[]{sl++, q, "-", "NOT FOUND IN DB", "-", "-", "-", "-", "-", "NOT FOUND"});
                continue;
            }
            matched++;
            if (seenCifs.add(match.cif)) bulkMatchedCifs.add(match);
            String primarySb = match.sb.isEmpty() ? q : match.sb.get(0).account;

            if (flatMode) {
                int added = 0;
                for (EnrichedAccount dmr : match.allDmr) {
                    if (onlyActive && dmr.balToday <= 0.0) continue;
                    String aprStr = dmr.existedOnApr1 ? SamayojanModels.AMT_FMT.format(dmr.balApr1) : ("New (" + SamayojanModels.formatDate(dmr.firstSeenDate) + ")");
                    String clrStr = dmr.clearedDate != null ? SamayojanModels.formatDate(dmr.clearedDate) : "-";
                    bulkFlatRows.add(new BulkDmrRow(primarySb, match.cif, match.name, dmr.category, dmr.account, dmr.balToday, match.finalStatus));
                    bulkTableModel.addRow(new Object[]{sl++, primarySb, match.cif, match.name, dmr.category, dmr.account, aprStr, clrStr, SamayojanModels.AMT_FMT.format(dmr.balToday), match.finalStatus});
                    added++;
                }
                if (added == 0 && !onlyActive) {
                    bulkFlatRows.add(new BulkDmrRow(primarySb, match.cif, match.name, "NO DMR LOAN", "-", 0, match.finalStatus));
                    bulkTableModel.addRow(new Object[]{sl++, primarySb, match.cif, match.name, "NO DMR LOAN", "-", "0.00", "-", "0.00", match.finalStatus});
                }
            } else {
                String joined = SamayojanModels.formatShortSummary(match.allDmr);
                double sum = 0;
                for (EnrichedAccount d : match.allDmr) sum += d.balToday;
                if (onlyActive && sum <= 0.0) continue;
                bulkFlatRows.add(new BulkDmrRow(primarySb, match.cif, match.name, "ALL DMRs", joined, sum, match.finalStatus));
                bulkTableModel.addRow(new Object[]{sl++, primarySb, match.cif, match.name, "ALL DMRs (" + match.allDmr.size() + ")", joined, "-", "-", SamayojanModels.AMT_FMT.format(sum), match.finalStatus});
            }
        }
        bulkStatsBadge.setText("Pasted: " + pastedList.size() + " | Matched: " + matched + " | Missing: " + missing);
        if (!bulkMatchedCifs.isEmpty()) renderBulkRightCards(bulkMatchedCifs.get(0).cif);
    }

    private void renderBulkRightCards(String cif) {
        CIFMasterRecord cm = ctx.cifLookupMap.get(cif);
        if (cm == null) return;
        bulkSelectedFarmerLbl.setText(cm.name + " | CIF: " + cm.cif + " (" + cm.finalStatus + ")");
        ctx.populateDmrCards(bulkQuickCardsPanel, cm, bulkOnlyActiveChk.isSelected());
    }

    private void copyBulkResults(boolean onlyEligible) {
        if (bulkFlatRows.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        if (!onlyEligible) sb.append("SB_ACCOUNT\tCIF_NUMBER\tFARMER_NAME\tDMR_CATEGORY\tDMR_ACCOUNT\tTODAY_BALANCE\tSTATUS\n");
        int count = 0;
        for (BulkDmrRow r : bulkFlatRows) {
            if (onlyEligible && !"ELIGIBLE".equals(r.status)) continue;
            if ("-".equals(r.dmrAccount)) continue;
            if (onlyEligible) sb.append(r.sbAccount).append("\t").append(r.dmrCategory).append("\t").append(r.dmrAccount).append("\t").append(SamayojanModels.AMT_FMT.format(r.todayBal)).append("\n");
            else sb.append(r.sbAccount).append("\t").append(r.cif).append("\t").append(r.farmerName).append("\t").append(r.dmrCategory).append("\t").append(r.dmrAccount).append("\t").append(SamayojanModels.AMT_FMT.format(r.todayBal)).append("\t").append(r.status).append("\n");
            count++;
        }
        ctx.copyToClipboard(sb.toString().trim(), count + " Bulk DMR Row(s)");
        JOptionPane.showMessageDialog(ctx.parentFrame, "Copied " + count + " DMR row(s) to Clipboard!", "Bulk Copy Ready", JOptionPane.INFORMATION_MESSAGE);
    }

    static class BulkDmrRow {
        String sbAccount, cif, farmerName, dmrCategory, dmrAccount, status;
        double todayBal;
        BulkDmrRow(String s, String c, String n, String cat, String acc, double bal, String st) {
            sbAccount = s; cif = c; farmerName = n; dmrCategory = cat; dmrAccount = acc; todayBal = bal; status = st;
        }
    }
}