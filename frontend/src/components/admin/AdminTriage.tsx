import React, { useState } from "react";
import { adminApi } from "../../services/api";
import { resolveApiUrl } from "../../config";
import { useAuth } from "../../context/AuthContext";
import type { WaterReport, ReportStatus } from "../../types";
import {
  EyeIcon,
  PencilSimpleIcon,
  TrashIcon,
  MagnifyingGlassIcon,
  XIcon,
  CircleNotchIcon,
  MapPinIcon
} from "@phosphor-icons/react";

interface AdminTriageProps {
  reports: WaterReport[];
  onRefresh: () => void;
}

function getReportBadgeStyle(r: WaterReport): string {
  if (r.status === "RESOLVED") {
    return "bg-emerald-950 text-emerald-300 border-emerald-800/60";
  }
  if (r.severity === "CRITICAL") {
    return "bg-rose-950 text-rose-300 border-rose-800/60";
  }
  if (r.status === "ESCALATED") {
    return "bg-amber-950 text-amber-300 border-amber-800/60";
  }
  return "bg-blue-950 text-blue-300 border-blue-800/60";
}

export const AdminTriage: React.FC<AdminTriageProps> = ({ reports, onRefresh }) => {
  const { showToast } = useAuth();

  const [statusFilter, setStatusFilter] = useState("ALL");
  const [searchTerm, setSearchTerm] = useState("");
  const [inspectReport, setInspectReport] = useState<WaterReport | null>(null);
  const [statusModalReport, setStatusModalReport] = useState<WaterReport | null>(null);
  const [reassignModalReport, setReassignModalReport] = useState<WaterReport | null>(null);

  const [newStatus, setNewStatus] = useState<ReportStatus>("IN_PROGRESS");
  const [statusNotes, setStatusNotes] = useState("");
  const [targetWard, setTargetWard] = useState(85);
  const [loadingAction, setLoadingAction] = useState(false);

  const filtered = reports.filter((r) => {
    const matchesStatus = statusFilter === "ALL" || r.status === statusFilter;
    const matchesSearch =
      searchTerm.trim() === "" ||
      r.reportCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
      r.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      r.wardNumber.toString().includes(searchTerm) ||
      (r.description?.toLowerCase().includes(searchTerm.toLowerCase()));
    return matchesStatus && matchesSearch;
  });

  const handleDelete = async (r: WaterReport) => {
    if (!window.confirm(`Are you sure you want to delete grievance ${r.reportCode}?`)) {
      return;
    }
    try {
      await adminApi.deleteReport(r.id);
      showToast(`Report ${r.reportCode} removed from intake database`, "info");
      onRefresh();
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Delete failed: ${error.message}`, "error");
    }
  };

  const handleUpdateStatus = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!statusModalReport) return;
    setLoadingAction(true);
    try {
      await adminApi.updateReportStatus(statusModalReport.id, newStatus, statusNotes);
      showToast(
        `Report ${statusModalReport.reportCode} updated to ${newStatus}. Citizen notified.`,
        "success"
      );
      setStatusModalReport(null);
      setStatusNotes("");
      onRefresh();
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Update status failed: ${error.message}`, "error");
    } finally {
      setLoadingAction(false);
    }
  };

  const handleReassignWard = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!reassignModalReport) return;
    setLoadingAction(true);
    try {
      await adminApi.reassignWard(reassignModalReport.id, targetWard);
      showToast(
        `Report ${reassignModalReport.reportCode} reassigned to Municipal Ward #${targetWard}`,
        "success"
      );
      setReassignModalReport(null);
      onRefresh();
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Reassignment failed: ${error.message}`, "error");
    } finally {
      setLoadingAction(false);
    }
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl text-slate-100">
      {/* Header & Controls */}
      <div className="flex flex-wrap items-center justify-between gap-4 pb-5 border-b border-slate-800">
        <div>
          <h2 className="text-base font-bold text-white">Grievance Triage &amp; Management</h2>
          <p className="text-xs text-slate-400">
            Delhi Jal Board Executive Engineer incident review, evidence verification &amp; status workflow
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          {/* Status Filter */}
          <div className="relative">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="pl-3 pr-8 py-1.5 bg-slate-950 border border-slate-700 rounded-xl text-xs text-white focus:outline-none focus:border-purple-500"
            >
              <option value="ALL">All Statuses ({reports.length})</option>
              <option value="SUBMITTED">SUBMITTED</option>
              <option value="CLUSTERED">CLUSTERED</option>
              <option value="ESCALATED">ESCALATED</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="RESOLVED">RESOLVED</option>
              <option value="CLOSED">CLOSED</option>
              <option value="REJECTED">REJECTED</option>
            </select>
          </div>

          {/* Search Input */}
          <div className="relative">
            <MagnifyingGlassIcon size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search code, title, ward..."
              className="pl-8 pr-3 py-1.5 bg-slate-950 border border-slate-700 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-purple-500"
            />
          </div>
        </div>
      </div>

      {/* Triage Table */}
      <div className="mt-5 overflow-x-auto">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b border-slate-800 text-slate-400 uppercase text-[10px] tracking-wider">
              <th className="py-3 px-3">Tracking Code</th>
              <th className="py-3 px-3">Hazard Title &amp; Classification</th>
              <th className="py-3 px-3">Ward</th>
              <th className="py-3 px-3">Severity</th>
              <th className="py-3 px-3">Status</th>
              <th className="py-3 px-3">Reported At</th>
              <th className="py-3 px-3 text-right">Triage Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-medium">
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={7} className="py-12 text-center text-slate-500">
                  No grievances found matching active triage filters.
                </td>
              </tr>
            ) : (
              filtered.map((r) => {
                const isCritical = r.severity === "CRITICAL";
                const badgeBg = getReportBadgeStyle(r);

                return (
                  <tr key={r.id} className="hover:bg-slate-950/60 transition-colors">
                    <td className="py-3 px-3 font-mono font-bold text-slate-200">
                      {r.reportCode}
                    </td>
                    <td className="py-3 px-3 max-w-xs">
                      <div className="font-semibold text-slate-200 truncate">{r.title}</div>
                      <div className="text-[11px] text-slate-400 truncate">
                        {r.issueType.replaceAll('_', " ")}
                      </div>
                    </td>
                    <td className="py-3 px-3 font-mono text-slate-300">#{r.wardNumber}</td>
                    <td className="py-3 px-3">
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded border uppercase ${
                          isCritical
                            ? "bg-rose-950 text-rose-300 border-rose-800/60"
                            : "bg-slate-800 text-slate-300 border-slate-700"
                        }`}
                      >
                        {r.severity}
                      </span>
                    </td>
                    <td className="py-3 px-3">
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded border uppercase ${badgeBg}`}>
                        {r.status}
                      </span>
                    </td>
                    <td className="py-3 px-3 text-slate-400 font-mono text-[11px]">
                      {new Date(r.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-3 px-3 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => setInspectReport(r)}
                          className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition"
                          title="Inspect Evidence & EXIF"
                        >
                          <EyeIcon size={15} />
                        </button>
                        <button
                          onClick={() => {
                            setStatusModalReport(r);
                            setNewStatus(r.status === "SUBMITTED" ? "IN_PROGRESS" : r.status);
                          }}
                          className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition"
                          title="Update Status"
                        >
                          <PencilSimpleIcon size={15} />
                        </button>
                        <button
                          onClick={() => {
                            setReassignModalReport(r);
                            setTargetWard(r.wardNumber);
                          }}
                          className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-purple-300 hover:text-white transition"
                          title="Reassign Ward"
                        >
                          <MapPinIcon size={15} />
                        </button>
                        <button
                          onClick={() => handleDelete(r)}
                          className="p-1.5 rounded-lg bg-slate-800 hover:bg-rose-900/60 text-slate-400 hover:text-rose-400 transition"
                          title="Delete Grievance"
                        >
                          <TrashIcon size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Inspect Modal */}
      {inspectReport && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-lg p-6 shadow-2xl overflow-y-auto max-h-[90vh]">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div>
                <h3 className="font-bold text-white text-base">
                  Grievance Dossier: {inspectReport.reportCode}
                </h3>
                <span className="text-xs text-slate-400">Delhi Jal Board Field Evidence Inspection</span>
              </div>
              <button
                onClick={() => setInspectReport(null)}
                className="text-slate-400 hover:text-white"
              >
                <XIcon size={20} />
              </button>
            </div>

            <div className="mt-4 space-y-4 text-xs">
              {/* Photo Evidence */}
              {inspectReport.imageUrl && (
                <div>
                  <span className="font-bold text-slate-300 block mb-1.5">
                    Sanitized Photo Evidence:
                  </span>
                  <div className="w-full h-52 bg-slate-950 rounded-xl border border-slate-800 overflow-hidden flex items-center justify-center">
                    {inspectReport.imageUrl.startsWith("data:image/svg") ? (
                      <div
                        dangerouslySetInnerHTML={{
                          __html: decodeURIComponent(
                            inspectReport.imageUrl.replace("data:image/svg+xml;utf8,", "")
                          )
                        }}
                        className="w-full h-full flex items-center justify-center"
                      />
                    ) : (
                      <img
                        src={resolveApiUrl(inspectReport.imageUrl)}
                        alt="Evidence"
                        className="w-full h-full object-cover"
                      />
                    )}
                  </div>
                </div>
              )}

              {/* Title & Narrative */}
              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                <span className="font-bold text-slate-300 block mb-1">
                  Title: {inspectReport.title}
                </span>
                <p className="text-slate-400 leading-relaxed">
                  {inspectReport.description || "No narrative details provided."}
                </p>
              </div>

              {/* Hardware EXIF & Coordinates */}
              <div className="grid grid-cols-2 gap-3 p-3 bg-slate-950 rounded-xl border border-slate-800 font-mono text-[11px]">
                <div>
                  <span className="text-slate-500 block">EXIF Latitude:</span>
                  <strong className="text-slate-200">
                    {inspectReport.latitude?.toFixed(6) || "28.644500"}° N
                  </strong>
                </div>
                <div>
                  <span className="text-slate-500 block">EXIF Longitude:</span>
                  <strong className="text-slate-200">
                    {inspectReport.longitude?.toFixed(6) || "77.195000"}° E
                  </strong>
                </div>
                <div>
                  <span className="text-slate-500 block">Camera Sensor:</span>
                  <strong className="text-slate-200">
                    {inspectReport.cameraMake || "Pixel"} {inspectReport.cameraModel || "Mobile Sensor"}
                  </strong>
                </div>
                <div>
                  <span className="text-slate-500 block">Municipal Ward:</span>
                  <strong className="text-blue-400">Ward #{inspectReport.wardNumber}</strong>
                </div>
              </div>

              {/* Citizen Contact */}
              {inspectReport.citizenPhone && (
                <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 text-[11px] font-mono">
                  <span className="text-slate-500 block">Citizen Contact:</span>
                  <strong className="text-emerald-400">{inspectReport.citizenPhone}</strong>
                </div>
              )}

              <div className="pt-2 flex justify-end">
                <button
                  onClick={() => setInspectReport(null)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-xl text-xs font-semibold"
                >
                  Close Dossier
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Change Status Modal */}
      {statusModalReport && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="font-bold text-white text-sm">
                Transition Status: {statusModalReport.reportCode}
              </h3>
              <button
                onClick={() => setStatusModalReport(null)}
                className="text-slate-400 hover:text-white"
              >
                <XIcon size={20} />
              </button>
            </div>

            <form onSubmit={handleUpdateStatus} className="mt-4 space-y-4 text-xs">
              <div>
                <label htmlFor="triage-target-status" className="block text-slate-300 font-semibold mb-1">Target Status</label>
                <select
                  id="triage-target-status"
                  value={newStatus}
                  onChange={(e) => setNewStatus(e.target.value as ReportStatus)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-white text-xs"
                >
                  <option value="IN_PROGRESS">IN_PROGRESS (Dispatched Field Crew)</option>
                  <option value="ESCALATED">ESCALATED (Municipal Engineering Escalation)</option>
                  <option value="RESOLVED">RESOLVED (Piping Repaired &amp; Restored)</option>
                  <option value="CLOSED">CLOSED (Verified by Junior Engineer)</option>
                  <option value="REJECTED">REJECTED (Invalid / Duplicate Grievance)</option>
                </select>
              </div>

              <div>
                <label htmlFor="triage-status-notes" className="block text-slate-300 font-semibold mb-1">
                  Resolution Notes (Dispatched to Citizen via SMS / Live Stream)
                </label>
                <textarea
                  id="triage-status-notes"
                  rows={3}
                  value={statusNotes}
                  onChange={(e) => setStatusNotes(e.target.value)}
                  placeholder="e.g., Replacement of 450mm sleeve completed at Pusa Road. Potable pressure normalized. Helpline: 1916."
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-white text-xs"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setStatusModalReport(null)}
                  className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loadingAction}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-bold flex items-center gap-1.5"
                >
                  {loadingAction && <CircleNotchIcon size={14} className="animate-spin" />}
                  <span>Save Transition</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reassign Ward Modal */}
      {reassignModalReport && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="font-bold text-white text-sm">
                Reassign Ward: {reassignModalReport.reportCode}
              </h3>
              <button
                onClick={() => setReassignModalReport(null)}
                className="text-slate-400 hover:text-white"
              >
                <XIcon size={20} />
              </button>
            </div>

            <form onSubmit={handleReassignWard} className="mt-4 space-y-4 text-xs">
              <div>
                <label htmlFor="triage-target-ward" className="block text-slate-300 font-semibold mb-1">
                  Target Municipal Ward Number
                </label>
                <input
                  id="triage-target-ward"
                  type="number"
                  required
                  value={targetWard}
                  onChange={(e) => setTargetWard(Number.parseInt(e.target.value, 10))}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-white font-mono text-sm"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setReassignModalReport(null)}
                  className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loadingAction}
                  className="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-xl font-bold flex items-center gap-1.5"
                >
                  {loadingAction && <CircleNotchIcon size={14} className="animate-spin" />}
                  <span>Confirm Reassignment</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
