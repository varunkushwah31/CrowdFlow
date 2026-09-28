import React from "react";
import type { DispatchLog } from "../../types";
import {
  PaperPlaneTiltIcon,
  BroadcastIcon,
  ClockIcon,
  ArrowClockwiseIcon,
  EnvelopeIcon,
  DeviceMobileIcon
} from "@phosphor-icons/react";

interface DispatchLogsViewProps {
  logs: DispatchLog[];
  onRefresh: () => void;
}

function renderChannelIcon(channel: string) {
  if (channel.includes("EMAIL")) {
    return <EnvelopeIcon size={18} className="text-blue-400" />;
  }
  if (channel.includes("WEBHOOK")) {
    return <BroadcastIcon size={18} className="text-purple-400" />;
  }
  return <DeviceMobileIcon size={18} className="text-emerald-400" />;
}

export const DispatchLogsView: React.FC<DispatchLogsViewProps> = ({ logs, onRefresh }) => {
  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl text-slate-100">
      <div className="flex items-center justify-between pb-5 border-b border-slate-800">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/40 flex items-center justify-center text-blue-400">
            <PaperPlaneTiltIcon size={22} weight="fill" />
          </div>
          <div>
            <h2 className="text-base font-bold text-white">Automated Municipal Dispatch Telemetry</h2>
            <p className="text-xs text-slate-400">
              Real-time audit log of SMS, Email, and Webhook dispatches to Delhi Jal Board field teams
            </p>
          </div>
        </div>

        <button
          onClick={onRefresh}
          className="text-xs text-blue-400 hover:text-blue-300 flex items-center gap-1.5 py-1.5 px-3 bg-slate-950 border border-slate-800 rounded-xl"
        >
          <ArrowClockwiseIcon size={14} />
          <span>Refresh Dispatches</span>
        </button>
      </div>

      <div className="mt-5 space-y-3">
        {logs.length === 0 ? (
          <div className="text-center py-12 text-slate-500 text-sm">
            No automated dispatches logged yet. Dispatches trigger automatically upon DBSCAN cluster escalation.
          </div>
        ) : (
          logs.map((log) => {
            const isDelivered = log.status === "DELIVERED";
            const channel = log.dispatchChannel?.toUpperCase() || "SMS";

            return (
              <div
                key={log.id}
                className="bg-slate-950/80 border border-slate-800 rounded-xl p-4 shadow flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
              >
                <div className="flex items-start gap-3">
                  <div className="p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 shrink-0 mt-0.5">
                    {renderChannelIcon(channel)}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-slate-200 text-sm">
                        {log.dispatchChannel} Dispatch Alert
                      </span>
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded border uppercase ${
                          isDelivered
                            ? "bg-emerald-950 text-emerald-300 border-emerald-800/60"
                            : "bg-amber-950 text-amber-300 border-amber-800/60"
                        }`}
                      >
                        {log.status}
                      </span>
                      {log.clusterCode && (
                        <span className="text-[10px] font-mono text-slate-500">
                          {log.clusterCode}
                        </span>
                      )}
                    </div>
                    <div className="text-slate-400 mt-1">
                      Recipient: <strong className="text-slate-200">{log.recipient}</strong>
                    </div>
                    <div className="text-slate-300 mt-1 bg-slate-900/60 p-2 rounded-lg border border-slate-800/80">
                      {log.payloadSummary}
                    </div>
                  </div>
                </div>

                <div className="text-slate-500 font-mono text-[11px] sm:text-right shrink-0 flex items-center sm:block gap-1">
                  <ClockIcon size={12} className="inline mr-1" />
                  <span>{new Date(log.dispatchedAt).toLocaleString()} IST</span>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
