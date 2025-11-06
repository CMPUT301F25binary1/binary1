package com.example.fairchance;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.fairchance.ui.RoleSelectionActivity;
// We no longer need the MaterialButton
// import com.google.android.material.button.MaterialButton;

// --- ADD THESE IMPORTS ---
import com.example.fairchance.ui.fragments.HistoryFragment;
import com.example.fairchance.ui.fragments.InvitationsFragment; // <-- ADD THIS IMPORT
import com.example.fairchance.ui.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
// --- END of new imports ---

import com.example.fairchance.ui.fragments.AdminDashboardFragment;
// Rename this import
import com.example.fairchance.ui.fragments.EntrantHomeFragment;
import com.example.fairchance.ui.fragments.OrganizerDashboardFragment;


public class MainActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private BottomNavigationView bottomNav; // <-- ADD this

    // The logoutButton is no longer here
    // private MaterialButton logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authRepository = new AuthRepository();
        bottomNav = findViewById(R.id.bottom_navigation); // <-- FIND the new view

        // --- All the old logoutButton logic is REMOVED ---

        // Fetch the user's role to determine which UI to show
        authRepository.getUserRole(new AuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {

                // --- THIS IS THE NEW LOGIC ---
                switch (role) {
                    case "entrant":
                        // If entrant, show the nav bar and set it up
                        bottomNav.setVisibility(View.VISIBLE);
                        setupEntrantNavigation();
                        break;
                    case "organizer":
                        // If organizer, hide nav bar and load their simple dashboard
                        bottomNav.setVisibility(View.GONE);
                        loadDashboardFragment(new OrganizerDashboardFragment());
                        break;
                    case "admin":
                        // If admin, hide nav bar and load their simple dashboard
                        bottomNav.setVisibility(View.GONE);
                        loadDashboardFragment(new AdminDashboardFragment());
                        break;
                    default:
                        Log.e("MainActivity", "Unknown role, defaulting to entrant.");
                        bottomNav.setVisibility(View.VISIBLE);
                        setupEntrantNavigation();
                        break;
                }
            }

            @Override
            public void onError(String message) {
                // Log the error and log out
                Log.e("MainActivity", "Error fetching role: " + message);
                logout();
            }
        });
    }

    /**
     * Sets up the BottomNavigationView for the Entrant role.
     */
    private void setupEntrantNavigation() {
        bottomNav.inflateMenu(R.menu.entrant_nav_menu);

        // Load the default "Home" fragment
        loadDashboardFragment(new EntrantHomeFragment());
        // Set "Home" as selected in the menu
        bottomNav.setSelectedItemId(R.id.nav_home); // <-- ADD THIS

        // Set the listener for navigation
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new EntrantHomeFragment();
            } else if (itemId == R.id.nav_invitations) { // <-- ADD THIS BLOCK
                selectedFragment = new InvitationsFragment();
            } else if (itemId == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadDashboardFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    /**
     * Helper method to load a Fragment into the 'dashboard_container'
     */
    private void loadDashboardFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.dashboard_container, fragment);
        transaction.commit();
    }

    /**
     * Helper method to log out the user and return to role selection
     */
    private void logout() {
        authRepository.signOut();
        Toast.makeText(MainActivity.this, "Logged out.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, RoleSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}