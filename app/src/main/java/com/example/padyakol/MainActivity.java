package com.example.padyakol;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private TextView tvPageTitle;
    private ImageButton btnSettings;

    // Fragments
    private HomeFragment homeFragment;
    private TravelLogFragment travelLogFragment;
    private Fragment activeFragment;

    // Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Auth Check
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // 2. Init Views
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        tvPageTitle = findViewById(R.id.tvPageTitle);
        btnSettings = findViewById(R.id.btnSettings);

        // 3. Init Fragments
        // We initialize them once and keep them alive
        homeFragment = new HomeFragment();
        travelLogFragment = new TravelLogFragment();

        // 4. Default Load Home (Add it initially)
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().add(R.id.fragment_container, homeFragment, "HOME").commit();
        activeFragment = homeFragment;
        tvPageTitle.setText("Ride Dashboard");

        // 5. Navigation Listener (Using hide/show to prevent Map crashes)
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_route) { // "Advisor" mapped to Home/Map
                switchFragment(homeFragment, "HOME");
                tvPageTitle.setText("Ride Dashboard");
                return true;
            } else if (id == R.id.nav_log) { // Travel Log
                switchFragment(travelLogFragment, "LOG");
                tvPageTitle.setText("My Travel Log");
                return true;
            } else if (id == R.id.nav_friends || id == R.id.nav_account) {
                Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        // 6. Settings Listener
        btnSettings.setOnClickListener(this::showSettingsMenu);
    }

    // CRASH FIX: Safe Fragment Switching
    // Instead of replacing (destroying) fragments, we hide/show them.
    // This keeps the heavy Google Map alive in memory.
    private void switchFragment(Fragment targetFragment, String tag) {
        if (activeFragment == targetFragment) return;

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);

        // Hide the current fragment
        if (activeFragment != null) {
            transaction.hide(activeFragment);
        }

        // Show or Add the target fragment
        if (!targetFragment.isAdded()) {
            transaction.add(R.id.fragment_container, targetFragment, tag);
        } else {
            transaction.show(targetFragment);
        }

        activeFragment = targetFragment;
        transaction.commit();
    }

    public void navigateToHome() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_route);
        }
    }

    private void showSettingsMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.settings_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            }
            return false;
        });
        popup.show();
    }
}