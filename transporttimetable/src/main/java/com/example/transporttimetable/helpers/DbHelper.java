package com.example.transporttimetable.helpers;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import com.example.transporttimetable.models.Bus;
import com.example.transporttimetable.models.Route;
import com.example.transporttimetable.models.Station;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.yandex.mapkit.geometry.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DbHelper{

    private static final List<String> keysToStations = Arrays.asList("ID", "Name", "Coordinates");
    private static final List<String> keysToBuses = Arrays.asList("ID", "NumberOfBus", "Interval", "FirsDeparture", "LastDeparture", "TransportType");
    private static final List<String> keysToRoutes = Arrays.asList("ID", "ID_BUS", "ID_STATIONS", "Name", "Reversed");
    private final ParseQuery<ParseObject> Buses;
    private final ParseQuery<ParseObject> Routes;
    private final ParseQuery<ParseObject> Stations;

    public DbHelper(){
        Stations = ParseQuery.getQuery("Stations");
        Buses = ParseQuery.getQuery("Buses");
        Routes = ParseQuery.getQuery("Routes");

    }
    public ArrayList<Station> getAllStations() {
        ArrayList<Station> Station = new ArrayList<>();
        Stations.selectKeys(keysToStations);
        try {

            List<ParseObject> results = Stations.find();

            for (ParseObject station : results) {
                Station s = new Station();
                int id = station.getInt("ID");
                String name = station.getString("Name");
                ParseGeoPoint coordinates = station.getParseGeoPoint("Coordinates");
                assert coordinates != null;
                Point point = new Point(coordinates.getLatitude(), coordinates.getLongitude());
                s.setName(name);
                s.setId(id);
                s.setCoordinates(point);
                Station.add(s);
            }
            return Station;
        } catch (ParseException e) {
            Log.e("DbHelper", "Error retrieving data: " + e.getMessage());
        }
        return Station;
    }
    public ArrayList<Bus> getAllBuses(int type) {
        ArrayList<Bus> Bus = new ArrayList<>();
        Buses.selectKeys(keysToBuses);
        try {
            Buses.whereEqualTo("TransportType", type);

            List<ParseObject> results = Buses.find();

            for (ParseObject bus : results) {
                Bus b = new Bus();
                int id = bus.getInt("ID");
                String number = bus.getString("NumberOfBus");
                String interval = bus.getString("Interval");
                String firsDeparture = bus.getString("FirsDeparture");
                String lastDeparture = bus.getString("LastDeparture");
                int transportType = bus.getInt("TransportType");
                b.setId(id);
                b.setBusNumber(number);
                b.setInterval(interval);
                b.setLastDeparture(lastDeparture);
                b.setFirstDeparture(firsDeparture);
                b.setTransportType(transportType);
                Bus.add(b);
            }
            return Bus;
        } catch (ParseException e) {
            Log.e("DbHelper", "Error retrieving data from Stations table: " + e.getMessage());
        }
        return Bus;
    }

    public ArrayList<Bus> getBusByStation(int stationId){
        ArrayList<Bus> Bus = new ArrayList<>();
        String stationIdStr = String.valueOf(stationId);
        try {
            Routes.whereContains("ID_STATIONS", stationIdStr).whereEqualTo("Reversed", false);

            List<ParseObject> routesResults = Routes.find();

            List<Integer> busIds = new ArrayList<>();

            for (ParseObject route : routesResults) {
                int busId = route.getInt("ID_BUS");
                busIds.add(busId);
            }

            Buses.whereContainedIn("ID", busIds);

            List<ParseObject> busesResults = Buses.find();

            for (ParseObject bus : busesResults) {
                Bus b = new Bus();
                int id = bus.getInt("ID");
                String numberOfBus = bus.getString("NumberOfBus");
                String interval = bus.getString("Interval");
                String firstDeparture = bus.getString("FirstDeparture");
                String lastDeparture = bus.getString("LastDeparture");
                int transportType = bus.getInt("TransportType");
                b.setId(id);
                b.setBusNumber(numberOfBus);
                b.setInterval(interval);
                b.setFirstDeparture(firstDeparture);
                b.setLastDeparture(lastDeparture);
                b.setTransportType(transportType);
                Bus.add(b);
            }
        } catch (ParseException e) {
            Log.e("DbHelper", "Error retrieving data: " + e.getMessage());
        }
        return Bus;
    }

    @SuppressLint("StaticFieldLeak")
    public void getBusesByStation(int stationId, OnBusesLoadedListener listener) {
        new AsyncTask<Integer, Void, String>() {

            @Override
            protected String doInBackground(Integer... params) {
                int stationId = params[0];
                String stationIdStr = String.valueOf(stationId);
                StringBuilder busesString = new StringBuilder();
                try {
                    Routes.whereContains("ID_STATIONS", stationIdStr).whereEqualTo("Reversed", false);

                    List<ParseObject> routesResults = Routes.find();

                    List<Integer> busIds = new ArrayList<>();

                    for (ParseObject route : routesResults) {
                        int busId = route.getInt("ID_BUS");
                        busIds.add(busId);
                    }

                    Buses.whereContainedIn("ID", busIds);

                    List<ParseObject> busesResults = Buses.find();

                    for (ParseObject bus : busesResults) {
                        String numberOfBus = bus.getString("NumberOfBus");
                        busesString.append(numberOfBus);
                        busesString.append(", ");
                    }

                    if (busesString.length() > 0) {
                        busesString.setLength(busesString.length() - 2);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                return String.valueOf(busesString);
            }

            @Override
            protected void onPostExecute(String result) {
                listener.onBusesLoaded(result);
            }
        }.execute(stationId);
    }

    public interface OnBusesLoadedListener {
        void onBusesLoaded(String buses);
    }

    public ArrayList<Route> getRoutsByBus(int busId) {
        ArrayList<Route> Route = new ArrayList<>();
        Routes.whereEqualTo("ID_BUS", busId).whereEqualTo("Reversed", false);
        try {
            List<ParseObject> routesResults = Routes.find();

            for (ParseObject route : routesResults) {
                Route r = new Route();
                int id = route.getInt("ID");
                int idS = route.getInt("ID_BUS");
                int idB = route.getInt("ID_STATION");
                String name = route.getString("Name");
                Boolean reversed = route.getBoolean("Reversed");
                r.setName(name);
                r.setId(id);
                r.setBus(idB);
                r.setStop(idS);
                r.setReversed(reversed);
                Route.add(r);
            }

            return Route;
        } catch (ParseException e) {
            Log.e("DbHelper", "Error retrieving data: " + e.getMessage());
        }
        return Route;
    }

    public ArrayList<Station> getRoutByBus(int busId, boolean Reversed) {
        ArrayList<Station> Station = new ArrayList<>();
        Routes.whereEqualTo("ID_BUS", busId).whereEqualTo("Reversed", Reversed);
        try {
            List<ParseObject> routesResults = Routes.find();

            List<Integer> stationIds = new ArrayList<>();
            for (ParseObject route : routesResults) {
                String idStations = route.getString("ID_STATIONS");
                assert idStations != null;
                String[] idArray = idStations.split(",");
                for (String idStr : idArray) {
                    stationIds.add(Integer.parseInt(idStr));
                }
            }

            Stations.selectKeys(keysToStations);
            Stations.whereContainedIn("ID", stationIds);

            List<ParseObject> stationResults = Stations.find();

            for (ParseObject station : stationResults) {
                Station s = new Station();
                int id = station.getInt("ID");
                String name = station.getString("Name");
                ParseGeoPoint coordinates = station.getParseGeoPoint("Coordinates");
                assert coordinates != null;
                Point point = new Point(coordinates.getLatitude(), coordinates.getLongitude());
                s.setName(name);
                s.setId(id);
                s.setCoordinates(point);
                Station.add(s);
            }
            return Station;
        } catch (ParseException e) {
            Log.e("DbHelper", "Error retrieving data: " + e.getMessage());
        }
        return Station;
    }
}