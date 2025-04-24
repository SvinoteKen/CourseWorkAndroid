package com.example.transporttimetable.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.transporttimetable.R;
import com.example.transporttimetable.models.Step;

import java.util.List;

public class StopDetailsAdapter extends BaseAdapter {

    private final Context context;
    private final List<Step> stepList;
    private final LayoutInflater inflater;

    public StopDetailsAdapter(Context context, List<Step> stepList) {
        this.context = context;
        this.stepList = stepList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return stepList.size();
    }

    @Override
    public Object getItem(int position) {
        return stepList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        ImageView icon;
        TextView detailText;
        LinearLayout transferLayout;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.stop_item, parent, false);
            holder = new ViewHolder();
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Step step = stepList.get(position);

        if (step instanceof Step.Walk) {
            holder.icon.setImageResource(R.drawable.ic_walk);
            holder.detailText.setText("Walk to the next stop");
        } else if (step instanceof Step.Bus) {
            holder.icon.setImageResource(R.drawable.ic_baseline_directions_bus_menu_24);
            holder.detailText.setText("Bus No: " + ((Step.Bus) step).number);
        } else if (step instanceof Step.Transfer) {
            holder.icon.setImageResource(R.drawable.ic_clock);
            holder.detailText.setText("Transfer time: " + ((Step.Transfer) step).time);
        }

        return convertView;
    }
}