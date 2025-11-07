package com.example.fairchance.ui;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fairchance.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AuthActivityTest {
    @Rule
    public ActivityScenarioRule<AuthActivity> rule =
            new ActivityScenarioRule<>(new Intent(
                    ApplicationProvider.getApplicationContext(),
                    AuthActivity.class
            ).putExtra("ROLE", "organizer"));

    @Test
    public void testAuthButtonIsVisible() {
        onView(withId(R.id.auth_action_button)).check(matches(isDisplayed()));
    }

    @Test
    public void testToggleButtonClickable() {
        onView(withId(R.id.auth_toggle_button)).perform(click());
    }
}

