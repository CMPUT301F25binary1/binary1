package com.example.fairchance;

import org.junit.Before;
import org.junit.Test;
import java.util.Date;
import static org.junit.Assert.*;

import com.example.fairchance.models.EventHistoryItem;

public class EventHistoryItemTest {
    private EventHistoryItem historyItem;
    private Date eventDate;

    @Before
    public void setUp() {
        historyItem = new EventHistoryItem();
        eventDate = new Date();
        historyItem.setEventName("Science Fair");
        historyItem.setEventDate(eventDate);
        historyItem.setStatus("Completed");
        historyItem.setEventId("E001");
    }

    @Test
    public void testHistoryItemFields() {
        assertEquals("Science Fair", historyItem.getEventName());
        assertEquals(eventDate, historyItem.getEventDate());
        assertEquals("Completed", historyItem.getStatus());
        assertEquals("E001", historyItem.getEventId());
    }
}
