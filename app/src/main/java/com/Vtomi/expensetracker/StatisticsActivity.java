package com.Vtomi.expensetracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.Vtomi.expensetracker.model.Category;
import com.Vtomi.expensetracker.model.Transaction;
import com.Vtomi.expensetracker.viewmodel.ExpenseViewModel;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class StatisticsActivity extends AppCompatActivity {

    private ExpenseViewModel viewModel;
    private PieChart pieChart;

    private List<Transaction> currentTransactions = null;
    private List<Category> currentCategories = null;

    private Spinner spinnerMonth;
    private Spinner spinnerYear; // ÚJ: Év spinner

    private String[] months = {"Összes", "Január", "Február", "Március", "Április", "Május", "Június",
            "Július", "Augusztus", "Szeptember", "Október", "November", "December"};

    // ÚJ: Dinamikus év lista
    private List<String> yearsList = new ArrayList<>();
    private ArrayAdapter<String> yearAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        spinnerMonth = findViewById(R.id.spinner_month);
        spinnerYear = findViewById(R.id.spinner_year); // ÚJ
        pieChart = findViewById(R.id.pieChart);

        // Hónap adapter beállítása
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        // ÚJ: Év adapter beállítása
        yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, yearsList);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        // Listenerek a spinnerekhez
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tryDrawChart();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerMonth.setOnItemSelectedListener(spinnerListener);
        spinnerYear.setOnItemSelectedListener(spinnerListener); // ÚJ

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";
        viewModel.setCurrentUserId(uid);

        viewModel.getAllTransactions().observe(this, transactions -> {
            currentTransactions = transactions;
            extractYearsFromTransactions(transactions); // ÚJ: Évek kigyűjtése
            tryDrawChart();
        });

        viewModel.getAllCategories().observe(this, categories -> {
            currentCategories = categories;
            tryDrawChart();
        });

        MyPieChartMarkerView marker = new MyPieChartMarkerView(this, R.layout.marker_view_pie);
        pieChart.setMarker(marker);
        pieChart.setDrawMarkers(true);
    }

    // ÚJ: Ez a metódus megkeresi az összes létező évet a tranzakcióidból
    private void extractYearsFromTransactions(List<Transaction> transactions) {
        String currentSelected = spinnerYear.getSelectedItem() != null ? spinnerYear.getSelectedItem().toString() : "Összes";

        yearsList.clear();
        yearsList.add("Összes"); // Alapértelmezett opció

        Set<Integer> uniqueYears = new TreeSet<>(); // TreeSet automatikusan sorba rendezi növekvőbe
        Calendar cal = Calendar.getInstance();

        for (Transaction t : transactions) {
            cal.setTimeInMillis(t.getDate());
            uniqueYears.add(cal.get(Calendar.YEAR));
        }

        for (Integer year : uniqueYears) {
            yearsList.add(String.valueOf(year));
        }

        yearAdapter.notifyDataSetChanged();

        // Ha volt már valami kiválasztva, megpróbáljuk visszaállítani
        int position = yearsList.indexOf(currentSelected);
        if (position >= 0) {
            spinnerYear.setSelection(position);
        }
    }

    private void tryDrawChart() {
        if (currentTransactions != null && currentCategories != null) {
            updatePieChart(currentTransactions, currentCategories);
        }
    }

    private void updatePieChart(List<Transaction> transactions, List<Category> categories) {
        int selectedMonthIndex = spinnerMonth.getSelectedItemPosition();
        String selectedYearStr = spinnerYear.getSelectedItem() != null ? spinnerYear.getSelectedItem().toString() : "Összes";

        List<PieEntry> entries = new ArrayList<>();
        Map<Integer, Double> categoryTotals = new HashMap<>();
        double totalExpenseSum = 0;
        boolean hasExpenses = false;

        for (Transaction t : transactions) {
            if (!t.isIncome()) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(t.getDate());
                int monthOfTransaction = cal.get(Calendar.MONTH) + 1;
                int yearOfTransaction = cal.get(Calendar.YEAR);

                // ÚJ: Kombinált szűrés évre és hónapra
                boolean matchesMonth = (selectedMonthIndex == 0) || (monthOfTransaction == selectedMonthIndex);
                boolean matchesYear = selectedYearStr.equals("Összes") || String.valueOf(yearOfTransaction).equals(selectedYearStr);

                if (matchesMonth && matchesYear) {
                    double amount = t.getAmount();
                    categoryTotals.put(t.getCategoryId(),
                            categoryTotals.getOrDefault(t.getCategoryId(), 0.0) + amount);
                    totalExpenseSum += amount;
                    hasExpenses = true;
                }
            }
        }

        if (!hasExpenses) {
            pieChart.clear();
            pieChart.setNoDataText("Még nincsenek kiadásaid ebben az időszakban.");
            pieChart.invalidate();
            return;
        }

        for (Map.Entry<Integer, Double> entry : categoryTotals.entrySet()) {
            String catName = "Egyéb";
            for (Category c : categories) {
                if (c.getId() == entry.getKey()) {
                    catName = c.getName();
                    break;
                }
            }
            entries.add(new PieEntry(entry.getValue().floatValue(), catName));
        }
        String centerLabel = "Kiadások\n" + String.format("%.0f Ft", totalExpenseSum);
        pieChart.setCenterText(centerLabel);

        ArrayList<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.MATERIAL_COLORS) colors.add(c);
        for (int c : ColorTemplate.JOYFUL_COLORS) colors.add(c);
        for (int c : ColorTemplate.COLORFUL_COLORS) colors.add(c);
        for (int c : ColorTemplate.LIBERTY_COLORS) colors.add(c);
        for (int c : ColorTemplate.PASTEL_COLORS) colors.add(c);


        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.animateY(1000);
        pieChart.setCenterTextSize(16f);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.getLegend().setEnabled(true);
        pieChart.getLegend().setWordWrapEnabled(true);

        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.invalidate();
    }
}