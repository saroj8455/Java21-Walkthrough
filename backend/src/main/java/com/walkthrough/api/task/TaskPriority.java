package com.walkthrough.api.task;

/** Task priority. Same pattern as {@link TaskStatus}. */
public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH;

    public String displayName() {
        return switch (this) {
            case LOW -> "Low";
            case MEDIUM -> "Medium";
            case HIGH -> "High";
        };
    }
}
