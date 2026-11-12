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
    private JCheckBox bulkHideZeroChk, bulkFlatModeChk;
    private JTable bulkTable;
    private DefaultTableModel bulkTableModel;
    private FlutterUI.ScrollableCardPanel bulkQuickCardsPanel;
    private JLabel bulkSelectedFarmerLbl;

    private final List<CIFMasterRecord> bulkMatchedCifs = new ArrayList<>();
    private final List<BulkDmrRow> bulkFlatRows = new ArrayList<>();

    public BulkDmrScreen(final AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(14, 14));
        setOpaque(false);

        FlutterUI.RoundedCard leftPasteCard = new FlutterUI.RoundedCard(Color.WHITE);
        leftPasteCard.setLayout(new BorderLayout(10, 10));
        leftPasteCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        leftPasteCard.setPreferredSize(new Dimension(300, 100));

        FlutterUI.PastelGradientCard pasteHdr = new FlutterUI.PastelGradientCard(
            FlutterUI.PASTEL_SKY_1, FlutterUI.PASTEL_LILAC, "SB"
        );
        pasteHdr.setLayout(new GridLayout(2, 1, 0, 2));
        pasteHdr.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel pTitle = new JLabel("1. PASTE BULK SB LIST");
        pTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pTitle.setForeground(FlutterUI.SLATE_900);
        JLabel pSub = new JLabel("Paste Excel column (1 A/c per line)");
        pSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pSub.setForeground(FlutterUI.SLATE_700);
        pasteHdr.add(pTitle);
        pasteHdr.add(pSub);
        leftPasteCard.add(pasteHdr, BorderLayout.NORTH);

        bulkSbPasteArea = new JTextArea("500010000001\n500010000002\n500010000003\n500010000005");
        bulkSbPasteArea.setFont(new Font("Consolas", Font.BOLD, 13));
        bulkSbPasteArea.setBackground(FlutterUI.SLATE_50);
        bulkSbPasteArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane pasteScroll = new JScrollPane(bulkSbPasteArea);
        pasteScroll.setBorder(new LineBorder(FlutterUI.SLATE_200, 1, true));
        leftPasteCard.add(pasteScroll, BorderLayout.CENTER);

        JPanel pasteFooter = new JPanel(new GridLayout(3, 1, 0, 6));
        pasteFooter.setOpaque(false);
        bulkStatsBadge = new JLabel("Pasted: 0 | Matched: 0 | Missing: 0");
        bulkStatsBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        bulkStatsBadge.setForeground(FlutterUI.SLATE_800);
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
        centerSplit.setResizeWeight(0.63);
        centerSplit.setDividerSize(8);
        centerSplit.setBorder(null);
        centerSplit.setOpaque(false);

        FlutterUI.RoundedCard midTableCard = new FlutterUI.RoundedCard(Color.WHITE);
        midTableCard.setLayout(new BorderLayout(8, 10));
        midTableCard.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel midTopBar = new JPanel(new BorderLayout(8, 4));
        midTopBar.setOpaque(false);
        JLabel midTitle = new JLabel("2. BULK MATCHED DMR RESULTS (EXACT PASTED ORDER)");
        midTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        midTitle.setForeground(FlutterUI.SLATE_900);
        midTopBar.add(midTitle, BorderLayout.WEST);

        JPanel modeToggles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        modeToggles.setOpaque(false);
        bulkFlatModeChk = new JCheckBox("Flat DMR Rows", true);
        bulkFlatModeChk.setOpaque(false);
        bulkFlatModeChk.setFont(new Font("Segoe UI", Font.BOLD, 11));
        bulkFlatModeChk.addActionListener(e -> processBulkSbList());
        bulkHideZeroChk = new JCheckBox("Hide Exact 0.00 DMRs", false);
        bulkHideZeroChk.setOpaque(false);
        bulkHideZeroChk.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bulkHideZeroChk.addActionListener(e -> processBulkSbList());
        modeToggles.add(bulkFlatModeChk);
        modeToggles.add(bulkHideZeroChk);
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

        JScrollPane tableScroll = new JScrollPane(bulkTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);
        midTableCard.add(tableScroll, BorderLayout.CENTER);
        centerSplit.setLeftComponent(midTableCard);

        FlutterUI.RoundedCard rightCopyCard = new FlutterUI.RoundedCard(Color.WHITE);
        rightCopyCard.setLayout(new BorderLayout(10, 10));
        rightCopyCard.setBorder(new EmptyBorder(14, 14, 14, 14));

        FlutterUI.PastelGradientCard bulkActionBox = new FlutterUI.PastelGradientCard(
            FlutterUI.PASTEL_MINT_1, FlutterUI.PASTEL_SKY_1, "GO"
        );
        bulkActionBox.setLayout(new GridLayout(4, 1, 0, 6));
        bulkActionBox.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel actHdr = new JLabel("3. 1-CLICK BULK COPY & EXPORT");
        actHdr.setFont(new Font("Segoe UI", Font.BOLD, 12));
        actHdr.setForeground(FlutterUI.SLATE_900);
        bulkActionBox.add(actHdr);

        FlutterUI.FlutterButton copyBulkTableBtn = new FlutterUI.FlutterButton("Copy All Bulk DMRs (Separate Cells)", FlutterUI.BLUE_600, Color.WHITE);
        copyBulkTableBtn.addActionListener(e -> copyBulkResults(false));
        bulkActionBox.add(copyBulkTableBtn);

        FlutterUI.FlutterButton copyEligDmrOnlyBtn = new FlutterUI.FlutterButton("Copy ONLY Eligible DMR A/c + Bal", FlutterUI.INDIGO_600, Color.WHITE);
        copyEligDmrOnlyBtn.addActionListener(e -> copyBulkResults(true));
        bulkActionBox.add(copyEligDmrOnlyBtn);

        FlutterUI.FlutterButton exportBulkExcelBtn = new FlutterUI.FlutterButton("Export Pasted List (6-Sheet .xls)", FlutterUI.EMERALD_700, Color.WHITE);
        exportBulkExcelBtn.addActionListener(e -> ctx.exportExcelList(bulkMatchedCifs, "Samayojan_00003_Bulk_Filtered_6Sheets.xls"));
        bulkActionBox.add(exportBulkExcelBtn);
        rightCopyCard.add(bulkActionBox, BorderLayout.NORTH);

        JPanel inspectorBox = new JPanel(new BorderLayout(6, 6));
        inspectorBox.setOpaque(false);
        bulkSelectedFarmerLbl = new JLabel("Select a row to copy individual DMR accounts:");
        bulkSelectedFarmerLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        bulkSelectedFarmerLbl.setForeground(FlutterUI.SLATE_800);
        inspectorBox.add(bulkSelectedFarmerLbl, BorderLayout.NORTH);

        bulkQuickCardsPanel = new FlutterUI.ScrollableCardPanel();
        bulkQuickCardsPanel.setLayout(new BoxLayout(bulkQuickCardsPanel, BoxLayout.Y_AXIS));
        bulkQuickCardsPanel.setBackground(Color.WHITE);
        JScrollPane cardScroll = new JScrollPane(bulkQuickCardsPanel);
        cardScroll.setBorder(null);
        cardScroll.getViewport().setBackground(Color.WHITE);
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

        boolean hideZero = bulkHideZeroChk.isSelected();
        boolean flatMode = bulkFlatModeChk.isSelected();
        int matched = 0, missing = 0, sl = 1;
        Set<String> seenCifs = new LinkedHashSet<>();

        for (String q : pastedList) {
            CIFMasterRecord match = ctx.resolveFarmer(q);
            if (match == null) {
                missing++;
                bulkFlatRows.add(new BulkDmrRow(q, "-", "NOT FOUND IN DB", "NOT FOUND", "-", "0.00", "-", "NOT FOUND"));
                bulkTableModel.addRow(new Object[]{sl++, q, "-", "NOT FOUND IN DB", "-", "-", "-", "-", "-", "NOT FOUND"});
                continue;
            }
            matched++;
            if (seenCifs.add(match.cif)) bulkMatchedCifs.add(match);
            String primarySb = match.sb.isEmpty() ? q : match.sb.get(0).account;

            if (flatMode) {
                int added = 0;
                for (EnrichedAccount dmr : match.allDmr) {
                    if (hideZero && dmr.drToday == 0.0 && dmr.crToday == 0.0) continue;
                    String aprStr = dmr.existedOnApr1
                        ? SamayojanModels.formatDrCr(dmr.drApr1, dmr.crApr1, false)
                        : ("New (" + SamayojanModels.formatDate(dmr.firstSeenDate) + ")");
                    String clrStr = dmr.clearedDate != null ? SamayojanModels.formatDate(dmr.clearedDate) : "-";
                    String todayStr = SamayojanModels.formatDrCr(dmr.drToday, dmr.crToday, false);
                    String todayNum = (dmr.crToday > 0 && dmr.drToday == 0) ? SamayojanModels.AMT_FMT.format(dmr.crToday) : SamayojanModels.AMT_FMT.format(dmr.drToday);
                    String drCrTag = (dmr.crToday > 0 && dmr.drToday == 0) ? "CR" : (dmr.drToday > 0 ? "DR" : "-");

                    bulkFlatRows.add(new BulkDmrRow(primarySb, match.cif, match.name, dmr.category, dmr.account, todayNum, drCrTag, match.finalStatus));
                    bulkTableModel.addRow(new Object[]{sl++, primarySb, match.cif, match.name, dmr.category, dmr.account, aprStr, clrStr, todayStr, match.finalStatus});
                    added++;
                }
                if (added == 0 && !hideZero) {
                    bulkFlatRows.add(new BulkDmrRow(primarySb, match.cif, match.name, "NO DMR LOAN", "-", "0.00", "-", match.finalStatus));
                    bulkTableModel.addRow(new Object[]{sl++, primarySb, match.cif, match.name, "NO DMR LOAN", "-", "0.00", "-", "0.00", match.finalStatus});
                }
            } else {
                String joinedAccts = SamayojanModels.joinAccountsOnly(match.allDmr);
                String joinedBals = SamayojanModels.joinTodayBalancesOnly(match.allDmr);
                if (hideZero && match.allDmr.isEmpty()) continue;
                bulkFlatRows.add(new BulkDmrRow(primarySb, match.cif, match.name, "ALL DMRs", joinedAccts, joinedBals, "-", match.finalStatus));
                bulkTableModel.addRow(new Object[]{sl++, primarySb, match.cif, match.name, "ALL DMRs (" + match.allDmr.size() + ")", joinedAccts, "-", "-", joinedBals, match.finalStatus});
            }
        }
        bulkStatsBadge.setText("Pasted: " + pastedList.size() + " | Matched: " + matched + " | Missing: " + missing);
        if (!bulkMatchedCifs.isEmpty()) renderBulkRightCards(bulkMatchedCifs.get(0).cif);
    }

    private void renderBulkRightCards(String cif) {
        CIFMasterRecord cm = ctx.cifLookupMap.get(cif);
        if (cm == null) return;
        bulkSelectedFarmerLbl.setText(cm.name + " | CIF: " + cm.cif + " (" + cm.finalStatus + ")");
        ctx.populateDmrCards(bulkQuickCardsPanel, cm, bulkHideZeroChk.isSelected());
    }

    private void copyBulkResults(boolean onlyEligible) {
        if (bulkFlatRows.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        if (!onlyEligible) {
            sb.append("SB_ACCOUNT\tCIF_NUMBER\tFARMER_NAME\tDMR_CATEGORY\tDMR_ACCOUNT\tTODAY_BALANCE\tDR_CR\tSTATUS\n");
        }
        int count = 0;
        for (BulkDmrRow r : bulkFlatRows) {
            if (onlyEligible && !"ELIGIBLE".equals(r.status)) continue;
            if ("-".equals(r.dmrAccount)) continue;
            if (onlyEligible) {
                sb.append(r.sbAccount).append("\t")
                  .append(r.dmrCategory).append("\t")
                  .append(r.dmrAccount).append("\t")
                  .append(r.todayBalNum).append("\t")
                  .append(r.drCrTag).append("\n");
            } else {
                sb.append(r.sbAccount).append("\t")
                  .append(r.cif).append("\t")
                  .append(r.farmerName).append("\t")
                  .append(r.dmrCategory).append("\t")
                  .append(r.dmrAccount).append("\t")
                  .append(r.todayBalNum).append("\t")
                  .append(r.drCrTag).append("\t")
                  .append(r.status).append("\n");
            }
            count++;
        }
        ctx.copyToClipboard(sb.toString().trim(), count + " Bulk DMR Row(s)");
        JOptionPane.showMessageDialog(ctx.parentFrame, "Copied " + count + " DMR row(s) to Clipboard with separate columns!", "Bulk Copy Ready", JOptionPane.INFORMATION_MESSAGE);
    }

    static class BulkDmrRow {
        String sbAccount, cif, farmerName, dmrCategory, dmrAccount, todayBalNum, drCrTag, status;
        BulkDmrRow(String s, String c, String n, String cat, String acc, String bal, String drCr, String st) {
            sbAccount = s; cif = c; farmerName = n; dmrCategory = cat; dmrAccount = acc;
            todayBalNum = bal; drCrTag = drCr; status = st;
        }
    }
}