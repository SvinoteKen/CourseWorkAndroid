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

import java.util.ArrayList;

public class StationInfoAdapter extends BaseAdapter {

    private final ArrayList<Bus> buses;
    private final String stationName;
    private static LayoutInflater inflater = null;

    public StationInfoAdapter(Context context, ArrayList<Bus> buses, String stationName) {
        this.buses = buses;
        this.stationName = stationName;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return buses.size();
    }

    @Override
    public Object getItem(int position) {
        return buses.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = inflater.inflate(R.layout.stationinfo_layot, parent, false);
        }
        TextView stationNameTextView = convertView.findViewById(R.id.stationNameTextView);
        TextView numberOfBusTextView = convertView.findViewById(R.id.numberOfBusTextView);
        TextView intervalTextView = convertView.findViewById(R.id.intervalTextView);
        TextView firstDepartureTextView = convertView.findViewById(R.id.firstDepartureTextView);
        TextView lastDepartureTextView = convertView.findViewById(R.id.lastDepartureTextView);

        Bus bus = buses.get(position);

        stationNameTextView.setText(stationName);
        String transport = "";
        int i = bus.getTransportType();
        if(i==1){transport = "Автобус";}else if(i == 2){transport = "Троллейбус";}else if(i ==3){transport = "Трамвай";}
        numberOfBusTextView.setText(transport+" №"+bus.getBusNumber());
        intervalTextView.setText("Интервал: "+bus.getInterval()+" минут");
        firstDepartureTextView.setText("Первая отправка: "+bus.getFirstDeparture());
        lastDepartureTextView.setText("Последняя отправка: "+bus.getLastDeparture());

        return convertView;
    }
}