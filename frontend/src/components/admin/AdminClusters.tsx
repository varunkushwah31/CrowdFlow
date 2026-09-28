import React, { useState } from "react";
import { adminApi, clustersApi } from "../../services/api";
import { useAuth } from "../../context/AuthContext";
import type { IncidentCluster } from "../../types";
import {
  BrainIcon,
  FilePdfIcon,
  LightningIcon,
  UsersIcon,
  CircleNotchIcon
} from "@phosphor-icons/react";

interface AdminClustersProps {
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

export const AdminClusters: React.FC<AdminClustersProps> = ({ clusters, onRefresh }) => {
  const { showToast } = useAuth();

  const [epsMeters, setEpsMeters] = useState(150);
  const [minPoints, setMinPoints] = useState(3);
  const [running, setRunning] = useState(false);
  const [escalatingId, setEscalatingId] = useState<number | null>(null);

  const handleRunDbscan = async () => {
    setRunning(true);
    try {
      const summary = await adminApi.runClusters(epsMeters, minPoints);
      showToast(
        `DBSCAN Clustering complete: ${summary.clustersFound} clusters formed, ${summary.reportsClustered} reports clustered. ${summary.escalationsTriggered} triggered AMRUT escalations.`,
        "success"
      );
      onRefresh();
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Clustering execution failed: ${error.message}`, "error");
    } finally {
      setRunning(false);
    }
  };

  const handleEscalate = async (cluster: IncidentCluster) => {
    setEscalatingId(cluster.id);
    try {
      await adminApi.escalateCluster(cluster.id);
      showToast(
        `Cluster ${cluster.clusterCode} escalated to Delhi Jal Board Zonal Commissioner! Official AMRUT 2.0 dossier generated.`,
        "success"
      );
      onRefresh();
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Escalation failed: ${error.message}`, "error");
    } finally {
      setEscalatingId(null);
    }
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl text-slate-100">
      <div className="flex flex-wrap items-center justify-between gap-4 pb-5 border-b border-slate-800">
        <div>
          <h2 className="text-base font-bold text-white">Spatial DBSCAN Engine &amp; AMRUT 2.0 Dossiers</h2>
          <p className="text-xs text-slate-400">
            Automated density-based spatial clustering of grievances &amp; municipal incident escalations
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 text-xs bg-slate-950 px-3 py-1.5 rounded-xl border border-slate-800">
            <span className="text-slate-400">Epsilon:</span>
            <input
              type="number"
              value={epsMeters}
              onChange={(e) => setEpsMeters(Number.parseInt(e.target.value, 10))}
              className="w-16 bg-transparent text-white font-mono text-center focus:outline-none"
            />
            <span className="text-slate-500">meters</span>
          </div>

          <div className="flex items-center gap-2 text-xs bg-slate-950 px-3 py-1.5 rounded-xl border border-slate-800">
            <span className="text-slate-400">Min Pts:</span>
            <input
              type="number"
              value={minPoints}
              onChange={(e) => setMinPoints(Number.parseInt(e.target.value, 10))}
              className="w-12 bg-transparent text-white font-mono text-center focus:outline-none"
            />
          </div>

          <button
            onClick={handleRunDbscan}
            disabled={running}
            className="py-1.5 px-4 bg-purple-600 hover:bg-purple-500 disabled:bg-purple-800 text-white rounded-xl text-xs font-bold transition flex items-center gap-2"
          >
            {running ? <CircleNotchIcon size={14} className="animate-spin" /> : <BrainIcon size={14} weight="bold" />}
            <span>Run DBSCAN</span>
          </button>
        </div>
      </div>

      {/* Clusters Grid */}
      <div className="mt-5 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {clusters.length === 0 ? (
          <div className="col-span-full text-center py-12 text-slate-500 text-sm">
            No clusters registered in spatial database.
          </div>
        ) : (
          clusters.map((c) => {
            const badgeBg = getClusterBadgeStyle(c);

            return (
              <div
                key={c.id}
                className="bg-slate-950/80 border border-slate-800 rounded-xl p-4 shadow flex flex-col justify-between hover:border-slate-700 transition"
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="font-mono font-bold text-sm text-white">{c.clusterCode}</span>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded border uppercase ${badgeBg}`}>
                      {c.status}
                    </span>
                  </div>

                  <div className="space-y-2 text-xs text-slate-300 my-3">
                    <div>
                      <span className="text-slate-500 block text-[11px]">Correlated Root Cause:</span>
                      <strong className="text-slate-200">
                        {c.correlatedRootCause || "Ductile Iron Joint Rupture"}
                      </strong>
                    </div>

                    <div className="flex items-center gap-1.5 text-slate-400">
                      <UsersIcon size={14} className="text-blue-400" />
                      <span>
                        Est. Population Affected:{" "}
                        <strong className="text-slate-200">
                          {(c.estimatedAffectedPopulation || 15000).toLocaleString()}
                        </strong>
                      </span>
                    </div>

                    <div className="text-[11px] text-slate-500 font-mono">
                      Radius: {c.radiusMeters}m • Incidents: {c.incidentCount}
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
                    onClick={() => handleEscalate(c)}
                    disabled={escalatingId === c.id || c.status === "ESCALATED"}
                    className="py-1.5 px-3 bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/40 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition disabled:opacity-50"
                  >
                    {escalatingId === c.id ? (
                      <CircleNotchIcon size={14} className="animate-spin" />
                    ) : (
                      <LightningIcon size={14} weight="fill" />
                    )}
                    <span>{c.status === "ESCALATED" ? "Escalated" : "Escalate"}</span>
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
