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
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
        // Pre-initialize maps to avoid layout inflation lag/crash in Adapter
        try {
            MapsInitializer.initialize(requireContext());
        } catch (Exception e) {
            e.printStackTrace();
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
        adapter = new TravelLogAdapter(getContext(), rideList);
        recyclerView.setAdapter(adapter);

        // Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Button Listener: Go back to map
        btnBackToMap.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Find the BottomNavigation in MainActivity and select the first item (Advisor/Home)
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_route);
                }
            }
        });

        loadTravelLogs();

        return view;
    }

    private void loadTravelLogs() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").document(userId).collection("rides")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return; // Fix crash if fragment removed before data loads

                    progressBar.setVisibility(View.GONE);
                    rideList.clear();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<Ride> rides = queryDocumentSnapshots.toObjects(Ride.class);
                        rideList.addAll(rides);
                        adapter.notifyDataSetChanged();
                        tvEmptyState.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error loading logs", Toast.LENGTH_SHORT).show();
                });
    }
}