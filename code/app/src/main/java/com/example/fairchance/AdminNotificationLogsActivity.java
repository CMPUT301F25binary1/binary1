package com.example.fairchance;
import android.view.View;


import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairchance.models.NotificationLog;
import com.example.fairchance.ui.adapters.NotificationLogAdapter;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AdminNotificationLogsActivity extends AppCompatActivity
        implements NotificationLogAdapter.OnLogClickListener {

    private NotificationLogRepository logRepository;
    private NotificationLogAdapter adapter;

    private final List<NotificationLog> allLogs = new ArrayList<>();

    private TextView tvStartDate, tvEndDate, tvEmptyState;
    private EditText etFilterEvent;
    private Date startDateFilter = null;
    private Date endDateFilter = null;

    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_notification);

        logRepository = new NotificationLogRepository();

        Button backButton = findViewById(R.id.button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        RecyclerView rvLogs = findViewById(R.id.rvNotificationLogs);
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationLogAdapter(this);
        rvLogs.setAdapter(adapter);

        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        etFilterEvent = findViewById(R.id.etFilterEvent);

        Button btnClearFilters = findViewById(R.id.btnClearFilters);

        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        tvEndDate.setOnClickListener(v -> showDatePicker(false));

        btnClearFilters.setOnClickListener(v -> {
            startDateFilter = null;
            endDateFilter = null;
            tvStartDate.setText("Start date");
            tvEndDate.setText("End date");
            etFilterEvent.setText("");
            applyFilters();
        });

        etFilterEvent.addTextChangedListener(new SimpleTextWatcher(this::applyFilters));

        loadLogs();
    }

    private void loadLogs() {
        logRepository.fetchAllLogs(new NotificationLogRepository.LogListCallback() {
            @Override
            public void onSuccess(List<NotificationLog> logs) {
                allLogs.clear();
                allLogs.addAll(logs);
                applyFilters();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AdminNotificationLogsActivity.this,
                        "Failed to load logs: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showDatePicker(boolean isStart) {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth, 0, 0, 0);
                    Date chosen = calendar.getTime();
                    if (isStart) {
                        startDateFilter = chosen;
                        tvStartDate.setText(dateFormat.format(chosen));
                    } else {
                        calendar.set(year, month, dayOfMonth, 23, 59, 59);
                        endDateFilter = calendar.getTime();
                        tvEndDate.setText(dateFormat.format(chosen));
                    }
                    applyFilters();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void applyFilters() {
        String eventQuery = etFilterEvent.getText() != null
                ? etFilterEvent.getText().toString().trim().toLowerCase()
                : "";

        List<NotificationLog> filtered = new ArrayList<>();
        for (NotificationLog log : allLogs) {
            Date ts = log.getTimestamp();
            if (startDateFilter != null && (ts == null || ts.before(startDateFilter))) continue;
            if (endDateFilter != null && (ts == null || ts.after(endDateFilter))) continue;

            if (!eventQuery.isEmpty()) {
                String eventName = log.getEventName() != null
                        ? log.getEventName().toLowerCase()
                        : "";
                if (!eventName.contains(eventQuery)) continue;
            }

            filtered.add(log);
        }

        adapter.setLogs(filtered);
        tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onLogClick(NotificationLog log) {
        NotificationLogDetailsActivity.start(this, log);
    }
}
