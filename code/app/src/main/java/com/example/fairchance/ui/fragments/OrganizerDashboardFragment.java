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

import com.example.fairchance.EventRepository;
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

        // --- Firestore reference (for simple settings demos) ---
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

        // ========== FEATURE 3: SAMPLE N ATTENDEES (US 02.05.02) ==========
        EditText etEventId = view.findViewById(R.id.edit_event_id_for_sampling);
        EditText etCount   = view.findViewById(R.id.edit_sample_count);
        Button btnRun      = view.findViewById(R.id.btnRunSampling);

        if (etEventId != null && etCount != null && btnRun != null) {
            EventRepository repo = new EventRepository();

            btnRun.setOnClickListener(v -> {
                String eventId = etEventId.getText().toString().trim();
                String c = etCount.getText().toString().trim();

                if (eventId.isEmpty() || c.isEmpty()) {
                    Toast.makeText(getContext(), "Enter Event ID and N.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int n;
                try {
                    n = Integer.parseInt(c);
                    if (n <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "N must be a positive number.", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnRun.setEnabled(false);
                repo.sampleAttendees(eventId, n, new EventRepository.EventTaskCallback() {
                    @Override public void onSuccess() {
                        btnRun.setEnabled(true);
                        Toast.makeText(getContext(), "Sampled " + n + " entrants.", Toast.LENGTH_LONG).show();
                    }
                    @Override public void onError(String msg) {
                        btnRun.setEnabled(true);
                        Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        return view;
    }
}