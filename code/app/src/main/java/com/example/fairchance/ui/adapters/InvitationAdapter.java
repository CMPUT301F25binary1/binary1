package com.example.fairchance.ui.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.Invitation;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * A RecyclerView.Adapter for displaying a list of pending Invitation objects.
 * This adapter is used in the InvitationsFragment and is responsible for
 * handling the "Accept" and "Decline" button clicks.
 */
public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.InvitationViewHolder> {

    private List<Invitation> invitationList;
    private EventRepository eventRepository;
    private Context context;

    /**
     * Constructs a new InvitationAdapter.
     *
     * @param context         The context of the calling fragment.
     * @param invitationList  The list of Invitation objects to display.
     * @param eventRepository An instance of EventRepository to call for actions.
     */
    public InvitationAdapter(Context context, List<Invitation> invitationList, EventRepository eventRepository) {
        this.context = context;
        this.invitationList = invitationList;
        this.eventRepository = eventRepository;
    }

    @NonNull
    @Override
    public InvitationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invitation_card, parent, false);
        return new InvitationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvitationViewHolder holder, int position) {
        Invitation item = invitationList.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return invitationList.size();
    }

    /**
     * ViewHolder class for an individual invitation card.
     */
    class InvitationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvEventTitle, tvEventDateTime, tvStatus;
        private Button btnAccept, btnDecline;
        private LinearLayout actionButtonsLayout;
        private ProgressBar progressBar;

        /**
         * Constructs a new ViewHolder.
         *
         * @param itemView The root view of the item_invitation_card layout.
         */
        InvitationViewHolder(View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDateTime = itemView.findViewById(R.id.tvEventDateTime);
            tvStatus = itemView.findViewById(R.id.tvInvitationStatus);
            btnAccept = itemView.findViewById(R.id.button_accept);
            btnDecline = itemView.findViewById(R.id.button_decline);
            actionButtonsLayout = itemView.findViewById(R.id.action_buttons_layout);
            progressBar = itemView.findViewById(R.id.action_progress_bar);
        }

        /**
         * Binds an Invitation object to the views in the ViewHolder.
         *
         * @param item     The Invitation object to display.
         * @param position The adapter position, used for removing the item.
         */
        void bind(Invitation item, int position) {
            tvEventTitle.setText(item.getEventName());
            tvStatus.setText("You've been selected!");

            if (item.getEventDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                tvEventDateTime.setText(sdf.format(item.getEventDate()));
            } else {
                tvEventDateTime.setText("Date not set");
            }

            btnAccept.setOnClickListener(v -> handleResponse(item, position, true));

            btnDecline.setOnClickListener(v -> showDeclineConfirmationDialog(item, position));
        }

        /**
         * Shows a dialog to confirm the user wants to decline the invitation.
         * Uses the custom layout dialog_confirm_action.xml.
         */
        private void showDeclineConfirmationDialog(Invitation item, int position) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            View dialogView = inflater.inflate(R.layout.dialog_confirm_action, null);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            TextView title = dialogView.findViewById(R.id.tvDialogTitle);
            TextView message = dialogView.findViewById(R.id.tvDialogMessage);
            Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);

            title.setText("Decline Invitation");
            message.setText("Are you sure you want to decline this invitation? This action cannot be undone.");

            btnConfirm.setText("Decline");
            btnConfirm.setBackgroundColor(Color.RED);

            btnConfirm.setOnClickListener(v -> {
                handleResponse(item, position, false);
                dialog.dismiss();
            });

            btnCancel.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        }

        /**
         * Handles the "Accept" or "Decline" click.
         * Calls the repository and updates the UI on success.
         *
         * @param item     The Invitation item.
         * @param position The adapter position.
         * @param accepted True if "Accept" was clicked, false for "Decline".
         */
        private void handleResponse(Invitation item, int position, boolean accepted) {
            setLoading(true);

            eventRepository.respondToInvitation(item.getEventId(), accepted, new EventRepository.EventTaskCallback() {
                @Override
                public void onSuccess() {
                    setLoading(false);
                    Toast.makeText(context, accepted ? "Invitation Accepted!" : "Invitation Declined.", Toast.LENGTH_SHORT).show();

                    invitationList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, invitationList.size());
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    Toast.makeText(context, "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * Shows/hides the progress bar and enables/disables buttons.
         *
         * @param isLoading True to show loading state, false otherwise.
         */
        private void setLoading(boolean isLoading) {
            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
                actionButtonsLayout.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.GONE);
                actionButtonsLayout.setVisibility(View.VISIBLE);
            }
        }
    }
}