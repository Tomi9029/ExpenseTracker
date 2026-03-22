package com.Vtomi.expensetracker;

import com.Vtomi.expensetracker.model.Transaction;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.DataFormatter;
import android.util.Log;

public class ExcelImporter {

    public static class ImportModel {
        public Transaction transaction;
        public String categoryName;

        public ImportModel(Transaction transaction, String categoryName) {
            this.transaction = transaction;
            this.categoryName = categoryName;
        }
    }

    public static List<ImportModel> parseOtpExcel(InputStream inputStream, String userId) throws Exception {
        List<ImportModel> results = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        DataFormatter formatter = new DataFormatter();

        // Az 1. sortól indulunk (mert a 0. a fejléc)
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            try {
                // A oszlop: Dátum
                String rawDate = formatter.formatCellValue(row.getCell(0));

                // Ha üres a dátum, valószínűleg egy üres sor a táblázat alján, ugorjuk át
                if (rawDate == null || rawDate.isEmpty()) {
                    continue;
                }

                // G oszlop: Kategória név
                String categoryName = formatter.formatCellValue(row.getCell(6));

                // K oszlop: Összeg
                org.apache.poi.ss.usermodel.Cell amountCell = row.getCell(10);
                double amount = 0.0;

                if (amountCell != null) {
                    if (amountCell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                        amount = amountCell.getNumericCellValue();
                    } else {
                        String rawAmount = formatter.formatCellValue(amountCell);
                        amount = cleanAmount(rawAmount);
                    }
                }

                // --- EZ A RÉSZ HIÁNYZOTT! ---
                long dateMillis = parseOtpDate(rawDate);

                // Ha nincs kategória, vagy "Nem kategorizált", legyen "Általános"
                if (categoryName.equalsIgnoreCase("Nem kategorizált") || categoryName.isEmpty()) {
                    categoryName = "Általános";
                }

                boolean isIncome = amount > 0;
                double absAmount = Math.abs(amount);

                // Létrehozzuk a tranzakciót, és betesszük a listába!
                Transaction t = new Transaction("OTP Import", absAmount, dateMillis, 0, isIncome, userId);
                results.add(new ImportModel(t, categoryName));
                // -----------------------------

            } catch (Exception e) {
                Log.e("ExcelImport", i + ". sor feldolgozása sikertelen: " + e.getMessage());
                continue;
            }
        }
        workbook.close();

        // Logoljuk ki, hány tételt találtunk, hogy lássuk a Logcatben
        Log.d("ExcelImport", "Sikeresen beolvasva " + results.size() + " darab tétel az Excelből.");
        return results;
    }

    private static double cleanAmount(String raw) {
        if (raw == null || raw.isEmpty()) return 0;

        String clean = raw.replace(" ", "");
        clean = clean.replace(".", "");
        clean = clean.replace(",", ".");
        clean = clean.replaceAll("[^0-9\\.-]", "");

        try {
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return 0;
        }
    }

    private static long parseOtpDate(String dateStr) {
        try {
            String onlyDate = dateStr.substring(0, 10);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.parse(onlyDate).getTime();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}