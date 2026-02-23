package com.example.taybudget.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.taybudget.R;
import com.example.taybudget.data.handler.SmsPatternChangelog;
import com.example.taybudget.data.handler.SmsPatternHandler;
import com.example.taybudget.data.model.Expense;
import com.example.taybudget.data.model.Sms;
import com.example.taybudget.data.model.SmsPattern;
import com.example.taybudget.tools.CommonConstants;
import com.example.taybudget.tools.CommonUtils;
import com.example.taybudget.tools.GenericRecyclerAdapter;
import com.example.taybudget.tools.behaviour.recycler.filterable.FilterableRecyclerBehaviour;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsReadActivity extends AppCompatActivity {

    private static final int READ_SMS_PERMISSION_CODE = 1;
    private final List<Sms> smsList = new ArrayList<>();
    private final List<Expense> expenseAddList = new ArrayList<>();
    private final List<SmsPattern> smsPatterns = new ArrayList<>();
    private final List<SmsPatternChangelog> smsPatternChangelogs = new ArrayList<>();
    private long smsPatternsLastUpdated = 0;
    private long lastRead = -1;
    private GenericRecyclerAdapter<Sms> adapter;
    private FilterableRecyclerBehaviour<Sms> filterableRecyclerBehaviour;
    private ActivityResultLauncher<Intent> launcher;
    private int positionBeingAdded = -1;
    private boolean added = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_read);
        CommonConstants.init();

        loadFromSharedPrefs();
        SmsPatternHandler.getInstance().getSmsPatterns((result, documentId) -> SmsPatternHandler.getInstance().getSmsPatternChangelog(smsPatternsLastUpdated, (changelogs, documentId2) -> {
                    smsPatterns.addAll(result);
                    smsPatternChangelogs.addAll(changelogs);
                    setupRecycler();
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{android.Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
                    } else {
                        smsReadSequence();
                        Log.d("Sms", String.valueOf(smsList));
                    }
                }, (message, exception) -> Toast.makeText(SmsReadActivity.this, message, Toast.LENGTH_LONG).show()),
                (message, exception) -> Toast.makeText(SmsReadActivity.this, message, Toast.LENGTH_LONG).show());

        findViewById(R.id.sms_info_button).setOnClickListener(v -> CommonUtils.showDocumentationAlert(SmsReadActivity.this,
                "SMS Reader", List.of("This reads your messages, every time you open this screen\n",
                        "1. Click to add to expense",
                        "2. Long click to flag",
                        "3. Type $flag to see flagged items")));

        findViewById(R.id.sms_clear_button).setOnClickListener(v -> new AlertDialog.Builder(SmsReadActivity.this)
                .setTitle("Clear Messages")
                .setMessage("Confirm clearance of all message. Note - This cannot be undone.")
                .setPositiveButton("OK", (dialog, which) -> {
                    smsList.clear();
                    filterableRecyclerBehaviour.updateOriginalList(smsList);
                    storeSmsAfterUpdate();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create().show());

        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        processAfterAdding();
                        added = true;
                    }
                });
    }

    private void smsReadSequence() {
        readSmsForChangelog(smsPatternChangelogs);
        readSms();
    }

    private void setupRecycler() {
        RecyclerView recyclerView = findViewById(R.id.sms_view_recycler);
        filterableRecyclerBehaviour = new FilterableRecyclerBehaviour.Builder<Sms>((sms, filterString) -> {
            Pattern p = Pattern.compile(Pattern.quote(filterString), Pattern.CASE_INSENSITIVE);
            return p.matcher(sms.getText()).find() || p.matcher(String.valueOf(sms.getExtractedAmount())).find()
                    || p.matcher(CommonConstants.DEFAULT_DATE_DISPLAY.format(sms.getDate())).find()
                    || p.matcher(sms.getSender()).find();
        }).addEditText(this, findViewById(R.id.sms_search))
                .addCustomRule(s -> s.equals("$flag"), Sms::isFlag)
                .build();
        adapter = new GenericRecyclerAdapter<>(this, R.layout.item_data, smsList, (context, viewHolder, position) -> {
            viewHolder.itemView.findViewById(R.id.item_data_delete_icon).setVisibility(View.VISIBLE);
            ((TextView) viewHolder.itemView.findViewById(R.id.item_data_heading)).setText(smsList.get(position).getDisplayTitle());
            if (smsList.get(position).getTag().contains("upi") || smsList.get(position).getTag().contains("Upi") || smsList.get(position).getTag().contains("UPI")) {
                ((TextView) viewHolder.itemView.findViewById(R.id.item_data_heading)).setTextColor(getResources().getColor(R.color.sms_upi));
            } else if (smsList.get(position).getTag().contains("Credit") || smsList.get(position).getTag().contains("credit") || smsList.get(position).getTag().contains("CREDIT")) {
                ((TextView) viewHolder.itemView.findViewById(R.id.item_data_heading)).setTextColor(getResources().getColor(R.color.sms_credit));
            } else if (smsList.get(position).getTag().contains("Debit") || smsList.get(position).getTag().contains("debit") || smsList.get(position).getTag().contains("DEBIT")) {
                ((TextView) viewHolder.itemView.findViewById(R.id.item_data_heading)).setTextColor(getResources().getColor(R.color.sms_debit));
            }
            ((TextView) viewHolder.itemView.findViewById(R.id.item_data_message)).setText(smsList.get(position).getText());
            String timeSender = CommonConstants.DEFAULT_DATE_DISPLAY.format(smsList.get(position).getDate()) + " | " + smsList.get(position).getSender();
            ((TextView) viewHolder.itemView.findViewById(R.id.item_data_footer)).setText(timeSender);
            if (smsList.get(position).isFlag()) {
                viewHolder.itemView.findViewById(R.id.item_data_flag_icon).setVisibility(View.VISIBLE);
            } else {
                viewHolder.itemView.findViewById(R.id.item_data_flag_icon).setVisibility(View.GONE);
            }
        }, viewHolder -> {
            viewHolder.itemView.findViewById(R.id.item_data_delete_icon).setOnClickListener(v -> {
                Log.d("SmsActivity", "Delete");
                int position = filterableRecyclerBehaviour.getOriginalItemPosition(viewHolder.getAdapterPosition());
                new AlertDialog.Builder(this)
                        .setTitle("Confirm Delete")
                        .setMessage(smsList.get(position).getText() + "\n" + smsList.get(position).getExtractedAmount() + " | " + smsList.get(position).getSender())
                        .setPositiveButton("Delete", (dialog, which) -> {
                            smsList.remove(position);
                            filterableRecyclerBehaviour.updateOriginalList(smsList);
                            storeSmsAfterUpdate();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .create().show();
            });
            viewHolder.itemView.setOnClickListener(v -> {
                Log.d("SmsActivity", "Add");
                positionBeingAdded = filterableRecyclerBehaviour.getOriginalItemPosition(viewHolder.getAdapterPosition());
                Sms sms = smsList.get(positionBeingAdded);
                Intent intent = new Intent(this, AddActivity.class);
                intent.putExtra("action", "Add");
                intent.putExtra("type", "Expense");
                intent.putExtra("sms", sms);
                launcher.launch(intent);
            });
            viewHolder.itemView.setOnLongClickListener(v -> {
                int pos = filterableRecyclerBehaviour.getOriginalItemPosition(viewHolder.getAdapterPosition());
                smsList.get(pos).setFlag(!smsList.get(pos).isFlag());
                filterableRecyclerBehaviour.updateOriginalList(smsList);
                storeSmsAfterUpdate();
                return true;
            });
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapter.addBehaviour(filterableRecyclerBehaviour);
    }

    private Sms createSmsObject(Cursor cursor, boolean highlight) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
        String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
        calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)));
        Date date = calendar.getTime();
        return new Sms(address, body, date, id, highlight);
    }

    private void readSmsForChangelog(List<SmsPatternChangelog> changelogs) {
        // NOTE - did nothing for updates - new messages will anyway be process based on updated regex. But existing ones will not be modified
        // NOTE - did nothing for remove - new messages will not be using removed regex anymore. But existing ones will not be affected
        SmsPatternChangelog changelog = SmsPatternHandler.getInstance().mergeChangeLogs(changelogs);
        if (changelog.getNewPatterns() != null && !changelog.getNewPatterns().isEmpty()) {
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            calendar.add(Calendar.MONTH, -1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date dateStart = calendar.getTime();
            Date dateEnd = getStartTime();
            String filter = "date>=" + dateStart.getTime() + " and date<=" + dateEnd.getTime();
            ContentResolver contentResolver = getContentResolver();
            Cursor cursor = contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    null,
                    filter,
                    null,
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                Toast.makeText(this, "Read " + cursor.getCount() + " new messages for changelog", Toast.LENGTH_LONG).show();
                do {
                    Sms sms = createSmsObject(cursor, true);
                    validateAndProcess(sms, changelog.getNewPatterns());
                } while (cursor.moveToNext());
            }

            if (cursor != null) {
                cursor.close();
            }
            smsList.sort(Comparator.comparing(Sms::getDate).reversed());
            filterableRecyclerBehaviour.updateOriginalList(smsList);
            storeSmsPatternsLastUpdated();
        }
    }

    private void readSms() {
        Date dateStart = getStartTime();
        String filter = "date>=" + dateStart.getTime();
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                filter,
                null,
                null);

        if (cursor != null && cursor.moveToFirst()) {
            Toast.makeText(this, "Read " + cursor.getCount() + " new messages", Toast.LENGTH_LONG).show();
            do {
                Sms sms = createSmsObject(cursor, true);
                validateAndProcess(sms, smsPatterns);
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
        smsList.sort(Comparator.comparing(Sms::getDate).reversed());
        filterableRecyclerBehaviour.updateOriginalList(smsList);
        storeSmsAfterRead();
    }


    private void validateAndProcess(Sms sms, List<SmsPattern> patterns) {
        for (SmsPattern smsPattern : patterns) {
            if (smsPattern.getPatterns().stream().map(Pattern::compile).map(pattern -> pattern.matcher(sms.getText())).anyMatch(Matcher::matches)) {
                CommonUtils.extractValueFromSms(sms);
                CommonUtils.extractNameFromSms(sms);
                sms.setTag(smsPattern.getTag());
                smsList.add(sms);
                break;
            }
        }
    }

    private Date getStartTime() {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        if (lastRead != -1)
            calendar.setTimeInMillis(lastRead + 1);
        else {
            calendar.add(Calendar.MONTH, -1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        }
        return calendar.getTime();
    }

    private void storeSmsAfterRead() {
        SharedPreferences sharedPref = getSharedPreferences(CommonConstants.SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(CommonConstants.SHARED_PREFERENCES_SMS, new Gson().toJson(smsList));
        editor.putLong(CommonConstants.SHARED_PREFERENCES_SMS_LAST_READ, Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
        editor.apply();
    }

    private void storeSmsAfterUpdate() {
        SharedPreferences sharedPref = getSharedPreferences(CommonConstants.SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(CommonConstants.SHARED_PREFERENCES_SMS, new Gson().toJson(smsList));
        editor.apply();
    }

    private void storeSmsPatternsLastUpdated() {
        SharedPreferences sharedPref = getSharedPreferences(CommonConstants.SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(CommonConstants.SHARED_PREFERENCES_PATTERNS_LAST_UPDATED, Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
        editor.apply();
    }

    private void loadFromSharedPrefs() {
        SharedPreferences sharedPref = getSharedPreferences(CommonConstants.SHARED_PREFERENCES, MODE_PRIVATE);
        String smsJson = sharedPref.getString(CommonConstants.SHARED_PREFERENCES_SMS, "[]");
        smsPatternsLastUpdated = sharedPref.getLong(CommonConstants.SHARED_PREFERENCES_PATTERNS_LAST_UPDATED, 0);
        lastRead = sharedPref.getLong(CommonConstants.SHARED_PREFERENCES_SMS_LAST_READ, -1);
        smsList.addAll(new Gson().fromJson(smsJson, new TypeToken<List<Sms>>() {
        }.getType()));
        smsList.sort(Comparator.comparing(Sms::getDate).reversed());
        smsList.stream().filter(Sms::isHighlight).forEach(sms -> {
            if (CommonUtils.getDaysBetween(sms.getDate(), Calendar.getInstance().getTime()) >= 1)
                sms.setHighlight(false);
        });
    }

    private void clear() {
        SharedPreferences sharedPref = getSharedPreferences(CommonConstants.SHARED_PREFERENCES, MODE_PRIVATE);
        sharedPref.edit().clear().apply();
    }

    private void processAfterAdding() {
        smsList.remove(positionBeingAdded);
        filterableRecyclerBehaviour.updateOriginalList(smsList);
        storeSmsAfterUpdate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                smsReadSequence();
            }
        }
    }
}