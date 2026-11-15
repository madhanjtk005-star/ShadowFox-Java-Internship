package com.shadowfox.smartclassroom.repository;

import com.shadowfox.smartclassroom.model.Booking;
import com.shadowfox.smartclassroom.model.Classroom;
import com.shadowfox.smartclassroom.model.Seat;
import com.shadowfox.smartclassroom.model.Session;
import com.shadowfox.smartclassroom.model.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Real-time snapshot listeners and atomic booking transactions.
 *
 * Double-booking protection: every booking writes two "slot lock" documents inside one
 * Firestore transaction:
 *   slots/{room}_{date}_{session}_{seat}   -> the seat for that session
 *   slots/u_{user}_{date}_{session}        -> one seat per student per session
 * The transaction aborts if either lock already exists, so two students tapping the
 * same seat at the same moment can never both succeed.
 */
public class BookingRepository {

    /** Thrown inside transactions for expected business failures (never retried). */
    public static class SeatUnavailableException extends RuntimeException {
        public SeatUnavailableException(String message) { super(message); }
    }

    private static BookingRepository instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static synchronized BookingRepository getInstance() {
        if (instance == null) instance = new BookingRepository();
        return instance;
    }

    private BookingRepository() { }

    // ------------------------------------------------------------- listeners

    public ListenerRegistration listenClassrooms(Callback<List<Classroom>> cb) {
        return db.collection("classrooms").addSnapshotListener((snap, e) -> {
            if (e != null) { cb.onError(e.getMessage()); return; }
            if (snap == null) return;
            List<Classroom> list = snap.toObjects(Classroom.class);
            Collections.sort(list, (a, b) -> a.name.compareToIgnoreCase(b.name));
            cb.onSuccess(list);
        });
    }

    public ListenerRegistration listenSessions(Callback<List<Session>> cb) {
        return db.collection("sessions").addSnapshotListener((snap, e) -> {
            if (e != null) { cb.onError(e.getMessage()); return; }
            if (snap == null) return;
            List<Session> list = snap.toObjects(Session.class);
            Collections.sort(list, (a, b) -> a.start.compareTo(b.start));
            cb.onSuccess(list);
        });
    }

