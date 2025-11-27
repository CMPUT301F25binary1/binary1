package com.example.fairchance.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.os.SystemClock;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fairchance.MainActivity;
import com.example.fairchance.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test verifies that MainActivity launches successfully without crashing.
 * We skip Firebase checks, since no user is logged in during instrumented tests.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> rule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testMainActivityLaunchesSuccessfully() {
        // Give time for Firebase callbacks and layout inflation
        SystemClock.sleep(2000);

        // Just verify the activity starts by checking that the main root view exists
        onView(withId(android.R.id.content)).check(matches(isDisplayed()));
    }
}
