package com.Vtomi.expensetracker.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.Vtomi.expensetracker.model.Category;
import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    void insert(Category category);

    @Query("SELECT * FROM category_table ORDER BY name ASC")
    LiveData<List<Category>> getAllCategories();
}