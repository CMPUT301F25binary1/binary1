package com.example.fairchance.ui;

import android.Manifest; // <-- ADD THIS
import android.content.Intent;
import android.content.pm.PackageManager; // <-- ADD THIS
import android.os.Build; // <-- ADD THIS
import android.os.Bundle;
import android.os.Handler;
import android.util.Log; // <-- ADD THIS

import androidx.core.app.ActivityCompat; // <-- ADD THIS
import androidx.core.content.ContextCompat; // <-- ADD THIS
import androidx.appcompat.app.AppCompatActivity;

import com.example.fairchance.MainActivity;
import com.example.fairchance.R;
import com.example.fairchance.AuthRepository;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging; // <-- ADD THIS

/**
 * The main entry point (launcher) activity for the app.
 * It checks the user's current authentication state and routes them
 * to the appropriate activity (MainActivity or RoleSelectionActivity).
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_SCREEN_DELAY = 1500L;
    private static final String TAG = "SplashActivity"; // <-- ADD THIS
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        authRepository = new AuthRepository();

        // --- ADD THIS: REQUEST NOTIFICATION PERMISSION ---
        requestNotificationPermission();
        // --- END ADDED ---

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = authRepository.getCurrentUser();
            if (currentUser != null) {
                // --- ADD THIS ---
                // User is logged in, get/save token
                getAndSaveFcmToken();
                // --- END ADDED ---
                goToMainApp();
            } else {
                goToRoleSelection();
            }
        }, SPLASH_SCREEN_DELAY);
    }

    // --- START: ADDED NEW METHODS ---
    /**
     * Requests the POST_NOTIFICATIONS permission on Android 13+
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                // You can show a UI explaining why you need the permission here
                // For now, we'll just request it
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    /**
     * Gets the current FCM token and saves it to the user's profile in Firestore.
     */
    private void getAndSaveFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);

                    // Save it to Firestore
                    authRepository.saveFcmToken(token);
                });
    }
    // --- END: ADDED NEW METHODS ---

    private void goToMainApp() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToRoleSelection() {
        Intent intent = new Intent(SplashActivity.this, RoleSelectionActivity.class);
        startActivity(intent);
        finish();
    }
}