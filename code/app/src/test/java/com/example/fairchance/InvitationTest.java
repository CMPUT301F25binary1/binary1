package com.example.fairchance;

import org.junit.Before;
import org.junit.Test;
import java.util.Date;
import static org.junit.Assert.*;

import com.example.fairchance.models.Invitation;

public class InvitationTest {
    private Invitation invitation;
    private Date date;

    @Before
    public void setUp() {
        invitation = new Invitation();
        date = new Date();
        invitation.setEventName("Charity Gala");
        invitation.setEventDate(date);
        invitation.setStatus("Selected");
        invitation.setEventId("event123");
    }

    @Test
    public void testInvitationGettersAndSetters() {
        assertEquals("Charity Gala", invitation.getEventName());
        assertEquals(date, invitation.getEventDate());
        assertEquals("Selected", invitation.getStatus());
        assertEquals("event123", invitation.getEventId());
    }
}
