package com.shadowfox.smartclassroom.model;

import com.google.firebase.firestore.DocumentId;

public class Booking {
    public static final String ACTIVE = "ACTIVE";
    public static final String CANCELLED = "CANCELLED";

    @DocumentId
    public String bookingId;
    public String userId;
    public String userName;
    public String roomId;
    public String roomName;
    public String seatId;
    public String seatLabel;
    public String sessionId;
    public String sessionLabel;
    public String sessionStart;
    public String sessionEnd;
    public String date;      // yyyy-MM-dd
    public String status;    // ACTIVE / CANCELLED
    public long createdAt;

    public Booking() { }
}
