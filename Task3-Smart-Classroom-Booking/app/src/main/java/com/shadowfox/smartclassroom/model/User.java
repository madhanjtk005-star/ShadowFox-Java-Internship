package com.shadowfox.smartclassroom.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

public class User {
    public static final String STUDENT = "STUDENT";
    public static final String ADMIN = "ADMIN";

    @DocumentId
    public String userId;
    public String name;
    public String email;
    public String role;

    public User() { }

    @Exclude
    public boolean isAdmin() {
        return ADMIN.equals(role);
    }
}
