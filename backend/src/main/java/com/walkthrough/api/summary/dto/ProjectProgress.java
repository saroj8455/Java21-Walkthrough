package com.walkthrough.api.summary.dto;

/** Progress of a single project, shown as a table row on the dashboard. */
public record ProjectProgress(
        Long id,
        String name,
        long totalTasks,
        long completedTasks,
        double completionRate
) {
}
