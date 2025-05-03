package com.example.transporttimetable.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.transporttimetable.R;
import com.example.transporttimetable.models.Station;

import java.util.ArrayList;

public class StationsAdapter extends BaseAdapter {

    private final Context context;
    private final ArrayList<Station> stations;
    private static LayoutInflater inflater = null;

    public StationsAdapter(Context context, ArrayList<Station> stations) {
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
        StationsAdapter.ViewHolder holder;
        if(convertView == null) {
            convertView = inflater.inflate(R.layout.choose_stationinfo, parent, false);
            holder = new StationsAdapter.ViewHolder();
            holder.stationName = convertView.findViewById(R.id.itemText);
            convertView.setTag(holder);
        }else{
            holder = (StationsAdapter.ViewHolder) convertView.getTag();
        }

        Station station = stations.get(position);

        holder.stationName.setText(station.getName());

        return convertView;
    }
    static class ViewHolder {
        TextView stationName;
    }
}
