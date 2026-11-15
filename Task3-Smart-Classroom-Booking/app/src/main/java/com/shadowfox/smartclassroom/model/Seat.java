package com.shadowfox.smartclassroom.model;

import com.google.firebase.firestore.DocumentId;

public class Seat {
    @DocumentId
    public String seatId;
    public String label;
    public int row;
    public int col;
    public boolean nearWindow;
    public boolean nearExit;
    public boolean chargingPort;
    public boolean accessible;
    public boolean blocked;

    public Seat() { }
}
