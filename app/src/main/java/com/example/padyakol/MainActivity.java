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
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private TextView tvPageTitle;
    private ImageButton btnSettings;

    // Fragments
    private HomeFragment homeFragment;
    private TravelLogFragment travelLogFragment;

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
        homeFragment = new HomeFragment();
        travelLogFragment = new TravelLogFragment();

        // 4. Default Load Home
        loadFragment(homeFragment);
        tvPageTitle.setText("Ride Dashboard");

        // 5. Navigation Listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_route) { // "Advisor" mapped to Home/Map
                loadFragment(homeFragment);
                tvPageTitle.setText("Ride Dashboard");
                return true;
            } else if (id == R.id.nav_log) { // Travel Log
                loadFragment(travelLogFragment);
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

    // Smooth Transition Method
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
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