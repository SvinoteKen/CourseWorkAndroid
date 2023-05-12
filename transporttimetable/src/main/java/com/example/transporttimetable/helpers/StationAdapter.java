package com.example.transporttimetable.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.transporttimetable.R;
import com.example.transporttimetable.models.Route;
import com.example.transporttimetable.models.Station;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StationAdapter extends ArrayAdapter<Station> {

    Context context;
    private final ArrayList<Station> stations;
    LayoutInflater inflater;
    DbHelper db = new DbHelper();
    private final Map<Integer, List<String>> busRoutes;
    public StationAdapter(@NonNull Context context, ArrayList<Station> stations, Map<Integer, List<String>> busRoutes) {
        super(context,0,stations);
        this.context = context;
        this.stations = stations;
        this.busRoutes = busRoutes;
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return stations.size();
    }

    @Override
    public Station getItem(int position) {
        return stations.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.stationlist_layot,parent,false);
            holder = new ViewHolder();
            holder.stationName = convertView.findViewById(R.id.station);
            holder.busNumber = convertView.findViewById(R.id.nBus);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        Station station = stations.get(position);
        holder.stationName.setText(station.getName());
        int stationId = station.getId();
        List<String> busesIds = busRoutes.get(stationId);
        if (busesIds != null) {
            StringBuilder busNumbers = new StringBuilder();
            for (String busesId : busesIds) {
                    busNumbers.append(busesId).append(", ");
            }
            if (busNumbers.length() > 0) {
                // Удаляем последнюю запятую и пробел
                busNumbers.setLength(busNumbers.length() - 2);
            }
            holder.busNumber.setText(busNumbers);
        } else {
            holder.busNumber.setText("");
        }

        return convertView;
    }

    static class ViewHolder {
        TextView stationName;
        TextView busNumber;
    }

}
