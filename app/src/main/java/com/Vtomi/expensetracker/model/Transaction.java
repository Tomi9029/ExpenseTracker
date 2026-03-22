package com.Vtomi.expensetracker.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transaction_table")
public class Transaction {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String note;
    private double amount;
    private long date;
    private int categoryId;
    private boolean isIncome;
    private String userId;

    public Transaction(String note, double amount, long date, int categoryId, boolean isIncome, String userId) {
        this.note = note;
        this.amount = amount;
        this.date = date;
        this.categoryId = categoryId;
        this.isIncome = isIncome;
        this.userId = userId;
    }
    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNote() { return note; }
    public double getAmount() { return amount; }
    public long getDate() { return date; }
    public int getCategoryId() { return categoryId; }
    public boolean isIncome() { return isIncome; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
