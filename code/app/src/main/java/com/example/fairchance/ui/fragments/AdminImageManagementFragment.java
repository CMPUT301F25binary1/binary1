package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fairchance.AdminImageRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.AdminImageItem;
import com.example.fairchance.ui.adapters.AdminImageAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Fragment for administrators to browse and remove uploaded images (posters).
 */
public class AdminImageManagementFragment extends Fragment
        implements AdminImageAdapter.OnImageActionListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;
    private CardView btnBack;

    private AdminImageAdapter adapter;
    private AdminImageRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_image_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new AdminImageRepository();

        recyclerView = view.findViewById(R.id.rvAdminImages);
        progressBar = view.findViewById(R.id.progressBarImages);
        emptyText = view.findViewById(R.id.tvEmptyImages);
        btnBack = view.findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new AdminImageAdapter(this);
        recyclerView.setAdapter(adapter);

        loadImages();
    }

    private void loadImages() {
        setLoading(true);
        repository.fetchAllImages(new AdminImageRepository.ImageListCallback() {
            @Override
            public void onSuccess(List<AdminImageItem> images) {
                setLoading(false);
                if (images == null || images.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.submitList(images);
                }
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        "Error loading images: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onPreview(AdminImageItem item) {
        showPreviewDialog(item);
    }

    @Override
    public void onRemoveClicked(AdminImageItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteImage(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteImage(AdminImageItem item) {
        setLoading(true);

        String adminId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "unknown-admin";

        repository.deleteImage(item, adminId, new AdminImageRepository.TaskCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(requireContext(), "Image deleted.", Toast.LENGTH_SHORT).show();
                loadImages();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(requireContext(),
                        "Error deleting: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showPreviewDialog(AdminImageItem item) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_admin_image_preview, null, false);

        ImageView ivPreview = dialogView.findViewById(R.id.ivPreviewImage);
        TextView tvTitle = dialogView.findViewById(R.id.tvPreviewTitle);
        TextView tvUploader = dialogView.findViewById(R.id.tvPreviewUploader);
        TextView tvUploadedAt = dialogView.findViewById(R.id.tvPreviewUploadedAt);
        Button btnRetain = dialogView.findViewById(R.id.btnRetainImage);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);

        Glide.with(this)
                .load(item.getImageUrl())
                .into(ivPreview);

        tvTitle.setText(item.getTitle());

        String uploader = item.getUploaderName();
        if (uploader == null || uploader.isEmpty()) {
            tvUploader.setText("Uploader: Unknown");
        } else {
            tvUploader.setText("Uploader: " + uploader);
        }

        if (item.getUploadedAt() != null) {
            Date date = item.getUploadedAt().toDate();
            DateFormat df = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT);
            tvUploadedAt.setText("Uploaded: " + df.format(date));
        } else {
            tvUploadedAt.setText("Uploaded: No date");
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnRetain.setOnClickListener(v -> {
            // Retain = do nothing, just dismiss
            Toast.makeText(requireContext(), "Image retained.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            onRemoveClicked(item);
        });

        dialog.show();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerView.setEnabled(!loading);
    }
}
