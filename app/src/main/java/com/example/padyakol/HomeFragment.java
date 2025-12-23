package com.example.padyakol;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.padyakol.models.Ride;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // UI
    private TextView tvSessionDistance, tvCurrentSpeed;
    private Chronometer chronometer;
    private Button btnRideToggle;

    // Data
    private boolean isTracking = false;
    private double sessionDistanceKm = 0.0;
    private Location lastLocation = null;
    private List<LatLng> pathPoints = new ArrayList<>();
    private Polyline currentPolyline;
    private long rideStartTime = 0;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        // Initialize Views
        tvSessionDistance = view.findViewById(R.id.tvSessionDistance);
        tvCurrentSpeed = view.findViewById(R.id.tvCurrentSpeed);
        chronometer = view.findViewById(R.id.chronometer);
        btnRideToggle = view.findViewById(R.id.btnRideToggle);

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        btnRideToggle.setOnClickListener(v -> toggleTracking());

        return view;
    }

    private void toggleTracking() {
        if (!isTracking) {
            startRide();
        } else {
            stopRide();
        }
    }

    private void startRide() {
        if (checkPermission()) {
            isTracking = true;
            btnRideToggle.setText("Finish Ride");
            btnRideToggle.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light));

            // Reset Data
            pathPoints.clear();
            if (currentPolyline != null) currentPolyline.remove();
            sessionDistanceKm = 0.0;
            tvSessionDistance.setText("0.00 km");
            tvCurrentSpeed.setText("0.0 km/h");
            lastLocation = null;

            // Start Timer
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            rideStartTime = System.currentTimeMillis();

            startLocationUpdates();
        } else {
            requestPermission();
        }
    }

    private void stopRide() {
        isTracking = false;
        btnRideToggle.setText("Start Ride");
        btnRideToggle.setBackgroundResource(R.drawable.bg_button_gradient);

        chronometer.stop();
        long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
        long durationSeconds = elapsedMillis / 1000;

        // Calculate Average Speed
        double hours = durationSeconds / 3600.0;
        double avgSpeed = (hours > 0) ? (sessionDistanceKm / hours) : 0.0;

        saveRideData(sessionDistanceKm, durationSeconds, rideStartTime, avgSpeed, pathPoints);
    }

    private void saveRideData(double distance, long duration, long timestamp, double avgSpeed, List<LatLng> points) {
        // Convert LatLng to GeoPoint for Firestore
        List<GeoPoint> geoPoints = new ArrayList<>();
        // Optimization: Only save every 3rd point to save DB space if route is long
        for (int i = 0; i < points.size(); i++) {
            if (i % 2 == 0) { // Take every 2nd point
                geoPoints.add(new GeoPoint(points.get(i).latitude, points.get(i).longitude));
            }
        }

        Ride newRide = new Ride(distance, duration, timestamp, avgSpeed, geoPoints);

        // 1. Save to "rides" subcollection
        db.collection("users").document(userId).collection("rides")
                .add(newRide)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Ride saved to Travel Log!", Toast.LENGTH_SHORT).show();
                });

        // 2. Update Total Stats
        db.collection("users").document(userId)
                .update("totalKmTraveled", FieldValue.increment(distance));

        // Reset UI
        tvSessionDistance.setText("0.00 km");
        tvCurrentSpeed.setText("0.0 km/h");
        chronometer.setBase(SystemClock.elapsedRealtime());
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateDistanceMeters(2)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    updateTracking(location);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void updateTracking(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (isTracking) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

            // Speed Update (Convert m/s to km/h)
            float speedKmh = (location.hasSpeed()) ? location.getSpeed() * 3.6f : 0f;
            tvCurrentSpeed.setText(String.format(Locale.US, "%.1f km/h", speedKmh));

            if (lastLocation != null) {
                double distanceInMeters = lastLocation.distanceTo(location);
                if (distanceInMeters > 2) { // Filter noise
                    sessionDistanceKm += (distanceInMeters / 1000.0);
                    tvSessionDistance.setText(String.format(Locale.US, "%.2f km", sessionDistanceKm));

                    lastLocation = location;
                    pathPoints.add(latLng);
                    drawRoute();
                }
            } else {
                lastLocation = location;
                pathPoints.add(latLng);
            }
        }
    }

    private void drawRoute() {
        if (currentPolyline != null) currentPolyline.remove();
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(pathPoints)
                .width(15f)
                .color(ContextCompat.getColor(requireContext(), R.color.padyak_accent))
                .geodesic(true);
        currentPolyline = mMap.addPolyline(polylineOptions);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (checkPermission()) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                // Center on user initially
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(location.getLatitude(), location.getLongitude()), 16f));
                    }
                });
            }
        }
    }

    private boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
    }
}