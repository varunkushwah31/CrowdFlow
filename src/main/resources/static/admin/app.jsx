// =============================================================================
// WaterWatch India — Government & Municipal Admin Control Room (React 18)
// Strictly restricted to Government Authorities (ROLE_WARD_OFFICER, ROLE_SUPER_ADMIN)
// =============================================================================

const { useState, useEffect, useRef, useMemo, useCallback } = React;

// Quick Authority Demo Profiles
const AUTHORITY_PRESETS = [
  {
    name: "Er. Alok Sharma",
    designation: "Executive Engineer (EE - Water)",
    jurisdiction: "Ward 85 - Karol Bagh (Delhi Jal Board)",
    phone: "+91 98110 23412",
    role: "WARD_OFFICER",
    badgeColor: "emerald"
  },
  {
    name: "Er. Rakesh K. Kaushik",
    designation: "Chief Engineer / Zonal Admin",
    jurisdiction: "Delhi Jal Board HQ (Varunalaya Phase II)",
    phone: "+91 98711 54321",
    role: "SUPER_ADMIN",
    badgeColor: "purple"
  },
  {
    name: "Unauthorized Citizen Attempt",
    designation: "Public Citizen (Restricted)",
    jurisdiction: "Delhi Resident",
    phone: "+91 91234 56789",
    role: "CITIZEN",
    badgeColor: "rose"
  }
];

// Helper to format ISO dates to IST
function formatIST(isoString) {
  if (!isoString) return "N/A";
  try {
    const d = new Date(isoString);
    return d.toLocaleString("en-IN", {
      timeZone: "Asia/Kolkata",
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    });
  } catch {
    return isoString;
  }
}

// Issue Type Metadata Helper
function getIssueMeta(issueType) {
  switch (issueType) {
    case "BURST_PIPE":
      return { label: "Burst Pipeline", bg: "bg-rose-500/20", border: "border-rose-500/40", text: "text-rose-400", icon: "fa-burst" };
    case "CONTAMINATION":
      return { label: "Water Contamination", bg: "bg-amber-500/20", border: "border-amber-500/40", text: "text-amber-400", icon: "fa-biohazard" };
    case "SEVERE_WATERLOGGING":
      return { label: "Severe Waterlogging", bg: "bg-blue-500/20", border: "border-blue-500/40", text: "text-blue-400", icon: "fa-water" };
    case "DRAINAGE_OVERFLOW":
      return { label: "Drainage Overflow", bg: "bg-orange-500/20", border: "border-orange-500/40", text: "text-orange-400", icon: "fa-faucet-drip" };
    case "LOW_PRESSURE":
      return { label: "Low Water Pressure", bg: "bg-cyan-500/20", border: "border-cyan-500/40", text: "text-cyan-400", icon: "fa-gauge-simple-low" };
    case "WATER_SCARCITY":
      return { label: "Water Scarcity", bg: "bg-yellow-500/20", border: "border-yellow-500/40", text: "text-yellow-400", icon: "fa-hand-holding-droplet" };
    case "OPEN_SEWAGE":
      return { label: "Open Sewage Effluent", bg: "bg-red-500/20", border: "border-red-500/40", text: "text-red-400", icon: "fa-triangle-exclamation" };
    default:
      return { label: issueType || "Hazard", bg: "bg-slate-500/20", border: "border-slate-500/40", text: "text-slate-300", icon: "fa-circle-exclamation" };
  }
}

// Status Badge Metadata Helper
function getStatusMeta(status) {
  switch (status) {
    case "SUBMITTED":
      return { label: "Intake / Pending", bg: "bg-sky-500/20", text: "text-sky-400", border: "border-sky-500/40", dot: "bg-sky-400" };
    case "CLUSTERED":
      return { label: "Hotspot Clustered", bg: "bg-purple-500/20", text: "text-purple-400", border: "border-purple-500/40", dot: "bg-purple-400" };
    case "ESCALATED":
      return { label: "Control Room Escalated", bg: "bg-amber-500/20", text: "text-amber-400", border: "border-amber-500/40", dot: "bg-amber-400" };
    case "IN_PROGRESS":
      return { label: "Field Crew Dispatched", bg: "bg-yellow-500/20", text: "text-yellow-400", border: "border-yellow-500/40", dot: "bg-yellow-400 animate-ping" };
    case "RESOLVED":
      return { label: "Restored & Normalized", bg: "bg-emerald-500/20", text: "text-emerald-400", border: "border-emerald-500/40", dot: "bg-emerald-400" };
    case "REJECTED":
      return { label: "Spam / Rejected", bg: "bg-slate-500/20", text: "text-slate-400", border: "border-slate-500/40", dot: "bg-slate-500" };
    default:
      return { label: status || "Unknown", bg: "bg-slate-500/20", text: "text-slate-400", border: "border-slate-500/40", dot: "bg-slate-400" };
  }
}

