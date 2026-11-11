import java.util.*;

public class EligibilityEngine {

    public static void recompute(
            LocalDatabase db,
            boolean missingAsZero,
            List<CIFMasterRecord> cifMasterList,
            Map<String, CIFMasterRecord> cifLookupMap,
            Map<String, CIFMasterRecord> acctToCifMap) {

        cifMasterList.clear();
        cifLookupMap.clear();
        acctToCifMap.clear();

        if (db.loadedDates.isEmpty()) return;

        List<String> sortedDates = new ArrayList<>(db.loadedDates);
        Collections.sort(sortedDates);
        String firstDate = sortedDates.get(0);
        String lastDate = sortedDates.get(sortedDates.size() - 1);

        Map<String, CIFMasterRecord> cifMap = new TreeMap<>();

        for (AccountRecord acc : db.accounts.values()) {
            CIFMasterRecord group = cifMap.get(acc.cif);
            if (group == null) {
                group = new CIFMasterRecord(acc.cif, acc.name);
                cifMap.put(acc.cif, group);
            } else if (acc.name != null && acc.name.length() > group.name.length()) {
                group.name = acc.name;
            }

            List<String> acctDates = new ArrayList<>(acc.history.keySet());
            Collections.sort(acctDates);
            String firstSeenDate = acctDates.isEmpty() ? firstDate : acctDates.get(0);
            boolean existedOnApr1 = acc.history.containsKey(firstDate);

            BalanceSnapshot snapFirst = acc.history.get(firstDate);
            BalanceSnapshot snapLast = acc.history.get(lastDate);

            double balApr1 = snapFirst != null ? ("SB".equals(acc.category) ? snapFirst.cr : snapFirst.dr) : 0.0;
            double balToday = snapLast != null ? ("SB".equals(acc.category) ? snapLast.cr : snapLast.dr) : 0.0;

            String clearedDate = null;
            String paltiDate = null;
            double prevDr = balApr1;

            if (!"SB".equals(acc.category) && existedOnApr1 && balApr1 > SamayojanModels.MARGIN_RS) {
                for (int i = 1; i < sortedDates.size(); i++) {
                    String dKey = sortedDates.get(i);
                    BalanceSnapshot snap = acc.history.get(dKey);
                    if (snap == null) {
                        if (missingAsZero && clearedDate == null) {
                            clearedDate = dKey;
                        }
                        continue;
                    }
                    if (snap.dr <= SamayojanModels.MARGIN_RS && clearedDate == null) {
                        clearedDate = dKey;
                    }
                    if (clearedDate == null && Math.abs(snap.dr - prevDr) > 100.0 && snap.dr > SamayojanModels.MARGIN_RS) {
                        paltiDate = dKey;
                    }
                    prevDr = snap.dr;
                }
            }

            EnrichedAccount ea = new EnrichedAccount(
                acc.account, acc.cif, acc.category, existedOnApr1,
                firstSeenDate, balApr1, balToday, clearedDate, paltiDate
            );

            if ("SB".equals(acc.category)) group.sb.add(ea);
            else if ("KHARIF KIND".equals(acc.category)) { group.kharifKind.add(ea); group.allDmr.add(ea); }
            else if ("KHARIF CASH".equals(acc.category)) { group.kharifCash.add(ea); group.allDmr.add(ea); }
            else if ("RABI KIND".equals(acc.category)) { group.rabiKind.add(ea); group.allDmr.add(ea); }
            else if ("RABI CASH".equals(acc.category)) { group.rabiCash.add(ea); group.allDmr.add(ea); }
            else { group.other.add(ea); group.allDmr.add(ea); }
        }

        for (CIFMasterRecord group : cifMap.values()) {
            List<String> unpaidList = new ArrayList<>();
            List<String> checkList = new ArrayList<>();
            List<String> clearedLateList = new ArrayList<>();
            List<String> newAfterApr1List = new ArrayList<>();
            boolean clearedBeforeApr1 = false;

            for (EnrichedAccount dmr : group.allDmr) {
                if (!dmr.existedOnApr1) {
                    if (dmr.balToday > SamayojanModels.MARGIN_RS) {
                        newAfterApr1List.add(dmr.category + " (" + dmr.account + ") opened after 01/04 [1st Seen: " +
                            SamayojanModels.formatDate(dmr.firstSeenDate) + ", Today: Rs." + SamayojanModels.AMT_FMT.format(dmr.balToday) + "]");
                    }
                    continue;
                }

                if (dmr.balApr1 <= SamayojanModels.MARGIN_RS) {
                    if (dmr.balApr1 > 0 || dmr.balToday > 0) clearedBeforeApr1 = true;
                } else {
                    if (dmr.balToday <= SamayojanModels.MARGIN_RS) {
                        String cDate = dmr.clearedDate != null ? SamayojanModels.formatDate(dmr.clearedDate) : SamayojanModels.formatDate(lastDate);
                        clearedLateList.add(dmr.category + " (" + dmr.account + ") cleared on " + cDate +
                            " [01/04: Rs." + SamayojanModels.AMT_FMT.format(dmr.balApr1) + " -> Today: Rs." + SamayojanModels.AMT_FMT.format(dmr.balToday) + "]");
                    } else if (dmr.clearedDate != null) {
                        clearedLateList.add(dmr.category + " (" + dmr.account + ") cleared on " +
                            SamayojanModels.formatDate(dmr.clearedDate) + " & re-availed fresh loan [Today: Rs." + SamayojanModels.AMT_FMT.format(dmr.balToday) + "]");
                    } else if (dmr.paltiDate != null || sortedDates.size() <= 2) {
                        String note = dmr.paltiDate != null
                            ? ("Balance shifted on " + SamayojanModels.formatDate(dmr.paltiDate) + " - Verify Same-Day Palti")
                            : ("01/04: Rs." + SamayojanModels.AMT_FMT.format(dmr.balApr1) + " & Today: Rs." + SamayojanModels.AMT_FMT.format(dmr.balToday));
                        checkList.add(dmr.category + " A/c " + dmr.account + " (" + note + ")");
                    } else {
                        unpaidList.add(dmr.category + " A/c " + dmr.account + " (01/04: Rs." +
                            SamayojanModels.AMT_FMT.format(dmr.balApr1) + ", Today: Rs." + SamayojanModels.AMT_FMT.format(dmr.balToday) + ")");
                    }
                }
            }

            if (!unpaidList.isEmpty()) {
                group.autoStatus = "NOT ELIGIBLE";
                group.autoReason = "Old Loan Unpaid since 01/04/2026: " + String.join("; ", unpaidList);
            } else if (!checkList.isEmpty()) {
                group.autoStatus = "NEEDS TO CHECK";
                group.autoReason = "Verify CBS Ledger: " + String.join("; ", checkList);
            } else if (!clearedLateList.isEmpty()) {
                group.autoStatus = "ELIGIBLE";
                group.autoReason = "Condition 2 (Cleared Late): " + String.join("; ", clearedLateList);
            } else if (!newAfterApr1List.isEmpty()) {
                group.autoStatus = "ELIGIBLE";
                group.autoReason = "Condition 1 (New Loan After 01/04/2026): " + String.join("; ", newAfterApr1List);
            } else if (clearedBeforeApr1) {
                group.autoStatus = "ELIGIBLE";
                group.autoReason = "Condition 1: Pre-01/04/2026 loan cleared (<= Rs. 10) on or before 01/04/2026.";
            } else {
                group.autoStatus = "ELIGIBLE";
                group.autoReason = "Condition 3: No pending DMR loan / Balance <= Rs. 10 on 01/04/2026 and Today.";
            }

            String ov = db.manualOverrides.getOrDefault(group.cif, "Auto");
            group.override = ov;
            if ("Cleared <= 01/04/2026".equals(ov)) {
                group.finalStatus = "ELIGIBLE";
                group.finalReason = "[CBS Verified: Cleared <= 01/04/2026] " + group.autoReason;
            } else if ("Cleared Late".equals(ov)) {
                group.finalStatus = "ELIGIBLE";
                group.finalReason = "[CBS Verified: Cleared Late] " + group.autoReason;
            } else if ("Old Loan Unpaid".equals(ov)) {
                group.finalStatus = "NOT ELIGIBLE";
                group.finalReason = "[CBS Verified: Old Loan Unpaid] " + group.autoReason;
            } else {
                group.finalStatus = group.autoStatus;
                group.finalReason = group.autoReason;
            }

            cifMasterList.add(group);
            cifLookupMap.put(group.cif, group);
            for (EnrichedAccount a : group.sb) acctToCifMap.put(a.account, group);
            for (EnrichedAccount a : group.allDmr) acctToCifMap.put(a.account, group);
        }
    }
}
