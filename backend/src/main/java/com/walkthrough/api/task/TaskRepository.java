package com.walkthrough.api.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for {@link Task}. Shows the two ways of writing queries in Spring Data:
 * <ol>
 *   <li><b>Derived queries</b> - Spring parses the METHOD NAME and writes the SQL for you.</li>
 *   <li><b>{@code @Query}</b> - you write JPQL yourself (needed for GROUP BY, projections, ...).</li>
 * </ol>
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Derived query: SELECT count(*) FROM tasks WHERE status = ? */
    long countByStatus(TaskStatus status);

    /** Derived query: ... WHERE due_date < ? AND status <> ?   (used for "overdue") */
    long countByDueDateBeforeAndStatusNot(LocalDate date, TaskStatus status);

    /** GROUP BY status -> one row per status that has at least one task. */
    @Query("""
            select new com.walkthrough.api.task.TaskStatusCount(t.status, count(t.id))
            from Task t
            group by t.status
            """)
    List<TaskStatusCount> countGroupedByStatus();

    /** GROUP BY priority. */
    @Query("""
            select new com.walkthrough.api.task.TaskPriorityCount(t.priority, count(t.id))
            from Task t
            group by t.priority
            """)
    List<TaskPriorityCount> countGroupedByPriority();
}
