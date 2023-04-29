package com.example.transporttimetable.models;

import com.yandex.mapkit.geometry.Point;

import java.io.Serializable;

public class Station implements Serializable {

    private int id;

    private String name;

    private Point coordinates;

    public Station() {
    }

    public Station(int id, String name, Point coordinates) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Point getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Point coordinates) {
        this.coordinates = coordinates;
    }
}