package com.example.transporttimetable.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StationViewActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    boolean isDataLoaded = false;
    boolean isInfoLoaded = false;
    GridView gridView;
    Button viewOnMap;
    Station station;
    String stationName = null;
            StationAdapter stationAdapter;
    StationInfoAdapter stationInfoAdapter;
    DbHelper dbHelper;
    int stationId = 0;
    int stationIdForTime = 0;
    String searchStation = null;
    EditText searchText;
    ArrayList<Station> stations = null;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper();
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build());
        setContentView(R.layout.station_activity);
        gridView = findViewById(R.id.gridView);
        viewOnMap = findViewById(R.id.viewOnMapStation);
        searchText = findViewById(R.id.search);
        searchText.setOnKeyListener(new View.OnKeyListener()
            {
                 public boolean onKey(View v, int keyCode, KeyEvent event)
                 {
                     if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && searchText.getText()!=null)
                     {
                         searchStation = searchText.getText().toString();
                         isDataLoaded = false;
                         if (!isDataLoaded) {
                             new LoadStationDataTask().execute();
                         }
                     }
                     return false;
                 }
            }
        );
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

        stationAdapter = new StationAdapter(StationViewActivity.this,new ArrayList<>(),new HashMap<>());
        stationInfoAdapter = new StationInfoAdapter(StationViewActivity.this,new ArrayList<>(),stationName, stationId);

        gridView.setAdapter(stationAdapter);
        gridView.setOnItemClickListener(this);
        try
        {
            stationIdForTime = (int) getIntent().getSerializableExtra("stationId");
            int busId = (int) getIntent().getSerializableExtra("busId");
            dbHelper.getTimeByRoute(busId ,stationIdForTime);
            stationName = (String) getIntent().getSerializableExtra("stationName");
            isDataLoaded =true;
            new LoadStationInfoTask().execute();
        } catch (NullPointerException e) {
            Log.e("StationInfoLoad", e.getMessage());
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
    private class LoadStationDataTask extends AsyncTask<Void, Void, Map<Integer, List<String>>> {

        @Override
        protected void onPreExecute() {
            viewOnMap.setVisibility(View.INVISIBLE);
            viewOnMap.setEnabled(false);
        }

        @Override
        protected Map<Integer, List<String>> doInBackground(Void... voids) {
            Map<Integer, List<String>> busRoutes = new HashMap<>();
            stations = dbHelper.getAllStations(searchStation);

            for (Station station : stations) {
                int stationId = station.getId();
                List<String> buses = dbHelper.getBusesByStation(stationId);
                busRoutes.put(stationId, buses);
            }

            return busRoutes;
        }

        @Override
        protected void onPostExecute(Map<Integer, List<String>> busRoutes) {
            if (!isDataLoaded) {
                stationAdapter = new StationAdapter(StationViewActivity.this, stations, busRoutes);
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
                stationInfoAdapter = new StationInfoAdapter(StationViewActivity.this, buses, stationName, stationIdForTime);
                gridView.setAdapter(stationInfoAdapter);
                isInfoLoaded = true;
                isDataLoaded = true;
            }
        }
    }
}
