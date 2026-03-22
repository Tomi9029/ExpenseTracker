package com.Vtomi.expensetracker;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.Vtomi.expensetracker.adapter.TransactionAdapter;
import com.Vtomi.expensetracker.model.Category;
import com.Vtomi.expensetracker.model.Transaction;
import com.Vtomi.expensetracker.viewmodel.ExpenseViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ExpenseViewModel expenseViewModel;
    private TextView textTotalBalance, textTotalIncome, textTotalExpense;
    private Button loginButton;
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<String> excelPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        initViews();
        setupRecyclerView();
        setupViewModel();
        setupLoginLogic();
        setupFab();

        // 2. INICIALIZÁLÁS (Metóduson belül!)
        excelPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        importExcelData(uri);
                    }
                }
        );
    }

    private void importExcelData(android.net.Uri uri) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            FirebaseUser user = mAuth.getCurrentUser();
            String userId = (user != null) ? user.getUid() : "anonymous";

            // Lefuttatjuk az importálót egy háttérszálon, hogy ne fagyjon le az app
            new Thread(() -> {
                try {
                    // Az ExcelImporter most már egy olyan listát ad vissza, amiben benne van a kategória neve is
                    List<ExcelImporter.ImportModel> importedData = ExcelImporter.parseOtpExcel(inputStream, userId);

                    // Visszatérünk a főszálra az adatbázis műveletekhez
                    runOnUiThread(() -> {
                        processImportedTransactions(importedData);
                        Toast.makeText(this, "Importálás befejezve!", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, "Hiba az importálás során!", Toast.LENGTH_SHORT).show());
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//    private void processImportedTransactions(List<ExcelImporter.ImportModel> data) {
//        // 1. Gyűjtsük ki az ÖSSZES egyedi kategória nevet az Excelből
//        java.util.Set<String> excelCategoryNames = new java.util.HashSet<>();
//        for (ExcelImporter.ImportModel item : data) {
//            excelCategoryNames.add(item.categoryName.trim());
//        }
//
//        // 2. Szerezzük meg a mostaniakat
//        List<Category> currentCats = expenseViewModel.getAllCategories().getValue();
//        if (currentCats == null) currentCats = new ArrayList<>();
//
//        // 3. Szúrjuk be azokat, amik hiányoznak
//        for (String name : excelCategoryNames) {
//            boolean exists = false;
//            for (Category c : currentCats) {
//                if (c.getName().equalsIgnoreCase(name)) {
//                    exists = true;
//                    break;
//                }
//            }
//            if (!exists) {
//                expenseViewModel.insertCategory(new Category(name));
//            }
//        }
//
//        // 4. TRÜKK: Várjunk egy kicsit, vagy használjunk egy Observer-t!
//        // Mivel a fenti insertek a háttérben futnak, a tranzakciókat
//        // érdemes egy kis késleltetéssel indítani, hogy az adatbázis "utolérje" magát.
//        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
//            // Frissítsük a listát (most már benne kell legyenek az újak ID-val)
//            List<Category> updatedCats = expenseViewModel.getAllCategories().getValue();
//            if (updatedCats == null) return;
//
//            for (ExcelImporter.ImportModel item : data) {
//                int foundId = -1;
//                for (Category c : updatedCats) {
//                    if (c.getName().equalsIgnoreCase(item.categoryName.trim())) {
//                        foundId = c.getId();
//                        break;
//                    }
//                }
//
//                // Ha még mindig nincs meg (nagyon lassú az adatbázis), akkor fallback
//                if (foundId == -1) foundId = getFallbackId(updatedCats);
//
//                item.transaction.setCategoryId(foundId);
//                expenseViewModel.insertTransaction(item.transaction);
//            }
//            Toast.makeText(this, "Importálás sikeresen befejeződött!", Toast.LENGTH_SHORT).show();
//        }, 500); // 500ms késleltetés általában elég a Room-nak az insertekhez
//    }

    private void processImportedTransactions(List<ExcelImporter.ImportModel> data) {
        // 1. Gyűjtsük ki az ÖSSZES egyedi kategória nevet az Excelből
        java.util.Set<String> excelCategoryNames = new java.util.HashSet<>();
        for (ExcelImporter.ImportModel item : data) {
            excelCategoryNames.add(item.categoryName.trim());
        }

        // 2. Szerezzük meg a mostaniakat
        List<Category> currentCats = expenseViewModel.getAllCategories().getValue();
        if (currentCats == null) currentCats = new ArrayList<>();

        // 3. Szúrjuk be azokat, amik hiányoznak
        for (String name : excelCategoryNames) {
            boolean exists = false;
            for (Category c : currentCats) {
                if (c.getName().equalsIgnoreCase(name)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                expenseViewModel.insertCategory(new Category(name));
            }
        }

        // 4. TRÜKK: Várjunk egy kicsit, vagy használjunk egy Observer-t!
        // Mivel a fenti insertek a háttérben futnak, a tranzakciókat
        // érdemes egy kis késleltetéssel indítani, hogy az adatbázis "utolérje" magát.
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            // Frissítsük a listát (most már benne kell legyenek az újak ID-val)
            List<Category> updatedCats = expenseViewModel.getAllCategories().getValue();
            if (updatedCats == null) return;

            for (ExcelImporter.ImportModel item : data) {
                int foundId = -1;
                for (Category c : updatedCats) {
                    if (c.getName().equalsIgnoreCase(item.categoryName.trim())) {
                        foundId = c.getId();
                        break;
                    }
                }

                // Ha még mindig nincs meg (nagyon lassú az adatbázis), akkor fallback
                if (foundId == -1) foundId = getFallbackId(updatedCats);

                item.transaction.setCategoryId(foundId);
                expenseViewModel.insertTransaction(item.transaction);
            }
            Toast.makeText(this, "Importálás sikeresen befejeződött!", Toast.LENGTH_SHORT).show();
        }, 500); // 500ms késleltetés általában elég a Room-nak az insertekhez
    }
    private int getFallbackId(List<Category> cats) {
        for (Category c : cats) {
            if (c.getName().equalsIgnoreCase("Általános")) return c.getId();
        }
        return cats.isEmpty() ? 1 : cats.get(0).getId();
    }

    private void initViews() {
        textTotalBalance = findViewById(R.id.text_total_balance);
        textTotalIncome = findViewById(R.id.text_total_income);
        textTotalExpense = findViewById(R.id.text_total_expense);
        loginButton = findViewById(R.id.btn_main_login);
        spinnerFilterCategory = findViewById(R.id.spinner_filter_category);
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final TransactionAdapter adapter = new TransactionAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnItemLongClickListener(transaction -> {
            showDeleteDialog(transaction);
        });
    }
    private Spinner spinnerFilterCategory;
    private List<Transaction> allTransactionsList = new ArrayList<>();
    private void setupViewModel() {
        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        // 1. Tranzakciók figyelése (ez már megvan, csak ellenőrizd)
        expenseViewModel.getAllTransactions().observe(this, transactions -> {
            allTransactionsList = transactions;
            filterTransactions();
        });


        // 2. ÚJ: Kategóriák figyelése és küldése az adapternek
        expenseViewModel.getAllCategories().observe(this, categories -> {
            // 1. Spinner frissítése (ez már megvan)
            List<String> categoryNames = new ArrayList<>();
            categoryNames.add("Összes");
            for (Category c : categories) {
                categoryNames.add(c.getName());
            }
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerFilterCategory.setAdapter(spinnerAdapter);

            // 2. ÚJ: Az ADAPTER-nek is adjuk át a kategóriákat!
            RecyclerView rv = findViewById(R.id.recyclerView);
            TransactionAdapter transactionAdapter = (TransactionAdapter) rv.getAdapter();
            if (transactionAdapter != null) {
                transactionAdapter.setCategories(categories); // Itt dől el, mi jelenik meg a listában
            }
        });
        spinnerFilterCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterTransactions(); // Frissítünk, ha a felhasználó választ
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private void filterTransactions() {
        if (spinnerFilterCategory == null || spinnerFilterCategory.getSelectedItem() == null) {
            return;
        }

        String selectedCategory = spinnerFilterCategory.getSelectedItem().toString();
        List<Transaction> filteredList = new ArrayList<>();

        if (selectedCategory.equals("Összes")) {
            filteredList = allTransactionsList;
        } else {
            List<Category> categories = expenseViewModel.getAllCategories().getValue();
            if (categories != null) {
                int selectedId = -1;
                for (Category c : categories) {
                    if (c.getName().equals(selectedCategory)) {
                        selectedId = c.getId();
                        break;
                    }
                }
                for (Transaction t : allTransactionsList) {
                    if (t.getCategoryId() == selectedId) {
                        filteredList.add(t);
                    }
                }
            } else {
                filteredList = allTransactionsList;
            }
        }

        RecyclerView rv = findViewById(R.id.recyclerView);
        TransactionAdapter adapter = (TransactionAdapter) rv.getAdapter();
        if (adapter != null) {
            adapter.setTransactions(filteredList);
            updateBalance(filteredList);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Kényszerítsük a gombot, hogy nézzen rá újra a Firebase-re
        updateLoginButtonUI();
    }

    private void setupLoginLogic() {
        updateLoginButtonUI();

        loginButton.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            } else {
                performLogout();
            }
        });
    }

    private void updateLoginButtonUI() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            // Ha van bejelentkezett felhasználó
            String name = mAuth.getCurrentUser().getDisplayName();
            loginButton.setText("Kijelentkezés (" + (name != null ? name : "Felhasználó") + ")");
        } else {
            // Ha nincs senki
            loginButton.setText("Bejelentkezés");
        }
        FirebaseUser user = mAuth.getCurrentUser();
        expenseViewModel.setCurrentUserId(user != null ? user.getUid() : "anonymous");
    }

    private void performLogout() {
        mAuth.signOut();
        GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()).signOut();
        updateLoginButtonUI();
        Toast.makeText(this, "Kijelentkezve", Toast.LENGTH_SHORT).show();
    }

    private void setupFab() {
        findViewById(R.id.fab_add).setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, AddTransactionActivity.class));
        });

        findViewById(R.id.fab_stats).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, StatisticsActivity.class));
        });
        findViewById(R.id.btn_import).setOnClickListener(v -> {
            excelPickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        });
    }

    private void showDeleteDialog(com.Vtomi.expensetracker.model.Transaction transaction) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Törlés")
                .setMessage("Biztosan törölni szeretnéd?")
                .setPositiveButton("Igen", (d, w) -> expenseViewModel.deleteTransaction(transaction))
                .setNegativeButton("Mégse", null)
                .show();
    }

    private void updateBalance(java.util.List<com.Vtomi.expensetracker.model.Transaction> transactions) {
        double total = 0;
        double income = 0;
        double expense = 0;

        for (com.Vtomi.expensetracker.model.Transaction t : transactions) {
            if (t.isIncome()) {
                income += t.getAmount();
                total += t.getAmount();
            } else {
                expense += t.getAmount();
                total -= t.getAmount();
            }
        }

        textTotalBalance.setText(String.format("%.0f Ft", total));
        textTotalIncome.setText(String.format("Bevétel: %.0f Ft", income));
        textTotalExpense.setText(String.format("Kiadás: %.0f Ft", expense));

        if (total < 0) {
            // Negatív egyenleg -> Piros
            textTotalBalance.setTextColor(android.graphics.Color.parseColor("#E91E63"));
        } else if (total > 0) {
            // Pozitív egyenleg -> Zöld
            textTotalBalance.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
        } else {
            // Pontosan 0 -> Fekete (vagy a téma szerinti alapértelmezett)
            textTotalBalance.setTextColor(android.graphics.Color.BLACK);
        }
    }
}