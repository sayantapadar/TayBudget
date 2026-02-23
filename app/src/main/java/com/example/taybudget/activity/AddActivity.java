package com.example.taybudget.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.taybudget.R;
import com.example.taybudget.data.handler.ExpenseHandler;
import com.example.taybudget.data.handler.IncomeHandler;
import com.example.taybudget.data.handler.TrackerHandler;
import com.example.taybudget.data.handler.interfaces.TaskCompletionCallback;
import com.example.taybudget.data.model.Data;
import com.example.taybudget.data.model.Expense;
import com.example.taybudget.data.model.Income;
import com.example.taybudget.data.model.Sms;
import com.example.taybudget.enums.CategoryEnum;
import com.example.taybudget.tools.CommonConstants;
import com.example.taybudget.tools.GenericRecyclerAdapter;
import com.example.taybudget.ui.model.UiTracker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AddActivity extends AppCompatActivity {
    private String action;
    private String type;  // only populated from sms
    private Data data;
    private boolean clickable = true;
    GenericRecyclerAdapter<UiTracker> trackerAdapter;
    List<UiTracker> trackers = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        CommonConstants.init();

        action = getIntent().getStringExtra("action");
        if ("Add".equals(action)) {
            populateSpinner();
            populateFromSms();
        } else if ("Edit".equals(action)) {
            populateHeading();
            populateEditFields();
        }

        populateCategory();
        recurringBehaviour();
        tagsBehaviour();
        dates();
        addButton();
        if (TrackerHandler.getInstance().getTrackerNames().isEmpty())
            TrackerHandler.getInstance().getTrackers(Calendar.getInstance(Locale.getDefault()).getTime(), (map, message) -> trackers(),
                    (message, exception) -> Toast.makeText(AddActivity.this, message, Toast.LENGTH_LONG).show());
        else
            trackers();
    }

    private void trackers() {
        findViewById(R.id.add_trackers_recycler).setVisibility(View.VISIBLE);
        trackers = TrackerHandler.getInstance().getTrackerNames().stream().map(UiTracker::new).collect(Collectors.toList());

        if ("Add".equals(action)) {
            for (UiTracker tracker : trackers) {
                if (tracker.getName().equals(CommonConstants.DAILY_BUDGET_TRACKER))
                    tracker.setSelected(true);
            }
        }

        if (data == null || data instanceof Expense) {
            if (data != null) {
                for (UiTracker tracker : trackers) {
                    if (((Expense) data).getTrackers().contains(tracker.getName()))
                        tracker.setSelected(true);
                }
            }

            runOnUiThread(() -> {
                trackerAdapter = new GenericRecyclerAdapter<>(AddActivity.this,
                        R.layout.item_tracker_select, trackers, (context, viewHolder, position) -> {
                    ((TextView) viewHolder.itemView.findViewById(R.id.item_tracker_select_name)).setText(trackers.get(position).getName());
                    if (trackers.get(position).isSelected()) {
                        viewHolder.itemView.findViewById(R.id.item_tracker_select_card).setBackgroundColor(getResources().getColor(R.color.yellow_light));
                    } else {
                        viewHolder.itemView.findViewById(R.id.item_tracker_select_card).setBackgroundColor(getResources().getColor(R.color.white));
                    }
                }, viewHolder -> viewHolder.itemView.setOnClickListener(view -> {
                    IntStream.range(0,trackers.size()).filter(i -> i != viewHolder.getAdapterPosition()).forEach(i -> trackers.get(i).setSelected(false));
                    trackers.get(viewHolder.getAdapterPosition()).setSelected(!trackers.get(viewHolder.getAdapterPosition()).isSelected());
                    trackerAdapter.notifyDataSetChanged();
                }));
                ((RecyclerView) findViewById(R.id.add_trackers_recycler)).setAdapter(trackerAdapter);
                ((RecyclerView) findViewById(R.id.add_trackers_recycler)).setLayoutManager(new LinearLayoutManager(AddActivity.this, LinearLayoutManager.HORIZONTAL, false));
            });
        }
    }

    private void populateFromSms() {
        Sms sms = (Sms) getIntent().getSerializableExtra("sms");
        if (sms != null) {
            findViewById(R.id.add_spinner_type).setVisibility(View.GONE);
            findViewById(R.id.add_text_type).setVisibility(View.VISIBLE);
            type = getIntent().getStringExtra("type");
            ((TextView) findViewById(R.id.add_text_type)).setText(("Adding " + type));
            if (sms.getExtractedName() != null)
                ((EditText) findViewById(R.id.add_name)).setText(sms.getExtractedName());
            if (sms.getExtractedAmount() != 0)
                ((EditText) findViewById(R.id.add_amount)).setText(String.valueOf(sms.getExtractedAmount()));
            if (sms.getTag() != null)
                ((TextView) findViewById(R.id.add_tags_selected_recycler)).setText(sms.getTag() + "\n" + sms.getId());
            ((EditText) findViewById(R.id.add_date_text)).setText(CommonConstants.DEFAULT_DATE_DISPLAY.format(sms.getDate()));
        }
    }

    private void populateHeading() {
        findViewById(R.id.add_spinner_type).setVisibility(View.GONE);
        findViewById(R.id.add_text_type).setVisibility(View.VISIBLE);
        type = getIntent().getStringExtra("type");
        ((TextView) findViewById(R.id.add_text_type)).setText(("Editing " + type));
        if ("Income".equals(type)) {
            data = (Income) getIntent().getSerializableExtra("data");

        } else if ("Expense".equals(type)) {
            data = (Expense) getIntent().getSerializableExtra("data");
            typeBehaviour(type);
        }
    }

    private void populateSpinner() {
        Spinner spinner = (Spinner) findViewById(R.id.add_spinner_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_add, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                typeBehaviour(spinner.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void typeBehaviour(String type) {
        if (type.contains("Expense")) {
            findViewById(R.id.add_taxable).setVisibility(View.GONE);
            findViewById(R.id.add_spinner_category).setVisibility(View.VISIBLE);
            findViewById(R.id.add_trackers_recycler).setVisibility(View.VISIBLE);
            findViewById(R.id.add_tags_selected_recycler).setVisibility(View.VISIBLE);
            if (data != null) {
                ((TextView) findViewById(R.id.add_tags_selected_recycler)).setText(String.join("\n", ((Expense) data).getTags()));
            }
        } else {
            findViewById(R.id.add_taxable).setVisibility(View.VISIBLE);
            findViewById(R.id.add_spinner_category).setVisibility(View.GONE);
            findViewById(R.id.add_trackers_recycler).setVisibility(View.GONE);
            findViewById(R.id.add_tags_selected_recycler).setVisibility(View.GONE);
            if (data != null)
                ((CheckBox) findViewById(R.id.add_taxable)).setChecked(((Income) data).isTaxable());
        }
    }

    private void populateEditFields() {
        if ("Edit".equals(action)) {
            ((EditText) findViewById(R.id.add_name)).setText(data.getName());
            ((EditText) findViewById(R.id.add_amount)).setText(String.valueOf(data.getAmount()));
            ((EditText) findViewById(R.id.add_date_text)).setText(CommonConstants.DEFAULT_DATE_DISPLAY.format(data.getDate()));
            ((EditText) findViewById(R.id.add_date_end_text)).setText(CommonConstants.DEFAULT_DATE_DISPLAY.format(data.getRecur().getTo()));
        }
    }

    private void populateCategory() {
        Spinner spinner = (Spinner) findViewById(R.id.add_spinner_category);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(AddActivity.this, android.R.layout.simple_spinner_item,
                Arrays.stream(CategoryEnum.values()).map(CategoryEnum::getName).collect(Collectors.toList()));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(1);

        if ("Edit".equals(action) && "Expense".equals(type)) {
            String category = ((Expense) data).getCategory();
            spinner.setSelection(adapter.getPosition(category));
        }
    }

    private void recurringBehaviour() {
        ((CheckBox) findViewById(R.id.add_recurring)).setOnCheckedChangeListener(
                (compoundButton, b) -> {
                    if (b) {
                        findViewById(R.id.add_period).setVisibility(View.VISIBLE);
                        findViewById(R.id.add_date_end).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.add_period).setVisibility(View.GONE);
                        findViewById(R.id.add_date_end).setVisibility(View.GONE);
                    }
                });
        if ("Edit".equals(action)) {
            if (data.isRecurring()) {
                findViewById(R.id.add_period).setVisibility(View.VISIBLE);
                findViewById(R.id.add_date_end).setVisibility(View.VISIBLE);
                ((EditText) findViewById(R.id.add_date_end_text)).setText(
                        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(data.getRecur().getTo()));
                ((EditText) findViewById(R.id.add_period_text)).setText(String.valueOf(data.getRecur().getPeriod()));
                ((CheckBox) findViewById(R.id.add_recurring)).setChecked(data.isRecurring());
            } else {
                findViewById(R.id.add_period).setVisibility(View.GONE);
                findViewById(R.id.add_date_end).setVisibility(View.GONE);
            }
        }
    }

    private void tagsBehaviour() {

    }

    private void dates() {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        Calendar calendarEnd = Calendar.getInstance(Locale.getDefault());
        String dateText = ((EditText) findViewById(R.id.add_date_text)).getText().toString();
        String dateEndText = ((EditText) findViewById(R.id.add_date_end_text)).getText().toString();
        try {
            if (!dateText.equals(""))
                calendar.setTime(CommonConstants.DEFAULT_DATE_DISPLAY.parse(dateText));
            if (!dateEndText.equals(""))
                calendarEnd.setTime(CommonConstants.DEFAULT_DATE_DISPLAY.parse(dateEndText));
        } catch (Exception ignore) {
        }

        ((EditText) findViewById(R.id.add_date_text)).setText(CommonConstants.DEFAULT_DATE_DISPLAY.format(calendar.getTime()));
        ((EditText) findViewById(R.id.add_date_end_text)).setText(CommonConstants.DEFAULT_DATE_DISPLAY.format(calendarEnd.getTime()));

        findViewById(R.id.add_date_text).setOnClickListener(view -> {
            Log.d("AddActivity", "Clicked add_date");
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddActivity.this,
                    (view1, year1, monthOfYear, dayOfMonth) ->
                            ((EditText) findViewById(R.id.add_date_text))
                                    .setText(new SimpleDateFormat("dd/MM/yyyy",
                                            Locale.getDefault())
                                            .format(new GregorianCalendar(year1, monthOfYear, dayOfMonth).getTime())),
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        findViewById(R.id.add_date_end_text).setOnClickListener(view -> {
            Log.d("AddActivity", "Clicked add_date_end");
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddActivity.this,
                    (view1, year1, monthOfYear, dayOfMonth) ->
                            ((EditText) findViewById(R.id.add_date_end_text))
                                    .setText(new SimpleDateFormat("dd/MM/yyyy",
                                            Locale.getDefault())
                                            .format(new GregorianCalendar(year1, monthOfYear, dayOfMonth).getTime())),
                    calendarEnd.get(Calendar.YEAR), calendarEnd.get(Calendar.MONTH), calendarEnd.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
    }

    private void addButton() {
        findViewById(R.id.add_button).setOnClickListener(v -> {
            if (clickable) {
                try {
                    String type;
                    if ("Add".equals(action))
                        type = ((Spinner) findViewById(R.id.add_spinner_type)).getSelectedItem().toString();
                    else {
                        type = this.type;
                    }
                    String category = ((Spinner) findViewById(R.id.add_spinner_category)).getSelectedItem().toString();
                    String name = ((EditText) findViewById(R.id.add_name)).getText().toString();
                    double amount = Double.parseDouble(((EditText) findViewById(R.id.add_amount)).getText().toString().equals("") ? "0" : ((EditText) findViewById(R.id.add_amount)).getText().toString());
                    Date date = CommonConstants.DEFAULT_DATE_DISPLAY.parse(((EditText) findViewById(R.id.add_date_text)).getText().toString());
                    boolean recurring = ((CheckBox) findViewById(R.id.add_recurring)).isChecked();
                    int recurringPeriod = Integer.parseInt(((EditText) findViewById(R.id.add_period_text)).getText().toString().equals("") ? "0" : ((EditText) findViewById(R.id.add_period_text)).getText().toString());
                    Date recurringDateEnd = CommonConstants.DEFAULT_DATE_DISPLAY.parse(((EditText) findViewById(R.id.add_date_end_text)).getText().toString());
                    boolean taxable = ((CheckBox) findViewById(R.id.add_taxable)).isChecked();
                    List<String> tags = Arrays.asList(((TextView) findViewById(R.id.add_tags_selected_recycler)).getText().toString().split("\n"));
                    List<String> trackersSelected = trackers != null ? trackers.stream().filter(UiTracker::isSelected).map(UiTracker::getName).collect(Collectors.toList()) : new ArrayList<>();

                    new AlertDialog.Builder(AddActivity.this)
                            .setTitle("Confirm " + type)
                            .setMessage("Amount: " + amount +
                                    (type.contains("Expense") ? "\nCategory: " + category : "\nIncome") +
                                    (recurring ? "\nRecurring till " + CommonConstants.DEFAULT_DATE_DISPLAY.format(recurringDateEnd) + " with period " + recurringPeriod : ""))
                            .setPositiveButton("Yes", (dialog, which) -> {
                                clickable = false;
                                if (type.contains("Expense")) {
                                    if ("Add".equals(action)) {
                                        ExpenseHandler.getInstance().addExpense(category, name, amount, date, recurring,
                                                recurringPeriod, recurringDateEnd, tags, trackersSelected, message ->
                                                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show(), taskCompletionCallback);
                                    } else if ("Edit".equals(action)) {
                                        ExpenseHandler.getInstance().editExpense((Expense) data, category, name, amount, date, recurring,
                                                recurringPeriod, recurringDateEnd, tags, trackersSelected, message ->
                                                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show(), taskCompletionCallback);
                                    }

                                } else {
                                    if ("Add".equals(action)) {
                                        IncomeHandler.getInstance().addIncome(name, amount, date, recurring,
                                                recurringPeriod, recurringDateEnd, taxable, message ->
                                                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show(), taskCompletionCallback);
                                    } else if ("Edit".equals(action)) {
                                        IncomeHandler.getInstance().editIncome((Income) data, name, amount, date, recurring,
                                                recurringPeriod, recurringDateEnd, taxable, message ->
                                                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show(), taskCompletionCallback);
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .create().show();


                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        if ("Edit".equals(action)) {
            findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
            findViewById(R.id.delete_button).setOnClickListener(v -> {
                new AlertDialog.Builder(AddActivity.this)
                        .setTitle("Confirm Delete")
                        .setMessage("Are you sure you want to delete this " + type)
                        .setPositiveButton("Yes", (dialog, which) -> {
                            clickable = false;
                            if (type.contains("Expense")) {
                                ExpenseHandler.getInstance().deleteExpense((Expense) data, taskCompletionCallback);
                            } else {
                                IncomeHandler.getInstance().deleteIncome((Income) data, taskCompletionCallback);
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .create().show();
            });

        }
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