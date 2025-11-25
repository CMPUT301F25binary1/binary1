package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairchance.AuthRepository;
import com.example.fairchance.R;
import com.example.fairchance.ui.adapters.AdminUserAdapter;
import com.example.fairchance.AuthRepository.AdminUserSummary;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AdminProfileManagementFragment extends Fragment {

    private RecyclerView rvUsers;
    private ProgressBar progressBar;
    private TextInputEditText etSearch;
    private ImageButton btnBack;

    private AdminUserAdapter adapter;
    private AuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_profile_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository();

        rvUsers = view.findViewById(R.id.rvAdminUsers);
        progressBar = view.findViewById(R.id.profile_mgmt_progress);
        etSearch = view.findViewById(R.id.etSearchUsers);
        btnBack = view.findViewById(R.id.btnBackProfiles);

        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminUserAdapter(new ArrayList<>(), this::confirmDeleteUser);
        rvUsers.setAdapter(adapter);

        btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack()
        );


        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        loadUsers();
    }

    private void setLoading(boolean loading) {
        if (progressBar == null) return;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        rvUsers.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
    }

    private void loadUsers() {
        setLoading(true);
        authRepository.fetchAllUsers(new AuthRepository.AdminUsersCallback() {
            @Override
            public void onSuccess(List<AdminUserSummary> users) {
                setLoading(false);
                adapter.setUsers(users);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(getContext(), "Failed to load users: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDeleteUser(AdminUserSummary user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Profile")
                .setMessage("You are about to delete the profile for:\n\n" +
                        user.getName() + " (" + user.getEmail() + ")\n\n" +
                        "This will permanently remove their data. Continue?")
                .setPositiveButton("Delete", (dialog, which) -> deleteUser(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser(AdminUserSummary user) {
        setLoading(true);
        authRepository.deleteUserProfileById(user.getUserId(), new AuthRepository.TaskCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(getContext(), "Profile removed.", Toast.LENGTH_SHORT).show();
                adapter.removeUser(user);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(getContext(), "Delete failed: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
