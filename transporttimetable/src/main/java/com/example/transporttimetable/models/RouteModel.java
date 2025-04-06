package com.example.transporttimetable.models;

import java.util.List;

public class RouteModel {
    private String timeRange;
    private List<Step> steps;

    public RouteModel(String timeRange, List<Step> steps) {
        this.timeRange = timeRange;
        this.steps = steps;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public List<Step> getSteps() {
        return steps;
    }
}