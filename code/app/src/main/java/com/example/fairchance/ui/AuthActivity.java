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
 * The UI adapts based on the "ROLE" extra passed in the Intent from RoleSelectionActivity.
 * - "entrant": Anonymous sign-in (registration only).
 * - "organizer" / "admin": Email/password login and registration.
 */
public class AuthActivity extends AppCompatActivity implements AuthRepository.AuthCallback {

    private AuthRepository authRepository;
    private String currentRole;
    private boolean isRegisterMode = false;

    // UI Components
    private TextView authTitle;
    private TextInputLayout passwordLayout, firstNameLayout, lastNameLayout, phoneLayout;
    private TextInputEditText emailEditText, passwordEditText, firstNameEditText, lastNameEditText, phoneEditText;
    private MaterialButton actionButton, toggleButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        authRepository = new AuthRepository();
        currentRole = getIntent().getStringExtra("ROLE");
        if (currentRole == null) {
            currentRole = "entrant";
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
        phoneLayout = findViewById(R.id.phone_layout);
        phoneEditText = findViewById(R.id.edit_text_phone);


        setupUIForRole();

        toggleButton.setOnClickListener(v -> toggleMode());
        actionButton.setOnClickListener(v -> performAuthAction());
    }

    /**
     * Configures the initial UI state based on the user's role.
     * Entrants have a register-only flow, while Organizers/Admins can toggle.
     */
    private void setupUIForRole() {
        if (currentRole.equals("entrant")) {
            authTitle.setText("Entrant Registration");
            passwordLayout.setVisibility(View.GONE);
            toggleButton.setVisibility(View.GONE);
            actionButton.setText("Register & Enter");
            isRegisterMode = true; // Entrant is always in register mode
            updateRegistrationFieldsVisibility(true);
        } else {
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
     * Toggles the UI between Login and Registration modes (for Organizers/Admins only).
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
     * Helper method to show or hide registration-specific fields.
     *
     * @param show True to show fields, false to hide.
     */
    private void updateRegistrationFieldsVisibility(boolean show) {
        firstNameLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        lastNameLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        phoneLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Validates user input and calls the appropriate AuthRepository method
     * based on the current mode (register/login) and role.
     */
    private void performAuthAction() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        // Validation
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

        // Call appropriate repository method
        if (isRegisterMode) {
            if (currentRole.equals("entrant")) {
                authRepository.registerAndLoginEntrant(email, firstName, lastName, phone, this);
            } else {
                authRepository.registerEmailPasswordUser(email, password, currentRole, firstName, lastName, phone, this);
            }
        } else {
            authRepository.loginEmailPasswordUser(email, password, currentRole, this);
        }
    }

    /**
     * Shows/hides the progress bar and enables/disables buttons.
     *
     * @param isLoading True to show loading state, false otherwise.
     */
    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        actionButton.setEnabled(!isLoading);
        toggleButton.setEnabled(!isLoading);
    }

    /**
     * Callback for a successful authentication from AuthRepository.
     * Navigates to the MainActivity.
     *
     * @param user The newly logged-in FirebaseUser.
     */
    @Override
    public void onSuccess(FirebaseUser user) {
        setLoading(false);
        Toast.makeText(this, "Success! Welcome.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Callback for a failed authentication from AuthRepository.
     * Shows an error dialog.
     *
     * @param message The error message to display.
     */
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