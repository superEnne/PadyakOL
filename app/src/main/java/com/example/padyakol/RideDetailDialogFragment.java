package com.example.padyakol;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.padyakol.models.Ride;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class RideDetailDialogFragment extends DialogFragment implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;

    // Data passed via arguments
    private double distance;
    private double speed;
    private long duration;
    private ArrayList<Double> latList;
    private ArrayList<Double> lngList;

    public static RideDetailDialogFragment newInstance(Ride ride) {
        RideDetailDialogFragment fragment = new RideDetailDialogFragment();
        Bundle args = new Bundle();
        args.putDouble("distance", ride.getDistanceKm());
        args.putDouble("speed", ride.getAvgSpeedKmh());
        args.putLong("duration", ride.getDurationSeconds());

        // Convert GeoPoints to Arrays for passing
        ArrayList<Double> lats = new ArrayList<>();
        ArrayList<Double> lngs = new ArrayList<>();
        if (ride.getRoutePoints() != null) {
            for (GeoPoint gp : ride.getRoutePoints()) {
                lats.add(gp.getLatitude());
                lngs.add(gp.getLongitude());
            }
        }
        args.putSerializable("lats", lats);
        args.putSerializable("lngs", lngs);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        if (getArguments() != null) {
            distance = getArguments().getDouble("distance");
            speed = getArguments().getDouble("speed");
            duration = getArguments().getLong("duration");
            latList = (ArrayList<Double>) getArguments().getSerializable("lats");
            lngList = (ArrayList<Double>) getArguments().getSerializable("lngs");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ride_detail, container, false);

        // Setup Header
        TextView tvTitle = view.findViewById(R.id.tvDetailTitle);
        tvTitle.setText("Ride Summary");
        ImageButton btnClose = view.findViewById(R.id.btnCloseDetail);
        btnClose.setOnClickListener(v -> dismiss());

        // Setup Stats
        TextView tvDist = view.findViewById(R.id.tvDetailDistance);
        TextView tvSpeed = view.findViewById(R.id.tvDetailSpeed);

        tvDist.setText(String.format("%.2f km", distance));
        tvSpeed.setText(String.format("%.1f km/h", speed));

        // Setup Map
        mapView = view.findViewById(R.id.mapViewDetail);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        drawRoute();
    }

    private void drawRoute() {
        if (googleMap == null || latList == null || latList.isEmpty()) return;

        PolylineOptions options = new PolylineOptions()
                .width(15)
                .color(0xFF2196F3) // Padyak Accent Blue
                .geodesic(true);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (int i = 0; i < latList.size(); i++) {
            LatLng point = new LatLng(latList.get(i), lngList.get(i));
            options.add(point);
            builder.include(point);
        }

        googleMap.addPolyline(options);

        // Move camera nicely
        try {
            LatLngBounds bounds = builder.build();
            // Add padding to edges
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            // Fallback if bounds fail (e.g. single point)
            if (!latList.isEmpty()) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(latList.get(0), lngList.get(0)), 15f));
            }
        }
    }

    // --- Lifecycle forwarding for MapView is CRUCIAL ---
    @Override
    public void onStart() { super.onStart(); mapView.onStart(); }
    @Override
    public void onResume() { super.onResume(); mapView.onResume(); }
    @Override
    public void onPause() { super.onPause(); mapView.onPause(); }
    @Override
    public void onStop() { super.onStop(); mapView.onStop(); }
    @Override
    public void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override
    public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}