package com.example.taybudget.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.example.taybudget.R;
import com.example.taybudget.data.firestore.MasterAggregatesDAO;
import com.example.taybudget.data.firestore.MonthlyAggregatesDAO;
import com.example.taybudget.data.firestore.interfaces.ResultCallback;
import com.example.taybudget.data.handler.ExpenseHandler;
import com.example.taybudget.data.handler.IncomeHandler;
import com.example.taybudget.data.handler.MasterAggregateHandler;
import com.example.taybudget.data.handler.TrackerHandler;
import com.example.taybudget.data.handler.builders.html.HtmlTableBuilder;
import com.example.taybudget.data.handler.model.Tracker;
import com.example.taybudget.data.model.Expense;
import com.example.taybudget.data.model.Income;
import com.example.taybudget.data.model.MasterAggregate;
import com.example.taybudget.data.model.MonthlyAggregate;
import com.example.taybudget.enums.CategoryEnum;
import com.example.taybudget.enums.DataSortType;
import com.example.taybudget.enums.TrackerTypeEnum;
import com.example.taybudget.tools.CommonConstants;
import com.example.taybudget.tools.CommonUtils;
import com.example.taybudget.tools.GenericRecyclerAdapter;
import com.example.taybudget.tools.behaviour.recycler.filterable.FilterableRecyclerBehaviour;
import com.example.taybudget.ui.model.UiAggregate;
import com.example.taybudget.ui.model.UiData;
import com.example.taybudget.ui.model.UiMonth;
import com.example.taybudget.ui.model.UiMonthSelector;
import com.example.taybudget.ui.model.UiTracker;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import androidx.cardview.widget.CardView;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taybudget.databinding.ActivityMainBinding;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> launcher;

    private MasterAggregate masterAggregate;
    private String masterAggregateDocumentId;

    private final Map<String, Boolean> updatingMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CommonConstants.init();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        populateView(false);
                    }
                });

        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        populateToday();
        setupMonthYearPicker();
        populateView(true);  // inserts only once

        binding.fab.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddActivity.class);
            intent.putExtra("action", "Add");
            launcher.launch(intent);
        });

    }

    private void populateTrackers() {
        TrackerHandler.getInstance().clear();
        Date today = Calendar.getInstance(Locale.getDefault()).getTime();
        findViewById(R.id.main_tracker_add).setOnClickListener(v -> {
            View layout = LayoutInflater.from(MainActivity.this).inflate(R.layout.alert_add_tracker, null, false);
            CommonUtils.setDatePicker(MainActivity.this, layout.findViewById(R.id.add_date_text), Calendar.getInstance().getTime());
            CommonUtils.setDropdownItems(MainActivity.this, layout.findViewById(R.id.add_renew_period), TrackerTypeEnum.getNames());

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Add Tracker")
                    .setView(layout)
                    .setPositiveButton("Submit", (dialog, which) -> {
                        String name = ((EditText) layout.findViewById(R.id.add_name)).getText().toString();
                        String amount = ((EditText) layout.findViewById(R.id.add_amount)).getText().toString();
                        String dateText = ((EditText) layout.findViewById(R.id.add_date_text)).getText().toString();
                        String renewPeriod = ((TextView) layout.findViewById(R.id.add_renew_period)).getText().toString();
                        if (name.isEmpty() || amount.isEmpty() || dateText.isEmpty() || renewPeriod.isEmpty()) {
                            Toast.makeText(MainActivity.this, "All fields are mandatory", Toast.LENGTH_LONG).show();
                        } else {
                            try {
                                TrackerHandler.getInstance().addTracker(new Tracker(name, CommonConstants.DEFAULT_DATE_DISPLAY.parse(dateText), renewPeriod, Double.parseDouble(amount)),
                                        (tracker, documentId) -> runOnUiThread(() -> {
                                            Toast.makeText(MainActivity.this, "Tracker added", Toast.LENGTH_LONG).show();
                                            populateTrackers();
                                        }),
                                        (message, exception) -> runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show()));
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    })
                    .setNegativeButton("Dismiss", (dialog, which) -> dialog.dismiss())
                    .create().show();
        });

        updatingMap.put("Trackers", true);
        TrackerHandler.getInstance().getTrackers(today, (trackers, documentIds) -> runOnUiThread(() -> {
            updatingMap.put("Trackers", false);
            List<UiTracker> uiTrackers = new ArrayList<>();
            trackers.forEach((tracker, biParameterizedObject) -> {
                Log.d("TrackerCount", "tracker: " + tracker.getName() + " -> " + biParameterizedObject.getSecond());
                uiTrackers.add(new UiTracker(tracker, biParameterizedObject.getFirst(), biParameterizedObject.getSecond()));
            });
            uiTrackers.sort(TrackerHandler.getInstance().getComparator());
            GenericRecyclerAdapter<UiTracker> adapter = new GenericRecyclerAdapter<>(MainActivity.this, R.layout.item_tracker_view, uiTrackers,
                    (context, viewHolder, position) -> {
                        ((TextView) viewHolder.itemView.findViewById(R.id.item_tracker_view_name)).setText(uiTrackers.get(position).getName());
                        ((TextView) viewHolder.itemView.findViewById(R.id.item_tracker_view_amount)).setText(String.format(Locale.getDefault(), "%.2f", uiTrackers.get(position).getAmount()));
                        String since = "(since " + CommonConstants.DEFAULT_DATE_DISPLAY.format(uiTrackers.get(position).getDate()) + " - " + uiTrackers.get(position).getTracker().getRenewPeriod() + ")";
                        ((TextView) viewHolder.itemView.findViewById(R.id.item_tracker_view_date)).setText(since);
                    }, viewHolder -> {
                viewHolder.itemView.setOnClickListener(v -> {
                    UiTracker tracker = uiTrackers.get(viewHolder.getAdapterPosition());
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
                    HtmlTableBuilder.Builder details = new HtmlTableBuilder.Builder(4)
                            .setHeading("Date", "Label", "Category", "Amount");
                    tracker.getExpenses().forEach(expense -> details.addRow(sdf.format(expense.getDate()), expense.getName(), expense.getCategory(), String.format(Locale.getDefault(), "%.2f", expense.getAmount())));
                    details.addRow("", "TOTAL", "", tracker.getExpenses().stream().map(Expense::getAmount).reduce(Double::sum).orElse(0.0).toString());
                    View layout = LayoutInflater.from(MainActivity.this).inflate(R.layout.alert_daily_budget_details, null, false);
                    ((WebView) layout.findViewById(R.id.alert_daily_budget_webview)).loadDataWithBaseURL(null, details.format(), "text/html", "utf-8", null);

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(tracker.getName())
                            .setView(layout)
                            .setNegativeButton("Dismiss", (dialog, which) -> dialog.dismiss())
                            .setPositiveButton("Chart", (dialog, which) -> {
                                dialog.dismiss();
                                TrackerHandler.getInstance().showBudgetChart(MainActivity.this, tracker.getExpenses(), tracker.getDate(), today, tracker.getRenewAmount());
                            })
                            .setNeutralButton("Stop Session", (dialog, which) -> {
                                dialog.dismiss();
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Confirm Stop")
                                        .setMessage("Are you sure to stop the current tracker session. The current session cannot be restarted. You can start a new session again.")
                                        .setPositiveButton("Stop", (dialog1, which1) -> {
                                            TrackerHandler.getInstance().stopTracker(tracker.getTracker(),
                                                    (tracker1, documentId) -> runOnUiThread(() -> {
                                                        Toast.makeText(MainActivity.this, "Tracker stopped", Toast.LENGTH_LONG).show();
                                                        populateTrackers();
                                                    }),
                                                    (message, exception) -> runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show()));
                                        }).setNegativeButton("Cancel", (dialog1, which1) -> dialog1.dismiss())
                                        .create().show();
                            })
                            .setOnDismissListener(dialog -> ((ViewGroup) layout.getParent()).removeView(layout))
                            .create().show();
                });
            });
            ((RecyclerView) findViewById(R.id.main_tracker_recycler)).setAdapter(adapter);
            ((RecyclerView) findViewById(R.id.main_tracker_recycler)).setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
        }), (message, exception) -> runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    updatingMap.put("Trackers", false);
                }
        ));

        findViewById(R.id.main_tracker_info).setOnClickListener(v -> CommonUtils.showDocumentationAlert(MainActivity.this,
                "Add Tracker", List.of("This will add a new tracker. Old expenses with the same tracker name enabled in past will also show up.\n",
                        "1. Type Daily Budget for daily budget only! This name contains past records",
                        "2. Type Gather in name for any tracker where you need to gather an amount of money for a lumpsum. These will be shown first, after daily budget",
                        "3. Other items are shown in the order One Time -> Daily -> Weekly -> Monthly -> Quarterly -> Annually")));
    }

    private void populateView(boolean insertNewMonthIfNotExists) {
        populateStatus();   // circle things at the top
        populateAggregates(insertNewMonthIfNotExists);  // master aggregate -> monthly aggregate
        populateData();  // the individual income/expenses
        populateTrackers();
    }

    private void setupMonthYearPicker() {
        Calendar calendar = Calendar.getInstance();
        List<String> months = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        for (int i = 0; i < 12; i++) {
            calendar.set(Calendar.MONTH, i);
            months.add(simpleDateFormat.format(calendar.getTime()));
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, months);
        ((Spinner) findViewById(R.id.main_month_spinner)).setAdapter(monthAdapter);

        SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy", Locale.getDefault());
        List<String> years = new ArrayList<>();
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);
        for (int i = 0; i < 5; i++) {
            years.add(simpleDateFormat2.format(calendar.getTime()));
            calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, years);
        ((Spinner) findViewById(R.id.main_year_spinner)).setAdapter(yearAdapter);

        calendar = Calendar.getInstance();
        String currMonth = simpleDateFormat.format(calendar.getTime());
        String currYear = simpleDateFormat2.format(calendar.getTime());
        ((Spinner) findViewById(R.id.main_month_spinner)).setSelection(monthAdapter.getPosition(currMonth));
        ((Spinner) findViewById(R.id.main_year_spinner)).setSelection(yearAdapter.getPosition(currYear));

        findViewById(R.id.main_date_select_button).setOnClickListener(v -> {
            String selection = ((Spinner) findViewById(R.id.main_month_spinner)).getSelectedItem().toString() + " "
                    + ((Spinner) findViewById(R.id.main_year_spinner)).getSelectedItem().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            try {
                Calendar calendar1 = Calendar.getInstance();
                calendar1.setTime(sdf.parse(selection));
                String selected = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar1.getTime());
                ((TextView) findViewById(R.id.main_date)).setText(selected);
                populateView(false);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void populateToday() {
        String today = new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(Calendar.getInstance(Locale.getDefault()).getTime());
        ((TextView) findViewById(R.id.main_date)).setText(today);
    }

    private void populateAggregates(boolean insertNewMonthIfNotExists) {
        updatingMap.put("Aggregates", true);
        new MasterAggregatesDAO().getMasterAggregate(true, new ResultCallback<MasterAggregate>() {
            @Override
            public void onResult(MasterAggregate masterAggregate, String documentId) {
                updatingMap.put("Aggregates", false);
                MainActivity.this.masterAggregate = masterAggregate;
                MainActivity.this.masterAggregateDocumentId = documentId;
                RecyclerView recyclerView = findViewById(R.id.main_aggregate_recycler);
                List<UiAggregate> uiAggregates = new ArrayList<>();
                uiAggregates.add(new UiAggregate("Total Bank Balance", masterAggregate.getBalance()));
                uiAggregates.add(new UiAggregate("Total Invested", masterAggregate.getCategoricalUsage().get(CategoryEnum.INVESTMENT.getName()).getCurrent()));
                uiAggregates.add(new UiAggregate("Total income", masterAggregate.getIncome()));
                uiAggregates.add(new UiAggregate("Tax Liable Income", masterAggregate.getTaxLiableIncome()));
                recyclerView.setAdapter(new GenericRecyclerAdapter<>(MainActivity.this,
                        R.layout.item_aggregate, uiAggregates, ((context, viewHolder, position) -> {
                    ((TextView) viewHolder.itemView.findViewById(R.id.item_aggregate_name)).setText(uiAggregates.get(position).getName());
                    ((TextView) viewHolder.itemView.findViewById(R.id.item_aggregate_amount)).setText(String.valueOf(uiAggregates.get(position).getAmount()));
                }), null));
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
                int healthColor = CommonUtils.getHealthColor(masterAggregate.getCategoricalUsage(), masterAggregate.getBalance());
                ((CardView) findViewById(R.id.main_aggregate_health)).setCardBackgroundColor(getApplication().getColor(healthColor));
                populateMonth(masterAggregate, documentId, insertNewMonthIfNotExists);
            }

            @Override
            public void onError(String message, Exception exception) {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                updatingMap.put("Aggregates", false);
            }

            @Override
            public void onNoResultFound() {
                // Not used
            }
        });
    }

    private void populateMonth(MasterAggregate masterAggregate, String masterAggId, boolean insertNewMonthIfNotExists) {
        try {
            Date date = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).parse(
                    ((TextView) findViewById(R.id.main_date)).getText().toString());
            updatingMap.put("Aggregates", true);
            new MonthlyAggregatesDAO().getMonthlyAggregate(date, insertNewMonthIfNotExists, new ResultCallback<MonthlyAggregate>() {
                @Override
                public void onResult(MonthlyAggregate monthlyAggregate, String documentId) {
                    updatingMap.put("Aggregates", false);
                    if (monthlyAggregate == null)
                        findViewById(R.id.main_aggregates_layout).setVisibility(View.GONE);
                    else
                        findViewById(R.id.main_aggregates_layout).setVisibility(View.VISIBLE);
                    RecyclerView recyclerView = findViewById(R.id.main_month_recycler);
                    List<UiMonth> uiMonths = new ArrayList<>();
                    for (CategoryEnum category : CategoryEnum.values()) {
                        if (monthlyAggregate != null)
                            uiMonths.add(new UiMonth(category.getName(), CommonUtils.getCategoryCurrent(category, monthlyAggregate.getCategoricalUsage()), CommonUtils.getCategoryLimit(category, monthlyAggregate.getCategoricalUsage())));
                        else
                            uiMonths.add(new UiMonth(category.getName(), 0, 0));
                    }
                    recyclerView.setAdapter(new GenericRecyclerAdapter<>(MainActivity.this,
                            R.layout.item_balance_left, uiMonths, ((context, viewHolder, position) -> {
                        ((TextView) viewHolder.itemView.findViewById(R.id.item_balance_name)).setText(uiMonths.get(position).getName());
                        ((TextView) viewHolder.itemView.findViewById(R.id.item_balance_actual_amount)).setText(uiMonths.get(position).getCurrent());
                        ((TextView) viewHolder.itemView.findViewById(R.id.item_balance_limit_amount)).setText(uiMonths.get(position).getLimit());
                        ((LinearProgressIndicator) viewHolder.itemView.findViewById(R.id.item_balance_progress)).setProgress((int) (uiMonths.get(position).getProgress() * 100));
                    }), null));
                    recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
                    int healthColor = monthlyAggregate != null ? CommonUtils.getHealthColor(monthlyAggregate.getCategoricalUsage(), monthlyAggregate.getBalance()) : android.R.color.holo_green_dark;
                    ((CardView) findViewById(R.id.main_month_health)).setCardBackgroundColor(getApplication().getColor(healthColor));
                }

                @Override
                public void onError(String message, Exception exception) {
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    updatingMap.put("Aggregates", false);
                }

                @Override
                public void onNoResultFound() {
                    // Not used
                }
            }, (inserted, documentId) -> {
                if (inserted) {
                    populateAggregates(false);
                    populateStatus();
                }
            }, masterAggregate, masterAggId, true);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void populateStatus() {
        List<UiMonthSelector> months = new ArrayList<>();
        GenericRecyclerAdapter<UiMonthSelector> adapter = new GenericRecyclerAdapter<>(this, R.layout.item_small_circle,
                months, (context, viewHolder, position) -> {
            ((CardView) viewHolder.itemView.findViewById(R.id.item_circle))
                    .setCardBackgroundColor(getApplication().getColor(months.get(position).getHealthColor()));
            String name = months.get(position).getMonthlyAggregate().getName();
            name = name.substring(0, 3) + "'" + name.substring(name.indexOf(' ') + 3);
            ((TextView) viewHolder.itemView.findViewById(R.id.item_circle_label)).setText(name);
        }, viewHolder -> {
            viewHolder.itemView.findViewById(R.id.item_circle).setOnClickListener(v -> {
                ((TextView) findViewById(R.id.main_date)).setText(months.get(viewHolder.getAdapterPosition()).getMonthlyAggregate().getName());
                populateAggregates(false);
                populateData();
            });
        });
        updatingMap.put("Monthly", true);
        new MonthlyAggregatesDAO().getAvailableMonths((monthlyAggregates, documentId) -> {
                    updatingMap.put("Monthly", false);
                    monthlyAggregates.forEach(monthlyAggregate -> months.add(new UiMonthSelector(monthlyAggregate)));
                    adapter.notifyDataSetChanged();
                },
                (message, exception) -> {
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    updatingMap.put("Monthly", false);
                });

        ((RecyclerView) findViewById(R.id.main_history_status_recycler)).setAdapter(adapter);
        ((RecyclerView) findViewById(R.id.main_history_status_recycler)).setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false
        ));
    }

    private void populateData() {
        List<UiData> uiDataList = new ArrayList<>();
        RecyclerView recyclerView = findViewById(R.id.main_data_recycler);
        FilterableRecyclerBehaviour<UiData> filterableRecyclerBehaviour = new FilterableRecyclerBehaviour.Builder<UiData>((object, filterString) -> {
            Pattern p = Pattern.compile(Pattern.quote(filterString), Pattern.CASE_INSENSITIVE);
            return p.matcher(object.getHeading()).find() || p.matcher(object.getMessage()).find()
                    || p.matcher(String.valueOf(object.getFooter())).find();
        })
                .addEditText(this, findViewById(R.id.main_search))
                .addConsumer(displayData -> {
                    double sum = displayData.stream().map(data -> data.getData().getAmount()).reduce(Double::sum).orElse(0.0);
                    ((TextView) findViewById(R.id.main_sum)).setText("Sum: " + String.format(Locale.getDefault(), "%.2f", sum));
                })
                .build();
        GenericRecyclerAdapter<UiData> adapter = new GenericRecyclerAdapter<>(MainActivity.this,
                R.layout.item_data, uiDataList, (context, viewHolder, position) -> {
            ((TextView) viewHolder.itemView.findViewById(R.id.item_data_heading)).setText(uiDataList.get(position).getHeading());
            ((TextView) viewHolder.itemView.findViewById(R.id.item_data_footer)).setText(uiDataList.get(position).getFooter());
            ((TextView) viewHolder.itemView.findViewById(R.id.item_data_message)).setText(uiDataList.get(position).getMessage());
            if (uiDataList.get(position).getFooter().isEmpty())
                viewHolder.itemView.findViewById(R.id.item_data_footer).setVisibility(View.GONE);
            else
                viewHolder.itemView.findViewById(R.id.item_data_footer).setVisibility(View.VISIBLE);
        }, viewHolder -> viewHolder.itemView.findViewById(R.id.item_data_card).setOnLongClickListener(v -> {
            editData(uiDataList.get(filterableRecyclerBehaviour.getOriginalItemPosition(viewHolder.getAdapterPosition())));
            return true;
        }));
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));
        adapter.addBehaviour(filterableRecyclerBehaviour);
        try {
            Date date = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).parse(
                    ((TextView) findViewById(R.id.main_date)).getText().toString());
            updatingMap.put("Data", true);
            IncomeHandler.getInstance().getMonthIncome(date, (incomes, documentId) ->
                    ExpenseHandler.getInstance().getMonthExpense(date, (expenses, documentId1) -> {
                        updatingMap.put("Data", false);
                        incomes.forEach(income -> {
                            String text = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(income.getDate())
                                    + " | " + income.getName();
                            if (income.isRecurring())
                                text = text + " (Recurring till "
                                        + new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                        .format(income.getRecur().getTo())
                                        + " with period " + income.getRecur().getPeriod()
                                        + ")";
                            String heading = String.format(Locale.getDefault(), "%.2f", income.getAmount()) + " | Income" + (income.isRecurring() ? " (R)" : "");
                            uiDataList.add(new UiData(heading, text, "", income));
                        });
                        expenses.forEach(expense -> {
                            String text = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(expense.getDate())
                                    + " | " + expense.getName();
                            if (expense.isRecurring())
                                text = text + " (Recurring till "
                                        + new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                        .format(expense.getRecur().getTo())
                                        + " with period " + expense.getRecur().getPeriod()
                                        + ")";
                            String heading = String.format(Locale.getDefault(), "%.2f", expense.getAmount()) + " | " + expense.getCategory() + (expense.isRecurring() ? " (R)" : "");
                            String footer = String.join(", ", expense.getTrackers());
                            uiDataList.add(new UiData(heading, text, footer, expense));
                        });
                        uiDataList.sort(Arrays.stream(DataSortType.values()).findFirst().orElse(null).getComparator());
                        filterableRecyclerBehaviour.updateOriginalList(uiDataList);
                    }, (message, exception) -> {
                        updatingMap.put("Data", false);
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    }), (message, exception) -> {
                updatingMap.put("Data", false);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            });
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        String searchText = ((EditText) findViewById(R.id.main_search)).getText().toString();
        if (!searchText.equals(""))
            filterableRecyclerBehaviour.filter(searchText);

        sortableBehaviour(uiDataList, filterableRecyclerBehaviour);
    }

    private void sortableBehaviour(List<UiData> uiDataList, FilterableRecyclerBehaviour<UiData> filterableRecyclerBehaviour) {
        MaterialAutoCompleteTextView sortTextView = findViewById(R.id.main_sort);
        String[] sortTypes = Arrays.stream(DataSortType.values()).map(DataSortType::getName).toArray(String[]::new);
        sortTextView.setSimpleItems(sortTypes);
        sortTextView.setText(sortTypes[0], false);
        sortTextView.setOnItemClickListener((parent, view, position, id) -> {
            DataSortType type = Arrays.stream(DataSortType.values())
                    .filter(type1 -> type1.getName().equals(sortTypes[position]))
                    .findFirst().orElse(null);
            uiDataList.sort(type.getComparator());
            filterableRecyclerBehaviour.updateOriginalList(uiDataList);
        });
    }

    private void editData(UiData uiData) {
        Intent intent = new Intent(MainActivity.this, AddActivity.class);
        intent.putExtra("action", "Edit");
        if (uiData.getData() instanceof Income) {
            Income income = (Income) uiData.getData();
            intent.putExtra("type", "Income");
            intent.putExtra("data", income);
        } else if (uiData.getData() instanceof Expense) {
            Expense expense = (Expense) uiData.getData();
            intent.putExtra("type", "Expense");
            intent.putExtra("data", expense);
        }
        launcher.launch(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_add_goal) {
            launcher.launch(new Intent(MainActivity.this, AddGoalActivity.class));
        } else if (id == R.id.action_view_goals) {
            launcher.launch(new Intent(MainActivity.this, ViewGoalsActivity.class));
        } else if (id == R.id.action_view_sms) {
            launcher.launch(new Intent(MainActivity.this, SmsReadActivity.class));
        } else if (id == R.id.action_view_refresh) {
            if (updatingMap.values().stream().anyMatch(Boolean::booleanValue))
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("NO")
                        .setMessage("Wait for the previous refresh to finish")
                        .create().show();
            else
                populateView(false);
        } else if (id == R.id.action_sms_pattern) {
            launcher.launch(new Intent(MainActivity.this, SmsPatternActivity.class));
        } else if (id == R.id.action_edit_bank_balance) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.alert_edit_bank_balance, null, false);
            ((EditText) view.findViewById(R.id.alert_amount_text)).setText(String.valueOf(masterAggregate.getBalance()));
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Edit Bank Balance")
                    .setView(view)
                    .setPositiveButton("Confirm", (dialog, i) -> {
                        String amt = ((EditText) view.findViewById(R.id.alert_amount_text)).getText().toString();
                        if (amt.equals(""))
                            Toast.makeText(MainActivity.this, "Amount entered is blank", Toast.LENGTH_LONG).show();
                        else {
                            MasterAggregateHandler.getInstance().editBankBalance(Double.parseDouble(amt), masterAggregate, masterAggregateDocumentId,
                                    ((result, documentId) -> populateView(false)),
                                    ((message, exception) -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show()));
                        }
                    }).setNegativeButton("Close", ((dialog, i) -> dialog.dismiss()))
                    .create().show();
        } else if (id == R.id.action_version) {
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                new AlertDialog.Builder(this)
                        .setTitle("Version")
                        .setMessage(versionName)
                        .setPositiveButton("Okay", (dialog, which) -> dialog.dismiss())
                        .create().show();
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return super.onOptionsItemSelected(item);
    }
}