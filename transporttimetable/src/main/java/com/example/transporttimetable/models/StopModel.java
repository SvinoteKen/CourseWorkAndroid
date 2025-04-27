package com.example.transporttimetable.models;

public class StopModel {
    public int id;
    public String name;
    public String time;

    public StopModel(int id, String name, String time) {
        this.id = id;
        this.name = name;
        this.time = time;
    }
}
