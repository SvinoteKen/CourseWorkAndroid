package com.example.transporttimetable.helpers;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.transporttimetable.R;
import com.example.transporttimetable.models.Step;
import com.example.transporttimetable.models.StopModel;

import java.util.ArrayList;
import java.util.List;

public class StepsAdapter extends BaseAdapter {

    private final Context context;
    private final List<Object> stepItems = new ArrayList<>();

    public StepsAdapter(Context context, List<Step> steps) {
        this.context = context;
        flattenSteps(steps); // Преобразуем список шагов в отдельные элементы
    }

    private void flattenSteps(List<Step> steps) {
        for (Step step : steps) {
            if (step instanceof Step.Bus) {
                Step.Bus bus = (Step.Bus) step;
                Log.d("StepsAdapter", "Добавляем автобус с " + bus.stops.size() + " остановками");
                stepItems.addAll(bus.stops);
            } else if (step instanceof Step.Walk) {
                Log.d("StepsAdapter", "Добавляем Walk: " + ((Step.Walk) step).getTime() + " минут");
                stepItems.add(step);
            } else if (step instanceof Step.Transfer) {
                Log.d("StepsAdapter", "Добавляем Transfer: " + ((Step.Transfer) step).time + " минут");
                stepItems.add(step);
            }
        }
    }

    @Override
    public int getCount() {
        return stepItems.size();
    }

    @Override
    public Object getItem(int position) {
        return stepItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = stepItems.get(position);
        if (item instanceof Step.Walk) return 0;
        if (item instanceof StopModel) return 1;
        if (item instanceof Step.Transfer) return 2;
        return -1;
    }

    @Override
    public int getViewTypeCount() {
        return 3; // Walk, Stop, Transfer
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == 0) {
            if (convertView == null)
                convertView = inflater.inflate(R.layout.item_walk, parent, false);
            TextView stopTime = convertView.findViewById(R.id.walk_time);
            Step.Walk walk = (Step.Walk) stepItems.get(position);
            String s = "Пешком "+ walk.getTime() + " минут";
            stopTime.setText(s);
            // Walk — ничего не отображаем, просто иконка
        } else if (viewType == 1) {
            if (convertView == null)
                convertView = inflater.inflate(R.layout.item_stop, parent, false);
            StopModel stop = (StopModel) stepItems.get(position);
            TextView stopName = convertView.findViewById(R.id.stop_name);
            TextView stopTime = convertView.findViewById(R.id.stop_time);
            stopName.setText(stop.name);
            String s = stop.time + " мин.";
            stopTime.setText(s);
        } else if (viewType == 2) {
            if (convertView == null)
                convertView = inflater.inflate(R.layout.item_transfer, parent, false);
            Step.Transfer transfer = (Step.Transfer) stepItems.get(position);
            TextView transferTime = convertView.findViewById(R.id.transfer_time);
            String s = transfer.time + " мин.";
            transferTime.setText(transfer.time);
        }

        return convertView;
    }
}