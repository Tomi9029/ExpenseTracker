package com.Vtomi.expensetracker.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.Vtomi.expensetracker.model.Transaction;
import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    // Ez az új metódus, ami a userId alapján szűr
    @Query("SELECT * FROM transaction_table WHERE userId = :currentUserId ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByUser(String currentUserId);

    @Query("DELETE FROM transaction_table")
    void deleteAll();

    @Query("UPDATE transaction_table SET userId = :newUserId WHERE userId = 'anonymous'")
    void updateAnonymousTransactions(String newUserId);
}
