// =============================================================================
// CrowdFlow WaterWatch India - Type-Safe API Client
// =============================================================================

import { resolveApiUrl } from "../config";
import type {
  WaterReport,
  IncidentCluster,
  MunicipalWard,
  WaterTankerUnit,
  EmergencyDepot,
  DispatchRoutePlan,
  DispatchLog,
  AuditLog,
  SystemDiagnostics,
  ExecutiveOverview,
  ExifExtractionResult,
  LiveTrackingData,
  UserRole
} from "../types";

class ApiError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
    this.name = "ApiError";
  }
}

function getStoredToken(): string | null {
  try {
    const raw = localStorage.getItem("civic_admin_auth");
    if (raw) {
      const parsed = JSON.parse(raw);
      return parsed.token || null;
    }
    return localStorage.getItem("crowdflow_jwt") || null;
  } catch {
    return null;
  }
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const url = resolveApiUrl(endpoint);
  const token = getStoredToken();

  const headers: Record<string, string> = {
    Accept: "application/json",
    ...(options.headers as Record<string, string>)
  };

  if (token && !headers["Authorization"]) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  // Do not set Content-Type if sending FormData (browser sets boundary)
  if (!(options.body instanceof FormData) && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(url, { ...options, headers });

  if (!response.ok) {
    let errorMsg = `HTTP Error ${response.status}: ${response.statusText}`;
    try {
      const body = await response.json();
      if (body.message) errorMsg = body.message;
      else if (body.error) errorMsg = body.error;
    } catch {
      // not JSON
    }
    throw new ApiError(errorMsg, response.status);
  }

  // Handle empty bodies (e.g. 204 No Content)
  if (response.status === 204) {
    return {} as T;
  }

  return response.json() as Promise<T>;
}

// -----------------------------------------------------------------------------
// Authentication
// -----------------------------------------------------------------------------
export const authApi = {
  sendOtp(phoneNumber: string): Promise<{ message: string; demoOtp?: string }> {
    return request("/api/auth/send-otp", {
      method: "POST",
      body: JSON.stringify({ phoneNumber })
    });
  },

  verifyOtp(
    phoneNumber: string,
    otp: string,
    role: UserRole
  ): Promise<{ token: string; phoneNumber: string; role: UserRole; name?: string }> {
    return request("/api/auth/verify-otp", {
      method: "POST",
      body: JSON.stringify({ phoneNumber, otp, role })
    });
  }
};

// -----------------------------------------------------------------------------
// Citizen Incident Reporting & Live Stream
// -----------------------------------------------------------------------------
export const reportsApi = {
  getAll(): Promise<WaterReport[]> {
    return request<WaterReport[]>("/api/reports");
  },

  getHeatmap(): Promise<Array<[number, number, number]>> {
    return request<Array<[number, number, number]>>("/api/reports/heatmap");
  },

  getStats(): Promise<{
    totalReports: number;
    totalClusters: number;
    criticalClusters: number;
    resolvedClusters: number;
  }> {
    return request<{
      totalReports: number;
      totalClusters: number;
      criticalClusters: number;
      resolvedClusters: number;
    }>("/api/stats").catch(() => ({
      totalReports: 0,
      totalClusters: 0,
      criticalClusters: 0,
      resolvedClusters: 0
    }));
  },

  create(formData: FormData): Promise<WaterReport> {
    return request<WaterReport>("/api/reports", {
      method: "POST",
      body: formData
    });
  },

  extractExif(file: File): Promise<ExifExtractionResult> {
    const fd = new FormData();
    fd.append("file", file);
    return request<ExifExtractionResult>("/api/reports/extract-exif", {
      method: "POST",
      body: fd
    });
  },

  track(reportCode: string): Promise<LiveTrackingData> {
    return request<LiveTrackingData>(`/api/reports/track/${encodeURIComponent(reportCode)}`);
  },

  submitFeedback(
    reportCode: string,
    rating: number,
    comment?: string
  ): Promise<LiveTrackingData> {
    const q = new URLSearchParams({
      rating: rating.toString(),
      comment: comment || ""
    });
    return request<LiveTrackingData>(
      `/api/reports/track/${encodeURIComponent(reportCode)}/feedback?${q.toString()}`,
      { method: "POST" }
    );
  }
};

// -----------------------------------------------------------------------------
// Clusters & DBSCAN
// -----------------------------------------------------------------------------
export const clustersApi = {
  getAll(): Promise<IncidentCluster[]> {
    return request<IncidentCluster[]>("/api/clusters");
  },

  runDbscan(
    epsMeters = 150,
    minPoints = 3,
    escalationThreshold = 5
  ): Promise<{
    clustersFound: number;
    reportsClustered: number;
    unclusteredNoise: number;
    escalationsTriggered: number;
  }> {
    return request(
      `/api/clusters/run?epsMeters=${epsMeters}&minPoints=${minPoints}&escalationThreshold=${escalationThreshold}`,
      { method: "POST" }
    );
  },

  updateStatus(
    clusterId: number,
    status: string,
    notes?: string
  ): Promise<IncidentCluster> {
    return request(`/api/clusters/${clusterId}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status, notes })
    });
  },

  escalate(clusterId: number): Promise<{ message: string; cluster: IncidentCluster }> {
    return request(`/api/clusters/${clusterId}/escalate`, { method: "POST" });
  },

  getPdfUrl(clusterId: number): string {
    return resolveApiUrl(`/api/clusters/${clusterId}/pdf`);
  }
};

// -----------------------------------------------------------------------------
// Municipal Wards & Dispatch
// -----------------------------------------------------------------------------
export const wardsApi = {
  getAll(): Promise<MunicipalWard[]> {
    return request<MunicipalWard[]>("/api/wards");
  },

  lookup(lat: number, lon: number): Promise<MunicipalWard> {
    return request<MunicipalWard>(`/api/wards/lookup?lat=${lat}&lon=${lon}`);
  }
};

export const dispatchApi = {
  getLogs(): Promise<DispatchLog[]> {
    return request<DispatchLog[]>("/api/dispatch/logs").catch(() =>
      request<DispatchLog[]>("/api/municipal/dispatch-logs")
    );
  }
};

// -----------------------------------------------------------------------------
// Administrative Operations (Delhi Jal Board & Super Admin)
// -----------------------------------------------------------------------------
export const adminApi = {
  getOverview(): Promise<ExecutiveOverview> {
    return request<ExecutiveOverview>("/api/admin/overview");
  },

  getReports(status?: string, search?: string): Promise<WaterReport[]> {
    const params = new URLSearchParams();
    if (status && status !== "ALL") params.append("status", status);
    if (search?.trim()) params.append("search", search.trim());
    const qs = params.toString();
    const endpoint = qs ? `/api/admin/reports?${qs}` : "/api/admin/reports";
    return request<WaterReport[]>(endpoint);
  },

  deleteReport(id: number): Promise<void> {
    return request<void>(`/api/admin/reports/${id}`, { method: "DELETE" });
  },

  updateReportStatus(id: number, status: string, notes?: string): Promise<WaterReport> {
    return request<WaterReport>(`/api/admin/reports/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status, notes })
    });
  },

  reassignWard(id: number, wardNumber: number): Promise<WaterReport> {
    return request<WaterReport>(`/api/admin/reports/${id}/reassign?wardNumber=${wardNumber}`, {
      method: "PATCH"
    });
  },

  getClusters(): Promise<IncidentCluster[]> {
    return request<IncidentCluster[]>("/api/admin/clusters");
  },

  runClusters(epsMeters = 150, minPoints = 3): Promise<{
    clustersFound: number;
    reportsClustered: number;
    unclusteredNoise: number;
    escalationsTriggered: number;
  }> {
    return request(`/api/admin/clusters/run?epsMeters=${epsMeters}&minPoints=${minPoints}`, {
      method: "POST"
    });
  },

  escalateCluster(id: number): Promise<IncidentCluster> {
    return request<IncidentCluster>(`/api/admin/clusters/${id}/escalate`, { method: "POST" });
  },

  getFleetStatus(): Promise<{
    tankers: WaterTankerUnit[];
    depots: EmergencyDepot[];
    activeRoutes: DispatchRoutePlan[];
  }> {
    return request("/api/admin/fleet/status");
  },

  calculateRoute(lat: number, lon: number, desc?: string): Promise<DispatchRoutePlan> {
    const q = new URLSearchParams({
      lat: lat.toString(),
      lon: lon.toString(),
      desc: desc || "Water Emergency"
    });
    return request<DispatchRoutePlan>(`/api/admin/fleet/dispatch?${q.toString()}`, {
      method: "POST"
    });
  },

  getAuditLogs(): Promise<AuditLog[]> {
    return request<AuditLog[]>("/api/admin/audit-logs");
  },

  getDiagnostics(): Promise<SystemDiagnostics> {
    return request<SystemDiagnostics>("/api/admin/diagnostics");
  },

  clearCache(cacheName: string): Promise<{ message: string; timestamp: string }> {
    return request(`/api/admin/cache/clear?cacheName=${encodeURIComponent(cacheName)}`, {
      method: "POST"
    });
  },

  geoNearby(
    lat: number,
    lon: number,
    radiusKm: number
  ): Promise<{ count: number; nearbyReports: WaterReport[] }> {
    return request(`/api/cache/geo/nearby?lat=${lat}&lon=${lon}&radiusKm=${radiusKm}`);
  }
};
