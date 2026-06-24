package com.example.cuisine_finder.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.ExploredPlace;
import com.example.cuisine_finder.repositories.InteractionRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private BarChart barChartVisits;
    private PieChart pieChartCategories;
    private TextView tvStatsSummary;
    private InteractionRepository interactionRepository;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        interactionRepository = new InteractionRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();

        barChartVisits = findViewById(R.id.barChartVisits);
        pieChartCategories = findViewById(R.id.pieChartCategories);
        tvStatsSummary = findViewById(R.id.tvStatsSummary);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadDataAndBuildCharts();
    }

    private void loadDataAndBuildCharts() {
        if (currentUserId == null) return;

        interactionRepository.getExploredByUser(currentUserId).get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<ExploredPlace> exploredPlaces = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                ExploredPlace ep = doc.toObject(ExploredPlace.class);
                if (ep != null) exploredPlaces.add(ep);
            }

            tvStatsSummary.setText("Bạn đã khám phá " + exploredPlaces.size() + " quán ăn");
            setupBarChart(exploredPlaces);
            setupPieChart(exploredPlaces);
        });
    }

    private void setupBarChart(List<ExploredPlace> places) {
        Map<Integer, Integer> monthCounts = new HashMap<>();
        // Initialize last 6 months
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 6; i++) {
            monthCounts.put(cal.get(Calendar.MONTH), 0);
            cal.add(Calendar.MONTH, -1);
        }

        for (ExploredPlace p : places) {
            Calendar pCal = Calendar.getInstance();
            pCal.setTimeInMillis(p.getLastVisitedAt());
            int month = pCal.get(Calendar.MONTH);
            if (monthCounts.containsKey(month)) {
                monthCounts.put(month, monthCounts.get(month) + 1);
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -5);
        for (int i = 0; i < 6; i++) {
            int m = cal.get(Calendar.MONTH);
            entries.add(new BarEntry(i, monthCounts.get(m)));
            labels.add("T" + (m + 1));
            cal.add(Calendar.MONTH, 1);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Số lần ghé thăm");
        dataSet.setColor(Color.parseColor("#FF5A00")); // orange_main
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        barChartVisits.setData(data);
        barChartVisits.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartVisits.getXAxis().setGranularity(1f);
        barChartVisits.getXAxis().setDrawGridLines(false);
        barChartVisits.getDescription().setEnabled(false);
        barChartVisits.animateY(1000);
        barChartVisits.invalidate();
    }

    private void setupPieChart(List<ExploredPlace> places) {
        Map<String, Integer> typeCounts = new HashMap<>();
        for (ExploredPlace p : places) {
            String type = p.getFoodType() != null ? p.getFoodType() : "Khác";
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueLineColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieChartCategories.setData(data);
        pieChartCategories.setEntryLabelColor(Color.BLACK);
        pieChartCategories.getDescription().setEnabled(false);
        pieChartCategories.setHoleRadius(40f);
        pieChartCategories.setTransparentCircleRadius(45f);
        pieChartCategories.animateXY(1000, 1000);
        pieChartCategories.invalidate();
    }
}
