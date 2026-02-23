package com.example.taybudget.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.example.taybudget.R;
import com.example.taybudget.data.handler.SmsPatternHandler;
import com.example.taybudget.data.model.CreditCycle;
import com.example.taybudget.data.model.Sms;
import com.example.taybudget.tools.CommonConstants;
import com.example.taybudget.tools.CommonUtils;
import com.example.taybudget.tools.GenericRecyclerAdapter;
import com.example.taybudget.ui.model.UiSmsPattern;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SmsPatternActivity extends AppCompatActivity {

    List<UiSmsPattern> uiSmsPatternList = new ArrayList<>();
    GenericRecyclerAdapter<UiSmsPattern> uiSmsPatternGenericRecyclerAdapter;
    private ActivityResultLauncher<Intent> launcher;
    private UiSmsPattern smsPatternState = null;
    private boolean thisCycleState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_pattern);

        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> testRegex(smsPatternState, thisCycleState));

        SmsPatternHandler.getInstance().getSmsPatterns((result, documentId) -> {
            uiSmsPatternList.addAll(result.stream().map(UiSmsPattern::new).collect(Collectors.toList()));
            uiSmsPatternGenericRecyclerAdapter = new GenericRecyclerAdapter<>(this, R.layout.item_add_sms_pattern, uiSmsPatternList,
                    (context, viewHolder, position) -> {
                        ((EditText) viewHolder.itemView.findViewById(R.id.item_sms_pattern_tag_text)).setText(uiSmsPatternList.get(position).getTag());
                        ((EditText) viewHolder.itemView.findViewById(R.id.item_sms_pattern_regex_text)).setText(String.join(" ## ", uiSmsPatternList.get(position).getPatterns()));
                        ((EditText) viewHolder.itemView.findViewById(R.id.item_sms_pattern_credit_cycle_text)).setText(uiSmsPatternList.get(position).getCreditCycle());
                        (viewHolder.itemView.findViewById(R.id.item_sms_pattern_layout)).setBackgroundColor(getResources().getColor(uiSmsPatternList.get(position).getBackgroundColour()));
                    }, viewHolder -> {
                ((EditText) viewHolder.itemView.findViewById(R.id.item_sms_pattern_tag_text)).addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        String tag = charSequence.toString();
                        uiSmsPatternList.get(viewHolder.getAdapterPosition()).setTag(tag);
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
                ((EditText) viewHolder.itemView.findViewById(R.id.item_sms_pattern_regex_text)).addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        String patterns = charSequence.toString();
                        uiSmsPatternList.get(viewHolder.getAdapterPosition()).setPatterns(Arrays.asList(patterns.split(" ## ")));
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
                viewHolder.itemView.findViewById(R.id.item_sms_pattern_credit_cycle_text).setOnClickListener(v -> {
                    Log.d("SMSPatternActivity", "credit cycle clicked");
                    MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                            .setTitleText("Select Credit Cycle")
                            .build();
                    datePicker.addOnPositiveButtonClickListener(selection -> {
                        Calendar calendar = Calendar.getInstance(Locale.getDefault());
                        calendar.setTimeInMillis(selection);
                        ((EditText) viewHolder.itemView.findViewById(R.id.item_sms_pattern_credit_cycle_text)).setText(CommonConstants.DEFAULT_DAY_ONLY_DISPLAY.format(calendar.getTime()));
                        uiSmsPatternList.get(viewHolder.getAdapterPosition()).setCreditCycle(CommonConstants.DEFAULT_DAY_ONLY_DISPLAY.format(calendar.getTime()));
                    });
                    datePicker.show(getSupportFragmentManager(), "CreditCycleDatePicker");
                });
                viewHolder.itemView.findViewById(R.id.item_sms_pattern_test_this_cycle).setOnClickListener(view -> {
                    testRegex(uiSmsPatternList.get(viewHolder.getAdapterPosition()), true);
                });
                viewHolder.itemView.findViewById(R.id.item_sms_pattern_test_last_cycle).setOnClickListener(view -> {
                    testRegex(uiSmsPatternList.get(viewHolder.getAdapterPosition()), false);
                });
            });
            ((RecyclerView) findViewById(R.id.sms_pattern_recycler)).setAdapter(uiSmsPatternGenericRecyclerAdapter);
            ((RecyclerView) findViewById(R.id.sms_pattern_recycler)).setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

            addButton();
            validateButton();
            confirmButton();
        }, (message, exception) -> Toast.makeText(SmsPatternActivity.this, message, Toast.LENGTH_LONG).show());
    }

    private void testRegex(UiSmsPattern smsPattern, boolean thisCycle) {
        Date start, end;
        if (!smsPattern.getCreditCycle().equals("")) {
            CreditCycle cycle = thisCycle
                    ? smsPattern.getCurrentCreditCycleFor(Calendar.getInstance(Locale.getDefault()).getTime())
                    : smsPattern.getPreviousCreditCycleFor(Calendar.getInstance(Locale.getDefault()).getTime());
            start = cycle.getStart();
            end = cycle.getEnd();
        } else {
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            end = calendar.getTime();
            calendar.add(Calendar.MONTH, -1);
            start = calendar.getTime();
        }
        List<Sms> filteredSms = SmsPatternHandler.getInstance().testRegexOnMessages(this, smsPattern.getPatterns(), start, end);
        filteredSms.forEach(sms -> sms.setTag(smsPattern.getTag()));
        filteredSms.forEach(CommonUtils::extractValueFromSms);
        filteredSms.forEach(CommonUtils::extractNameFromSms);
        filteredSms.sort(Comparator.comparing(Sms::getDate).reversed());
        double totalValue = filteredSms.stream().map(Sms::getExtractedAmount).reduce(Double::sum).orElse(0.0);
        new AlertDialog.Builder(this)
                .setTitle(filteredSms.size() + " Messages -> Rs. " + totalValue + "\n" + CommonConstants.DEFAULT_DATE_DISPLAY.format(start) + " -> " + CommonConstants.DEFAULT_DATE_DISPLAY.format(end))
                .setItems(filteredSms.stream().map(sms -> "\n" + sms.getExtractedAmount() + "   " + sms.getExtractedName() + "\n" + CommonConstants.DEFAULT_DATE_DISPLAY.format(sms.getDate()) + "\n" + sms.getText()).toArray(String[]::new), ((dialogInterface, i) -> {
                    Sms sms = filteredSms.get(i);
                    Intent intent = new Intent(this, AddActivity.class);
                    intent.putExtra("action", "Add");
                    intent.putExtra("type", "Expense");
                    intent.putExtra("sms", sms);
                    launcher.launch(intent);
                    smsPatternState = smsPattern;
                    thisCycleState = thisCycle;
                })).create().show();
    }

    private void addButton() {
        findViewById(R.id.sms_pattern_add_button).setOnClickListener(view -> {
            uiSmsPatternList.add(new UiSmsPattern());
            uiSmsPatternGenericRecyclerAdapter.notifyItemInserted(uiSmsPatternList.size() - 1);
            ((RecyclerView) findViewById(R.id.sms_pattern_recycler)).smoothScrollToPosition(uiSmsPatternList.size() - 1);
        });
    }

    private void validateButton() {
        findViewById(R.id.sms_pattern_validate_button).setOnClickListener(view -> {
            new AlertDialog.Builder(SmsPatternActivity.this)
                    .setTitle("Validation Rules")
                    .setItems(new String[]{"Items without a tag will be removed. This can be used to remove existing regex functions.",
                            "Removed tags will show as Red",
                            "New tags will show as Green",
                            "Modified / Updated tags will show as Yellow"}, null)
                    .setPositiveButton("Proceed", ((dialogInterface, i) -> {
                        List<UiSmsPattern> originalList = new ArrayList<>(uiSmsPatternList);
                        uiSmsPatternList.removeIf(uiSmsPattern -> uiSmsPattern.getTag() == null || uiSmsPattern.getTag().equals(""));
                        List<String> errors = SmsPatternHandler.getInstance().validateChanges(uiSmsPatternList);
                        if (errors.size() > 0) {
                            uiSmsPatternList.clear();
                            uiSmsPatternList.addAll(originalList);
                        }
                        uiSmsPatternGenericRecyclerAdapter.notifyDataSetChanged();
                    })).setNegativeButton("Close", ((dialogInterface, i) -> dialogInterface.dismiss()))
                    .create().show();
        });
    }

    private void confirmButton() {
        findViewById(R.id.sms_pattern_confirm_button).setOnClickListener(view -> {
            SmsPatternHandler.getInstance().confirmUpdates(((result, documentId) -> {
                Toast.makeText(SmsPatternActivity.this, "Successfully updated", Toast.LENGTH_LONG).show();
                finish();
            }), ((message, exception) -> Toast.makeText(SmsPatternActivity.this, message, Toast.LENGTH_LONG).show()));
        });
    }
}