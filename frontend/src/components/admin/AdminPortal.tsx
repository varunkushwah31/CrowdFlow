import React, { useState, useEffect, useCallback } from "react";
import { useAuth, AUTHORITY_PRESETS } from "../../context/AuthContext";
import { adminApi, wardsApi } from "../../services/api";
import type {
  ExecutiveOverview,
  WaterReport,
  IncidentCluster,
  MunicipalWard,
  WaterTankerUnit,
  EmergencyDepot,
  AuditLog,
  SystemDiagnostics
} from "../../types";
import { AdminOverview } from "./AdminOverview";
import { AdminTriage } from "./AdminTriage";
import { AdminClusters } from "./AdminClusters";
import { AdminFleet } from "./AdminFleet";
import { AdminWards } from "./AdminWards";
import { AdminAudit } from "./AdminAudit";
import { AdminDiagnostics } from "./AdminDiagnostics";
import type { AdminTab } from "../common/Navbar";
import {
  ShieldCheckIcon,
  ShieldWarningIcon,
  CircleNotchIcon,
  ArrowClockwiseIcon,
  SparkleIcon
} from "@phosphor-icons/react";

interface AdminPortalProps {
  activeTab: AdminTab;
  onTabChange: (tab: AdminTab) => void;
  onOpenAuth: () => void;
}

