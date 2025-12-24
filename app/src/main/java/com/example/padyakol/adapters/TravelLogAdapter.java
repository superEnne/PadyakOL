package com.example.padyakol.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.padyakol.R;
import com.example.padyakol.models.Ride;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
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
        // Ensure Maps SDK is ready before we try to inflate any MapViews
        try {
            MapsInitializer.initialize(context.getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(ride.getTimestamp());
        String dateString = DateFormat.format("MMM dd, yyyy", cal).toString();
        String timeString = DateFormat.format("hh:mm a", cal).toString();

        holder.tvDate.setText(dateString);
        holder.tvTime.setText(timeString);
        holder.tvDistance.setText(String.format(Locale.US, "%.2f km", ride.getDistanceKm()));

        // --- Expanded View Details ---
        long hours = ride.getDurationSeconds() / 3600;
        long minutes = (ride.getDurationSeconds() % 3600) / 60;
        String durationStr = (hours > 0) ? String.format("%dh %02dm", hours, minutes) : String.format("%d mins", minutes);

        holder.tvDuration.setText(durationStr);
        holder.tvAvgSpeed.setText(String.format(Locale.US, "%.1f km/h", ride.getAvgSpeedKmh()));

        // --- Lazy Loading Logic (CRITICAL FIX) ---
        // Only initialize and show the map if the item is explicitly expanded.
        // This prevents the app from trying to load 10+ maps at once and crashing.
        if (holder.isExpanded) {
            holder.layoutExpanded.setVisibility(View.VISIBLE);

            if (holder.mapView != null) {
                holder.mapView.setVisibility(View.VISIBLE);
                holder.mapView.getMapAsync(googleMap -> {
                    googleMap.clear();
                    googleMap.setMapType(com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL);
                    googleMap.getUiSettings().setMapToolbarEnabled(false);

                    if (ride.getRoutePoints() != null && !ride.getRoutePoints().isEmpty()) {
                        PolylineOptions options = new PolylineOptions().width(12).color(context.getColor(R.color.padyak_accent));
                        com.google.android.gms.maps.model.LatLngBounds.Builder builder = new com.google.android.gms.maps.model.LatLngBounds.Builder();

                        for (com.google.firebase.firestore.GeoPoint p : ride.getRoutePoints()) {
                            LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
                            options.add(latLng);
                            builder.include(latLng);
                        }

                        googleMap.addPolyline(options);

                        try {
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
                        } catch (Exception e) {
                            LatLng latLng = new LatLng(ride.getRoutePoints().get(0).getLatitude(), ride.getRoutePoints().get(0).getLongitude());
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                        }
                    }
                });
            }
        } else {
            // If collapsed, hide the layout AND the map view to save memory
            holder.layoutExpanded.setVisibility(View.GONE);
            if (holder.mapView != null) {
                holder.mapView.setVisibility(View.GONE);
                holder.mapView.clearAnimation();
            }
        }

        holder.tvViewMore.setText(holder.isExpanded ? "Show Less" : "View Details");

        holder.itemView.setOnClickListener(v -> {
            holder.isExpanded = !holder.isExpanded;
            // Notify change to trigger rebinding (and thus map loading/unloading)
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return rideList.size();
    }

    @Override
    public void onViewRecycled(@NonNull RideViewHolder holder) {
        if (holder.mapView != null) {
            holder.mapView.clearAnimation();
        }
        super.onViewRecycled(holder);
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

            if (mapView != null) {
                mapView.onCreate(null);
            }
        }
    }
}