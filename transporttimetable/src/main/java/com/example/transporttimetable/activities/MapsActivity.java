package com.example.transporttimetable.activities;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.appsearch.SearchResult;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.example.transporttimetable.R;
import com.example.transporttimetable.helpers.DbHelper;
import com.example.transporttimetable.models.Station;
import com.parse.Parse;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.GeoObject;
import com.yandex.mapkit.MapKit;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.geometry.Circle;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.SubpolylineHelper;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.places.panorama.PanoramaService;
import com.yandex.mapkit.search.Response;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.traffic.TrafficLayer;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.mapkit.transport.masstransit.FilterVehicleTypes;
import com.yandex.mapkit.transport.masstransit.MasstransitRouter;
import com.yandex.mapkit.transport.masstransit.Route;
import com.yandex.mapkit.transport.masstransit.Section;
import com.yandex.mapkit.transport.masstransit.SectionMetadata;
import com.yandex.mapkit.transport.masstransit.Session;
import com.yandex.mapkit.transport.masstransit.TimeOptions;
import com.yandex.mapkit.transport.masstransit.TransitOptions;
import com.yandex.mapkit.transport.masstransit.Transport;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.runtime.Error;
import com.yandex.runtime.i18n.I18nManagerFactory;
import com.yandex.runtime.network.NetworkError;
import com.yandex.runtime.network.RemoteError;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.search.*;
import com.yandex.runtime.Error;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class MapsActivity extends AppCompatActivity implements Session.RouteListener {
    private MapObjectCollection mapObjects;
    Button routeBuilderButton;
    public MapView mapview;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    public Switch traffic;
    private MasstransitRouter drivingRouter;
    private Session drivingSession = null;
    private Session drivingSession2 = null;
    private static final int DELAY_MILLIS = 5000; // 5 seconds delay
    private final Point TARGET_LOCATION = new Point(48.010591, 37.838702);
    private SearchManager searchManager;
    private Session searchSession;
    private boolean fromRouteBuilding = false; // Флаг, вызван ли из RouteBuilding
    Station station = new Station();
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build());

        MapKitFactory.initialize(this);

        setContentView(R.layout.map);

        mapview = (MapView)findViewById(R.id.mapView);
        mapview.getMap().move(
                new CameraPosition(new Point(48.010591, 37.838702), 11.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 5f),
                null);

        MapKit mapKit = MapKitFactory.getInstance();
        TrafficLayer trafficJams = mapKit.createTrafficLayer(mapview.getMapWindow());
        UserLocationLayer locationUser = mapKit.createUserLocationLayer(mapview.getMapWindow());
        locationUser.setVisible(true);

        traffic = (Switch) findViewById(R.id.trafficSwitch);
        traffic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    trafficJams.setTrafficVisible(true);
                }
                if(!isChecked){
                    trafficJams.setTrafficVisible(false);
                }
            }
        });
        drivingRouter = TransportFactory.getInstance().createMasstransitRouter();
        mapObjects = mapview.getMap().getMapObjects().addCollection();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                drawOnMap();
            }
        }, DELAY_MILLIS);
        routeBuilderButton = findViewById(R.id.routeBuilderButton);
        Intent intent = getIntent();
        if (intent.hasExtra("buttonText")) {
            routeBuilderButton.setText(intent.getStringExtra("buttonText"));
            fromRouteBuilding = true;
        }
        routeBuilderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fromRouteBuilding) {
                    Log.e("ИЗ","2222222222222222222222222222222222");
                    getAddressFromMapCenter(); // Если вызвано из RouteBuilding, получаем адрес
                } else {
                    Log.e("Неиз","1111111111111111111111111111111");
                    // Если вызвано НЕ из RouteBuilding, открываем RouteBuilding
                    Intent routeIntent = new Intent(MapsActivity.this, RouteBuilding.class);
                    startActivity(routeIntent);
                }

            }
        });

    }


    // Метод для получения адреса в центре карты
    private void getAddressFromMapCenter() {
        // 1. Получаем центр карты
        Point center = mapview.getMap().getCameraPosition().getTarget();
        // 2. Создаем экземпляр SearchManager
        SearchManager searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);
        // 3. Определяем параметры поиска
        SearchOptions searchOptions = new SearchOptions();
        searchOptions.setSearchTypes(SearchType.GEO.value);  // Ищем только географические объекты
        searchOptions.setResultPageSize(1);  // Нам нужен только один результат
        // 4. Запускаем сессию поиска
        searchManager.submit(center, 16, searchOptions, new com.yandex.mapkit.search.Session.SearchListener() {
            @Override
            public void onSearchResponse(Response response) {
                if (!response.getCollection().getChildren().isEmpty()) {
                    GeoObject searchResult = response.getCollection().getChildren().get(0).getObj();
                    if (searchResult != null) {
                        ToponymObjectMetadata metadata = searchResult.getMetadataContainer().getItem(ToponymObjectMetadata.class);
                        String streetName = metadata.getAddress().getFormattedAddress();
                        Log.d("Search", "Найден адрес: " + streetName);

                        // Передача адреса обратно в RouteBuilding
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("latitude", center.getLatitude());
                        resultIntent.putExtra("longitude", center.getLongitude());
                        resultIntent.putExtra("streetName", streetName);
                        setResult(Activity.RESULT_OK, resultIntent);
                        Intent intent = getIntent();
                        if (intent.hasExtra("savedFrom")) {
                            resultIntent.putExtra("savedFrom", intent.getStringExtra("savedFrom"));
                            resultIntent.putExtra("direction", "Куда");
                        }
                        if (intent.hasExtra("savedTo")) {
                            resultIntent.putExtra("savedTo", intent.getStringExtra("savedTo"));
                            resultIntent.putExtra("direction", "Откуда");
                        }
                        finish();
                    }
                }
            }

            @Override
            public void onSearchError(Error error) {
                Log.e("Search", "Ошибка поиска: " + error.toString());
            }
        });
    }

    private void setMapZoom(Point point, float zoom){
        mapview.getMap().move(
                new CameraPosition(new Point(point.getLatitude(), point.getLongitude()), zoom, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 5f),
                null);
    }

    private void drawOnMap(){
        ArrayList<RequestPoint> points = new ArrayList<>();
        TransitOptions transitOptions = new TransitOptions(FilterVehicleTypes.BUS.value, new TimeOptions());
        try
        {
            Point point = new Point((double) getIntent().getSerializableExtra("stationLatitude"),
                    (double) getIntent().getSerializableExtra("stationLongitude"));
            setMapZoom(point,30.0f);
            station.setId((int) getIntent().getSerializableExtra("stationId"));
            station.setName((String) getIntent().getSerializableExtra("stationName"));
            station.setCoordinates(point);
            ArrayList<Station> stations = new ArrayList<>();
            stations.add(station);
            createTappableCircle(stations);

        } catch (NullPointerException e) {
            Log.e("createRoute1", e.getMessage());
        }
        try
        {
            setMapZoom(TARGET_LOCATION,11.0f);
            int busId = (int) getIntent().getSerializableExtra("busId");
            boolean Reversed = (boolean) getIntent().getSerializableExtra("Reversed");
            DbHelper dbHelper = new DbHelper();
            ArrayList<Station> Stations = dbHelper.getRoutByBus(busId, Reversed);
            Collections.sort(Stations, Station.getIndexComparator());
            createTappableCircle(Stations);
            Station firstElement = Stations.get(0);
            Station lastElement = Stations.get(Stations.size() - 1);
            points.add(new RequestPoint(new Point(firstElement.getCoordinates().getLatitude(),
                    firstElement.getCoordinates().getLongitude()),RequestPointType.WAYPOINT, ""));
            /*for (int i = 1; i < Stations.size() - 1; i++) {
                Station station = Stations.get(i);
                points.add(new RequestPoint(new Point(station.getCoordinates().getLatitude(),
                        station.getCoordinates().getLongitude()),RequestPointType.VIAPOINT, ""));
            }*/
            points.add(new RequestPoint(new Point(lastElement.getCoordinates().getLatitude(),
                    lastElement.getCoordinates().getLongitude()), RequestPointType.WAYPOINT, ""));
            if(points.size()>19){
                ArrayList<RequestPoint> points1 = new ArrayList<>(points.subList(0, 19));
                ArrayList<RequestPoint> points2 = new ArrayList<>();
                RequestPoint lastPoint = points1.remove(18);
                points1.add(new RequestPoint(lastPoint.getPoint(), RequestPointType.WAYPOINT, ""));

                RequestPoint firstPoint = points.get(18);
                points2.add(new RequestPoint(firstPoint.getPoint(), RequestPointType.WAYPOINT, ""));
                points2.addAll(points.subList(17, points.size()));

                RequestPoint lastPoint2 = points.remove(points.size() - 1);
                points2.add(new RequestPoint(lastPoint2.getPoint(), RequestPointType.WAYPOINT, ""));
                drivingSession = drivingRouter.requestRoutes(points1,transitOptions,this);
                drivingSession2 = drivingRouter.requestRoutes(points2,transitOptions,this);}
            else {
                drivingSession = drivingRouter.requestRoutes(points,transitOptions,this);
            }

        } catch (NullPointerException e) {
            Log.e("createRoute2", e.getMessage());
        }
    }

    private final MapObjectTapListener circleMapObjectTapListener = new MapObjectTapListener() {
        @Override
        public boolean onMapObjectTap(@NonNull MapObject mapObject, @NonNull Point point) {
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
                    new Circle(new Point(station.getCoordinates().getLatitude(),
                            station.getCoordinates().getLongitude()), 15), Color.GREEN, 2, Color.WHITE);
            circle.setUserData(station.getName());

            circle.addTapListener(circleMapObjectTapListener);
        }
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
    public void onMasstransitRoutes(List<Route> routes) {
        if (routes.size() > 0) {
            for (Section section : routes.get(0).getSections()) {
                drawSection(
                        section.getMetadata().getData(),
                        SubpolylineHelper.subpolyline(
                                routes.get(0).getGeometry(), section.getGeometry()));
            }
        }
    }

    @Override
    public void onMasstransitRoutesError(@NonNull Error error) {
        String errorMessage = getString(R.string.unknown_error_message);
        if (error instanceof RemoteError) {
            errorMessage = getString(R.string.remote_error_message);
        } else if (error instanceof NetworkError) {
            errorMessage = getString(R.string.network_error_message);
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        Log.e("MasstransitRoute",error.toString());
    }

    private void drawSection(SectionMetadata.SectionData data,
                             Polyline geometry) {
        PolylineMapObject polylineMapObject = mapObjects.addPolyline(geometry);
        if (data.getTransports() != null) {
            for (Transport transport : data.getTransports()) {
                Log.d("drawSection", "Transport type: " + transport.getLine().getVehicleTypes());
                if (transport.getLine().getStyle() != null) {
                    polylineMapObject.setStrokeColor(
                            transport.getLine().getStyle().getColor() | 0xFF000000
                    );
                    return;
                }
            }
            HashSet<String> knownVehicleTypes = new HashSet<>();
            knownVehicleTypes.add("bus");
            knownVehicleTypes.add("tramway");
            knownVehicleTypes.add("trolleybus");
            for (Transport transport : data.getTransports()) {
                String sectionVehicleType = getVehicleType(transport, knownVehicleTypes);
                if ("bus".equals(sectionVehicleType)) {  // Избегаем sectionVehicleType.equals("bus")
                    polylineMapObject.setStrokeColor(0xFF00FF00);  // Green
                    return;
                } else if ("tramway".equals(sectionVehicleType)) {
                    polylineMapObject.setStrokeColor(0xFFFF0000);  // Red
                    return;
                } else if ("trolleybus".equals(sectionVehicleType)) {
                    polylineMapObject.setStrokeColor(0xFFFFAA00);  // Yellow
                    return;
                }

            }
        } else {
            polylineMapObject.setStrokeColor(0xFF0000FF);  // Blue
        }
    }

    private String getVehicleType(Transport transport, HashSet<String> knownVehicleTypes) {
        for (String type : transport.getLine().getVehicleTypes()) {
            if (knownVehicleTypes.contains(type)) {
                return type;
            }
        }
        return null;
    }
}