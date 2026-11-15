package com.shadowfox.smartclassroom.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.shadowfox.smartclassroom.R;
import com.shadowfox.smartclassroom.databinding.ItemClassroomBinding;
import com.shadowfox.smartclassroom.model.Classroom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassroomAdapter extends RecyclerView.Adapter<ClassroomAdapter.VH> {

    public interface OnClassroomClickListener {
        void onClick(Classroom classroom);
    }

    private final OnClassroomClickListener listener;
    private List<Classroom> rooms = new ArrayList<>();
    private Map<String, Integer> bookedByRoom = new HashMap<>();

    public ClassroomAdapter(OnClassroomClickListener listener) {
        this.listener = listener;
    }

    public void setRooms(List<Classroom> rooms) {
        this.rooms = rooms;
        notifyDataSetChanged();
    }

    public void setBookedCounts(Map<String, Integer> bookedByRoom) {
        this.bookedByRoom = bookedByRoom;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemClassroomBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Classroom c = rooms.get(position);
        int usable = Math.max(0, c.totalSeats - c.blockedSeats);
        Integer booked = bookedByRoom.get(c.roomId);
        int free = Math.max(0, usable - (booked == null ? 0 : booked));
        int percentFree = usable == 0 ? 0 : free * 100 / usable;

        h.b.nameText.setText(c.name);
        h.b.buildingText.setText(c.building + "  \u2022  Capacity " + c.totalSeats);
        h.b.availabilityText.setText(free + " of " + usable + " seats available");
        h.b.availabilityBar.setProgressCompat(percentFree, true);

        int colorRes = percentFree > 30 ? R.color.seat_available
                : percentFree > 10 ? R.color.seat_match : R.color.seat_booked;
        h.b.availabilityBar.setIndicatorColor(ContextCompat.getColor(h.itemView.getContext(), colorRes));

        h.itemView.setOnClickListener(v -> listener.onClick(c));
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemClassroomBinding b;

        VH(ItemClassroomBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
