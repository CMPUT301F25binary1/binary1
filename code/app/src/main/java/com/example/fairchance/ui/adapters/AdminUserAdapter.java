package com.example.fairchance.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Filter;
import android.widget.Filterable;

import com.example.fairchance.AuthRepository.AdminUserSummary;
import com.example.fairchance.R;

import java.util.ArrayList;
import java.util.List;

public class AdminUserAdapter
        extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder>
        implements Filterable {

    public interface OnDeleteClickListener {
        void onDeleteClick(AdminUserSummary user);
    }

    private final OnDeleteClickListener deleteClickListener;
    private final List<AdminUserSummary> users = new ArrayList<>();
    private final List<AdminUserSummary> allUsers = new ArrayList<>();

    public AdminUserAdapter(List<AdminUserSummary> initial,
                            OnDeleteClickListener deleteClickListener) {
        this.deleteClickListener = deleteClickListener;
        setUsers(initial);
    }

    public void setUsers(List<AdminUserSummary> list) {
        users.clear();
        allUsers.clear();
        if (list != null) {
            allUsers.addAll(list);
            users.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void removeUser(AdminUserSummary user) {
        allUsers.remove(user);
        users.remove(user);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        AdminUserSummary u = users.get(position);
        holder.tvName.setText(u.getName());
        holder.tvEmail.setText(u.getEmail());
        holder.tvRole.setText(u.getRole() != null ? u.getRole() : "Unknown");
        holder.btnRemove.setOnClickListener(v -> deleteClickListener.onDeleteClick(u));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    private final Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<AdminUserSummary> filtered = new ArrayList<>();
            String query = constraint == null ? "" :
                    constraint.toString().toLowerCase().trim();

            if (query.isEmpty()) {
                filtered.addAll(allUsers);
            } else {
                for (AdminUserSummary u : allUsers) {
                    if ((u.getName() != null && u.getName().toLowerCase().contains(query)) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(query))) {
                        filtered.add(u);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filtered;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            users.clear();
            //noinspection unchecked
            users.addAll((List<AdminUserSummary>) results.values);
            notifyDataSetChanged();
        }
    };

    static class UserViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvEmail, tvRole;
        Button btnRemove;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            tvRole = itemView.findViewById(R.id.tvUserRole);
            btnRemove = itemView.findViewById(R.id.btnRemoveUser);
        }
    }
}