// =============================================================================
// MAIN APP COMPONENT
// =============================================================================
function AdminApp() {
  // Authentication State
  const [auth, setAuth] = useState(() => {
    try {
      const saved = localStorage.getItem("civic_admin_auth");
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  const [activeTab, setActiveTab] = useState("overview");
  const [toasts, setToasts] = useState([]);

  // Data States
  const [overview, setOverview] = useState(null);
  const [reports, setReports] = useState([]);
  const [clusters, setClusters] = useState([]);
  const [depots, setDepots] = useState([]);
  const [tankers, setTankers] = useState([]);
  const [wards, setWards] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [diagnostics, setDiagnostics] = useState(null);
  const [redisStats, setRedisStats] = useState(null);

  // Filter States
  const [reportFilter, setReportFilter] = useState({
    status: "ALL",
    issueType: "ALL",
    wardNumber: 0,
    search: ""
  });

  // Modal States
  const [inspectReport, setInspectReport] = useState(null);
  const [statusModalReport, setStatusModalReport] = useState(null);
  const [reassignModalReport, setReassignModalReport] = useState(null);
  const [inspectAudit, setInspectAudit] = useState(null);
  const [clusterModal, setClusterModal] = useState(null);

  // Loading States
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  // Toast Dispatcher
  const showToast = useCallback((message, type = "info") => {
    const id = Date.now() + Math.random();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 4500);
  }, []);

  // Authenticated API Fetch Wrapper
  const apiFetch = useCallback(async (url, options = {}) => {
    const token = auth?.token;
    const headers = {
      ...(options.headers || {}),
      ...(token ? { "Authorization": `Bearer ${token}` } : {})
    };

    try {
      const res = await fetch(url, { ...options, headers });
      if (res.status === 401) {
        showToast("Session expired or unauthenticated. Please re-authenticate.", "error");
        setAuth(null);
        localStorage.removeItem("civic_admin_auth");
        throw new Error("UNAUTHORIZED");
      }
      if (res.status === 403) {
        showToast("403 Forbidden: Citizens are restricted from accessing government control operations.", "error");
        throw new Error("ACCESS_DENIED");
      }
      if (!res.ok) {
        const errorBody = await res.json().catch(() => ({}));
        throw new Error(errorBody.message || `HTTP ${res.status}`);
      }
      return await res.json();
    } catch (err) {
      if (err.message !== "UNAUTHORIZED" && err.message !== "ACCESS_DENIED") {
        showToast(`Request failed: ${err.message}`, "error");
      }
      throw err;
    }
  }, [auth, showToast]);

  // Load all initial data when authenticated
  const loadDashboardData = useCallback(async () => {
    if (!auth || (auth.role !== "ROLE_WARD_OFFICER" && auth.role !== "ROLE_SUPER_ADMIN")) {
      return;
    }
    setRefreshing(true);
    try {
      const [ovData, repData, clustData, fleetData, wardData, auditData, diagData, rStats] = await Promise.all([
        apiFetch("/api/admin/overview"),
        apiFetch("/api/admin/reports"),
        apiFetch("/api/admin/clusters"),
        apiFetch("/api/admin/fleet/status"),
        apiFetch("/api/wards"),
        apiFetch("/api/admin/audit-logs"),
        apiFetch("/api/admin/diagnostics"),
        apiFetch("/api/cache/stats").catch(() => null)
      ]);

      setOverview(ovData);
      setReports(repData || []);
      setClusters(clustData || []);
      setDepots(fleetData?.depots || []);
      setTankers(fleetData?.tankers || []);
      setWards(wardData || []);
      setAuditLogs(auditData || []);
      setDiagnostics(diagData);
      setRedisStats(rStats);
    } catch (err) {
      console.error("Dashboard data load error:", err);
    } finally {
      setRefreshing(false);
    }
  }, [auth, apiFetch]);

  useEffect(() => {
    if (auth && (auth.role === "ROLE_WARD_OFFICER" || auth.role === "ROLE_SUPER_ADMIN")) {
      loadDashboardData();
    }
  }, [auth, loadDashboardData]);

  // Handle Logout
  const handleLogout = () => {
    setAuth(null);
    localStorage.removeItem("civic_admin_auth");
    showToast("Signed out from Municipal Control Room.", "info");
  };

  // If user is unauthenticated or has citizen role, show the Government Security Portal Gate
  if (!auth || (auth.role !== "ROLE_WARD_OFFICER" && auth.role !== "ROLE_SUPER_ADMIN")) {
    return (
      <GovernmentAuthGate
        onAuthenticated={(authData) => {
          if (authData.role === "ROLE_CITIZEN") {
            showToast("ACCESS DENIED: Citizen account (+91...) is not authorized to access municipal command controls.", "error");
            return;
          }
          localStorage.setItem("civic_admin_auth", JSON.stringify(authData));
          setAuth(authData);
          showToast(`Welcome, ${authData.role === "ROLE_SUPER_ADMIN" ? "Super Admin" : "Ward Officer"}! Control Room Initialized.`, "success");
        }}
        showToast={showToast}
      />
    );
  }

  return (
    <div className="min-h-screen bg-gov-950 flex flex-col">
      {/* Toast Notification Container */}
      <div className="fixed top-5 right-5 z-50 flex flex-col gap-2 max-w-md w-full pointer-events-none">
        {toasts.map(t => (
          <div
            key={t.id}
            className={`pointer-events-auto p-4 rounded-lg shadow-xl border flex items-start gap-3 backdrop-blur-md transition-all ${
              t.type === "error"
                ? "bg-rose-950/90 border-rose-500/60 text-rose-200"
                : t.type === "success"
                ? "bg-emerald-950/90 border-emerald-500/60 text-emerald-200"
                : "bg-gov-900/90 border-civic-blue/60 text-cyan-200"
            }`}
          >
            <i className={`fa-solid mt-0.5 text-lg ${
              t.type === "error" ? "fa-circle-exclamation text-rose-400" :
              t.type === "success" ? "fa-circle-check text-emerald-400" : "fa-circle-info text-cyan-400"
            }`}></i>
            <div className="flex-1 text-sm font-medium leading-relaxed">{t.message}</div>
          </div>
        ))}
      </div>

      {/* TOP COMMAND HEADER */}
      <header className="bg-gov-900 border-b border-gov-800 sticky top-0 z-40 px-6 py-3 shadow-lg">
        <div className="flex flex-col lg:flex-row items-center justify-between gap-4">
          {/* Emblem & Title */}
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-civic-blue to-blue-700 flex items-center justify-center text-white shadow-md shadow-civic-blue/20">
              <i className="fa-solid fa-building-shield text-2xl"></i>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xs uppercase tracking-widest font-bold px-2 py-0.5 rounded bg-blue-500/20 text-blue-400 border border-blue-500/30">
                  Government of NCT of Delhi
                </span>
                <span className="text-xs uppercase tracking-wider font-semibold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center gap-1.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                  Control Room Online
                </span>
              </div>
              <h1 className="text-lg font-bold text-white tracking-tight flex items-center gap-2">
                Delhi Jal Board & Municipal Water Command Center
              </h1>
            </div>
          </div>

          {/* Officer Identity & Quick Controls */}
          <div className="flex items-center gap-3">
            {/* Officer Badge */}
            <div className="bg-gov-850 border border-gov-700 rounded-lg px-3.5 py-1.5 flex items-center gap-3">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold text-white ${
                auth.role === "ROLE_SUPER_ADMIN" ? "bg-purple-600" : "bg-emerald-600"
              }`}>
                {auth.role === "ROLE_SUPER_ADMIN" ? "SA" : "WO"}
              </div>
              <div className="text-left">
                <div className="text-xs font-semibold text-slate-200 flex items-center gap-1.5">
                  {auth.role === "ROLE_SUPER_ADMIN" ? "Delhi Central Super Admin" : "Ward Executive Engineer"}
                  <span className={`text-[10px] uppercase font-bold px-1.5 py-0.2 rounded ${
                    auth.role === "ROLE_SUPER_ADMIN"
                      ? "bg-purple-500/20 text-purple-300 border border-purple-500/40"
                      : "bg-emerald-500/20 text-emerald-300 border border-emerald-500/40"
                  }`}>
                    {auth.role === "ROLE_SUPER_ADMIN" ? "SUPER ADMIN" : "WARD OFFICER"}
                  </span>
                </div>
                <div className="text-[11px] text-slate-400 font-mono">
                  {auth.phoneNumber || "+91 98110 23412"}
                </div>
              </div>
            </div>

            {/* Refresh Button */}
            <button
              onClick={loadDashboardData}
              disabled={refreshing}
              title="Sync Latest Data"
              className="px-3 py-2 bg-gov-800 hover:bg-gov-700 text-slate-300 hover:text-white rounded-lg border border-gov-700 transition flex items-center gap-2 text-xs font-medium"
            >
              <i className={`fa-solid fa-rotate ${refreshing ? "fa-spin text-civic-blue" : ""}`}></i>
              <span className="hidden sm:inline">Sync Data</span>
            </button>

            {/* Sign Out Button */}
            <button
              onClick={handleLogout}
              title="Sign Out of Municipal Portal"
              className="px-3 py-2 bg-rose-500/10 hover:bg-rose-500/20 text-rose-300 hover:text-rose-200 rounded-lg border border-rose-500/30 transition flex items-center gap-1.5 text-xs font-medium"
            >
              <i className="fa-solid fa-arrow-right-from-bracket"></i>
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>
        </div>

        {/* NAVIGATION TABS */}
        <nav className="flex items-center gap-1 mt-4 overflow-x-auto border-t border-gov-800 pt-3 text-sm">
          {[
            { id: "overview", label: "Executive Overview", icon: "fa-chart-pie", badge: null },
            { id: "triage", label: "Grievance Triage & Intake", icon: "fa-list-check", badge: reports.filter(r => r.status === "SUBMITTED").length },
            { id: "clusters", label: "DBSCAN Clusters & AMRUT Dossiers", icon: "fa-circle-nodes", badge: clusters.length },
            { id: "fleet", label: "Emergency Fleet & Tankers", icon: "fa-truck-droplet", badge: tankers.length },
            { id: "wards", label: "Municipal Wards GIS", icon: "fa-map-location-dot", badge: wards.length },
            { id: "audit", label: "Dispatch Audit Trail", icon: "fa-clock-rotate-left", badge: null },
            { id: "redis", label: "Redis & Telemetry", icon: "fa-server", badge: null }
          ].map(tab => {
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-4 py-2 rounded-lg font-medium text-xs whitespace-nowrap transition flex items-center gap-2 ${
                  isActive
                    ? "bg-civic-blue text-white shadow-md shadow-civic-blue/20"
                    : "text-slate-400 hover:text-slate-200 hover:bg-gov-800/60"
                }`}
              >
                <i className={`fa-solid ${tab.icon}`}></i>
                <span>{tab.label}</span>
                {tab.badge !== null && tab.badge > 0 && (
                  <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded-full ${
                    isActive ? "bg-white/20 text-white" : "bg-gov-700 text-slate-300"
                  }`}>
                    {tab.badge}
                  </span>
                )}
              </button>
            );
          })}
        </nav>
      </header>

      {/* MAIN VIEWPORT */}
      <main className="flex-1 p-6 max-w-7xl w-full mx-auto">
        {activeTab === "overview" && (
          <OverviewTab
            overview={overview}
            reports={reports}
            clusters={clusters}
            tankers={tankers}
            diagnostics={diagnostics}
            onSelectTab={setActiveTab}
            apiFetch={apiFetch}
            showToast={showToast}
            onRefresh={loadDashboardData}
          />
        )}

        {activeTab === "triage" && (
          <TriageTab
            reports={reports}
            wards={wards}
            filter={reportFilter}
            setFilter={setReportFilter}
            onInspect={setInspectReport}
            onUpdateStatus={setStatusModalReport}
            onReassignWard={setReassignModalReport}
            apiFetch={apiFetch}
            showToast={showToast}
            onRefresh={loadDashboardData}
          />
        )}

        {activeTab === "clusters" && (
          <ClustersTab
            clusters={clusters}
            apiFetch={apiFetch}
            showToast={showToast}
            onRefresh={loadDashboardData}
          />
        )}

        {activeTab === "fleet" && (
          <FleetTab
            depots={depots}
            tankers={tankers}
            reports={reports}
            apiFetch={apiFetch}
            showToast={showToast}
            onRefresh={loadDashboardData}
          />
        )}

        {activeTab === "wards" && (
          <WardsTab
            wards={wards}
            apiFetch={apiFetch}
            showToast={showToast}
          />
        )}

        {activeTab === "audit" && (
          <AuditTab
            auditLogs={auditLogs}
            onInspectAudit={setInspectAudit}
          />
        )}

        {activeTab === "redis" && (
          <RedisTab
            redisStats={redisStats}
            apiFetch={apiFetch}
            showToast={showToast}
            onRefresh={loadDashboardData}
          />
        )}
      </main>

      {/* FOOTER */}
      <footer className="bg-gov-900 border-t border-gov-800 px-6 py-4 text-xs text-slate-400 flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <span className="font-semibold text-slate-300">WaterWatch India Monolith</span>
          <span>•</span>
          <span>Delhi Jal Board / MCD Central Operations</span>
          <span>•</span>
          <span className="text-civic-blue">Helpline: 1916</span>
        </div>
        <div className="flex items-center gap-4 text-slate-500">
          <span>Spring Boot 3.4 • Java 25 • PostGIS • Redis • React 18</span>
          <span>•</span>
          <span>Confidential Municipal Console</span>
        </div>
      </footer>

      {/* MODALS */}
      {inspectReport && (
        <InspectReportModal
          report={inspectReport}
          onClose={() => setInspectReport(null)}
          onUpdateStatus={() => {
            const rep = inspectReport;
            setInspectReport(null);
            setStatusModalReport(rep);
          }}
        />
      )}

      {statusModalReport && (
        <UpdateStatusModal
          report={statusModalReport}
          onClose={() => setStatusModalReport(null)}
          apiFetch={apiFetch}
          showToast={showToast}
          onSuccess={() => {
            setStatusModalReport(null);
            loadDashboardData();
          }}
        />
      )}

      {reassignModalReport && (
        <ReassignWardModal
          report={reassignModalReport}
          wards={wards}
          onClose={() => setReassignModalReport(null)}
          apiFetch={apiFetch}
          showToast={showToast}
          onSuccess={() => {
            setReassignModalReport(null);
            loadDashboardData();
          }}
        />
      )}

      {inspectAudit && (
        <InspectAuditModal
          log={inspectAudit}
          onClose={() => setInspectAudit(null)}
        />
      )}
    </div>
  );
}

// =============================================================================
// COMPONENT: GOVERNMENT AUTHENTICATION & ACCESS CONTROL GATE (STRICT RBAC)
// =============================================================================
function GovernmentAuthGate({ onAuthenticated, showToast }) {
  const [phone, setPhone] = useState("+91 98110 23412");
  const [role, setRole] = useState("WARD_OFFICER");
  const [otp, setOtp] = useState("123456");
  const [step, setStep] = useState("CREDENTIALS"); // 'CREDENTIALS' | 'OTP'
  const [loading, setLoading] = useState(false);
  const [accessDeniedMessage, setAccessDeniedMessage] = useState(null);

  // Quick Preset Selection
  const applyPreset = (preset) => {
    setPhone(preset.phone);
    setRole(preset.role);
    setAccessDeniedMessage(null);
    if (preset.role === "CITIZEN") {
      setAccessDeniedMessage("NOTICE: Selecting Citizen account will trigger 403 Forbidden Access Gate to test RBAC security controls.");
    }
  };

  const handleSendOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    setAccessDeniedMessage(null);

    try {
      const res = await fetch("/api/auth/send-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ phoneNumber: phone })
      });
      const data = await res.json();
      if (res.ok) {
        showToast(`SMS OTP dispatched to ${phone}. (Demo Master OTP: ${data.demoOtp || "123456"})`, "info");
        setStep("OTP");
      } else {
        showToast(data.message || "Failed to dispatch OTP", "error");
      }
    } catch (err) {
      showToast("Network error contacting Civic SMS Gateway: " + err.message, "error");
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    setAccessDeniedMessage(null);

    try {
      const res = await fetch("/api/auth/verify-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          phoneNumber: phone,
          otp: otp,
          role: role
        })
      });
      const data = await res.json();

      if (!res.ok) {
        showToast(data.message || "Invalid OTP code", "error");
        setLoading(false);
        return;
      }

      // Check Role: If CITIZEN, reject immediately!
      if (data.role === "ROLE_CITIZEN") {
        setAccessDeniedMessage("403 ACCESS FORBIDDEN: This web console is strictly restricted to Delhi Jal Board (DJB) and Municipal Corporation of Delhi (MCD) authorities. Citizen accounts cannot access administrative control controls. Please track your grievance on the public tracking portal.");
        showToast("Access Denied: Citizen accounts are forbidden from Municipal Command Room.", "error");
        setLoading(false);
        return;
      }

      // Ward Officer or Super Admin allowed!
      onAuthenticated({
        token: data.token,
        role: data.role,
        phoneNumber: data.phoneNumber,
        officerName: role === "SUPER_ADMIN" ? "Chief Engineer (Admin)" : "Executive Engineer (Ward Officer)"
      });
    } catch (err) {
      showToast("Authentication failure: " + err.message, "error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gov-950 flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Ambient Glows */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-civic-blue/10 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl pointer-events-none"></div>

      <div className="max-w-xl w-full bg-gov-900 border border-gov-800 rounded-2xl shadow-2xl p-8 relative z-10">
        {/* Emblem & Official Header */}
        <div className="text-center mb-6">
          <div className="w-16 h-16 mx-auto mb-3 rounded-2xl bg-gradient-to-tr from-civic-blue to-blue-600 flex items-center justify-center text-white shadow-xl shadow-civic-blue/30">
            <i className="fa-solid fa-shield-halved text-3xl"></i>
          </div>
          <div className="inline-block text-[11px] font-bold uppercase tracking-widest text-civic-blue bg-civic-blue/10 border border-civic-blue/25 px-3 py-1 rounded-full mb-2">
            Delhi Jal Board • Municipal Corporation of Delhi
          </div>
          <h2 className="text-2xl font-bold text-white tracking-tight">
            Municipal Command & Control Room
          </h2>
          <p className="text-sm text-slate-400 mt-1">
            Restricted Government Operations Portal (Web Only)
          </p>
        </div>

        {/* Security Advisory Banner */}
        <div className="mb-6 p-3.5 bg-gov-950/80 border border-gov-700/60 rounded-xl text-xs text-slate-300 flex items-start gap-3">
          <i className="fa-solid fa-triangle-exclamation text-amber-400 mt-0.5 text-sm"></i>
          <div className="leading-relaxed">
            <strong className="text-amber-300">Security Advisory:</strong> Access is restricted strictly to verified Executive Engineers (<code className="text-emerald-400">ROLE_WARD_OFFICER</code>) and Control Room Administrators (<code className="text-purple-400">ROLE_SUPER_ADMIN</code>). Citizens (<code className="text-rose-400">ROLE_CITIZEN</code>) are prohibited.
          </div>
        </div>

        {/* RBAC Access Denied Banner if citizen attempted */}
        {accessDeniedMessage && (
          <div className="mb-6 p-4 bg-rose-950/90 border border-rose-500/70 rounded-xl text-xs text-rose-200 flex items-start gap-3 shadow-lg shadow-rose-950/50 animate-shake">
            <i className="fa-solid fa-hand text-rose-400 text-lg mt-0.5"></i>
            <div>
              <div className="font-bold text-sm text-rose-300 mb-1">ACCESS DENIED (403 FORBIDDEN)</div>
              <p className="leading-relaxed">{accessDeniedMessage}</p>
              <div className="mt-2.5">
                <a href="/index.html" className="inline-flex items-center gap-1.5 text-xs font-semibold text-white bg-rose-600 hover:bg-rose-500 px-3 py-1 rounded transition">
                  <i className="fa-solid fa-arrow-left"></i> Return to Public Citizen Portal
                </a>
              </div>
            </div>
          </div>
        )}

        {/* Quick Demo Credentials Switcher */}
        <div className="mb-6 bg-gov-950 border border-gov-800 rounded-xl p-3.5">
          <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2 flex items-center justify-between">
            <span>Fast Test Profiles</span>
            <span className="text-slate-500 font-normal">Click to auto-fill</span>
          </div>
          <div className="grid grid-cols-1 gap-2">
            {AUTHORITY_PRESETS.map((p, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => applyPreset(p)}
                className={`p-2.5 rounded-lg border text-left text-xs transition flex items-center justify-between ${
                  phone === p.phone && role === p.role
                    ? "bg-civic-blue/20 border-civic-blue text-white shadow-sm"
                    : "bg-gov-900 border-gov-800 text-slate-300 hover:border-gov-700"
                }`}
              >
                <div>
                  <div className="font-semibold text-slate-200">{p.name}</div>
                  <div className="text-[11px] text-slate-400">{p.jurisdiction} • {p.phone}</div>
                </div>
                <span className={`text-[10px] font-bold px-2 py-0.5 rounded uppercase ${
                  p.badgeColor === "emerald" ? "bg-emerald-500/20 text-emerald-300 border border-emerald-500/30" :
                  p.badgeColor === "purple" ? "bg-purple-500/20 text-purple-300 border border-purple-500/30" :
                  "bg-rose-500/20 text-rose-300 border border-rose-500/30"
                }`}>
                  {p.role}
                </span>
              </button>
            ))}
          </div>
        </div>

        {/* Authentication Form */}
        {step === "CREDENTIALS" ? (
          <form onSubmit={handleSendOtp} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Indian Mobile Number (+91)
              </label>
              <div className="relative">
                <i className="fa-solid fa-phone text-slate-500 absolute left-3.5 top-3 text-xs"></i>
                <input
                  type="text"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+91 98110 23412"
                  required
                  className="w-full pl-9 pr-4 py-2.5 bg-gov-950 border border-gov-700 rounded-lg text-sm text-white placeholder-slate-500 focus:outline-none focus:border-civic-blue font-mono"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Designated Government Role
              </label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value)}
                className="w-full px-3 py-2.5 bg-gov-950 border border-gov-700 rounded-lg text-sm text-white focus:outline-none focus:border-civic-blue"
              >
                <option value="WARD_OFFICER">Executive Engineer / Ward Officer (ROLE_WARD_OFFICER)</option>
                <option value="SUPER_ADMIN">Zonal Commissioner / Super Admin (ROLE_SUPER_ADMIN)</option>
                <option value="CITIZEN">Citizen Account (ROLE_CITIZEN — For Testing RBAC Rejection)</option>
              </select>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-civic-blue hover:bg-blue-600 text-white rounded-lg text-sm font-semibold transition shadow-md shadow-civic-blue/20 flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <i className="fa-solid fa-circle-notch fa-spin"></i>
                  <span>Dispatching OTP...</span>
                </>
              ) : (
                <>
                  <i className="fa-solid fa-paper-plane"></i>
                  <span>Request Official OTP</span>
                </>
              )}
            </button>
          </form>
        ) : (
          <form onSubmit={handleVerifyOtp} className="space-y-4">
            <div className="p-3 bg-gov-950 rounded-lg border border-gov-800 flex items-center justify-between text-xs">
              <span className="text-slate-400">Verifying: <strong className="text-white font-mono">{phone}</strong></span>
              <button
                type="button"
                onClick={() => setStep("CREDENTIALS")}
                className="text-civic-blue hover:underline"
              >
                Change Number
              </button>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Enter 6-Digit Verification OTP
              </label>
              <div className="relative">
                <i className="fa-solid fa-key text-slate-500 absolute left-3.5 top-3 text-xs"></i>
                <input
                  type="text"
                  maxLength={6}
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  placeholder="123456"
                  required
                  className="w-full pl-9 pr-4 py-2.5 bg-gov-950 border border-gov-700 rounded-lg text-sm text-white placeholder-slate-500 focus:outline-none focus:border-civic-blue font-mono tracking-widest text-center text-lg"
                />
              </div>
              <p className="text-[11px] text-slate-500 mt-1">
                Demo Master OTP: <code className="text-civic-blue font-mono">123456</code> (or check Fast2SMS console)
              </p>
            </div>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setStep("CREDENTIALS")}
                className="w-1/3 py-2.5 bg-gov-800 hover:bg-gov-700 text-slate-300 rounded-lg text-sm font-medium transition"
              >
                Back
              </button>
              <button
                type="submit"
                disabled={loading}
                className="w-2/3 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-sm font-semibold transition shadow-md shadow-emerald-600/20 flex items-center justify-center gap-2"
              >
                {loading ? (
                  <>
                    <i className="fa-solid fa-circle-notch fa-spin"></i>
                    <span>Verifying...</span>
                  </>
                ) : (
                  <>
                    <i className="fa-solid fa-unlock-keyhole"></i>
                    <span>Authorize & Enter</span>
                  </>
                )}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

// =============================================================================
// TAB 1: EXECUTIVE OVERVIEW (KPIS, SLA, ISSUE DISTRIBUTION)
// =============================================================================
function OverviewTab({ overview, reports, clusters, tankers, diagnostics, onSelectTab, apiFetch, showToast, onRefresh }) {
  if (!overview) {
    return (
      <div className="p-12 text-center text-slate-400">
        <i className="fa-solid fa-circle-notch fa-spin text-3xl text-civic-blue mb-3"></i>
        <p>Loading Executive Command Metrics...</p>
      </div>
    );
  }

  const kpis = [
    { label: "Total Citizen Grievances", value: overview.totalReports, icon: "fa-bullhorn", color: "blue" },
    { label: "Pending Intake & Triage", value: overview.submittedReports, icon: "fa-inbox", color: "sky" },
    { label: "Clustered Hotspots", value: overview.clusteredReports, icon: "fa-circle-nodes", color: "purple" },
    { label: "Escalated to Jal Board", value: overview.escalatedReports, icon: "fa-triangle-exclamation", color: "amber" },
    { label: "Field Units On-Site", value: overview.inProgressReports, icon: "fa-person-digging", color: "yellow" },
    { label: "Restored & Normalized", value: overview.resolvedReports, icon: "fa-circle-check", color: "emerald" },
    { label: "Critical Feeder Ruptures", value: overview.criticalClusters, icon: "fa-burst", color: "rose" },
    { label: "Avg Resolution SLA", value: `${overview.averageResolutionHours || 2.8} hrs`, icon: "fa-stopwatch", color: "cyan" }
  ];

  const getColorClasses = (c) => {
    switch (c) {
      case "blue": return "bg-blue-500/10 border-blue-500/30 text-blue-400";
      case "sky": return "bg-sky-500/10 border-sky-500/30 text-sky-400";
      case "purple": return "bg-purple-500/10 border-purple-500/30 text-purple-400";
      case "amber": return "bg-amber-500/10 border-amber-500/30 text-amber-400";
      case "yellow": return "bg-yellow-500/10 border-yellow-500/30 text-yellow-400";
      case "emerald": return "bg-emerald-500/10 border-emerald-500/30 text-emerald-400";
      case "rose": return "bg-rose-500/10 border-rose-500/30 text-rose-400";
      case "cyan": return "bg-cyan-500/10 border-cyan-500/30 text-cyan-400";
      default: return "bg-slate-500/10 border-slate-500/30 text-slate-400";
    }
  };

  return (
    <div className="space-y-6">
      {/* KPI Stats Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {kpis.map((kpi, idx) => (
          <div
            key={idx}
            className="bg-gov-900 border border-gov-800 rounded-xl p-4 shadow-sm relative overflow-hidden glass-panel"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-400 uppercase tracking-wide">{kpi.label}</span>
              <div className={`w-8 h-8 rounded-lg flex items-center justify-center border text-sm ${getColorClasses(kpi.color)}`}>
                <i className={`fa-solid ${kpi.icon}`}></i>
              </div>
            </div>
            <div className="text-2xl font-bold text-white mt-2 font-mono">{kpi.value}</div>
          </div>
        ))}
      </div>

      {/* Analytics & Breakdown Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Issue Type Distribution */}
        <div className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2">
              <i className="fa-solid fa-chart-bar text-civic-blue"></i>
              Grievance Breakdown by Issue
            </h3>
            <span className="text-xs text-slate-400 font-mono">{overview.totalReports} Total</span>
          </div>

          <div className="space-y-3">
            {overview.issueTypeCounts && Object.entries(overview.issueTypeCounts).map(([type, count]) => {
              const meta = getIssueMeta(type);
              const percentage = overview.totalReports ? Math.round((count / overview.totalReports) * 100) : 0;
              return (
                <div key={type} className="text-xs">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-slate-300 font-medium flex items-center gap-1.5">
                      <i className={`fa-solid ${meta.icon} ${meta.text}`}></i>
                      {meta.label}
                    </span>
                    <span className="text-slate-400 font-mono">{count} ({percentage}%)</span>
                  </div>
                  <div className="w-full h-1.5 bg-gov-800 rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full ${
                        type === "BURST_PIPE" ? "bg-rose-500" :
                        type === "CONTAMINATION" ? "bg-amber-500" :
                        type === "SEVERE_WATERLOGGING" ? "bg-blue-500" : "bg-cyan-500"
                      }`}
                      style={{ width: `${percentage}%` }}
                    ></div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Municipal Ward Distribution */}
        <div className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2">
              <i className="fa-solid fa-building-shield text-emerald-400"></i>
              Ward Distribution (NCT Delhi)
            </h3>
            <button
              onClick={() => onSelectTab("wards")}
              className="text-xs text-civic-blue hover:underline"
            >
              View GIS Map
            </button>
          </div>

          <div className="space-y-2.5">
            {overview.wardCounts && Object.entries(overview.wardCounts).map(([wardName, count]) => (
              <div
                key={wardName}
                className="p-2.5 bg-gov-950 border border-gov-800 rounded-lg flex items-center justify-between text-xs"
              >
                <div className="font-semibold text-slate-200">{wardName}</div>
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded bg-gov-800 text-slate-300 font-mono">{count} reports</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Quick Operations & Command Shortcuts */}
        <div className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel flex flex-col justify-between">
          <div>
            <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-4 flex items-center gap-2">
              <i className="fa-solid fa-bolt text-amber-400"></i>
              Rapid Command Shortcuts
            </h3>
            <div className="space-y-2.5">
              <button
                onClick={() => onSelectTab("clusters")}
                className="w-full p-3 bg-gov-950 hover:bg-gov-800/80 border border-gov-800 rounded-xl text-left text-xs transition flex items-center justify-between group"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-purple-500/20 text-purple-400 border border-purple-500/30 flex items-center justify-center">
                    <i className="fa-solid fa-circle-nodes"></i>
                  </div>
                  <div>
                    <div className="font-semibold text-slate-200 group-hover:text-white">Spatial DBSCAN Engine</div>
                    <div className="text-[11px] text-slate-400">Correlate reports into cluster dossiers</div>
                  </div>
                </div>
                <i className="fa-solid fa-chevron-right text-slate-600 group-hover:text-slate-300"></i>
              </button>

              <button
                onClick={() => onSelectTab("fleet")}
                className="w-full p-3 bg-gov-950 hover:bg-gov-800/80 border border-gov-800 rounded-xl text-left text-xs transition flex items-center justify-between group"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center">
                    <i className="fa-solid fa-truck-droplet"></i>
                  </div>
                  <div>
                    <div className="font-semibold text-slate-200 group-hover:text-white">Emergency Tanker Fleet</div>
                    <div className="text-[11px] text-slate-400">Mobilize potable water supply units</div>
                  </div>
                </div>
                <i className="fa-solid fa-chevron-right text-slate-600 group-hover:text-slate-300"></i>
              </button>

              <button
                onClick={() => onSelectTab("redis")}
                className="w-full p-3 bg-gov-950 hover:bg-gov-800/80 border border-gov-800 rounded-xl text-left text-xs transition flex items-center justify-between group"
              >
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-cyan-500/20 text-cyan-400 border border-cyan-500/30 flex items-center justify-center">
                    <i className="fa-solid fa-server"></i>
                  </div>
                  <div>
                    <div className="font-semibold text-slate-200 group-hover:text-white">Redis Cache & Telemetry</div>
                    <div className="text-[11px] text-slate-400">Purge cache and test sub-ms geo queries</div>
                  </div>
                </div>
                <i className="fa-solid fa-chevron-right text-slate-600 group-hover:text-slate-300"></i>
              </button>
            </div>
          </div>

          {/* Diagnostic Heartbeat */}
          {diagnostics && (
            <div className="mt-4 pt-4 border-t border-gov-800 text-[11px] text-slate-400 space-y-1">
              <div className="flex items-center justify-between">
                <span>JVM Memory:</span>
                <span className="font-mono text-slate-300">{diagnostics.jvmUsedMemoryMb} MB / {diagnostics.jvmTotalMemoryMb} MB</span>
              </div>
              <div className="flex items-center justify-between">
                <span>Database Health:</span>
                <span className="text-emerald-400 font-semibold">{diagnostics.databaseConnected ? "PostGIS Connected" : "Degraded"}</span>
              </div>
              <div className="flex items-center justify-between">
                <span>Redis Ping:</span>
                <span className="font-mono text-cyan-400">{diagnostics.redisPing}</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Recent Activity Ticker */}
      <div className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2">
            <i className="fa-solid fa-clock-rotate-left text-slate-400"></i>
            Recent Grievance Submissions & Triage Queue
          </h3>
          <button
            onClick={() => onSelectTab("triage")}
            className="text-xs text-civic-blue hover:underline"
          >
            Open Full Triage Board ({reports.length} Reports)
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-gov-950 text-slate-400 uppercase tracking-wider border-b border-gov-800">
              <tr>
                <th className="py-2.5 px-3">Report Code</th>
                <th className="py-2.5 px-3">Hazard Category</th>
                <th className="py-2.5 px-3">Ward / Locality</th>
                <th className="py-2.5 px-3">Reported Time</th>
                <th className="py-2.5 px-3">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gov-800/60">
              {reports.slice(0, 5).map(r => {
                const meta = getIssueMeta(r.issueType);
                const sMeta = getStatusMeta(r.status);
                return (
                  <tr key={r.id} className="hover:bg-gov-850/50 transition">
                    <td className="py-2.5 px-3 font-mono font-semibold text-cyan-400">{r.reportCode}</td>
                    <td className="py-2.5 px-3">
                      <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded border text-[11px] ${meta.bg} ${meta.border} ${meta.text}`}>
                        <i className={`fa-solid ${meta.icon}`}></i>
                        {meta.label}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 text-slate-300">{r.wardName || "Ward 85 - Karol Bagh"}</td>
                    <td className="py-2.5 px-3 text-slate-400 font-mono">{formatIST(r.reportedAt)}</td>
                    <td className="py-2.5 px-3">
                      <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded border text-[10px] uppercase font-bold ${sMeta.bg} ${sMeta.border} ${sMeta.text}`}>
                        <span className={`w-1.5 h-1.5 rounded-full ${sMeta.dot}`}></span>
                        {sMeta.label}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

// =============================================================================
// TAB 2: LIVE GRIEVANCE MANAGEMENT & TRIAGE
// =============================================================================
function TriageTab({ reports, wards, filter, setFilter, onInspect, onUpdateStatus, onReassignWard, apiFetch, showToast, onRefresh }) {
  const [purgingId, setPurgingId] = useState(null);

  // Filtered reports calculation
  const filteredReports = useMemo(() => {
    return reports.filter(r => {
      if (filter.status !== "ALL" && r.status !== filter.status) return false;
      if (filter.issueType !== "ALL" && r.issueType !== filter.issueType) return false;
      if (filter.wardNumber > 0 && r.wardNumber !== filter.wardNumber) return false;
      if (filter.search) {
        const q = filter.search.toLowerCase();
        const matchCode = r.reportCode?.toLowerCase().includes(q);
        const matchAddr = r.address?.toLowerCase().includes(q);
        const matchCit = r.citizenName?.toLowerCase().includes(q) || r.citizenPhone?.includes(q);
        const matchDesc = r.description?.toLowerCase().includes(q);
        if (!matchCode && !matchAddr && !matchCit && !matchDesc) return false;
      }
      return true;
    });
  }, [reports, filter]);

  // Purge spam / duplicate report
  const handlePurge = async (r) => {
    if (!confirm(`Are you sure you want to PURGE report ${r.reportCode} from the registry?`)) {
      return;
    }
    setPurgingId(r.id);
    try {
      await apiFetch(`/api/admin/reports/${r.id}`, { method: "DELETE" });
      showToast(`Report ${r.reportCode} successfully purged from database.`, "success");
      onRefresh();
    } catch (err) {
      showToast("Failed to delete report: " + err.message, "error");
    } finally {
      setPurgingId(null);
    }
  };

  return (
    <div className="space-y-4">
      {/* Filter and Search Bar */}
      <div className="bg-gov-900 border border-gov-800 rounded-xl p-4 shadow-sm glass-panel flex flex-col md:flex-row items-center justify-between gap-3">
        {/* Search Input */}
        <div className="relative w-full md:w-80">
          <i className="fa-solid fa-magnifying-glass text-slate-500 absolute left-3 top-3 text-xs"></i>
          <input
            type="text"
            value={filter.search}
            onChange={(e) => setFilter(prev => ({ ...prev, search: e.target.value }))}
            placeholder="Search code, address, citizen, phone..."
            className="w-full pl-9 pr-3 py-2 bg-gov-950 border border-gov-700 rounded-lg text-xs text-white placeholder-slate-500 focus:outline-none focus:border-civic-blue"
          />
        </div>

        {/* Dropdown Filters */}
        <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
          {/* Status Filter */}
          <select
            value={filter.status}
            onChange={(e) => setFilter(prev => ({ ...prev, status: e.target.value }))}
            className="px-3 py-2 bg-gov-950 border border-gov-700 rounded-lg text-xs text-white focus:outline-none focus:border-civic-blue"
          >
            <option value="ALL">All Statuses</option>
            <option value="SUBMITTED">Intake / Pending (SUBMITTED)</option>
            <option value="CLUSTERED">Hotspot Clustered (CLUSTERED)</option>
            <option value="ESCALATED">Escalated (ESCALATED)</option>
            <option value="IN_PROGRESS">Dispatched (IN_PROGRESS)</option>
            <option value="RESOLVED">Resolved (RESOLVED)</option>
            <option value="REJECTED">Rejected (REJECTED)</option>
          </select>

          {/* Issue Filter */}
          <select
            value={filter.issueType}
            onChange={(e) => setFilter(prev => ({ ...prev, issueType: e.target.value }))}
            className="px-3 py-2 bg-gov-950 border border-gov-700 rounded-lg text-xs text-white focus:outline-none focus:border-civic-blue"
          >
            <option value="ALL">All Issue Types</option>
            <option value="BURST_PIPE">Burst Pipeline</option>
            <option value="CONTAMINATION">Water Contamination</option>
            <option value="SEVERE_WATERLOGGING">Severe Waterlogging</option>
            <option value="DRAINAGE_OVERFLOW">Drainage Overflow</option>
            <option value="LOW_PRESSURE">Low Pressure</option>
            <option value="WATER_SCARCITY">Water Scarcity</option>
            <option value="OPEN_SEWAGE">Open Sewage</option>
          </select>

          {/* Ward Filter */}
          <select
            value={filter.wardNumber}
            onChange={(e) => setFilter(prev => ({ ...prev, wardNumber: Number.parseInt(e.target.value) || 0 }))}
            className="px-3 py-2 bg-gov-950 border border-gov-700 rounded-lg text-xs text-white focus:outline-none focus:border-civic-blue"
          >
            <option value="0">All Municipal Wards</option>
            {wards.map(w => (
              <option key={w.wardNumber} value={w.wardNumber}>Ward {w.wardNumber} - {w.wardName}</option>
            ))}
          </select>

          {/* Reset Button */}
          <button
            onClick={() => setFilter({ status: "ALL", issueType: "ALL", wardNumber: 0, search: "" })}
            className="px-3 py-2 bg-gov-800 hover:bg-gov-700 text-slate-300 rounded-lg text-xs font-medium transition"
            title="Reset Filters"
          >
            <i className="fa-solid fa-arrow-rotate-left"></i>
          </button>
        </div>
      </div>

      {/* Triage Results Count */}
      <div className="flex items-center justify-between text-xs text-slate-400 px-1">
        <span>Showing <strong className="text-white font-mono">{filteredReports.length}</strong> of {reports.length} registered grievances</span>
        <span>Delhi Jal Board Triage SLA: <strong>&lt; 4 Hours</strong></span>
      </div>

      {/* Reports Data Table */}
      <div className="bg-gov-900 border border-gov-800 rounded-xl shadow-sm glass-panel overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-gov-950 text-slate-400 uppercase tracking-wider border-b border-gov-800">
              <tr>
                <th className="py-3 px-3.5">Code</th>
                <th className="py-3 px-3.5">Hazard Type</th>
                <th className="py-3 px-3.5">Address & Ward</th>
                <th className="py-3 px-3.5">Citizen Reporter</th>
                <th className="py-3 px-3.5">Reported At</th>
                <th className="py-3 px-3.5">Status</th>
                <th className="py-3 px-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gov-800/60">
              {filteredReports.length === 0 ? (
                <tr>
                  <td colSpan="7" className="py-12 text-center text-slate-500">
                    <i className="fa-solid fa-filter text-2xl mb-2 text-slate-600 block"></i>
                    No grievances match the selected filter criteria.
                  </td>
                </tr>
              ) : (
                filteredReports.map(r => {
                  const meta = getIssueMeta(r.issueType);
                  const sMeta = getStatusMeta(r.status);
                  return (
                    <tr key={r.id} className="hover:bg-gov-850/50 transition">
                      {/* Code */}
                      <td className="py-3 px-3.5 font-mono font-bold text-cyan-400 whitespace-nowrap">
                        {r.reportCode}
                        {r.clusterId && (
                          <span className="block text-[10px] text-purple-400 font-sans">
                            <i className="fa-solid fa-circle-nodes mr-1"></i>Cluster #{r.clusterId}
                          </span>
                        )}
                      </td>

                      {/* Hazard */}
                      <td className="py-3 px-3.5 whitespace-nowrap">
                        <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded border text-[11px] font-medium ${meta.bg} ${meta.border} ${meta.text}`}>
                          <i className={`fa-solid ${meta.icon}`}></i>
                          {meta.label}
                        </span>
                      </td>

                      {/* Address & Ward */}
                      <td className="py-3 px-3.5 max-w-xs">
                        <div className="font-semibold text-slate-200 truncate">{r.address || "Pusa Road, Karol Bagh"}</div>
                        <div className="text-[11px] text-slate-400 flex items-center gap-1">
                          <i className="fa-solid fa-location-dot text-slate-500"></i>
                          {r.wardName || "Ward 85 Karol Bagh"} (Ward #{r.wardNumber || 85})
                        </div>
                      </td>

                      {/* Citizen */}
                      <td className="py-3 px-3.5 whitespace-nowrap">
                        <div className="font-medium text-slate-200">{r.citizenName || "Anonymous Resident"}</div>
                        <div className="text-[11px] text-slate-400 font-mono">{r.citizenPhone || "N/A"}</div>
                      </td>

                      {/* Reported At */}
                      <td className="py-3 px-3.5 text-slate-400 font-mono whitespace-nowrap">
                        {formatIST(r.reportedAt)}
                      </td>

                      {/* Status */}
                      <td className="py-3 px-3.5 whitespace-nowrap">
                        <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded border text-[10px] uppercase font-bold ${sMeta.bg} ${sMeta.border} ${sMeta.text}`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${sMeta.dot}`}></span>
                          {sMeta.label}
                        </span>
                      </td>

                      {/* Actions */}
                      <td className="py-3 px-3.5 text-right whitespace-nowrap">
                        <div className="inline-flex items-center gap-1">
                          <button
                            onClick={() => onInspect(r)}
                            title="Inspect Hardware Camera EXIF & Details"
                            className="px-2.5 py-1 bg-gov-800 hover:bg-gov-700 text-slate-200 rounded border border-gov-700 transition text-[11px] font-medium"
                          >
                            <i className="fa-solid fa-eye text-cyan-400 mr-1"></i> Inspect
                          </button>

                          <button
                            onClick={() => onUpdateStatus(r)}
                            title="Update Resolution Status & Field Notes"
                            className="px-2.5 py-1 bg-civic-blue/20 hover:bg-civic-blue/30 text-cyan-300 rounded border border-civic-blue/40 transition text-[11px] font-medium"
                          >
                            <i className="fa-solid fa-pen mr-1"></i> Triage
                          </button>

                          <button
                            onClick={() => onReassignWard(r)}
                            title="Reassign Ward Jurisdiction"
                            className="px-2.5 py-1 bg-purple-500/20 hover:bg-purple-500/30 text-purple-300 rounded border border-purple-500/40 transition text-[11px] font-medium"
                          >
                            <i className="fa-solid fa-arrows-split-up-and-left mr-1"></i> Reassign
                          </button>

                          <button
                            onClick={() => handlePurge(r)}
                            disabled={purgingId === r.id}
                            title="Purge Spam Report"
                            className="p-1 px-2 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 hover:text-rose-300 rounded border border-rose-500/30 transition text-[11px]"
                          >
                            <i className={`fa-solid ${purgingId === r.id ? "fa-circle-notch fa-spin" : "fa-trash"}`}></i>
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
      </div>
    </div>
  );
}

// =============================================================================
// TAB 3: SPATIAL DBSCAN CLUSTERS & AMRUT DOSSIERS
// =============================================================================
function ClustersTab({ clusters, apiFetch, showToast, onRefresh }) {
  const [epsMeters, setEpsMeters] = useState(150);
  const [minPoints, setMinPoints] = useState(3);
  const [escalationThreshold, setEscalationThreshold] = useState(5);
  const [running, setRunning] = useState(false);
  const [escalatingId, setEscalatingId] = useState(null);

  // Trigger DBSCAN Spatial Clustering
  const handleRunClustering = async () => {
    setRunning(true);
    try {
      const summary = await apiFetch(`/api/admin/clusters/run?epsMeters=${epsMeters}&minPoints=${minPoints}&escalationThreshold=${escalationThreshold}`, {
        method: "POST"
      });
      showToast(`DBSCAN Spatial Analysis complete! Discovered ${summary.newClustersCreated} new clusters. Total reports clustered: ${summary.totalReportsClustered}.`, "success");
      onRefresh();
    } catch (err) {
      showToast("Clustering execution failed: " + err.message, "error");
    } finally {
      setRunning(false);
    }
  };

  // Escalate cluster to Jal Board
  const handleEscalate = async (cluster) => {
    setEscalatingId(cluster.id);
    try {
      await apiFetch(`/api/admin/clusters/${cluster.id}/escalate`, { method: "POST" });
      showToast(`Cluster ${cluster.clusterCode} escalated and dispatched to ${cluster.municipalBody || "Delhi Jal Board"}.`, "success");
      onRefresh();
    } catch (err) {
      showToast("Escalation failed: " + err.message, "error");
    } finally {
      setEscalatingId(null);
    }
  };

  return (
    <div className="space-y-6">
      {/* DBSCAN Spatial Algorithm Control Panel */}
      <div className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 mb-4">
          <div>
            <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2">
              <i className="fa-solid fa-circle-nodes text-purple-400"></i>
              Geodesic DBSCAN Spatial Correlation Engine
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Executes high-precision Haversine spatial density clustering to aggregate concurrent citizen grievances into municipal incident zones.
            </p>
          </div>

          <button
            onClick={handleRunClustering}
            disabled={running}
            className="px-4 py-2.5 bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white rounded-lg text-xs font-bold transition shadow-lg shadow-purple-600/20 flex items-center justify-center gap-2 whitespace-nowrap"
          >
            <i className={`fa-solid ${running ? "fa-circle-notch fa-spin" : "fa-play"}`}></i>
            <span>{running ? "Executing DBSCAN Analysis..." : "Execute DBSCAN Spatial Analysis"}</span>
          </button>
        </div>

        {/* Sliders Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 pt-3 border-t border-gov-800 text-xs">
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <span className="text-slate-300 font-semibold">Epsilon Radius (ε):</span>
              <span className="font-mono text-cyan-400 font-bold">{epsMeters} Meters</span>
            </div>
            <input
              type="range"
              min="50"
              max="500"
              step="25"
              value={epsMeters}
              onChange={(e) => setEpsMeters(Number.parseFloat(e.target.value))}
              className="w-full h-1.5 bg-gov-800 rounded-lg appearance-none cursor-pointer accent-purple-500"
            />
            <span className="text-[10px] text-slate-500">Maximum neighbor distance</span>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1.5">
              <span className="text-slate-300 font-semibold">Min Points (MinPts):</span>
              <span className="font-mono text-cyan-400 font-bold">{minPoints} Reports</span>
            </div>
            <input
              type="range"
              min="2"
              max="10"
              value={minPoints}
              onChange={(e) => setMinPoints(Number.parseInt(e.target.value))}
              className="w-full h-1.5 bg-gov-800 rounded-lg appearance-none cursor-pointer accent-purple-500"
            />
            <span className="text-[10px] text-slate-500">Minimum density threshold</span>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1.5">
              <span className="text-slate-300 font-semibold">Escalation Trigger:</span>
              <span className="font-mono text-cyan-400 font-bold">{escalationThreshold} Reports</span>
            </div>
            <input
              type="range"
              min="3"
              max="15"
              value={escalationThreshold}
              onChange={(e) => setEscalationThreshold(Number.parseInt(e.target.value))}
              className="w-full h-1.5 bg-gov-800 rounded-lg appearance-none cursor-pointer accent-purple-500"
            />
            <span className="text-[10px] text-slate-500">Auto-dispatch to Executive Engineer</span>
          </div>
        </div>
      </div>

      {/* Clusters List Cards */}
      <div className="space-y-4">
        <div className="flex items-center justify-between text-xs text-slate-400 px-1">
          <span>Active Incident Clusters: <strong className="text-white font-mono">{clusters.length}</strong></span>
          <span>Standards: <strong>Ministry of Housing & Urban Affairs (AMRUT 2.0)</strong></span>
        </div>

        {clusters.length === 0 ? (
          <div className="bg-gov-900 border border-gov-800 rounded-xl p-12 text-center text-slate-500 glass-panel">
            <i className="fa-solid fa-circle-nodes text-4xl mb-3 text-slate-600 block"></i>
            <p className="text-sm font-semibold text-slate-300">No Spatial Clusters Identified Yet</p>
            <p className="text-xs text-slate-500 mt-1">Execute the DBSCAN Spatial Algorithm above to correlate nearby citizen reports into incident clusters.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {clusters.map(c => {
              const isCritical = c.severity === "CRITICAL";
              return (
                <div
                  key={c.id}
                  className={`bg-gov-900 border rounded-xl p-5 shadow-sm glass-panel relative overflow-hidden flex flex-col justify-between ${
                    isCritical ? "border-rose-500/50" : "border-gov-800"
                  }`}
                >
                  <div>
                    {/* Top Bar */}
                    <div className="flex items-start justify-between gap-3 mb-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono font-bold text-sm text-cyan-300">{c.clusterCode}</span>
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded uppercase ${
                            c.severity === "CRITICAL" ? "bg-rose-500/20 text-rose-300 border border-rose-500/40 animate-pulse" :
                            c.severity === "HIGH" ? "bg-amber-500/20 text-amber-300 border border-amber-500/40" :
                            "bg-blue-500/20 text-blue-300 border border-blue-500/40"
                          }`}>
                            {c.severity} SEVERITY
                          </span>
                        </div>
                        <div className="text-xs text-slate-300 font-semibold mt-1">
                          {c.municipalBody || "Delhi Jal Board (DJB)"} • Ward #{c.wardNumber || 85}
                        </div>
                      </div>

                      <div className="text-right">
                        <span className="px-2.5 py-1 rounded bg-gov-950 border border-gov-800 text-xs font-mono font-bold text-white">
                          <i className="fa-solid fa-users text-civic-blue mr-1"></i> {c.reportCount} Reports
                        </span>
                        <div className="text-[10px] text-slate-500 font-mono mt-1">
                          Radius: ~{Math.round(c.radiusMeters || 120)}m
                        </div>
                      </div>
                    </div>

                    {/* Root Cause & Engineering Remedy */}
                    <div className="space-y-2 mb-4">
                      <div className="p-3 bg-gov-950 border border-gov-800 rounded-lg text-xs">
                        <span className="text-[10px] font-bold uppercase tracking-wider text-purple-400 block mb-1">
                          <i className="fa-solid fa-brain mr-1"></i> AI Root Cause Hypothesis
                        </span>
                        <p className="text-slate-200 leading-relaxed font-medium">
                          {c.rootCauseHypothesis || "Arterial Feeder Main Rupture - High Pressure Stress"}
                        </p>
                      </div>

                      {c.recommendedAction && (
                        <div className="p-3 bg-gov-950 border border-gov-800 rounded-lg text-xs">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-400 block mb-1">
                            <i className="fa-solid fa-wrench mr-1"></i> Recommended Engineering Action
                          </span>
                          <p className="text-slate-300 leading-relaxed">
                            {c.recommendedAction}
                          </p>
                        </div>
                      )}
                    </div>

                    {/* Centroid Coordinates & Status */}
                    <div className="flex items-center justify-between text-xs text-slate-400 mb-4 pb-3 border-b border-gov-800">
                      <div className="flex items-center gap-1.5 font-mono">
                        <i className="fa-solid fa-location-crosshairs text-slate-500"></i>
                        <span>{c.centroidLat?.toFixed(4)}° N, {c.centroidLon?.toFixed(4)}° E</span>
                      </div>
                      <div className="flex items-center gap-1.5">
                        <span className="text-[10px] uppercase font-bold text-slate-300">Status:</span>
                        <span className="font-semibold text-cyan-400 uppercase">{c.status}</span>
                      </div>
                    </div>
                  </div>

                  {/* Actions Bar */}
                  <div className="flex flex-wrap items-center gap-2 pt-1">
                    {/* AMRUT 2.0 Official PDF Dossier Download */}
                    <a
                      href={`/api/clusters/${c.id}/pdf`}
                      download={`AMRUT-Incident-Dossier-${c.clusterCode}.pdf`}
                      className="flex-1 py-2 px-3 bg-civic-blue hover:bg-blue-600 text-white rounded-lg text-xs font-semibold transition text-center shadow-md shadow-civic-blue/20 flex items-center justify-center gap-1.5"
                    >
                      <i className="fa-solid fa-file-pdf"></i>
                      <span>AMRUT Dossier PDF</span>
                    </a>

                    {/* Escalate Button */}
                    <button
                      onClick={() => handleEscalate(c)}
                      disabled={escalatingId === c.id || c.status === "ESCALATED"}
                      className="py-2 px-3 bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/40 rounded-lg text-xs font-semibold transition flex items-center gap-1.5"
                    >
                      <i className={`fa-solid ${escalatingId === c.id ? "fa-circle-notch fa-spin" : "fa-paper-plane"}`}></i>
                      <span>{c.status === "ESCALATED" ? "Escalated" : "Escalate to DJB"}</span>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

// =============================================================================
// TAB 4: EMERGENCY FLEET & TANKER COORDINATION
// =============================================================================
function FleetTab({ depots, tankers, reports, apiFetch, showToast, onRefresh }) {
  const mapRef = useRef(null);
  const leafletInstance = useRef(null);
  const routePolyline = useRef(null);

  const [selectedIncident, setSelectedIncident] = useState(reports[0] || null);
  const [dispatchResult, setDispatchResult] = useState(null);
  const [dispatching, setDispatching] = useState(false);

  // Initialize Leaflet Map
  useEffect(() => {
    if (!mapRef.current) return;

    if (!leafletInstance.current) {
      // Default centered on New Delhi (Varunalaya HQ)
      const map = L.map(mapRef.current).setView([28.6445, 77.2100], 12);
      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: '&copy; <a href="https://openstreetmap.org">OpenStreetMap</a> contributors',
        maxZoom: 18
      }).addTo(map);

      leafletInstance.current = map;
    }

    const map = leafletInstance.current;

    // Render Depots
    depots.forEach(depot => {
      const depotIcon = L.divIcon({
        className: 'custom-depot-pin',
        html: '<i class="fa-solid fa-building-flag"></i>',
        iconSize: [32, 32],
        iconAnchor: [16, 16]
      });

      L.marker([depot.latitude, depot.longitude], { icon: depotIcon })
        .addTo(map)
        .bindPopup(`
          <div style="font-size:12px; line-height:1.4;">
            <strong style="color:#38bdf8;">${depot.name}</strong><br/>
            <strong>Code:</strong> ${depot.depotCode}<br/>
            <strong>In-Charge:</strong> ${depot.inChargeOfficer}<br/>
            <strong>VHF Radio:</strong> ${depot.vhfRadioChannel}<br/>
            <strong>Hotline:</strong> ${depot.emergencyHotline}
          </div>
        `);
    });

    // Render Tankers
    tankers.forEach(tanker => {
      const tankerIcon = L.divIcon({
        className: 'custom-tanker-pin',
        html: '<i class="fa-solid fa-truck-droplet"></i>',
        iconSize: [32, 32],
        iconAnchor: [16, 16]
      });

      L.marker([tanker.latitude, tanker.longitude], { icon: tankerIcon })
        .addTo(map)
        .bindPopup(`
          <div style="font-size:12px; line-height:1.4;">
            <strong style="color:#4ade80;">Tanker Unit: ${tanker.vehicleNumber}</strong><br/>
            <strong>Capacity:</strong> ${tanker.capacityLiters} Liters<br/>
            <strong>Driver:</strong> ${tanker.driverName} (${tanker.driverPhone})<br/>
            <strong>Status:</strong> ${tanker.status}<br/>
            <strong>Assigned Destination:</strong> ${tanker.destinationWard}
          </div>
        `);
    });

  }, [depots, tankers]);

  // Execute Rapid Dispatch Calculation
  const handleDispatchCoordination = async () => {
    if (!selectedIncident) {
      showToast("Please select a target grievance or coordinate first.", "error");
      return;
    }

    setDispatching(true);
    try {
      const lat = selectedIncident.latitude || 28.6445;
      const lon = selectedIncident.longitude || 77.1950;
      const desc = `${selectedIncident.reportCode || "EMERGENCY"}: ${selectedIncident.issueType || "HAZARD"}`;

      const plan = await apiFetch(`/api/admin/fleet/dispatch?lat=${lat}&lon=${lon}&desc=${encodeURIComponent(desc)}`, {
        method: "POST"
      });

      setDispatchResult(plan);
      showToast(`Rapid route generated! Assigned Depot: ${plan.assignedDepot?.name}. ETA: ${plan.estimatedMinutes} Mins.`, "success");

      // Draw polyline on Leaflet map
      if (leafletInstance.current && plan.routeCoordinates) {
        if (routePolyline.current) {
          leafletInstance.current.removeLayer(routePolyline.current);
        }
        // Leaflet expects [lat, lon], routeCoordinates are [lon, lat] GeoJSON format
        const latLngs = plan.routeCoordinates.map(c => [c[1], c[0]]);
        routePolyline.current = L.polyline(latLngs, { color: '#0284c7', weight: 5, opacity: 0.85, dashArray: '8, 8' }).addTo(leafletInstance.current);
        leafletInstance.current.fitBounds(routePolyline.current.getBounds(), { padding: [40, 40] });
      }
    } catch (err) {
      showToast("Dispatch coordination failed: " + err.message, "error");
    } finally {
      setDispatching(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Map & Dispatch Calculator Split Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Interactive Map (2 Columns) */}
        <div className="lg:col-span-2 bg-gov-900 border border-gov-800 rounded-xl p-4 shadow-sm glass-panel flex flex-col">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2">
              <i className="fa-solid fa-map-location-dot text-emerald-400"></i>
              Delhi Emergency Fleet Telemetry & Tactical Map
            </h3>
            <div className="flex items-center gap-3 text-xs">
              <span className="flex items-center gap-1.5 text-slate-300">
                <span className="w-2.5 h-2.5 rounded-full bg-civic-blue"></span> Depots ({depots.length})
              </span>
              <span className="flex items-center gap-1.5 text-slate-300">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-400"></span> Active Tankers ({tankers.length})
              </span>
            </div>
          </div>

          <div
            ref={mapRef}
            className="w-full h-[450px] rounded-lg border border-gov-800 overflow-hidden relative z-0"
          ></div>
        </div>

        {/* Dispatch Action & Calculation Panel */}
        <div className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel flex flex-col justify-between">
          <div>
            <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-3 flex items-center gap-2">
              <i className="fa-solid fa-truck-fast text-amber-400"></i>
              Emergency Fleet Dispatcher
            </h3>
            <p className="text-xs text-slate-400 mb-4">
              Determines optimal emergency base, computes geodesic & road distances, and calculates traffic-adjusted turn-by-turn ETA.
            </p>

            {/* Select Target Incident */}
            <div className="space-y-3 mb-4 text-xs">
              <div>
                <label className="block font-semibold text-slate-300 mb-1">Target Grievance Hotspot</label>
                <select
                  value={selectedIncident?.id || ""}
                  onChange={(e) => {
                    const id = Number.parseInt(e.target.value);
                    const found = reports.find(r => r.id === id);
                    setSelectedIncident(found);
                  }}
                  className="w-full px-3 py-2 bg-gov-950 border border-gov-700 rounded-lg text-white focus:outline-none focus:border-civic-blue"
                >
                  {reports.slice(0, 10).map(r => (
                    <option key={r.id} value={r.id}>
                      {r.reportCode} - {r.wardName || "Ward"} ({r.issueType})
                    </option>
                  ))}
                </select>
              </div>

              {selectedIncident && (
                <div className="p-3 bg-gov-950 border border-gov-800 rounded-lg space-y-1 font-mono text-[11px]">
                  <div className="text-slate-400">Locality: <strong className="text-white">{selectedIncident.address}</strong></div>
                  <div className="text-slate-400">Coords: <strong className="text-cyan-400">{selectedIncident.latitude?.toFixed(4)}, {selectedIncident.longitude?.toFixed(4)}</strong></div>
                </div>
              )}

              <button
                onClick={handleDispatchCoordination}
                disabled={dispatching}
                className="w-full py-2.5 bg-civic-blue hover:bg-blue-600 text-white rounded-lg text-xs font-bold transition shadow-md shadow-civic-blue/20 flex items-center justify-center gap-2"
              >
                {dispatching ? (
                  <>
                    <i className="fa-solid fa-circle-notch fa-spin"></i>
                    <span>Computing Optimal Route & Mobilizing...</span>
                  </>
                ) : (
                  <>
                    <i className="fa-solid fa-paper-plane"></i>
                    <span>Calculate Navigation & Mobilize Unit</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Dispatch Calculation Result Dossier */}
          {dispatchResult && (
            <div className="p-3.5 bg-gov-950 border border-civic-blue/40 rounded-xl space-y-2 text-xs">
              <div className="font-bold text-cyan-300 flex items-center justify-between">
                <span>DISPATCH ROUTE PLAN</span>
                <span className="font-mono text-emerald-400 text-sm">{dispatchResult.estimatedMinutes} Mins ETA</span>
              </div>
              <div className="text-slate-300">
                <strong>Assigned Base:</strong> {dispatchResult.assignedDepot?.name}
              </div>
              <div className="text-slate-300">
                <strong>Road Distance:</strong> {dispatchResult.roadDistanceKm} km ({dispatchResult.cardinalDirection} heading)
              </div>
              <div className="text-slate-300">
                <strong>Traffic Condition:</strong> <span className="text-amber-400">{dispatchResult.trafficCondition}</span>
              </div>
              <div className="text-slate-300">
                <strong>Recommended Vehicle:</strong> <span className="text-cyan-400 font-semibold">{dispatchResult.recommendedVehicleType}</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Tanker Fleet Roster Table */}
      <div className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-4 flex items-center gap-2">
          <i className="fa-solid fa-table-list text-cyan-400"></i>
          Delhi Jal Board Emergency Potable Tanker Fleet Roster
        </h3>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-gov-950 text-slate-400 uppercase tracking-wider border-b border-gov-800">
              <tr>
                <th className="py-2.5 px-3">Vehicle Number</th>
                <th className="py-2.5 px-3">Capacity</th>
                <th className="py-2.5 px-3">Home Depot Base</th>
                <th className="py-2.5 px-3">Driver Name & Contact</th>
                <th className="py-2.5 px-3">Operational Status</th>
                <th className="py-2.5 px-3">Assigned Destination</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gov-800/60">
              {tankers.map(t => (
                <tr key={t.vehicleNumber} className="hover:bg-gov-850/50 transition">
                  <td className="py-2.5 px-3 font-mono font-bold text-white">{t.vehicleNumber}</td>
                  <td className="py-2.5 px-3 font-mono text-cyan-400 font-semibold">{t.capacityLiters?.toLocaleString()} Liters</td>
                  <td className="py-2.5 px-3 text-slate-300">{t.depotCode}</td>
                  <td className="py-2.5 px-3">
                    <div className="font-semibold text-slate-200">{t.driverName}</div>
                    <div className="text-[11px] text-slate-400 font-mono">{t.driverPhone}</div>
                  </td>
                  <td className="py-2.5 px-3">
                    <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                      t.status === "DISPATCHED" ? "bg-amber-500/20 text-amber-300 border border-amber-500/40" :
                      t.status === "FILLING_AT_HYDRANT" ? "bg-blue-500/20 text-blue-300 border border-blue-500/40" :
                      "bg-emerald-500/20 text-emerald-300 border border-emerald-500/40"
                    }`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${t.status === "DISPATCHED" ? "bg-amber-400 animate-ping" : "bg-emerald-400"}`}></span>
                      {t.status}
                    </span>
                  </td>
                  <td className="py-2.5 px-3 text-slate-300 font-medium">{t.destinationWard}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

// =============================================================================
// TAB 5: MUNICIPAL WARDS GIS DIRECTORY
// =============================================================================
function WardsTab({ wards, apiFetch, showToast }) {
  const [lookupLat, setLookupLat] = useState("28.6445");
  const [lookupLon, setLookupLon] = useState("77.1950");
  const [lookupResult, setLookupResult] = useState(null);
  const [lookingUp, setLookingUp] = useState(false);

  const handleLookup = async (e) => {
    e.preventDefault();
    setLookingUp(true);
    try {
      const res = await apiFetch(`/api/wards/lookup?lat=${lookupLat}&lon=${lookupLon}`);
      setLookupResult(res);
      showToast(`Coordinate mapped into: ${res.ward?.wardName || "Ward"} (Direct Containment: ${res.directlyContained})`, "info");
    } catch (err) {
      showToast("Lookup failed: " + err.message, "error");
    } finally {
      setLookingUp(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* GPS Ward Boundary Resolver Tool */}
      <div className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-2 flex items-center gap-2">
          <i className="fa-solid fa-crosshairs text-cyan-400"></i>
          GPS Ward Jurisdiction Resolver
        </h3>
        <p className="text-xs text-slate-400 mb-4">
          Determines which of Delhi's 6 master municipal wards directly contains specific GPS coordinates, calculating geodesic distance to centroid.
        </p>

        <form onSubmit={handleLookup} className="flex flex-col sm:flex-row items-center gap-3">
          <input
            type="number"
            step="any"
            value={lookupLat}
            onChange={(e) => setLookupLat(e.target.value)}
            placeholder="Latitude (e.g. 28.6445)"
            required
            className="w-full sm:w-1/3 px-3 py-2 bg-gov-950 border border-gov-700 rounded-lg text-xs text-white focus:outline-none focus:border-civic-blue font-mono"
          />
          <input
            type="number"
            step="any"
            value={lookupLon}
            onChange={(e) => setLookupLon(e.target.value)}
            placeholder="Longitude (e.g. 77.1950)"
            required
            className="w-full sm:w-1/3 px-3 py-2 bg-gov-950 border border-gov-700 rounded-lg text-xs text-white focus:outline-none focus:border-civic-blue font-mono"
          />
          <button
            type="submit"
            disabled={lookingUp}
            className="w-full sm:w-auto px-5 py-2 bg-civic-blue hover:bg-blue-600 text-white rounded-lg text-xs font-bold transition flex items-center justify-center gap-2"
          >
            <i className={`fa-solid ${lookingUp ? "fa-circle-notch fa-spin" : "fa-magnifying-glass"}`}></i>
            <span>Resolve Ward</span>
          </button>
        </form>

        {lookupResult && lookupResult.ward && (
          <div className="mt-4 p-4 bg-gov-950 border border-emerald-500/40 rounded-xl text-xs space-y-1">
            <div className="font-bold text-emerald-400 text-sm flex items-center justify-between">
              <span>{lookupResult.ward.wardName} (Ward #{lookupResult.ward.wardNumber})</span>
              <span className="font-mono text-xs">{lookupResult.directlyContained ? "✓ Direct Containment" : "Centroid Proximity"}</span>
            </div>
            <div className="text-slate-300">
              <strong>Executive Engineer:</strong> {lookupResult.ward.officerName} ({lookupResult.ward.officerDesignation})
            </div>
            <div className="text-slate-300">
              <strong>Emergency Hotline:</strong> <span className="font-mono text-cyan-400">{lookupResult.ward.emergencyHotline}</span>
            </div>
            <div className="text-slate-400 font-mono text-[11px]">
              Distance to Centroid: {lookupResult.distanceToCentroidMeters} meters
            </div>
          </div>
        )}
      </div>

      {/* Ward Directory Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {wards.map(w => (
          <div
            key={w.wardNumber}
            className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel flex flex-col justify-between"
          >
            <div>
              <div className="flex items-start justify-between gap-2 mb-3">
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-cyan-400 bg-cyan-500/10 px-2 py-0.5 rounded border border-cyan-500/20">
                    PIN {w.pincode}
                  </span>
                  <h4 className="text-sm font-bold text-white mt-1.5">{w.wardName}</h4>
                  <div className="text-xs text-slate-400">{w.municipalBody}</div>
                </div>
                <div className="text-right">
                  <span className="font-mono text-lg font-bold text-slate-200">#{w.wardNumber}</span>
                </div>
              </div>

              <div className="space-y-2 py-2 border-t border-gov-800 text-xs">
                <div>
                  <span className="text-slate-400 block text-[11px]">Nodal Executive Engineer:</span>
                  <strong className="text-slate-200">{w.officerName}</strong>
                </div>
                <div>
                  <span className="text-slate-400 block text-[11px]">Contact Hotline:</span>
                  <span className="text-cyan-400 font-mono font-semibold">{w.contactPhone}</span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[11px]">Official Email:</span>
                  <span className="text-slate-300 font-mono truncate block">{w.contactEmail}</span>
                </div>
              </div>
            </div>

            <div className="mt-3 pt-3 border-t border-gov-800 flex items-center justify-between text-[11px] font-mono text-slate-500">
              <span>Centroid: {w.centerLat?.toFixed(4)}, {w.centerLon?.toFixed(4)}</span>
              <span className="text-emerald-400 font-sans font-bold">24x7 Ready</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// =============================================================================
// TAB 6: DISPATCH AUDIT TRAIL
// =============================================================================
function AuditTab({ auditLogs, onInspectAudit }) {
  const [channelFilter, setChannelFilter] = useState("ALL");

  const filteredLogs = useMemo(() => {
    if (channelFilter === "ALL") return auditLogs;
    return auditLogs.filter(l => l.dispatchChannel === channelFilter);
  }, [auditLogs, channelFilter]);

  return (
    <div className="space-y-4">
      {/* Top Filter Bar */}
      <div className="bg-gov-900 border border-gov-800 rounded-xl p-4 shadow-sm glass-panel flex items-center justify-between">
        <div>
          <h3 className="text-sm font-bold text-white uppercase tracking-wider flex items-center gap-2">
            <i className="fa-solid fa-clock-rotate-left text-slate-400"></i>
            Automated Escalation Dispatch Audit Trail
          </h3>
          <p className="text-xs text-slate-400 mt-0.5">
            Immutable log of all automated Email notifications, Municipal Webhook transfers, and Citizen SMS alerts.
          </p>
        </div>

        <select
          value={channelFilter}
          onChange={(e) => setChannelFilter(e.target.value)}
          className="px-3 py-1.5 bg-gov-950 border border-gov-700 rounded-lg text-xs text-white focus:outline-none focus:border-civic-blue"
        >
          <option value="ALL">All Channels</option>
          <option value="EMAIL">Email Dispatches</option>
          <option value="WEBHOOK">Municipal Webhooks</option>
          <option value="SMS">Citizen SMS Notices</option>
        </select>
      </div>

      {/* Audit Table */}
      <div className="bg-gov-900 border border-gov-800 rounded-xl shadow-sm glass-panel overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-gov-950 text-slate-400 uppercase tracking-wider border-b border-gov-800">
              <tr>
                <th className="py-3 px-3.5">Dispatched At</th>
                <th className="py-3 px-3.5">Cluster Code</th>
                <th className="py-3 px-3.5">Ward</th>
                <th className="py-3 px-3.5">Channel</th>
                <th className="py-3 px-3.5">Recipient Endpoint</th>
                <th className="py-3 px-3.5">Delivery Status</th>
                <th className="py-3 px-3.5 text-right">Payload</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gov-800/60">
              {filteredLogs.length === 0 ? (
                <tr>
                  <td colSpan="7" className="py-12 text-center text-slate-500">
                    No dispatch audit logs registered yet.
                  </td>
                </tr>
              ) : (
                filteredLogs.map(l => (
                  <tr key={l.id} className="hover:bg-gov-850/50 transition">
                    <td className="py-3 px-3.5 font-mono text-slate-400 whitespace-nowrap">
                      {formatIST(l.dispatchedAt)}
                    </td>
                    <td className="py-3 px-3.5 font-mono font-bold text-cyan-400">
                      {l.clusterCode || "IND-CLUST"}
                    </td>
                    <td className="py-3 px-3.5 text-slate-300">
                      Ward #{l.wardNumber || "85"}
                    </td>
                    <td className="py-3 px-3.5">
                      <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                        l.dispatchChannel === "EMAIL" ? "bg-blue-500/20 text-blue-300 border border-blue-500/30" :
                        l.dispatchChannel === "WEBHOOK" ? "bg-purple-500/20 text-purple-300 border border-purple-500/30" :
                        "bg-emerald-500/20 text-emerald-300 border border-emerald-500/30"
                      }`}>
                        {l.dispatchChannel}
                      </span>
                    </td>
                    <td className="py-3 px-3.5 font-mono text-slate-300 max-w-xs truncate">
                      {l.recipient}
                    </td>
                    <td className="py-3 px-3.5">
                      <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                        l.status === "DELIVERED" ? "bg-emerald-500/20 text-emerald-400" : "bg-cyan-500/20 text-cyan-400"
                      }`}>
                        <i className="fa-solid fa-check"></i> {l.status}
                      </span>
                    </td>
                    <td className="py-3 px-3.5 text-right">
                      <button
                        onClick={() => onInspectAudit(l)}
                        className="px-2.5 py-1 bg-gov-800 hover:bg-gov-700 text-slate-300 rounded border border-gov-700 transition text-[11px]"
                      >
                        Inspect
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

// =============================================================================
// TAB 7: REDIS CACHE & GEOSPATIAL TELEMETRY
// =============================================================================
function RedisTab({ redisStats, apiFetch, showToast, onRefresh }) {
  const [clearing, setClearing] = useState(false);
  const [geoLat, setGeoLat] = useState("28.6445");
  const [geoLon, setGeoLon] = useState("77.1950");
  const [geoRadius, setGeoRadius] = useState("2.5");
  const [geoResult, setGeoResult] = useState(null);
  const [geoLoading, setGeoLoading] = useState(false);

  // Evict Cache
  const handleClearCache = async (cacheName = "all") => {
    setClearing(true);
    try {
      const res = await apiFetch(`/api/admin/cache/clear?cacheName=${cacheName}`, { method: "POST" });
      showToast(res.message || "Cache partition successfully evicted.", "success");
      onRefresh();
    } catch (err) {
      showToast("Cache flush failed: " + err.message, "error");
    } finally {
      setClearing(false);
    }
  };

  // Sub-millisecond Geo Radius Proximity Query
  const handleGeoSearch = async (e) => {
    e.preventDefault();
    setGeoLoading(true);
    try {
      const res = await apiFetch(`/api/cache/geo/nearby?lat=${geoLat}&lon=${geoLon}&radiusKm=${geoRadius}`);
      setGeoResult(res);
      showToast(`Redis GeoSearch found ${res.totalFound} reports in ${res.queryDurationMicroseconds} microseconds!`, "info");
    } catch (err) {
      showToast("GeoSearch failed: " + err.message, "error");
    } finally {
      setGeoLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Cluster Health Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase">Redis Health Status</span>
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse"></span>
          </div>
          <div className="text-2xl font-bold text-white mt-2 font-mono flex items-center gap-2">
            <i className="fa-solid fa-server text-cyan-400"></i>
            {redisStats?.status || "HEALTHY"}
          </div>
          <div className="text-xs text-slate-400 mt-1 font-mono">
            Ping Response: <strong className="text-emerald-400">{redisStats?.redisPing || "PONG"}</strong>
          </div>
        </div>

        <div className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase">Native 2D Geo Index</span>
            <i className="fa-solid fa-earth-asia text-emerald-400"></i>
          </div>
          <div className="text-2xl font-bold text-white mt-2 font-mono">
            {redisStats?.geospatialIndexedReports || 0} Points
          </div>
          <div className="text-xs text-slate-400 mt-1">
            Sub-millisecond GEO SEARCH Index
          </div>
        </div>

        <div className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase">Active Namespaces</span>
            <i className="fa-solid fa-database text-purple-400"></i>
          </div>
          <div className="text-2xl font-bold text-white mt-2 font-mono">
            {redisStats?.crowdflowKeyCount || 0} Keys
          </div>
          <div className="text-xs text-slate-400 mt-1">
            Namespace: <code className="text-slate-300">crowdflow:*</code>
          </div>
        </div>
      </div>

      {/* Cache Eviction Controls */}
      <div className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-2 flex items-center gap-2">
          <i className="fa-solid fa-broom text-amber-400"></i>
          Cache Partition Management & Selective Eviction
        </h3>
        <p className="text-xs text-slate-400 mb-4">
          Evict specific cache partitions to force fresh database recalculation across geospatial heatmaps, cluster dossiers, and ward routes.
        </p>

        <div className="flex flex-wrap gap-2.5">
          <button
            onClick={() => handleClearCache("heatmap")}
            disabled={clearing}
            className="px-3.5 py-2 bg-gov-800 hover:bg-gov-700 text-slate-200 rounded-lg text-xs font-semibold transition border border-gov-700 flex items-center gap-2"
          >
            <i className="fa-solid fa-fire text-amber-400"></i>
            <span>Evict 'heatmap'</span>
          </button>

          <button
            onClick={() => handleClearCache("clusters_open")}
            disabled={clearing}
            className="px-3.5 py-2 bg-gov-800 hover:bg-gov-700 text-slate-200 rounded-lg text-xs font-semibold transition border border-gov-700 flex items-center gap-2"
          >
            <i className="fa-solid fa-circle-nodes text-purple-400"></i>
            <span>Evict 'clusters_open'</span>
          </button>

          <button
            onClick={() => handleClearCache("live_tracking")}
            disabled={clearing}
            className="px-3.5 py-2 bg-gov-800 hover:bg-gov-700 text-slate-200 rounded-lg text-xs font-semibold transition border border-gov-700 flex items-center gap-2"
          >
            <i className="fa-solid fa-tower-broadcast text-cyan-400"></i>
            <span>Evict 'live_tracking'</span>
          </button>

          <button
            onClick={() => handleClearCache("wards")}
            disabled={clearing}
            className="px-3.5 py-2 bg-gov-800 hover:bg-gov-700 text-slate-200 rounded-lg text-xs font-semibold transition border border-gov-700 flex items-center gap-2"
          >
            <i className="fa-solid fa-map-location-dot text-emerald-400"></i>
            <span>Evict 'wards'</span>
          </button>

          <button
            onClick={() => handleClearCache("all")}
            disabled={clearing}
            className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white rounded-lg text-xs font-bold transition shadow-md shadow-rose-600/20 flex items-center gap-2"
          >
            <i className={`fa-solid ${clearing ? "fa-circle-notch fa-spin" : "fa-trash-can"}`}></i>
            <span>Purge All Partitions</span>
          </button>
        </div>
      </div>

      {/* Sub-Millisecond Geospatial Radius Proximity Tester */}
      <div className="bg-gov-900 border border-gov-800 rounded-xl p-5 shadow-sm glass-panel">
        <h3 className="text-sm font-bold text-white uppercase tracking-wider mb-2 flex items-center gap-2">
          <i className="fa-solid fa-radar text-cyan-400"></i>
          Sub-Millisecond Native Redis GEO SEARCH Proximity Tester
        </h3>
        <p className="text-xs text-slate-400 mb-4">
          Queries Redis spatial tree using geodesic Haversine distance, benchmarking latency in microseconds.
        </p>

        <form onSubmit={handleGeoSearch} className="flex flex-col sm:flex-row items-center gap-3">
          <input
            type="number"
            step="any"
            value={geoLat}
            onChange={(e) => setGeoLat(e.target.value)}
            placeholder="Latitude (e.g. 28.6445)"
            required
            className="w-full sm:w-1/3 px-3 py-2 bg-gov-950 border border-gov-700 rounded-lg text-xs text-white focus:outline-none focus:border-civic-blue font-mono"
          />
          <input
            type="number"
            step="any"
            value={geoLon}
            onChange={(e) => setGeoLon(e.target.value)}
            placeholder="Longitude (e.g. 77.1950)"
            required
            className="w-full sm:w-1/3 px-3 py-2 bg-gov-950 border border-gov-700 rounded-lg text-xs text-white focus:outline-none focus:border-civic-blue font-mono"
          />
          <input
            type="number"
            step="any"
            value={geoRadius}
            onChange={(e) => setGeoRadius(e.target.value)}
            placeholder="Radius in KM"
            required
            className="w-full sm:w-1/4 px-3 py-2 bg-gov-950 border border-gov-700 rounded-lg text-xs text-white focus:outline-none focus:border-civic-blue font-mono"
          />
          <button
            type="submit"
            disabled={geoLoading}
            className="w-full sm:w-auto px-5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-bold transition flex items-center justify-center gap-2 whitespace-nowrap"
          >
            <i className={`fa-solid ${geoLoading ? "fa-circle-notch fa-spin" : "fa-bolt"}`}></i>
            <span>Test Proximity</span>
          </button>
        </form>

        {geoResult && (
          <div className="mt-4 p-4 bg-gov-950 border border-gov-800 rounded-xl text-xs space-y-2">
            <div className="flex items-center justify-between font-mono">
              <span className="text-slate-300 font-bold">Query Execution Duration:</span>
              <span className="text-emerald-400 font-bold text-sm">{geoResult.queryDurationMicroseconds} µs (Microseconds)</span>
            </div>
            <div className="text-slate-400">
              Found <strong className="text-white">{geoResult.totalFound} reports</strong> within {geoResult.radiusKm} km radius.
            </div>
            {geoResult.reports && geoResult.reports.length > 0 && (
              <div className="pt-2 border-t border-gov-800 space-y-1">
                {geoResult.reports.map(r => (
                  <div key={r.reportCode} className="flex items-center justify-between text-slate-300 font-mono text-[11px]">
                    <span className="text-cyan-400 font-bold">{r.reportCode}</span>
                    <span>{r.issueLabel || r.issueType}</span>
                    <span>{r.wardName || "Ward 85"}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// =============================================================================
// MODAL: INSPECT REPORT & HARDWARE CAMERA EXIF TELEMETRY
// =============================================================================
function InspectReportModal({ report, onClose, onUpdateStatus }) {
  const meta = getIssueMeta(report.issueType);
  const sMeta = getStatusMeta(report.status);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 modal-backdrop animate-fadeIn">
      <div className="bg-gov-900 border border-gov-700 max-w-2xl w-full rounded-2xl shadow-2xl p-6 relative max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between gap-4 mb-4 pb-3 border-b border-gov-800">
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono font-bold text-lg text-cyan-400">{report.reportCode}</span>
              <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded border text-[11px] font-medium ${meta.bg} ${meta.border} ${meta.text}`}>
                <i className={`fa-solid ${meta.icon}`}></i>
                {meta.label}
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-0.5">{report.address}</p>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white text-lg p-1 transition"
          >
            <i className="fa-solid fa-xmark"></i>
          </button>
        </div>

        {/* Media Evidence & Hardware EXIF Details */}
        <div className="space-y-4 text-xs">
          {/* Photo Evidence */}
          {report.imageUrl && (
            <div>
              <span className="font-semibold text-slate-300 block mb-1.5">Photo Evidence (Sanitized & Stripped):</span>
              <div className="w-full h-56 bg-gov-950 rounded-xl border border-gov-800 overflow-hidden flex items-center justify-center">
                {report.imageUrl.startsWith("data:image/svg") ? (
                  <div dangerouslySetInnerHTML={{ __html: decodeURIComponent(report.imageUrl.replace("data:image/svg+xml;utf8,", "")) }} className="w-full h-full flex items-center justify-center" />
                ) : (
                  <img src={report.imageUrl} alt="Evidence" className="w-full h-full object-cover" />
                )}
              </div>
            </div>
          )}

          {/* Description */}
          <div className="p-3 bg-gov-950 border border-gov-800 rounded-lg">
            <span className="font-semibold text-slate-300 block mb-1">Citizen Narrative:</span>
            <p className="text-slate-200 leading-relaxed">{report.description || "No additional description provided."}</p>
          </div>

          {/* Camera EXIF Metadata */}
          <div className="grid grid-cols-2 gap-3 p-3 bg-gov-950 border border-gov-800 rounded-lg font-mono text-[11px]">
            <div>
              <span className="text-slate-500 block">EXIF Latitude:</span>
              <strong className="text-slate-200">{report.latitude?.toFixed(6) || "28.644500"}° N</strong>
            </div>
            <div>
              <span className="text-slate-500 block">EXIF Longitude:</span>
              <strong className="text-slate-200">{report.longitude?.toFixed(6) || "77.195000"}° E</strong>
            </div>
            <div>
              <span className="text-slate-500 block">Device Model:</span>
              <strong className="text-slate-200">{report.deviceModel || "Citizen Mobile Client (India)"}</strong>
            </div>
            <div>
              <span className="text-slate-500 block">Reported Timestamp:</span>
              <strong className="text-slate-200">{formatIST(report.reportedAt)}</strong>
            </div>
          </div>

          {/* Citizen Reporter Information */}
          <div className="p-3 bg-gov-950 border border-gov-800 rounded-lg space-y-1">
            <span className="font-semibold text-slate-300 block text-xs">Citizen Contact Details:</span>
            <div className="text-slate-200"><strong>Name:</strong> {report.citizenName || "Anonymous Resident"}</div>
            <div className="text-slate-200"><strong>Phone:</strong> <span className="font-mono text-cyan-400">{report.citizenPhone || "N/A"}</span></div>
            {report.citizenEmail && <div className="text-slate-200"><strong>Email:</strong> {report.citizenEmail}</div>}
          </div>

          {/* Current Status Notes */}
          {report.statusNotes && (
            <div className="p-3 bg-blue-950/30 border border-blue-500/30 rounded-lg">
              <span className="font-semibold text-blue-300 block text-xs mb-0.5">Nodal Engineer Field Notes:</span>
              <p className="text-slate-300 leading-relaxed">{report.statusNotes}</p>
            </div>
          )}
        </div>

        {/* Footer Actions */}
        <div className="flex items-center justify-end gap-2 mt-6 pt-4 border-t border-gov-800">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gov-800 hover:bg-gov-700 text-slate-300 rounded-lg text-xs font-semibold transition"
          >
            Close
          </button>
          <button
            onClick={onUpdateStatus}
            className="px-4 py-2 bg-civic-blue hover:bg-blue-600 text-white rounded-lg text-xs font-semibold transition flex items-center gap-1.5 shadow-md shadow-civic-blue/20"
          >
            <i className="fa-solid fa-pen"></i>
            <span>Update Status & Notes</span>
          </button>
        </div>
      </div>
    </div>
  );
}

// =============================================================================
// MODAL: UPDATE GRIEVANCE RESOLUTION STATUS & FIELD NOTES
// =============================================================================
function UpdateStatusModal({ report, onClose, apiFetch, showToast, onSuccess }) {
  const [newStatus, setNewStatus] = useState(report.status);
  const [notes, setNotes] = useState(report.statusNotes || "");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      await apiFetch(`/api/admin/reports/${report.id}/status`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          status: newStatus,
          notes: notes
        })
      });

      showToast(`Status updated to ${newStatus} for ${report.reportCode}. Real-time SSE stream notified.`, "success");
      onSuccess();
    } catch (err) {
      showToast("Status transition failed: " + err.message, "error");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 modal-backdrop animate-fadeIn">
      <div className="bg-gov-900 border border-gov-700 max-w-md w-full rounded-2xl shadow-2xl p-6 relative">
        <div className="flex items-start justify-between gap-4 mb-4 pb-3 border-b border-gov-800">
          <div>
            <h3 className="font-bold text-white text-base">Triage Status Transition</h3>
            <p className="text-xs text-slate-400 font-mono">{report.reportCode} • {report.wardName || "Ward 85"}</p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-white text-lg">
            <i className="fa-solid fa-xmark"></i>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold text-slate-300 mb-1.5">Resolution Lifecycle Status</label>
            <select
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value)}
              className="w-full px-3 py-2.5 bg-gov-950 border border-gov-700 rounded-lg text-white focus:outline-none focus:border-civic-blue text-xs font-semibold"
            >
              <option value="SUBMITTED">SUBMITTED (Intake Queue / Awaiting Triage)</option>
              <option value="CLUSTERED">CLUSTERED (Grouped into Incident Hotspot)</option>
              <option value="ESCALATED">ESCALATED (Forwarded to Jal Board Control Room)</option>
              <option value="IN_PROGRESS">IN_PROGRESS (Repair Crew Dispatched to Site)</option>
              <option value="RESOLVED">RESOLVED (Restored & Pressure Verified)</option>
              <option value="REJECTED">REJECTED (False Alarm / Duplicate Grievance)</option>
            </select>
          </div>

          <div>
            <label className="block font-semibold text-slate-300 mb-1.5">
              Official Engineering Field Notes
            </label>
            <textarea
              rows={4}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="e.g., Crew 4B on site with 450mm ductile sleeve clamp. Welding complete, lines sanitized."
              className="w-full p-3 bg-gov-950 border border-gov-700 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:border-civic-blue text-xs leading-relaxed"
            ></textarea>
            <span className="text-[10px] text-slate-500">Visible to citizen in real-time tracking timeline.</span>
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 bg-gov-800 hover:bg-gov-700 text-slate-300 rounded-lg text-xs font-semibold transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-bold transition flex items-center gap-1.5 shadow-md shadow-emerald-600/20"
            >
              {submitting ? (
                <>
                  <i className="fa-solid fa-circle-notch fa-spin"></i>
                  <span>Saving...</span>
                </>
              ) : (
                <>
                  <i className="fa-solid fa-check"></i>
                  <span>Commit Status Change</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// =============================================================================
// MODAL: REASSIGN MUNICIPAL WARD JURISDICTION
// =============================================================================
function ReassignWardModal({ report, wards, onClose, apiFetch, showToast, onSuccess }) {
  const [targetWardNumber, setTargetWardNumber] = useState(report.wardNumber || wards[0]?.wardNumber || 85);
  const [submitting, setSubmitting] = useState(false);

  const handleReassign = async (e) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      await apiFetch(`/api/admin/reports/${report.id}/reassign?wardNumber=${targetWardNumber}`, {
        method: "POST"
      });

      showToast(`Report ${report.reportCode} successfully reassigned to Ward ${targetWardNumber}.`, "success");
      onSuccess();
    } catch (err) {
      showToast("Reassignment failed: " + err.message, "error");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 modal-backdrop animate-fadeIn">
      <div className="bg-gov-900 border border-gov-700 max-w-md w-full rounded-2xl shadow-2xl p-6 relative">
        <div className="flex items-start justify-between gap-4 mb-4 pb-3 border-b border-gov-800">
          <div>
            <h3 className="font-bold text-white text-base">Reassign Municipal Ward</h3>
            <p className="text-xs text-slate-400 font-mono">{report.reportCode}</p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-white text-lg">
            <i className="fa-solid fa-xmark"></i>
          </button>
        </div>

        <form onSubmit={handleReassign} className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold text-slate-300 mb-1.5">Select Target Ward Jurisdiction</label>
            <select
              value={targetWardNumber}
              onChange={(e) => setTargetWardNumber(Number.parseInt(e.target.value))}
              className="w-full px-3 py-2.5 bg-gov-950 border border-gov-700 rounded-lg text-white focus:outline-none focus:border-civic-blue text-xs font-semibold"
            >
              {wards.map(w => (
                <option key={w.wardNumber} value={w.wardNumber}>
                  Ward #{w.wardNumber} - {w.wardName} ({w.officerName})
                </option>
              ))}
            </select>
          </div>

          <div className="p-3 bg-gov-950 border border-gov-800 rounded-lg space-y-1 text-slate-300">
            <div>Current Ward: <strong className="text-white">{report.wardName || "Ward 85"} (Ward #{report.wardNumber || 85})</strong></div>
            <div className="text-slate-400">Reassigning will update responsible nodal engineer and emergency routing.</div>
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 bg-gov-800 hover:bg-gov-700 text-slate-300 rounded-lg text-xs font-semibold transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-lg text-xs font-bold transition flex items-center gap-1.5 shadow-md shadow-purple-600/20"
            >
              {submitting ? (
                <>
                  <i className="fa-solid fa-circle-notch fa-spin"></i>
                  <span>Transferring...</span>
                </>
              ) : (
                <>
                  <i className="fa-solid fa-arrows-split-up-and-left"></i>
                  <span>Transfer Jurisdiction</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// =============================================================================
// MODAL: INSPECT DISPATCH AUDIT PAYLOAD
// =============================================================================
function InspectAuditModal({ log, onClose }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 modal-backdrop animate-fadeIn">
      <div className="bg-gov-900 border border-gov-700 max-w-lg w-full rounded-2xl shadow-2xl p-6 relative">
        <div className="flex items-start justify-between gap-4 mb-4 pb-3 border-b border-gov-800">
          <div>
            <h3 className="font-bold text-white text-base">Dispatch Audit Dossier</h3>
            <p className="text-xs text-slate-400 font-mono">Cluster: {log.clusterCode || "N/A"}</p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-white text-lg">
            <i className="fa-solid fa-xmark"></i>
          </button>
        </div>

        <div className="space-y-3 text-xs">
          <div className="grid grid-cols-2 gap-2 p-3 bg-gov-950 border border-gov-800 rounded-lg font-mono">
            <div>
              <span className="text-slate-500 block text-[10px]">Channel:</span>
              <strong className="text-cyan-400">{log.dispatchChannel}</strong>
            </div>
            <div>
              <span className="text-slate-500 block text-[10px]">Status:</span>
              <strong className="text-emerald-400">{log.status}</strong>
            </div>
            <div>
              <span className="text-slate-500 block text-[10px]">Ward Number:</span>
              <strong className="text-slate-200">#{log.wardNumber}</strong>
            </div>
            <div>
              <span className="text-slate-500 block text-[10px]">Response Code:</span>
              <strong className="text-slate-200">{log.responseCode || 200}</strong>
            </div>
          </div>

          <div>
            <span className="font-semibold text-slate-300 block mb-1">Recipient Destination:</span>
            <div className="p-2.5 bg-gov-950 border border-gov-800 rounded-lg font-mono text-slate-200 text-[11px] truncate">
              {log.recipient}
            </div>
          </div>

          <div>
            <span className="font-semibold text-slate-300 block mb-1">Payload Content Summary:</span>
            <pre className="p-3 bg-gov-950 border border-gov-800 rounded-lg font-mono text-slate-300 text-[11px] overflow-x-auto whitespace-pre-wrap leading-relaxed max-h-48">
              {log.payloadSummary || "No payload summary recorded."}
            </pre>
          </div>
        </div>

        <div className="flex justify-end mt-4 pt-3 border-t border-gov-800">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gov-800 hover:bg-gov-700 text-slate-300 rounded-lg text-xs font-semibold transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}

// Mount the React Application
const root = ReactDOM.createRoot(document.getElementById("admin-root"));
root.render(<AdminApp />);
