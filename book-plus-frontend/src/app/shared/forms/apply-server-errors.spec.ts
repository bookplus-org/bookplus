import { FormControl, FormGroup } from '@angular/forms';
import { applyServerErrors } from './apply-server-errors';
import { ProblemDetail } from '@core/models/problem-detail.model';

describe('applyServerErrors', () => {
  function buildForm(): FormGroup {
    return new FormGroup({
      email: new FormControl(''),
      password: new FormControl(''),
    });
  }

  it('maps backend field errors onto matching controls', () => {
    const form = buildForm();
    const problem: ProblemDetail = {
      type: 'https://bookplus.com/errors/validation-error',
      title: 'Validation Error',
      status: 400,
      errors: { email: 'must be a valid email' },
    };

    const applied = applyServerErrors(form, problem);

    expect(applied).toBeTrue();
    expect(form.get('email')?.getError('server')).toBe('must be a valid email');
    expect(form.get('password')?.errors).toBeNull();
  });

  it('returns false when there are no field errors', () => {
    const problem: ProblemDetail = { type: 'about:blank', title: 'Error', status: 500 };
    expect(applyServerErrors(buildForm(), problem)).toBeFalse();
  });
});
