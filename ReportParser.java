import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ReportParser {

    public static final String DEFAULT_FILE_FILTER = "*";

    public static Set<String> syncNewDatesFromFolder(Path rootDir, String filterPattern, LocalDatabase db) throws IOException {
        String cleanFilter = filterPattern == null ? "*" : filterPattern.trim().toUpperCase();
        List<Path> newReportFiles = new ArrayList<>();
        Set<String> newDatesFound = new TreeSet<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(rootDir)) {
            for (Path subDir : stream) {
                if (!Files.isDirectory(subDir)) continue;
                String folderDate = subDir.getFileName().toString();
                if (!folderDate.matches("^202[6-9]\\d{4}$") || folderDate.compareTo("20260401") < 0) {
                    continue;
                }
                if (db.loadedDates.contains(folderDate)) {
                    continue;
                }

                try (DirectoryStream<Path> fileStream = Files.newDirectoryStream(subDir)) {
                    for (Path f : fileStream) {
                        if (!Files.isRegularFile(f)) continue;
                        String fName = f.getFileName().toString().toUpperCase();
                        boolean matches = cleanFilter.isEmpty() || cleanFilter.equals("*") || fName.contains(cleanFilter);
                        if (matches && (fName.endsWith(".GZ") || fName.endsWith(".TXT") || !fName.contains("."))) {
                            newReportFiles.add(f);
                            newDatesFound.add(folderDate);
                        }
                    }
                }
            }
        }

        for (Path file : newReportFiles) {
            String dateKey = file.getParent().getFileName().toString();
            parseReportFileIntoDb(file, dateKey, db);
        }

        return newDatesFound;
    }

    public static void parseReportFileIntoDb(Path file, String dateKey, LocalDatabase db) throws IOException {
        db.loadedDates.add(dateKey);

        byte[] rawBytes = Files.readAllBytes(file);
        InputStream in = new ByteArrayInputStream(rawBytes);
        if (rawBytes.length >= 2 && (rawBytes[0] & 0xFF) == 0x1f && (rawBytes[1] & 0xFF) == 0x8b) {
            in = new GZIPInputStream(in);
        }

        Pattern rowPattern = Pattern.compile("^\\d+\\s+(\\d{8,14})\\s+(\\d{9,16})\\s+(.+)$");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            String currentCategory = "OTHER";

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("---") || trimmed.startsWith("===")) continue;

                currentCategory = detectCategory(trimmed, currentCategory);

                String normalized = trimmed.replace('|', ' ');
                Matcher m = rowPattern.matcher(normalized);
                if (!m.find()) continue;

                String cif = String.format("%12s", m.group(1).trim()).replace(' ', '0');
                String acct = m.group(2).trim();
                String rest = m.group(3).trim();

                String[] tokens = rest.split("\\s+");
                StringBuilder nameBuilder = new StringBuilder();
                List<Double> numCols = new ArrayList<>();
                List<String> rawNumTokens = new ArrayList<>();
                boolean hitNumbers = false;

                for (String tok : tokens) {
                    boolean isNum = tok.matches("^-?[\\d,]+\\.\\d{2}(DR|CR|dr|cr|-)?$") || tok.matches("^-?[\\d,]+$");
                    if (!hitNumbers && !isNum) {
                        if (nameBuilder.length() > 0) nameBuilder.append(" ");
                        nameBuilder.append(tok);
                    } else {
                        hitNumbers = true;
                        if (isNum) {
                            numCols.add(parseAmount(tok));
                            rawNumTokens.add(tok.toUpperCase());
                        }
                    }
                }

                String holderName = nameBuilder.length() > 0 ? nameBuilder.toString() : "FARMER";
                double drBal = numCols.size() > 0 ? numCols.get(0) : 0.0;
                double crBal = numCols.size() > 1 ? numCols.get(1) : 0.0;
                double odInt = numCols.size() > 4 ? numCols.get(4) : 0.0;

                // If single balance column has explicit CR suffix (e.g. 450.00CR), assign to crBal
                if (rawNumTokens.size() == 1 && rawNumTokens.get(0).endsWith("CR")) {
                    crBal = drBal;
                    drBal = 0.0;
                }

                String rowCat = detectCategory(trimmed, currentCategory);
                if ("OTHER".equals(rowCat) && crBal > 0 && drBal == 0) {
                    rowCat = "SB";
                }

                AccountRecord rec = db.accounts.get(acct);
                if (rec == null) {
                    rec = new AccountRecord(acct, cif, holderName, rowCat);
                    db.accounts.put(acct, rec);
                } else {
                    if ("OTHER".equals(rec.category) && !"OTHER".equals(rowCat)) {
                        rec.category = rowCat;
                    }
                    if (holderName.length() > rec.name.length()) {
                        rec.name = holderName;
                    }
                }
                rec.history.put(dateKey, new BalanceSnapshot(drBal, crBal, odInt));
            }
        }
    }

    private static String detectCategory(String line, String currentCat) {
        String u = line.toUpperCase();
        if (u.contains("SBCHQ") || u.contains("SAVING") || u.contains("SB GEN") || u.contains("SB-")) return "SB";
        if (u.contains("KHARIF") && u.contains("KIND")) return "KHARIF KIND";
        if (u.contains("KHARIF") && (u.contains("CASH") || u.contains("CROP"))) return "KHARIF CASH";
        if (u.contains("RABI") && u.contains("KIND")) return "RABI KIND";
        if (u.contains("RABI") && (u.contains("CASH") || u.contains("CROP"))) return "RABI CASH";
        if (u.contains("JAI KISAN") || u.contains("OVERDRAFT") || u.contains("CASH CREDIT") || u.contains("KCC")) return "OTHER";
        return currentCat;
    }

    private static double parseAmount(String raw) {
        if (raw == null) return 0.0;
        String clean = raw.replaceAll("[,DRCRdrcr\\-]", "").trim();
        try {
            return Math.abs(Double.parseDouble(clean));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static int generateDemoFiles(Path demoRoot) throws IOException {
        Files.createDirectories(demoRoot);
        Path d1 = demoRoot.resolve("20260401");
        Path d2 = demoRoot.resolve("20260518");
        Path d3 = demoRoot.resolve("20261109");
        Path d4 = demoRoot.resolve("20261110");

        if (!Files.exists(d1)) {
            writeGzipSample(d1.resolve("REPORT_ANY_NAME.gz"), Arrays.asList(
                "SCHEME: SBCHQ SAVINGS ACCOUNT",
                "1 000010000001 500010000001 RAMESHWAR PRASAD 0.00 18500.00 0.00 4.00 0.00 0.00 0.00",
                "2 000010000002 500010000002 KAMLESH PATEL 0.00 4200.00 0.00 4.00 0.00 0.00 0.00",
                "3 000010000003 500010000003 SURESH YADAV 0.00 9100.00 0.00 4.00 0.00 0.00 0.00",
                "4 000010000004 500010000004 DINESH KUSHWAHA 0.00 12000.00 0.00 4.00 0.00 0.00 0.00",
                "5 000010000005 500010000005 HARISHANKAR TIWARI 0.00 14200.00 0.00 4.00 0.00 0.00 0.00",
                "SCHEME: KHARIF KIND FERTILIZER",
                "6 000010000001 710010000001 RAMESHWAR PRASAD 24000.00 0.00 0.00 7.00 0.00 0.00 0.00",
                "7 000010000002 710010000002 KAMLESH PATEL 8.50 0.00 0.00 7.00 0.00 0.00 0.00",
                "8 000010000003 710010000003 SURESH YADAV 31500.00 0.00 0.00 7.00 0.00 0.00 0.00",
                // Harishankar Tiwari has a CR balance of Rs. 450.00 in his Kharif Kind DMR account!
                "9 000010000005 710010000005 HARISHANKAR TIWARI 0.00 450.00 0.00 7.00 0.00 0.00 0.00",
                "SCHEME: RABI CASH CROP",
                "10 000010000001 740010000001 RAMESHWAR PRASAD 6.00 0.00 0.00 7.00 0.00 0.00 0.00"
            ));
            writeGzipSample(d2.resolve("REPORT_ANY_NAME.gz"), Arrays.asList(
                "SCHEME: SBCHQ SAVINGS ACCOUNT",
                "1 000010000001 500010000001 RAMESHWAR PRASAD 0.00 19000.00 0.00 4.00 0.00 0.00 0.00",
                "2 000010000002 500010000002 KAMLESH PATEL 0.00 4200.00 0.00 4.00 0.00 0.00 0.00",
                "3 000010000003 500010000003 SURESH YADAV 0.00 9100.00 0.00 4.00 0.00 0.00 0.00",
                "4 000010000004 500010000004 DINESH KUSHWAHA 0.00 12000.00 0.00 4.00 0.00 0.00 0.00",
                "5 000010000005 500010000005 HARISHANKAR TIWARI 0.00 14200.00 0.00 4.00 0.00 0.00 0.00",
                "SCHEME: KHARIF KIND FERTILIZER",
                "6 000010000001 710010000001 RAMESHWAR PRASAD 5.00 0.00 0.00 7.00 0.00 0.00 0.00",
                "7 000010000002 710010000002 KAMLESH PATEL 8.50 0.00 0.00 7.00 0.00 0.00 0.00",
                "8 000010000003 710010000003 SURESH YADAV 31500.00 0.00 0.00 7.00 0.00 0.00 0.00",
                "9 000010000005 710010000005 HARISHANKAR TIWARI 0.00 450.00 0.00 7.00 0.00 0.00 0.00"
            ));
            writeGzipSample(d3.resolve("REPORT_ANY_NAME.gz"), Arrays.asList(
                "SCHEME: SBCHQ SAVINGS ACCOUNT",
                "1 000010000001 500010000001 RAMESHWAR PRASAD 0.00 22400.00 0.00 4.00 0.00 0.00 0.00",
                "2 000010000002 500010000002 KAMLESH PATEL 0.00 6500.00 0.00 4.00 0.00 0.00 0.00",
                "3 000010000003 500010000003 SURESH YADAV 0.00 3100.00 0.00 4.00 0.00 0.00 0.00",
                "4 000010000004 500010000004 DINESH KUSHWAHA 0.00 15500.00 0.00 4.00 0.00 0.00 0.00",
                "5 000010000005 500010000005 HARISHANKAR TIWARI 0.00 14200.00 0.00 4.00 0.00 0.00 0.00",
                "SCHEME: KHARIF KIND FERTILIZER",
                "6 000010000001 710010000001 RAMESHWAR PRASAD 28000.00 0.00 0.00 7.00 0.00 0.00 0.00",
                "7 000010000002 710010000002 KAMLESH PATEL 7.25 0.00 0.00 7.00 0.00 0.00 0.00",
                "8 000010000003 710010000003 SURESH YADAV 31500.00 0.00 0.00 7.00 0.00 0.00 0.00",
                "9 000010000005 710010000005 HARISHANKAR TIWARI 0.00 450.00 0.00 7.00 0.00 0.00 0.00",
                "SCHEME: RABI KIND FERTILIZER",
                "10 000010000004 730010000004 DINESH KUSHWAHA 16500.00 0.00 0.00 7.00 0.00 0.00 0.00"
            ));
            return 1;
        } else if (!Files.exists(d4)) {
            writeGzipSample(d4.resolve("REPORT_ANY_NAME.gz"), Arrays.asList(
                "SCHEME: SBCHQ SAVINGS ACCOUNT",
                "1 000010000001 500010000001 RAMESHWAR PRASAD 0.00 25000.00 0.00 4.00 0.00 0.00 0.00",
                "2 000010000002 500010000002 KAMLESH PATEL 0.00 6500.00 0.00 4.00 0.00 0.00 0.00",
                "3 000010000003 500010000003 SURESH YADAV 0.00 8100.00 0.00 4.00 0.00 0.00 0.00",
                "4 000010000004 500010000004 DINESH KUSHWAHA 0.00 15500.00 0.00 4.00 0.00 0.00 0.00",
                "5 000010000005 500010000005 HARISHANKAR TIWARI 0.00 14200.00 0.00 4.00 0.00 0.00 0.00",
                "SCHEME: KHARIF KIND FERTILIZER",
                "6 000010000001 710010000001 RAMESHWAR PRASAD 28000.00 0.00 0.00 7.00 0.00 0.00 0.00",
                "7 000010000002 710010000002 KAMLESH PATEL 7.25 0.00 0.00 7.00 0.00 0.00 0.00",
                "8 000010000003 710010000003 SURESH YADAV 0.00 0.00 0.00 7.00 0.00 0.00 0.00",
                "9 000010000005 710010000005 HARISHANKAR TIWARI 0.00 450.00 0.00 7.00 0.00 0.00 0.00",
                "SCHEME: RABI KIND FERTILIZER",
                "10 000010000004 730010000004 DINESH KUSHWAHA 16500.00 0.00 0.00 7.00 0.00 0.00 0.00"
            ));
            return 2;
        }
        return 0;
    }

    private static void writeGzipSample(Path gzFile, List<String> lines) throws IOException {
        Files.createDirectories(gzFile.getParent());
        try (GZIPOutputStream gz = new GZIPOutputStream(Files.newOutputStream(gzFile));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gz, StandardCharsets.UTF_8))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
}