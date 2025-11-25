package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fairchance.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdminUserDetailsFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";

    public static AdminUserDetailsFragment newInstance(String userId) {
        AdminUserDetailsFragment frag = new AdminUserDetailsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_USER_ID, userId);
        frag.setArguments(b);
        return frag;
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final SimpleDateFormat df =
            new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());

    private TextView tvName, tvEmail, tvRole, tvCreated;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_user_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName = view.findViewById(R.id.tvDetailName);
        tvEmail = view.findViewById(R.id.tvDetailEmail);
        tvRole = view.findViewById(R.id.tvDetailRole);
        tvCreated = view.findViewById(R.id.tvDetailCreated);
        progressBar = view.findViewById(R.id.progressBarUserDetail);

        String userId = getArguments() != null ? getArguments().getString(ARG_USER_ID) : null;
        if (userId == null) {
            Toast.makeText(requireContext(), "Missing user id", Toast.LENGTH_LONG).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return;
        }

        loadUser(userId);
    }

    private void loadUser(String id) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(id)
                .get()
                .addOnSuccessListener(this::bindUser)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Error loading user: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void bindUser(DocumentSnapshot doc) {
        progressBar.setVisibility(View.GONE);
        if (!doc.exists()) {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_LONG).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return;
        }

        tvName.setText(doc.getString("name"));
        tvEmail.setText(doc.getString("email"));
        tvRole.setText("Role: " + doc.getString("role"));

        com.google.firebase.Timestamp ts = doc.getTimestamp("timeCreated");
        if (ts != null) {
            tvCreated.setText("Registered: " + df.format(ts.toDate()));
        } else {
            tvCreated.setText("Registered: N/A");
        }
    }
}
