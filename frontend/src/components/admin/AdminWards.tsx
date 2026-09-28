import React, { useState } from "react";
import { wardsApi } from "../../services/api";
import { useAuth } from "../../context/AuthContext";
import type { MunicipalWard } from "../../types";
import {
  BuildingsIcon,
  MapPinIcon,
  MagnifyingGlassIcon,
  CircleNotchIcon
} from "@phosphor-icons/react";

interface AdminWardsProps {
  wards: MunicipalWard[];
}

export const AdminWards: React.FC<AdminWardsProps> = ({ wards }) => {
  const { showToast } = useAuth();

  const [lookupLat, setLookupLat] = useState("28.6445");
  const [lookupLon, setLookupLon] = useState("77.1950");
  const [lookupResult, setLookupResult] = useState<MunicipalWard | null>(null);
  const [lookingUp, setLookingUp] = useState(false);

  const handleLookup = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLookingUp(true);
    try {
      const res = await wardsApi.lookup(Number.parseFloat(lookupLat), Number.parseFloat(lookupLon));
      setLookupResult(res);
      showToast(
        `Point resolved to ${res.wardName} (Ward #${res.wardNumber}) under ${res.officerName}`,
        "success"
      );
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Ward lookup failed: ${error.message}`, "error");
    } finally {
      setLookingUp(false);
    }
  };

  return (
    <div className="space-y-6 text-slate-100">
      {/* Geocoding Lookup Tool */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
        <div className="flex items-center gap-3 pb-4 border-b border-slate-800">
          <div className="w-10 h-10 rounded-xl bg-purple-600/20 border border-purple-500/40 flex items-center justify-center text-purple-400">
            <MapPinIcon size={22} weight="fill" />
          </div>
          <div>
            <h2 className="text-base font-bold text-white">
              Municipal Polygon Geocoding &amp; Spatial Jurisdiction Lookup
            </h2>
            <p className="text-xs text-slate-400">
              Test spatial point-in-polygon matching to identify assigned Delhi Jal Board Executive Engineers
            </p>
          </div>
        </div>

        <form onSubmit={handleLookup} className="mt-4 flex flex-wrap items-end gap-3">
          <div>
            <label htmlFor="ward-lookup-lat" className="text-[11px] text-slate-400 block mb-1">Latitude</label>
            <input
              id="ward-lookup-lat"
              type="text"
              required
              value={lookupLat}
              onChange={(e) => setLookupLat(e.target.value)}
              className="px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-xs font-mono text-white w-32"
            />
          </div>
          <div>
            <label htmlFor="ward-lookup-lon" className="text-[11px] text-slate-400 block mb-1">Longitude</label>
            <input
              id="ward-lookup-lon"
              type="text"
              required
              value={lookupLon}
              onChange={(e) => setLookupLon(e.target.value)}
              className="px-3 py-2 bg-slate-950 border border-slate-700 rounded-xl text-xs font-mono text-white w-32"
            />
          </div>
          <button
            type="submit"
            disabled={lookingUp}
            className="py-2 px-4 bg-purple-600 hover:bg-purple-500 disabled:bg-purple-800 text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5"
          >
            {lookingUp ? <CircleNotchIcon size={14} className="animate-spin" /> : <MagnifyingGlassIcon size={14} />}
            <span>Lookup Jurisdiction</span>
          </button>
        </form>

        {lookupResult && (
          <div className="mt-4 p-4 bg-slate-950 rounded-xl border border-slate-800 text-xs grid grid-cols-1 sm:grid-cols-3 gap-3 animate-in fade-in">
            <div>
              <span className="text-slate-500 block">Matched Ward:</span>
              <strong className="text-white text-sm">
                {lookupResult.wardName} (Ward #{lookupResult.wardNumber})
              </strong>
              <div className="text-[11px] text-slate-400">
                {lookupResult.municipalBody} • {lookupResult.zoneName}
              </div>
            </div>
            <div>
              <span className="text-slate-500 block">Executive Engineer:</span>
              <strong className="text-slate-200">{lookupResult.officerName}</strong>
              <div className="text-[11px] text-slate-400">{lookupResult.contactEmail}</div>
            </div>
            <div>
              <span className="text-slate-500 block">Emergency Response Hotline:</span>
              <strong className="text-emerald-400 text-sm">{lookupResult.emergencyHotline}</strong>
            </div>
          </div>
        )}
      </div>

      {/* Full Wards Directory */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
        <h3 className="font-bold text-white text-sm mb-4 flex items-center gap-2">
          <BuildingsIcon size={18} className="text-blue-400" />
          <span>Active Delhi Jal Board &amp; MCD Jurisdictions ({wards.length})</span>
        </h3>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 uppercase text-[10px] tracking-wider">
                <th className="py-2.5 px-3">Ward #</th>
                <th className="py-2.5 px-3">Name &amp; Zone</th>
                <th className="py-2.5 px-3">Municipal Body</th>
                <th className="py-2.5 px-3">Executive Engineer</th>
                <th className="py-2.5 px-3">Helpline</th>
                <th className="py-2.5 px-3">Webhook Endpoint</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-medium">
              {wards.map((w) => (
                <tr key={w.id || w.wardNumber} className="hover:bg-slate-950/60 transition">
                  <td className="py-3 px-3 font-mono font-bold text-slate-200">#{w.wardNumber}</td>
                  <td className="py-3 px-3">
                    <div className="font-semibold text-white">{w.wardName}</div>
                    <div className="text-[11px] text-slate-400">{w.zoneName}</div>
                  </td>
                  <td className="py-3 px-3 text-slate-300">{w.municipalBody}</td>
                  <td className="py-3 px-3">
                    <div className="text-slate-200">{w.officerName}</div>
                    <div className="text-[11px] text-slate-400">{w.contactEmail}</div>
                  </td>
                  <td className="py-3 px-3 font-mono text-emerald-400 font-bold">
                    {w.emergencyHotline}
                  </td>
                  <td className="py-3 px-3 font-mono text-[10px] text-purple-400 truncate max-w-xs">
                    {w.webhookUrl}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
