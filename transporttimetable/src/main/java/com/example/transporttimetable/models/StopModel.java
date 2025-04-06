package com.example.transporttimetable.models;

public class StopModel {
    public String name;
    public String time;
    public boolean isExpanded;

    public StopModel(String name, String time) {
        this.name = name;
        this.time = time;
        this.isExpanded = false;
    }
}
