package com.shadowfox.smartclassroom.ui.booking;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.shadowfox.smartclassroom.R;
import com.shadowfox.smartclassroom.databinding.ItemSeatBinding;
import com.shadowfox.smartclassroom.model.Seat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Colour-coded seat map. Used by students (selection) and admins (maintenance / cancel). */
public class SeatGridAdapter extends RecyclerView.Adapter<SeatGridAdapter.VH> {

    public enum State { AVAILABLE, BOOKED, BLOCKED, SELECTED, MINE }

    public interface OnSeatClickListener {
        void onSeatClick(Seat seat, State state);
    }

    private final OnSeatClickListener listener;
    private List<Seat> seats = new ArrayList<>();
    private Set<String> booked = new HashSet<>();
    private String mySeatId, selectedSeatId, pulseSeatId;
    private boolean fWindow, fCharging, fExit, fAccessible;

    public SeatGridAdapter(OnSeatClickListener listener) {
        this.listener = listener;
    }

    /** filters = {window, charging, exit, accessible}. */
    public void update(List<Seat> seats, Set<String> booked, String mySeatId,
                       String selectedSeatId, boolean[] filters) {
        this.seats = seats;
        this.booked = booked;
        this.mySeatId = mySeatId;
        this.selectedSeatId = selectedSeatId;
        fWindow = filters[0];
        fCharging = filters[1];
        fExit = filters[2];
        fAccessible = filters[3];
        notifyDataSetChanged();
    }

    /** Marks a seat to play the scale/pulse animation on its next bind. */
    public void pulse(String seatId) {
        this.pulseSeatId = seatId;
    }

    public boolean isFilterActive() {
        return fWindow || fCharging || fExit || fAccessible;
    }

    /** A seat matches when it has every attribute that is currently filtered on. */
    public boolean matches(Seat s) {
        return (!fWindow || s.nearWindow) && (!fCharging || s.chargingPort)
                && (!fExit || s.nearExit) && (!fAccessible || s.accessible);
    }

    public int countMatchingFree() {
        int n = 0;
        for (Seat s : seats) {
            State st = stateOf(s);
            if ((st == State.AVAILABLE || st == State.SELECTED) && matches(s)) n++;
        }
        return n;
    }

    public State stateOf(Seat s) {
        if (s.blocked) return State.BLOCKED;
        if (s.seatId.equals(mySeatId)) return State.MINE;
        if (booked.contains(s.seatId)) return State.BOOKED;
        if (s.seatId.equals(selectedSeatId)) return State.SELECTED;
        return State.AVAILABLE;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemSeatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Seat seat = seats.get(position);
        State st = stateOf(seat);
        boolean filterOn = isFilterActive();
        boolean match = filterOn && st == State.AVAILABLE && matches(seat);

        int colorRes;
        int textColor = Color.WHITE;
        switch (st) {
            case BLOCKED: colorRes = R.color.seat_blocked; break;
            case BOOKED: colorRes = R.color.seat_booked; break;
            case SELECTED: colorRes = R.color.seat_selected; break;
            case MINE: colorRes = R.color.seat_mine; break;
            default:
                if (match) {
                    colorRes = R.color.seat_match;
                    textColor = Color.parseColor("#212121");
                } else {
                    colorRes = R.color.seat_available;
                }
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(h.itemView.getResources().getDisplayMetrics().density * 8);
        bg.setColor(ContextCompat.getColor(h.itemView.getContext(), colorRes));
        h.b.seatText.setBackground(bg);
        h.b.seatText.setTextColor(textColor);
        h.b.seatText.setText(seat.label);

        // Smooth dim of seats that do not match the active filters.
        float targetAlpha = (filterOn && !match && st != State.SELECTED && st != State.MINE) ? 0.35f : 1f;
        if (Math.abs(h.itemView.getAlpha() - targetAlpha) > 0.01f) {
            h.itemView.animate().alpha(targetAlpha).setDuration(200).start();
        }

        // Scale / pulse animation on selection.
        if (seat.seatId.equals(pulseSeatId)) {
            pulseSeatId = null;
            h.itemView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(140)
                    .withEndAction(() -> h.itemView.animate().scaleX(1f).scaleY(1f).setDuration(140).start())
                    .start();
        } else {
            h.itemView.setScaleX(1f);
            h.itemView.setScaleY(1f);
        }

        h.itemView.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Seat s = seats.get(pos);
            listener.onSeatClick(s, stateOf(s));
        });
    }

    @Override
    public int getItemCount() {
        return seats.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemSeatBinding b;

        VH(ItemSeatBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
