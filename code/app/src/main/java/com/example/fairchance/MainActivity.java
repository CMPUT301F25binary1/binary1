package com.example.fairchance;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fairchance.AuthRepository;
import com.example.fairchance.ui.RoleSelectionActivity;
import com.google.android.material.button.MaterialButton;

/**
 * The main screen of the application, shown after a user is logged in.
 * This class also handles the logic for conditionally displaying the
 * logout button based on the user's role.
 */
public class MainActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private MaterialButton logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authRepository = new AuthRepository();
        logoutButton = findViewById(R.id.button_logout);

        // Hide the button by default
        logoutButton.setVisibility(View.GONE);

        // Set the click listener
        logoutButton.setOnClickListener(v -> {
            authRepository.signOut();
            Toast.makeText(MainActivity.this, "Logged out successfully.", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(MainActivity.this, RoleSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Fetch the user's role to determine if the logout button should be visible
        authRepository.getUserRole(new AuthRepository.RoleCallback() {
            @Override
            public void onRoleFetched(String role) {
                if ("organizer".equals(role) || "admin".equals(role)) {
                    logoutButton.setVisibility(View.VISIBLE);
                }
                // If the role is "entrant", the button remains GONE
            }

            @Override
            public void onError(String message) {
                // Log the error and keep the button hidden
                Log.e("MainActivity", "Error fetching role: " + message);
            }
        });
    }
}