package com.example.padyakol.models;

import com.google.firebase.firestore.GeoPoint;
import java.util.List;

public class Ride {
    private String rideId;
    // We use Wrapper classes (Double, Long) instead of primitives (double, long)
    // This prevents the app from crashing if a field is missing or null in Firestore.
    private Double distanceKm;
    private Long durationSeconds;
    private Long timestamp;
    private Double avgSpeedKmh;
    private List<GeoPoint> routePoints;

    public Ride() {
        // Empty constructor needed for Firestore
    }

    public Ride(Double distanceKm, Long durationSeconds, Long timestamp, Double avgSpeedKmh, List<GeoPoint> routePoints) {
        this.distanceKm = distanceKm;
        this.durationSeconds = durationSeconds;
        this.timestamp = timestamp;
        this.avgSpeedKmh = avgSpeedKmh;
        this.routePoints = routePoints;
    }

    // Getters and Setters
    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }

    // --- Safe Getters (Return 0 instead of null to prevent crashes in UI) ---
    public double getDistanceKm() {
        return distanceKm != null ? distanceKm : 0.0;
    }

    public long getDurationSeconds() {
        return durationSeconds != null ? durationSeconds : 0L;
    }

    public long getTimestamp() {
        return timestamp != null ? timestamp : 0L;
    }

    public double getAvgSpeedKmh() {
        return avgSpeedKmh != null ? avgSpeedKmh : 0.0;
    }

    public List<GeoPoint> getRoutePoints() { return routePoints; }

    // --- Setters (Used by Firestore) ---
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    public void setAvgSpeedKmh(Double avgSpeedKmh) { this.avgSpeedKmh = avgSpeedKmh; }
    public void setRoutePoints(List<GeoPoint> routePoints) { this.routePoints = routePoints; }
}