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

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class StationInfoAdapter extends BaseAdapter {

    private final ArrayList<Bus> buses;
    private final String stationName;
    private static LayoutInflater inflater = null;
    private final int stationId;
    DbHelper dbHelper = new DbHelper();
    public StationInfoAdapter(Context context, ArrayList<Bus> buses, String stationName, int stationId) {
        this.buses = buses;
        this.stationName = stationName;
        this.stationId = stationId;
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
        int timeStation = dbHelper.getTimeByRoute(bus.getId() ,stationId);
        stationNameTextView.setText(stationName);
        String transport = "";
        int Interval = Integer.parseInt(bus.getInterval());
        int i = bus.getTransportType();
        if(i==1){transport = "Автобус";}else if(i == 2){transport = "Троллейбус";}else if(i ==3){transport = "Трамвай";}
        numberOfBusTextView.setText(transport+" №"+bus.getBusNumber());
        if(stationId!=0){
            intervalTextView.setText("Ближайщее время прибытия: " +arrivalTimeStr(bus.getFirstDeparture(),Interval, timeStation)+ "\n\nИнтервал: "+bus.getInterval()+" минут");
        }
        else
        {
            intervalTextView.setText("Интервал: "+Interval+" минут");
        }
        firstDepartureTextView.setText("Первая отправка: "+bus.getFirstDeparture());
        lastDepartureTextView.setText("Последняя отправка: "+bus.getLastDeparture());

        return convertView;
    }
    private String arrivalTimeStr(String firstDeparture, int interval, int timeStation){
        String[] parts = firstDeparture.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);

        int totalMinutes = hours * 60 + minutes + timeStation;

        int newHours = totalMinutes / 60;
        int newMinutes = totalMinutes % 60;
        String newTime = String.format("%d:%02d", newHours, newMinutes);

        LocalTime firstDepartureTime = LocalTime.parse(newTime, DateTimeFormatter.ofPattern("H:mm"));

        LocalTime currentTimeLocal = LocalTime.now();

        Duration duration = Duration.between(firstDepartureTime, currentTimeLocal);
        long minutesBetween = duration.toMinutes();

        int arrivalTimeMinutes = ((int) minutesBetween / interval + 1) * interval;
        LocalTime arrivalTime = firstDepartureTime.plusMinutes(arrivalTimeMinutes);
        return arrivalTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}