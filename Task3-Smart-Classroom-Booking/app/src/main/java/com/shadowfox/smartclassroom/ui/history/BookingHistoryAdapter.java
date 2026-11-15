package com.shadowfox.smartclassroom.ui.history;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.shadowfox.smartclassroom.databinding.ItemBookingBinding;
import com.shadowfox.smartclassroom.model.Booking;
import com.shadowfox.smartclassroom.util.DateUtil;
import java.util.ArrayList;
import java.util.List;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.VH> {

    public interface OnCancelListener {
        void onCancel(Booking booking);
    }

    private final OnCancelListener listener;
    private List<Booking> items = new ArrayList<>();

    public BookingHistoryAdapter(OnCancelListener listener) {
        this.listener = listener;
    }

    public void submit(List<Booking> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemBookingBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Booking bk = items.get(position);
        boolean past = DateUtil.isPast(bk.date, bk.sessionEnd);
        boolean active = Booking.ACTIVE.equals(bk.status);

        h.b.seatBadge.setText(bk.seatLabel);
        h.b.roomText.setText(bk.roomName);
        h.b.whenText.setText(DateUtil.pretty(bk.date) + "\n" + bk.sessionLabel);

        String status;
        int color;
        if (!active) { status = "CANCELLED"; color = Color.parseColor("#E53935"); }
        else if (past) { status = "COMPLETED"; color = Color.parseColor("#757575"); }
        else { status = "UPCOMING"; color = Color.parseColor("#43A047"); }
        h.b.statusText.setText(status);
        h.b.statusText.setTextColor(color);

        boolean canCancel = active && !past;
        h.b.cancelButton.setVisibility(canCancel ? View.VISIBLE : View.GONE);
        h.b.cancelButton.setOnClickListener(v -> listener.onCancel(bk));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemBookingBinding b;

        VH(ItemBookingBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
