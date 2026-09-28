import React, { useState, useEffect } from "react";
import { AuthProvider } from "./context/AuthContext";
import { ToastContainer } from "./components/common/Toast";
import { Navbar, type ViewMode, type CitizenTab, type AdminTab } from "./components/common/Navbar";
import { AuthModal } from "./components/common/AuthModal";
import { CitizenPortal } from "./components/citizen/CitizenPortal";
import { AdminPortal } from "./components/admin/AdminPortal";
import { DropIcon } from "@phosphor-icons/react";

export const AppContent: React.FC = () => {
  // Determine initial view from URL path
  const [viewMode, setViewMode] = useState<ViewMode>(() => {
    if (typeof window !== "undefined") {
      const path = window.location.pathname.toLowerCase();
      if (path.includes("/admin")) return "ADMIN";
    }
    return "CITIZEN";
  });

  const [citizenTab, setCitizenTab] = useState<CitizenTab>("map");
  const [adminTab, setAdminTab] = useState<AdminTab>("overview");
  const [authModalOpen, setAuthModalOpen] = useState(false);

  // Sync browser URL / history when view mode changes
  useEffect(() => {
    if (typeof window !== "undefined") {
      const targetPath = viewMode === "ADMIN" ? "/admin" : "/";
      if (window.location.pathname !== targetPath && !window.location.pathname.startsWith(targetPath)) {
        window.history.pushState(null, "", targetPath);
      }
    }
  }, [viewMode]);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-blue-600 selection:text-white">
      <ToastContainer />
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        defaultRole={viewMode === "ADMIN" ? "ROLE_WARD_OFFICER" : "ROLE_CITIZEN"}
      />

      <Navbar
        viewMode={viewMode}
        onViewChange={setViewMode}
        citizenTab={citizenTab}
        onCitizenTabChange={setCitizenTab}
        adminTab={adminTab}
        onAdminTabChange={setAdminTab}
        onOpenAuth={() => setAuthModalOpen(true)}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6">
        {viewMode === "CITIZEN" ? (
          <CitizenPortal activeTab={citizenTab} onTabChange={setCitizenTab} />
        ) : (
          <AdminPortal
            activeTab={adminTab}
            onTabChange={setAdminTab}
            onOpenAuth={() => setAuthModalOpen(true)}
          />
        )}
      </main>

      {/* Footer */}
      <footer className="bg-slate-900/90 border-t border-slate-800/80 py-6 px-4 text-center text-xs text-slate-400">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <DropIcon size={18} className="text-blue-500" weight="fill" />
            <span className="font-semibold text-slate-300">WaterWatch India</span>
            <span>• Ministry of Jal Shakti &amp; AMRUT 2.0 Civic Tech Framework</span>
          </div>

          <div className="text-[11px] text-slate-500 font-mono">
            React 19 &amp; TypeScript • Phosphor Icons • Leaflet GIS • Spring Boot 3.4 Decoupled Architecture
          </div>
        </div>
      </footer>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
};

export default App;
