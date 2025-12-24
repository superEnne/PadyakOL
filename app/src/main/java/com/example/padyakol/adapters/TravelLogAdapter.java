package com.example.padyakol.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.padyakol.R;
import com.example.padyakol.models.Ride;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TravelLogAdapter extends RecyclerView.Adapter<TravelLogAdapter.RideViewHolder> {

    private List<Ride> rideList;
    private Context context;
    private OnRideClickListener listener;

    // Interface for click handling
    public interface OnRideClickListener {
        void onRideClick(Ride ride);
    }

    public TravelLogAdapter(Context context, List<Ride> rideList, OnRideClickListener listener) {
        this.context = context;
        this.rideList = rideList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_travel_log, parent, false);
        return new RideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        Ride ride = rideList.get(position);

        // --- Date & Time ---
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(ride.getTimestamp());
        String dateString = DateFormat.format("MMM dd, yyyy", cal).toString();
        String timeString = DateFormat.format("hh:mm a", cal).toString();

        holder.tvDate.setText(dateString);
        holder.tvTime.setText(timeString);

        // --- Stats ---
        holder.tvDistance.setText(String.format(Locale.US, "%.2f km", ride.getDistanceKm()));
        holder.tvAvgSpeed.setText(String.format(Locale.US, "%.1f", ride.getAvgSpeedKmh())); // Removed km/h for space

        long hours = ride.getDurationSeconds() / 3600;
        long minutes = (ride.getDurationSeconds() % 3600) / 60;
        String durationStr;
        if (hours > 0) {
            durationStr = String.format(Locale.US, "%dh %02dm", hours, minutes);
        } else {
            durationStr = String.format(Locale.US, "%d mins", minutes);
        }
        holder.tvDuration.setText(durationStr);

        // --- Click Listener ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRideClick(ride);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rideList.size();
    }

    public static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvDistance, tvDuration, tvAvgSpeed;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvLogDate);
            tvTime = itemView.findViewById(R.id.tvLogTime);
            tvDistance = itemView.findViewById(R.id.tvLogDistance);
            tvDuration = itemView.findViewById(R.id.tvLogDuration);
            tvAvgSpeed = itemView.findViewById(R.id.tvLogSpeed);
        }
    }
}