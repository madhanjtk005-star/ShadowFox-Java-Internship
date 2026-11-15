package com.shadowfox.smartclassroom.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.shadowfox.smartclassroom.databinding.FragmentClassroomListBinding;
import com.shadowfox.smartclassroom.model.Booking;
import com.shadowfox.smartclassroom.model.Session;
import com.shadowfox.smartclassroom.repository.BookingRepository;
import com.shadowfox.smartclassroom.repository.Callback;
import com.shadowfox.smartclassroom.ui.booking.SeatSelectionActivity;
import com.shadowfox.smartclassroom.util.DateUtil;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassroomListFragment extends Fragment {

    private FragmentClassroomListBinding b;
    private final BookingRepository repo = BookingRepository.getInstance();
    private ClassroomAdapter adapter;

    private ListenerRegistration roomsReg, sessionsReg, bookingsReg;
    private List<Session> sessions = new ArrayList<>();
    private Session session;
    private String date = DateUtil.todayKey();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentClassroomListBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new ClassroomAdapter(room -> {
            if (session == null) {
                Toast.makeText(requireContext(), "Choose a session first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(requireContext(), SeatSelectionActivity.class);
            i.putExtra(SeatSelectionActivity.EXTRA_ROOM_ID, room.roomId);
            i.putExtra(SeatSelectionActivity.EXTRA_DATE, date);
            i.putExtra(SeatSelectionActivity.EXTRA_SESSION_ID, session.sessionId);
            startActivity(i);
        });
        b.classroomList.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.classroomList.setAdapter(adapter);

        b.dateButton.setText(DateUtil.pretty(date));
        b.dateButton.setOnClickListener(v -> pickDate());
        b.sessionDropdown.setOnItemClickListener((p, v, pos, id) -> {
            session = sessions.get(pos);
            subscribeBookings();
        });

        roomsReg = repo.listenClassrooms(Callback.of(adapter::setRooms, this::toast));
        sessionsReg = repo.listenSessions(Callback.of(this::onSessions, this::toast));
    }

    private void onSessions(List<Session> list) {
        sessions = list;
        List<String> labels = new ArrayList<>();
        for (Session s : list) labels.add(s.label);
        b.sessionDropdown.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, labels));

        Session keep = null;
        if (session != null) {
            for (Session s : list) if (s.sessionId.equals(session.sessionId)) keep = s;
        }
        if (keep == null && !list.isEmpty()) {
            keep = list.get(0);
            for (Session s : list) {
                if (!DateUtil.isPast(date, s.end)) { keep = s; break; }
            }
        }
        session = keep;
        b.sessionDropdown.setText(session == null ? "" : session.label, false);
        subscribeBookings();
    }

    private void subscribeBookings() {
        if (bookingsReg != null) bookingsReg.remove();
        bookingsReg = null;
        if (session == null) {
            adapter.setBookedCounts(new HashMap<>());
            return;
        }
        bookingsReg = repo.listenBookingsForSlot(date, session.sessionId, Callback.of(list -> {
            Map<String, Integer> counts = new HashMap<>();
            for (Booking bk : list) {
                Integer c = counts.get(bk.roomId);
                counts.put(bk.roomId, c == null ? 1 : c + 1);
            }
            adapter.setBookedCounts(counts);
        }, this::toast));
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
            subscribeBookings();
        });
        picker.show(getChildFragmentManager(), "date");
    }

    private void toast(String msg) {
        if (isAdded()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        if (roomsReg != null) roomsReg.remove();
        if (sessionsReg != null) sessionsReg.remove();
        if (bookingsReg != null) bookingsReg.remove();
        roomsReg = sessionsReg = bookingsReg = null;
        b = null;
        super.onDestroyView();
    }
}
