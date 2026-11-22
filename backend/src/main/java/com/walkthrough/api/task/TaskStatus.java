package com.walkthrough.api.task;

/**
 * Task lifecycle states. Stored in the DB as text (see {@code @Enumerated(EnumType.STRING)} in {@link Task}).
 *
 * <p>Java enums are full classes - they can have methods, unlike TypeScript enums.
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE;

    /**
     * Human readable label.
     *
     * <p>Java 21 "switch expression": returns a value, uses {@code ->} (no fall-through, no break),
     * and is EXHAUSTIVE - if someone adds a new enum constant and forgets it here, it won't compile.
     */
    public String displayName() {
        return switch (this) {
            case TODO -> "To Do";
            case IN_PROGRESS -> "In Progress";
            case DONE -> "Done";
        };
    }
}
