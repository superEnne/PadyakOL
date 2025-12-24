package com.example.padyakol;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.padyakol.adapters.TravelLogAdapter;
import com.example.padyakol.models.Ride;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class TravelLogFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private Button btnBackToMap;
    private TravelLogAdapter adapter;
    private List<Ride> rideList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_travel_log, container, false);

        recyclerView = view.findViewById(R.id.recyclerTravelLog);
        progressBar = view.findViewById(R.id.progressBarLog);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        btnBackToMap = view.findViewById(R.id.btnBackToMap);

        // Setup Recycler
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        rideList = new ArrayList<>();

        // --- ADAPTER SETUP WITH CLICK LISTENER ---
        adapter = new TravelLogAdapter(requireContext(), rideList, ride -> {
            // This code runs when a user clicks a log card
            try {
                if (ride.getRoutePoints() != null && !ride.getRoutePoints().isEmpty()) {
                    RideDetailDialogFragment dialog = RideDetailDialogFragment.newInstance(ride);
                    dialog.show(getChildFragmentManager(), "RideDetail");
                } else {
                    Toast.makeText(getContext(), "No route data available for this ride.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("TravelLog", "Error opening details: ", e);
                Toast.makeText(getContext(), "Unable to open ride details.", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(adapter);

        // Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Button Listener: Go back to map
        btnBackToMap.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            }
        });

        loadTravelLogs();

        return view;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadTravelLogs();
        }
    }

    private void loadTravelLogs() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        // Only show progress if list is empty
        if (rideList.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        db.collection("users").document(userId).collection("rides")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    progressBar.setVisibility(View.GONE);
                    rideList.clear();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        try {
                            // WRAPPED IN TRY-CATCH TO PREVENT CRASHES ON BAD DATA
                            List<Ride> rides = queryDocumentSnapshots.toObjects(Ride.class);
                            rideList.addAll(rides);
                            adapter.notifyDataSetChanged();
                            tvEmptyState.setVisibility(View.GONE);
                        } catch (Exception e) {
                            // This catches data type mismatches or missing fields
                            Log.e("TravelLog", "Error parsing ride data", e);
                            Toast.makeText(getContext(), "Error loading some rides: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                            // Optional: Show empty state if everything failed
                            if (rideList.isEmpty()) {
                                tvEmptyState.setText("Error loading ride history.");
                                tvEmptyState.setVisibility(View.VISIBLE);
                            }
                        }
                    } else {
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    Log.e("TravelLog", "Firestore Error", e);
                    Toast.makeText(getContext(), "Failed to load history.", Toast.LENGTH_SHORT).show();
                });
    }
}