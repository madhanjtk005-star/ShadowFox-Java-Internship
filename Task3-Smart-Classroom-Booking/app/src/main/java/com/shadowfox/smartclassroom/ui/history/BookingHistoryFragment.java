package com.shadowfox.smartclassroom.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.shadowfox.smartclassroom.databinding.FragmentBookingHistoryBinding;
import com.shadowfox.smartclassroom.model.Booking;
import com.shadowfox.smartclassroom.model.User;
import com.shadowfox.smartclassroom.repository.AuthRepository;
import com.shadowfox.smartclassroom.repository.BookingRepository;
import com.shadowfox.smartclassroom.repository.Callback;
import com.shadowfox.smartclassroom.util.DateUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookingHistoryFragment extends Fragment {

    private FragmentBookingHistoryBinding b;
    private final BookingRepository repo = BookingRepository.getInstance();
    private BookingHistoryAdapter adapter;
    private ListenerRegistration reg;
    private List<Booking> all = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentBookingHistoryBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new BookingHistoryAdapter(this::confirmCancel);
        b.bookingList.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.bookingList.setAdapter(adapter);

        b.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { render(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        User user = AuthRepository.getInstance().getCurrentUser();
        if (user == null) return;
        reg = repo.listenUserBookings(user.userId, Callback.of(list -> {
            all = list;
            render();
        }, msg -> {
            if (isAdded()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        }));
    }

    private void render() {
        if (b == null) return;
        boolean upcomingTab = b.tabLayout.getSelectedTabPosition() == 0;
        List<Booking> shown = new ArrayList<>();
        for (Booking bk : all) {
            boolean upcoming = Booking.ACTIVE.equals(bk.status) && !DateUtil.isPast(bk.date, bk.sessionEnd);
            if (upcoming == upcomingTab) shown.add(bk);
        }
        // Upcoming: soonest first. Past: most recent first.
        Collections.sort(shown, (x, y) -> {
            int c = (x.date + x.sessionStart).compareTo(y.date + y.sessionStart);
            return upcomingTab ? c : -c;
        });
        adapter.submit(shown);
        b.emptyText.setText(upcomingTab ? "No upcoming bookings" : "No past bookings");
        b.emptyText.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmCancel(Booking bk) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancel booking?")
                .setMessage("Seat " + bk.seatLabel + " in " + bk.roomName + "\n"
                        + DateUtil.pretty(bk.date) + ", " + bk.sessionLabel)
                .setNegativeButton("Keep it", null)
                .setPositiveButton("Cancel booking", (d, w) ->
                        repo.cancelBooking(bk, Callback.of(
                                v -> toast("Booking cancelled"), this::toast)))
                .show();
    }

    private void toast(String msg) {
        if (isAdded()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        if (reg != null) reg.remove();
        reg = null;
        b = null;
        super.onDestroyView();
    }
}
