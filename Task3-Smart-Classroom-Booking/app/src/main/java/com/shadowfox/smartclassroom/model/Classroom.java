package com.shadowfox.smartclassroom.model;

import com.google.firebase.firestore.DocumentId;

public class Classroom {
    @DocumentId
    public String roomId;
    public String name;
    public String building;
    public int totalSeats;
    public int rows;
    public int cols;
    public int blockedSeats;

    public Classroom() { }
}
