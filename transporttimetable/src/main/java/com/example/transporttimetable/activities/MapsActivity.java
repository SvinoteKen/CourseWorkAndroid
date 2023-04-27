package com.example.transporttimetable.activities;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.example.transporttimetable.R;
import com.example.transporttimetable.helpers.DbHelper;
import com.example.transporttimetable.models.Bus;
import com.example.transporttimetable.models.Route;
import com.example.transporttimetable.models.Station;
import com.parse.Parse;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKit;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.directions.driving.VehicleType;
import com.yandex.mapkit.geometry.Circle;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.traffic.TrafficLayer;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapsActivity extends AppCompatActivity implements DrivingSession.DrivingRouteListener {
    private MapObjectCollection mapObjects;
    private static final int REQUEST_PERMISSION_PHONE_STATE = 1;
    public MapView mapview;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    public Switch traffic;
    private DrivingRouter drivingRouter = null;
    private DrivingSession drivingSession = null;
    Station station = new Station();
    Bus bus = new Bus();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ниже, вместо ??????? вставляем Ваш ключ, присланный Вам из Яндекс:
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build());
        MapKitFactory.initialize(this);
        // Укажите имя своей Activity вместо mapview. У меня она называется map
        setContentView(R.layout.map);

        mapview = (MapView)findViewById(R.id.mapView);
        mapview.getMap().move(
                new CameraPosition(new Point(48.010591, 37.838702), 11.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 5f),
                null);

        MapKit mapKit = MapKitFactory.getInstance();
        TrafficLayer probki = mapKit.createTrafficLayer(mapview.getMapWindow());
        UserLocationLayer locationUser = mapKit.createUserLocationLayer(mapview.getMapWindow());
        locationUser.setVisible(true);
        showPhoneStatePermission();
        // initiate a Switch
        traffic = (Switch) findViewById(R.id.trafficSwitch);
        traffic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    probki.setTrafficVisible(true);
                }
                if(!isChecked){
                    probki.setTrafficVisible(false);
                }
            }
        });
        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter();
        mapObjects = mapview.getMap().getMapObjects().addCollection();
        createRoute();
    }

    private void createRoute(){
        try
        {
            station.setId((int) getIntent().getSerializableExtra("stationId"));
            station.setName((String) getIntent().getSerializableExtra("stationName"));
            station.setCoordinates(new Point((double) getIntent().getSerializableExtra("stationLatitude"),(double) getIntent().getSerializableExtra("stationLongitude")));
            ArrayList<Station> stations = new ArrayList<>();
            stations.add(station);
            createTappableCircle(stations);

        } catch (NullPointerException e) {
            // обработка исключения, например:
            Toast.makeText(MapsActivity.this, "АЙДИ СТАНЦИИ НЕТ", Toast.LENGTH_SHORT).show();
        }
        try
        {
            int busId = (int) getIntent().getSerializableExtra("busId");
            boolean Reversed = (boolean) getIntent().getSerializableExtra("Reversed");
            DbHelper dbHelper = new DbHelper();
            ArrayList<Station> Stations = dbHelper.getRoutByBus(busId, Reversed);
            createTappableCircle(Stations);
            Station firstElement = Stations.get(0);
            Station lastElement = Stations.get(Stations.size() - 1);
            ArrayList<RequestPoint> points = new ArrayList<>();
            points.add(new RequestPoint(new Point(firstElement.getCoordinates().getLatitude(), firstElement.getCoordinates().getLongitude()),RequestPointType.WAYPOINT, ""));
            for (Station station : Stations) {
                points.add(new RequestPoint(new Point(station.getCoordinates().getLatitude(),station.getCoordinates().getLongitude()),RequestPointType.VIAPOINT, ""));
            }
            points.add(new RequestPoint(new Point(lastElement.getCoordinates().getLatitude(),lastElement.getCoordinates().getLongitude()), RequestPointType.WAYPOINT, ""));
            DrivingOptions options = new DrivingOptions(); // создаем объект настроек маршрута
            options.setRoutesCount(1);
            VehicleOptions v = new VehicleOptions();
            v.setVehicleType(VehicleType.TRUCK);
            drivingSession = drivingRouter.requestRoutes(points,options,v, this);

        } catch (NullPointerException e) {
            // обработка исключения, например:
            Toast.makeText(MapsActivity.this, "АЙДИ АВТОБУСА НЕТ", Toast.LENGTH_SHORT).show();
        }
    }

    private MapObjectTapListener circleMapObjectTapListener = new MapObjectTapListener() {
        @Override
        public boolean onMapObjectTap(MapObject mapObject, Point point) {
            if (mapObject instanceof CircleMapObject) {
                CircleMapObject circle = (CircleMapObject)mapObject;

                float randomRadius = 10.0f + 5.0f * new Random().nextFloat();

                Circle curGeometry = circle.getGeometry();
                Circle newGeometry = new Circle(curGeometry.getCenter(), randomRadius);
                circle.setGeometry(newGeometry);

                String userData = (String) mapObject.getUserData();
                Toast toast = Toast.makeText(
                            getApplicationContext(),
                            "Остановка "+userData,
                            Toast.LENGTH_SHORT);
                    toast.show();
            }
            return true;
        }
    };

    private void createTappableCircle(ArrayList<Station> S) {
        for (Station station : S) {
            CircleMapObject circle = mapObjects.addCircle(
                    new Circle(new Point(station.getCoordinates().getLatitude(),station.getCoordinates().getLongitude()), 15), Color.GREEN, 2, Color.WHITE);
            circle.setUserData(station.getName());
            // Client code must retain strong reference to the listener.
            circle.addTapListener(circleMapObjectTapListener);
        }
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
            Toast.makeText(MapsActivity.this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
        }
        if (permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                showExplanation("Permission Needed", "Rationale", android.Manifest.permission.ACCESS_COARSE_LOCATION);
            } else {
                requestPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_PERMISSION_PHONE_STATE);
            }
        } else {
            Toast.makeText(MapsActivity.this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showExplanation(String title, String message, final String permission) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, MapsActivity.REQUEST_PERMISSION_PHONE_STATE);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    @Override
    protected void onStop() {
        mapview.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }
    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapview.onStart();
    }

    @Override
    public void onDrivingRoutes(@NonNull List<DrivingRoute> list) {
        for (DrivingRoute route: list) {
            mapObjects.addPolyline(route.getGeometry());
        }
    }

    @Override
    public void onDrivingRoutesError(@NonNull Error error) {

    }
}