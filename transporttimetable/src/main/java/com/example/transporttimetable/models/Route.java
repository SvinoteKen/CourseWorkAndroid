package com.example.transporttimetable.models;

public class Route {

    private int id;

    private int bus;

    private String stop;

    private String Name;

    private Boolean Reversed;

    private String Time;

    public Route() {
    }

    public Route(int bus, String stop, String Name, Boolean Reversed, String Time) {
        this.bus = bus;
        this.stop = stop;
        this.Name = Name;
        this.Reversed = Reversed;
        this.Time = Time;
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

    public String getStop() {
        return stop;
    }

    public void setStop(String stop) {
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

    public String getTime() {
        return Time;
    }

    public void setTime(String time) {
        Time = time;
    }
}