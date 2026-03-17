package com.example.reservation.model;

public class Room {
    private int id;
    private String name;
    private int capacity;
    private String location;
    private boolean hasProjector;

    public Room(int id, String name, int capacity, String location, boolean hasProjector) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.location = location;
        this.hasProjector = hasProjector;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}