package com.walkthrough.api.project;

import com.walkthrough.api.task.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data repository: you write an INTERFACE, Spring generates the implementation at startup.
 *
 * <p>{@code JpaRepository<Project, Long>} already gives you {@code findAll(), findById(), save(),
 * deleteById(), count()} ... for free - like a TypeORM {@code Repository<Project>}.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Per-project task counts in ONE query (no N+1 problem).
     *
     * <p>This is JPQL, not SQL: it talks about entity classes/fields ({@code Project p}, {@code t.status}),
     * Hibernate translates it to SQL. {@code select new ...Record(...)} builds records directly.
     * The {@code left join ... on} is an "ad-hoc entity join" - Project does not need a tasks collection.
     */
    @Query("""
            select new com.walkthrough.api.project.ProjectTaskStats(
                p.id,
                p.name,
                count(t.id),
                coalesce(sum(case when t.status = :doneStatus then 1L else 0L end), 0L)
            )
            from Project p
            left join Task t on t.project = p
            group by p.id, p.name
            order by p.name
            """)
    List<ProjectTaskStats> findTaskStats(@Param("doneStatus") TaskStatus doneStatus);
}
