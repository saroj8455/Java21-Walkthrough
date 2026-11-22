package com.walkthrough.api.summary;

import com.walkthrough.api.project.ProjectRepository;
import com.walkthrough.api.summary.dto.CountItem;
import com.walkthrough.api.summary.dto.ProjectProgress;
import com.walkthrough.api.summary.dto.SummaryResponse;
import com.walkthrough.api.task.TaskPriority;
import com.walkthrough.api.task.TaskPriorityCount;
import com.walkthrough.api.task.TaskRepository;
import com.walkthrough.api.task.TaskStatus;
import com.walkthrough.api.task.TaskStatusCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic for the dashboard summary.
 *
 * <p>{@code @Service} registers this class as a singleton in Spring's DI container -
 * exactly like {@code @Injectable({providedIn: 'root'})} in Angular.
 *
 * <p><b>Constructor injection</b>: Spring sees the constructor parameters and passes the beans in.
 * Same idea as {@code constructor(private http: HttpClient)} in Angular. Fields are {@code final},
 * so the dependencies can never be reassigned, and in unit tests we just call {@code new SummaryService(mocks)}.
 */
@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    /** Injected instead of calling LocalDate.now() directly so tests can freeze "today". */
    private final Clock clock;

    public SummaryService(ProjectRepository projectRepository, TaskRepository taskRepository, Clock clock) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    /**
     * Builds the summary from several aggregate queries.
     *
     * <p>{@code @Transactional(readOnly = true)}: all queries run in one DB transaction/connection,
     * so the numbers are consistent with each other, and Hibernate skips dirty-checking (faster).
     */
    @Transactional(readOnly = true)
    public SummaryResponse getSummary() {
        long totalProjects = projectRepository.count();
        long totalTasks = taskRepository.count();
        long completedTasks = taskRepository.countByStatus(TaskStatus.DONE);
        long overdueTasks = taskRepository.countByDueDateBeforeAndStatusNot(LocalDate.now(clock), TaskStatus.DONE);

        List<ProjectProgress> projects = projectRepository.findTaskStats(TaskStatus.DONE).stream()
                .map(stats -> new ProjectProgress(
                        stats.projectId(),
                        stats.projectName(),
                        stats.totalTasks(),
                        stats.completedTasks(),
                        percentage(stats.completedTasks(), stats.totalTasks())))
                .toList();

        log.debug("Summary computed: {} projects, {} tasks, {} done, {} overdue",
                totalProjects, totalTasks, completedTasks, overdueTasks);

        return new SummaryResponse(
                totalProjects,
                totalTasks,
                completedTasks,
                overdueTasks,
                percentage(completedTasks, totalTasks),
                statusBreakdown(),
                priorityBreakdown(),
                projects,
                Instant.now(clock));
    }

    /**
     * The DB only returns statuses that have tasks. We want EVERY status (with 0 when missing)
     * so the UI layout is stable. Streams are Java's version of JS array methods:
     * {@code .stream().map(...).toList()} ~ {@code array.map(...)}.
     */
    private List<CountItem> statusBreakdown() {
        Map<TaskStatus, Long> counts = taskRepository.countGroupedByStatus().stream()
                .collect(Collectors.toMap(TaskStatusCount::status, TaskStatusCount::count));

        return Arrays.stream(TaskStatus.values())
                .map(status -> new CountItem(status.name(), status.displayName(), counts.getOrDefault(status, 0L)))
                .toList();
    }

    private List<CountItem> priorityBreakdown() {
        Map<TaskPriority, Long> counts = taskRepository.countGroupedByPriority().stream()
                .collect(Collectors.toMap(TaskPriorityCount::priority, TaskPriorityCount::count));

        // reversed order -> HIGH, MEDIUM, LOW (most important first)
        return Arrays.stream(TaskPriority.values())
                .map(p -> new CountItem(p.name(), p.displayName(), counts.getOrDefault(p, 0L)))
                .toList()
                .reversed(); // Java 21 "Sequenced Collections" API
    }

    /** part/total as a percentage rounded to 1 decimal; 0 when total is 0 (avoid division by zero). */
    static double percentage(long part, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round(part * 1000.0 / total) / 10.0;
    }
}
