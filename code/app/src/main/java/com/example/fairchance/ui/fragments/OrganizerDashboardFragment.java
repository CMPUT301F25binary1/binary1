package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


import com.example.fairchance.R;

public class OrganizerDashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.organizer_dashboard, container, false);

        Button btnCreateEvent = view.findViewById(R.id.btnCreateNewEvent);
        Button btnCurrentEvents = view.findViewById(R.id.btnCurrentEvents);

        // open create new event screen
        btnCreateEvent.setOnClickListener(v -> openFragment(new CreateNewEventFragment()));

        // open ongoing events screen
        btnCurrentEvents.setOnClickListener(v -> openFragment(new OngoingEventsFragment()));

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