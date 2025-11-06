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
import com.example.fairchance.ui.fragments.HistoryFragment;
import com.example.fairchance.ui.fragments.InvitationsFragment;
import com.example.fairchance.ui.fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.fairchance.ui.fragments.AdminDashboardFragment;
import com.example.fairchance.ui.fragments.EntrantHomeFragment;
import com.example.fairchance.ui.fragments.OrganizerDashboardFragment;

/**
 * The main container activity for the app after a user is logged in.
 * It's responsible for checking the user's role (Entrant, Organizer, or Admin)
 * and loading the appropriate dashboard fragment.
 * For Entrants, it also manages the BottomNavigationView.
 */
public class MainActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authRepository = new AuthRepository();
        bottomNav = findViewById(R.id.bottom_navigation);

        // Fetch the user's role to determine which UI to show
        authRepository.getUserRole(new AuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {
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
     * Sets up the BottomNavigationView for the Entrant role,
     * inflates its menu, and handles navigation item clicks.
     */
    private void setupEntrantNavigation() {
        bottomNav.inflateMenu(R.menu.entrant_nav_menu);

        // Load the default "Home" fragment
        loadDashboardFragment(new EntrantHomeFragment());
        // Set "Home" as selected in the menu
        bottomNav.setSelectedItemId(R.id.nav_home);

        // Set the listener for navigation
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new EntrantHomeFragment();
            } else if (itemId == R.id.nav_invitations) {
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
     * Helper method to replace the content of the 'dashboard_container' FrameLayout
     * with a given Fragment.
     *
     * @param fragment The Fragment to display.
     */
    private void loadDashboardFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.dashboard_container, fragment);
        transaction.commit();
    }

    /**
     * Helper method to log out the current user, clear the activity stack,
     * and redirect to the RoleSelectionActivity.
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