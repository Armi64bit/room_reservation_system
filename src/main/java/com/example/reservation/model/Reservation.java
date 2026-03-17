package com.example.reservation.model;

public class Reservation {
    private int roomId;
    private String event;
    private String organizer;
    private String date;
    private String start;
    private String end;

    public Reservation(int roomId, String event, String organizer,
                       String date, String start, String end) {
        this.roomId = roomId;
        this.event = event;
        this.organizer = organizer;
        this.date = date;
        this.start = start;
        this.end = end;
    }
}