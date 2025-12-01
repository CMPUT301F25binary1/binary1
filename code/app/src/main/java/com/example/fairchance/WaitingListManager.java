package com.example.fairchance;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for managing a list of user IDs in a waiting list context.
 * Handles joining, leaving, and drawing replacements.
 */
public class WaitingListManager {

    private final List<String> waitingList;

    public WaitingListManager() {
        this.waitingList = new ArrayList<>();
    }

    public WaitingListManager(List<String> initialList) {
        this.waitingList = new ArrayList<>(initialList);
    }

    public List<String> getWaitingList() {
        return waitingList;
    }

    public void join(String userId) {
        if (!waitingList.contains(userId)) {
            waitingList.add(userId);
        }
    }

    public void leave(String userId) {
        waitingList.remove(userId);
    }

    public int count() {
        return waitingList.size();
    }

    public String drawReplacement(List<String> alreadySelected) {
        for (String id : waitingList) {
            if (!alreadySelected.contains(id)) {
                return id;
            }
        }
        return null;
    }
}
