import { Pipe, PipeTransform } from '@angular/core';

import { formatInr } from '../../core/utils/format';

/**
 * `{{ 118800 | inr }}` -> ₹1,18,800    `{{ 560 | inr: true }}` -> ₹560.00
 * Pure pipe: Angular re-runs it only when the input value changes.
 */
@Pipe({ name: 'inr', standalone: true })
export class InrPipe implements PipeTransform {
  transform(value: number | null | undefined, exact = false): string {
    return value === null || value === undefined ? '' : formatInr(value, exact);
  }
}
