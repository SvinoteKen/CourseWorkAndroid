package com.example.transporttimetable.models;

public class Bus {

    private int id;

    private String busNumber;

    private String interval;

    private String firstDeparture;

    private String lastDeparture;

    private int transportType;

    public Bus() {
    }

    public Bus(int id, String busNumber, String interval, String firstDeparture, String lastDeparture, int transportType) {
        this.id = id;
        this.busNumber = busNumber;
        this.interval = interval;
        this.firstDeparture = firstDeparture;
        this.lastDeparture = lastDeparture;
        this.transportType = transportType;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getFirstDeparture() {
        return firstDeparture;
    }

    public void setFirstDeparture(String firstDeparture) {
        this.firstDeparture = firstDeparture;
    }

    public String getLastDeparture() {
        return lastDeparture;
    }

    public void setLastDeparture(String lastDeparture) {
        this.lastDeparture = lastDeparture;
    }

    public int getTransportType() {return transportType;}

    public void setTransportType(int transportType) {this.transportType = transportType;}
}