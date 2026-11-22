package com.walkthrough.api.summary.dto;

/**
 * Generic "label + count" pair used for breakdowns (by status, by priority).
 *
 * @param key   stable machine value, e.g. {@code IN_PROGRESS} (use it for CSS classes / logic)
 * @param label display text, e.g. {@code In Progress}
 * @param count number of tasks
 */
public record CountItem(String key, String label, long count) {
}
