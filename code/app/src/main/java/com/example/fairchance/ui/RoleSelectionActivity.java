package com.example.fairchance.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fairchance.R;
import com.google.android.material.button.MaterialButton;

/**
 * This activity is shown to new or logged-out users, allowing them
 * to select their role before proceeding to authentication.
 */
public class RoleSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        MaterialButton entrantButton = findViewById(R.id.button_entrant);
        MaterialButton organizerButton = findViewById(R.id.button_organizer);
        MaterialButton adminButton = findViewById(R.id.button_admin);

        entrantButton.setOnClickListener(v -> startAuthActivity("entrant"));
        organizerButton.setOnClickListener(v -> startAuthActivity("organizer"));
        adminButton.setOnClickListener(v -> startAuthActivity("admin"));
    }

    /**
     * Starts the AuthActivity, passing the selected role as an Intent extra.
     * @param role The role selected ("entrant", "organizer", or "admin").
     */
    private void startAuthActivity(String role) {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra("ROLE", role);
        startActivity(intent);
    }
}