import React, { useState } from "react";
import { adminApi } from "../../services/api";
import { useAuth } from "../../context/AuthContext";
import type { WaterTankerUnit, EmergencyDepot, DispatchRoutePlan } from "../../types";
import {
  TruckIcon,
  CircleNotchIcon,
  GasPumpIcon,
  PathIcon,
  XIcon,
  NavigationArrowIcon,
  ArrowClockwiseIcon
} from "@phosphor-icons/react";

interface AdminFleetProps {
  tankers: WaterTankerUnit[];
  depots: EmergencyDepot[];
  onRefresh: () => void;
}

export const AdminFleet: React.FC<AdminFleetProps> = ({ tankers, depots, onRefresh }) => {
  const { showToast } = useAuth();

  const [dispatchLat, setDispatchLat] = useState("28.644500");
  const [dispatchLon, setDispatchLon] = useState("77.195000");
  const [incidentDesc, setIncidentDesc] = useState("Hospital ICU Water Cessation - Pusa Road");
  const [calculating, setCalculating] = useState(false);
  const [routePlan, setRoutePlan] = useState<DispatchRoutePlan | null>(null);

  const handleCalculateRoute = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    setCalculating(true);
    try {
      const plan = await adminApi.calculateRoute(
        Number.parseFloat(dispatchLat),
        Number.parseFloat(dispatchLon),
        incidentDesc
      );
      setRoutePlan(plan);
      showToast(
        `Optimized Route Dispatched: Tanker ${plan.vehicleNumber} from ${plan.depotName} ETA: ${plan.estimatedArrivalMinutes}m (${plan.distanceKm} km)`,
        "success"
      );
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Fleet routing error: ${error.message}`, "error");
    } finally {
      setCalculating(false);
    }
  };

  return (
    <div className="space-y-6 text-slate-100">
      {/* Fleet Routing Dispatcher */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-purple-600/20 border border-purple-500/40 flex items-center justify-center text-purple-400">
              <NavigationArrowIcon size={22} weight="fill" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">
                Emergency Water Tanker Fleet Dispatch &amp; Telemetry
              </h2>
              <p className="text-xs text-slate-400">
                Automated closest-depot distance computation and shortest-path GIS routing for emergency water delivery
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onRefresh}
            className="text-xs text-purple-400 hover:text-purple-300 flex items-center gap-1.5 py-1.5 px-3 bg-slate-950 border border-slate-800 rounded-xl transition"
          >
            <ArrowClockwiseIcon size={14} />
            <span>Refresh Fleet</span>
          </button>
        </div>

        <form onSubmit={handleCalculateRoute} className="mt-5 grid grid-cols-1 sm:grid-cols-4 gap-3">
          <div>
            <label htmlFor="fleet-dispatch-lat" className="text-[11px] text-slate-400 block mb-1">Target Latitude</label>
            <input
              id="fleet-dispatch-lat"
              type="text"
              required
              value={dispatchLat}
              onChange={(e) => setDispatchLat(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-xs font-mono text-white"
            />
          </div>
          <div>
            <label htmlFor="fleet-dispatch-lon" className="text-[11px] text-slate-400 block mb-1">Target Longitude</label>
            <input
              id="fleet-dispatch-lon"
              type="text"
              required
              value={dispatchLon}
              onChange={(e) => setDispatchLon(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-xs font-mono text-white"
            />
          </div>
          <div>
            <label htmlFor="fleet-dispatch-desc" className="text-[11px] text-slate-400 block mb-1">Incident Objective</label>
            <input
              id="fleet-dispatch-desc"
              type="text"
              value={incidentDesc}
              onChange={(e) => setIncidentDesc(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-xs text-white"
            />
          </div>
          <div className="flex items-end">
            <button
              type="submit"
              disabled={calculating}
              className="w-full py-2 px-4 bg-purple-600 hover:bg-purple-500 disabled:bg-purple-800 text-white rounded-xl text-xs font-bold transition flex items-center justify-center gap-1.5 shadow-md shadow-purple-600/20"
            >
              {calculating ? (
                <CircleNotchIcon size={14} className="animate-spin" />
              ) : (
                <TruckIcon size={14} weight="bold" />
              )}
              <span>Calculate Fleet Route</span>
            </button>
          </div>
        </form>
      </div>

      {/* Depots & Tanker Units Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Depots */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow">
          <h3 className="font-bold text-white text-sm mb-3 flex items-center gap-2">
            <GasPumpIcon size={18} className="text-purple-400" />
            <span>Emergency Depots &amp; Reservoirs ({depots.length})</span>
          </h3>
          <div className="space-y-3">
            {depots.map((d) => (
              <div
                key={d.id}
                className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-xs flex items-center justify-between"
              >
                <div>
                  <strong className="text-slate-200 block">{d.depotName}</strong>
                  <span className="text-slate-400 text-[11px]">
                    Ward #{d.wardNumber} • Hotline: {d.contactNumber}
                  </span>
                </div>
                <div className="text-right">
                  <span className="text-sm font-bold font-mono text-emerald-400 block">
                    {d.availableTankers} / {d.totalTankers}
                  </span>
                  <span className="text-[10px] text-slate-500 uppercase">Available Units</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Tankers */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow">
          <h3 className="font-bold text-white text-sm mb-3 flex items-center gap-2">
            <TruckIcon size={18} className="text-blue-400" />
            <span>Active Water Tanker Fleet Units ({tankers.length})</span>
          </h3>
          <div className="space-y-3 max-h-80 overflow-y-auto pr-1">
            {tankers.map((t) => (
              <div
                key={t.id}
                className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-xs flex items-center justify-between"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <strong className="text-slate-200 font-mono">{t.vehicleNumber}</strong>
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-blue-950 text-blue-300 border border-blue-800">
                      {t.status}
                    </span>
                  </div>
                  <span className="text-slate-400 text-[11px] block mt-0.5">
                    Driver: {t.driverName} ({t.driverPhone})
                  </span>
                </div>
                <div className="text-right font-mono">
                  <span className="text-xs font-bold text-slate-300 block">
                    {t.capacityLiters.toLocaleString()} L
                  </span>
                  <span className="text-[10px] text-slate-500">
                    {t.currentLatitude?.toFixed(4)}, {t.currentLongitude?.toFixed(4)}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Route Plan Modal */}
      {routePlan && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="font-bold text-white text-sm flex items-center gap-2">
                <PathIcon size={18} className="text-emerald-400" />
                <span>Computed GIS Dispatch Route Plan</span>
              </h3>
              <button onClick={() => setRoutePlan(null)} className="text-slate-400 hover:text-white">
                <XIcon size={20} />
              </button>
            </div>

            <div className="mt-4 space-y-3 text-xs">
              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 space-y-2">
                <div className="flex justify-between">
                  <span className="text-slate-400">Assigned Unit:</span>
                  <strong className="text-slate-200 font-mono">{routePlan.vehicleNumber}</strong>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Depot of Origin:</span>
                  <strong className="text-slate-200">{routePlan.depotName}</strong>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Shortest Road Distance:</span>
                  <strong className="text-emerald-400 font-mono">{routePlan.distanceKm} km</strong>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Estimated Field Arrival (ETA):</span>
                  <strong className="text-amber-400 font-mono">
                    {routePlan.estimatedArrivalMinutes} Minutes
                  </strong>
                </div>
              </div>

              <div className="p-3 bg-slate-950 rounded-xl border border-slate-800 text-[11px] text-slate-400">
                Waypoints:{" "}
                <span className="font-mono text-slate-300">
                  {routePlan.waypoints?.length || 2} nodes computed along Delhi Municipal arterial
                  routes.
                </span>
              </div>

              <div className="pt-2 flex justify-end">
                <button
                  onClick={() => setRoutePlan(null)}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold"
                >
                  Acknowledge &amp; Dispatch Unit
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
