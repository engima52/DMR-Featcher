import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExcelExporter {

    public static void exportSixSheets(File outFile, List<CIFMasterRecord> cifMasterList) throws Exception {
        try (PrintWriter pw = new PrintWriter(outFile, StandardCharsets.UTF_8.name())) {
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<?mso-application progid=\"Excel.Sheet\"?>");
            pw.println("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"");
            pw.println(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"");
            pw.println(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"");
            pw.println(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">");
            pw.println("<Styles>");
            pw.println(" <Style ss:ID=\"Header\"><Font ss:FontName=\"Segoe UI\" ss:Size=\"10\" ss:Color=\"#FFFFFF\" ss:Bold=\"1\"/><Interior ss:Color=\"#1E293B\" ss:Pattern=\"Solid\"/></Style>");
            pw.println(" <Style ss:ID=\"RowEven\"><Font ss:FontName=\"Segoe UI\" ss:Size=\"10\" ss:Color=\"#0F172A\"/><Interior ss:Color=\"#FFFFFF\" ss:Pattern=\"Solid\"/><NumberFormat ss:Format=\"@\"/></Style>");
            pw.println(" <Style ss:ID=\"RowOdd\"><Font ss:FontName=\"Segoe UI\" ss:Size=\"10\" ss:Color=\"#0F172A\"/><Interior ss:Color=\"#F1F5F9\" ss:Pattern=\"Solid\"/><NumberFormat ss:Format=\"@\"/></Style>");
            pw.println("</Styles>");

            // 1. ALL Sheet — Every Account No, 01/04 Balance, Cleared Date & Today Balance in SEPARATE CELLS
            pw.println("<Worksheet ss:Name=\"ALL\"><Table>");
            writeExcelRow(pw, "Header", new String[]{
                "SLNO", "12-DIGIT CIF", "FARMER NAME",
                "SB A/C NO", "SB TODAY BAL",
                "KHARIF KIND A/C NO", "KHARIF KIND 01/04 BAL", "KHARIF KIND CLEARED DATE", "KHARIF KIND TODAY BAL",
                "KHARIF CASH A/C NO", "KHARIF CASH 01/04 BAL", "KHARIF CASH CLEARED DATE", "KHARIF CASH TODAY BAL",
                "RABI KIND A/C NO", "RABI KIND 01/04 BAL", "RABI KIND CLEARED DATE", "RABI KIND TODAY BAL",
                "RABI CASH A/C NO", "RABI CASH 01/04 BAL", "RABI CASH CLEARED DATE", "RABI CASH TODAY BAL",
                "OTHER / OD A/C NO", "OTHER / OD TODAY BAL",
                "ELIGIBILITY STATUS", "EXACT AUDIT REASON", "MANUAL CBS OVERRIDE"
            });

            int idx = 1;
            for (CIFMasterRecord c : cifMasterList) {
                String st = (idx % 2 == 0) ? "RowEven" : "RowOdd";
                writeExcelRow(pw, st, new String[]{
                    String.valueOf(idx++), c.cif, c.name,
                    SamayojanModels.joinAccountsOnly(c.sb), SamayojanModels.joinTodayBalancesOnly(c.sb),
                    SamayojanModels.joinAccountsOnly(c.kharifKind), SamayojanModels.joinAprBalancesOnly(c.kharifKind), SamayojanModels.joinClearedDatesOnly(c.kharifKind), SamayojanModels.joinTodayBalancesOnly(c.kharifKind),
                    SamayojanModels.joinAccountsOnly(c.kharifCash), SamayojanModels.joinAprBalancesOnly(c.kharifCash), SamayojanModels.joinClearedDatesOnly(c.kharifCash), SamayojanModels.joinTodayBalancesOnly(c.kharifCash),
                    SamayojanModels.joinAccountsOnly(c.rabiKind), SamayojanModels.joinAprBalancesOnly(c.rabiKind), SamayojanModels.joinClearedDatesOnly(c.rabiKind), SamayojanModels.joinTodayBalancesOnly(c.rabiKind),
                    SamayojanModels.joinAccountsOnly(c.rabiCash), SamayojanModels.joinAprBalancesOnly(c.rabiCash), SamayojanModels.joinClearedDatesOnly(c.rabiCash), SamayojanModels.joinTodayBalancesOnly(c.rabiCash),
                    SamayojanModels.joinAccountsOnly(c.other), SamayojanModels.joinTodayBalancesOnly(c.other),
                    c.finalStatus, c.finalReason, c.override
                });
            }
            pw.println("</Table></Worksheet>");

            // 2-6. Dedicated Scheme Sheets — Separate DR and CR Balance Cells
            writeSchemeSheet(pw, cifMasterList, "SB", 1);
            writeSchemeSheet(pw, cifMasterList, "KHARIF KIND", 2);
            writeSchemeSheet(pw, cifMasterList, "KHARIF CASH", 3);
            writeSchemeSheet(pw, cifMasterList, "RABI KIND", 4);
            writeSchemeSheet(pw, cifMasterList, "RABI CASH", 5);

            pw.println("</Workbook>");
        }
    }

    private static void writeSchemeSheet(PrintWriter pw, List<CIFMasterRecord> cifMasterList, String sheetName, int type) {
        pw.println("<Worksheet ss:Name=\"" + sheetName + "\"><Table>");
        writeExcelRow(pw, "Header", new String[]{
            "SLNO", "12-DIGIT CIF", "ACCOUNT NUMBER", "FARMER NAME",
            "01/04/2026 DR BAL", "01/04/2026 CR BAL", "FIRST SEEN DATE",
            "INTERMEDIATE CLEARED DATE", "TODAY DR BAL", "TODAY CR BAL",
            "ELIGIBILITY STATUS", "EXACT AUDIT REASON", "MANUAL CBS OVERRIDE"
        });
        int r = 1;
        for (CIFMasterRecord c : cifMasterList) {
            List<EnrichedAccount> list = type == 1 ? c.sb : (type == 2 ? c.kharifKind : (type == 3 ? c.kharifCash : (type == 4 ? c.rabiKind : c.rabiCash)));
            for (EnrichedAccount a : list) {
                String st = (r % 2 == 0) ? "RowEven" : "RowOdd";
                writeExcelRow(pw, st, new String[]{
                    String.valueOf(r++), c.cif, a.account, c.name,
                    SamayojanModels.AMT_FMT.format(a.drApr1),
                    SamayojanModels.AMT_FMT.format(a.crApr1),
                    SamayojanModels.formatDate(a.firstSeenDate),
                    a.clearedDate != null ? SamayojanModels.formatDate(a.clearedDate) : "-",
                    SamayojanModels.AMT_FMT.format(a.drToday),
                    SamayojanModels.AMT_FMT.format(a.crToday),
                    c.finalStatus, c.finalReason, c.override
                });
            }
        }
        pw.println("</Table></Worksheet>");
    }

    private static void writeExcelRow(PrintWriter pw, String styleId, String[] cols) {
        pw.print("<Row>");
        for (String col : cols) {
            pw.print("<Cell ss:StyleID=\"" + styleId + "\"><Data ss:Type=\"String\">" + escXml(col) + "</Data></Cell>");
        }
        pw.println("</Row>");
    }

    private static String escXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}