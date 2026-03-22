package com.Vtomi.expensetracker;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public class MyPieChartMarkerView extends MarkerView {

    private TextView tvCategory, tvAmount;

    public MyPieChartMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvCategory = findViewById(R.id.marker_category_name);
        tvAmount = findViewById(R.id.marker_amount);
    }

    // Ez a metódus fut le, amikor a felhasználó rákattint egy szeletre
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e instanceof PieEntry) {
            PieEntry pe = (PieEntry) e;
            // Beállítjuk a kategória nevét
            tvCategory.setText(pe.getLabel());
            // Beállítjuk a pontos összeget Ft formátumban
            tvAmount.setText(String.format("%.0f Ft", pe.getValue()));
        }
        super.refreshContent(e, highlight);
    }

    // Beállítjuk a buborék pozícióját (középre, a szelet fölé)
    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}