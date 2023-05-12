package com.example.transporttimetable.models;

import com.yandex.mapkit.geometry.Point;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

public class Station implements Serializable {

    private int id;

    private String name;

    private Point coordinates;

    private int index;

    public Station() {
    }

    public Station(int id, String name, Point coordinates, int index) {
        this.id = id;
        this.name = name;
        this.coordinates = coordinates;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public static Comparator<Station> getIndexComparator() {
        return Comparator.comparingInt(Station::getIndex);
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