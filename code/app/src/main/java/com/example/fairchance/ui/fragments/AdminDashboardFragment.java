package com.example.fairchance.ui.fragments;

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

/**
 * Dashboard for Admin users.
 * Uses the admin_main_dashboard layout with stats + management cards.
 */
public class AdminDashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate the full admin dashboard UI
        View view = inflater.inflate(R.layout.admin_main_dashboard, container, false);

        // Notifications management card
        CardView cardNotifications = view.findViewById(R.id.card_notifications);
        if (cardNotifications != null) {
            cardNotifications.setOnClickListener(v -> openFragment(new AdminNotificationFragment()));
        }

        // You can wire more cards here later (Event Management, Profile, Images, etc.)

        return view;
    }

    private void openFragment(Fragment fragment) {
        FragmentTransaction transaction = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction();

        transaction.replace(R.id.dashboard_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
