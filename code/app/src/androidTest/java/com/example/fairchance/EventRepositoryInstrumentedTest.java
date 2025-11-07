package com.example.fairchance;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class EventRepositoryInstrumentedTest {

    private EventRepository repository;

    @Before
    public void setup() {
        repository = new EventRepository();
    }

    @Test
    public void testEventRepositoryInitialization() {
        assertNotNull("EventRepository should not be null after initialization", repository);
    }

    @Test
    public void testEventRepositorySafeToUse() {
        try {
            // Just verifies it doesn't crash when referenced
            assertTrue("Repository instance is safe to use", repository != null);
        } catch (Exception e) {
            throw new AssertionError("EventRepository threw an exception during safe use test", e);
        }
    }
}
