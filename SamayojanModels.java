import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.*;

public class SamayojanModels {
    public static final double MARGIN_RS = 10.0;
    public static final DecimalFormat AMT_FMT = new DecimalFormat("0.00");

    public static String formatDate(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) return yyyymmdd != null ? yyyymmdd : "-";
        return yyyymmdd.substring(6, 8) + "/" + yyyymmdd.substring(4, 6) + "/" + yyyymmdd.substring(0, 4);
    }

    // Formats exact DR or CR balance without ever zeroing out amounts < Rs. 10
    public static String formatDrCr(double dr, double cr, boolean isSb) {
        if (isSb) {
            if (dr > 0 && cr == 0) return AMT_FMT.format(dr) + " DR";
            return AMT_FMT.format(cr) + " CR";
        }
        if (cr > 0 && dr == 0) return AMT_FMT.format(cr) + " CR";
        if (dr > 0) return AMT_FMT.format(dr) + " DR";
        return "0.00";
    }

    // Table cell summary showing exact DR/CR values
    public static String formatShortSummary(List<EnrichedAccount> list) {
        if (list == null || list.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            EnrichedAccount a = list.get(i);
            if (i > 0) sb.append(" ; ");
            boolean isSb = "SB".equals(a.category);
            sb.append(a.account).append(" (").append(formatDrCr(a.drToday, a.crToday, isSb)).append(")");
        }
        return sb.toString();
    }

    // Excel helper: joins ONLY account numbers (kept strictly separate from balances)
    public static String joinAccountsOnly(List<EnrichedAccount> list) {
        if (list == null || list.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(" ; ");
            sb.append(list.get(i).account);
        }
        return sb.toString();
    }

    // Excel helper: joins ONLY 01/04/2026 balances (exact DR/CR)
    public static String joinAprBalancesOnly(List<EnrichedAccount> list) {
        if (list == null || list.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            EnrichedAccount a = list.get(i);
            if (i > 0) sb.append(" ; ");
            if (!a.existedOnApr1) {
                sb.append("New (").append(formatDate(a.firstSeenDate)).append(")");
            } else {
                sb.append(formatDrCr(a.drApr1, a.crApr1, "SB".equals(a.category)));
            }
        }
        return sb.toString();
    }

    // Excel helper: joins ONLY intermediate cleared dates
    public static String joinClearedDatesOnly(List<EnrichedAccount> list) {
        if (list == null || list.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            EnrichedAccount a = list.get(i);
            if (i > 0) sb.append(" ; ");
            sb.append(a.clearedDate != null ? formatDate(a.clearedDate) : "-");
        }
        return sb.toString();
    }

    // Excel helper: joins ONLY Today's balances (exact DR/CR, separate from account numbers)
    public static String joinTodayBalancesOnly(List<EnrichedAccount> list) {
        if (list == null || list.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            EnrichedAccount a = list.get(i);
            if (i > 0) sb.append(" ; ");
            sb.append(formatDrCr(a.drToday, a.crToday, "SB".equals(a.category)));
        }
        return sb.toString();
    }
}

class LocalDatabase implements Serializable {
    private static final long serialVersionUID = 2L;
    Set<String> loadedDates = new TreeSet<>();
    Map<String, AccountRecord> accounts = new TreeMap<>();
    Map<String, String> manualOverrides = new HashMap<>();
}

class BalanceSnapshot implements Serializable {
    private static final long serialVersionUID = 2L;
    double dr, cr, odInt;

    BalanceSnapshot(double dr, double cr, double odInt) {
        this.dr = dr;
        this.cr = cr;
        this.odInt = odInt;
    }
}

class AccountRecord implements Serializable {
    private static final long serialVersionUID = 2L;
    String account, cif, name, category;
    Map<String, BalanceSnapshot> history = new TreeMap<>();

    AccountRecord(String account, String cif, String name, String category) {
        this.account = account;
        this.cif = cif;
        this.name = name;
        this.category = category;
    }
}

class EnrichedAccount {
    String account, cif, category, firstSeenDate, clearedDate, paltiDate;
    boolean existedOnApr1;
    double drApr1, crApr1, drToday, crToday;
    double balApr1, balToday;

    EnrichedAccount(String account, String cif, String category, boolean existedOnApr1,
                    String firstSeenDate, double drApr1, double crApr1,
                    double drToday, double crToday, String clearedDate, String paltiDate) {
        this.account = account;
        this.cif = cif;
        this.category = category;
        this.existedOnApr1 = existedOnApr1;
        this.firstSeenDate = firstSeenDate;
        this.drApr1 = drApr1;
        this.crApr1 = crApr1;
        this.drToday = drToday;
        this.crToday = crToday;
        this.balApr1 = "SB".equals(category) ? crApr1 : drApr1;
        this.balToday = "SB".equals(category) ? crToday : drToday;
        this.clearedDate = clearedDate;
        this.paltiDate = paltiDate;
    }
}

class CIFMasterRecord {
    String cif, name;
    List<EnrichedAccount> sb = new ArrayList<>();
    List<EnrichedAccount> kharifKind = new ArrayList<>();
    List<EnrichedAccount> kharifCash = new ArrayList<>();
    List<EnrichedAccount> rabiKind = new ArrayList<>();
    List<EnrichedAccount> rabiCash = new ArrayList<>();
    List<EnrichedAccount> other = new ArrayList<>();
    List<EnrichedAccount> allDmr = new ArrayList<>();
    String autoStatus = "ELIGIBLE", autoReason = "";
    String finalStatus = "ELIGIBLE", finalReason = "", override = "Auto";

    CIFMasterRecord(String cif, String name) {
        this.cif = cif;
        this.name = name;
    }
}