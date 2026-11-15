package com.shadowfox.smartclassroom.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.shadowfox.smartclassroom.databinding.ActivityAdminDashboardBinding;
import com.shadowfox.smartclassroom.databinding.DialogAddSessionBinding;
import com.shadowfox.smartclassroom.model.Booking;
import com.shadowfox.smartclassroom.model.Classroom;
import com.shadowfox.smartclassroom.model.Seat;
import com.shadowfox.smartclassroom.model.Session;
import com.shadowfox.smartclassroom.model.User;
import com.shadowfox.smartclassroom.repository.AuthRepository;
import com.shadowfox.smartclassroom.repository.BookingRepository;
import com.shadowfox.smartclassroom.repository.Callback;
import com.shadowfox.smartclassroom.ui.auth.LoginActivity;
import com.shadowfox.smartclassroom.ui.booking.SeatGridAdapter;
import com.shadowfox.smartclassroom.util.DateUtil;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Occupancy overview, seat maintenance blocking, booking cancellation and session management. */
public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding b;
    private final BookingRepository repo = BookingRepository.getInstance();

    private List<Classroom> rooms = new ArrayList<>();
    private List<Session> sessions = new ArrayList<>();
    private List<Seat> seats = new ArrayList<>();
    private List<Booking> bookings = new ArrayList<>();
    private Classroom room;
    private Session session;
    private String date = DateUtil.todayKey();

    private SeatGridAdapter adapter;
    private ListenerRegistration roomsReg, sessionsReg, seatsReg, bookingsReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        User user = AuthRepository.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (!user.isAdmin()) {
            Toast.makeText(this, "Admin access only", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        b.toolbar.setNavigationOnClickListener(v -> finish());
        b.dateButton.setText(DateUtil.pretty(date));
        b.dateButton.setOnClickListener(v -> pickDate());

        adapter = new SeatGridAdapter(this::onSeatClick);
        b.seatGrid.setAdapter(adapter);
        ((SimpleItemAnimator) b.seatGrid.getItemAnimator()).setSupportsChangeAnimations(false);

        b.roomDropdown.setOnItemClickListener((p, v, pos, id) -> selectRoom(rooms.get(pos)));
        b.sessionDropdown.setOnItemClickListener((p, v, pos, id) -> {
            session = sessions.get(pos);
            subscribeBookings();
        });
        b.addSessionButton.setOnClickListener(v -> showAddSessionDialog());
        b.deleteSessionButton.setOnClickListener(v -> showDeleteSessionDialog());

        roomsReg = repo.listenClassrooms(Callback.of(this::onRooms, this::toast));
        sessionsReg = repo.listenSessions(Callback.of(this::onSessions, this::toast));
    }

    // ------------------------------------------------------- rooms / sessions

    private void onRooms(List<Classroom> list) {
        rooms = list;
        List<String> names = new ArrayList<>();
        for (Classroom c : list) names.add(c.name);
        b.roomDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));

        Classroom keep = null;
        if (room != null) for (Classroom c : list) if (c.roomId.equals(room.roomId)) keep = c;
        if (keep == null && !list.isEmpty()) keep = list.get(0);
        if (keep != null && (room == null || !room.roomId.equals(keep.roomId))) {
            selectRoom(keep);
        } else {
            room = keep; // refreshed counts
        }
        b.roomDropdown.setText(room == null ? "" : room.name, false);
    }

    private void selectRoom(Classroom c) {
        room = c;
        b.roomDropdown.setText(c.name, false);
        b.seatGrid.setLayoutManager(new GridLayoutManager(this, c.cols));
        if (seatsReg != null) seatsReg.remove();
        seats = new ArrayList<>();
        seatsReg = repo.listenSeats(c.roomId, Callback.of(list -> {
            seats = list;
            refresh();
        }, this::toast));
        subscribeBookings();
    }

    private void onSessions(List<Session> list) {
        sessions = list;
        List<String> labels = new ArrayList<>();
        for (Session s : list) labels.add(s.label);
        b.sessionDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));

        Session keep = null;
        if (session != null) for (Session s : list) if (s.sessionId.equals(session.sessionId)) keep = s;
        if (keep == null && !list.isEmpty()) keep = list.get(0);
        boolean changed = (session == null) != (keep == null)
                || (session != null && keep != null && !session.sessionId.equals(keep.sessionId));
        session = keep;
        b.sessionDropdown.setText(session == null ? "" : session.label, false);
        if (changed) subscribeBookings();
    }

    private void pickDate() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setCalendarConstraints(new CalendarConstraints.Builder().build())
                .build();
        picker.addOnPositiveButtonClickListener(sel -> {
            date = DateUtil.keyFromUtcMillis(sel);
            b.dateButton.setText(DateUtil.pretty(date));
            subscribeBookings();
        });
        picker.show(getSupportFragmentManager(), "date");
    }

    // ------------------------------------------------------------ live data

    private void subscribeBookings() {
        if (bookingsReg != null) bookingsReg.remove();
        bookingsReg = null;
        bookings = new ArrayList<>();
        if (room != null && session != null) {
            bookingsReg = repo.listenBookings(room.roomId, date, session.sessionId, Callback.of(list -> {
                bookings = list;
                refresh();
            }, this::toast));
        }
        refresh();
    }

    private void refresh() {
        Set<String> booked = new HashSet<>();
        for (Booking bk : bookings) booked.add(bk.seatId);
        adapter.update(seats, booked, null, null, new boolean[]{false, false, false, false});

        int blocked = 0;
        for (Seat s : seats) if (s.blocked) blocked++;
        int usable = seats.size() - blocked;
        int taken = 0;
        for (Seat s : seats) if (!s.blocked && booked.contains(s.seatId)) taken++;
        int percent = usable == 0 ? 0 : taken * 100 / usable;

        b.occupancyText.setText("Occupancy " + taken + "/" + usable + " (" + percent + "%)"
                + "  \u2022  " + blocked + " under maintenance");
        b.occupancyBar.setProgressCompat(percent, true);
    }

    // --------------------------------------------------------- seat actions

    private void onSeatClick(Seat seat, SeatGridAdapter.State state) {
        if (room == null) return;
        switch (state) {
            case BLOCKED:
                confirm("Unblock seat " + seat.label + "?", "Students will be able to book it again.",
                        "Unblock", () -> repo.setSeatBlocked(room, seat, false, Callback.of(v -> { }, this::toast)));
                break;
            case BOOKED:
            case MINE:
                Booking target = null;
                for (Booking bk : bookings) if (bk.seatId.equals(seat.seatId)) target = bk;
                if (target == null) return;
                final Booking toCancel = target;
                confirm("Cancel this booking?",
                        "Seat " + seat.label + " booked by " + toCancel.userName + ". The seat will be released.",
                        "Cancel booking", () -> repo.cancelBooking(toCancel,
                                Callback.of(v -> toast("Booking cancelled"), this::toast)));
                break;
            default:
                confirm("Block seat " + seat.label + "?", "Mark it as under maintenance so nobody can book it.",
                        "Block", () -> repo.setSeatBlocked(room, seat, true, Callback.of(v -> { }, this::toast)));
        }
    }

    private void confirm(String title, String message, String positive, Runnable action) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Back", null)
                .setPositiveButton(positive, (d, w) -> action.run())
                .show();
    }

    // ---------------------------------------------------- session management

    private void showAddSessionDialog() {
        DialogAddSessionBinding d = DialogAddSessionBinding.inflate(getLayoutInflater());
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Add class session")
                .setView(d.getRoot())
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String start = String.valueOf(d.startInput.getText()).trim();
                    String end = String.valueOf(d.endInput.getText()).trim();
                    d.startLayout.setError(null);
                    d.endLayout.setError(null);
                    if (!DateUtil.isValidTime(start)) { d.startLayout.setError("Use HH:mm, e.g. 16:00"); return; }
                    if (!DateUtil.isValidTime(end)) { d.endLayout.setError("Use HH:mm, e.g. 17:30"); return; }
                    if (end.compareTo(start) <= 0) { d.endLayout.setError("End must be after start"); return; }
                    repo.addSession(DateUtil.sessionLabel(start, end), start, end,
                            Callback.of(r -> toast("Session added"), this::toast));
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void showDeleteSessionDialog() {
        if (sessions.isEmpty()) { toast("No sessions to delete"); return; }
        String[] labels = new String[sessions.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = sessions.get(i).label;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete which session?")
                .setItems(labels, (d, which) -> {
                    Session s = sessions.get(which);
                    confirm("Delete " + s.label + "?",
                            "Existing bookings keep their details, but nobody can book this session any more.",
                            "Delete", () -> repo.deleteSession(s.sessionId,
                                    Callback.of(v -> toast("Session deleted"), this::toast)));
                })
                .setNegativeButton("Back", null)
                .show();
    }

    private void toast(String msg) {
        if (!isFinishing()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (roomsReg != null) roomsReg.remove();
        if (sessionsReg != null) sessionsReg.remove();
        if (seatsReg != null) seatsReg.remove();
        if (bookingsReg != null) bookingsReg.remove();
        super.onDestroy();
    }
}
