package com.walkthrough.api.summary;

import com.walkthrough.api.project.ProjectRepository;
import com.walkthrough.api.project.ProjectTaskStats;
import com.walkthrough.api.summary.dto.CountItem;
import com.walkthrough.api.summary.dto.SummaryResponse;
import com.walkthrough.api.task.TaskPriority;
import com.walkthrough.api.task.TaskPriorityCount;
import com.walkthrough.api.task.TaskRepository;
import com.walkthrough.api.task.TaskStatus;
import com.walkthrough.api.task.TaskStatusCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Pure unit test: no Spring, no DB. Mockito fakes the repositories (like jest.fn() / jasmine spies).
 * Runs in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    private SummaryService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new SummaryService(projectRepository, taskRepository, fixedClock);
    }

    @Test
    void buildsSummaryAndFillsMissingBreakdownsWithZero() {
        when(projectRepository.count()).thenReturn(2L);
        when(taskRepository.count()).thenReturn(8L);
        when(taskRepository.countByStatus(TaskStatus.DONE)).thenReturn(3L);
        when(taskRepository.countByDueDateBeforeAndStatusNot(LocalDate.of(2026, 1, 15), TaskStatus.DONE))
                .thenReturn(2L);
        // No IN_PROGRESS row returned by the DB -> service must still report it with 0.
        when(taskRepository.countGroupedByStatus()).thenReturn(List.of(
                new TaskStatusCount(TaskStatus.TODO, 5L),
                new TaskStatusCount(TaskStatus.DONE, 3L)));
        when(taskRepository.countGroupedByPriority()).thenReturn(List.of(
                new TaskPriorityCount(TaskPriority.HIGH, 8L)));
        when(projectRepository.findTaskStats(TaskStatus.DONE)).thenReturn(List.of(
                new ProjectTaskStats(1L, "Alpha", 6L, 3L),
                new ProjectTaskStats(2L, "Empty", 0L, 0L)));

        SummaryResponse summary = service.getSummary();

        assertThat(summary.totalProjects()).isEqualTo(2);
        assertThat(summary.totalTasks()).isEqualTo(8);
        assertThat(summary.completedTasks()).isEqualTo(3);
        assertThat(summary.overdueTasks()).isEqualTo(2);
        assertThat(summary.completionRate()).isEqualTo(37.5);
        assertThat(summary.generatedAt()).isEqualTo(NOW);

        assertThat(summary.tasksByStatus()).containsExactly(
                new CountItem("TODO", "To Do", 5),
                new CountItem("IN_PROGRESS", "In Progress", 0),
                new CountItem("DONE", "Done", 3));

        assertThat(summary.tasksByPriority()).extracting(CountItem::key)
                .containsExactly("HIGH", "MEDIUM", "LOW");

        assertThat(summary.projects()).hasSize(2);
        assertThat(summary.projects().get(0).completionRate()).isEqualTo(50.0);
        assertThat(summary.projects().get(1).completionRate()).isEqualTo(0.0); // no division by zero
    }

    @Test
    void percentageRoundsToOneDecimal() {
        assertThat(SummaryService.percentage(1, 3)).isEqualTo(33.3);
        assertThat(SummaryService.percentage(2, 3)).isEqualTo(66.7);
        assertThat(SummaryService.percentage(0, 0)).isEqualTo(0.0);
    }
}
