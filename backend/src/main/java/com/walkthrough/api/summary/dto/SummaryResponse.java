package com.walkthrough.api.summary.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * API response for {@code GET /api/v1/summary}.
 *
 * <p>DTO (Data Transfer Object) = the public JSON contract. We NEVER return JPA entities directly:
 * that would leak DB structure, risk lazy-loading errors and couple clients to the schema.
 * The Angular {@code Summary} interface (frontend/src/app/core/models/summary.model.ts) mirrors this record.
 *
 * <p>Jackson (Spring's JSON library, like JSON.stringify) serializes record components by name.
 */
@Schema(description = "Dashboard summary of projects and tasks")
public record SummaryResponse(
        long totalProjects,
        long totalTasks,
        long completedTasks,
        long overdueTasks,
        @Schema(description = "Completed / total tasks, as a percentage with 1 decimal", example = "38.9")
        double completionRate,
        List<CountItem> tasksByStatus,
        List<CountItem> tasksByPriority,
        List<ProjectProgress> projects,
        Instant generatedAt
) {
}
