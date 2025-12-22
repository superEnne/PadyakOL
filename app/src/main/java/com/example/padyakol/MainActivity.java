package com.example.padyakol;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // UI
    private TextView tvSessionDistance, tvTotalDistance;
    private Button btnRideToggle;

    // Data
    private boolean isTracking = false;
    private double sessionDistanceKm = 0.0;
    private double totalDistanceKm = 0.0;
    private Location lastLocation = null;
    private List<LatLng> pathPoints = new ArrayList<>();
    private Polyline currentPolyline;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Firebase & Auth Check
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Not logged in? Go back to Login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = currentUser.getUid();

        // 2. Initialize Views
        tvSessionDistance = findViewById(R.id.tvSessionDistance);
        tvTotalDistance = findViewById(R.id.tvTotalDistance);
        btnRideToggle = findViewById(R.id.btnRideToggle);

        // 3. Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 4. Load initial data
        loadUserData();

        // 5. Setup Buttons
        btnRideToggle.setOnClickListener(v -> toggleTracking());
    }

    // --- FIREBASE LOGIC ---

    private void loadUserData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double total = documentSnapshot.getDouble("totalKmTraveled");
                        if (total != null) {
                            totalDistanceKm = total;
                            tvTotalDistance.setText(String.format(Locale.US, "%.1f km", totalDistanceKm));
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to sync data", Toast.LENGTH_SHORT).show());
    }

    private void saveRideData() {
        // Update Firestore: Add session distance to total
        db.collection("users").document(userId)
                .update("totalKmTraveled", FieldValue.increment(sessionDistanceKm))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Ride saved!", Toast.LENGTH_SHORT).show();
                    // Update local total immediately for UI responsiveness
                    totalDistanceKm += sessionDistanceKm;
                    tvTotalDistance.setText(String.format(Locale.US, "%.1f km", totalDistanceKm));
                    // Reset session
                    sessionDistanceKm = 0.0;
                    tvSessionDistance.setText("0.0 km");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving ride", Toast.LENGTH_SHORT).show());
    }

    // --- TRACKING LOGIC ---

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
            btnRideToggle.setText("Stop Ride");
            btnRideToggle.setBackgroundResource(android.R.color.holo_red_light); // Visual feedback

            // Reset for new ride
            pathPoints.clear();
            if (currentPolyline != null) currentPolyline.remove();
            sessionDistanceKm = 0.0;
            tvSessionDistance.setText("0.0 km");
            lastLocation = null;

            startLocationUpdates();
        } else {
            requestPermission();
        }
    }

    private void stopRide() {
        isTracking = false;
        btnRideToggle.setText("Start Ride");
        btnRideToggle.setBackgroundResource(R.drawable.bg_button_gradient);

        stopLocationUpdates();
        saveRideData();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateDistanceMeters(10) // Only update if moved 10 meters
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateTracking(location);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void updateTracking(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        // Move camera to user
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));

        if (isTracking) {
            if (lastLocation != null) {
                // Calculate distance
                double distanceInMeters = lastLocation.distanceTo(location);
                sessionDistanceKm += (distanceInMeters / 1000.0);
                tvSessionDistance.setText(String.format(Locale.US, "%.2f km", sessionDistanceKm));
            }

            lastLocation = location;
            pathPoints.add(latLng);
            drawRoute();
        }
    }

    private void drawRoute() {
        if (currentPolyline != null) {
            currentPolyline.remove();
        }
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(pathPoints)
                .width(12f)
                .color(ContextCompat.getColor(this, R.color.padyak_accent))
                .geodesic(true);
        currentPolyline = mMap.addPolyline(polylineOptions);
    }

    // --- MAP SETUP ---

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (checkPermission()) {
            mMap.setMyLocationEnabled(true);
            // Get last known location to center map initially
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f));
                }
            });
        } else {
            requestPermission();
        }

        // Route Suggestion (Placeholder logic)
        findViewById(R.id.cardSearch).setOnClickListener(v -> {
            Toast.makeText(this, "Safe Route feature coming soon!", Toast.LENGTH_SHORT).show();
            // Future implementation: Open Place Autocomplete -> Get Directions API with "Avoid highways"
        });
    }

    // --- PERMISSIONS ---

    private boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkPermission()) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, "Location permission needed for tracking", Toast.LENGTH_LONG).show();
            }
        }
    }
}