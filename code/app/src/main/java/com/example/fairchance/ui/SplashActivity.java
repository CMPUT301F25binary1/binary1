package com.example.fairchance.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fairchance.MainActivity;
import com.example.fairchance.R;
import com.example.fairchance.AuthRepository;
import com.google.firebase.auth.FirebaseUser;

/**
 * The main entry point (launcher) activity for the app.
 * It checks the user's current authentication state and routes them
 * to the appropriate activity (MainActivity or RoleSelectionActivity).
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_SCREEN_DELAY = 1500L;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        authRepository = new AuthRepository();

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = authRepository.getCurrentUser();
            if (currentUser != null) {
                goToMainApp();
            } else {
                goToRoleSelection();
            }
        }, SPLASH_SCREEN_DELAY);
    }

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