package com.shadowfox.smartclassroom.ui.booking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.transition.TransitionManager;
import com.shadowfox.smartclassroom.databinding.ActivitySeatSelectionBinding;
import com.shadowfox.smartclassroom.databinding.SheetConfirmBookingBinding;
import com.shadowfox.smartclassroom.model.Booking;
import com.shadowfox.smartclassroom.model.Classroom;
import com.shadowfox.smartclassroom.model.Seat;
import com.shadowfox.smartclassroom.model.Session;
import com.shadowfox.smartclassroom.model.User;
import com.shadowfox.smartclassroom.repository.AuthRepository;
import com.shadowfox.smartclassroom.repository.BookingRepository;
import com.shadowfox.smartclassroom.repository.Callback;
import com.shadowfox.smartclassroom.ui.auth.LoginActivity;
import com.shadowfox.smartclassroom.util.DateUtil;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeatSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "roomId";
    public static final String EXTRA_DATE = "date";
    public static final String EXTRA_SESSION_ID = "sessionId";

    private ActivitySeatSelectionBinding b;
    private final BookingRepository repo = BookingRepository.getInstance();
    private User user;
    private Classroom room;
    private Session session;
    private String date;
    private String initialSessionId;
    private String selectedSeatId;

    private List<Session> sessions = new ArrayList<>();
    private List<Seat> seats = new ArrayList<>();
    private List<Booking> bookings = new ArrayList<>();
    private SeatGridAdapter adapter;
    private ListenerRegistration sessionsReg, seatsReg, bookingsReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySeatSelectionBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        user = AuthRepository.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        date = getIntent().getStringExtra(EXTRA_DATE);
        if (date == null) date = DateUtil.todayKey();
        initialSessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);

        b.toolbar.setNavigationOnClickListener(v -> finish());
        b.dateButton.setText(DateUtil.pretty(date));
        b.dateButton.setOnClickListener(v -> pickDate());
        b.sessionDropdown.setOnItemClickListener((p, v, pos, id) -> selectSession(sessions.get(pos)));

        adapter = new SeatGridAdapter(this::onSeatClick);
        b.seatGrid.setAdapter(adapter);
        ((SimpleItemAnimator) b.seatGrid.getItemAnimator()).setSupportsChangeAnimations(false);

        b.chipWindow.setOnCheckedChangeListener((c, checked) -> refreshGrid());
        b.chipCharging.setOnCheckedChangeListener((c, checked) -> refreshGrid());
        b.chipExit.setOnCheckedChangeListener((c, checked) -> refreshGrid());
        b.chipAccessible.setOnCheckedChangeListener((c, checked) -> refreshGrid());

        sessionsReg = repo.listenSessions(Callback.of(this::onSessions, this::snack));

        repo.getClassroom(roomId, Callback.of(c -> {
            room = c;
            b.toolbar.setTitle(c.name);
            b.seatGrid.setLayoutManager(new GridLayoutManager(this, c.cols));
            seatsReg = repo.listenSeats(c.roomId, Callback.of(list -> {
                seats = list;
                refreshGrid();
            }, this::snack));
            subscribeBookings();
        }, this::snack));
    }

    // ---------------------------------------------------- sessions / date

    private void onSessions(List<Session> list) {
        sessions = list;
        List<String> labels = new ArrayList<>();
        for (Session s : list) labels.add(s.label);
        b.sessionDropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));

        String wanted = session != null ? session.sessionId : initialSessionId;
        Session pick = null;
        for (Session s : list) if (s.sessionId.equals(wanted)) pick = s;
        if (pick == null && !list.isEmpty()) pick = list.get(0);
        selectSession(pick);
    }

    private void selectSession(Session s) {
        session = s;
        b.sessionDropdown.setText(s == null ? "" : s.label, false);
        selectedSeatId = null;
        subscribeBookings();
    }

    private void pickDate() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now()).build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setCalendarConstraints(constraints)
                .build();
        picker.addOnPositiveButtonClickListener(sel -> {
            date = DateUtil.keyFromUtcMillis(sel);
            b.dateButton.setText(DateUtil.pretty(date));
            selectedSeatId = null;
            subscribeBookings();
        });
        picker.show(getSupportFragmentManager(), "date");
    }

    // ------------------------------------------------------ live updates

    private void subscribeBookings() {
        if (bookingsReg != null) bookingsReg.remove();
        bookingsReg = null;
        bookings = new ArrayList<>();
        if (room == null || session == null) {
            refreshGrid();
            return;
        }
        bookingsReg = repo.listenBookings(room.roomId, date, session.sessionId, Callback.of(list -> {
            bookings = list;
            refreshGrid();
        }, this::snack));
    }

    private void refreshGrid() {
        if (room == null) return;

        Set<String> booked = new HashSet<>();
        String mine = null;
        for (Booking bk : bookings) {
            booked.add(bk.seatId);
            if (user.userId.equals(bk.userId)) mine = bk.seatId;
        }
        if (selectedSeatId != null && booked.contains(selectedSeatId)) {
            selectedSeatId = null;
            snack("That seat was just taken by someone else");
        }

        boolean[] filters = {b.chipWindow.isChecked(), b.chipCharging.isChecked(),
                b.chipExit.isChecked(), b.chipAccessible.isChecked()};
        adapter.update(seats, booked, mine, selectedSeatId, filters);

        int usable = 0, free = 0;
        for (Seat s : seats) {
            if (s.blocked) continue;
            usable++;
            if (!booked.contains(s.seatId)) free++;
        }
        TransitionManager.beginDelayedTransition(b.summaryContainer);
        b.summaryText.setText(free + " of " + usable + " seats free");
        b.hintText.setText(adapter.isFilterActive()
                ? adapter.countMatchingFree() + " free seats match your filters (highlighted in amber)"
                : "Tap a green seat to see its details and book it");
    }

    // ------------------------------------------------------------ booking

    private void onSeatClick(Seat seat, SeatGridAdapter.State state) {
        switch (state) {
            case BOOKED:
                snack("Seat " + seat.label + " is already booked");
                return;
            case MINE:
                snack("Seat " + seat.label + " is your booking for this session");
                return;
            case BLOCKED:
                snack("Seat " + seat.label + " is under maintenance");
                return;
            default:
                break;
        }
        if (session == null) { snack("Choose a session first"); return; }
        if (DateUtil.isPast(date, session.end)) { snack("This session has already ended"); return; }

        selectedSeatId = seat.seatId;
        adapter.pulse(seat.seatId);
        refreshGrid();
        showConfirmSheet(seat);
    }

    private void showConfirmSheet(Seat seat) {
        final Session chosen = session;
        final String chosenDate = date;
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        SheetConfirmBookingBinding sb = SheetConfirmBookingBinding.inflate(getLayoutInflater());
        dialog.setContentView(sb.getRoot());

        sb.seatTitle.setText("Seat " + seat.label);
        sb.roomText.setText(room.name + "  \u2022  " + room.building);
        sb.whenText.setText(DateUtil.pretty(chosenDate) + "   |   " + chosen.label);
        sb.attributesText.setText(attr("Near window", seat.nearWindow) + "\n"
                + attr("Close to exit", seat.nearExit) + "\n"
                + attr("Charging port", seat.chargingPort) + "\n"
                + attr("Wheelchair / easy access", seat.accessible));

        sb.cancelButton.setOnClickListener(v -> dialog.dismiss());
        sb.confirmButton.setOnClickListener(v -> {
            sb.confirmButton.setEnabled(false);
            sb.progress.setVisibility(View.VISIBLE);
            repo.bookSeat(user, room, seat, chosen, chosenDate, Callback.of(bk -> {
                dialog.dismiss();
                snack("Seat " + seat.label + " booked!");
            }, msg -> {
                dialog.dismiss();
                snack(msg);
            }));
        });
        dialog.setOnDismissListener(d -> {
            selectedSeatId = null;
            refreshGrid();
        });
        dialog.show();
    }

    private String attr(String label, boolean on) {
        return (on ? "\u2713  " : "\u2717  ") + label;
    }

    private void snack(String msg) {
        if (!isFinishing() && b != null) Snackbar.make(b.getRoot(), msg, Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        if (sessionsReg != null) sessionsReg.remove();
        if (seatsReg != null) seatsReg.remove();
        if (bookingsReg != null) bookingsReg.remove();
        super.onDestroy();
    }
}
