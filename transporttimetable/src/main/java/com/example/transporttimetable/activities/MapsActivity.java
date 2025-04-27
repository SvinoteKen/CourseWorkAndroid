package com.example.transporttimetable.activities;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.transporttimetable.R;
import com.example.transporttimetable.helpers.DbHelper;
import com.example.transporttimetable.helpers.RouteAdapter;
import com.example.transporttimetable.helpers.RouteUploader;
import com.example.transporttimetable.models.FoundRoute;
import com.example.transporttimetable.models.RouteModel;
import com.example.transporttimetable.models.Station;
import com.example.transporttimetable.models.Step;
import com.example.transporttimetable.models.StopModel;
import com.example.transporttimetable.routes_finders.RouteFinder;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.parse.Parse;
import com.parse.ParseObject;
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
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.search.Response;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.traffic.TrafficLayer;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.mapkit.transport.masstransit.FilterVehicleTypes;
import com.yandex.mapkit.transport.masstransit.Flags;
import com.yandex.mapkit.transport.masstransit.MasstransitRouter;
import com.yandex.mapkit.transport.masstransit.PedestrianRouter;
import com.yandex.mapkit.transport.masstransit.Route;
import com.yandex.mapkit.transport.masstransit.RouteStop;
import com.yandex.mapkit.transport.masstransit.Section;
import com.yandex.mapkit.transport.masstransit.SectionMetadata;
import com.yandex.mapkit.transport.masstransit.Session;
import com.yandex.mapkit.transport.masstransit.Stop;
import com.yandex.mapkit.transport.masstransit.Summary;
import com.yandex.mapkit.transport.masstransit.SummarySession;
import com.yandex.mapkit.transport.masstransit.TimeOptions;
import com.yandex.mapkit.transport.masstransit.TransitOptions;
import com.yandex.mapkit.transport.masstransit.Transport;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.network.NetworkError;
import com.yandex.runtime.network.RemoteError;
import com.yandex.mapkit.search.*;

