import React, { useState } from "react";
import { clustersApi } from "../../services/api";
import { useAuth } from "../../context/AuthContext";
import type { IncidentCluster } from "../../types";
import {
  BrainIcon,
  FilePdfIcon,
  CircleNotchIcon,
  ArrowClockwiseIcon,
  PencilSimpleIcon,
  UsersIcon,
  XIcon
} from "@phosphor-icons/react";

interface MunicipalOpsViewProps {
  clusters: IncidentCluster[];
  onRefresh: () => void;
}

function getClusterBadgeStyle(c: IncidentCluster): string {
  if (c.status === "RESOLVED") {
    return "bg-emerald-950 text-emerald-300 border-emerald-800/60";
  }
  if (c.severity === "CRITICAL") {
    return "bg-rose-950 text-rose-300 border-rose-800/60";
  }
  if (c.status === "ESCALATED") {
    return "bg-amber-950 text-amber-300 border-amber-800/60";
  }
  return "bg-blue-950 text-blue-300 border-blue-800/60";
}

export const MunicipalOpsView: React.FC<MunicipalOpsViewProps> = ({ clusters, onRefresh }) => {
  const { showToast } = useAuth();

  const [epsMeters, setEpsMeters] = useState(150);
  const [minPoints, setMinPoints] = useState(3);
  const [runningDbscan, setRunningDbscan] = useState(false);
  const [selectedCluster, setSelectedCluster] = useState<IncidentCluster | null>(null);
  const [newStatus, setNewStatus] = useState("IN_PROGRESS");
  const [resolutionNotes, setResolutionNotes] = useState("");
  const [updatingStatus, setUpdatingStatus] = useState(false);

  const handleRunDbscan = async () => {
    setRunningDbscan(true);
    try {
      const summary = await clustersApi.runDbscan(epsMeters, minPoints, 5);
      showToast(
        `DBSCAN Completed: ${summary.clustersFound} clusters formed, ${summary.reportsClustered} grievances grouped, ${summary.escalationsTriggered} escalations to Jal Board!`,
        "success"
      );
      onRefresh();
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Clustering failed: ${error.message}`, "error");
    } finally {
      setRunningDbscan(false);
    }
  };

  const handleUpdateStatus = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedCluster) return;

    setUpdatingStatus(true);
    try {
      await clustersApi.updateStatus(selectedCluster.id, newStatus, resolutionNotes);
      showToast(
        `Cluster ${selectedCluster.clusterCode} updated to ${newStatus}. Citizens notified via SMS/WhatsApp feedback loop.`,
        "success"
      );
      setSelectedCluster(null);
      setResolutionNotes("");
      onRefresh();
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Status update failed: ${error.message}`, "error");
    } finally {
      setUpdatingStatus(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* DBSCAN Control Room Panel */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl text-slate-100">
        <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-purple-600/20 border border-purple-500/40 flex items-center justify-center text-purple-400">
              <BrainIcon size={22} weight="fill" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">Spatial DBSCAN Clustering Engine</h2>
              <p className="text-xs text-slate-400">
                Correlates individual citizen reports into municipal infrastructure failure zones
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 text-xs">
              <label htmlFor="dbscan-eps-input" className="text-slate-400">Epsilon (Eps):</label>
              <input
                id="dbscan-eps-input"
                type="number"
                value={epsMeters}
                onChange={(e) => setEpsMeters(Number.parseInt(e.target.value, 10))}
                className="w-20 px-2 py-1 bg-slate-950 border border-slate-700 rounded-lg text-white font-mono text-xs"
              />
              <span className="text-slate-500">meters</span>
            </div>

            <div className="flex items-center gap-2 text-xs">
              <label htmlFor="dbscan-min-pts-input" className="text-slate-400">Min Pts:</label>
              <input
                id="dbscan-min-pts-input"
                type="number"
                value={minPoints}
                onChange={(e) => setMinPoints(Number.parseInt(e.target.value, 10))}
                className="w-16 px-2 py-1 bg-slate-950 border border-slate-700 rounded-lg text-white font-mono text-xs"
              />
            </div>

            <button
              onClick={handleRunDbscan}
              disabled={runningDbscan}
              className="py-2 px-4 bg-purple-600 hover:bg-purple-500 disabled:bg-purple-800 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-purple-600/20 flex items-center gap-2"
            >
              {runningDbscan ? (
                <CircleNotchIcon size={16} className="animate-spin" />
              ) : (
                <BrainIcon size={16} weight="bold" />
              )}
              <span>Execute DBSCAN</span>
            </button>
          </div>
        </div>

        {/* Clusters Cards Grid */}
        <div className="mt-5">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider">
              Active Municipal Clusters ({clusters.length})
            </h3>
            <button
              onClick={onRefresh}
              className="text-xs text-blue-400 hover:text-blue-300 flex items-center gap-1"
            >
              <ArrowClockwiseIcon size={14} />
              <span>Refresh Feed</span>
            </button>
          </div>

          {clusters.length === 0 ? (
            <div className="text-center py-12 text-slate-500 text-sm">
              No active spatial clusters detected. Run DBSCAN above or submit new grievance reports.
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {clusters.map((c) => {
                const badgeBg = getClusterBadgeStyle(c);

                return (
                  <div
                    key={c.id}
                    className="bg-slate-950/80 border border-slate-800 rounded-xl p-4 shadow flex flex-col justify-between hover:border-slate-700 transition-colors"
                  >
                    <div>
                      <div className="flex items-start justify-between gap-2 mb-2">
                        <div>
                          <span className="font-mono font-bold text-sm text-slate-200">
                            {c.clusterCode}
                          </span>
                          <span className="text-xs text-slate-400 block mt-0.5">
                            {c.incidentCount} Correlated Grievances
                          </span>
                        </div>
                        <span
                          className={`text-[10px] font-bold px-2 py-0.5 rounded border uppercase ${badgeBg}`}
                        >
                          {c.status}
                        </span>
                      </div>

                      <div className="space-y-2 text-xs text-slate-300 my-3">
                        <div>
                          <span className="text-slate-500 block text-[11px]">Correlated Root Cause:</span>
                          <strong className="text-slate-200">
                            {c.correlatedRootCause || "Main Pipeline Pressure Imbalance"}
                          </strong>
                        </div>
                        <div className="flex items-center gap-1.5 text-slate-400">
                          <UsersIcon size={14} className="text-blue-400" />
                          <span>
                            Est. Affected:{" "}
                            <strong className="text-slate-200">
                              {(c.estimatedAffectedPopulation || 15000).toLocaleString()} residents
                            </strong>
                          </span>
                        </div>
                        <div className="text-[11px] text-slate-500">
                          Radius: {c.radiusMeters || 150}m • Center: {c.centerLatitude?.toFixed(4)},{" "}
                          {c.centerLongitude?.toFixed(4)}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 pt-3 border-t border-slate-800/80">
                      <a
                        href={clustersApi.getPdfUrl(c.id)}
                        target="_blank"
                        rel="noreferrer"
                        className="flex-1 py-1.5 px-2 bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 border border-blue-500/40 rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition"
                      >
                        <FilePdfIcon size={14} />
                        <span>AMRUT Dossier</span>
                      </a>

                      <button
                        onClick={() => {
                          setSelectedCluster(c);
                          setNewStatus(c.status === "ACTIVE" ? "IN_PROGRESS" : c.status);
                        }}
                        className="py-1.5 px-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg text-xs font-medium flex items-center gap-1 transition"
                      >
                        <PencilSimpleIcon size={14} />
                        <span>Update</span>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Status Update Modal */}
      {selectedCluster && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <h3 className="font-bold text-white text-sm">
                Update Status: {selectedCluster.clusterCode}
              </h3>
              <button
                onClick={() => setSelectedCluster(null)}
                className="text-slate-400 hover:text-white"
              >
                <XIcon size={20} />
              </button>
            </div>

            <form onSubmit={handleUpdateStatus} className="mt-4 space-y-4 text-xs">
              <div>
                <label htmlFor="target-cluster-status-select" className="block text-slate-300 font-semibold mb-1">Target Cluster Status</label>
                <select
                  id="target-cluster-status-select"
                  value={newStatus}
                  onChange={(e) => setNewStatus(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-white text-xs"
                >
                  <option value="IN_PROGRESS">IN_PROGRESS (Field Crews Dispatched)</option>
                  <option value="ESCALATED">ESCALATED (Forwarded to Chief Engineer)</option>
                  <option value="RESOLVED">RESOLVED (Pressure Restored &amp; Closed)</option>
                  <option value="ACTIVE">ACTIVE (Under Continuous Monitoring)</option>
                </select>
              </div>

              <div>
                <label htmlFor="cluster-resolution-notes-textarea" className="block text-slate-300 font-semibold mb-1">
                  Resolution Work Notes (Dispatched to Citizens via SMS/WhatsApp)
                </label>
                <textarea
                  id="cluster-resolution-notes-textarea"
                  rows={3}
                  value={resolutionNotes}
                  onChange={(e) => setResolutionNotes(e.target.value)}
                  placeholder="e.g., Replacement of 450mm feeder sleeve completed. Potable water pressure normalized. Helpline: 1916."
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-white text-xs"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setSelectedCluster(null)}
                  className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={updatingStatus}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-bold flex items-center gap-1.5"
                >
                  {updatingStatus && <CircleNotchIcon size={14} className="animate-spin" />}
                  <span>Save &amp; Notify Citizens</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
