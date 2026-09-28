// =============================================================================
// CrowdFlow WaterWatch India - Domain TypeScript Interfaces
// =============================================================================

export type ReportStatus =
  | "SUBMITTED"
  | "CLUSTERED"
  | "ESCALATED"
  | "IN_PROGRESS"
  | "RESOLVED"
  | "CLOSED"
  | "REJECTED";

export type IssueType =
  | "PIPELINE_BURST"
  | "WATER_CONTAMINATION"
  | "SEWAGE_OVERFLOW"
  | "LOW_PRESSURE"
  | "ILLEGAL_EXTRACTION"
  | "BOREWELL_DEPLETION"
  | "WATER_TANKER_IRREGULARITY"
  | "OTHER";

export type SeverityLevel = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";

export type ClusterStatus = "ACTIVE" | "IN_PROGRESS" | "ESCALATED" | "RESOLVED";
export type ClusterSeverity = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";

export type UserRole = "ROLE_CITIZEN" | "ROLE_WARD_OFFICER" | "ROLE_SUPER_ADMIN";

export interface WaterReport {
  id: number;
  reportCode: string;
  title: string;
  description?: string;
  issueType: IssueType;
  severity: SeverityLevel;
  status: ReportStatus;
  latitude: number;
  longitude: number;
  wardNumber: number;
  imageUrl?: string;
  cameraMake?: string;
  cameraModel?: string;
  createdAt: string;
  updatedAt?: string;
  clusterId?: number;
  citizenPhone?: string;
  resolutionNotes?: string;
}

export interface IncidentCluster {
  id: number;
  clusterCode: string;
  status: ClusterStatus;
  severity: ClusterSeverity;
  centerLatitude: number;
  centerLongitude: number;
  radiusMeters: number;
  incidentCount: number;
  correlatedRootCause?: string;
  estimatedAffectedPopulation?: number;
  escalatedAt?: string;
  resolvedAt?: string;
  createdAt: string;
  reports?: WaterReport[];
}

export interface MunicipalWard {
  id: number;
  wardNumber: number;
  wardName: string;
  zoneName: string;
  municipalBody: string;
  officerName: string;
  officerDesignation: string;
  contactEmail: string;
  emergencyHotline: string;
  webhookUrl: string;
  pincode?: string;
  centerLatitude?: number;
  centerLongitude?: number;
  polygonGeoJson?: string;
}

export interface WaterTankerUnit {
  id: number;
  vehicleNumber: string;
  capacityLiters: number;
  status: "AVAILABLE" | "DISPATCHED" | "REFUELING" | "MAINTENANCE";
  currentLatitude: number;
  currentLongitude: number;
  assignedDepotId?: number;
  driverName?: string;
  driverPhone?: string;
}

export interface EmergencyDepot {
  id: number;
  depotName: string;
  wardNumber: number;
  latitude: number;
  longitude: number;
  totalTankers: number;
  availableTankers: number;
  contactNumber: string;
}

export interface DispatchRoutePlan {
  unitId: number;
  depotId: number;
  vehicleNumber: string;
  depotName: string;
  targetLatitude: number;
  targetLongitude: number;
  estimatedArrivalMinutes: number;
  distanceKm: number;
  waypoints: Array<[number, number]>;
}

export interface DispatchLog {
  id: number;
  clusterCode: string;
  dispatchChannel: string;
  recipient: string;
  payloadSummary: string;
  status: "DELIVERED" | "PENDING" | "FAILED";
  dispatchedAt: string;
}

export interface AuditLog {
  id: number;
  timestamp: string;
  action: string;
  performedBy: string;
  role: string;
  ipAddress?: string;
  details?: string;
  entityType?: string;
  entityId?: string;
}

export interface SystemDiagnostics {
  uptime: string;
  activeThreads: number;
  dbPoolActive: number;
  dbPoolIdle: number;
  redisConnected: boolean;
  cacheHitRatio: number;
  heapUsedBytes: number;
  heapMaxBytes: number;
  virtualThreadsEnabled: boolean;
}

export interface ExecutiveOverview {
  totalReports: number;
  submittedReports: number;
  clusteredReports: number;
  escalatedReports: number;
  inProgressReports: number;
  resolvedReports: number;
  totalClusters: number;
  activeClusters: number;
  criticalClusters: number;
  resolvedClusters: number;
  averageSlaMinutes: number;
  activeTankerUnits: number;
  availableTankers: number;
  reportsByWard: Record<string, number>;
  reportsByType: Record<string, number>;
}

export interface ExifExtractionResult {
  hasGps: boolean;
  latitude?: number;
  longitude?: number;
  altitude?: string;
  cameraMake?: string;
  cameraModel?: string;
  dateTimeOriginal?: string;
}

export interface LiveTrackingStage {
  stage: string;
  timestamp: string;
  completed: boolean;
  description: string;
}

export interface LiveTrackingData {
  reportCode: string;
  currentStage: string;
  latitude: number;
  longitude: number;
  title: string;
  description?: string;
  issueType: IssueType;
  wardNumber: number;
  createdAt: string;
  stages: LiveTrackingStage[];
  assignedOfficer?: string;
  helpline?: string;
  resolutionNotes?: string;
}

export interface AuthState {
  token: string;
  phoneNumber: string;
  role: UserRole;
  name?: string;
  designation?: string;
}

export interface ToastNotification {
  id: number;
  message: string;
  type: "info" | "success" | "warning" | "error";
}
