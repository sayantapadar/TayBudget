package com.example.taybudget.data.handler;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.widget.Toast;

import com.example.taybudget.data.firestore.SmsPatternChangelogDAO;
import com.example.taybudget.data.firestore.SmsPatternDAO;
import com.example.taybudget.data.firestore.interfaces.ResultFailureCallback;
import com.example.taybudget.data.firestore.interfaces.ResultSuccessCallback;
import com.example.taybudget.data.model.Sms;
import com.example.taybudget.data.model.SmsPattern;
import com.example.taybudget.ui.model.UiSmsPattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SmsPatternHandler {
    private static volatile SmsPatternHandler INSTANCE = null;

    private SmsPatternHandler() {
    }

    public static SmsPatternHandler getInstance() {
        if (INSTANCE == null) {
            synchronized (SmsPatternHandler.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SmsPatternHandler();
                }
            }
        }
        return INSTANCE;
    }

    private final SmsPatternDAO smsPatternDAO = new SmsPatternDAO();
    private final SmsPatternChangelogDAO smsPatternChangelogDAO = new SmsPatternChangelogDAO();
    private final HashMap<String, String> originalMap = new HashMap<>();
    private boolean validated;
    private final List<Sms> smsMessages = new ArrayList<>();

    private final List<SmsPattern> newPatterns = new ArrayList<>();
    private final Map<String, SmsPattern> updatePatterns = new HashMap<>();
    private final Map<String, SmsPattern> removePatterns = new HashMap<>();

    public void getSmsPatterns(ResultSuccessCallback<List<SmsPattern>> successCallback, ResultFailureCallback failureCallback) {
        originalMap.clear();
        validated = false;
        newPatterns.clear();
        updatePatterns.clear();
        removePatterns.clear();
        smsMessages.clear();
        smsPatternDAO.getSmsPatterns((resultMap, documentId) -> {
            resultMap.forEach((id, smsPattern) -> originalMap.put(smsPattern.getTag(), id + " ## " + smsPattern.getCreditCycle() + " ### " + String.join(" ## ", smsPattern.getPatterns())));
            List<SmsPattern> results = new ArrayList<>(resultMap.values());
            results.sort(Comparator.comparing(SmsPattern::getTag));
            successCallback.onResult(results, null);
        }, failureCallback);
    }

    public void getSmsPatternChangelog(long timestamp, ResultSuccessCallback<List<SmsPatternChangelog>> successCallback, ResultFailureCallback failureCallback) {
        smsPatternChangelogDAO.getChangelogAfter(timestamp, (result, documentId) -> successCallback.onResult(result, null), failureCallback);
    }


    public List<String> validateChanges(List<UiSmsPattern> uiSmsPatternList) {
        Map<String, String> validationMap = new HashMap<>();
        List<String> validationErrors = new ArrayList<>();
        uiSmsPatternList.forEach(uiSmsPattern -> {
            if (validationMap.containsKey(uiSmsPattern.getTag())) {
                validationErrors.add("Duplicate tag found");
            }
            if (uiSmsPattern.getPatterns().stream().anyMatch(pattern -> pattern.contains(" ## ") || pattern.contains(" ### ")))
                validationErrors.add("Regex expression cannot contain ## or ###");
            validationMap.put(uiSmsPattern.getTag(), originalMap.getOrDefault(uiSmsPattern.getTag(), ""));
            if (!originalMap.containsKey(uiSmsPattern.getTag())) {
                uiSmsPattern.setBackgroundColourGreen();  // new item
                newPatterns.add((SmsPattern) uiSmsPattern);
            } else {
                String[] metadata = originalMap.get(uiSmsPattern.getTag()).split("###")[0].split("##");
                String documentId = metadata[0].trim();
                String previousCreditCycle = metadata[1].trim();
                String previousRegex = originalMap.get(uiSmsPattern.getTag()).substring(originalMap.get(uiSmsPattern.getTag()).indexOf("###") + 4).trim();
                String nowRegex = String.join(" ## ", uiSmsPattern.getPatterns());
                if (previousRegex.equals(nowRegex) && uiSmsPattern.getCreditCycle().equals(previousCreditCycle))
                    uiSmsPattern.setBackgroundColourDefault();
                else {
                    uiSmsPattern.setBackgroundColourYellow();  // value has changed
                    updatePatterns.put(documentId, (SmsPattern) uiSmsPattern);
                }
            }
        });
        originalMap.forEach((tag, values) -> {
            if (!validationMap.containsKey(tag)) {
                String[] metadata = originalMap.get(tag).split("###")[0].split("##");
                String documentId = metadata[0].trim();
                String creditCycle = metadata[1].trim();
                String regex = originalMap.get(tag).substring(originalMap.get(tag).indexOf("###") + 4).trim();
                UiSmsPattern smsPattern = new UiSmsPattern();
                smsPattern.setTag(tag);
                smsPattern.setPatterns(Arrays.asList(regex.split(" ### ")));
                smsPattern.setBackgroundColourRed();
                uiSmsPatternList.add(smsPattern);
                removePatterns.put(documentId, (SmsPattern) smsPattern);
            }
        });
        validated = true;
        return validationErrors;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public void confirmUpdates(ResultSuccessCallback<String> successCallback, ResultFailureCallback failureCallback) {
        if (!validated)
            failureCallback.onError("Validate first", null);
        else {
            smsPatternDAO.updateSmsPatterns(new SmsPatternChangelog(newPatterns, removePatterns, updatePatterns), successCallback, failureCallback);
        }
    }

    /**
     * @param context
     * @param regexPatterns
     * @param timeStart     Inclusive
     * @param timeEnd       Non inclusive
     * @return
     */
    public List<Sms> testRegexOnMessages(Context context, List<String> regexPatterns, Date timeStart, Date timeEnd) {
        List<Pattern> patterns;
        try {
            patterns = regexPatterns.stream().map(Pattern::compile).collect(Collectors.toList());
        } catch (Exception e) {
            Toast.makeText(context, "Regex expression is not valid", Toast.LENGTH_LONG).show();
            return null;
        }
        if (smsMessages.size() == 0) {
            Calendar calendar = Calendar.getInstance(Locale.getDefault());
            Date end = calendar.getTime();
            calendar.add(Calendar.MONTH, -3);
            Date start = calendar.getTime();
            String filter = "date>=" + start.getTime() + " and date<" + end.getTime();
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    null,
                    filter,
                    null,
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                Toast.makeText(context, "Read " + cursor.getCount() + " messages", Toast.LENGTH_LONG).show();
                do {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
                    calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)));
                    Date date = calendar.getTime();
                    smsMessages.add(new Sms(address, body, date, id));
                } while (cursor.moveToNext());
            }

            if (cursor != null) {
                cursor.close();
            }
        }
        return smsMessages.stream().filter(sms ->
                        sms.getDate().after(timeStart) && sms.getDate().before(timeEnd) &&
                                patterns.stream().map(pattern -> pattern.matcher(sms.getText())).anyMatch(Matcher::matches))
                .collect(Collectors.toList());
    }

    public SmsPatternChangelog mergeChangeLogs(List<SmsPatternChangelog> changelogs) {
        SmsPatternChangelog mergedChangelog = new SmsPatternChangelog(new ArrayList<>(), new HashMap<>(), new HashMap<>());
        if (changelogs != null && !changelogs.isEmpty()) {
            mergedChangelog.getNewPatterns().addAll(changelogs.get(0).getNewPatterns());
            mergedChangelog.getUpdatePatterns().putAll(changelogs.get(0).getUpdatePatterns());
            mergedChangelog.getRemovePatterns().putAll(changelogs.get(0).getRemovePatterns());
            for (int i = 1; i < changelogs.size(); i++) {
                SmsPatternChangelog thisChangelog = changelogs.get(i);

                // new entry
                if (thisChangelog.getNewPatterns() != null && !thisChangelog.getNewPatterns().isEmpty()) {
                    thisChangelog.getNewPatterns().forEach(pattern -> {
                        Map.Entry<String, SmsPattern> removePattern, updatePattern;
                        if ((removePattern = mergedChangelog.getRemovePatterns().entrySet().stream().filter(patternEntry -> patternEntry.getValue().getTag().equals(pattern.getTag())).findFirst().orElse(null)) != null) {
                            // This item was removed, but has been added back
                            // This item should be treated as an update
                            mergedChangelog.getRemovePatterns().remove(removePattern.getKey());
                            if ((updatePattern = mergedChangelog.getUpdatePatterns().entrySet().stream().filter(patternEntry -> patternEntry.getValue().getTag().equals(pattern.getTag())).findFirst().orElse(null)) != null)
                                mergedChangelog.getUpdatePatterns().remove(updatePattern.getKey());
                            mergedChangelog.getUpdatePatterns().put(removePattern.getKey(), pattern);
                        } else {
                            mergedChangelog.getNewPatterns().add(pattern);
                        }
                        // Assuming new entry cannot be part of update list or new list (same tag cannot exist twice)
                    });
                }

                // updated entry
                if (thisChangelog.getUpdatePatterns() != null && !thisChangelog.getUpdatePatterns().isEmpty()) {
                    thisChangelog.getUpdatePatterns().forEach((documentId, pattern) -> {
                        Map.Entry<String, SmsPattern> removePattern, updatePattern;
                        SmsPattern newPattern;
                        if ((updatePattern = mergedChangelog.getUpdatePatterns().entrySet().stream().filter(patternEntry -> patternEntry.getValue().getTag().equals(pattern.getTag())).findFirst().orElse(null)) != null)
                            mergedChangelog.getUpdatePatterns().put(updatePattern.getKey(), pattern);  // if already in update, update the object that was going to be updated
                        else if ((newPattern = mergedChangelog.getNewPatterns().stream().filter(patternEntry -> patternEntry.getTag().equals(pattern.getTag())).findFirst().orElse(null)) != null) {
                            mergedChangelog.getNewPatterns().remove(newPattern);
                            mergedChangelog.getNewPatterns().add(pattern);
                        } else {
                            mergedChangelog.getUpdatePatterns().put(documentId, pattern);
                        }
                        // Assuming updated entry was not previously removed
                    });
                }

                // removed entry
                if (thisChangelog.getRemovePatterns() != null && !thisChangelog.getRemovePatterns().isEmpty()) {
                    thisChangelog.getUpdatePatterns().forEach((documentId, pattern) -> {
                        Map.Entry<String, SmsPattern> removePattern, updatePattern;
                        SmsPattern newPattern;
                        if ((newPattern = mergedChangelog.getNewPatterns().stream().filter(patternEntry -> patternEntry.getTag().equals(pattern.getTag())).findFirst().orElse(null)) != null)
                            mergedChangelog.getNewPatterns().remove(newPattern); // no need to add if new entry is removed
                        else if ((updatePattern = mergedChangelog.getUpdatePatterns().entrySet().stream().filter(patternEntry -> patternEntry.getValue().getTag().equals(pattern.getTag())).findFirst().orElse(null)) != null) {
                            mergedChangelog.getUpdatePatterns().remove(updatePattern.getKey());
                            mergedChangelog.getRemovePatterns().put(documentId, pattern);
                        } else
                            mergedChangelog.getRemovePatterns().put(documentId, pattern);
                        // Assuming previously removed entry cannot be removed again
                    });
                }
            }
        }
        return mergedChangelog;
    }
}

    