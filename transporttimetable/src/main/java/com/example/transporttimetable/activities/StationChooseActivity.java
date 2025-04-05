package com.example.transporttimetable.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.transporttimetable.R;
import com.example.transporttimetable.helpers.BusAdapter;
import com.example.transporttimetable.helpers.DbHelper;
import com.example.transporttimetable.helpers.RoutesInfoAdapter;
import com.example.transporttimetable.helpers.StationAdapter;
import com.example.transporttimetable.helpers.StationsAdapter;
import com.example.transporttimetable.models.Bus;
import com.example.transporttimetable.models.Station;
import com.parse.Parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

public class StationChooseActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    EditText chooseField;
    GridView stationsGridView;
    BusAdapter busAdapter;
    TextView headerTitle;
    LinearLayout myLocationButton;
    DbHelper dbHelper;
    String searchStation = null;
    ArrayList<Station> stations = null;
    StationsAdapter stationsAdapter;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper();
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build());
        setContentView(R.layout.choose_station);
        chooseField = findViewById(R.id.chooseField);
        stationsGridView = findViewById(R.id.gridView);
        headerTitle = findViewById(R.id.headerTitle);
        myLocationButton = findViewById(R.id.myLocationButton);
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent RouteBuilding = new Intent(StationChooseActivity.this, RouteBuilding.class);
                RouteBuilding.putExtra("direction", getIntent().getSerializableExtra("hintText"));
                RouteBuilding.putExtra("valueStation", "Мое местоположение");
                startActivity(RouteBuilding);
            }
        });
        ArrayList<Bus> bus = dbHelper.getAllBuses(1);
        headerTitle.setText((String) getIntent().getSerializableExtra("headerText"));
        busAdapter = new BusAdapter(StationChooseActivity.this, bus);
        stationsGridView.setAdapter(busAdapter);
        stationsGridView.setOnItemClickListener(this);
        chooseField.setHint((String) getIntent().getSerializableExtra("hintText"));
        stationsAdapter = new StationsAdapter(StationChooseActivity.this, new ArrayList<>());
        chooseField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Не используется, но должен быть определен
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Здесь обрабатываем изменения текста
                searchStation = s.toString();
                new StationChooseActivity.LoadRoutesInfoTask().execute();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Не используется, но должен быть определен
            }
        });
        new StationChooseActivity.LoadRoutesInfoTask().execute();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent RouteBuilding = new Intent(StationChooseActivity.this, RouteBuilding.class);
        Station station = stations.get(position);

        String text = String.valueOf(chooseField.getText());
        String hint = (String) getIntent().getSerializableExtra("hintText");
        RouteBuilding.putExtra("direction", hint);
        RouteBuilding.putExtra("streetName", station.getName());
        RouteBuilding.putExtra("latitude", station.getCoordinates().getLatitude());
        RouteBuilding.putExtra("longitude", station.getCoordinates().getLongitude());
        Intent intent = getIntent();
        if (intent.hasExtra("savedFrom")) {
            RouteBuilding.putExtra("savedFrom", intent.getStringExtra("savedFrom"));
        }
        if (intent.hasExtra("savedTo")) {
            RouteBuilding.putExtra("savedTo", intent.getStringExtra("savedTo"));
        }
        startActivity(RouteBuilding);
    }
    @SuppressLint("StaticFieldLeak")
    private class LoadRoutesInfoTask extends AsyncTask<Void, Void, ArrayList<Station>> {

        @Override
        protected ArrayList<Station> doInBackground(Void... voids) {


            if(searchStation == null)
            {
                stations = dbHelper.getStations();
            }
            else
            {
                Log.d("Test", searchStation.toString());
                stations = dbHelper.searchStations(searchStation);
            }
            Collections.sort(stations, Station.getIndexComparator());
            return stations;
        }

        @Override
        protected void onPostExecute(ArrayList<Station> stations) {
            stationsAdapter = new StationsAdapter(StationChooseActivity.this, stations);
            stationsGridView.setAdapter(stationsAdapter);
            //isInfoLoaded = true;
            //isDataLoaded = true;
        }
    }

}
