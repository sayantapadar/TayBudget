package com.example.taybudget.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.app.AlertDialog;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.taybudget.R;
import com.example.taybudget.data.handler.GoalHandler;
import com.example.taybudget.data.model.Goal;
import com.example.taybudget.tools.GenericRecyclerAdapter;
import com.example.taybudget.ui.model.UiGoal;
import com.example.taybudget.ui.model.UiViewGoal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViewGoalsActivity extends AppCompatActivity {
    private List<UiViewGoal> uiViewGoalList;
    private List<UiGoal> uiGoalList;
    private GenericRecyclerAdapter<UiGoal> monthAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_goals);

        populateGoals();
        setupMonths();
    }

    private void populateGoals() {
        uiViewGoalList = new ArrayList<>();

        RecyclerView recyclerView = findViewById(R.id.view_goal_recycler);
        GenericRecyclerAdapter<UiViewGoal> adapter = new GenericRecyclerAdapter<>(ViewGoalsActivity.this, R.layout.item_data, uiViewGoalList,
                (context, viewHolder, position) -> {
                    ((TextView) viewHolder.itemView.findViewById(R.id.item_data_heading)).setText(uiViewGoalList.get(position).getName());
                    ((TextView) viewHolder.itemView.findViewById(R.id.item_data_message)).setText(uiViewGoalList.get(position).getAmountString());
                    ((TextView) viewHolder.itemView.findViewById(R.id.item_data_footer)).setText(uiViewGoalList.get(position).getMonths() + " months");
                }, viewHolder -> {
            viewHolder.itemView.findViewById(R.id.item_data_card).setOnLongClickListener(v -> {
                new AlertDialog.Builder(ViewGoalsActivity.this)
                        .setTitle("Delete Goal")
                        .setMessage("Are you sure you want to delete " + uiViewGoalList.get(viewHolder.getAdapterPosition()).getName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            dialog.dismiss();
                            GoalHandler.getInstance().delete(uiViewGoalList.get(viewHolder.getAdapterPosition()).getGoal(),
                                    (message, documentId) -> {
                                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                                        populateGoals();
                                        populateMonths();
                                    },
                                    (message, exception) -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
                        }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).create().show();
                return false;
            });
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(ViewGoalsActivity.this, LinearLayoutManager.VERTICAL, false));

        GoalHandler.getInstance().getGoals((goals, documentId) -> {
            goals.forEach(goal -> uiViewGoalList.add(new UiViewGoal(goal)));
            adapter.notifyDataSetChanged();
            populateMonths();
        }, (message, exception) -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show(), false);
    }

    private void setupMonths() {
        uiGoalList = new ArrayList<>();
        RecyclerView recyclerView = findViewById(R.id.view_goal_monthly_recycler);
        monthAdapter = new GenericRecyclerAdapter<>(ViewGoalsActivity.this, R.layout.item_big_text_web_view_card,
                uiGoalList, (context, viewHolder, position) -> {
            UiGoal goalMonth = uiGoalList.get(position);
            ((TextView) viewHolder.itemView.findViewById(R.id.item_big_text)).setText(goalMonth.getName());
            ((WebView) viewHolder.itemView.findViewById(R.id.item_small_text)).loadDataWithBaseURL(null, goalMonth.getHtml(), "text/html", "utf-8", null);
        }, null);
        recyclerView.setAdapter(monthAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(ViewGoalsActivity.this, LinearLayoutManager.HORIZONTAL, false));
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
    }

    private void populateMonths() {
        uiGoalList.clear();
        monthAdapter.notifyDataSetChanged();
        getMonthlySavingsGoals();
        monthAdapter.notifyDataSetChanged();
    }

    private void getMonthlySavingsGoals() {
        uiGoalList.clear();
        Map<String, Map<String, Double>> savings = new LinkedHashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        if (uiViewGoalList != null && uiViewGoalList.size() != 0) {
            uiViewGoalList.forEach(uiViewGoal -> {
                Goal goal = uiViewGoal.getGoal();
                updateValues(savings, "Balance", "Balance", goal.getMasterBalanceUsed());
                if (goal.getSavings() != null & !goal.getSavings().isEmpty()) {
                    goal.getSavings().forEach((monthName, categoricalSaving) -> {
                        categoricalSaving.forEach((category, amount) -> {
                            updateValues(savings, monthName, category, amount);
                        });
                    });
                }
            });
            savings.forEach((heading, categoricalSavings) -> {
                uiGoalList.add(new UiGoal(heading, categoricalSavings));
            });
        }
    }

    private void updateValues(Map<String, Map<String, Double>> savings, String heading, String category, double amount) {
        savings.putIfAbsent(heading, new HashMap<>());
        savings.get(heading).putIfAbsent(category, 0.0);
        savings.get(heading).put(category, savings.get(heading).get(category) + amount);
    }
}