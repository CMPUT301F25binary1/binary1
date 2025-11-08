package com.example.fairchance.ui.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.Event;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class EventDetailsFragment extends Fragment {

    private TextView tvEventName, tvEventDescription, tvEventTimeLocation, tvRegistrationDates, tvRegistrationTimes;
    private ImageView ivEventPoster, ivQRCode;
    private Button btnWaitingList, btnChosen, btnCancelled, btnFinal;

    // Optional views for the two user stories
    private TextView tvGeolocationRequiredLabel, tvGeolocationRequiredValue;
    private Button btnUpdatePoster, btnEditEvent;

    private String eventId;
    private EventRepository repository;
    private Event loadedEvent;

    // Image picker for Update Poster
    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && loadedEvent != null) {
                    repository.uploadPosterAndUpdate(
                            loadedEvent.getEventId(),
                            uri,
                            new EventRepository.EventTaskCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(getContext(), "Poster updated.", Toast.LENGTH_SHORT).show();
                                    loadEventDetails(loadedEvent.getEventId());
                                }
                                @Override
                                public void onError(String message) {
                                    Toast.makeText(getContext(), "Upload failed: " + message, Toast.LENGTH_LONG).show();
                                }
                            }
                    );
                }
            });

    public EventDetailsFragment() { }

    public static EventDetailsFragment newInstance(String eventId) {
        EventDetailsFragment fragment = new EventDetailsFragment();
        Bundle args = new Bundle();
        args.putString("EVENT_ID", eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_event_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();

        ivEventPoster = view.findViewById(R.id.ivEventPoster);
        ivQRCode = view.findViewById(R.id.ivQRCode);
        tvEventName = view.findViewById(R.id.tvEventName);
        tvEventDescription = view.findViewById(R.id.tvEventDescription);
        tvEventTimeLocation = view.findViewById(R.id.tvEventTimeLocation);
        tvRegistrationDates = view.findViewById(R.id.tvRegistrationDates);
        tvRegistrationTimes = view.findViewById(R.id.tvRegistrationTimes);

        btnWaitingList = view.findViewById(R.id.btnEntrantsWaitingList);
        btnChosen = view.findViewById(R.id.btnChosenEntrants);
        btnCancelled = view.findViewById(R.id.btnCancelledEntrants);
        btnFinal = view.findViewById(R.id.btnFinalEntrants);

        // Optional new views if added in layout
        tvGeolocationRequiredLabel = view.findViewById(R.id.tvGeolocationRequiredLabel);
        tvGeolocationRequiredValue = view.findViewById(R.id.tvGeolocationRequiredValue);
        btnUpdatePoster = view.findViewById(R.id.btnUpdatePoster);
        btnEditEvent = view.findViewById(R.id.btnEditEvent);

        if (btnUpdatePoster != null) {
            btnUpdatePoster.setOnClickListener(v -> {
                if (loadedEvent == null) return;
                pickImage.launch("image/*");
            });
        }

        if (btnEditEvent != null) {
            btnEditEvent.setOnClickListener(v1 -> {
                if (eventId != null) {
                    FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment_container, UpdateEventFragment.newInstance(eventId));
                    ft.addToBackStack(null);
                    ft.commit();
                }
            });
        }

        if (getArguments() != null) {
            eventId = getArguments().getString("EVENT_ID");
            loadEventDetails(eventId);
        }

        // Navigation buttons
        btnWaitingList.setOnClickListener(v -> openFragment(new EntrantsWaitingListFragment()));
        btnChosen.setOnClickListener(v -> openFragment(new ChosenEntrantsFragment()));
        btnCancelled.setOnClickListener(v -> openFragment(new CancelledEntrantsFragment()));
        btnFinal.setOnClickListener(v -> openFragment(new FinalEntrantsFragment()));
    }

    private void loadEventDetails(String eventId) {
        repository.getEvent(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                loadedEvent = event;
                populateUI(event);
            }
            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "Error loading event: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void populateUI(Event event) {
        tvEventName.setText(event.getName());
        tvEventDescription.setText(event.getDescription());
        tvEventTimeLocation.setText(event.getLocation());

        SimpleDateFormat sdfDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        if (event.getRegistrationStart() != null && event.getRegistrationEnd() != null) {
            tvRegistrationDates.setText(
                    sdfDate.format(event.getRegistrationStart()) + " - " +
                            sdfDate.format(event.getRegistrationEnd()));
            tvRegistrationTimes.setText(
                    sdfTime.format(event.getRegistrationStart()) + " - " +
                            sdfTime.format(event.getRegistrationEnd()));
        }

        Glide.with(requireContext())
                .load(event.getPosterImageUrl())
                .placeholder(R.drawable.fairchance_logo_with_words___transparent)
                .into(ivEventPoster);

        if (tvGeolocationRequiredValue != null) {
            tvGeolocationRequiredValue.setText(event.isGeolocationRequired() ? "Yes" : "No");
        }

        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(event.getEventId(), BarcodeFormat.QR_CODE, 400, 400);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.createBitmap(matrix);
            ivQRCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openFragment(Fragment fragment) {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
