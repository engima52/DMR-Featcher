import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class AppContext {
    public static final File DB_FILE = new File("samayojan_local.db");
    public static final File CONFIG_FILE = new File("samayojan_config.properties");

    public final JFrame parentFrame;
    public LocalDatabase db = new LocalDatabase();
    public final Properties config = new Properties();

    public final List<CIFMasterRecord> cifMasterList = new ArrayList<>();
    public final Map<String, CIFMasterRecord> cifLookupMap = new HashMap<>();
    public final Map<String, CIFMasterRecord> acctToCifMap = new HashMap<>();

    public boolean missingAsZero = true;
    private final List<Runnable> refreshListeners = new ArrayList<>();
    private final List<Consumer<String>> logListeners = new ArrayList<>();
    private Consumer<String> statusUpdater;
    private Consumer<String> jumpTo360Handler;

    public AppContext(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        loadConfig();
    }

    public void setStatusUpdater(Consumer<String> updater) { this.statusUpdater = updater; }
    public void setJumpTo360Handler(Consumer<String> handler) { this.jumpTo360Handler = handler; }
    public void addRefreshListener(Runnable r) { refreshListeners.add(r); }
    public void addLogListener(Consumer<String> l) { logListeners.add(l); }

    public void setStatus(String msg) { if (statusUpdater != null) statusUpdater.accept(msg); }
    public void appendLog(String msg) { for (Consumer<String> l : logListeners) l.accept(msg); }
    public void openIn360Screen(String cif) { if (jumpTo360Handler != null) jumpTo360Handler.accept(cif); }

    public void recomputeAndNotify() {
        EligibilityEngine.recompute(db, missingAsZero, cifMasterList, cifLookupMap, acctToCifMap);
        for (Runnable r : refreshListeners) r.run();
    }

    public CIFMasterRecord resolveFarmer(String q) {
        if (q == null || q.isEmpty()) return null;
        CIFMasterRecord m = acctToCifMap.get(q);
        if (m != null) return m;
        m = cifLookupMap.get(q);
        if (m != null) return m;
        for (CIFMasterRecord cm : cifMasterList) {
            if (cm.cif.endsWith(q)) return cm;
            for (EnrichedAccount sb : cm.sb) if (sb.account.endsWith(q) || sb.account.equals(q)) return cm;
            for (EnrichedAccount dmr : cm.allDmr) if (dmr.account.endsWith(q) || dmr.account.equals(q)) return cm;
        }
        return null;
    }

    public void copyToClipboard(String val, String label) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(val), null);
        setStatus("Copied to Clipboard: " + label + " -> [" + val + "]");
    }

    public void populateDmrCards(JPanel container, final CIFMasterRecord cm, boolean hideZero) {
        container.removeAll();
        FlutterUI.RoundedCard reasonBox = new FlutterUI.RoundedCard(new Color(255, 251, 235));
        reasonBox.setLayout(new BorderLayout(8, 4));
        reasonBox.setBorder(new EmptyBorder(8, 10, 8, 10));

        JTextArea rArea = new JTextArea("Audit Remark: " + cm.finalReason);
        rArea.setEditable(false);
        rArea.setLineWrap(true);
        rArea.setWrapStyleWord(true);
        rArea.setOpaque(false);
        rArea.setFont(new Font("Segoe UI", Font.BOLD, 11));
        rArea.setForeground(FlutterUI.SLATE_900);
        reasonBox.add(rArea, BorderLayout.CENTER);

        FlutterUI.FlutterButton copyCifBtn = new FlutterUI.FlutterButton("Copy CIF", FlutterUI.SLATE_700, Color.WHITE);
        copyCifBtn.addActionListener(e -> copyToClipboard(cm.cif, "CIF " + cm.cif));
        JPanel cifWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        cifWrap.setOpaque(false);
        cifWrap.add(copyCifBtn);
        reasonBox.add(cifWrap, BorderLayout.EAST);

        container.add(reasonBox);
        container.add(Box.createVerticalStrut(8));

        for (EnrichedAccount sbAcc : cm.sb) {
            container.add(buildSingleAccountCard("SAVINGS (SBCHQ)", sbAcc, true));
            container.add(Box.createVerticalStrut(6));
        }
        for (EnrichedAccount dmr : cm.allDmr) {
            if (hideZero && dmr.balToday <= 0.0) continue;
            container.add(buildSingleAccountCard(dmr.category, dmr, false));
            container.add(Box.createVerticalStrut(6));
        }
        container.revalidate();
        container.repaint();
    }

    private JPanel buildSingleAccountCard(final String catTitle, final EnrichedAccount acc, boolean isSb) {
        FlutterUI.RoundedCard card = new FlutterUI.RoundedCard(isSb ? new Color(240, 249, 255) : FlutterUI.SLATE_50);
        card.setLayout(new BorderLayout(6, 6));
        card.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel topRow = new JPanel(new BorderLayout(6, 0));
        topRow.setOpaque(false);
        JLabel leftTitle = new JLabel(catTitle + " :  " + acc.account);
        leftTitle.setFont(new Font("Consolas", Font.BOLD, 14));
        leftTitle.setForeground(FlutterUI.SLATE_900);
        topRow.add(leftTitle, BorderLayout.WEST);

        JLabel balLbl = new JLabel("Today: Rs. " + SamayojanModels.AMT_FMT.format(acc.balToday));
        balLbl.setFont(new Font("Consolas", Font.BOLD, 14));
        balLbl.setForeground((!isSb && acc.balToday > SamayojanModels.MARGIN_RS) ? FlutterUI.ROSE_700 : FlutterUI.EMERALD_700);
        topRow.add(balLbl, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        JPanel botRow = new JPanel(new BorderLayout(6, 0));
        botRow.setOpaque(false);
        String aprInfo = acc.existedOnApr1
            ? ("01/04 Bal: Rs." + SamayojanModels.AMT_FMT.format(acc.balApr1))
            : ("New (" + SamayojanModels.formatDate(acc.firstSeenDate) + ")");
        String clrInfo = acc.clearedDate != null ? (" | Cleared: " + SamayojanModels.formatDate(acc.clearedDate)) : "";
        JLabel histLbl = new JLabel(aprInfo + clrInfo);
        histLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        histLbl.setForeground(FlutterUI.SLATE_700);
        botRow.add(histLbl, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setOpaque(false);
        FlutterUI.FlutterButton cpAcc = new FlutterUI.FlutterButton("Copy A/c: " + acc.account, FlutterUI.SLATE_800, Color.WHITE);
        cpAcc.addActionListener(e -> copyToClipboard(acc.account, catTitle + " A/c"));
        FlutterUI.FlutterButton cpBal = new FlutterUI.FlutterButton("Copy Bal", FlutterUI.BLUE_600, Color.WHITE);
        cpBal.addActionListener(e -> copyToClipboard(SamayojanModels.AMT_FMT.format(acc.balToday), "Balance"));
        btns.add(cpAcc);
        btns.add(cpBal);
        botRow.add(btns, BorderLayout.EAST);

        card.add(botRow, BorderLayout.SOUTH);
        return card;
    }

    public void runIncrementalSync(final boolean silent) {
        final String rootPathStr = config.getProperty("reportsPath", "").trim();
        final String filterPattern = config.getProperty("fileFilter", ReportParser.DEFAULT_FILE_FILTER).trim();
        if (rootPathStr.isEmpty() || !Files.exists(Paths.get(rootPathStr))) {
            if (!silent) JOptionPane.showMessageDialog(parentFrame, "Click 'Generate Home Test Data' or set your 'reports' folder in '4. Sync & DB Center'.", "Folder Not Set", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setStatus("Scanning reports/YYYYMMDD/ for new un-imported dates...");
        new Thread(() -> {
            try {
                final Set<String> newDates = ReportParser.syncNewDatesFromFolder(Paths.get(rootPathStr), filterPattern, db);
                if (!newDates.isEmpty()) saveDatabase();
                SwingUtilities.invokeLater(() -> {
                    recomputeAndNotify();
                    if (newDates.isEmpty()) {
                        setStatus("Local DB is 100% up-to-date (" + db.loadedDates.size() + " dates).");
                        if (!silent) JOptionPane.showMessageDialog(parentFrame, "All YYYYMMDD folders are already synced!", "Up-To-Date", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        appendLog("Synced new dates: " + newDates + "\n");
                        setStatus("1-Click Sync Complete! Imported " + newDates.size() + " new date(s): " + newDates);
                        if (!silent) JOptionPane.showMessageDialog(parentFrame, "Newly Imported Date(s): " + newDates + "\nTotal Farmers: " + cifMasterList.size(), "Sync Complete", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> setStatus("Sync Error: " + ex.getMessage()));
            }
        }).start();
    }

    public void exportExcelList(List<CIFMasterRecord> listToExport, String defaultFileName) {
        if (listToExport.isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame, "No records to export.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser(".");
        chooser.setSelectedFile(new File(defaultFileName));
        if (chooser.showSaveDialog(parentFrame) != JFileChooser.APPROVE_OPTION) return;
        try {
            ExcelExporter.exportSixSheets(chooser.getSelectedFile(), listToExport);
            JOptionPane.showMessageDialog(parentFrame, "Exported 6-Sheet Styled Excel (" + listToExport.size() + " CIFs):\n" + chooser.getSelectedFile().getAbsolutePath(), "Export Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parentFrame, "Export Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadDatabaseOnStartup() {
        if (DB_FILE.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DB_FILE))) {
                db = (LocalDatabase) ois.readObject();
                recomputeAndNotify();
            } catch (Throwable e) {
                db = new LocalDatabase();
                DB_FILE.delete();
            }
        }
        runIncrementalSync(true);
    }

    public void saveDatabase() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DB_FILE))) {
            oos.writeObject(db);
        } catch (Exception ignored) {}
    }

    public void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) { config.load(fis); } catch (Exception ignored) {}
        }
    }

    public void saveConfig(String path, String filter) {
        config.setProperty("reportsPath", path);
        config.setProperty("fileFilter", filter);
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) { config.store(fos, "DCCB 00003 Settings"); } catch (Exception ignored) {}
    }
}
