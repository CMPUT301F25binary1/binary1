package com.example.fairchance.ui;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.intent.Intents;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test for SplashActivity.
 * Ensures the splash screen correctly routes to RoleSelectionActivity when the user is logged out.
 */
@RunWith(AndroidJUnit4.class)
public class SplashActivityTest {

    @Rule
    public ActivityTestRule<SplashActivity> rule =
            new ActivityTestRule<>(SplashActivity.class);

    @Before
    public void setUp() {
        // Initialize Espresso Intents before the test
        Intents.init();
    }

    @After
    public void tearDown() {
        // Release Espresso Intents after the test
        Intents.release();
    }

    @Test
    public void testSplashRoutesToRoleSelectionWhenLoggedOut() throws InterruptedException {
        // Wait long enough for SplashActivity to complete its delay (adjust if needed)
        Thread.sleep(2500); // Use same duration as your splash delay in SplashActivity

        // Verify that SplashActivity launched RoleSelectionActivity
        intended(hasComponent(RoleSelectionActivity.class.getName()));
    }
}
