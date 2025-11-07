package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fairchance.R;

/**
 * Fragment that displays the comprehensive rules and guidelines for the FairChance
 * lottery system. Fulfills US 01.05.05.
 */
public class GuidelinesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guidelines, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the back button (which is currently named 'button2' in the XML)
        Button backButton = view.findViewById(R.id.button2);

        // Implement the back navigation logic
        backButton.setOnClickListener(v -> {
            if (getFragmentManager() != null) {
                // Pop the current fragment off the back stack
                getFragmentManager().popBackStack();
            }
        });
    }
}