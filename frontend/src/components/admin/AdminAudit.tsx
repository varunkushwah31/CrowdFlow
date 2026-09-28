import React, { useState } from "react";
import type { AuditLog } from "../../types";
import {EyeIcon, ScrollIcon, XIcon} from "@phosphor-icons/react";

interface AdminAuditProps {
  logs: AuditLog[];
}

export const AdminAudit: React.FC<AdminAuditProps> = ({ logs }) => {
  const [selectedAudit, setSelectedAudit] = useState<AuditLog | null>(null);

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl text-slate-100">
      <div className="flex items-center justify-between pb-5 border-b border-slate-800">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-purple-600/20 border border-purple-500/40 flex items-center justify-center text-purple-400">
            <ScrollIcon size={22} weight="fill" />
          </div>
          <div>
            <h2 className="text-base font-bold text-white">
              Municipal Compliance &amp; Tamper-Evident Audit Trail
            </h2>
            <p className="text-xs text-slate-400">
              Immutable logging of all administrative status overrides, ward reassignments, and fleet dispatches
            </p>
          </div>
        </div>

        <span className="text-xs text-slate-400 font-mono">
          Total Logged Actions: <strong>{logs.length}</strong>
        </span>
      </div>

      <div className="mt-5 overflow-x-auto">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b border-slate-800 text-slate-400 uppercase text-[10px] tracking-wider">
              <th className="py-2.5 px-3">Timestamp (IST)</th>
              <th className="py-2.5 px-3">Action</th>
              <th className="py-2.5 px-3">Performed By</th>
              <th className="py-2.5 px-3">Authority Role</th>
              <th className="py-2.5 px-3">Details / Summary</th>
              <th className="py-2.5 px-3 text-right">Inspect</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-medium">
            {logs.length === 0 ? (
              <tr>
                <td colSpan={6} className="py-12 text-center text-slate-500">
                  No administrative audit events recorded yet.
                </td>
              </tr>
            ) : (
              logs.map((log) => (
                <tr key={log.id} className="hover:bg-slate-950/60 transition">
                  <td className="py-3 px-3 font-mono text-slate-400 text-[11px]">
                    {new Date(log.timestamp).toLocaleString()}
                  </td>
                  <td className="py-3 px-3">
                    <span className="px-2 py-0.5 rounded font-mono text-[10px] font-bold uppercase bg-slate-800 text-slate-200 border border-slate-700">
                      {log.action}
                    </span>
                  </td>
                  <td className="py-3 px-3 text-white font-semibold">{log.performedBy}</td>
                  <td className="py-3 px-3">
                    <span className="text-[10px] font-mono font-medium px-2 py-0.5 rounded bg-purple-950 text-purple-300 border border-purple-800">
                      {log.role}
                    </span>
                  </td>
                  <td className="py-3 px-3 text-slate-300 truncate max-w-xs">{log.details}</td>
                  <td className="py-3 px-3 text-right">
                    <button
                      onClick={() => setSelectedAudit(log)}
                      className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition"
                    >
                      <EyeIcon size={15} />
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Inspect Audit Modal */}
      {selectedAudit && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="font-bold text-white text-sm">Audit Record #{selectedAudit.id}</h3>
              <button
                onClick={() => setSelectedAudit(null)}
                className="text-slate-400 hover:text-white"
              >
                <XIcon size={20} />
              </button>
            </div>

            <div className="mt-4 space-y-3 text-xs">
              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 space-y-2 font-mono text-[11px]">
                <div>
                  <span className="text-slate-500 block">Timestamp:</span>
                  <strong className="text-slate-200">
                    {new Date(selectedAudit.timestamp).toUTCString()}
                  </strong>
                </div>
                <div>
                  <span className="text-slate-500 block">Action Type:</span>
                  <strong className="text-purple-400">{selectedAudit.action}</strong>
                </div>
                <div>
                  <span className="text-slate-500 block">Performed By:</span>
                  <strong className="text-slate-200">
                    {selectedAudit.performedBy} ({selectedAudit.role})
                  </strong>
                </div>
                {selectedAudit.ipAddress && (
                  <div>
                    <span className="text-slate-500 block">Origin IP:</span>
                    <strong className="text-slate-200">{selectedAudit.ipAddress}</strong>
                  </div>
                )}
              </div>

              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                <span className="text-slate-500 block mb-1">Payload / Details:</span>
                <p className="text-slate-200 leading-relaxed font-mono text-[11px]">
                  {selectedAudit.details}
                </p>
              </div>

              <div className="pt-2 flex justify-end">
                <button
                  onClick={() => setSelectedAudit(null)}
                  className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white rounded-xl text-xs font-semibold"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
