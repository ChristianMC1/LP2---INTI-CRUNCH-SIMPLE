import { HttpInterceptorFn } from '@angular/common/http';
import { from, switchMap, catchError, of } from 'rxjs';
import { keycloak } from './keycloak';

/** Agrega el token de la sesión a cada llamada a la API cuando hay alguien conectado. */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const traceId = crypto.randomUUID();
  const isApi = request.url.includes('/api/v1/');
  const isLogin = request.url.includes('/api/v1/public/auth/');
  const send = () => {
    const headers: Record<string, string> = { 'X-Trace-ID': traceId };
    if (isApi && !isLogin && keycloak.authenticated && keycloak.token) headers['Authorization'] = `Bearer ${keycloak.token}`;
    return next(request.clone({ setHeaders: headers }));
  };
  if (!isApi || isLogin || !keycloak.authenticated || !keycloak.refreshToken) return send();
  return from(keycloak.updateToken(30)).pipe(catchError(() => of(false)), switchMap(send));
};
