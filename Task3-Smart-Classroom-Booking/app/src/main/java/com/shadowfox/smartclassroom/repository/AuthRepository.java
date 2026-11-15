package com.shadowfox.smartclassroom.repository;

import com.shadowfox.smartclassroom.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/** Firebase Auth login / registration and role lookup. */
public class AuthRepository {

    private static AuthRepository instance;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private User currentUser;

    public static synchronized AuthRepository getInstance() {
        if (instance == null) instance = new AuthRepository();
        return instance;
    }

    private AuthRepository() { }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void login(String email, String password, Callback<User> cb) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> loadProfile(r.getUser().getUid(), cb))
                .addOnFailureListener(e -> cb.onError(message(e)));
    }

    public void register(String name, String email, String password, String role, Callback<User> cb) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> {
                    String uid = r.getUser().getUid();
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", name);
                    data.put("email", email);
                    data.put("role", role);
                    db.collection("users").document(uid).set(data)
                            .addOnSuccessListener(v -> loadProfile(uid, cb))
                            .addOnFailureListener(e -> cb.onError(message(e)));
                })
                .addOnFailureListener(e -> cb.onError(message(e)));
    }

    public void loadCurrentProfile(Callback<User> cb) {
        if (auth.getCurrentUser() == null) {
            cb.onError("Not logged in");
            return;
        }
        loadProfile(auth.getCurrentUser().getUid(), cb);
    }

    private void loadProfile(String uid, Callback<User> cb) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    User u = doc.toObject(User.class);
                    if (u == null) {
                        cb.onError("User profile not found");
                        return;
                    }
                    currentUser = u;
                    cb.onSuccess(u);
                })
                .addOnFailureListener(e -> cb.onError(message(e)));
    }

    public void logout() {
        auth.signOut();
        currentUser = null;
    }

    private String message(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Something went wrong. Please try again.";
    }
}
