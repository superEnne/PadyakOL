package com.example.padyakol;

import android.os.Bundle;
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
import com.google.android.gms.maps.MapsInitializer;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Pre-initialize maps using Application context if possible, or Activity
        if (getContext() != null) {
            try {
                MapsInitializer.initialize(getContext().getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

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
        // Use requireContext() for safety
        adapter = new TravelLogAdapter(requireContext(), rideList);
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

        // Always reload logs when view is created to get latest
        loadTravelLogs();

        return view;
    }

    // Refresh logs when the fragment becomes visible (since we use hide/show now)
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
                        List<Ride> rides = queryDocumentSnapshots.toObjects(Ride.class);
                        rideList.addAll(rides);
                        adapter.notifyDataSetChanged();
                        tvEmptyState.setVisibility(View.   GONE);
                    } else {
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    // Toast.makeText(getContext(), "Error loading logs", Toast.LENGTH_SHORT).show();
                });
    }
}