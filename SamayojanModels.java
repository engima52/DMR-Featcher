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

    public static String formatShortSummary(List<EnrichedAccount> list) {
        if (list == null || list.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            EnrichedAccount a = list.get(i);
            if (i > 0) sb.append(" ; ");
            sb.append(a.account).append(" (Rs.").append(AMT_FMT.format(a.balToday)).append(")");
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
    double balApr1, balToday;

    EnrichedAccount(String account, String cif, String category, boolean existedOnApr1,
                    String firstSeenDate, double balApr1, double balToday, String clearedDate, String paltiDate) {
        this.account = account;
        this.cif = cif;
        this.category = category;
        this.existedOnApr1 = existedOnApr1;
        this.firstSeenDate = firstSeenDate;
        this.balApr1 = balApr1;
        this.balToday = balToday;
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
