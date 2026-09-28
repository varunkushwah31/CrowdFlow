import React, { useState } from "react";
import { adminApi } from "../../services/api";
import { useAuth } from "../../context/AuthContext";
import type { SystemDiagnostics, WaterReport } from "../../types";
import {
  CpuIcon,
  BroomIcon,
  RadioIcon,
  CircleNotchIcon,
  ArrowClockwiseIcon
} from "@phosphor-icons/react";

interface AdminDiagnosticsProps {
  diagnostics: SystemDiagnostics | null;
  onRefresh: () => void;
}

export const AdminDiagnostics: React.FC<AdminDiagnosticsProps> = ({ diagnostics, onRefresh }) => {
  const { showToast } = useAuth();

  const [cacheName, setCacheName] = useState("crowdflow:cache:reports");
  const [clearingCache, setClearingCache] = useState(false);

  const [geoLat, setGeoLat] = useState("28.6445");
  const [geoLon, setGeoLon] = useState("77.1950");
  const [geoRadius, setGeoRadius] = useState(5);
  const [queryingGeo, setQueryingGeo] = useState(false);
  const [geoResults, setGeoResults] = useState<{ count: number; nearbyReports: WaterReport[] } | null>(null);

  const handleClearCache = async () => {
    setClearingCache(true);
    try {
      const res = await adminApi.clearCache(cacheName);
      showToast(res.message || `Cache ${cacheName} purged successfully`, "success");
      onRefresh();
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Cache purge failed: ${error.message}`, "error");
    } finally {
      setClearingCache(false);
    }
  };

  const handleGeoQuery = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    setQueryingGeo(true);
    try {
      const res = await adminApi.geoNearby(Number.parseFloat(geoLat), Number.parseFloat(geoLon), geoRadius);
      setGeoResults(res);
      showToast(`Redis Geo-query found ${res.count} reports within ${geoRadius}km!`, "success");
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Geo-query failed: ${error.message}`, "error");
    } finally {
      setQueryingGeo(false);
    }
  };

  return (
    <div className="space-y-6 text-slate-100">
      {/* Telemetry Dashboard */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-purple-600/20 border border-purple-500/40 flex items-center justify-center text-purple-400">
              <CpuIcon size={22} weight="fill" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white">
                Spring Boot 3.4 &amp; Java 25 Platform Telemetry
              </h2>
              <p className="text-xs text-slate-400">
                Virtual threads, database pool metrics, and Redis 7.2 geospatial clustering state
              </p>
            </div>
          </div>

          <button
            onClick={onRefresh}
            className="text-xs text-purple-400 hover:text-purple-300 flex items-center gap-1.5 py-1.5 px-3 bg-slate-950 border border-slate-800 rounded-xl"
          >
            <ArrowClockwiseIcon size={14} />
            <span>Refresh Telemetry</span>
          </button>
        </div>

        {diagnostics ? (
          <div className="mt-5 grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
              <span className="text-[11px] text-slate-400 block">JVM Uptime</span>
              <strong className="text-sm font-mono text-white block mt-1">
                {diagnostics.uptime || "Operational"}
              </strong>
            </div>

            <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
              <span className="text-[11px] text-slate-400 block">Virtual Threads</span>
              <strong className="text-sm font-mono text-emerald-400 block mt-1">
                {diagnostics.virtualThreadsEnabled ? "Enabled (Java 25)" : "Standard"}
              </strong>
            </div>

            <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
              <span className="text-[11px] text-slate-400 block">Database Pool (Active/Idle)</span>
              <strong className="text-sm font-mono text-blue-400 block mt-1">
                {diagnostics.dbPoolActive || 2} / {diagnostics.dbPoolIdle || 10}
              </strong>
            </div>

            <div className="p-3 bg-slate-950 rounded-xl border border-slate-800">
              <span className="text-[11px] text-slate-400 block">Redis Geo Status</span>
              <strong
                className={`text-sm font-mono block mt-1 ${
                  diagnostics.redisConnected ? "text-emerald-400" : "text-rose-400"
                }`}
              >
                {diagnostics.redisConnected ? "Connected (Healthy)" : "Disconnected"}
              </strong>
            </div>
          </div>
        ) : (
          <div className="text-center py-6 text-slate-500 text-xs">
            Diagnostics data initializing...
          </div>
        )}
      </div>

      {/* Cache & Geo Proximity Operations */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Cache Invalidation */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow">
          <h3 className="font-bold text-white text-sm mb-3 flex items-center gap-2">
            <BroomIcon size={18} className="text-purple-400" />
            <span>Redis Cache Management &amp; Eviction</span>
          </h3>
          <p className="text-xs text-slate-400 mb-4">
            Evict municipal cache regions to force immediate geospatial re-clustering and ward synchronization.
          </p>

          <div className="flex gap-2">
            <select
              value={cacheName}
              onChange={(e) => setCacheName(e.target.value)}
              className="flex-1 px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-xs text-white"
            >
              <option value="crowdflow:cache:reports">crowdflow:cache:reports</option>
              <option value="crowdflow:cache:clusters">crowdflow:cache:clusters</option>
              <option value="crowdflow:cache:wards">crowdflow:cache:wards</option>
              <option value="ALL">ALL REGIONS (Flush Entire Redis Cache)</option>
            </select>

            <button
              onClick={handleClearCache}
              disabled={clearingCache}
              className="py-2 px-4 bg-rose-600 hover:bg-rose-500 disabled:bg-rose-800 text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5"
            >
              {clearingCache ? <CircleNotchIcon size={14} className="animate-spin" /> : <BroomIcon size={14} />}
              <span>Evict Cache</span>
            </button>
          </div>
        </div>

        {/* Redis Geo Proximity Query */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow">
          <h3 className="font-bold text-white text-sm mb-3 flex items-center gap-2">
            <RadioIcon size={18} className="text-blue-400" />
            <span>Redis GEORADIUS Spatial Proximity Probe</span>
          </h3>
          <p className="text-xs text-slate-400 mb-4">
            Query incidents within a spatial radius using high-speed Redis geospatial indices.
          </p>

          <form onSubmit={handleGeoQuery} className="grid grid-cols-3 gap-2">
            <div>
              <label htmlFor="diag-geo-lat" className="text-[10px] text-slate-400 block mb-1">Lat</label>
              <input
                id="diag-geo-lat"
                type="text"
                value={geoLat}
                onChange={(e) => setGeoLat(e.target.value)}
                className="w-full px-2 py-1.5 bg-slate-950 border border-slate-700 rounded-lg text-xs font-mono text-white"
              />
            </div>
            <div>
              <label htmlFor="diag-geo-lon" className="text-[10px] text-slate-400 block mb-1">Lon</label>
              <input
                id="diag-geo-lon"
                type="text"
                value={geoLon}
                onChange={(e) => setGeoLon(e.target.value)}
                className="w-full px-2 py-1.5 bg-slate-950 border border-slate-700 rounded-lg text-xs font-mono text-white"
              />
            </div>
            <div>
              <label htmlFor="diag-geo-radius" className="text-[10px] text-slate-400 block mb-1">Radius (km)</label>
              <input
                id="diag-geo-radius"
                type="number"
                value={geoRadius}
                onChange={(e) => setGeoRadius(Number.parseInt(e.target.value, 10))}
                className="w-full px-2 py-1.5 bg-slate-950 border border-slate-700 rounded-lg text-xs font-mono text-white"
              />
            </div>
            <div className="col-span-3 mt-1">
              <button
                type="submit"
                disabled={queryingGeo}
                className="w-full py-2 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-800 text-white rounded-xl text-xs font-bold transition flex items-center justify-center gap-1.5"
              >
                {queryingGeo ? <CircleNotchIcon size={14} className="animate-spin" /> : <RadioIcon size={14} />}
                <span>Probe Spatial Radius</span>
              </button>
            </div>
          </form>

          {geoResults && (
            <div className="mt-3 p-3 bg-slate-950 rounded-xl border border-slate-800 text-xs">
              <span className="font-semibold text-emerald-400">
                Found {geoResults.count} reports within {geoRadius}km radius
              </span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
