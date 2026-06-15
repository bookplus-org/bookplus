import { Injectable } from '@angular/core';
import { AuthSession } from './auth.models';

const STORAGE_KEY = 'bookplus.session';

/**
 * Persists the auth session. Uses localStorage so the session survives reloads.
 * Swapping to an httpOnly-cookie strategy later only touches this class.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  read(): AuthSession | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as AuthSession) : null;
    } catch {
      return null;
    }
  }

  write(session: AuthSession): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  clear(): void {
    localStorage.removeItem(STORAGE_KEY);
  }
}
