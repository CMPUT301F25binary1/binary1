package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fairchance.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple fragment that displays the main dashboard for the 'Organizer' role.
 * This is loaded into MainActivity when an Organizer logs in.
 */
public class OrganizerDashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard_organizer, container, false);

        // --- Firestore reference ---
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // ========== FEATURE 1: GEOLOCATION TOGGLE (US 02.02.03) ==========
        CheckBox geoCheckBox = view.findViewById(R.id.checkbox_geolocation_required);
        Button btnSaveGeo = view.findViewById(R.id.btnSaveGeoSetting);

        btnSaveGeo.setOnClickListener(v -> {
            boolean geoRequired = geoCheckBox.isChecked();

            Map<String, Object> data = new HashMap<>();
            data.put("geolocationRequired", geoRequired);
            data.put("updatedAt", com.google.firebase.Timestamp.now());

            db.collection("testOrganizerSettings").document("geoToggleDemo")
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        String message = geoRequired
                                ? "Setting saved: Geolocation is now REQUIRED for entrants."
                                : "Setting saved: Geolocation is now OPTIONAL for entrants.";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Error saving setting: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // ========== FEATURE 2: WAITLIST LIMIT (US 02.03.01) ==========
        EditText limitInput = view.findViewById(R.id.edit_waitlist_limit);
        Button btnSaveLimit = view.findViewById(R.id.btnSaveWaitlistLimit);

        btnSaveLimit.setOnClickListener(v -> {
            String inputText = limitInput.getText().toString().trim();
            long limitValue = 0;

            if (!inputText.isEmpty()) {
                try {
                    limitValue = Long.parseLong(inputText);
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Please enter a valid number.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("waitingListLimit", limitValue);
            data.put("updatedAt", com.google.firebase.Timestamp.now());

            long finalLimitValue = limitValue;
            db.collection("testOrganizerSettings").document("waitlistLimitDemo")
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        if (finalLimitValue == 0) {
                            Toast.makeText(getContext(), "No limit set (unlimited).", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Waitlist limit saved: " + finalLimitValue, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Error saving limit: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        return view;
    }
}