package com.example.fairchance.ui.fragments;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.Event;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CreateNewEventFragment extends Fragment {

    // Views (IDs match your XML)
    private ImageView ivEventPoster;
    private TextInputEditText etEventName, etEventDescription, etEventTimeLocation;
    private TextInputEditText etRegistrationDates, etRegistrationTimes;
    private CheckBox cbGeolocation, cbWaitlistLimit;
    private Button btnGenerateQRCode, btnCreateEvent;

    // State
    private final Calendar regStartCal = Calendar.getInstance();
    private final Calendar regEndCal   = Calendar.getInstance();
    private Date registrationStart, registrationEnd;
    private Long waitlistLimit = null;
    private Uri posterUri = null;
    private String uploadedPosterUrl = null;

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private EventRepository repo;

    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    posterUri = uri;
                    ivEventPoster.setImageURI(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.create_new_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = new EventRepository();

        // Bind
        ivEventPoster         = view.findViewById(R.id.ivEventPoster);
        etEventName           = view.findViewById(R.id.etEventName);
        etEventDescription    = view.findViewById(R.id.etEventDescription);
        etEventTimeLocation   = view.findViewById(R.id.etEventTimeLocation);
        etRegistrationDates   = view.findViewById(R.id.etRegistrationDates);
        etRegistrationTimes   = view.findViewById(R.id.etRegistrationTimes);
        cbGeolocation         = view.findViewById(R.id.cbGeolocation);
        cbWaitlistLimit       = view.findViewById(R.id.cbWaitlistLimit);
        btnGenerateQRCode     = view.findViewById(R.id.btnGenerateQRCode);
        btnCreateEvent        = view.findViewById(R.id.btnCreateEvent);

        // Poster picker
        ivEventPoster.setOnClickListener(v -> pickImage.launch("image/*"));

        // Registration date range picker (MaterialDatePicker)
        etRegistrationDates.setKeyListener(null); // make it non-typing, click-only
        etRegistrationDates.setOnClickListener(v -> showDateRangePicker());

        // Registration start/end time picker
        etRegistrationTimes.setKeyListener(null);
        etRegistrationTimes.setOnClickListener(v -> showStartEndTimePickers());

        // Waitlist limit dialog when toggled on
        cbWaitlistLimit.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) showWaitlistLimitDialog();
            else waitlistLimit = null;
        });

        // QR – you can hook real QR after event is created (has ID)
        btnGenerateQRCode.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "QR will be generated on the event details screen after creation.",
                        Toast.LENGTH_SHORT).show());

        // Create event
        btnCreateEvent.setOnClickListener(v -> onCreateEventClicked());
    }

    // ---------- UI helpers ----------

    private void showDateRangePicker() {
        MaterialDatePicker<Long> startPicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Pick registration START date")
                .build();

        startPicker.addOnPositiveButtonClickListener(startUtc -> {
            regStartCal.setTimeInMillis(startUtc);
            MaterialDatePicker<Long> endPicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Pick registration END date")
                    .build();
            endPicker.addOnPositiveButtonClickListener(endUtc -> {
                regEndCal.setTimeInMillis(endUtc);
                // Display in the field
                String display = dateFmt.format(regStartCal.getTime()) + " → " + dateFmt.format(regEndCal.getTime());
                etRegistrationDates.setText(display);
            });
            endPicker.show(getParentFragmentManager(), "endDate");
        });

        startPicker.show(getParentFragmentManager(), "startDate");
    }

    private void showStartEndTimePickers() {
        // START time
        MaterialTimePicker start = new MaterialTimePicker.Builder()
                .setTitleText("Pick registration START time")
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(9).setMinute(0)
                .build();

        start.addOnPositiveButtonClickListener(v -> {
            regStartCal.set(Calendar.HOUR_OF_DAY, start.getHour());
            regStartCal.set(Calendar.MINUTE, start.getMinute());
            regStartCal.set(Calendar.SECOND, 0);
            // END time
            MaterialTimePicker end = new MaterialTimePicker.Builder()
                    .setTitleText("Pick registration END time")
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(17).setMinute(0)
                    .build();
            end.addOnPositiveButtonClickListener(v2 -> {
                regEndCal.set(Calendar.HOUR_OF_DAY, end.getHour());
                regEndCal.set(Calendar.MINUTE, end.getMinute());
                regEndCal.set(Calendar.SECOND, 0);
                String display = timeFmt.format(regStartCal.getTime()) + " → " + timeFmt.format(regEndCal.getTime());
                etRegistrationTimes.setText(display);
            });
            end.show(getParentFragmentManager(), "endTime");
        });

        start.show(getParentFragmentManager(), "startTime");
    }

    private void showWaitlistLimitDialog() {
        Context ctx = requireContext();
        NumberPicker picker = new NumberPicker(ctx);
        picker.setMinValue(1);
        picker.setMaxValue(10000);
        picker.setValue(waitlistLimit == null ? 100 : waitlistLimit.intValue());

        new AlertDialog.Builder(ctx)
                .setTitle("Set waiting list limit")
                .setView(picker)
                .setPositiveButton("Set", (d, w) -> waitlistLimit = (long) picker.getValue())
                .setNegativeButton("Cancel", (d, w) -> {
                    cbWaitlistLimit.setChecked(false);
                    waitlistLimit = null;
                })
                .show();
    }

    // ---------- Create Event flow ----------

    private void onCreateEventClicked() {
        String name = s(etEventName);
        String desc = s(etEventDescription);
        String timeLocation = s(etEventTimeLocation);
        boolean geoRequired = cbGeolocation.isChecked();

        if (TextUtils.isEmpty(name)) {
            etEventName.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(desc)) {
            etEventDescription.setError("Required");
            return;
        }
        if (etRegistrationDates.getText() == null || etRegistrationTimes.getText() == null ||
                TextUtils.isEmpty(etRegistrationDates.getText().toString()) ||
                TextUtils.isEmpty(etRegistrationTimes.getText().toString())) {
            Toast.makeText(requireContext(), "Pick registration start & end date/time.", Toast.LENGTH_SHORT).show();
            return;
        }

        registrationStart = regStartCal.getTime();
        registrationEnd   = regEndCal.getTime();
        if (!registrationEnd.after(registrationStart)) {
            Toast.makeText(requireContext(), "End must be after start.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build Event model
        Event event = new Event();
        event.setOrganizerId(FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "unknown");
        event.setName(name);
        event.setDescription(desc);
        // You used a single field for "time & location" – store as location for now
        event.setLocation(timeLocation);
        event.setRegistrationStart(registrationStart);
        event.setRegistrationEnd(registrationEnd);
        event.setGeolocationRequired(geoRequired);
        if (cbWaitlistLimit.isChecked() && waitlistLimit != null) {
            event.setWaitingListLimit(waitlistLimit);
        }

        // If poster selected, upload first; otherwise create immediately
        if (posterUri != null) {
            uploadPosterThenCreate(event);
        } else {
            createEvent(event);
        }
    }

    private void uploadPosterThenCreate(Event event) {
        try {
            String ext = getExtFromUri(posterUri);
            String fileName = "poster_" + System.currentTimeMillis() + (ext != null ? ("." + ext) : "");
            StorageReference ref = FirebaseStorage.getInstance()
                    .getReference("event_posters")
                    .child(FirebaseAuth.getInstance().getUid() == null ? "unknown" : FirebaseAuth.getInstance().getUid())
                    .child(fileName);

            ref.putFile(posterUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(uri -> {
                        uploadedPosterUrl = uri.toString();
                        event.setPosterImageUrl(uploadedPosterUrl);
                        createEvent(event);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Poster upload failed, creating event without image.", Toast.LENGTH_LONG).show();
                        createEvent(event);
                    });

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Poster upload error, creating event without image.", Toast.LENGTH_LONG).show();
            createEvent(event);
        }
    }

    private void createEvent(Event event) {
        btnCreateEvent.setEnabled(false);
        repo.createEvent(event, new EventRepository.EventTaskCallback() {
            @Override public void onSuccess() {
                btnCreateEvent.setEnabled(true);
                Toast.makeText(requireContext(), "Event created!", Toast.LENGTH_SHORT).show();
                // Optional: clear form
                clearForm();
            }
            @Override public void onError(String message) {
                btnCreateEvent.setEnabled(true);
                Toast.makeText(requireContext(), "Create failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearForm() {
        posterUri = null;
        uploadedPosterUrl = null;
        ivEventPoster.setImageResource(android.R.drawable.ic_menu_gallery);
        etEventName.setText("");
        etEventDescription.setText("");
        etEventTimeLocation.setText("");
        etRegistrationDates.setText("");
        etRegistrationTimes.setText("");
        cbGeolocation.setChecked(true);
        cbWaitlistLimit.setChecked(false);
        waitlistLimit = null;
        regStartCal.setTime(new Date());
        regEndCal.setTime(new Date());
    }

    // ---------- Utils ----------

    private static String s(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private String getExtFromUri(Uri uri) {
        try {
            ContentResolver cr = requireContext().getContentResolver();
            String type = cr.getType(uri);
            if (type != null) return MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
            String name = queryDisplayName(uri);
            int dot = name.lastIndexOf('.');
            return dot >= 0 ? name.substring(dot + 1) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String queryDisplayName(Uri uri) {
        try (android.database.Cursor c = requireContext().getContentResolver()
                .query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        }
        return "file";
    }
}