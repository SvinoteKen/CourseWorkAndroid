package com.example.transporttimetable.models;

import java.util.List;

public abstract class Step {
    public static class Walk extends Step {
    }

    public static class Bus extends Step {
        public String number;
        public List<Integer> stops;

        public Bus(String number, List<Integer> stops) {
            this.number = number;
            this.stops = stops;
        }
        public String getBusNumber() {
            return number;
        }

        public List<Integer> getStations() {
            return stops;
        }
    }

    public static class Transfer extends Step {
        public String time;

        public Transfer(String time) {
            this.time = time;
        }
    }
}
