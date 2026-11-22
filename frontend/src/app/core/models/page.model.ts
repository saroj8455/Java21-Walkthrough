/** Mirrors backend common/PageResponse.java. */
export interface Page<T> {
  content: T[];
  page: number; // 0-based
  size: number;
  totalElements: number;
  totalPages: number;
}

/** RFC 7807 error body returned by the backend GlobalExceptionHandler. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  [extra: string]: unknown; // e.g. retryAfterSeconds, importId
}
