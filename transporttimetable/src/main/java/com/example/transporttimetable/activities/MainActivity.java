package com.example.transporttimetable.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;

import com.example.transporttimetable.R;
import com.example.transporttimetable.helpers.DbFetcher;
import com.example.transporttimetable.helpers.DbHelper;
import com.example.transporttimetable.models.FoundRoute;
import com.example.transporttimetable.models.Station;
import com.example.transporttimetable.routes_finders.RouteFinder;
import com.parse.Parse;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.geometry.Point;
import com.yandex.runtime.i18n.I18nManagerFactory;

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

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_PERMISSION_PHONE_STATE = 1;
    private DbFetcher dbFetcher;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build());
        dbFetcher = new DbFetcher(this);

        dbFetcher.fetchAllDataFromDB(() -> {
            // Этот код выполнится после завершения загрузки всех данных и записи в файлы
            runOnUiThread(() -> {
                Toast.makeText(this, "Данные успешно загружены и сохранены", Toast.LENGTH_SHORT).show();
                // Можешь вызвать следующий шаг, например — открыть другую активити или обновить UI
            });
        });
        Locale locale = new Locale("ru_RU");
        Locale.setDefault(locale);
        MapKitFactory.setLocale("ru_RU");
        MapKitFactory.setApiKey("d2e547f0-6e63-4f93-9ebe-08bc3a8c3757");
        showPhoneStatePermission();
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

        final Button button_routes = (Button)findViewById(R.id.button_routs);
        button_routes.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent stationViewActivity = new Intent(MainActivity.this,RoutesViewActivity.class);
                startActivity(stationViewActivity);
            }
        });

        final Button button_stations = (Button)findViewById(R.id.button_station);
        button_stations.setOnClickListener(new View.OnClickListener() {

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

    private void showPhoneStatePermission() {
        int permissionCheck1 = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        int permissionCheck2 = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permissionCheck1 != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                showExplanation("Permission Needed", "Rationale", android.Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                requestPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_PERMISSION_PHONE_STATE);
            }
        } else {
            Toast.makeText(MainActivity.this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
        }
        if (permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                showExplanation("Permission Needed", "Rationale", android.Manifest.permission.ACCESS_COARSE_LOCATION);
            } else {
                requestPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_PERMISSION_PHONE_STATE);
            }
        } else {
            Toast.makeText(MainActivity.this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showExplanation(String title, String message, final String permission) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, MainActivity.REQUEST_PERMISSION_PHONE_STATE);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

}
