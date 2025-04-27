package com.example.transporttimetable.models;

import java.util.List;

public class RoutePart {
    public String busNumber;
    public List<Station> stations;
    public int transferTime; // Если это пересадка
}
