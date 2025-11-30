package com.example.fairchance.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fairchance.R;
import com.example.fairchance.ui.RoleSelectionActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RoleSelectionUiTest {

    @Rule
    public ActivityScenarioRule<RoleSelectionActivity> activityRule =
            new ActivityScenarioRule<>(RoleSelectionActivity.class);

    @Test
    public void roleCardsAreVisible() {
        onView(withId(R.id.cardEntrant)).check(matches(isDisplayed()));
        onView(withId(R.id.cardOrganizer)).check(matches(isDisplayed()));
        onView(withId(R.id.cardAdmin)).check(matches(isDisplayed()));
    }

    @Test
    public void clickingEntrantOpensAuth() {
        onView(withId(R.id.cardEntrant)).perform(click());
        // Replace with an actual view inside AuthActivity
        // onView(withId(R.id.auth_root)).check(matches(isDisplayed()));
    }
}
