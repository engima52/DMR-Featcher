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

            // 1. ALL Sheet
            pw.println("<Worksheet ss:Name=\"ALL\"><Table>");
            writeExcelRow(pw, "Header", new String[]{
                "SLNO", "12-DIGIT CIF", "FARMER NAME", "SB ACCOUNT (TODAY)",
                "KHARIF KIND", "KHARIF CASH", "RABI KIND", "RABI CASH", "OTHER / OD / CC",
                "ELIGIBILITY STATUS", "EXACT AUDIT REASON", "MANUAL CBS OVERRIDE"
            });
            int idx = 1;
            for (CIFMasterRecord c : cifMasterList) {
                String st = (idx % 2 == 0) ? "RowEven" : "RowOdd";
                writeExcelRow(pw, st, new String[]{
                    String.valueOf(idx++), c.cif, c.name, SamayojanModels.formatShortSummary(c.sb),
                    SamayojanModels.formatShortSummary(c.kharifKind), SamayojanModels.formatShortSummary(c.kharifCash),
                    SamayojanModels.formatShortSummary(c.rabiKind), SamayojanModels.formatShortSummary(c.rabiCash),
                    SamayojanModels.formatShortSummary(c.other), c.finalStatus, c.finalReason, c.override
                });
            }
            pw.println("</Table></Worksheet>");

            // 2-6. Dedicated Scheme Sheets
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
            "01/04/2026 BAL / FIRST SEEN", "INTERMEDIATE CLEARED DATE", "TODAY BALANCE",
            "ELIGIBILITY STATUS", "EXACT AUDIT REASON", "MANUAL CBS OVERRIDE"
        });
        int r = 1;
        for (CIFMasterRecord c : cifMasterList) {
            List<EnrichedAccount> list = type == 1 ? c.sb : (type == 2 ? c.kharifKind : (type == 3 ? c.kharifCash : (type == 4 ? c.rabiKind : c.rabiCash)));
            for (EnrichedAccount a : list) {
                String st = (r % 2 == 0) ? "RowEven" : "RowOdd";
                String aprCol = a.existedOnApr1 ? SamayojanModels.AMT_FMT.format(a.balApr1) : ("New (" + SamayojanModels.formatDate(a.firstSeenDate) + ")");
                writeExcelRow(pw, st, new String[]{
                    String.valueOf(r++), c.cif, a.account, c.name,
                    aprCol, a.clearedDate != null ? SamayojanModels.formatDate(a.clearedDate) : "-",
                    SamayojanModels.AMT_FMT.format(a.balToday), c.finalStatus, c.finalReason, c.override
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
