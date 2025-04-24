package com.example.transporttimetable.helpers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.transport.masstransit.RouteStop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RouteUploader {
    private static final String STATIONS_FILE_NAME = "stations.json";
    private final Context context;
    private final List<Integer> stationIdList = new ArrayList<>();
    private final List<StationRecord> localStations = new ArrayList<>();
    private int nextStationId = 1;
    private boolean initialLoadComplete = true;

    public RouteUploader(Context context) {
        this.context = context;
    }

    public void initializeStations(List<RouteStop> stops, Runnable onComplete) {
        loadStationsFromFile();
        Log.e("ФАЙЛ",context.getFilesDir().getAbsolutePath());
        if (!initialLoadComplete) {
            fetchAllStationsFromDB(() -> {
                saveStationsToFile();
                processStopsSequentially(stops, 0, onComplete);
            });
        } else {
            processStopsSequentially(stops, 0, onComplete);
        }
    }

    private void fetchAllStationsFromDB(Runnable onComplete) {
        localStations.clear();
        final int batchSize = 50;
        fetchStationBatch(1, batchSize, onComplete);
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
                        nextStationId = Math.max(nextStationId, id + 1);
                    }
                }
                fetchStationBatch(startId + batchSize, batchSize, onComplete);
            } else {
                initialLoadComplete = true;
                onComplete.run();
            }
        });
    }

    private void processStopsSequentially(List<RouteStop> stops, int index, Runnable onComplete) {
        if (index >= stops.size()) {
            Log.d("RouteUploader", "Все станции обработаны: " + stationIdList);
            onComplete.run();
            return;
        }

        RouteStop stop = stops.get(index);
        String stopName = stop.getMetadata().getStop().getName();
        Point coords = stop.getPosition();
        ParseGeoPoint geoPoint = new ParseGeoPoint(coords.getLatitude(), coords.getLongitude());

        checkOrCreateStation(stopName, geoPoint, stationId -> {
            stationIdList.add(stationId);
            processStopsSequentially(stops, index + 1, onComplete);
        });
    }

    private void checkOrCreateStation(String name, ParseGeoPoint coords, StationIdCallback callback) {
        for (StationRecord record : localStations) {
            if (areCoordinatesEqual(record.coordinates, coords)) {
                callback.onResult(record.id);
                return;
            }
        }

        // Остановка не найдена — создаём новую
        int newId = nextStationId++;
        StationRecord newRecord = new StationRecord(newId, name, coords);
        localStations.add(newRecord);
        appendStationToFile(newRecord); // добавляем только одну новую станцию в файл

        ParseObject station = new ParseObject("Stations");
        station.put("ID", newId);
        station.put("Name", name);
        station.put("Coordinates", coords);
        station.saveInBackground();

        Log.d("StationNew", "Новая остановка: ID = " + newId + ", Name = " + name);
        callback.onResult(newId);
    }
    private void appendStationToFile(StationRecord newRecord) {
        try {
            File file = new File(context.getFilesDir(), STATIONS_FILE_NAME);
            JSONArray stationsArray;

            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                byte[] bytes = new byte[fis.available()];
                fis.read(bytes);
                fis.close();

                String json = new String(bytes);
                stationsArray = new JSONArray(json);
            } else {
                stationsArray = new JSONArray();
            }

            // Добавляем новую станцию
            JSONObject obj = new JSONObject();
            obj.put("id", newRecord.id);
            obj.put("name", newRecord.name);
            obj.put("lat", newRecord.coordinates.getLatitude());
            obj.put("lon", newRecord.coordinates.getLongitude());
            stationsArray.put(obj);

            // Сохраняем обновлённый массив в файл
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(stationsArray.toString().getBytes());
            fos.close();

            Log.d("StationAppend", "Добавлена новая остановка в файл: ID = " + newRecord.id);
        } catch (IOException | JSONException e) {
            Log.e("StationAppend", "Ошибка добавления станции в файл", e);
        }
    }
    private boolean areCoordinatesEqual(ParseGeoPoint p1, ParseGeoPoint p2) {
        return Double.compare(p1.getLatitude(), p2.getLatitude()) == 0 &&
                Double.compare(p1.getLongitude(), p2.getLongitude()) == 0;
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

    private void loadStationsFromFile() {
        localStations.clear();
        try (FileInputStream fis = context.openFileInput(STATIONS_FILE_NAME)) {
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            String json = new String(bytes);
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                int id = obj.getInt("id");
                String name = obj.getString("name");
                double lat = obj.getDouble("lat");
                double lon = obj.getDouble("lon");
                localStations.add(new StationRecord(id, name, new ParseGeoPoint(lat, lon)));
                nextStationId = Math.max(nextStationId, id + 1);
            }
            initialLoadComplete = true;
            Log.d("StationLoad", "Загружено из файла: " + localStations.size());
        } catch (FileNotFoundException e) {
            Log.d("StationLoad", "Файл не найден, будет создан позже.");
        } catch (IOException | JSONException e) {
            Log.e("StationLoad", "Ошибка чтения файла", e);
        }
    }

    public List<Integer> getStationIdList() {
        return new ArrayList<>(stationIdList);
    }

    private interface StationIdCallback {
        void onResult(int stationId);
    }

    private static class StationRecord {
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
