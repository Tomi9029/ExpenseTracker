package com.Vtomi.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton; // ÚJ
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog; // ÚJ
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.Vtomi.expensetracker.model.Category; // EZ HIÁNYZOTT
import com.Vtomi.expensetracker.model.Transaction;
import com.Vtomi.expensetracker.viewmodel.ExpenseViewModel;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.Calendar;

public class AddTransactionActivity extends AppCompatActivity {

    private EditText editAmount, editNote;
    private SwitchMaterial switchIncome;
    private Spinner spinnerCategory;
    private ExpenseViewModel viewModel;
    private long selectedDate = System.currentTimeMillis();
    private ArrayAdapter<Category> categoryAdapter;
    private ActivityResultLauncher<Intent> ocrLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        final Calendar calendar = Calendar.getInstance();

        editAmount = findViewById(R.id.edit_amount);
        editNote = findViewById(R.id.edit_note);
        switchIncome = findViewById(R.id.switch_income);
        spinnerCategory = findViewById(R.id.spinner_category);
        Button saveButton = findViewById(R.id.button_save);
        Button dateButton = findViewById(R.id.button_pick_date);
        ImageButton addCategoryButton = findViewById(R.id.btn_add_category); // ÚJ GOMB

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        // Spinner beállítása
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Kategóriák betöltése az adatbázisból
        viewModel.getAllCategories().observe(this, categories -> {
            categoryAdapter.clear();
            categoryAdapter.addAll(categories);
            categoryAdapter.notifyDataSetChanged();
        });

        // Új kategória gomb eseménye
        addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());

        // Dátumválasztó (marad a régi)
        dateButton.setOnClickListener(v -> {
            android.app.DatePickerDialog datePicker = new android.app.DatePickerDialog(this,
                    (view, year1, month1, dayOfMonth) -> {
                        calendar.set(year1, month1, dayOfMonth);
                        selectedDate = calendar.getTimeInMillis();
                        dateButton.setText(year1 + "." + (month1 + 1) + "." + dayOfMonth);
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });
        ocrLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        double amount = result.getData().getDoubleExtra("EXTRACTED_AMOUNT", 0);
                        if (amount > 0) {
                            editAmount.setText(String.valueOf((int)amount));
                            Toast.makeText(this, "Összeg beillesztve!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
        findViewById(R.id.fab_scan_receipt).setOnClickListener(v -> {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // HA VAN ENGEDÉLY: Mehetünk az OCR-re
                Intent intent = new Intent(this, OcrActivity.class);
                ocrLauncher.launch(intent);
            } else {
                // HA NINCS: Kérünk, és NEM indulunk el (majd a következő kattintásra, ha megadta)
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 100);
            }
        });

        saveButton.setOnClickListener(v -> saveTransaction());
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Új kategória");
        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Hozzáadás", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                viewModel.insertCategory(new Category(name));
            }
        });
        builder.setNegativeButton("Mégse", null);
        builder.show();
    }

    private void saveTransaction() {
        String note = editNote.getText().toString().trim();
        String amountStr = editAmount.getText().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Az összeg megadása kötelező!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        boolean isIncome = switchIncome.isChecked();

        // Fontos: Itt most már a kategória objektum ID-ját kérjük le!
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        int categoryId = (selectedCategory != null) ? selectedCategory.getId() : 0;

        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String userId = (currentUser != null) ? currentUser.getUid() : "anonymous";

        Transaction transaction = new Transaction(note, amount, selectedDate, categoryId, isIncome, userId);
        viewModel.insertTransaction(transaction);

        finish();
    }
}