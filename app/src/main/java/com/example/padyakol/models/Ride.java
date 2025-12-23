package com.example.padyakol.models;

import com.google.firebase.firestore.GeoPoint;
import java.util.List;

public class Ride {
    private String rideId;
    private double distanceKm;
    private long durationSeconds;
    private long timestamp;
    private double avgSpeedKmh;
    private List<GeoPoint> routePoints;

    public Ride() {
        // Empty constructor needed for Firestore
    }

    public Ride(double distanceKm, long durationSeconds, long timestamp, double avgSpeedKmh, List<GeoPoint> routePoints) {
        this.distanceKm = distanceKm;
        this.durationSeconds = durationSeconds;
        this.timestamp = timestamp;
        this.avgSpeedKmh = avgSpeedKmh;
        this.routePoints = routePoints;
    }

    // Getters and Setters
    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }

    public double getDistanceKm() { return distanceKm; }
    public long getDurationSeconds() { return durationSeconds; }
    public long getTimestamp() { return timestamp; }
    public double getAvgSpeedKmh() { return avgSpeedKmh; }
    public List<GeoPoint> getRoutePoints() { return routePoints; }
}