package com.Vtomi.expensetracker.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.Vtomi.expensetracker.model.Category;
import com.Vtomi.expensetracker.model.Transaction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Megadjuk, melyik táblák tartozzanak az adatbázishoz
@Database(entities = {Transaction.class, Category.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TransactionDao transactionDao();
    public abstract CategoryDao categoryDao();

    private static volatile AppDatabase INSTANCE;

    // Ez a szálkezelő segít, hogy ne a kijelzőt kezelő szálon mentsünk adatot (ne fagyjon le az app)
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "expense_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}