package com.walkthrough.api.task;

/** Query projection: how many tasks have a given priority. */
public record TaskPriorityCount(TaskPriority priority, Long count) {
}
