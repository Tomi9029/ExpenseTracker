package com.Vtomi.expensetracker.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "category_table")
public class Category {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;

    public Category(String name) {
        this.name = name;
    }

    // Getterek és Setterek
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // Ez azért kell, hogy a Spinnerben a név jelenjen meg
    @Override
    public String toString() {
        return name;
    }
}