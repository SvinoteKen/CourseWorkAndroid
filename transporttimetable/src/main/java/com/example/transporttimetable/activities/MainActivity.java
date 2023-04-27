package com.example.transporttimetable.activities;

import android.os.Bundle;

import com.example.transporttimetable.R;
import com.example.transporttimetable.helpers.DbHelper;
import com.example.transporttimetable.models.Station;
import com.parse.Parse;
import com.parse.ParseObject;
import com.yandex.mapkit.MapKitFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MapKitFactory.setApiKey("43cc20bb-8e7f-4168-a3c0-910cd451d895");
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("GPS выключен")
                    .setCancelable(false)
                    .setPositiveButton("Настройки GPS", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, 1);
                        }
                    })
                    .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int id) {
                            finish();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        }

        final Button button_map = (Button)findViewById(R.id.button_map);
        button_map.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent mapsActivity = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(mapsActivity);

            }
        });

        final Button button_routs = (Button)findViewById(R.id.button_routs);
        button_routs.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent stationViewActivity = new Intent(MainActivity.this,RoutesViewActivity.class);
                startActivity(stationViewActivity);
            }
        });

        final Button button_settings = (Button)findViewById(R.id.button_poi);
        button_settings.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Intent stationViewActivity = new Intent(MainActivity.this,StationViewActivity.class);
                startActivity(stationViewActivity);
            }
        });

        final Button button_exit = (Button)findViewById(R.id.button_exit);
        button_exit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
