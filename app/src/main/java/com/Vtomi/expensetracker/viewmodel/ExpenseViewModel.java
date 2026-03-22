package com.Vtomi.expensetracker.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.Vtomi.expensetracker.database.ExpenseRepository;
import com.Vtomi.expensetracker.model.Category;
import com.Vtomi.expensetracker.model.Transaction;
import java.util.List;

public class ExpenseViewModel extends AndroidViewModel {

    private ExpenseRepository repository;
    private LiveData<List<Transaction>> transactions;
    private LiveData<List<Category>> allCategories; // Vedd ki a kommentet!
    private MutableLiveData<String> currentUserId = new MutableLiveData<>();

    public ExpenseViewModel(Application application) {
        super(application);
        repository = new ExpenseRepository(application);
        allCategories = repository.getAllCategories(); // Inicializáld!

        // Ez a mágia: ha a currentUserId értéke változik, a lista automatikusan frissül
        transactions = Transformations.switchMap(currentUserId, id ->
                repository.getTransactionsByUser(id)
        );
    }

    public void setCurrentUserId(String id) {
        currentUserId.setValue(id);
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return transactions;
    }

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public void insertTransaction(Transaction transaction) { repository.insertTransaction(transaction); }
    public void insertCategory(Category category) { repository.insertCategory(category); }
    public void deleteTransaction(Transaction transaction) { repository.deleteTransaction(transaction); }
}