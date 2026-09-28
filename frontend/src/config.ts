/**
 * CrowdFlow Frontend Configuration
 */

// Heuristic fallback for development vs production
let defaultBase = "";
if (typeof window !== "undefined" && window.location) {
  const port = window.location.port;
  const host = window.location.hostname;
  if ((host === "localhost" || host === "127.0.0.1") && port !== "8085") {
    defaultBase = "http://localhost:8085";
  }
}

export const CROWDFLOW_CONFIG = {
  getApiBaseUrl(): string {
    if (typeof window === "undefined") return "";
    return (
      (window as unknown as { __CROWDFLOW_API_BASE__?: string }).__CROWDFLOW_API_BASE__ ||
      localStorage.getItem("crowdflow_api_base") ||
      defaultBase
    );
  },
  setApiBaseUrl(url: string): void {
    if (typeof window !== "undefined") {
      localStorage.setItem("crowdflow_api_base", url);
    }
  },
  APP_NAME: "WaterWatch India Civic Monitoring",
  VERSION: "1.0.0"
};

/**
 * Resolves any endpoint path to the full target backend URL.
 */
export function resolveApiUrl(endpoint: string): string {
  if (!endpoint) return "";
  if (
    endpoint.startsWith("http://") ||
    endpoint.startsWith("https://") ||
    endpoint.startsWith("data:") ||
    endpoint.startsWith("blob:")
  ) {
    return endpoint;
  }
  let base = CROWDFLOW_CONFIG.getApiBaseUrl();
  while (base.endsWith("/")) {
    base = base.slice(0, -1);
  }
  const cleanEndpoint = endpoint.startsWith("/") ? endpoint : `/${endpoint}`;
  return base ? `${base}${cleanEndpoint}` : cleanEndpoint;
}
