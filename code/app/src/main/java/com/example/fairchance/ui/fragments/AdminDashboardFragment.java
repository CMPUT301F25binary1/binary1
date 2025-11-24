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

import com.example.fairchance.R;
import com.example.fairchance.ui.OrganizerManagementActivity;

public class AdminDashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_main_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CardView btnManageOrganizers = view.findViewById(R.id.btnManageOrganizers);
        btnManageOrganizers.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), OrganizerManagementActivity.class);
            startActivity(intent);
        });
    }
}