    public ListenerRegistration listenSeats(String roomId, Callback<List<Seat>> cb) {
        return db.collection("classrooms").document(roomId).collection("seats")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { cb.onError(e.getMessage()); return; }
                    if (snap == null) return;
                    List<Seat> list = snap.toObjects(Seat.class);
                    Collections.sort(list, (a, b) -> a.row != b.row ? a.row - b.row : a.col - b.col);
                    cb.onSuccess(list);
                });
    }

    /** Active bookings of one room for one date + session. */
    public ListenerRegistration listenBookings(String roomId, String date, String sessionId,
                                               Callback<List<Booking>> cb) {
        Query q = db.collection("bookings")
                .whereEqualTo("roomId", roomId)
                .whereEqualTo("date", date)
                .whereEqualTo("sessionId", sessionId)
                .whereEqualTo("status", Booking.ACTIVE);
        return attach(q, cb);
    }

    /** Active bookings of all rooms for one date + session (used for live counts). */
    public ListenerRegistration listenBookingsForSlot(String date, String sessionId,
                                                      Callback<List<Booking>> cb) {
        Query q = db.collection("bookings")
                .whereEqualTo("date", date)
                .whereEqualTo("sessionId", sessionId)
                .whereEqualTo("status", Booking.ACTIVE);
        return attach(q, cb);
    }

    public ListenerRegistration listenUserBookings(String userId, Callback<List<Booking>> cb) {
        return attach(db.collection("bookings").whereEqualTo("userId", userId), cb);
    }

    private ListenerRegistration attach(Query q, Callback<List<Booking>> cb) {
        return q.addSnapshotListener((snap, e) -> {
            if (e != null) { cb.onError(e.getMessage()); return; }
            if (snap != null) cb.onSuccess(snap.toObjects(Booking.class));
        });
    }

    public void getClassroom(String roomId, Callback<Classroom> cb) {
        db.collection("classrooms").document(roomId).get()
                .addOnSuccessListener(d -> {
                    Classroom c = d.toObject(Classroom.class);
                    if (c == null) cb.onError("Classroom not found"); else cb.onSuccess(c);
                })
                .addOnFailureListener(e -> cb.onError(friendly(e)));
    }

    // ---------------------------------------------------------- transactions

    public void bookSeat(User user, Classroom room, Seat seat, Session session, String date,
                         Callback<Booking> cb) {
        String slotId = room.roomId + "_" + date + "_" + session.sessionId + "_" + seat.seatId;
        String userSlotId = "u_" + user.userId + "_" + date + "_" + session.sessionId;
        DocumentReference slotRef = db.collection("slots").document(slotId);
        DocumentReference userSlotRef = db.collection("slots").document(userSlotId);
        DocumentReference seatRef = db.collection("classrooms").document(room.roomId)
                .collection("seats").document(seat.seatId);
        DocumentReference bookingRef = db.collection("bookings").document();

        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("userId", user.userId);
        bookingData.put("userName", user.name);
        bookingData.put("roomId", room.roomId);
        bookingData.put("roomName", room.name);
        bookingData.put("seatId", seat.seatId);
        bookingData.put("seatLabel", seat.label);
        bookingData.put("sessionId", session.sessionId);
        bookingData.put("sessionLabel", session.label);
        bookingData.put("sessionStart", session.start);
        bookingData.put("sessionEnd", session.end);
        bookingData.put("date", date);
        bookingData.put("status", Booking.ACTIVE);
        bookingData.put("createdAt", System.currentTimeMillis());

        Map<String, Object> lock = new HashMap<>();
        lock.put("bookingId", bookingRef.getId());
        lock.put("userId", user.userId);

        db.runTransaction((Transaction.Function<Void>) tx -> {
            if (tx.get(slotRef).exists()) {
                throw new SeatUnavailableException(
                        "Seat " + seat.label + " was just booked by someone else. Please pick another seat.");
            }
            if (tx.get(userSlotRef).exists()) {
                throw new SeatUnavailableException("You already have a seat booked for this session.");
            }
            DocumentSnapshot seatSnap = tx.get(seatRef);
            if (Boolean.TRUE.equals(seatSnap.getBoolean("blocked"))) {
                throw new SeatUnavailableException("Seat " + seat.label + " is under maintenance.");
            }
            tx.set(slotRef, lock);
            tx.set(userSlotRef, lock);
            tx.set(bookingRef, bookingData);
            return null;
        }).addOnSuccessListener(v -> {
            Booking b = new Booking();
            b.bookingId = bookingRef.getId();
            b.seatLabel = seat.label;
            cb.onSuccess(b);
        }).addOnFailureListener(e -> cb.onError(friendly(e)));
    }

    /** Cancels a booking and releases its seat locks atomically (student or admin). */
    public void cancelBooking(Booking booking, Callback<Void> cb) {
        DocumentReference bookingRef = db.collection("bookings").document(booking.bookingId);
        DocumentReference slotRef = db.collection("slots").document(
                booking.roomId + "_" + booking.date + "_" + booking.sessionId + "_" + booking.seatId);
        DocumentReference userSlotRef = db.collection("slots").document(
                "u_" + booking.userId + "_" + booking.date + "_" + booking.sessionId);

        db.runTransaction((Transaction.Function<Void>) tx -> {
            DocumentSnapshot snap = tx.get(bookingRef);
            if (!snap.exists() || !Booking.ACTIVE.equals(snap.getString("status"))) {
                throw new SeatUnavailableException("This booking is no longer active.");
            }
            tx.update(bookingRef, "status", Booking.CANCELLED);
            tx.delete(slotRef);
            tx.delete(userSlotRef);
            return null;
        }).addOnSuccessListener(v -> cb.onSuccess(null))
          .addOnFailureListener(e -> cb.onError(friendly(e)));
    }

    // ----------------------------------------------------------------- admin

    public void setSeatBlocked(Classroom room, Seat seat, boolean blocked, Callback<Void> cb) {
        DocumentReference roomRef = db.collection("classrooms").document(room.roomId);
        DocumentReference seatRef = roomRef.collection("seats").document(seat.seatId);
        db.runTransaction((Transaction.Function<Void>) tx -> {
            boolean current = Boolean.TRUE.equals(tx.get(seatRef).getBoolean("blocked"));
            if (current != blocked) {
                tx.update(seatRef, "blocked", blocked);
                tx.update(roomRef, "blockedSeats", FieldValue.increment(blocked ? 1 : -1));
            }
            return null;
        }).addOnSuccessListener(v -> cb.onSuccess(null))
          .addOnFailureListener(e -> cb.onError(friendly(e)));
    }

    public void addSession(String label, String start, String end, Callback<Void> cb) {
        Map<String, Object> data = new HashMap<>();
        data.put("label", label);
        data.put("start", start);
        data.put("end", end);
        db.collection("sessions").add(data)
                .addOnSuccessListener(r -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(friendly(e)));
    }

    public void deleteSession(String sessionId, Callback<Void> cb) {
        db.collection("sessions").document(sessionId).delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError(friendly(e)));
    }

    // --------------------------------------------------------------- helpers

    private String friendly(Exception e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof SeatUnavailableException) return t.getMessage();
            t = t.getCause();
        }
        return e.getMessage() != null ? e.getMessage() : "Something went wrong. Please try again.";
    }
}
