import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class Single360Screen extends JPanel {
    private AppContext ctx;
    private JTextField singleSearchField;
    private JCheckBox singleHideZeroChk;
    private JLabel singleFarmerTitleLbl, singleStatusBadgeLbl;
    private FlutterUI.ScrollableCardPanel singleDmrCardsPanel;
    private DefaultTableModel timelineTableModel;
    private CIFMasterRecord currentSingleCif = null;

    public Single360Screen(final AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(14, 14));
        setOpaque(false);

        FlutterUI.PastelGradientCard searchCard = new FlutterUI.PastelGradientCard(
            FlutterUI.PASTEL_SKY_1, FlutterUI.PASTEL_MINT_1, "360"
        );
        searchCard.setLayout(new BorderLayout(12, 0));
        searchCard.setBorder(new EmptyBorder(14, 18, 14, 58));

        JLabel sLbl = new JLabel("ENTER SAVING (SB) A/C, CIF, OR LOAN A/C:");
        sLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sLbl.setForeground(FlutterUI.SLATE_900);
        searchCard.add(sLbl, BorderLayout.WEST);

        singleSearchField = new JTextField();
        singleSearchField.setFont(new Font("Consolas", Font.BOLD, 16));
        singleSearchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FlutterUI.SLATE_200, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        singleSearchField.addActionListener(e -> searchSingle());
        searchCard.add(singleSearchField, BorderLayout.CENTER);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setOpaque(false);
        singleHideZeroChk = new JCheckBox("Hide Exact 0.00 DMRs", false);
        singleHideZeroChk.setOpaque(false);
        singleHideZeroChk.setFont(new Font("Segoe UI", Font.BOLD, 11));
        singleHideZeroChk.addActionListener(e -> { if (currentSingleCif != null) renderFarmer(currentSingleCif.cif); });
        rightBtns.add(singleHideZeroChk);

        FlutterUI.FlutterButton fetchBtn = new FlutterUI.FlutterButton("Fetch Farmer 360°", FlutterUI.SLATE_900, Color.WHITE);
        fetchBtn.addActionListener(e -> searchSingle());
        rightBtns.add(fetchBtn);

        FlutterUI.FlutterButton copyAllBtn = new FlutterUI.FlutterButton("Copy Farmer's DMRs", FlutterUI.EMERALD_700, Color.WHITE);
        copyAllBtn.addActionListener(e -> copyFarmerDmrs());
        rightBtns.add(copyAllBtn);

        searchCard.add(rightBtns, BorderLayout.EAST);
        add(searchCard, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.52);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setOpaque(false);

        FlutterUI.RoundedCard leftCardsWrap = new FlutterUI.RoundedCard(Color.WHITE);
        leftCardsWrap.setLayout(new BorderLayout(8, 10));
        leftCardsWrap.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel banner = new JPanel(new BorderLayout(8, 4));
        banner.setBackground(FlutterUI.SLATE_50);
        banner.setBorder(new EmptyBorder(10, 12, 10, 12));
        singleFarmerTitleLbl = new JLabel("Select a farmer or enter an SB Account above.");
        singleFarmerTitleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        singleFarmerTitleLbl.setForeground(FlutterUI.SLATE_900);
        singleStatusBadgeLbl = new JLabel("");
        singleStatusBadgeLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        banner.add(singleFarmerTitleLbl, BorderLayout.CENTER);
        banner.add(singleStatusBadgeLbl, BorderLayout.EAST);
        leftCardsWrap.add(banner, BorderLayout.NORTH);

        singleDmrCardsPanel = new FlutterUI.ScrollableCardPanel();
        singleDmrCardsPanel.setLayout(new BoxLayout(singleDmrCardsPanel, BoxLayout.Y_AXIS));
        singleDmrCardsPanel.setBackground(Color.WHITE);
        JScrollPane leftScroll = new JScrollPane(singleDmrCardsPanel);
        leftScroll.setBorder(null);
        leftScroll.getViewport().setBackground(Color.WHITE);
        leftScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftCardsWrap.add(leftScroll, BorderLayout.CENTER);
        split.setLeftComponent(leftCardsWrap);

        FlutterUI.RoundedCard rightTimelineCard = new FlutterUI.RoundedCard(Color.WHITE);
        rightTimelineCard.setLayout(new BorderLayout(8, 10));
        rightTimelineCard.setBorder(new EmptyBorder(16, 18, 16, 18));
        JLabel tlHdr = new JLabel("COMPLETE MULTI-DATE BALANCE TIMELINE (01/04/2026 -> TODAY)");
        tlHdr.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tlHdr.setForeground(FlutterUI.SLATE_900);
        rightTimelineCard.add(tlHdr, BorderLayout.NORTH);

        String[] tlCols = {"Report Date", "Category", "Account Number", "DR Balance", "CR Balance", "Eligibility / Margin Note"};
        timelineTableModel = new DefaultTableModel(tlCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tlTable = FlutterUI.createStyledTable(timelineTableModel, -1);
        JScrollPane tlScroll = new JScrollPane(tlTable);
        tlScroll.setBorder(BorderFactory.createEmptyBorder());
        tlScroll.getViewport().setBackground(Color.WHITE);
        rightTimelineCard.add(tlScroll, BorderLayout.CENTER);
        split.setRightComponent(rightTimelineCard);
        add(split, BorderLayout.CENTER);

        ctx.addRefreshListener(() -> {
            if (currentSingleCif != null) renderFarmer(currentSingleCif.cif);
            else if (!ctx.cifMasterList.isEmpty()) renderFarmer(ctx.cifMasterList.get(0).cif);
        });
    }

    private void searchSingle() {
        CIFMasterRecord m = ctx.resolveFarmer(singleSearchField.getText().trim());
        if (m == null) JOptionPane.showMessageDialog(ctx.parentFrame, "Account or CIF not found.", "Not Found", JOptionPane.WARNING_MESSAGE);
        else renderFarmer(m.cif);
    }

    public void renderFarmer(String cif) {
        CIFMasterRecord cm = ctx.cifLookupMap.get(cif);
        if (cm == null) return;
        currentSingleCif = cm;
        singleFarmerTitleLbl.setText(cm.name + "  |  CIF: " + cm.cif);
        singleStatusBadgeLbl.setText("  " + cm.finalStatus + "  ");
        singleStatusBadgeLbl.setOpaque(true);
        if ("ELIGIBLE".equals(cm.finalStatus)) { singleStatusBadgeLbl.setBackground(FlutterUI.PASTEL_MINT_1); singleStatusBadgeLbl.setForeground(FlutterUI.EMERALD_700); }
        else if ("NOT ELIGIBLE".equals(cm.finalStatus)) { singleStatusBadgeLbl.setBackground(FlutterUI.PASTEL_ROSE_1); singleStatusBadgeLbl.setForeground(FlutterUI.ROSE_700); }
        else { singleStatusBadgeLbl.setBackground(FlutterUI.PASTEL_AMBER); singleStatusBadgeLbl.setForeground(FlutterUI.AMBER_700); }

        ctx.populateDmrCards(singleDmrCardsPanel, cm, singleHideZeroChk.isSelected());

        timelineTableModel.setRowCount(0);
        List<String> sortedDates = new ArrayList<>(ctx.db.loadedDates);
        Collections.sort(sortedDates);
        List<EnrichedAccount> allLinked = new ArrayList<>(cm.sb);
        allLinked.addAll(cm.allDmr);

        for (EnrichedAccount ea : allLinked) {
            AccountRecord rawRec = ctx.db.accounts.get(ea.account);
            if (rawRec == null) continue;
            for (String dKey : sortedDates) {
                BalanceSnapshot snap = rawRec.history.get(dKey);
                if (snap == null) {
                    timelineTableModel.addRow(new Object[]{SamayojanModels.formatDate(dKey), ea.category, ea.account, "Absent", "Absent", "Not in report"});
                } else {
                    String note;
                    if ("SB".equals(ea.category)) {
                        note = "Savings Account";
                    } else if (snap.cr > 0 && snap.dr == 0) {
                        note = "ELIGIBLE (Credit Balance in DMR)";
                    } else if (snap.dr <= SamayojanModels.MARGIN_RS) {
                        note = "CLEARED (<= Rs. 10 DR Margin)";
                    } else {
                        note = "DR Dues Active (> Rs. 10)";
                    }
                    timelineTableModel.addRow(new Object[]{
                        SamayojanModels.formatDate(dKey), ea.category, ea.account,
                        SamayojanModels.AMT_FMT.format(snap.dr),
                        SamayojanModels.AMT_FMT.format(snap.cr),
                        note
                    });
                }
            }
        }
    }

    private void copyFarmerDmrs() {
        if (currentSingleCif == null) return;
        StringBuilder sb = new StringBuilder();
        for (EnrichedAccount dmr : currentSingleCif.allDmr) {
            if (singleHideZeroChk.isSelected() && dmr.drToday == 0.0 && dmr.crToday == 0.0) continue;
            String balNum = (dmr.crToday > 0 && dmr.drToday == 0) ? SamayojanModels.AMT_FMT.format(dmr.crToday) : SamayojanModels.AMT_FMT.format(dmr.drToday);
            String drCr = (dmr.crToday > 0 && dmr.drToday == 0) ? "CR" : (dmr.drToday > 0 ? "DR" : "-");
            sb.append(dmr.category).append("\t").append(dmr.account).append("\t").append(balNum).append("\t").append(drCr).append("\n");
        }
        if (sb.length() > 0) ctx.copyToClipboard(sb.toString().trim(), "DMRs for " + currentSingleCif.name);
    }
}