package com.example.padyakol;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private ImageButton btnSettings;
    private BottomNavigationView bottomNavigationView;

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
            logout();
            return;
        }
        userId = currentUser.getUid();

        // 2. Initialize Views
        tvSessionDistance = findViewById(R.id.tvSessionDistance);
        tvTotalDistance = findViewById(R.id.tvTotalDistance);
        btnRideToggle = findViewById(R.id.btnRideToggle);
        btnSettings = findViewById(R.id.btnSettings);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 3. Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 4. Load initial data
        loadUserData();

        // 5. Setup Listeners
        btnRideToggle.setOnClickListener(v -> toggleTracking());
        btnSettings.setOnClickListener(v -> showSettingsMenu(v));

        // Bottom Navigation Listener (Functionality placeholders)
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_log) {
                Toast.makeText(this, "Travel Log coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_route) {
                Toast.makeText(this, "Travel Advisor coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_friends) {
                Toast.makeText(this, "Friends coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_account) {
                Toast.makeText(this, "Account coming soon", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    // --- SETTINGS MENU ---
    private void showSettingsMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.settings_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_general) {
                Toast.makeText(MainActivity.this, "General Settings", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_help) {
                Toast.makeText(MainActivity.this, "Help Section", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_about) {
                Toast.makeText(MainActivity.this, "PadyakOL v1.0", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_logout) {
                logout();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
                    // Update local total immediately
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

            // Start updates specifically for tracking
            startLocationUpdates();
        } else {
            requestPermission();
        }
    }

    private void stopRide() {
        isTracking = false;
        btnRideToggle.setText("Start Ride");
        btnRideToggle.setBackgroundResource(R.drawable.bg_button_gradient);

        // Don't stop location updates completely, just stop saving points
        // We might want to keep updating the user's dot on the map

        saveRideData();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateDistanceMeters(5)
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

    private void updateTracking(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        // Always move camera if tracking is active
        if (isTracking) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

            if (lastLocation != null) {
                double distanceInMeters = lastLocation.distanceTo(location);
                // Filter out small jitters (e.g., less than 2 meters)
                if (distanceInMeters > 2) {
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
        if (currentPolyline != null) {
            currentPolyline.remove();
        }
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(pathPoints)
                .width(15f)
                .color(ContextCompat.getColor(this, R.color.padyak_accent))
                .geodesic(true);
        currentPolyline = mMap.addPolyline(polylineOptions);
    }

    // --- MAP SETUP ---

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (checkPermission()) {
            setupMapUserLocation();
        } else {
            requestPermission();
        }
    }

    private void setupMapUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true); // Allow user to center themselves

            // Get immediate location to center the camera upon opening app
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 16f));
                }
            });
        }
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
                setupMapUserLocation();
            } else {
                Toast.makeText(this, "Location permission needed for maps", Toast.LENGTH_LONG).show();
            }
        }
    }
}