export const AdminPortal: React.FC<AdminPortalProps> = ({
  activeTab,
  onTabChange,
  onOpenAuth
}) => {
  const { auth, isAdmin, selectPreset, showToast } = useAuth();

  const [overview, setOverview] = useState<ExecutiveOverview | null>(null);
  const [reports, setReports] = useState<WaterReport[]>([]);
  const [clusters, setClusters] = useState<IncidentCluster[]>([]);
  const [wards, setWards] = useState<MunicipalWard[]>([]);
  const [tankers, setTankers] = useState<WaterTankerUnit[]>([]);
  const [depots, setDepots] = useState<EmergencyDepot[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [diagnostics, setDiagnostics] = useState<SystemDiagnostics | null>(null);
  const [loading, setLoading] = useState(false);

  const loadAdminData = useCallback(async () => {
    if (!isAdmin) return;

    setLoading(true);
    try {
      const [ov, reps, clusts, wrds, flt, aud, diag] = await Promise.all([
        adminApi.getOverview().catch(() => null),
        adminApi.getReports().catch(() => []),
        adminApi.getClusters().catch(() => []),
        wardsApi.getAll().catch(() => []),
        adminApi.getFleetStatus().catch(() => ({ tankers: [], depots: [] })),
        adminApi.getAuditLogs().catch(() => []),
        adminApi.getDiagnostics().catch(() => null)
      ]);

      if (ov) setOverview(ov);
      setReports(reps);
      setClusters(clusts);
      setWards(wrds);
      setTankers(flt.tankers || []);
      setDepots(flt.depots || []);
      setAuditLogs(aud);
      setDiagnostics(diag);
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Error synchronizing command data: ${error.message}`, "error");
    } finally {
      setLoading(false);
    }
  }, [isAdmin, showToast]);

  useEffect(() => {
    loadAdminData();
  }, [loadAdminData]);

  // If user is not authenticated or is just a CITIZEN, display authority access barrier
  if (!isAdmin) {
    return (
      <div className="bg-slate-900 border border-slate-800 rounded-3xl p-8 max-w-xl mx-auto text-center shadow-2xl space-y-6 text-slate-100 my-8">
        <div className="w-16 h-16 rounded-2xl bg-purple-600/20 border border-purple-500/40 flex items-center justify-center text-purple-400 mx-auto">
          <ShieldWarningIcon size={36} weight="bold" />
        </div>

        <div>
          <h2 className="text-xl font-extrabold text-white">Government Authority Restricted Area</h2>
          <p className="text-xs text-slate-400 mt-1.5 max-w-md mx-auto">
            Access to spatial triage, AMRUT 2.0 escalations, and emergency fleet dispatch is strictly
            restricted to Delhi Jal Board &amp; MCD Municipal Ward Officers (ROLE_WARD_OFFICER, ROLE_SUPER_ADMIN).
          </p>
        </div>

        {/* Quick Demo Access Buttons */}
        <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800 text-left space-y-2.5">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-400 uppercase tracking-wider">
            <SparkleIcon size={14} className="text-amber-400" weight="fill" />
            <span>Select an Authority Profile to enter:</span>
          </div>

          {AUTHORITY_PRESETS.filter((p) => p.role !== "ROLE_CITIZEN").map((p) => (
            <button
              key={p.phone}
              onClick={() => selectPreset(p)}
              className="w-full flex items-center justify-between p-3 rounded-xl border border-slate-800 bg-slate-900/80 hover:border-purple-500 hover:bg-slate-850 text-left transition group"
            >
              <div>
                <div className="text-sm font-semibold text-white group-hover:text-purple-300 flex items-center gap-2">
                  <span>{p.name}</span>
                  <span className="text-[10px] px-2 py-0.5 rounded font-mono font-medium bg-purple-950 text-purple-300 border border-purple-800">
                    {p.role.replace("ROLE_", "")}
                  </span>
                </div>
                <div className="text-xs text-slate-400 mt-0.5">{p.designation}</div>
              </div>
              <ShieldCheckIcon size={20} className="text-slate-500 group-hover:text-purple-400 transition" />
            </button>
          ))}
        </div>

        <div>
          <button
            onClick={onOpenAuth}
            className="py-2.5 px-6 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold transition shadow-lg shadow-blue-600/20"
          >
            Sign in with custom mobile number (SMS OTP)
          </button>
        </div>
      </div>
    );
  }

  const adminTabs: { id: AdminTab; label: string }[] = [
    { id: "overview", label: "Executive Overview" },
    { id: "triage", label: "Incident Triage" },
    { id: "clusters", label: "Spatial Clusters" },
    { id: "fleet", label: "Tanker Fleet & Depots" },
    { id: "wards", label: "Wards Directory" },
    { id: "audit", label: "Compliance & Audit" },
    { id: "diagnostics", label: "Platform Diagnostics" }
  ];

  return (
    <div className="space-y-6">
      {/* Admin Top Status Bar */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl px-5 py-3.5 shadow flex flex-wrap items-center justify-between gap-3 text-xs">
        <div className="flex items-center gap-3">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
          <span className="font-bold text-slate-200">
            Delhi Jal Board Central Command Stream (Live)
          </span>
          <span className="text-slate-500 hidden sm:inline">•</span>
          <span className="text-slate-400 hidden sm:inline">
            Logged in as <strong className="text-white">{auth?.name || auth?.phoneNumber}</strong> ({auth?.role})
          </span>
        </div>

        <div className="flex items-center gap-3">
          <div className="hidden lg:flex items-center gap-1 bg-slate-950 p-1 rounded-xl border border-slate-800">
            {adminTabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => onTabChange(tab.id)}
                className={`px-2.5 py-1 rounded-lg text-xs font-medium transition ${
                  activeTab === tab.id
                    ? "bg-purple-600 text-white shadow"
                    : "text-slate-400 hover:text-white"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          <button
            onClick={loadAdminData}
            disabled={loading}
            className="text-slate-400 hover:text-white flex items-center gap-1.5 py-1.5 px-3 rounded-xl bg-slate-950 border border-slate-800 transition"
          >
            <ArrowClockwiseIcon size={14} className={loading ? "animate-spin" : ""} />
            <span>Sync Telemetry</span>
          </button>
        </div>
      </div>

      {loading && !overview ? (
        <div className="flex flex-col items-center justify-center py-20 text-slate-400 gap-3">
          <CircleNotchIcon size={36} className="animate-spin text-purple-500" />
          <p className="text-sm font-medium">Bootstrapping Municipal Control Room...</p>
        </div>
      ) : (
        <>
          {activeTab === "overview" && <AdminOverview overview={overview} />}
          {activeTab === "triage" && (
            <AdminTriage reports={reports} onRefresh={loadAdminData} />
          )}
          {activeTab === "clusters" && (
            <AdminClusters clusters={clusters} onRefresh={loadAdminData} />
          )}
          {activeTab === "fleet" && (
            <AdminFleet tankers={tankers} depots={depots} onRefresh={loadAdminData} />
          )}
          {activeTab === "wards" && <AdminWards wards={wards} />}
          {activeTab === "audit" && <AdminAudit logs={auditLogs} />}
          {activeTab === "diagnostics" && (
            <AdminDiagnostics diagnostics={diagnostics} onRefresh={loadAdminData} />
          )}
        </>
      )}
    </div>
  );
};
