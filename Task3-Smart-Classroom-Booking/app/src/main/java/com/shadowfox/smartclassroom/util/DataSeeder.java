package com.shadowfox.smartclassroom.util;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-populates 5 classrooms with realistic seat layouts and 3 default sessions.
 * Runs once: it is skipped when meta/seeded already exists. Ids are deterministic, so a
 * simultaneous first run from two devices is harmless.
 */
public final class DataSeeder {

    private DataSeeder() { }

    // id, name, building, rows, cols
    private static final Object[][] ROOMS = {
            {"cs101", "CS-101 Smart Lecture Hall", "Computer Science Block", 6, 8},
            {"ai_lab", "AI & Robotics Lab", "Innovation Centre", 4, 6},
            {"auditorium", "Main Auditorium", "Central Campus", 8, 10},
            {"electronics", "Electronics Studio", "Engineering Block", 5, 6},
            {"seminar_b", "Seminar Room B", "Administration Block", 4, 5}
    };

    // id, start, end
    private static final String[][] SESSIONS = {
            {"s1", "09:00", "10:30"},
            {"s2", "11:00", "12:30"},
            {"s3", "14:00", "15:30"}
    };

    public static void seedIfNeeded() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference marker = db.collection("meta").document("seeded");
        marker.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) seed(db, marker);
        });
    }

    private static void seed(FirebaseFirestore db, DocumentReference marker) {
        List<Task<Void>> tasks = new ArrayList<>();

        WriteBatch sessionBatch = db.batch();
        for (String[] s : SESSIONS) {
            Map<String, Object> data = new HashMap<>();
            data.put("label", DateUtil.sessionLabel(s[1], s[2]));
            data.put("start", s[1]);
            data.put("end", s[2]);
            sessionBatch.set(db.collection("sessions").document(s[0]), data);
        }
        tasks.add(sessionBatch.commit());

        for (Object[] r : ROOMS) {
            String roomId = (String) r[0];
            int rows = (int) r[3];
            int cols = (int) r[4];

            WriteBatch batch = db.batch();
            DocumentReference roomRef = db.collection("classrooms").document(roomId);

            Map<String, Object> room = new HashMap<>();
            room.put("name", r[1]);
            room.put("building", r[2]);
            room.put("rows", rows);
            room.put("cols", cols);
            room.put("totalSeats", rows * cols);
            room.put("blockedSeats", 0);
            batch.set(roomRef, room);

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    String label = String.valueOf((char) ('A' + row)) + (col + 1);
                    Map<String, Object> seat = new HashMap<>();
                    seat.put("label", label);
                    seat.put("row", row);
                    seat.put("col", col);
                    seat.put("nearWindow", col == 0);
                    seat.put("nearExit", (col == cols - 1 && row >= rows - 2) || (col == 0 && row == rows - 1));
                    seat.put("chargingPort", row % 2 == 0);
                    seat.put("accessible", row == rows - 1 && col % 3 == 0);
                    seat.put("blocked", false);
                    batch.set(roomRef.collection("seats").document(label), seat);
                }
            }
            tasks.add(batch.commit());
        }

        Tasks.whenAll(tasks).addOnSuccessListener(v -> {
            Map<String, Object> m = new HashMap<>();
            m.put("seededAt", System.currentTimeMillis());
            marker.set(m);
        });
    }
}
