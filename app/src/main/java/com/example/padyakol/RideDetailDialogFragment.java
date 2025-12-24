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
    // Changed to double[] for better performance and stability
    private double[] latArray;
    private double[] lngArray;

    public static RideDetailDialogFragment newInstance(Ride ride) {
        RideDetailDialogFragment fragment = new RideDetailDialogFragment();
        Bundle args = new Bundle();
        // Use safe getters if available, otherwise handle nulls manually
        args.putDouble("distance", ride.getDistanceKm());
        args.putDouble("speed", ride.getAvgSpeedKmh());
        args.putLong("duration", ride.getDurationSeconds());

        // Convert List<GeoPoint> to double[] (primitive arrays)
        // This avoids Serializable overhead and prevents crashes with large data
        if (ride.getRoutePoints() != null && !ride.getRoutePoints().isEmpty()) {
            int size = ride.getRoutePoints().size();
            double[] lats = new double[size];
            double[] lngs = new double[size];

            for (int i = 0; i < size; i++) {
                GeoPoint gp = ride.getRoutePoints().get(i);
                if (gp != null) {
                    lats[i] = gp.getLatitude();
                    lngs[i] = gp.getLongitude();
                }
            }
            args.putDoubleArray("lats", lats);
            args.putDoubleArray("lngs", lngs);
        }

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        if (getArguments() != null) {
            distance = getArguments().getDouble("distance", 0.0);
            speed = getArguments().getDouble("speed", 0.0);
            duration = getArguments().getLong("duration", 0);
            latArray = getArguments().getDoubleArray("lats");
            lngArray = getArguments().getDoubleArray("lngs");
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

        // Setup Map safely
        mapView = view.findViewById(R.id.mapViewDetail);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        if (latArray != null && lngArray != null && latArray.length > 0) {
            drawRoute();
        }
    }

    private void drawRoute() {
        try {
            if (googleMap == null || latArray == null || latArray.length == 0) return;

            PolylineOptions options = new PolylineOptions()
                    .width(15)
                    .color(0xFF2196F3) // Padyak Accent Blue
                    .geodesic(true);

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            boolean hasPoints = false;

            for (int i = 0; i < latArray.length; i++) {
                // simple check to avoid 0,0 if something went wrong during conversion
                if (latArray[i] != 0 || lngArray[i] != 0) {
                    LatLng point = new LatLng(latArray[i], lngArray[i]);
                    options.add(point);
                    builder.include(point);
                    hasPoints = true;
                }
            }

            if (hasPoints) {
                googleMap.addPolyline(options);
                try {
                    LatLngBounds bounds = builder.build();
                    // Add padding to edges
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                } catch (IllegalStateException e) {
                    // Fallback if bounds fail (e.g. all points were identical)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(latArray[0], lngArray[0]), 15f));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fail silently on map drawing error rather than crashing app
        }
    }

    // --- Lifecycle forwarding for MapView is CRUCIAL ---
    @Override
    public void onStart() { super.onStart(); if(mapView!=null) mapView.onStart(); }
    @Override
    public void onResume() { super.onResume(); if(mapView!=null) mapView.onResume(); }
    @Override
    public void onPause() { super.onPause(); if(mapView!=null) mapView.onPause(); }
    @Override
    public void onStop() { super.onStop(); if(mapView!=null) mapView.onStop(); }
    @Override
    public void onDestroy() { super.onDestroy(); if(mapView!=null) mapView.onDestroy(); }
    @Override
    public void onLowMemory() { super.onLowMemory(); if(mapView!=null) mapView.onLowMemory(); }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mapView!=null) mapView.onSaveInstanceState(outState);
    }
}