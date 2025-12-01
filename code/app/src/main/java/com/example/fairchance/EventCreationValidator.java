package com.example.fairchance;

/**
 * Utility class to validate event creation parameters such as name, capacity,
 * and date ranges.
 */
public class EventCreationValidator {

    public static boolean isValid(
            String name,
            int capacity,
            long regStartMs,
            long regEndMs,
            double price
    ) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        if (capacity <= 0) {
            return false;
        }
        if (regStartMs >= regEndMs) {
            return false;
        }
        if (price < 0.0) {
            return false;
        }
        return true;
    }
}
