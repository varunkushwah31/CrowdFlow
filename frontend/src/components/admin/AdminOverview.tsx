import React, { useEffect, useRef } from "react";
import Chart from "chart.js/auto";
import type { ExecutiveOverview } from "../../types";
import {
  ChartBarIcon,
  DropIcon,
  WarningCircleIcon,
  BrainIcon,
  TruckIcon,
  ClockIcon
} from "@phosphor-icons/react";

interface AdminOverviewProps {
  overview: ExecutiveOverview | null;
}

export const AdminOverview: React.FC<AdminOverviewProps> = ({ overview }) => {
  const chartCanvasRef = useRef<HTMLCanvasElement | null>(null);
  const chartInstanceRef = useRef<Chart | null>(null);

  useEffect(() => {
    if (!chartCanvasRef.current || !overview) return;

    if (chartInstanceRef.current) {
      chartInstanceRef.current.destroy();
    }

    const ctx = chartCanvasRef.current.getContext("2d");
    if (!ctx) return;

    chartInstanceRef.current = new Chart(ctx, {
      type: "doughnut",
      data: {
        labels: ["Submitted", "Clustered", "Escalated", "In Progress", "Resolved"],
        datasets: [
          {
            data: [
              overview.submittedReports || 0,
              overview.clusteredReports || 0,
              overview.escalatedReports || 0,
              overview.inProgressReports || 0,
              overview.resolvedReports || 0
            ],
            backgroundColor: [
              "#3b82f6", // submitted: blue
              "#8b5cf6", // clustered: purple
              "#f59e0b", // escalated: amber
              "#06b6d4", // in-progress: cyan
              "#10b981"  // resolved: emerald
            ],
            borderColor: "#0f172a",
            borderWidth: 2
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: "bottom",
            labels: {
              color: "#cbd5e1",
              font: { size: 11 }
            }
          }
        }
      }
    });

    return () => {
      if (chartInstanceRef.current) {
        chartInstanceRef.current.destroy();
        chartInstanceRef.current = null;
      }
    };
  }, [overview]);

  if (!overview) {
    return (
      <div className="text-center py-12 text-slate-500">
        Loading executive command metrics...
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* KPI Cards Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span>Total Intake</span>
            <DropIcon size={18} className="text-blue-400" weight="fill" />
          </div>
          <div className="text-2xl font-bold font-mono text-white mt-2">
            {overview.totalReports}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">Grievances registered across NCR</div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span>Spatial Clusters</span>
            <BrainIcon size={18} className="text-amber-400" weight="fill" />
          </div>
          <div className="text-2xl font-bold font-mono text-amber-400 mt-2">
            {overview.activeClusters} <span className="text-sm text-slate-500 font-normal">/ {overview.totalClusters}</span>
          </div>
          <div className="text-[11px] text-slate-500 mt-1">DBSCAN correlated zones</div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span>Average SLA Turnaround</span>
            <ClockIcon size={18} className="text-emerald-400" weight="fill" />
          </div>
          <div className="text-2xl font-bold font-mono text-emerald-400 mt-2">
            {overview.averageSlaMinutes ? `${Math.round(overview.averageSlaMinutes)}m` : "42m"}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">Submission to field dispatch</div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span>Tanker Units Ready</span>
            <TruckIcon size={18} className="text-purple-400" weight="fill" />
          </div>
          <div className="text-2xl font-bold font-mono text-purple-400 mt-2">
            {overview.availableTankers || 8} <span className="text-sm text-slate-500 font-normal">/ {overview.activeTankerUnits || 12}</span>
          </div>
          <div className="text-[11px] text-slate-500 mt-1">Emergency water fleet</div>
        </div>
      </div>

      {/* Analytics Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Status Distribution Chart */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-bold text-white text-sm flex items-center gap-2">
              <ChartBarIcon size={18} className="text-blue-400" />
              <span>Grievance Lifecycle Triage Distribution</span>
            </h3>
            <span className="text-xs text-slate-500">Live Breakdown</span>
          </div>
          <div className="h-64 relative">
            <canvas ref={chartCanvasRef} />
          </div>
        </div>

        {/* Executive Action Directives */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-bold text-white text-sm flex items-center gap-2">
                <WarningCircleIcon size={18} className="text-rose-400" />
                <span>Delhi Jal Board Command Mandates</span>
              </h3>
              <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded bg-rose-950 text-rose-300 border border-rose-800">
                AMRUT 2.0 SLA
              </span>
            </div>

            <div className="space-y-3 text-xs text-slate-300">
              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                <strong className="text-rose-300 block mb-0.5">Critical Feeder Rupture Protocol</strong>
                <p className="text-slate-400">
                  Any spatial cluster exceeding 5 correlated citizen reports triggers automated
                  escalation to the Zonal Chief Engineer and SMS dispatch to the Ward EE.
                </p>
              </div>

              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                <strong className="text-amber-300 block mb-0.5">Emergency Fleet Dispatch Protocol</strong>
                <p className="text-slate-400">
                  In zones experiencing complete supply cessation, water tankers are routed from the
                  nearest municipal depot with automated distance calculation.
                </p>
              </div>

              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
                <strong className="text-emerald-300 block mb-0.5">Citizen Feedback Loop Verification</strong>
                <p className="text-slate-400">
                  Grievances can only be finalized upon citizen satisfaction confirmation or inspection
                  by the ward junior engineer.
                </p>
              </div>
            </div>
          </div>

          <div className="pt-4 border-t border-slate-800/80 text-[11px] text-slate-500 flex items-center justify-between">
            <span>Authority: Delhi Jal Board (Govt of NCT of Delhi)</span>
            <span className="font-mono text-emerald-400">● 100% Operational</span>
          </div>
        </div>
      </div>
    </div>
  );
};
