package com.example.fairchance.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fairchance.R;
import com.example.fairchance.models.AdminImageItem;

import java.util.ArrayList;
import java.util.List;

public class AdminImageAdapter extends RecyclerView.Adapter<AdminImageAdapter.ImageViewHolder> {

    private List<AdminImageItem> images = new ArrayList<>();
    private final OnRemoveClickListener listener;

    /** Callback to the fragment */
    public interface OnRemoveClickListener {
        void onRemoveClicked(AdminImageItem item);
    }

    public AdminImageAdapter(OnRemoveClickListener listener) {
        this.listener = listener;
    }

    /** Replace entire list */
    public void submitList(List<AdminImageItem> newList) {
        this.images = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        AdminImageItem item = images.get(position);

        holder.tvTitle.setText(item.getTitle());

        Glide.with(holder.itemView.getContext())
                .load(item.getImageUrl())
                .into(holder.ivPoster);

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {

        ImageView ivPoster;
        TextView tvTitle;
        Button btnDelete;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivPoster);
            tvTitle = itemView.findViewById(R.id.tvEventName);
            btnDelete = itemView.findViewById(R.id.btnDeleteImage);
        }
    }
}
