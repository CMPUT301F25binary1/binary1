package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairchance.R;
import com.example.fairchance.models.AdminUserItem;
import com.example.fairchance.ui.adapters.AdminUserAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminProfileManagementFragment extends Fragment
        implements AdminUserAdapter.OnUserClickListener {

    private RecyclerView rvUsers;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private EditText etSearch;

    private AdminUserAdapter adapter;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnBack = view.findViewById(R.id.btnBack);
        rvUsers = view.findViewById(R.id.rvAdminUsers);
        progressBar = view.findViewById(R.id.progressBarUsers);
        tvEmpty = view.findViewById(R.id.tvEmptyUsers);
        etSearch = view.findViewById(R.id.etSearchUsers);

        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminUserAdapter(this);
        rvUsers.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadUsers();
    }

    private void loadUsers() {
        setLoading(true);
        db.collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    setLoading(false);
                    List<AdminUserItem> list = new ArrayList<>();
                    for (DocumentSnapshot doc : query) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String role = doc.getString("role");
                        // adjust the field name if your timestamp is different
                        com.google.firebase.Timestamp createdAt =
                                doc.getTimestamp("timeCreated");

                        list.add(new AdminUserItem(id, name, email, role, createdAt));
                    }

                    if (list.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvUsers.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvUsers.setVisibility(View.VISIBLE);
                    }

                    adapter.submitList(list);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvUsers.setVisibility(View.GONE);
                    Toast.makeText(requireContext(),
                            "Error loading users: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvUsers.setEnabled(!loading);
    }

    @Override
    public void onUserClick(AdminUserItem user) {
        // Open a simple details fragment
        Fragment details = AdminUserDetailsFragment.newInstance(user.getId());
        FragmentTransaction ft = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction();
        ft.replace(R.id.fragment_container, details);
        ft.addToBackStack(null);
        ft.commit();
    }
}
