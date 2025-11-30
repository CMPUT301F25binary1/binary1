package com.example.fairchance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class DataInsertion_Update_DeletionTest {

    private FirebaseFirestore db;
    private String collectionName = "test_crud";
    private String docId = "crud_doc_1";

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
    }

    @Test
    public void insertUpdateDelete_roundTrip() throws Exception {
        DocumentReference ref = db.collection(collectionName).document(docId);

        Map<String, Object> data = new HashMap<>();
        data.put("field", "value1");
        Tasks.await(ref.set(data), 10, TimeUnit.SECONDS);

        DocumentSnapshot snapshot = Tasks.await(ref.get(), 10, TimeUnit.SECONDS);
        assertTrue(snapshot.exists());
        assertEquals("value1", snapshot.getString("field"));

        Map<String, Object> update = new HashMap<>();
        update.put("field", "value2");
        Tasks.await(ref.update(update), 10, TimeUnit.SECONDS);

        snapshot = Tasks.await(ref.get(), 10, TimeUnit.SECONDS);
        assertTrue(snapshot.exists());
        assertEquals("value2", snapshot.getString("field"));

        Tasks.await(ref.delete(), 10, TimeUnit.SECONDS);

        snapshot = Tasks.await(ref.get(), 10, TimeUnit.SECONDS);
        assertFalse(snapshot.exists());
    }
}
