package com.example.taybudget.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.taybudget.R;
import com.example.taybudget.data.handler.GoalHandler;
import com.example.taybudget.data.handler.interfaces.TaskCompletionCallback;
import com.example.taybudget.data.handler.model.GoalCalculation;
import com.example.taybudget.enums.CategoryEnum;
import com.example.taybudget.tools.GenericRecyclerAdapter;
import com.example.taybudget.ui.model.UiGoalCategory;
import com.example.taybudget.ui.model.UiGoal;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AddGoalActivity extends AppCompatActivity {
    private List<UiGoalCategory> uiGoalCategories;
    private List<UiGoal> uiGoals;
    private GenericRecyclerAdapter<UiGoalCategory> weightAdapter;
    private GenericRecyclerAdapter<UiGoal> reductionAdapter;
    private boolean clickable = true;
    private GoalCalculation calculation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_goal);

        prepareData();
        createRecyclers();
        calculateButton();
        addButton();
    }

    private void addButton() {
        findViewById(R.id.goal_add_button).setOnClickListener(v -> {
            String name = ((EditText) findViewById(R.id.goal_name)).getText().toString();
            if (name.equals(""))
                Toast.makeText(getApplicationContext(), "Name cannot be blank", Toast.LENGTH_SHORT).show();
            else if (calculation == null)
                Toast.makeText(getApplicationContext(), "Calculation not ready yet", Toast.LENGTH_SHORT).show();
            else if (!calculation.isSuccess())
                Toast.makeText(getApplicationContext(), "Goal was not sucessful", Toast.LENGTH_SHORT).show();
            else {
                GoalHandler.getInstance().saveGoal(name, calculation, taskCompletionCallback);
            }
        });
    }

    private void calculateButton() {
        findViewById(R.id.goal_calculate_button).setOnClickListener(v -> {
            String amt = ((EditText) findViewById(R.id.goal_amount)).getText().toString();
            String mnt = ((EditText) findViewById(R.id.goal_time_expected)).getText().toString();
            double balance = ((Slider) findViewById(R.id.item_goal_balance_slider)).getValue();
            double amount;
            int months;
            if (amt.equals("") || mnt.equals(""))
                Toast.makeText(getApplicationContext(), "Amount or Time cannot be blank", Toast.LENGTH_LONG).show();
            else if ((amount = Double.parseDouble(amt)) == 0 || (months = Integer.parseInt(mnt)) == 0) {
                Toast.makeText(getApplicationContext(), "Amount or Time cannot be 0", Toast.LENGTH_LONG).show();
            } else if (balance == 0 && uiGoalCategories.stream().filter(uiGoalCategory -> uiGoalCategory.getWeight() == 0).count() == CategoryEnum.values().length) {
                Toast.makeText(getApplicationContext(), "All category weights and balance cannot be 0", Toast.LENGTH_LONG).show();
            } else if (clickable) {
                clickable = false;
                findViewById(R.id.animation_loading).setVisibility(View.VISIBLE);
                Map<CategoryEnum, Double> weights = new HashMap<>();
                uiGoalCategories.forEach(uiGoalCategory -> weights.put(uiGoalCategory.getCategory(), uiGoalCategory.getWeight()));
                GoalHandler.getInstance().calculate(amount, balance, months, weights, (goalCalculation, documentId) -> {
                    if (uiGoals.size() != 0) {
                        uiGoals.clear();
                        reductionAdapter.notifyDataSetChanged();
                    }
                    uiGoals.add(new UiGoal("Status", goalCalculation.getComments(), goalCalculation.getMasterAggregate(), goalCalculation.getUpdatedMasterAggregate()));
                    for (int i = 0; i < goalCalculation.getMonthlyAggregates().size(); i++) {
                        uiGoals.add(new UiGoal(goalCalculation.getMonthlyAggregates().get(i), goalCalculation.getUpdatedMonthlyAggregates().get(i)));
                    }
                    reductionAdapter.notifyDataSetChanged();
                    clickable = true;
                    calculation = goalCalculation;
                    findViewById(R.id.animation_loading).setVisibility(View.GONE);
                }, (message, exception) -> {
                    clickable = true;
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    findViewById(R.id.animation_loading).setVisibility(View.GONE);
                });
            }
        });
    }

    private void prepareData() {
        GoalHandler.getInstance().getMasterAggregate((result, documentId) -> {
            Slider slider = findViewById(R.id.item_goal_balance_slider);
            slider.setValueTo((float) result.getBalance());
            slider.setValueFrom(0.0f);
        }, (message, exception) -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
        uiGoalCategories = Arrays.stream(CategoryEnum.values())
                .map(category -> new UiGoalCategory(category, 0))
                .collect(Collectors.toList());
        uiGoals = new ArrayList<>();
    }

    private void createRecyclers() {
        RecyclerView weightRecycler = findViewById(R.id.goal_weight_recycler);
        RecyclerView reductionRecycler = findViewById(R.id.goal_category_recycler);

        weightAdapter = new GenericRecyclerAdapter<>(AddGoalActivity.this,
                R.layout.item_goal_weight, uiGoalCategories,
                (context, viewHolder, position) -> {
                    ((TextView) viewHolder.itemView.findViewById(R.id.item_goal_category))
                            .setText(uiGoalCategories.get(position).getCategory().getName());
                }, viewHolder -> ((Slider) viewHolder.itemView.findViewById(R.id.item_goal_slider))
                .addOnChangeListener((slider, value, fromUser) -> {
                    uiGoalCategories.get(viewHolder.getAdapterPosition()).setWeight(value);
                    Log.d("AddGoalSlider", uiGoalCategories.get(viewHolder.getAdapterPosition()).getCategory().getName() + " => " + value);
                }));

        reductionAdapter = new GenericRecyclerAdapter<>(AddGoalActivity.this,
                R.layout.item_big_text_web_view_card, uiGoals,
                (context, viewHolder, position) -> {
                    UiGoal goalMonth = uiGoals.get(position);
                    ((TextView) viewHolder.itemView.findViewById(R.id.item_big_text)).setText(goalMonth.getName());
                    ((WebView) viewHolder.itemView.findViewById(R.id.item_small_text)).loadDataWithBaseURL(null, goalMonth.getHtml(), "text/html", "utf-8", null);
                }, null);

        weightRecycler.setAdapter(weightAdapter);
        weightRecycler.setLayoutManager(new LinearLayoutManager(AddGoalActivity.this, LinearLayoutManager.VERTICAL, false));

        reductionRecycler.setAdapter(reductionAdapter);
        reductionRecycler.setLayoutManager(new LinearLayoutManager(AddGoalActivity.this, LinearLayoutManager.HORIZONTAL, false));
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(reductionRecycler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GoalHandler.getInstance().destroy();
    }

    private final TaskCompletionCallback taskCompletionCallback = new TaskCompletionCallback() {
        @Override
        public void taskComplete(String message) {
            if (message != null)
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            clickable = true;
            setResult(RESULT_OK);
            finish();
        }

        @Override
        public void error(String message) {
            if (message != null)
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            clickable = true;
        }
    };
}