package com.example.transporttimetable.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.transporttimetable.R;
import com.example.transporttimetable.models.Bus;
import com.example.transporttimetable.models.Route;
import com.example.transporttimetable.models.Station;

import java.util.ArrayList;

public class RoutesInfoAdapter extends BaseAdapter {
    private Context context;
    private final ArrayList<Station> stations;
    private static LayoutInflater inflater = null;
    public RoutesInfoAdapter(Context context, ArrayList<Station> stations) {
        this.context = context;
        this.stations = stations;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return stations.size();
    }

    @Override
    public Object getItem(int position) {
        return stations.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RoutesInfoAdapter.ViewHolder holder;
        if(convertView == null) {
            convertView = inflater.inflate(R.layout.routeinfo_layout, parent, false);
            holder = new RoutesInfoAdapter.ViewHolder();
            holder.stationName = convertView.findViewById(R.id.stationName);
            convertView.setTag(holder);
        }else{
            holder = (RoutesInfoAdapter.ViewHolder) convertView.getTag();
        }

        Station station = stations.get(position);

        holder.stationName.setText(station.getName());

        return convertView;
    }
    static class ViewHolder {
        TextView stationName;
    }
}
