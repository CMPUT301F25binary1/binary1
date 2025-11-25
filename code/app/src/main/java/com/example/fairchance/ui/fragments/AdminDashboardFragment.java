package com.example.fairchance.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.fairchance.R;
import com.example.fairchance.ui.OrganizerManagementActivity;

/**
 * Admin Dashboard Screen
 * Allows navigation to:
 *  - Event Management
 *  - Profile Management
 *  - Image Management
 *  - Notification Logs
 */
public class AdminDashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate the correct full dashboard layout
        View root = inflater.inflate(R.layout.admin_main_dashboard, container, false);

        // Wire the cards (make sure XML has these IDs)
        CardView cardProfile = root.findViewById(R.id.cardProfileManagement);
        CardView cardEvent = root.findViewById(R.id.cardEventManagement);
        CardView cardImage = root.findViewById(R.id.cardImageManagement);
        CardView cardNotifications = root.findViewById(R.id.cardNotifications);

        // PROFILE MANAGEMENT → AdminProfileManagementFragment
        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> openFragment(new AdminProfileManagementFragment()));
        }

        // EVENT MANAGEMENT → AdminEventManagementFragment
        if (cardEvent != null) {
            cardEvent.setOnClickListener(v -> openFragment(new AdminEventManagementFragment()));
        }

        // IMAGE MANAGEMENT → AdminImageManagementFragment
        if (cardImage != null) {
            cardImage.setOnClickListener(v -> openFragment(new AdminImageManagementFragment()));
        }

        // NOTIFICATION LOGS → AdminNotificationLogsFragment
        if (cardNotifications != null) {
        }

        return root;
    }

    /**
     * Helper method for clean fragment navigation.
     */
    private void openFragment(Fragment fragment) {
        FragmentTransaction ft = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction();
        // ❌ old
        // ft.replace(R.id.fragment_container, fragment);

        // ✅ new: replace the same container that holds AdminDashboardFragment
        ft.replace(R.id.dashboard_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }
}

