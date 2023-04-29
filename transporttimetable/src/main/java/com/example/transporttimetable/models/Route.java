package com.example.transporttimetable.models;

public class Route {

    private int id;

    private int bus;

    private int stop;

    private String Name;

    private Boolean Reversed;

    public Route() {
    }

    public Route(int bus, int stop, String Name, Boolean Reversed) {
        this.bus = bus;
        this.stop = stop;
        this.Name = Name;
        this.Reversed = Reversed;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBus() {
        return bus;
    }

    public void setBus(int bus) {
        this.bus = bus;
    }

    public int getStop() {
        return stop;
    }

    public void setStop(int stop) {
        this.stop = stop;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public Boolean getReversed() {
        return Reversed;
    }

    public void setReversed(Boolean reversed) {
        Reversed = reversed;
    }
}