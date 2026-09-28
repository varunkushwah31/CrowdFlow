import React from "react";
import { useAuth } from "../../context/AuthContext";
import {
  DropIcon,
  ShieldIcon,
  ShieldCheckIcon,
  UserIcon,
  SignOutIcon,
  CodeIcon,
  MapTrifoldIcon,
  CameraIcon,
  WarningCircleIcon,
  BuildingsIcon,
  PaperPlaneTiltIcon,
  MagnifyingGlassIcon,
  ChartBarIcon,
  TruckIcon,
  DatabaseIcon,
  ScrollIcon
} from "@phosphor-icons/react";

export type ViewMode = "CITIZEN" | "ADMIN";
export type CitizenTab = "map" | "report" | "operations" | "wards" | "dispatch" | "track";
export type AdminTab = "overview" | "triage" | "clusters" | "fleet" | "wards" | "audit" | "diagnostics";

interface NavbarProps {
  viewMode: ViewMode;
  onViewChange: (mode: ViewMode) => void;
  citizenTab: CitizenTab;
  onCitizenTabChange: (tab: CitizenTab) => void;
  adminTab: AdminTab;
  onAdminTabChange: (tab: AdminTab) => void;
  onOpenAuth: () => void;
}

function getRoleLabel(role?: string): string {
  if (role === "ROLE_SUPER_ADMIN") return "Super Admin";
  if (role === "ROLE_WARD_OFFICER") return "Ward Officer";
  return "Citizen";
}

interface CitizenNavLinksProps {
  currentTab: CitizenTab;
  onTabChange: (tab: CitizenTab) => void;
}

const CITIZEN_ITEMS: Array<{ tab: CitizenTab; label: string; icon: React.ComponentType<{ size?: number; weight?: "fill" | "regular" }> }> = [
  { tab: "map", label: "Live Indian Map & Heatmap", icon: MapTrifoldIcon },
  { tab: "report", label: "Report Water Hazard", icon: CameraIcon },
  { tab: "operations", label: "Municipal Operations", icon: WarningCircleIcon },
  { tab: "wards", label: "Indian Municipal Wards", icon: BuildingsIcon },
  { tab: "dispatch", label: "Dispatch Logs", icon: PaperPlaneTiltIcon },
  { tab: "track", label: "Track Grievance", icon: MagnifyingGlassIcon },
];

const CitizenNavLinks: React.FC<CitizenNavLinksProps> = ({ currentTab, onTabChange }) => (
  <>
    {CITIZEN_ITEMS.map((item) => {
      const Icon = item.icon;
      const isActive = currentTab === item.tab;
      return (
        <button
          key={item.tab}
          onClick={() => onTabChange(item.tab)}
          className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-all ${
            isActive
              ? "bg-blue-600/20 border border-blue-500/40 text-blue-300 font-semibold"
              : "text-slate-400 hover:text-slate-200 hover:bg-slate-850"
          }`}
        >
          <Icon size={16} weight={isActive ? "fill" : "regular"} />
          <span>{item.label}</span>
        </button>
      );
    })}
  </>
);

interface AdminNavLinksProps {
  currentTab: AdminTab;
  onTabChange: (tab: AdminTab) => void;
}

const ADMIN_ITEMS: Array<{ tab: AdminTab; label: string; icon: React.ComponentType<{ size?: number; weight?: "fill" | "regular" }> }> = [
  { tab: "overview", label: "Command Overview & KPIs", icon: ChartBarIcon },
  { tab: "triage", label: "Grievance Triage & Status", icon: DropIcon },
  { tab: "clusters", label: "Spatial DBSCAN & AMRUT 2.0", icon: WarningCircleIcon },
  { tab: "fleet", label: "Emergency Fleet & Routing", icon: TruckIcon },
  { tab: "wards", label: "Municipal Jurisdictions", icon: BuildingsIcon },
  { tab: "audit", label: "Audit Trail & Logs", icon: ScrollIcon },
  { tab: "diagnostics", label: "Redis & System Telemetry", icon: DatabaseIcon },
];

const AdminNavLinks: React.FC<AdminNavLinksProps> = ({ currentTab, onTabChange }) => (
  <>
    {ADMIN_ITEMS.map((item) => {
      const Icon = item.icon;
      const isActive = currentTab === item.tab;
      return (
        <button
          key={item.tab}
          onClick={() => onTabChange(item.tab)}
          className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-all ${
            isActive
              ? "bg-purple-600/20 border border-purple-500/40 text-purple-300 font-semibold"
              : "text-slate-400 hover:text-slate-200 hover:bg-slate-850"
          }`}
        >
          <Icon size={16} weight={isActive ? "fill" : "regular"} />
          <span>{item.label}</span>
        </button>
      );
    })}
  </>
);

interface UserBadgeMenuProps {
  onOpenAuth: () => void;
}

