package com.Vtomi.expensetracker.database;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.Vtomi.expensetracker.model.Category;
import com.Vtomi.expensetracker.model.Transaction;
import java.util.List;

public class ExpenseRepository {

    private TransactionDao transactionDao;
    private CategoryDao categoryDao;
    private LiveData<List<Category>> allCategories;

    public ExpenseRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        transactionDao = db.transactionDao();
        categoryDao = db.categoryDao();
        // A tranzakciókat már nem itt inicializáljuk fixen, mert a userId-tól függenek!
        allCategories = categoryDao.getAllCategories();
    }

    // ÚJ: Ez a metódus hidalja át a DAO és a ViewModel közötti szakadékot
    public LiveData<List<Transaction>> getTransactionsByUser(String userId) {
        return transactionDao.getTransactionsByUser(userId);
    }

    public LiveData<List<Category>> getAllCategories() {
        return allCategories;
    }

    public void insertTransaction(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> transactionDao.insert(transaction));
    }

    public void insertCategory(Category category) {
        AppDatabase.databaseWriteExecutor.execute(() -> categoryDao.insert(category));
    }

    public void deleteTransaction(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> transactionDao.delete(transaction));
    }
}