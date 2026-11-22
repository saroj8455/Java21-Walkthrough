package com.walkthrough.api.project;

/**
 * Read-only "projection" filled directly by a JPQL query (see {@link ProjectRepository#findTaskStats}).
 *
 * <p>A Java {@code record} is an immutable data carrier - similar to a TypeScript
 * {@code interface} + {@code Readonly<>}, but it is a real class: the compiler generates the
 * constructor, accessors ({@code projectId()}, not {@code getProjectId()}), equals/hashCode/toString.
 *
 * <p>Wrapper type {@code Long} (not primitive {@code long}) because SQL {@code COUNT/SUM} return
 * {@code Long} objects and Hibernate matches the constructor by exact types.
 */
public record ProjectTaskStats(Long projectId, String projectName, Long totalTasks, Long completedTasks) {
}
