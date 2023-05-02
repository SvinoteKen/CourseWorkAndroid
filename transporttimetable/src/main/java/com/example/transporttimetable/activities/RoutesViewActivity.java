package com.example.transporttimetable.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.transporttimetable.R;
import com.example.transporttimetable.helpers.BusAdapter;
import com.example.transporttimetable.helpers.DbHelper;
import com.example.transporttimetable.helpers.RoutesInfoAdapter;
import com.example.transporttimetable.models.Bus;
import com.example.transporttimetable.models.Station;
import com.parse.Parse;

import java.util.ArrayList;

public class RoutesViewActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    boolean isDataLoaded = false;
    boolean isInfoLoaded = false;
    boolean Reversed = false;
    GridView gridView;
    int transportType = 1;
    int routeId = 0;
    Bus busInfo;
    Station station;
    Button viewOnMap;
    Button directRouteButton, returnRouteButton;
    ImageButton busButton, trolleybusButton, subwayButton;
    BusAdapter busAdapter;
    DbHelper dbHelper;
    int busId = 0;
    RoutesInfoAdapter routesInfoAdapter;

    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper();
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build());
        setContentView(R.layout.route_activity);

        gridView = findViewById(R.id.gridView);
        viewOnMap = findViewById(R.id.viewOnMapStation);
        directRouteButton = findViewById(R.id.directRouteButton);
        returnRouteButton = findViewById(R.id.returnRouteButton);
        busButton = findViewById(R.id.busButton);
        trolleybusButton = findViewById(R.id.trolleybusButton);
        subwayButton = findViewById(R.id.subwayButton);

        busAdapter = new BusAdapter(RoutesViewActivity.this,new ArrayList<>());
        routesInfoAdapter = new RoutesInfoAdapter(RoutesViewActivity.this, new ArrayList<>());
        gridView.setAdapter(busAdapter);
        gridView.setOnItemClickListener(this);

        trolleybusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDataLoaded = false;
                trolleybusButton.setEnabled(false);
                subwayButton.setEnabled(true);
                busButton.setEnabled(true);
                transportType = 2;
                new RoutesViewActivity.LoadRoutesTask().execute();
            }
        });
        subwayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDataLoaded = false;
                trolleybusButton.setEnabled(true);
                subwayButton.setEnabled(false);
                busButton.setEnabled(true);
                transportType = 3;
                new RoutesViewActivity.LoadRoutesTask().execute();
            }
        });
        busButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDataLoaded = false;
                trolleybusButton.setEnabled(true);
                subwayButton.setEnabled(true);
                busButton.setEnabled(false);
                transportType = 1;
                new RoutesViewActivity.LoadRoutesTask().execute();
            }
        });
        viewOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapsActivity = new Intent(RoutesViewActivity.this, MapsActivity.class);
                int busId = busInfo.getId();

                mapsActivity.putExtra("busId",busId);
                mapsActivity.putExtra("Reversed",Reversed);
                startActivity(mapsActivity);
            }
        });
        directRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Reversed = false;
                isInfoLoaded = false;
                directRouteButton.setEnabled(false);
                returnRouteButton.setEnabled(true);
                new RoutesViewActivity.LoadRoutesInfoTask().execute();
            }
        });
        returnRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Reversed = true;
                isInfoLoaded = false;
                directRouteButton.setEnabled(true);
                returnRouteButton.setEnabled(false);
                new RoutesViewActivity.LoadRoutesInfoTask().execute();
            }
        });
        if (!isDataLoaded) {
            new RoutesViewActivity.LoadRoutesTask().execute();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDataLoaded = false;
        isInfoLoaded = false;
        Reversed = false;
        new RoutesViewActivity.LoadRoutesTask().execute();
    }
    @Override
    public void onBackPressed() {
        isDataLoaded = false;
        Reversed = false;
        new LoadRoutesTask().execute();
        if(!isInfoLoaded){
            super.onBackPressed();
        }
    }
    @Override
    public void onItemClick(AdapterView<?> adapterView, View v, int position, long l) {
        if(isDataLoaded && !isInfoLoaded){
            viewOnMap.setVisibility(View.VISIBLE);
            viewOnMap.setEnabled(true);
            directRouteButton.setVisibility(View.VISIBLE);
            directRouteButton.setEnabled(false);
            returnRouteButton.setVisibility(View.VISIBLE);
            returnRouteButton.setEnabled(true);
            busInfo = (Bus) adapterView.getItemAtPosition(position);
            busId = busInfo.getId();
            new RoutesViewActivity.LoadRoutesInfoTask().execute();
        }
        if(isDataLoaded && isInfoLoaded){
            station = (Station) adapterView.getItemAtPosition(position);
            Intent stationViewActivity = new Intent(RoutesViewActivity.this,StationViewActivity.class);
            stationViewActivity.putExtra("stationId",station.getId());
            stationViewActivity.putExtra("busId",busId);
            stationViewActivity.putExtra("stationName",station.getName());
            startActivity(stationViewActivity);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadRoutesTask extends AsyncTask<Void, Void, ArrayList<Bus>> {

        @Override
        protected void onPreExecute() {
            viewOnMap.setVisibility(View.INVISIBLE);
            viewOnMap.setEnabled(false);
            directRouteButton.setVisibility(View.INVISIBLE);
            directRouteButton.setEnabled(false);
            returnRouteButton.setVisibility(View.INVISIBLE);
            returnRouteButton.setEnabled(false);
        }

        @Override
        protected ArrayList<Bus> doInBackground(Void... voids) {
            return dbHelper.getAllBuses(transportType);
        }

        @Override
        protected void onPostExecute(ArrayList<Bus> bus) {
            if (!isDataLoaded) {
                busAdapter = new BusAdapter(RoutesViewActivity.this, bus);
                gridView.setAdapter(busAdapter);
                isDataLoaded = true;
                isInfoLoaded = false;
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadRoutesInfoTask extends AsyncTask<Void, Void, ArrayList<Station>> {

        @Override
        protected ArrayList<Station> doInBackground(Void... voids) {
            return dbHelper.getRoutByBus(busId, Reversed);
        }

        @Override
        protected void onPostExecute(ArrayList<Station> stations) {
            if (!isInfoLoaded) {
                routesInfoAdapter = new RoutesInfoAdapter(RoutesViewActivity.this, stations);
                gridView.setAdapter(routesInfoAdapter);
                isInfoLoaded = true;
                isDataLoaded = true;
            }
        }
    }
}