import java.util.ArrayList;
import java.util.Arrays;
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
    public String descrip = null;
    public int ID_BUS = 0;
    public int IDD = 0;
    public String busNumber = null;
    public Boolean Rev = false;
    private boolean fromRouteBuilding = false; // Флаг, вызван ли из RouteBuilding
    Station station = new Station();
    BottomSheetBehavior<View> bottomSheetBehavior;
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
        View dimView = findViewById(R.id.dimView);
        View bottomSheet = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        int quarterHeight = screenHeight / 3;

        bottomSheetBehavior.setPeekHeight(quarterHeight);
        // Начальное состояние — свернуто
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        // Слушатель состояния
        // Затемнение + переключения состояний
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        dimView.setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        dimView.setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        dimView.setVisibility(View.GONE);
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Плавное затемнение
                dimView.setAlpha(slideOffset);
                if (slideOffset > 0) {
                    dimView.setVisibility(View.VISIBLE);
                } else {
                    dimView.setVisibility(View.GONE);
                }
            }
        });


        mapview = (MapView)findViewById(R.id.mapView);
        mapview.getMap().move(
                new CameraPosition(new Point(48.010591, 37.838702), 11.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 5f),
                null);

        MapKit mapKit = MapKitFactory.getInstance();
        TrafficLayer trafficJams = mapKit.createTrafficLayer(mapview.getMapWindow());
        UserLocationLayer locationUser = mapKit.createUserLocationLayer(mapview.getMapWindow());
        locationUser.setVisible(true);

        /*traffic = (Switch) findViewById(R.id.trafficSwitch);
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
        });*/
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
        ArrayList<RequestPoint> points = new ArrayList<>();
        TransitOptions transitOptions = new TransitOptions(FilterVehicleTypes.NONE.value, new TimeOptions());
        Point firstElement = new Point(48.063442, 37.772343);
        Point lastElement = new Point(48.013572, 37.865598);
        descrip = "ДС Центр - Герцена";
        ID_BUS = 7;
        IDD = 3;
        Rev = false;
        busNumber = "4";
        points.add(new RequestPoint(new Point(firstElement.getLatitude(),
                firstElement.getLongitude()),RequestPointType.WAYPOINT, ""));
        points.add(new RequestPoint(new Point(lastElement.getLatitude(),
                lastElement.getLongitude()), RequestPointType.WAYPOINT, ""));
        // drivingSession = drivingRouter.requestRoutes(points,transitOptions,this);
        DbHelper db = new DbHelper(this);
        List<Station> stations = db.getStations();
        List<com.example.transporttimetable.models.Route> routes = db.getRoutes();
        RouteFinder rf = new RouteFinder(this, stations, routes);
        rf.findRoutes(firstElement, lastElement, new RouteFinder.RouteCallback() {
            @Override
            public void onRoutesFound(List<FoundRoute> routes) {
                // Здесь обработка результата
                if (routes.isEmpty()) {
                    Log.e("RouteFinder", "Маршруты не найдены");
                } else {
                    for (FoundRoute route : routes) {
                        // Логируем общую информацию о маршруте
                        Log.e("RouteFinder", "=== Новый FoundRoute ===");
                        Log.e("RouteFinder", "TotalTime (мин): " + route.getTotalTime());
                        Log.e("RouteFinder", "Number of parts: " + route.parts.size());

                        // Пробегаемся по каждому шагу
                        for (int i = 0; i < route.parts.size(); i++) {
                            Step step = route.parts.get(i);
                            if (step instanceof Step.Walk) {
                                Log.e("RouteFinder", String.format(
                                        "Part %d: WALK (пешком)", i
                                ));
                            }
                            else if (step instanceof Step.Bus) {
                                Step.Bus bus = (Step.Bus) step;
                                Log.e("RouteFinder", String.format(
                                        "Part %d: BUS №%s, stops IDs = %s",
                                        i,
                                        bus.getBusNumber(),
                                        bus.getStations().toString()
                                ));
                            }
                            else if (step instanceof Step.Transfer) {
                                Step.Transfer tr = (Step.Transfer) step;
                                Log.e("RouteFinder", String.format(
                                        "Part %d: TRANSFER, wait time = %s",
                                        i,
                                        tr.time
                                ));
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("MAPS", "FFFFFFFFFFFFFFFFFFFFFFF" );
        try {
            Intent intent = getIntent();
            String direction = intent.getStringExtra("Test");
            Log.e("MAPS", "DDDDDDDDDDDDDDDDDDD" );
            if(Objects.equals(direction, "1")){
                Log.e("MAPS", "22222222222222222" );
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
            GridView gridView = findViewById(R.id.routesGridView);
//            List<RouteModel> routes = new ArrayList<>();
//
//            routes.add(new RouteModel(
//                    "07:00–07:30 (30 мин, 10 остановок)",
//                    Arrays.asList(
//                            new Step.Walk(),
//                            new Step.Bus("3", Arrays.asList(
//                                    new StopModel(1,"Остановка 1", "07:05"),
//                                    new StopModel(2,"Остановка 2", "07:10"),
//                                    new StopModel(3, "Остановка 3", "07:15")
//                            )),
//                            new Step.Transfer("13 мин"),
//                            new Step.Bus("49", Arrays.asList(
//                                    new StopModel(4,"Остановка 4", "07:20"),
//                                    new StopModel(5,"Остановка 5", "07:25"),
//                                    new StopModel(6,"Остановка 6", "07:30")
//                            )),
//                            new Step.Walk()
//                    )));
//
//            routes.add(new RouteModel(
//                    "07:10–07:40 (30 мин, 12 остановок)",
//                    Arrays.asList(
//                            new Step.Walk(),
//                            new Step.Bus("122", Arrays.asList(
//                                    new StopModel(1,"Остановка 7", "07:15"),
//                                    new StopModel(2,"Остановка 8", "07:20"),
//                                    new StopModel(3,"Остановка 9", "07:25"),
//                                    new StopModel(4,"Остановка 10", "07:30")
//                            )),
//                            new Step.Walk()
//                    )));
//
//            RouteAdapter routeAdapter = new RouteAdapter(this, routes);
//            gridView.setAdapter(routeAdapter);
            GridView routesGridView = findViewById(R.id.routesGridView);
            Button showAllButton = findViewById(R.id.showAllVariantsButton);


        }
        catch (NullPointerException e) {
            Log.e("Ошибка при обработке intent", "Ошибка при обработке intent: " + e.getMessage());
        }
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
            DbHelper dbHelper = new DbHelper(this);
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



//    @Override
//    public void onMasstransitRoutes(List<Route> routes) {
//        RouteUploader uploader = new RouteUploader(this);
//        if (routes.isEmpty()) {
//            Log.e("Route", "Маршруты не найдены");
//            return;
//        }
//
//        for (Route route : routes) {
//            boolean isBus4Found = false;
//
//            if(route.getMetadata().getWeight().getTransfersCount()==0){
//            for (Section section : route.getSections()) {
//                if (section.getMetadata().getData().getTransports() != null) {
//                    List<Transport> transports = section.getMetadata().getData().getTransports();
//                    com.yandex.mapkit.transport.masstransit.Line line = transports.get(0).getLine();
//                        String routeName = line.getName(); // или line.getRouteId()
//                        Log.e("Название автобуса","Название: " + routeName);
//                        if (busNumber.equals(routeName)) {
//                            Log.e("ВОШЛИ", "Проверка");
//                            isBus4Found = true;
//                            drawSection(
//                                    section.getMetadata().getData(),
//                                    SubpolylineHelper.subpolyline(route.getGeometry(), section.getGeometry())
//                            );
//                            List<RouteStop> stops = section.getStops();
//                            if (stops != null) {
//                                uploader.initializeStations(stops, () -> {
//                                    List<Integer> ids = uploader.getStationIdList();
//                                    String idStations = ids != null && !ids.isEmpty()
//                                            ? "," + TextUtils.join(",", ids) + ","
//                                            : null;
//
//                                    if (idStations == null) {
//                                        Log.e("Uploader", "IDs list is null or empty");
//                                        return;
//                                    }
//
//                                    Log.d("ROUTE_RESULT", "ID_STATIONS = " + idStations);
//
//                                    ParseObject routed = new ParseObject("Routes");
//                                    routed.put("ID", IDD);
//                                    routed.put("ID_STATIONS", idStations);
//                                    routed.put("Name", descrip);
//                                    routed.put("ID_BUS", ID_BUS);
//                                    routed.put("Reversed", Rev);
//                                    routed.put("Time", new String(new char[ids.size()]).replace('\0', ','));
//                                    // routed.saveInBackground();
//                                });
//                            }
//                        }
//                }
//
//            }
//            }
//        }
//    }
    @Override
    public void onMasstransitRoutes(List<Route> routes) {
        if (routes.size() > 0) {
            for (Section section : routes.get(0).getSections()) {
                drawSection(
                        section.getMetadata().getData(),
                        SubpolylineHelper.subpolyline(
                                routes.get(0).getGeometry(), section.getGeometry()));
                // 👇 Здесь получаем остановки
                List<RouteStop> stops = section.getStops();
                if (stops != null) {
                    for (RouteStop stop : stops) {
                        String stopName = stop.getMetadata().getStop().getName(); // название остановки
                        Point coords = stop.getPosition(); // координаты остановки
                        Log.d("Stop", "Остановка: " + stopName + " / " + coords);
                    }
                }
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