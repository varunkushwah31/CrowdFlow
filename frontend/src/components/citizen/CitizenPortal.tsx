import React, { useState, useEffect } from "react";
import { reportsApi, clustersApi, wardsApi, dispatchApi, adminApi } from "../../services/api";
import { useAuth } from "../../context/AuthContext";
import type { WaterReport, IncidentCluster, MunicipalWard, WaterTankerUnit, EmergencyDepot, DispatchLog } from "../../types";
import { IncidentMap } from "./IncidentMap";
import { ReportHazardForm } from "./ReportHazardForm";
import { MunicipalOpsView } from "./MunicipalOpsView";
import { WardsView } from "./WardsView";
import { DispatchLogsView } from "./DispatchLogsView";
import { TrackGrievanceView } from "./TrackGrievanceView";
import type { CitizenTab } from "../common/Navbar";
import {
  DropIcon,
  WarningCircleIcon,
  CheckCircleIcon,
  BrainIcon,
  CircleNotchIcon
} from "@phosphor-icons/react";

interface CitizenPortalProps {
  activeTab: CitizenTab;
  onTabChange: (tab: CitizenTab) => void;
}

export const CitizenPortal: React.FC<CitizenPortalProps> = ({ activeTab, onTabChange }) => {
  const { showToast } = useAuth();

  const [reports, setReports] = useState<WaterReport[]>([]);
  const [clusters, setClusters] = useState<IncidentCluster[]>([]);
  const [wards, setWards] = useState<MunicipalWard[]>([]);
  const [tankers, setTankers] = useState<WaterTankerUnit[]>([]);
  const [depots, setDepots] = useState<EmergencyDepot[]>([]);
  const [dispatchLogs, setDispatchLogs] = useState<DispatchLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [clickedCoords, setClickedCoords] = useState<{ lat: number; lon: number } | null>(null);
  const [trackedReportCode, setTrackedReportCode] = useState<string>("");

  const loadData = async () => {
    try {
      const [reps, clusts, wrds] = await Promise.all([
        reportsApi.getAll().catch(() => []),
        clustersApi.getAll().catch(() => []),
        wardsApi.getAll().catch(() => [])
      ]);
      setReports(reps);
      setClusters(clusts);
      setWards(wrds);

      // Load fleet & logs in background
      adminApi.getFleetStatus()
        .then((f) => {
          setTankers(f.tankers || []);
          setDepots(f.depots || []);
        })
        .catch(() => {});

      dispatchApi.getLogs()
        .then((l) => setDispatchLogs(l || []))
        .catch(() => {});
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Error syncing municipal data: ${error.message}`, "error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const totalReports = reports.length;
  const criticalReports = reports.filter((r) => r.severity === "CRITICAL").length;
  const activeClustersCount = clusters.filter((c) => c.status !== "RESOLVED").length;
  const resolvedCount = reports.filter((r) => r.status === "RESOLVED").length;

  return (
    <div className="space-y-6">
      {/* Top Civic KPI Metric Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/40 flex items-center justify-center text-blue-400 shrink-0">
            <DropIcon size={20} weight="fill" />
          </div>
          <div>
            <span className="text-xl font-bold text-white font-mono block">{totalReports}</span>
            <span className="text-xs text-slate-400">Total Grievances</span>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-amber-600/20 border border-amber-500/40 flex items-center justify-center text-amber-400 shrink-0">
            <BrainIcon size={20} weight="fill" />
          </div>
          <div>
            <span className="text-xl font-bold text-amber-400 font-mono block">{activeClustersCount}</span>
            <span className="text-xs text-slate-400">Active Clusters</span>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-rose-600/20 border border-rose-500/40 flex items-center justify-center text-rose-400 shrink-0">
            <WarningCircleIcon size={20} weight="fill" />
          </div>
          <div>
            <span className="text-xl font-bold text-rose-400 font-mono block">{criticalReports}</span>
            <span className="text-xs text-slate-400">Critical Ruptures</span>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-600/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400 shrink-0">
            <CheckCircleIcon size={20} weight="fill" />
          </div>
          <div>
            <span className="text-xl font-bold text-emerald-400 font-mono block">{resolvedCount}</span>
            <span className="text-xs text-slate-400">Resolved Reports</span>
          </div>
        </div>
      </div>

      {/* Main Tab Content */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-20 text-slate-400 gap-3">
          <CircleNotchIcon size={36} className="animate-spin text-blue-500" />
          <p className="text-sm font-medium">Synchronizing Delhi Jal Board GIS Stream...</p>
        </div>
      ) : (
        <>
          {activeTab === "map" && (
            <div className="space-y-4">
              <IncidentMap
                reports={reports}
                clusters={clusters}
                wards={wards}
                tankers={tankers}
                depots={depots}
                onMapClick={(lat, lon) => {
                  setClickedCoords({ lat, lon });
                  showToast(
                    `Coordinates [${lat.toFixed(5)}, ${lon.toFixed(5)}] pinned! Click "Report Water Hazard" to file grievance here.`,
                    "info"
                  );
                }}
              />
            </div>
          )}

          {activeTab === "report" && (
            <ReportHazardForm
              clickedCoords={clickedCoords}
              onSuccess={(created) => {
                setTrackedReportCode(created.reportCode);
                loadData();
                onTabChange("track");
              }}
            />
          )}

          {activeTab === "operations" && (
            <MunicipalOpsView clusters={clusters} onRefresh={loadData} />
          )}

          {activeTab === "wards" && <WardsView wards={wards} />}

          {activeTab === "dispatch" && (
            <DispatchLogsView logs={dispatchLogs} onRefresh={loadData} />
          )}

          {activeTab === "track" && (
            <TrackGrievanceView initialCode={trackedReportCode} />
          )}
        </>
      )}
    </div>
  );
};
