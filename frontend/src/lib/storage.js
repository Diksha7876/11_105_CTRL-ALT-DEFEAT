const THEME_KEY = "payment-dashboard.theme";export function getStoredTheme() {
  return localStorage.getItem(THEME_KEY);
}

export function setStoredTheme(theme) {
  localStorage.setItem(THEME_KEY, theme);
}
