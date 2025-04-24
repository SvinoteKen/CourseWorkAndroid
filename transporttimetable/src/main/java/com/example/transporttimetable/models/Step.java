package com.example.transporttimetable.models;

public abstract class Step {
    public static class Walk extends Step {
    }

    public static class Bus extends Step {
        public String number;

        public Bus(String number) {
            this.number = number;
        }
    }

    public static class Transfer extends Step {
        public String time;

        public Transfer(String time) {
            this.time = time;
        }
    }
}
