package com.example.transporttimetable.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.transporttimetable.R;
import com.example.transporttimetable.helpers.BusAdapter;
import com.example.transporttimetable.helpers.DbHelper;
import com.example.transporttimetable.helpers.RoutesInfoAdapter;
import com.example.transporttimetable.models.Bus;
import com.example.transporttimetable.models.Station;
import com.parse.Parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class RouteBuilding extends AppCompatActivity {

    EditText fromField;
    EditText toField;
    Button fromMapButton;
    Button toMapButton;
    ImageButton swapButton;
    Button buildRouteButton;
    EditText routeNumber;
    private String savedFrom = "", savedTo = "";
    @SuppressLint("MissingInflatedId")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_builder);

        fromField = findViewById(R.id.fromField);
        toField = findViewById(R.id.toField);
        fromMapButton = findViewById(R.id.fromMapButton);
        toMapButton = findViewById(R.id.toMapButton);
        swapButton = findViewById(R.id.swapButton);
        buildRouteButton = findViewById(R.id.buildRouteButton);
        routeNumber = findViewById(R.id.routeNumber);

        fromMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent stationMapChooseActivity = new Intent(RouteBuilding.this, MapsActivity.class);
                stationMapChooseActivity.putExtra("buttonText","Готово");
                stationMapChooseActivity.putExtra("savedTo", toField.getText().toString());
                startActivityForResult(stationMapChooseActivity, 100);
            }
        });
        toMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent stationMapChooseActivity = new Intent(RouteBuilding.this, MapsActivity.class);
                stationMapChooseActivity.putExtra("buttonText","Готово");
                stationMapChooseActivity.putExtra("savedFrom", fromField.getText().toString());
                startActivityForResult(stationMapChooseActivity, 100);
            }
        });
        fromField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent stationChooseActivity = new Intent(RouteBuilding.this, StationChooseActivity.class);
                stationChooseActivity.putExtra("headerText","Выберите начальную остановку");
                stationChooseActivity.putExtra("hintText","Откуда");
                stationChooseActivity.putExtra("savedTo", toField.getText().toString());
                startActivity(stationChooseActivity);
            }
        });
        toField.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent stationChooseActivity = new Intent(RouteBuilding.this, StationChooseActivity.class);
                stationChooseActivity.putExtra("headerText","Выберите конечную остановку");
                stationChooseActivity.putExtra("hintText","Куда");
                // Сохраняем текущее значение первого поля перед переходом
                stationChooseActivity.putExtra("savedFrom", fromField.getText().toString());
                startActivity(stationChooseActivity);
            }
        });
        swapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String from = String.valueOf(fromField.getText());
                fromField.setText(toField.getText());
                toField.setText(from);
            }
        });
        buildRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapsActivity = new Intent(RouteBuilding.this, MapsActivity.class);
                startActivity(mapsActivity);
            }
        });
        routeNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent StationViewActivity = new Intent(RouteBuilding.this, StationViewActivity.class);
                startActivity(StationViewActivity);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            String streetName = data.getStringExtra("streetName");
            double latitude = data.getDoubleExtra("latitude", 0.0);
            double longitude = data.getDoubleExtra("longitude", 0.0);
            String direction = data.getStringExtra("direction");
            Log.d("RouteBuilding", "Получен адрес: " + streetName);
            Log.d("RouteBuilding", "Координаты: " + latitude + ", " + longitude);
            Log.e("Тест","Улица: " + streetName + "\nКоординаты: " + latitude + ", " + longitude+ " Направление: "+ direction);
            if ("Откуда".equals(direction)) {
                fromField.setText(streetName);
            } else if ("Куда".equals(direction)) {
                toField.setText(streetName);
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        try {
            Intent intent = getIntent();
            String direction = intent.getStringExtra("direction");
            String valueStation = intent.getStringExtra("streetName");
            double latitude = intent.getDoubleExtra("latitude", 0.0);
            double longitude = intent.getDoubleExtra("longitude", 0.0);

            // Восстанавливаем сохраненные значения
            if (intent.hasExtra("savedFrom")) {
                savedFrom = intent.getStringExtra("savedFrom");
                fromField.setText(savedFrom);
            }
            if (intent.hasExtra("savedTo")) {
                savedTo = intent.getStringExtra("savedTo");
                toField.setText(savedTo);
            }

            // Обновляем конкретное поле, которое было выбрано
            if ("Откуда".equals(direction)) {
                fromField.setText(valueStation);
            } else if ("Куда".equals(direction)) {
                toField.setText(valueStation);
            }
            Log.e("Тест","Улица: " + valueStation + "\nКоординаты: " + latitude + ", " + longitude+ "Направление: "+ direction);
        } catch (NullPointerException e) {
            Log.e("RouteBuilding", "Ошибка при обработке intent: " + e.getMessage());
        }
    }
}
