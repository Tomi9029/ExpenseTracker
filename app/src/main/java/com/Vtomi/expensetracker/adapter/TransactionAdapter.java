package com.Vtomi.expensetracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.Vtomi.expensetracker.R;
import com.Vtomi.expensetracker.model.Category;
import com.Vtomi.expensetracker.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionHolder> {

    private List<Transaction> transactions = new ArrayList<>();
    private List<com.Vtomi.expensetracker.model.Category> categoryList = new ArrayList<>();
    public interface OnItemLongClickListener {
        void onItemLongClick(Transaction transaction);
    }

    private OnItemLongClickListener longClickListener;
    private List<Category> categories = new ArrayList<>();

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public TransactionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.transaction_item, parent, false);
        return new TransactionHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionHolder holder, int position) {
        Transaction current = transactions.get(position);

        String categoryName = "Egyéb";
        for (Category cat : categories) {
            if (cat.getId() == current.getCategoryId()) {
                categoryName = cat.getName();
                break;
            }
        }
        holder.textViewNote.setText(categoryName);

        long dateInMillis = current.getDate();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.", Locale.getDefault());
        String formattedDate = sdf.format(new java.util.Date(dateInMillis));

        if (current.getNote() != null && !current.getNote().isEmpty()) {
            holder.textViewDate.setText(formattedDate + " • " + current.getNote().toLowerCase());
        } else {
            holder.textViewDate.setText(formattedDate);
        }

        holder.textViewAmount.setText(String.format("%.0f Ft", current.getAmount()));
        if (current.isIncome()) {
            holder.textViewAmount.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            holder.textViewAmount.setText("+ " + holder.textViewAmount.getText());
        } else {
            holder.textViewAmount.setTextColor(android.graphics.Color.parseColor("#E91E63"));
            holder.textViewAmount.setText("- " + holder.textViewAmount.getText());
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(current);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }
    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    public class TransactionHolder extends RecyclerView.ViewHolder {
        private TextView textViewNote;
        private TextView textViewAmount;
        private TextView textViewDate;

        public TransactionHolder(@NonNull View itemView) {
            super(itemView);
            textViewNote = itemView.findViewById(R.id.text_note);
            textViewAmount = itemView.findViewById(R.id.text_amount);
            textViewDate = itemView.findViewById(R.id.text_date);
        }
    }
}