import React, { useState } from "react";
import type { MunicipalWard } from "../../types";
import {
  BuildingsIcon,
  UserIcon,
  EnvelopeIcon,
  PhoneIcon,
  BroadcastIcon,
  MagnifyingGlassIcon
} from "@phosphor-icons/react";

interface WardsViewProps {
  wards: MunicipalWard[];
}

export const WardsView: React.FC<WardsViewProps> = ({ wards }) => {
  const [search, setSearch] = useState("");

  const filtered = wards.filter(
    (w) =>
      w.wardName.toLowerCase().includes(search.toLowerCase()) ||
      w.officerName.toLowerCase().includes(search.toLowerCase()) ||
      w.zoneName.toLowerCase().includes(search.toLowerCase()) ||
      w.wardNumber.toString().includes(search)
  );

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl text-slate-100">
      <div className="flex flex-wrap items-center justify-between gap-4 pb-5 border-b border-slate-800">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/40 flex items-center justify-center text-blue-400">
            <BuildingsIcon size={22} weight="fill" />
          </div>
          <div>
            <h2 className="text-base font-bold text-white">Indian Municipal Wards Jurisdiction</h2>
            <p className="text-xs text-slate-400">
              Delhi Jal Board &amp; MCD Executive Engineer Routing Framework
            </p>
          </div>
        </div>

        {/* Search */}
        <div className="relative w-full sm:w-64">
          <MagnifyingGlassIcon size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search ward or officer..."
            className="w-full pl-9 pr-3 py-1.5 bg-slate-950 border border-slate-700 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />
        </div>
      </div>

      <div className="mt-5 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filtered.map((w) => (
          <div
            key={w.id || w.wardNumber}
            className="bg-slate-950/80 border border-slate-800 rounded-xl p-4 shadow hover:border-slate-700 transition-colors"
          >
            <div className="flex items-center justify-between pb-2 border-b border-slate-800/80">
              <div>
                <span className="font-bold text-white text-sm block">{w.wardName}</span>
                <span className="text-[11px] text-slate-400">
                  {w.municipalBody} • {w.zoneName}
                </span>
              </div>
              <span className="text-xs font-mono font-bold px-2 py-0.5 rounded bg-blue-950 text-blue-300 border border-blue-800/60">
                Ward #{w.wardNumber}
              </span>
            </div>

            <div className="space-y-2 mt-3 text-xs text-slate-300">
              <div className="flex items-center gap-2">
                <UserIcon size={15} className="text-slate-500 shrink-0" />
                <span>
                  Officer: <strong className="text-slate-200">{w.officerName}</strong> (
                  {w.officerDesignation || "EE - Water"})
                </span>
              </div>

              <div className="flex items-center gap-2">
                <EnvelopeIcon size={15} className="text-slate-500 shrink-0" />
                <span className="truncate">{w.contactEmail}</span>
              </div>

              <div className="flex items-center gap-2">
                <PhoneIcon size={15} className="text-emerald-400 shrink-0" />
                <span>
                  Helpline: <strong className="text-emerald-400">{w.emergencyHotline}</strong>
                </span>
              </div>

              <div className="pt-2 border-t border-slate-800/80 text-[10px] text-slate-500 flex items-center gap-1.5 font-mono truncate">
                <BroadcastIcon size={13} className="text-purple-400 shrink-0" />
                <span className="truncate">{w.webhookUrl}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
