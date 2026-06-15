import { FormGroup } from '@angular/forms';
import { ProblemDetail } from '@core/models/problem-detail.model';

/**
 * Maps a backend RFC 7807 validation payload (`errors` map) onto reactive-form
 * controls, so server-side validation surfaces inline next to each field.
 * Returns true when at least one control error was applied.
 */
export function applyServerErrors(form: FormGroup, problem: ProblemDetail): boolean {
  if (!problem.errors) {
    return false;
  }
  let applied = false;
  for (const [field, message] of Object.entries(problem.errors)) {
    const control = form.get(field);
    if (control) {
      control.setErrors({ ...(control.errors ?? {}), server: message });
      control.markAsTouched();
      applied = true;
    }
  }
  return applied;
}
