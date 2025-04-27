package com.example.transporttimetable.routes_finders;

import com.example.transporttimetable.models.Station;

import java.util.List;

public interface NearestStationsCallback {
    void onStationsFound(List<Station> stations);
}