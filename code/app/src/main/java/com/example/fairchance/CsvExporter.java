package com.example.fairchance;

import java.util.List;

/**
 * Utility class for generating CSV strings from data models.
 * Used for exporting lists like final entrants.
 */
public class CsvExporter {

    public static class EntrantRow {
        private final String name;
        private final String email;
        private final String status;

        public EntrantRow(String name, String email, String status) {
            this.name = name;
            this.email = email;
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getStatus() {
            return status;
        }
    }

    /**
     * Generate a CSV string with header:
     * name,email,status
     */
    public static String generateCsv(List<EntrantRow> entrants) {
        StringBuilder sb = new StringBuilder();
        sb.append("name,email,status\n");

        for (EntrantRow row : entrants) {
            sb.append(row.getName())
                    .append(",")
                    .append(row.getEmail())
                    .append(",")
                    .append(row.getStatus())
                    .append("\n");
        }

        return sb.toString();
    }
}