const UserBadgeMenu: React.FC<UserBadgeMenuProps> = ({ onOpenAuth }) => {
  const { auth, isAdmin, logout } = useAuth();

  if (!auth) {
    return (
      <button
        onClick={onOpenAuth}
        className="py-1.5 px-3 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-semibold transition-all shadow-md shadow-blue-600/20 flex items-center gap-1.5"
      >
        <UserIcon size={16} />
        <span>Sign In / Authority</span>
      </button>
    );
  }

  return (
    <div className="flex items-center gap-2 bg-slate-950/60 border border-slate-800 py-1 px-2.5 rounded-xl">
      <div className="flex items-center gap-1.5 text-xs">
        {isAdmin ? (
          <ShieldCheckIcon size={16} className="text-emerald-400" weight="fill" />
        ) : (
          <UserIcon size={16} className="text-blue-400" />
        )}
        <div className="hidden sm:block text-left">
          <span className="font-semibold text-slate-200 block text-[11px] leading-tight">
            {auth.name || auth.phoneNumber}
          </span>
          <span className="text-[10px] text-slate-400 block leading-tight">
            {getRoleLabel(auth.role)}
          </span>
        </div>
      </div>
      <button
        onClick={logout}
        className="text-slate-400 hover:text-rose-400 p-1 transition-colors"
        title="Sign Out"
      >
        <SignOutIcon size={16} />
      </button>
    </div>
  );
};

export const Navbar: React.FC<NavbarProps> = ({
  viewMode,
  onViewChange,
  citizenTab,
  onCitizenTabChange,
  adminTab,
  onAdminTabChange,
  onOpenAuth
}) => {
  const { isAdmin } = useAuth();

  const handleAdminModeClick = () => {
    onViewChange("ADMIN");
    if (!isAdmin) {
      onOpenAuth();
    }
  };

  return (
    <header className="bg-slate-900 border-b border-slate-800 text-slate-100 sticky top-0 z-40 shadow-md">
      {/* Top Banner */}
      <div className="max-w-7xl mx-auto px-4 py-3 flex flex-wrap items-center justify-between gap-4">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-cyan-600 flex items-center justify-center text-white shadow-lg shadow-blue-500/20">
            <DropIcon size={24} weight="fill" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-extrabold text-lg tracking-tight text-white">WaterWatch India</h1>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-blue-500/20 border border-blue-500/40 text-blue-400">
                Civic v1.0
              </span>
            </div>
            <p className="text-xs text-slate-400 hidden sm:block">
              National Water Crowdsourcing, Spatial DBSCAN &amp; Delhi Jal Board Escalation
            </p>
          </div>
        </div>

        {/* View Mode Switcher & User Profile */}
        <div className="flex items-center gap-2.5">
          {/* View Toggle */}
          <div className="bg-slate-950/80 p-1 rounded-xl border border-slate-800 flex items-center gap-1">
            <button
              onClick={() => onViewChange("CITIZEN")}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                viewMode === "CITIZEN"
                  ? "bg-blue-600 text-white shadow-sm"
                  : "text-slate-400 hover:text-slate-200"
              }`}
            >
              <DropIcon size={16} weight={viewMode === "CITIZEN" ? "fill" : "regular"} />
              <span>Citizen Portal</span>
            </button>
            <button
              onClick={handleAdminModeClick}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                viewMode === "ADMIN"
                  ? "bg-purple-600 text-white shadow-sm"
                  : "text-slate-400 hover:text-purple-300"
              }`}
            >
              <ShieldIcon size={16} weight={viewMode === "ADMIN" ? "fill" : "regular"} />
              <span>Gov Command Center</span>
            </button>
          </div>

          {/* Swagger link */}
          <a
            href="http://localhost:8085/swagger-ui.html"
            target="_blank"
            rel="noreferrer"
            className="p-2 rounded-xl border border-slate-800 bg-slate-950/50 hover:bg-slate-800 text-slate-400 hover:text-white transition-colors hidden md:flex items-center gap-1.5 text-xs font-medium"
            title="OpenAPI / Swagger Documentation"
          >
            <CodeIcon size={16} />
            <span className="hidden lg:inline">Swagger Docs</span>
          </a>

          <UserBadgeMenu onOpenAuth={onOpenAuth} />
        </div>
      </div>

      {/* Secondary Navigation Tabs Bar */}
      <div className="bg-slate-950/60 border-t border-slate-800/80 px-4">
        <div className="max-w-7xl mx-auto flex items-center gap-1 overflow-x-auto py-1.5 no-scrollbar">
          {viewMode === "CITIZEN" ? (
            <CitizenNavLinks currentTab={citizenTab} onTabChange={onCitizenTabChange} />
          ) : (
            <AdminNavLinks currentTab={adminTab} onTabChange={onAdminTabChange} />
          )}
        </div>
      </div>
    </header>
  );
};
