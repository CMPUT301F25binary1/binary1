package com.example.fairchance;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.NotificationManager;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Smoke test for MyFirebaseMessagingService.
 * Does NOT depend on RemoteMessage or Firebase internals.
 * Verifies service and context are accessible.
 */
@RunWith(AndroidJUnit4.class)
public class MyFirebaseMessagingServiceTest {

    private Context context;
    private MyFirebaseMessagingService service;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        service = new MyFirebaseMessagingService();
    }

    @Test
    public void context_isNotNull() {
        assertNotNull("Context should not be null", context);
        assertTrue("Package name should not be empty", !context.getPackageName().isEmpty());
    }

    @Test
    public void service_instanceCreatedSuccessfully() {
        assertNotNull("Firebase messaging service should initialize", service);
    }

    @Test
    public void notificationManager_isAvailable() {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        assertNotNull("NotificationManager should be available", manager);
    }
}

