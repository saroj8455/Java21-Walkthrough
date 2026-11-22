/**
 * TypeScript mirror of the backend DTOs in
 * backend/src/main/java/com/walkthrough/api/summary/dto/*.java
 * Keep these two in sync whenever the API contract changes.
 */

export interface CountItem {
  key: string; // e.g. 'IN_PROGRESS'
  label: string; // e.g. 'In Progress'
  count: number;
}

export interface ProjectProgress {
  id: number;
  name: string;
  totalTasks: number;
  completedTasks: number;
  completionRate: number;
}

export interface Summary {
  totalProjects: number;
  totalTasks: number;
  completedTasks: number;
  overdueTasks: number;
  completionRate: number;
  tasksByStatus: CountItem[];
  tasksByPriority: CountItem[];
  projects: ProjectProgress[];
  generatedAt: string; // ISO-8601 instant, e.g. 2026-01-15T10:00:00Z
}
