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

import androidx.appcompat.app.AppCompatActivity;

import com.example.transporttimetable.R;
import com.example.transporttimetable.helpers.StationAdapter;
import com.example.transporttimetable.helpers.DbHelper;
import com.example.transporttimetable.helpers.StationInfoAdapter;
import com.example.transporttimetable.models.Bus;
import com.example.transporttimetable.models.Station;
import com.parse.Parse;

import java.util.ArrayList;

public class StationViewActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    boolean isDataLoaded = false;
    boolean isInfoLoaded = false;
    GridView gridView;
    Button viewOnMap;
    Station station;
    StationAdapter stationAdapter;
    StationInfoAdapter stationInfoAdapter;
    DbHelper dbHelper;
    int stationId = 0;
    String stationName = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper();
        // ниже, вместо ??????? вставляем Ваш ключ, присланный Вам из Яндекс:
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build());
        // Укажите имя своей Activity вместо mapview. У меня она называется map
        setContentView(R.layout.station_activity);
        gridView = findViewById(R.id.gridView);
        viewOnMap = findViewById(R.id.viewOnMapStation);

        viewOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapsActivity = new Intent(StationViewActivity.this, MapsActivity.class);
                int stationId = station.getId();
                String stationName = station.getName();
                double stationLatitude = station.getCoordinates().getLatitude();
                double stationLongitude = station.getCoordinates().getLongitude();

                mapsActivity.putExtra("stationId",stationId);
                mapsActivity.putExtra("stationName",stationName);
                mapsActivity.putExtra("stationLatitude",stationLatitude);
                mapsActivity.putExtra("stationLongitude",stationLongitude);
                startActivity(mapsActivity);
            }
        });

        stationAdapter = new StationAdapter(StationViewActivity.this,new ArrayList<>());
        stationInfoAdapter = new StationInfoAdapter(StationViewActivity.this,new ArrayList<>(),stationName);

        gridView.setAdapter(stationAdapter);
        gridView.setOnItemClickListener(this);
        try
        {
            stationId = (int) getIntent().getSerializableExtra("stationId");
            stationName = (String) getIntent().getSerializableExtra("stationName");
            isDataLoaded =true;
            new LoadStationInfoTask().execute();
        } catch (NullPointerException e) {
            Log.e("StationInfoLoad", e.getMessage());
        }
        if (!isDataLoaded) {
            new LoadStationDataTask().execute();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDataLoaded = false;
        isInfoLoaded = false;
        new LoadStationDataTask().execute();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View v, int position, long l) {
        if(isDataLoaded && !isInfoLoaded){
            viewOnMap.setVisibility(View.VISIBLE);
            viewOnMap.setEnabled(true);
            station = (Station) adapterView.getItemAtPosition(position);
            Log.e("Test","ID"+ station.getId()+" Name:" + station.getName());
            stationId = station.getId();
            stationName = station.getName();
            new LoadStationInfoTask().execute();
        }
    }
    @Override
    public void onBackPressed() {
        isDataLoaded = false;
        new LoadStationDataTask().execute();
        if(!isInfoLoaded){
            super.onBackPressed();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadStationDataTask extends AsyncTask<Void, Void, ArrayList<Station>> {

        @Override
        protected void onPreExecute() {
            viewOnMap.setVisibility(View.INVISIBLE);
            viewOnMap.setEnabled(false);
        }

        @Override
        protected ArrayList<Station> doInBackground(Void... voids) {
            return dbHelper.getAllStations();
        }

        @Override
        protected void onPostExecute(ArrayList<Station> stations) {
            if (!isDataLoaded) {
                stationAdapter = new StationAdapter(StationViewActivity.this, stations);
                gridView.setAdapter(stationAdapter);
                isDataLoaded = true;
                isInfoLoaded = false;
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadStationInfoTask extends AsyncTask<Void, Void, ArrayList<Bus>> {

        @Override
        protected ArrayList<Bus> doInBackground(Void... voids) {
            return dbHelper.getBusByStation(stationId);
        }

        @Override
        protected void onPostExecute(ArrayList<Bus> buses) {
            if (!isInfoLoaded) {
                stationInfoAdapter = new StationInfoAdapter(StationViewActivity.this, buses, stationName);
                gridView.setAdapter(stationInfoAdapter);
                isInfoLoaded = true;
                isDataLoaded = true;
            }
        }
    }
}
