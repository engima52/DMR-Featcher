import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.*;

public class SyncSettingsScreen extends JPanel {
    private final AppContext ctx;
    private final JTextField pathField, fileFilterField;
    private final JTextArea syncLogArea;

    public SyncSettingsScreen(final AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(12, 12));
        setOpaque(false);

        FlutterUI.RoundedCard settingsCard = new FlutterUI.RoundedCard(Color.WHITE);
        settingsCard.setLayout(new GridBagLayout());
        settingsCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel hdr = new JLabel("1-CLICK AUTO-SYNC & STANDALONE DATABASE CONFIGURATION");
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        settingsCard.add(hdr, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.1;
        settingsCard.add(new JLabel("Reports Root Folder (reports/YYYYMMDD/):"), gbc);

        pathField = new JTextField(ctx.config.getProperty("reportsPath", ""), 32);
        pathField.setFont(new Font("Consolas", Font.PLAIN, 13));
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.75;
        settingsCard.add(pathField, gbc);

        FlutterUI.FlutterButton browseBtn = new FlutterUI.FlutterButton("Browse Folder...", FlutterUI.SLATE_800, Color.WHITE);
        browseBtn.addActionListener(e -> chooseFolder());
        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.15;
        settingsCard.add(browseBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        settingsCard.add(new JLabel("Report Filename Filter (* = Any .gz/.txt, or GL7043):"), gbc);

        fileFilterField = new JTextField(ctx.config.getProperty("fileFilter", ReportParser.DEFAULT_FILE_FILTER), 15);
        fileFilterField.setFont(new Font("Consolas", Font.BOLD, 13));
        gbc.gridx = 1; gbc.gridy = 2;
        settingsCard.add(fileFilterField, gbc);

        FlutterUI.FlutterButton saveCfgBtn = new FlutterUI.FlutterButton("Save Filter & Sync Now", FlutterUI.BLUE_600, Color.WHITE);
        saveCfgBtn.addActionListener(e -> {
            ctx.saveConfig(pathField.getText().trim(), fileFilterField.getText().trim());
            ctx.runIncrementalSync(false);
        });
        gbc.gridx = 2; gbc.gridy = 2;
        settingsCard.add(saveCfgBtn, gbc);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        actionRow.setOpaque(false);

        FlutterUI.FlutterButton testGenBtn = new FlutterUI.FlutterButton("Generate Home Test Data (20260401 -> 20261110)", FlutterUI.AMBER_700, Color.WHITE);
        testGenBtn.addActionListener(e -> generateHomeTestData());
        actionRow.add(testGenBtn);

        FlutterUI.FlutterButton resetBtn = new FlutterUI.FlutterButton("Reset Local Database (samayojan_local.db)", FlutterUI.ROSE_700, Color.WHITE);
        resetBtn.addActionListener(e -> resetDb());
        actionRow.add(resetBtn);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3;
        settingsCard.add(actionRow, gbc);
        add(settingsCard, BorderLayout.NORTH);

        FlutterUI.RoundedCard logCard = new FlutterUI.RoundedCard(Color.WHITE);
        logCard.setLayout(new BorderLayout(8, 8));
        logCard.setBorder(new EmptyBorder(14, 16, 14, 16));
        logCard.add(new JLabel("SYNC & DATABASE ACTIVITY LOG"), BorderLayout.NORTH);

        syncLogArea = new JTextArea("Standalone Local Database file: " + AppContext.DB_FILE.getAbsolutePath() + "\n");
        syncLogArea.setEditable(false);
        syncLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logCard.add(new JScrollPane(syncLogArea), BorderLayout.CENTER);
        add(logCard, BorderLayout.CENTER);

        ctx.addLogListener(syncLogArea::append);
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser(pathField.getText().trim().isEmpty() ? "." : pathField.getText().trim());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(ctx.parentFrame) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            ctx.saveConfig(pathField.getText().trim(), fileFilterField.getText().trim());
            ctx.runIncrementalSync(false);
        }
    }

    public void generateHomeTestData() {
        try {
            Path demoRoot = Paths.get("reports").toAbsolutePath();
            pathField.setText(demoRoot.toString());
            ctx.saveConfig(demoRoot.toString(), fileFilterField.getText().trim());
            int stage = ReportParser.generateDemoFiles(demoRoot);
            if (stage == 2) JOptionPane.showMessageDialog(ctx.parentFrame, "Created new folder 'reports/20261110'! Syncing delta now...", "Next Day Simulation", JOptionPane.INFORMATION_MESSAGE);
            ctx.runIncrementalSync(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(ctx.parentFrame, "Demo Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetDb() {
        if (JOptionPane.showConfirmDialog(ctx.parentFrame, "Clear local database (samayojan_local.db)?", "Reset DB", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            ctx.db = new LocalDatabase();
            if (AppContext.DB_FILE.exists()) AppContext.DB_FILE.delete();
            ctx.recomputeAndNotify();
            ctx.setStatus("Local Database reset.");
        }
    }
}
