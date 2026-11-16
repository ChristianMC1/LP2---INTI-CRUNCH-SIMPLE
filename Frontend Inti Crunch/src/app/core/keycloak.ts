import Keycloak from 'keycloak-js';

export const keycloak = new Keycloak({
  url: 'http://localhost:8180',
  realm: 'inti-crunch',
  clientId: 'inti-crunch-web'
});

const STORAGE_KEY = 'inti-crunch-session';
export type SessionTokens = { access_token: string; refresh_token: string; id_token?: string };

/** Guarda los tokens del inicio de sesión con DNI para mantener la sesión al recargar. */
export function storeSession(tokens: SessionTokens | undefined): void {
  try {
    if (!tokens) localStorage.removeItem(STORAGE_KEY);
    else localStorage.setItem(STORAGE_KEY, JSON.stringify({ access_token: tokens.access_token, refresh_token: tokens.refresh_token, id_token: tokens.id_token }));
  } catch { /* almacenamiento no disponible */ }
}
function storedSession(): SessionTokens | undefined {
  try { const value = localStorage.getItem(STORAGE_KEY); return value ? JSON.parse(value) : undefined; } catch { return undefined; }
}

keycloak.onAuthRefreshSuccess = () => storeSession({ access_token: keycloak.token!, refresh_token: keycloak.refreshToken!, id_token: keycloak.idToken });
keycloak.onTokenExpired = () => { void keycloak.updateToken(30).catch(() => { storeSession(undefined); keycloak.clearToken(); }); };

export async function initializeKeycloak(): Promise<void> {
  const saved = storedSession();
  try {
    if (saved?.refresh_token) {
      await keycloak.init({ token: saved.access_token, refreshToken: saved.refresh_token, idToken: saved.id_token, checkLoginIframe: false });
      try { await keycloak.updateToken(30); } catch { storeSession(undefined); keycloak.clearToken(); }
    } else {
      await keycloak.init({ onLoad: 'check-sso', checkLoginIframe: false });
    }
  } catch (error) {
    console.warn('Keycloak no está disponible; el catálogo público sigue accesible.', error);
  }
}
