package com.example.taybudget.tools;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taybudget.tools.behaviour.recycler.RecyclerBehaviour;
import com.example.taybudget.tools.behaviour.recycler.filterable.FilterableInitializationFunction;
import com.example.taybudget.tools.behaviour.recycler.filterable.FilterableRecyclerBehaviour;

import java.util.ArrayList;
import java.util.List;

public class GenericRecyclerAdapter<E> extends RecyclerView.Adapter<GenericRecyclerAdapter.ViewHolder> {
    private final Context context;
    private final int resourceId;
    private List<E> list;
    private final SetLayoutItemsListener layoutItemsListener;
    private final OnCreateListener onCreateListener;
    private final List<RecyclerBehaviour> behaviours;

    public GenericRecyclerAdapter(Context context, int resourceId, List<E> list, SetLayoutItemsListener layoutItemsListener, OnCreateListener onCreateListener) {
        this.context = context;
        this.resourceId = resourceId;
        this.list = list;
        this.layoutItemsListener = layoutItemsListener;
        this.onCreateListener = onCreateListener;
        behaviours = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(context).inflate(resourceId, parent, false);
        ViewHolder viewHolder = new ViewHolder(layout);
        if (onCreateListener != null)
            onCreateListener.onCreated(viewHolder);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (layoutItemsListener != null) {
            RecyclerBehaviour filterableBehaviour = behaviours.stream().filter(behaviour -> behaviour instanceof FilterableRecyclerBehaviour).findFirst().orElse(null);
            if (filterableBehaviour != null)
                layoutItemsListener.setLayoutItems(context, holder, ((FilterableRecyclerBehaviour<E>) filterableBehaviour).getOriginalItemPosition(position));
            else
                layoutItemsListener.setLayoutItems(context, holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void addBehaviour(RecyclerBehaviour behaviour) {
        behaviours.add(behaviour);
        if (behaviour instanceof FilterableRecyclerBehaviour) {
            ((FilterableInitializationFunction<E>) behaviour).initialize(list, this);
        }
    }

    public List<E> getList() {
        return list;
    }

    public void setList(List<E> list) {
        this.list = list;
    }

    public List<RecyclerBehaviour> getBehaviours() {
        return behaviours;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public interface OnCreateListener {
        void onCreated(ViewHolder viewHolder);
    }

    public interface SetLayoutItemsListener {
        void setLayoutItems(Context context, ViewHolder viewHolder, int position);
    }
}
