package com.example.fairchance;

/**
 * Manages the status state machine for an invitation (Pending, Accepted, Declined).
 */
public class InvitationManager {

    public enum Status {
        PENDING,
        ACCEPTED,
        DECLINED
    }

    private Status status = Status.PENDING;

    public Status getStatus() {
        return status;
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public boolean isAccepted() {
        return status == Status.ACCEPTED;
    }

    public boolean isDeclined() {
        return status == Status.DECLINED;
    }

    public void accept() {
        if (status == Status.PENDING) {
            status = Status.ACCEPTED;
        }
    }

    public void decline() {
        if (status == Status.PENDING) {
            status = Status.DECLINED;
        }
    }
}
