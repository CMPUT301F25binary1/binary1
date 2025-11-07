package com.example.fairchance.ui;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;

import android.os.SystemClock;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Stable test: verifies SplashActivity routes to RoleSelectionActivity (when no user is logged in).
 */
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest {

    @Rule
    public ActivityScenarioRule<SplashActivity> rule =
            new ActivityScenarioRule<>(SplashActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testSplashRoutesToRoleSelectionWhenLoggedOut() {
        // Splash has a delay before routing; give it a small buffer.
        SystemClock.sleep(1800);
        intended(hasComponent(RoleSelectionActivity.class.getName()));
    }
}
