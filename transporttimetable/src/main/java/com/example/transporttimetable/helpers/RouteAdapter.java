package com.example.transporttimetable.helpers;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.transporttimetable.R;
import com.example.transporttimetable.models.RouteModel;
import com.example.transporttimetable.models.Step;

import java.util.List;

public class RouteAdapter extends BaseAdapter {

    private final Context context;
    private final List<RouteModel> routeList;
    private final LayoutInflater inflater;
    private int expandedPosition = -1; // для отслеживания открытого маршрута

    public RouteAdapter(Context context, List<RouteModel> routes) {
        this.context = context;
        this.routeList = routes;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        Log.e("RouteAdapter", "expandedPosition: " + expandedPosition);
        return expandedPosition == -1 ? routeList.size() : 1;

    }

    @Override
    public Object getItem(int position) {
        Log.e("RouteAdapter", "expandedPosition: " + expandedPosition);
        return expandedPosition == -1 ? routeList.get(position) : routeList.get(expandedPosition);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        TextView timeRange;
        LinearLayout routeContainer;
        RelativeLayout detailContainer;
        ExpandableHeightGridView stopsGridView;
        View timelineLine;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.route_item, parent, false);
            holder = new ViewHolder();
            holder.timeRange      = convertView.findViewById(R.id.time_range);
            holder.routeContainer = convertView.findViewById(R.id.route_container);
            holder.detailContainer= convertView.findViewById(R.id.detailContainer);
            holder.stopsGridView  = convertView.findViewById(R.id.stopsGridView);
            holder.timelineLine   = convertView.findViewById(R.id.timelineLine);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // вычисляем, какой элемент из routeList мы сейчас отображаем
        int actualPosition = (expandedPosition == -1 ? position : expandedPosition);
        RouteModel route = routeList.get(actualPosition);

        holder.timeRange.setText(route.getTimeRange());

        // рисуем summary
        holder.routeContainer.removeAllViews();
        // Отрисовываем маршрут в одну линию
        for (int i = 0; i < route.getSteps().size(); i++) {
            Step step = route.getSteps().get(i);

            if (step instanceof Step.Walk) {
                ImageView walkIcon = new ImageView(context);
                walkIcon.setImageResource(R.drawable.ic_walk);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(60, 60);
                walkIcon.setLayoutParams(params);
                holder.routeContainer.addView(walkIcon);
            }

            if (step instanceof Step.Bus) {
                View busView = inflater.inflate(R.layout.bus_block, holder.routeContainer, false);
                TextView numberText = busView.findViewById(R.id.bus_number);
                numberText.setText(((Step.Bus) step).number);
                holder.routeContainer.addView(busView);
            }

            if (step instanceof Step.Transfer) {
                LinearLayout transferLayout = new LinearLayout(context);
                transferLayout.setOrientation(LinearLayout.HORIZONTAL);
                transferLayout.setGravity(Gravity.CENTER_VERTICAL);

                ImageView clock = new ImageView(context);
                clock.setImageResource(R.drawable.ic_clock);
                clock.setLayoutParams(new LinearLayout.LayoutParams(40, 40));

                TextView timeText = new TextView(context);
                timeText.setText(((Step.Transfer) step).time);
                timeText.setTextColor(Color.WHITE);
                timeText.setTextSize(14f);
                timeText.setPadding(8, 0, 0, 0);

                transferLayout.addView(clock);
                transferLayout.addView(timeText);

                holder.routeContainer.addView(transferLayout);
            }

            if (i != route.getSteps().size() - 1) {
                TextView arrow = new TextView(context);
                arrow.setText(" < ");
                arrow.setTextColor(Color.WHITE);
                arrow.setTextSize(16f);
                arrow.setPadding(8, 0, 8, 0);
                holder.routeContainer.addView(arrow);
            }
        }

        // показываем detailContainer только для actualPosition == expandedPosition
        if (expandedPosition == actualPosition) {
            holder.detailContainer.setVisibility(View.VISIBLE);
            holder.stopsGridView.setExpanded(true);
            holder.stopsGridView.setAdapter(new StepsAdapter(context, route.getSteps()));
        } else {
            holder.detailContainer.setVisibility(View.GONE);
        }

        // клик — переключаем между “все” и “только этот”
        convertView.setOnClickListener(v -> {
            if (expandedPosition == actualPosition) {
                expandedPosition = -1;
            } else {
                expandedPosition = actualPosition;
            }
            notifyDataSetChanged();
        });

        return convertView;
    }
}