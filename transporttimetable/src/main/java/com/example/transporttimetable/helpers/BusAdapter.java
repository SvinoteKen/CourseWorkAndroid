package com.example.transporttimetable.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.transporttimetable.R;
import com.example.transporttimetable.models.Bus;
import com.example.transporttimetable.models.Route;

import java.util.ArrayList;

public class BusAdapter extends ArrayAdapter<Bus> {

    Context context;
    private final ArrayList<Bus> buses;
    LayoutInflater inflater;
    DbHelper db = new DbHelper();

    public BusAdapter(@NonNull Context context, ArrayList<Bus> buses) {
        super(context,0,buses);
        this.context = context;
        this.buses = buses;
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return buses.size();
    }

    @Override
    public Bus getItem(int position) {
        return buses.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BusAdapter.ViewHolder holder;
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.routeslist_layout,parent,false);
            holder = new BusAdapter.ViewHolder();
            holder.routeName = convertView.findViewById(R.id.routeName);
            holder.busNumber = convertView.findViewById(R.id.nBus);
            convertView.setTag(holder);
        }else {
            holder = (BusAdapter.ViewHolder) convertView.getTag();
        }
        Bus bus = buses.get(position);
        int busId = bus.getId();
        ArrayList<Route> routes = db.getRoutsByBus(busId);
        if(routes.size() != 0){
        holder.busNumber.setText(bus.getBusNumber());
        holder.routeName.setText(routes.get(0).getName());}
        return convertView;
    }

    static class ViewHolder {
        TextView routeName;
        TextView busNumber;
    }
}
