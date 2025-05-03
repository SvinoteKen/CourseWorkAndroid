package com.example.transporttimetable.models;

import java.util.List;

public class FoundRoute {
    public List<Step> parts; // куски маршрута: Bus, Transfer
    public int totalTime; // Общее время маршрута в минутах

    public FoundRoute(List<Step> parts, int totalTime) {
        this.parts = parts;
        this.totalTime = totalTime;
    }
    public int getTotalTime() {
        return totalTime;
    }
    public List<Step> getSteps() {
        return parts;
    }
}

