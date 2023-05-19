package com.example.transporttimetable;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.example.transporttimetable.helpers.DbHelper;
import com.example.transporttimetable.models.Bus;
import com.example.transporttimetable.models.Route;
import com.example.transporttimetable.models.Station;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExampleInstrumentedTest{

    private void initDB(){
        Parse.initialize(new Parse.Configuration.Builder(appContext)
                .applicationId("KbkDBhHTaUZUb6GELN3rIbt6SqrI6bmcDiv9ctU8")
                .clientKey("GF2mAlRIIRZBgZTYoV6qWTfY0xNtNWMEB59SS3qc")
                .server("https://parseapi.back4app.com")
                .build());
    }

    private Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private DbHelper dbHelper;

    @Test
    public void testGetSearchStations() throws ParseException {
        initDB();
        // Создаем заглушку для объекта ParseQuery<ParseObject>
        ParseQuery<ParseObject> mockStationsQuery = Mockito.mock(ParseQuery.class);
        dbHelper = new DbHelper();
        // Создаем фиктивные данные для возвращения
        List<ParseObject> fakeResults = new ArrayList<>();
        ParseObject stationObject = new ParseObject("Stations");
        stationObject.put("ID", 3);
        stationObject.put("Name", "ДС Крытый рынок");
        ParseGeoPoint coordinates = new ParseGeoPoint(48.011747, 37.811048);
        stationObject.put("Coordinates", coordinates);
        fakeResults.add(stationObject);
        // Создаем заглушку для метода find() объекта mockStationsQuery
        when(mockStationsQuery.find()).thenReturn(fakeResults);
        // Вызываем тестируемый метод
        ArrayList<Station> stations = dbHelper.searchStations("ДС Крытый рынок");
        // Проверяем результаты
        Assert.assertEquals(1, stations.size());
        Station station = stations.get(0);
        Assert.assertEquals(3, station.getId());
        Assert.assertEquals("ДС Крытый рынок", station.getName());
        Assert.assertEquals(48.011747, station.getCoordinates().getLatitude(), 0.0);
        Assert.assertEquals(37.811048, station.getCoordinates().getLongitude(), 0.0);
    }
    @Test
    public void testGetAllBuses() throws ParseException {
        initDB();
        dbHelper = new DbHelper();
        // Создаем заглушку для объекта ParseQuery<ParseObject>
        ParseQuery<ParseObject> mockStationsQuery = Mockito.mock(ParseQuery.class);
        // Создаем фиктивные данные для возвращения
        List<ParseObject> fakeResults = new ArrayList<>();
        ParseObject busObject1 = new ParseObject("Buses");
        busObject1.put("ID", 7);
        busObject1.put("NumberOfBus", 4);
        busObject1.put("Interval", 20);
        busObject1.put("FirstDeparture", "05:16");
        busObject1.put("LastDeparture", "22:08");
        busObject1.put("TransportType", 3);
        fakeResults.add(busObject1);

        ParseObject busObject2 = new ParseObject("Buses");
        busObject2.put("ID", 8);
        busObject2.put("NumberOfBus", 9);
        busObject2.put("Interval", 15);
        busObject2.put("FirstDeparture", "04:26");
        busObject2.put("LastDeparture", "22:08");
        busObject2.put("TransportType", 3);
        fakeResults.add(busObject2);
        // Создаем заглушку для метода find() объекта mockStationsQuery
        when(mockStationsQuery.find()).thenReturn(fakeResults);
        // Вызываем тестируемый метод
        ArrayList<Bus> buses = dbHelper.getAllBuses(3);
        // Проверяем результаты
        Assert.assertEquals(2, buses.size());
        Bus bus = buses.get(0);
        Assert.assertEquals(7, bus.getId());
        Assert.assertEquals("4", bus.getBusNumber());
        bus = buses.get(1);
        Assert.assertEquals(8, bus.getId());
        Assert.assertEquals("9", bus.getBusNumber());
    }
    @Test
    public void testGetBusByStation() throws ParseException {
        initDB();
        dbHelper = new DbHelper();
        // Создаем заглушку для объекта ParseQuery<ParseObject>
        ParseQuery<ParseObject> mockStationsQuery = Mockito.mock(ParseQuery.class);
        // Создаем фиктивные данные для возвращения
        List<ParseObject> fakeResults = new ArrayList<>();
        ParseObject busObject = new ParseObject("Buses");
        busObject.put("ID", 8);
        busObject.put("NumberOfBus", 9);
        busObject.put("Interval", 15);
        busObject.put("FirstDeparture", "04:26");
        busObject.put("LastDeparture", "22:08");
        busObject.put("TransportType", 3);
        fakeResults.add(busObject);
        // Создаем заглушку для метода find() объекта mockStationsQuery
        when(mockStationsQuery.find()).thenReturn(fakeResults);
        // Вызываем тестируемый метод
        ArrayList<Bus> buses = dbHelper.getBusByStation(263,true);
        // Проверяем результаты
        Assert.assertEquals(1, buses.size());
        Bus bus = buses.get(0);
        Assert.assertEquals(8, bus.getId());
        Assert.assertEquals("9", bus.getBusNumber());
    }
    @Test
    public void testGetBusesByStation() throws ParseException {
        initDB();
        dbHelper = new DbHelper();
        // Установка ожидаемого значения
        String expected = "14, 28";
        // Вызываем тестируемый метод
        String buses = dbHelper.getBusesByStation(3,true);
        // Проверяем результаты
        Assert.assertEquals(expected, buses);
    }
    @Test
    public void testGetTimeByRoute() throws ParseException {
        initDB();
        dbHelper = new DbHelper();
        // Установка ожидаемого значения
        int expected = 9;
        // Вызываем тестируемый метод
        int time = dbHelper.getTimeByRoute(1,12);
        // Проверяем результаты
        Assert.assertEquals(expected, time);
    }
    @Test
    public void testGetRoutsByBus() throws ParseException {
        initDB();
        dbHelper = new DbHelper();
        // Создаем заглушку для объекта ParseQuery<ParseObject>
        ParseQuery<ParseObject> mockStationsQuery = Mockito.mock(ParseQuery.class);
        // Создаем фиктивные данные для возвращения
        List<ParseObject> fakeResults = new ArrayList<>();
        ParseObject routeObject = new ParseObject("Routes");
        routeObject.put("ID", 11);
        routeObject.put("ID_BUS", 6);
        routeObject.put("ID_STATION", ",123,124,125,126,4,5,6,7,8,9,10,11,12,47,48,");
        routeObject.put("Name", "Ул. Университетская – Свято-Покровский храм");
        routeObject.put("Reversed", false);
        fakeResults.add(routeObject);
        // Создаем заглушку для метода find() объекта mockStationsQuery
        when(mockStationsQuery.find()).thenReturn(fakeResults);
        // Вызываем тестируемый метод
        ArrayList<Route> routes = dbHelper.getRoutsByBus(6);
        // Проверяем результаты
        // Проверяем результаты
        Assert.assertEquals(1, routes.size());
        Route route = routes.get(0);
        Assert.assertEquals(11, route.getId());
        Assert.assertEquals(6, route.getBus());
        Assert.assertEquals("Ул. Университетская – Свято-Покровский храм", route.getName());
    }

    @Test
    public void testGetRoutByBus() throws ParseException {
        initDB();
        dbHelper = new DbHelper();
        // Создаем заглушку для объекта ParseQuery<ParseObject>
        ParseQuery<ParseObject> mockStationsQuery = Mockito.mock(ParseQuery.class);
        // Создаем фиктивные данные для возвращения
        List<ParseObject> fakeResults = new ArrayList<>();
        ParseObject stationObject1 = new ParseObject("Stations");
        stationObject1.put("ID", 254);
        stationObject1.put("Name", "Горького");
        ParseGeoPoint coordinates = new ParseGeoPoint(47.991984, 37.806585);
        stationObject1.put("Coordinates", coordinates);
        fakeResults.add(stationObject1);
        ParseObject stationObject2 = new ParseObject("Stations");
        stationObject2.put("ID", 255);
        stationObject2.put("Name", "Трамвайная");
        coordinates = new ParseGeoPoint(47.993538, 37.811171);
        stationObject2.put("Coordinates", coordinates);
        fakeResults.add(stationObject1);
        fakeResults.add(stationObject2);
        // Создаем заглушку для метода find() объекта mockStationsQuery
        when(mockStationsQuery.find()).thenReturn(fakeResults);
        // Вызываем тестируемый метод
        ArrayList<Station> stations = dbHelper.getRoutByBus(8,false);
        // Проверяем результаты
        Assert.assertEquals(8, stations.size());
        Station station = stations.get(0);
        Assert.assertEquals(254, station.getId());
        Assert.assertEquals("Горького", station.getName());
        Assert.assertEquals(47.991984, station.getCoordinates().getLatitude(), 0.0);
        Assert.assertEquals(37.806585, station.getCoordinates().getLongitude(), 0.0);
    }
}