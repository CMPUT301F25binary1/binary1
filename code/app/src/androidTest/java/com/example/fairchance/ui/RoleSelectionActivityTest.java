package com.example.fairchance.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fairchance.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso UI test for RoleSelectionActivity.
 * Verifies that selecting each role card launches the correct AuthActivity intent.
 */
@RunWith(AndroidJUnit4.class)
public class RoleSelectionActivityTest {

    @Rule
    public ActivityScenarioRule<RoleSelectionActivity> rule =
            new ActivityScenarioRule<>(RoleSelectionActivity.class);

    @Before
    public void setUp() {
        Intents.init(); // Initialize Espresso Intents before tests
    }

    @After
    public void tearDown() {
        Intents.release(); // Clean up after tests
    }

    @Test
    public void testEntrantCardLaunchesAuthActivity() {
        onView(withId(R.id.cardEntrant)).perform(click());
        intended(hasComponent(AuthActivity.class.getName()));
    }

    @Test
    public void testOrganizerCardLaunchesAuthActivity() {
        onView(withId(R.id.cardOrganizer)).perform(click());
        intended(hasComponent(AuthActivity.class.getName()));
    }

    @Test
    public void testAdminCardLaunchesAuthActivity() {
        onView(withId(R.id.cardAdmin)).perform(click());
        intended(hasComponent(AuthActivity.class.getName()));
    }
}
