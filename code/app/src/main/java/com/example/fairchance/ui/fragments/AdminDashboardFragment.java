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
 * A simple fragment that displays the main dashboard for the 'Admin' role.
 * This is loaded into MainActivity when an Admin logs in.
 */
public class AdminDashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Use the layout file that actually has cardEventManagement
        return inflater.inflate(R.layout.admin_main_dashboard, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CardView cardEventManagement = view.findViewById(R.id.cardEventManagement);
        cardEventManagement.setOnClickListener(v -> {
            FragmentTransaction ft = requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction();
            ft.replace(R.id.fragment_container, new AdminEventManagementFragment());
            ft.addToBackStack(null);
            ft.commit();
        });
    }
}
