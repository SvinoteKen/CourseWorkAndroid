package com.example.transporttimetable.helpers;
import android.content.Context;
import android.util.Log;

import com.example.transporttimetable.models.Bus;
import com.example.transporttimetable.models.Route;
import com.example.transporttimetable.models.Station;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.yandex.mapkit.geometry.Point;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DbHelper{

    private List<Station> stationList = new ArrayList<>();
    private List<Bus> busList = new ArrayList<>();
    private List<Route> routeList = new ArrayList<>();
    Context context;

    public DbHelper(Context context) {
        this.context = context;
        loadAllData(); // Сразу загружаем все из файлов
    }

    private void loadAllData() {
        stationList.addAll(readStationsFromFile());
        busList.addAll(readBusesFromFile());
        routeList.addAll(readRoutesFromFile());
    }

    private List<Station> readStationsFromFile() {
        List<Station> result = new ArrayList<>();
        try {
            File file = new File(context.getFilesDir(), "stationList.json");
            JSONArray jsonArray = new JSONArray(readFileContent(file));
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                int id = obj.getInt("id");
                String name = obj.getString("name");
                double lat = obj.getDouble("lat");
                double lon = obj.getDouble("lon");
                Point coordinates = new Point(lat, lon);
                Station s = new Station(id, name, coordinates);
                result.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private List<Bus> readBusesFromFile() {
        List<Bus> result = new ArrayList<>();
        try {
            File file = new File(context.getFilesDir(), "buses.json");
            JSONArray jsonArray = new JSONArray(readFileContent(file));
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Bus b = new Bus();
                b.setId(obj.getInt("id"));
                b.setBusNumber(obj.getString("number"));
                b.setInterval(obj.getString("interval"));
                b.setFirstDeparture(obj.getString("first"));
                b.setLastDeparture(obj.getString("last"));
                b.setTransportType(obj.getInt("type"));
                result.add(b);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private List<Route> readRoutesFromFile() {
        List<Route> result = new ArrayList<>();
        try {
            File file = new File(context.getFilesDir(), "routes.json");
            JSONArray jsonArray = new JSONArray(readFileContent(file));
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Route r = new Route();
                r.setId(obj.getInt("id"));
                r.setName(obj.getString("name"));
                r.setBus(obj.getInt("busId"));
                r.setStop(obj.getString("stations"));
                r.setTime(obj.optString("time", ""));
                r.setReversed(obj.optBoolean("reversed", false));
                r.setDistance(obj.getString("distance"));
                result.add(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private String readFileContent(File file) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    public ArrayList<Station> getStations() {
        return new ArrayList<>(stationList);
    }
    public ArrayList<Route> getRoutes() {
        return new ArrayList<>(routeList);
    }
    public ArrayList<Station> searchStations(String name) {
        ArrayList<Station> result = new ArrayList<>();
        if (name == null) return result;

        for (Station s : stationList) {
            if (s.getName().toLowerCase().contains(name.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
    public ArrayList<Bus> getAllBuses(int type) {
        ArrayList<Bus> result = new ArrayList<>();
        for (Bus b : busList) {
            if (b.getTransportType() == type) {
                result.add(b);
            }
        }
        return result;
    }
    public ArrayList<Bus> getBusByStation(int stationId, boolean stationReversed) {
        ArrayList<Bus> result = new ArrayList<>();
        Set<Integer> busIds = new HashSet<>();

        for (Route route : routeList) {
            if (route.getReversed() == stationReversed && route.getStop().contains("," + stationId + ",")) {
                busIds.add(route.getBus());
                Log.e("BUS123"," "+busIds);
            }
        }

        for (Bus bus : busList) {
            if (busIds.contains(bus.getId())) {
                result.add(bus);
                Log.e("BUS123"," "+result);
            }
        }

        return result;
    }

    public String getBusesByStation(int stationId, boolean reversed) {
        StringBuilder busesString = new StringBuilder();
        Set<Integer> busIds = new HashSet<>();

        for (Route route : routeList) {
            Log.e("BUS","ID_ROUTES: "+route.getStop() + " Reversed: " + route.getReversed());
            if (route.getReversed() == reversed && route.getStop().contains("," + stationId + ",")) {
                busIds.add(route.getBus());

            }
        }

        for (Bus bus : busList) {
            if (busIds.contains(bus.getId())) {
                busesString.append(bus.getBusNumber()).append(", ");
                Log.e("BUS"," "+ busesString);
            }
        }

        if (busesString.length() > 0) {
            busesString.setLength(busesString.length() - 2); // remove last ", "
        }

        return busesString.toString();
    }


    public int getTimeByRoute(int idBus, int idStation) {
        for (Route route : routeList) {
            if (route.getBus() == idBus) {
                String[] idArray = route.getStop().split(",");
                String[] timeArray = route.getTime().split(",");

                List<Integer> idList = new ArrayList<>();
                for (int i = 1; i < idArray.length; i++) {
                    idList.add(Integer.parseInt(idArray[i]));
                }

                int index = idList.indexOf(idStation);
                if (index == -1) return 0;

                int totalTime = 0;
                for (int i = 0; i <= index && i < timeArray.length; i++) {
                    totalTime += Integer.parseInt(timeArray[i]);
                }

                return totalTime;
            }
        }
        return 0;
    }
    public ArrayList<Route> getRoutesByBus(int busId) {
        ArrayList<Route> result = new ArrayList<>();
        for (Route r : routeList) {
            if (r.getBus() == busId && !r.getReversed()) {
                result.add(r);
            }
        }
        return result;
    }
    public ArrayList<Station> getRoutByBus(int busId, boolean reversed) {
        ArrayList<Station> result = new ArrayList<>();

        for (Route route : routeList) {
            if (route.getBus() == busId && route.getReversed() == reversed) {
                String[] idArray = route.getStop().split(",");
                List<Integer> stationIds = new ArrayList<>();
                for (int i = 1; i < idArray.length; i++) {
                    stationIds.add(Integer.parseInt(idArray[i]));
                }

                for (int i = 0; i < stationIds.size(); i++) {
                    int id = stationIds.get(i);
                    for (Station s : stationList) {
                        if (s.getId() == id) {
                            Station copy = new Station(s.getId(), s.getName(), s.getCoordinates());
                            copy.setIndex(i);
                            result.add(copy);
                            break;
                        }
                    }
                }
                break;
            }
        }
        return result;
    }
    public ArrayList<Station> getRoutByBus(int busId) {
        ArrayList<Station> result = new ArrayList<>();

        for (Route route : routeList) {
            if (route.getBus() == busId) {
                String[] idArray = route.getStop().split(",");
                List<Integer> stationIds = new ArrayList<>();
                for (int i = 1; i < idArray.length; i++) {
                    stationIds.add(Integer.parseInt(idArray[i]));
                }

                for (int i = 0; i < stationIds.size(); i++) {
                    int id = stationIds.get(i);
                    for (Station s : stationList) {
                        if (s.getId() == id) {
                            Station copy = new Station(s.getId(), s.getName(), s.getCoordinates());
                            copy.setIndex(i);
                            result.add(copy);
                            break;
                        }
                    }
                }
                break;
            }
        }
        return result;
    }
}