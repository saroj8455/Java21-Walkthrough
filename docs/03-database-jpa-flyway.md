# 03 — Database: JPA, Hibernate, Flyway

## The pieces
| Piece | Node equivalent | Role |
|---|---|---|
| JDBC driver (`org.postgresql`) | `pg` | raw connection to Postgres |
| HikariCP (auto-configured) | `pg.Pool` | connection pool |
| JPA (spec) + Hibernate (impl) | TypeORM / Prisma | ORM: objects ↔ rows |
| Spring Data JPA | TypeORM `Repository<T>` | generates repositories from interfaces |
| Flyway | knex/prisma migrations | versioned SQL schema changes |

## Schema = Flyway, not Hibernate
`backend/src/main/resources/db/migration/`
- `V1__create_projects_and_tasks.sql` — tables, constraints, indexes
- `V2__seed_demo_data.sql` — demo data

On startup Flyway checks table `flyway_schema_history` and runs any new `V<n>__*.sql` file once.
Then Hibernate runs with `ddl-auto: validate`: it checks entities match tables and refuses to start if not.

> **Rule:** never edit an applied migration (Flyway checksums it and fails). Add `V3__...sql`.

## Entity mapping — `task/Task.java`
```java
@Entity @Table(name = "tasks")
public class Task {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;   // BIGINT IDENTITY
  @Enumerated(EnumType.STRING) private TaskStatus status;                     // 'IN_PROGRESS' text
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id")
  private Project project;                                                    // FK
}
```

## Three ways to query (all in this repo)
1. **Built-in**: `projectRepository.count()`, `findAll()`, `findById(id)`, `save(entity)`.
2. **Derived from method name**: `countByDueDateBeforeAndStatusNot(date, DONE)`
   → `select count(*) from tasks where due_date < ? and status <> ?`
3. **`@Query` JPQL** with constructor projection (best for aggregates — no entities loaded):
   ```java
   select new com.walkthrough.api.task.TaskStatusCount(t.status, count(t.id))
   from Task t group by t.status
   ```
   JPQL uses **entity and field names** (`Task`, `t.status`), not table/column names.

## The N+1 problem (why `findTaskStats` is one query)
Naively: load 4 projects, then for each project load its tasks → 1 + 4 queries. With 1,000 projects that's
1,001 queries. `ProjectRepository.findTaskStats` does a single `LEFT JOIN ... GROUP BY` instead.
Lesson 08 goes deeper (fetch joins, entity graphs).

## Explore in pgAdmin
http://localhost:5050 → Servers → *Walkthrough DB (docker)* → password `walkthrough`.
```sql
SELECT status, count(*) FROM tasks GROUP BY status;
SELECT * FROM flyway_schema_history;
```
