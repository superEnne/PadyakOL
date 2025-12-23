package com.example.padyakol.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.example.padyakol.R;
import com.example.padyakol.models.Ride;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TravelLogAdapter extends RecyclerView.Adapter<TravelLogAdapter.RideViewHolder> {

    private List<Ride> rideList;
    private Context context;

    public TravelLogAdapter(Context context, List<Ride> rideList) {
        this.context = context;
        this.rideList = rideList;
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

        // --- Summary View (Always Visible) ---

        // Date Formatting
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(ride.getTimestamp());
        String dateString = DateFormat.format("MMM dd, yyyy", cal).toString();
        String timeString = DateFormat.format("hh:mm a", cal).toString();

        holder.tvDate.setText(dateString);
        holder.tvTime.setText(timeString);
        holder.tvDistance.setText(String.format(Locale.US, "%.2f km", ride.getDistanceKm()));

        // --- Expanded View Details ---

        // Duration Formatting
        long hours = ride.getDurationSeconds() / 3600;
        long minutes = (ride.getDurationSeconds() % 3600) / 60;
        String durationStr = (hours > 0) ? String.format("%dh %02dm", hours, minutes) : String.format("%d mins", minutes);

        holder.tvDuration.setText(durationStr);
        holder.tvAvgSpeed.setText(String.format(Locale.US, "%.1f km/h", ride.getAvgSpeedKmh()));

        // Map Setup for Expanded View
        if (holder.mapView != null) {
            // FIX: Removed mapView.onCreate(null) from here. It is now in the ViewHolder constructor.

            holder.mapView.getMapAsync(googleMap -> {
                MapsInitializer.initialize(context);

                googleMap.clear(); // Clear previous polylines if view is recycled

                // Draw Route
                if (ride.getRoutePoints() != null && !ride.getRoutePoints().isEmpty()) {
                    PolylineOptions options = new PolylineOptions().width(10).color(context.getColor(R.color.padyak_accent));
                    com.google.android.gms.maps.model.LatLngBounds.Builder builder = new com.google.android.gms.maps.model.LatLngBounds.Builder();

                    for (com.google.firebase.firestore.GeoPoint p : ride.getRoutePoints()) {
                        LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
                        options.add(latLng);
                        builder.include(latLng);
                    }

                    googleMap.addPolyline(options);
                    googleMap.getUiSettings().setAllGesturesEnabled(false); // Static map

                    // Move camera to show route
                    try {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
                    } catch (Exception e) {
                        // Handle single point case
                        LatLng latLng = new LatLng(ride.getRoutePoints().get(0).getLatitude(), ride.getRoutePoints().get(0).getLongitude());
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                    }
                }
            });
        }

        // --- Expand/Collapse Logic ---
        holder.layoutExpanded.setVisibility(holder.isExpanded ? View.VISIBLE : View.GONE);
        holder.tvViewMore.setText(holder.isExpanded ? "Show Less" : "View Details");

        holder.itemView.setOnClickListener(v -> {
            holder.isExpanded = !holder.isExpanded;
            notifyItemChanged(position); // Refresh to trigger animation/visibility
        });
    }

    @Override
    public int getItemCount() {
        return rideList.size();
    }

    public static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvDistance, tvDuration, tvAvgSpeed, tvViewMore;
        LinearLayout layoutExpanded;
        MapView mapView;
        boolean isExpanded = false;

        public RideViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvLogDate);
            tvTime = itemView.findViewById(R.id.tvLogTime);
            tvDistance = itemView.findViewById(R.id.tvLogDistance);
            tvDuration = itemView.findViewById(R.id.tvLogDuration);
            tvAvgSpeed = itemView.findViewById(R.id.tvLogSpeed);
            tvViewMore = itemView.findViewById(R.id.tvViewMore);
            layoutExpanded = itemView.findViewById(R.id.layoutExpanded);
            mapView = itemView.findViewById(R.id.mapViewLog);

            // FIX: Initialize MapView lifecycle here, once per ViewHolder creation.
            if (mapView != null) {
                mapView.onCreate(null);
                mapView.onResume();
            }
        }
    }
}