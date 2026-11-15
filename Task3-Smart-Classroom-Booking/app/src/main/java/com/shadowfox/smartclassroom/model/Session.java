package com.shadowfox.smartclassroom.model;

import com.google.firebase.firestore.DocumentId;

/** A class timing slot, e.g. 09:00 AM - 10:30 AM. start/end are 24h "HH:mm". */
public class Session {
    @DocumentId
    public String sessionId;
    public String label;
    public String start;
    public String end;

    public Session() { }
}
