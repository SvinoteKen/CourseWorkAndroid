package com.example.transporttimetable.models;

import java.util.List;

public abstract class Step {
    public static class Walk extends Step {
        public String time;
        public Walk(String time) {
            this.time = time;
        }
        public String getTime() {
            return time;
        }
    }

    public static class Bus extends Step {
        public String number;
        public List<StopModel> stops;

        public Bus(String number, List<StopModel> stops) {
            this.number = number;
            this.stops = stops;
        }

        public String getBusNumber() {
            return number;
        }

    }

    public static class Transfer extends Step {
        public String time;

        public Transfer(String time) {
            this.time = time;
        }
    }
}
