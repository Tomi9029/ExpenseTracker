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

public class StatisticsActivity extends AppCompatActivity {

    private ExpenseViewModel viewModel;
    private PieChart pieChart;

    private List<Transaction> currentTransactions = null;
    private List<Category> currentCategories = null;
    private Spinner spinnerMonth;
    private String[] months = {"Összes", "Január", "Február", "Március", "Április", "Május", "Június",
            "Július", "Augusztus", "Szeptember", "Október", "November", "December"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // 1. hívás
        setContentView(R.layout.activity_statistics); // 2. layout betöltése

        // 3. Csak ezután jöhetnek a View-k!
        spinnerMonth = findViewById(R.id.spinner_month);
        pieChart = findViewById(R.id.pieChart);

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tryDrawChart();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";
        viewModel.setCurrentUserId(uid);

        viewModel.getAllTransactions().observe(this, transactions -> {
            currentTransactions = transactions;
            tryDrawChart();
        });

        viewModel.getAllCategories().observe(this, categories -> {
            currentCategories = categories;
            tryDrawChart();
        });
        pieChart = findViewById(R.id.pieChart);
        MyPieChartMarkerView marker = new MyPieChartMarkerView(this, R.layout.marker_view_pie);
        pieChart.setMarker(marker); // Megmondjuk a chartnak, hogy használja ezt a markert
        pieChart.setDrawMarkers(true); // Engedélyezzük a markerek rajzolását
    }

    // Ez a metódus csak akkor rajzol, ha már minden adat megérkezett
    private void tryDrawChart() {
        if (currentTransactions != null && currentCategories != null) {
            updatePieChart(currentTransactions, currentCategories);
        }
    }

    private void updatePieChart(List<Transaction> transactions, List<Category> categories) {
        int selectedMonthIndex = spinnerMonth.getSelectedItemPosition();

        List<PieEntry> entries = new ArrayList<>();
        Map<Integer, Double> categoryTotals = new HashMap<>();
        double totalExpenseSum = 0; // ÚJ: Itt tároljuk a végösszeget
        boolean hasExpenses = false;

        for (Transaction t : transactions) {
            if (!t.isIncome()) {
                boolean matchesMonth = true;
                if (selectedMonthIndex > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(t.getDate());
                    int monthOfTransaction = cal.get(Calendar.MONTH) + 1;
                    matchesMonth = (monthOfTransaction == selectedMonthIndex);
                }

                if (matchesMonth) {
                    double amount = t.getAmount();
                    categoryTotals.put(t.getCategoryId(),
                            categoryTotals.getOrDefault(t.getCategoryId(), 0.0) + amount);
                    totalExpenseSum += amount; // ÚJ: Hozzáadjuk a havi/összes szumhoz
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
        //pieChart.setCenterText("Kiadások");
        pieChart.setCenterTextSize(16f);
        //pieChart.setCenterTextSize(18f);
        pieChart.setHoleRadius(45f); // A lyuk mérete középen
        pieChart.setTransparentCircleRadius(50f);
        pieChart.getLegend().setEnabled(true); // Mutassa a jelmagyarázatot alul
        pieChart.getLegend().setWordWrapEnabled(true); // Ha sok a név, törje több sorba

        pieChart.setEntryLabelColor(Color.BLACK); // A feliratok a szeleteken legyenek feketék (jobban olvasható)
        pieChart.invalidate();
    }
}