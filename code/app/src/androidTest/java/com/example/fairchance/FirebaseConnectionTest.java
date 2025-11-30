package com.example.fairchance.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class FirebaseConnectionTest {

    private FirebaseFirestore db;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
    }

    @Test
    public void firestoreInstanceIsNotNull() {
        assertNotNull(db);
    }

    @Test
    public void canReadFromTestCollection() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> errorRef = new AtomicReference<>(null);

        db.collection("test_connection")
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        errorRef.set(task.getException());
                    }
                    latch.countDown();
                });

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        if (!completed) {
            throw new AssertionError("Timed out waiting for Firestore read");
        }
        assertNull(errorRef.get());
    }
}
