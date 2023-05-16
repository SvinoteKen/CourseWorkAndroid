package com.example.transporttimetable;

import static android.provider.Settings.System.getString;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.manipulation.Ordering;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.transporttimetable.activities.MainActivity;
import com.example.transporttimetable.helpers.DbHelper;
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


    @Mock
    private ParseQuery<ParseObject> mockStationsQuery;

    private DbHelper dbHelper;

    @Test
    public void testGetAllStations() throws ParseException {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Parse.initialize(new Parse.Configuration.Builder(appContext)
                .applicationId("KbkDBhHTaUZUb6GELN3rIbt6SqrI6bmcDiv9ctU8")
                .clientKey("GF2mAlRIIRZBgZTYoV6qWTfY0xNtNWMEB59SS3qc")
                .server("https://parseapi.back4app.com")
                .build());
        dbHelper = new DbHelper();
        // Создаем фиктивные данные для возвращения
        List<ParseObject> fakeResults = new ArrayList<>();
        ParseObject stationObject = new ParseObject("Stations");
        stationObject.put("ID", 3);
        stationObject.put("Name", "ДС Крытый рынок");
        ParseGeoPoint coordinates = new ParseGeoPoint(48.011747, 37.811048);
        stationObject.put("Coordinates", coordinates);
        fakeResults.add(stationObject);


        // Вызываем тестируемый метод
        ArrayList<Station> stations = dbHelper.getAllStations("ДС Крытый рынок");

        // Проверяем результаты
        Assert.assertEquals(1, stations.size());
        Station station = stations.get(0);
        Assert.assertEquals(3, station.getId());
        Assert.assertEquals("ДС Крытый рынок", station.getName());
        Assert.assertEquals(48.011747, station.getCoordinates().getLatitude(), 0.0);
        Assert.assertEquals(37.811048, station.getCoordinates().getLongitude(), 0.0);
    }
}