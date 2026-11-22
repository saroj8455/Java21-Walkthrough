import { HttpErrorResponse } from '@angular/common/http';

import { ProblemDetail } from '../models/page.model';

/** Turns any HTTP error into a sentence for the UI, using the backend's ProblemDetail when present. */
export function toErrorMessage(err: HttpErrorResponse): string {
  if (err.status === 0) {
    return 'Cannot reach the API. Is the backend container running?';
  }
  if (err.status === 413) {
    return 'The file is too large (maximum 5 MB).';
  }
  const problem = err.error as ProblemDetail | null;
  if (err.status === 429) {
    const seconds = Number(err.headers.get('Retry-After') ?? problem?.['retryAfterSeconds'] ?? 5);
    return `Server is busy with other uploads. Please retry in ${seconds}s.`;
  }
  return problem?.detail ?? `Request failed (${err.status} ${err.statusText})`;
}
