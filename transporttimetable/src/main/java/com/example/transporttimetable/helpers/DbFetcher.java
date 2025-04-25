package com.example.transporttimetable.helpers;

import android.content.Context;
import android.util.Log;

import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DbFetcher {
    private final ArrayList<BusRecord> localBuses = new ArrayList<>();
    private static final String BUSES_FILE_NAME = "buses.json";
    private final ArrayList<RouteRecord> localRoutes = new ArrayList<>();
    private static final String ROUTES_FILE_NAME = "routes.json";
    private final List<StationRecord> localStations = new ArrayList<>();
    private static final String STATIONS_FILE_NAME = "stationList.json";
    private final Context context;

    public DbFetcher(Context context) {
        this.context = context;
    }

    public void fetchAllDataFromDB(Runnable onComplete) {
        //fetchAllStations(() -> fetchAllBuses(() -> fetchAllRoutes(onComplete)));
    }
    private void fetchAllStations(Runnable next) {
        localStations.clear();
        fetchStationBatch(1, 50, next);
    }

    private void fetchStationBatch(int startId, int batchSize, Runnable onComplete) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Stations");
        query.whereGreaterThanOrEqualTo("ID", startId);
        query.whereLessThan("ID", startId + batchSize);
        query.orderByAscending("ID");

        query.findInBackground((results, e) -> {
            if (e == null && results != null && !results.isEmpty()) {
                for (ParseObject obj : results) {
                    ParseGeoPoint point = obj.getParseGeoPoint("Coordinates");
                    String name = obj.getString("Name");
                    int id = obj.getInt("ID");
                    if (point != null) {
                        localStations.add(new StationRecord(id, name, point));
                    }
                }
                fetchStationBatch(startId + batchSize, batchSize, onComplete);
            } else {
                saveStationsToFile();
                onComplete.run();
            }
        });
    }
    private void fetchAllBuses(Runnable next) {
        localBuses.clear();
        fetchBusBatch(1, 50, next);
    }

    private void fetchBusBatch(int startId, int batchSize, Runnable onComplete) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Buses");
        query.whereGreaterThanOrEqualTo("ID", startId);
        query.whereLessThan("ID", startId + batchSize);
        query.orderByAscending("ID");

        query.findInBackground((results, e) -> {
            if (e == null && results != null && !results.isEmpty()) {
                for (ParseObject obj : results) {
                    int id = obj.getInt("ID");
                    String number = obj.getString("NumberOfBus");
                    String interval = obj.getString("Interval");
                    String first = obj.getString("FirstDeparture");
                    String last = obj.getString("LastDeparture");
                    int type = obj.getInt("TransportType");
                    localBuses.add(new BusRecord(id, number, interval, first, last, type));
                }
                fetchBusBatch(startId + batchSize, batchSize, onComplete);
            } else {
                saveBusesToFile();
                onComplete.run();
            }
        });
    }

    private void fetchAllRoutes(Runnable next) {
        localRoutes.clear();
        fetchRouteBatch(1, 50, next);
    }

    private void fetchRouteBatch(int startId, int batchSize, Runnable onComplete) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Routes");
        query.whereGreaterThanOrEqualTo("ID", startId);
        query.whereLessThan("ID", startId + batchSize);
        query.orderByAscending("ID");

        query.findInBackground((results, e) -> {
            if (e == null && results != null && !results.isEmpty()) {
                for (ParseObject obj : results) {
                    int id = obj.getInt("ID");
                    int busId = obj.getInt("ID_BUS");
                    String name = obj.getString("Name");
                    String stationIds = obj.getString("ID_STATIONS");
                    String time = obj.getString("Time");
                    boolean reversed = obj.getBoolean("Reversed");

                    localRoutes.add(new RouteRecord(id, name, busId, stationIds, time, reversed));
                }
                fetchRouteBatch(startId + batchSize, batchSize, onComplete);
            } else {
                saveRoutesToFile();
                onComplete.run();
            }
        });
    }
    private void saveStationsToFile() {
        JSONArray jsonArray = new JSONArray();
        for (StationRecord s : localStations) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", s.id);
                obj.put("name", s.name);
                obj.put("lat", s.coordinates.getLatitude());
                obj.put("lon", s.coordinates.getLongitude());
                jsonArray.put(obj);
            } catch (JSONException e) {
                Log.e("StationSave", "Ошибка сериализации JSON", e);
            }
        }

        try (FileOutputStream fos = context.openFileOutput(STATIONS_FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(jsonArray.toString().getBytes());
            Log.d("StationSave", "Станции сохранены в файл.");
        } catch (IOException e) {
            Log.e("StationSave", "Ошибка записи файла", e);
        }
    }
    private void saveBusesToFile() {
        JSONArray jsonArray = new JSONArray();
        for (BusRecord b : localBuses) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", b.id);
                obj.put("number", b.number);
                obj.put("interval", b.interval);
                obj.put("first", b.firstDeparture);
                obj.put("last", b.lastDeparture);
                obj.put("type", b.transportType);
                jsonArray.put(obj);
            } catch (JSONException e) {
                Log.e("BusSave", "Ошибка сериализации JSON", e);
            }
        }

        try (FileOutputStream fos = context.openFileOutput(BUSES_FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(jsonArray.toString().getBytes());
            Log.d("BusSave", "Автобусы сохранены в файл.");
        } catch (IOException e) {
            Log.e("BusSave", "Ошибка записи файла", e);
        }
    }
    private void saveRoutesToFile() {
        JSONArray jsonArray = new JSONArray();
        for (RouteRecord r : localRoutes) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", r.id);
                obj.put("name", r.name);
                obj.put("busId", r.busId);
                obj.put("stations", r.stationIds);
                obj.put("time", r.time);
                obj.put("reversed", r.reversed);
                jsonArray.put(obj);
            } catch (JSONException e) {
                Log.e("RouteSave", "Ошибка сериализации JSON", e);
            }
        }

        try (FileOutputStream fos = context.openFileOutput(ROUTES_FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(jsonArray.toString().getBytes());
            Log.d("RouteSave", "Маршруты сохранены в файл.");
        } catch (IOException e) {
            Log.e("RouteSave", "Ошибка записи файла", e);
        }
    }
    public class BusRecord {
        public int id;
        public String number;
        public String interval;
        public String firstDeparture;
        public String lastDeparture;
        public int transportType;

        public BusRecord(int id, String number, String interval, String first, String last, int type) {
            this.id = id;
            this.number = number;
            this.interval = interval;
            this.firstDeparture = first;
            this.lastDeparture = last;
            this.transportType = type;
        }
    }

    public class RouteRecord {
        public int id;
        public String name;
        public int busId;
        public String stationIds;
        public String time;
        public boolean reversed;

        public RouteRecord(int id, String name, int busId, String stationIds, String time, boolean reversed) {
            this.id = id;
            this.name = name;
            this.busId = busId;
            this.stationIds = stationIds;
            this.time = time;
            this.reversed = reversed;
        }
    }
    public class StationRecord {
        int id;
        String name;
        ParseGeoPoint coordinates;

        StationRecord(int id, String name, ParseGeoPoint coordinates) {
            this.id = id;
            this.name = name;
            this.coordinates = coordinates;
        }
    }
}
