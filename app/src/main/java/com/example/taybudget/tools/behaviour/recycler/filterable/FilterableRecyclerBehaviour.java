package com.example.taybudget.tools.behaviour.recycler.filterable;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;

import com.example.taybudget.tools.GenericRecyclerAdapter;
import com.example.taybudget.tools.behaviour.recycler.RecyclerBehaviour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Usage
 * -----
 * GenericRecyclerAdapter<Something> adapter = ...
 * FilterableRecyclerBehaviour<Something> filterable = ...
 * adapter.addBehaviour(filterableRecyclerBehaviour);
 * <p>
 * If Filterable behaviour is used, the adapter position will not be = position in the actual list
 * get real position using getOriginalItemPosition(adapterPosition)
 * Use real position in implementation of OnCreateListener
 * <p>
 * <p>
 * To tie to typing behaviour of a search text field
 * filterableRecyclerBehaviour.tieToEditText(findViewById(R.id.main_search));
 * <p>
 * Invoke this if the original list has been updated
 * filterableRecyclerBehaviour.updateOriginalList(updatedList);
 *
 * @param <E> Should be identical to type of Generic Recycler Adapter
 */
public class FilterableRecyclerBehaviour<E> extends RecyclerBehaviour implements FilterableInitializationFunction<E> {
    private List<E> originalList;
    private List<E> displayList;
    private final Builder<E> builder;
    private GenericRecyclerAdapter<E> adapter;
    private String lastFilterString;

    private final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            builder.editTexts.forEach(editText -> editText.setText(""));
        }
    };

    public static class Builder<E> {
        private final List<EditText> editTexts;
        private final RecyclerFilterFunction<E> filter;
        private ComponentActivity activity;
        private final Map<Function<String, Boolean>, Function<E, Boolean>> customRules;
        private final List<Consumer<List<E>>> consumers = new ArrayList<>();

        public Builder(RecyclerFilterFunction<E> filter) {
            this.filter = filter;
            this.editTexts = new ArrayList<>();
            this.customRules = new HashMap<>();
        }

        public Builder<E> addEditText(ComponentActivity activity, EditText editText) {
            this.activity = activity;
            this.editTexts.add(editText);
            return this;
        }

        public Builder<E> addCustomRule(Function<String, Boolean> rule, Function<E, Boolean> function) {
            customRules.put(rule, function);
            return this;
        }

        public Builder<E> addConsumer(Consumer<List<E>> consumer) {
            consumers.add(consumer);
            return this;
        }

        public FilterableRecyclerBehaviour<E> build() {
            return new FilterableRecyclerBehaviour<>(this);
        }
    }

    private FilterableRecyclerBehaviour(Builder<E> builder) {
        this.builder = builder;
        if (builder.activity != null) {
            builder.activity.getOnBackPressedDispatcher().addCallback(backPressedCallback);
            this.builder.editTexts.forEach(this::tieToEditText);
        }
    }

    @Override
    public void initialize(List<E> list, GenericRecyclerAdapter<E> adapter) {
        this.originalList = new ArrayList<>();
        originalList.addAll(list);
        this.displayList = new ArrayList<>();
        displayList.addAll(originalList);
        this.adapter = adapter;
        adapter.setList(displayList);
    }

    public void filter(String filterString) {
        lastFilterString = filterString;

        if (filterString == null || filterString.isEmpty()) {
            backPressedCallback.setEnabled(false);
            int count = displayList.size();
            displayList.clear();
            adapter.notifyItemRangeRemoved(0, count);
            displayList.addAll(originalList);
            adapter.notifyItemRangeInserted(0, displayList.size());
        } else {
            backPressedCallback.setEnabled(true);
            int count = displayList.size();
            displayList.clear();
            adapter.notifyItemRangeRemoved(0, count);

            AtomicBoolean flag = new AtomicBoolean(false);
            builder.customRules.forEach((rule, function) -> {
                if (rule.apply(filterString) && !flag.get()) {
                    flag.set(true);
                    displayList.addAll(originalList.stream().filter(function::apply).collect(Collectors.toList()));
                }
            });
            if (!flag.get())
                displayList.addAll(originalList.stream().filter(item -> builder.filter.filter(item, filterString)).collect(Collectors.toList()));

            adapter.notifyItemRangeInserted(0, displayList.size());
        }
        builder.consumers.forEach(consumer -> consumer.accept(displayList));
    }

    public void updateOriginalList(List<E> originalList) {
        this.originalList.clear();
        this.originalList.addAll(originalList);
        filter(lastFilterString);
    }

    public int getOriginalItemPosition(int adapterPosition) {
        return originalList.indexOf(displayList.get(adapterPosition));
    }

    private void tieToEditText(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });
    }
}
