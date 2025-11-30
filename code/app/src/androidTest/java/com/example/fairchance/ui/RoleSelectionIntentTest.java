package com.example.fairchance.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.intent.Intents;

import com.example.fairchance.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RoleSelectionIntentTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void clickingEntrantCard_launchesAuthActivity() {
        ActivityScenario.launch(RoleSelectionActivity.class);
        onView(withId(R.id.cardEntrant)).perform(click());
        intended(hasComponent(AuthActivity.class.getName()));
    }

    @Test
    public void clickingOrganizerCard_launchesAuthActivity() {
        ActivityScenario.launch(RoleSelectionActivity.class);
        onView(withId(R.id.cardOrganizer)).perform(click());
        intended(hasComponent(AuthActivity.class.getName()));
    }
    @Test
    public void clickingAdminCard_launchesAuthActivity() {
        ActivityScenario.launch(RoleSelectionActivity.class);
        onView(withId(R.id.cardAdmin)).perform(click());
        intended(hasComponent(AuthActivity.class.getName()));
    }

}
