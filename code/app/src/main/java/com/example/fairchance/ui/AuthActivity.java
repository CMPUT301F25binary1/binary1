package com.example.fairchance.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fairchance.MainActivity;
import com.example.fairchance.R;
import com.example.fairchance.AuthRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

/**
 * A dynamic activity that handles both Registration and Login for all user roles.
 * The UI adapts based on the "ROLE" extra passed in the Intent.
 */
public class AuthActivity extends AppCompatActivity implements AuthRepository.AuthCallback {

    private AuthRepository authRepository;
    private String currentRole;
    private boolean isRegisterMode = false;

    // UI Components
    private TextView authTitle;
    private TextInputLayout passwordLayout, firstNameLayout, lastNameLayout;
    private TextInputEditText emailEditText, passwordEditText, firstNameEditText, lastNameEditText;
    private MaterialButton actionButton, toggleButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        authRepository = new AuthRepository();
        currentRole = getIntent().getStringExtra("ROLE");
        if (currentRole == null) {
            currentRole = "entrant"; // Default
        }

        // Find all UI views
        authTitle = findViewById(R.id.auth_title);
        passwordLayout = findViewById(R.id.password_layout);
        emailEditText = findViewById(R.id.edit_text_email);
        passwordEditText = findViewById(R.id.edit_text_password);
        actionButton = findViewById(R.id.auth_action_button);
        toggleButton = findViewById(R.id.auth_toggle_button);
        progressBar = findViewById(R.id.auth_progress);
        firstNameLayout = findViewById(R.id.firstName_layout);
        lastNameLayout = findViewById(R.id.lastName_layout);
        firstNameEditText = findViewById(R.id.edit_text_firstName);
        lastNameEditText = findViewById(R.id.edit_text_lastName);

        setupUIForRole();

        toggleButton.setOnClickListener(v -> toggleMode());
        actionButton.setOnClickListener(v -> performAuthAction());
    }

    /**
     * Configures the initial UI state based on the user's role.
     */
    private void setupUIForRole() {
        if (currentRole.equals("entrant")) {
            // Entrant Flow: Register-only, no password, no toggle
            authTitle.setText("Entrant Registration");
            passwordLayout.setVisibility(View.GONE);
            toggleButton.setVisibility(View.GONE);
            actionButton.setText("Register & Enter");
            isRegisterMode = true; // Entrant is always in register mode
            updateRegistrationFieldsVisibility(true);
        } else {
            // Organizer/Admin Flow: Login/Register toggle
            String roleCapitalized = currentRole.substring(0, 1).toUpperCase() + currentRole.substring(1);
            authTitle.setText(roleCapitalized + " Login");
            passwordLayout.setVisibility(View.VISIBLE);
            toggleButton.setVisibility(View.VISIBLE);
            actionButton.setText("Login");
            toggleButton.setText("Don't have an account? Register");
            isRegisterMode = false;
            updateRegistrationFieldsVisibility(false);
        }
    }

    /**
     * Toggles the UI between Login and Registration modes for Organizers/Admins.
     */
    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        String roleCapitalized = currentRole.substring(0, 1).toUpperCase() + currentRole.substring(1);

        if (isRegisterMode) {
            authTitle.setText(roleCapitalized + " Registration");
            actionButton.setText("Register");
            toggleButton.setText("Already have an account? Login");
            updateRegistrationFieldsVisibility(true);
        } else {
            authTitle.setText(roleCapitalized + " Login");
            actionButton.setText("Login");
            toggleButton.setText("Don't have an account? Register");
            updateRegistrationFieldsVisibility(false);
        }
    }

    /**
     * Helper method to show or hide the First/Last Name fields.
     */
    private void updateRegistrationFieldsVisibility(boolean show) {
        firstNameLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        lastNameLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Validates user input and calls the appropriate AuthRepository method.
     */
    private void performAuthAction() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return;
        }

        if (isRegisterMode) {
            if (firstName.isEmpty()) {
                firstNameEditText.setError("First name is required");
                return;
            }
            if (lastName.isEmpty()) {
                lastNameEditText.setError("Last name is required");
                return;
            }
        }

        if (passwordLayout.getVisibility() == View.VISIBLE && password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return;
        }

        setLoading(true);

        if (isRegisterMode) {
            if (currentRole.equals("entrant")) {
                authRepository.registerAndLoginEntrant(email, firstName, lastName, this);
            } else {
                authRepository.registerEmailPasswordUser(email, password, currentRole, firstName, lastName, this);
            }
        } else {
            // LOGIN (Only for Organizer/Admin)
            authRepository.loginEmailPasswordUser(email, password, currentRole, this);
        }
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        actionButton.setEnabled(!isLoading);
        toggleButton.setEnabled(!isLoading);
    }

    @Override
    public void onSuccess(FirebaseUser user) {
        setLoading(false);
        Toast.makeText(this, "Success! Welcome.", Toast.LENGTH_SHORT).show();

        // Go directly to the main app
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onError(String message) {
        setLoading(false);
        new AlertDialog.Builder(this)
                .setTitle("Authentication Failed")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}