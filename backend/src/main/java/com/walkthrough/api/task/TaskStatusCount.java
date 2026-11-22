package com.walkthrough.api.task;

/** Query projection: how many tasks have a given status. Built by JPQL {@code select new ...}. */
public record TaskStatusCount(TaskStatus status, Long count) {
}
