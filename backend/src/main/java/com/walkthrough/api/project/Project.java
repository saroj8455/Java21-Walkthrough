package com.walkthrough.api.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity mapped to table {@code projects}.
 *
 * <p>Think of an entity as a TypeORM {@code @Entity()} class: one instance == one row.
 * Hibernate (the JPA implementation) reads/writes these objects for you.
 *
 * <p>Rules JPA imposes (why this is a normal class, not a {@code record}):
 * <ul>
 *   <li>Needs a no-args constructor (Hibernate creates the object, then fills the fields).</li>
 *   <li>Fields must be mutable (not {@code final}) so Hibernate can set them.</li>
 * </ul>
 * We use plain getters/setters instead of Lombok so nothing is "magic" while learning.
 */
@Entity
@Table(name = "projects")
public class Project {

    /** Primary key; the DB generates it (IDENTITY column) on INSERT. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    /** updatable=false -> Hibernate never includes this column in UPDATE statements. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. {@code protected} so application code uses the public constructor. */
    protected Project() {
    }

    public Project(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /** Lifecycle hook, runs just before INSERT (like a TypeORM @BeforeInsert). */
    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
