package com.example.fairchance.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.fairchance.AuthRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.User;
import com.example.fairchance.ui.RoleSelectionActivity;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private AuthRepository authRepository;
    private TextInputEditText etName, etEmail, etPhoneNumber, etRole;
    private Button btnSaveChanges, btnLogout, btnDeleteProfile;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.entrant_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository();

        // Find all the views from the layout
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhoneNumber = view.findViewById(R.id.etPhoneNumber);
        etRole = view.findViewById(R.id.etRole);
        btnSaveChanges = view.findViewById(R.id.button_save_changes);
        btnLogout = view.findViewById(R.id.button_logout);
        btnDeleteProfile = view.findViewById(R.id.button_delete_profile);
        progressBar = view.findViewById(R.id.profile_progress_bar);

        // Disable editing for role (as requested)
        etRole.setEnabled(false);

        // Load the user's data
        loadUserProfile();

        // --- SET UP BUTTON LISTENERS ---
        btnLogout.setOnClickListener(v -> logout());
        btnSaveChanges.setOnClickListener(v -> saveChanges());
        btnDeleteProfile.setOnClickListener(v -> confirmDeleteProfile());
    }

    private void loadUserProfile() {
        setLoading(true);
        authRepository.getUserProfile(new AuthRepository.UserProfileCallback() {
            @Override
            public void onSuccess(User user) {
                etName.setText(user.getName());
                etEmail.setText(user.getEmail());
                etPhoneNumber.setText(user.getPhone());

                String role = user.getRole();
                if (role != null && !role.isEmpty()) {
                    // Capitalize first letter for display
                    String capitalizedRole = role.substring(0, 1).toUpperCase() + role.substring(1);
                    etRole.setText(capitalizedRole);
                }
                setLoading(false);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error loading profile: " + message);
                Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        });
    }

    private void saveChanges() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();

        // Simple Validation
        if (name.isEmpty() || email.isEmpty()) {
            etName.setError(name.isEmpty() ? "Name cannot be empty" : null);
            etEmail.setError(email.isEmpty() ? "Email cannot be empty" : null);
            return;
        }

        setLoading(true);
        authRepository.updateUserProfile(name, email, phone, new AuthRepository.TaskCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Log.e(TAG, "Error updating profile: " + message);
                Toast.makeText(getContext(), "Failed to update: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteProfile() {
        // Show a confirmation dialog before deleting
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to permanently delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProfile() {
        setLoading(true);
        authRepository.deleteCurrentUser(new AuthRepository.TaskCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(getContext(), "Profile deleted.", Toast.LENGTH_SHORT).show();
                goToRoleSelection();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Log.e(TAG, "Error deleting profile: " + message);
                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                // Note: Deleting an anonymous account may require re-authentication.
                // If this fails, the user may need to clear app data.
            }
        });
    }

    private void logout() {
        authRepository.signOut();
        Toast.makeText(getContext(), "Logged out.", Toast.LENGTH_SHORT).show();
        goToRoleSelection();
    }

    private void goToRoleSelection() {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), RoleSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private void setLoading(boolean isLoading) {
        if (progressBar == null) return;

        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveChanges.setEnabled(!isLoading);
        btnLogout.setEnabled(!isLoading);
        btnDeleteProfile.setEnabled(!isLoading);
    }
}