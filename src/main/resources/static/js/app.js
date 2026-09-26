/**
 * WaterWatch India - Frontend Application Logic
 */

let map;
let markersLayer;
let heatmapLayer;
let clustersLayer;
let tempClickMarker = null;

let allReports = [];
let allClusters = [];
let allWards = [];

// Initialize on page load
document.addEventListener("DOMContentLoaded", () => {
  initMap();
  initTabNavigation();
  initEventListeners();
  initAuthAndTracking();
  loadAllData();
});

// Map Initialization
function initMap() {
  // Center on Central Delhi, India (Karol Bagh / Connaught Place grid)
  const defaultLat = 28.6320;
  const defaultLon = 77.2105;

  map = L.map("map", {
    center: [defaultLat, defaultLon],
    zoom: 13,
    zoomControl: true
  });

  // OpenStreetMap Tile Layer
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: '&copy; <a href="https://openstreetmap.org/copyright">OpenStreetMap</a> contributors | WaterWatch India Civic Platform'
  }).addTo(map);

  markersLayer = L.layerGroup().addTo(map);
  clustersLayer = L.layerGroup().addTo(map);

  // Map Click to Pick Location anywhere in India
  map.on("click", (e) => {
    const activeTab = document.querySelector(".nav-tab-btn.active")?.getAttribute("data-tab");
    if (activeTab === "tab-report") {
      const lat = e.latlng.lat.toFixed(6);
      const lon = e.latlng.lng.toFixed(6);

      document.getElementById("report-latitude").value = lat;
      document.getElementById("report-longitude").value = lon;

      if (tempClickMarker) {
        map.removeLayer(tempClickMarker);
      }
      tempClickMarker = L.marker([lat, lon], {
        icon: L.divIcon({
          className: "temp-pin",
          html: `<div style="background:#ef4444; width:16px; height:16px; border-radius:50%; border:3px solid white; box-shadow:0 0 8px rgba(0,0,0,0.5);"></div>`,
          iconSize: [16, 16],
          iconAnchor: [8, 8]
        })
      }).addTo(map);
    }
  });
}

// Navigation Tabs
function initTabNavigation() {
  const tabs = document.querySelectorAll(".nav-tab-btn[data-tab]");
  tabs.forEach(tab => {
    tab.addEventListener("click", () => {
      tabs.forEach(t => t.classList.remove("active"));
      tab.classList.add("active");

      const targetTabId = tab.getAttribute("data-tab");
      document.querySelectorAll(".tab-content-panel").forEach(panel => {
        panel.style.display = (panel.id === targetTabId) ? "block" : "none";
      });

      if (targetTabId === "tab-wards") loadWards();
      if (targetTabId === "tab-dispatch") loadDispatchLogs();
      if (targetTabId === "tab-operations") renderClustersPanel();

      setTimeout(() => map.invalidateSize(), 150);
    });
  });
}

// Event Listeners
function initEventListeners() {
  // Layer Toggles
  document.getElementById("layer-toggle-pins")?.addEventListener("click", function() {
    this.classList.toggle("active");
    if (this.classList.contains("active")) {
      map.addLayer(markersLayer);
    } else {
      map.removeLayer(markersLayer);
    }
  });

  document.getElementById("layer-toggle-heatmap")?.addEventListener("click", function() {
    this.classList.toggle("active");
    if (heatmapLayer) {
      if (this.classList.contains("active")) {
        map.addLayer(heatmapLayer);
      } else {
        map.removeLayer(heatmapLayer);
      }
    }
  });

  document.getElementById("layer-toggle-clusters")?.addEventListener("click", function() {
    this.classList.toggle("active");
    if (this.classList.contains("active")) {
      map.addLayer(clustersLayer);
    } else {
      map.removeLayer(clustersLayer);
    }
  });

  // Filter Dropdown
  document.getElementById("filter-issue-type")?.addEventListener("change", (e) => {
    renderMarkers(e.target.value);
  });

  // Refresh Button
  document.getElementById("btn-refresh-data")?.addEventListener("click", () => {
    loadAllData();
  });
  document.getElementById("btn-refresh-dispatch")?.addEventListener("click", () => {
    loadDispatchLogs();
  });

  // Camera EXIF Extraction on File Select
  const fileInput = document.getElementById("report-file-input");
  fileInput?.addEventListener("change", async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    try {
      const res = await fetch("/api/reports/extract-exif", {
        method: "POST",
        body: formData
      });
      const data = await res.json();
      const previewCard = document.getElementById("exif-preview");

      if (data.hasGps && data.latitude && data.longitude) {
        document.getElementById("report-latitude").value = data.latitude.toFixed(6);
        document.getElementById("report-longitude").value = data.longitude.toFixed(6);

        document.getElementById("exif-lat-lon").innerHTML =
          `Camera EXIF GPS: <strong>${data.latitude.toFixed(5)}, ${data.longitude.toFixed(5)}</strong> (${data.altitude || 'Surface'})`;
        document.getElementById("exif-device").textContent =
          `Device: ${data.cameraMake || ''} ${data.cameraModel || 'Android / iPhone'}`;
        document.getElementById("exif-time").textContent =
          `Captured: ${data.capturedAt ? new Date(data.capturedAt).toLocaleString() + ' IST' : 'Live'}`;

        previewCard.style.display = "block";
        previewCard.style.background = "#f0fdf4";
        previewCard.style.borderColor = "#bbf7d0";
        previewCard.style.color = "#166534";

        map.flyTo([data.latitude, data.longitude], 16, { animate: true });
      } else {
        previewCard.style.display = "block";
        previewCard.style.background = "#fffbeb";
        previewCard.style.borderColor = "#fef3c7";
        previewCard.style.color = "#92400e";
        document.getElementById("exif-lat-lon").innerHTML =
          `<i class="fa-solid fa-triangle-exclamation"></i> No GPS EXIF tags detected. Click anywhere on the map to set exact coordinates.`;
        document.getElementById("exif-device").textContent = "";
        document.getElementById("exif-time").textContent = "";
      }
    } catch (err) {
      console.warn("EXIF extraction error:", err);
    }
  });

  // Indian Civic Demo Preloaders
  document.getElementById("btn-demo-burst")?.addEventListener("click", () => {
    document.getElementById("report-issue-type").value = "BURST_PIPE";
    document.getElementById("report-latitude").value = "28.6447";
    document.getElementById("report-longitude").value = "77.1953";
    document.getElementById("report-description").value = "Main feeder pipeline burst on Pusa Road near Metro Pillar 118. Clean water shooting 3 meters into the air. Road caving in towards Karol Bagh.";
    document.getElementById("report-citizen-name").value = "Suresh Aggarwal";
    document.getElementById("report-citizen-email").value = "suresh.a@karolbagh.in";
    document.getElementById("report-citizen-phone").value = "+91 98110 99887";
    map.flyTo([28.6447, 77.1953], 16);
  });

  document.getElementById("btn-demo-contamination")?.addEventListener("click", () => {
    document.getElementById("report-issue-type").value = "CONTAMINATION";
    document.getElementById("report-latitude").value = "28.6083";
    document.getElementById("report-longitude").value = "77.2983";
    document.getElementById("report-description").value = "Tap water in Mayur Vihar Pocket 1 is dark brown with foul sewage smell. Trunk sewer manhole overflowing adjacent to potable line.";
    document.getElementById("report-citizen-name").value = "Pooja Nambiar";
    document.getElementById("report-citizen-email").value = "p.nambiar@gmail.com";
    document.getElementById("report-citizen-phone").value = "+91 98712 33445";
    map.flyTo([28.6083, 77.2983], 16);
  });

  // Citizen Report Submission
  document.getElementById("incident-report-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const btn = document.getElementById("btn-submit-report");
    btn.disabled = true;
    btn.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Submitting &amp; Extracting...`;

    const formData = new FormData();
    formData.append("issueType", document.getElementById("report-issue-type").value);
    formData.append("latitude", document.getElementById("report-latitude").value);
    formData.append("longitude", document.getElementById("report-longitude").value);
    formData.append("description", document.getElementById("report-description").value);
    formData.append("citizenName", document.getElementById("report-citizen-name").value);
    formData.append("citizenEmail", document.getElementById("report-citizen-email").value);
    formData.append("citizenPhone", document.getElementById("report-citizen-phone").value);

    const file = document.getElementById("report-file-input").files[0];
    if (file) {
      formData.append("file", file);
    }

    try {
      const res = await fetch("/api/reports", {
        method: "POST",
        body: formData
      });
      if (res.ok) {
        const created = await res.json();
        alert(`Citizen Grievance Registered!\nReference Code: ${created.reportCode}\nAssigned Ward: ${created.wardName}\nAuthority: ${created.municipalBody || 'Delhi Jal Board'}`);
        document.getElementById("incident-report-form").reset();
        document.getElementById("exif-preview").style.display = "none";
        if (tempClickMarker) map.removeLayer(tempClickMarker);

        await loadAllData();

        document.querySelector('.nav-tab-btn[data-tab="tab-map"]').click();
        map.flyTo([created.latitude, created.longitude], 16);
      } else {
        alert("Failed to submit grievance. Please verify parameters.");
      }
    } catch (err) {
      alert("Error contacting server: " + err.message);
    } finally {
      btn.disabled = false;
      btn.innerHTML = `<i class="fa-solid fa-paper-plane"></i> Submit Grievance Report`;
    }
  });

  // Manual DBSCAN Execution
  document.getElementById("btn-run-clustering")?.addEventListener("click", async () => {
    const btn = document.getElementById("btn-run-clustering");
    btn.disabled = true;
    btn.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Executing DBSCAN...`;

    const eps = document.getElementById("clustering-eps").value || 150;
    const minPts = document.getElementById("clustering-minpts").value || 3;

    try {
      const res = await fetch(`/api/clusters/run?epsMeters=${eps}&minPoints=${minPts}&escalationThreshold=5`, {
        method: "POST"
      });
      const summary = await res.json();
      alert(`Spatial DBSCAN Clustering Complete!\n• Clusters Formed: ${summary.clustersFound}\n• Grievances Clustered: ${summary.reportsClustered}\n• Unclustered Noise: ${summary.unclusteredNoise}\n• Auto-Escalations to Jal Board: ${summary.escalationsTriggered}`);
      await loadAllData();
      renderClustersPanel();
    } catch (err) {
      alert("Clustering error: " + err.message);
    } finally {
      btn.disabled = false;
      btn.innerHTML = `<i class="fa-solid fa-brain"></i> Execute Spatial DBSCAN &amp; Correlate`;
    }
  });

  // Modal Close Handlers
  document.getElementById("modal-close-btn")?.addEventListener("click", closeModal);
  document.getElementById("modal-cancel-btn")?.addEventListener("click", closeModal);
  document.getElementById("modal-submit-status-btn")?.addEventListener("click", submitStatusUpdate);
}

// Data Fetching
async function loadAllData() {
  try {
    const [reportsRes, clustersRes, statsRes, heatmapRes] = await Promise.all([
      fetch("/api/reports"),
      fetch("/api/clusters"),
      fetch("/api/stats"),
      fetch("/api/reports/heatmap")
    ]);

    allReports = await reportsRes.json();
    allClusters = await clustersRes.json();
    const stats = await statsRes.json();
    const heatmapPoints = await heatmapRes.json();

    document.getElementById("stat-total-reports").textContent = stats.totalReports || 0;
    document.getElementById("stat-active-clusters").textContent = stats.totalClusters || 0;
    document.getElementById("stat-critical-alerts").textContent = stats.criticalClusters || 0;
    document.getElementById("stat-resolved-reports").textContent = stats.resolvedClusters || 0;

    document.getElementById("reports-count-badge").textContent = `${allReports.length} Grievances`;
    document.getElementById("clusters-count-badge").textContent = `${allClusters.length} Clusters`;

    renderMarkers("ALL");
    renderHeatmap(heatmapPoints);
    renderClusters();
    renderReportsList();
    renderClustersPanel();
  } catch (err) {
    console.error("Failed to load platform data:", err);
  }
}

// Marker Rendering
function renderMarkers(filterType) {
  markersLayer.clearLayers();

  const filtered = (filterType === "ALL")
    ? allReports
    : allReports.filter(r => r.issueType === filterType);

  filtered.forEach(report => {
    if (!report.latitude || !report.longitude) return;

    const pinClass = getPinClassForIssue(report.issueType);
    const iconHtml = `<div class="custom-pin ${pinClass}"><i class="fa-solid ${getIconForIssue(report.issueType)}"></i></div>`;

    const customIcon = L.divIcon({
      className: "custom-leaflet-marker",
      html: iconHtml,
      iconSize: [28, 28],
      iconAnchor: [14, 28],
      popupAnchor: [0, -28]
    });

    const marker = L.marker([report.latitude, report.longitude], { icon: customIcon });

    const statusBadgeClass = report.status === "RESOLVED" ? "badge-status resolved"
      : report.status === "ESCALATED" ? "badge-status escalated" : "badge-status active";

    const popupHtml = `
      <div style="min-width: 220px; font-family:var(--font-sans);">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
          <strong style="color:var(--primary-dark); font-size:13px;">${report.reportCode}</strong>
          <span class="badge ${statusBadgeClass}">${report.status}</span>
        </div>
        <div style="font-weight:700; font-size:12px; margin-bottom:4px;">${report.issueTypeName}</div>
        <div style="font-size:11px; color:#475569; margin-bottom:6px;">
          <i class="fa-solid fa-location-dot"></i> ${report.address || 'Delhi NCT Precinct'}
        </div>
        ${report.imageUrl ? `<img src="${report.imageUrl}" style="width:100%; height:110px; object-fit:cover; border-radius:4px; margin-bottom:6px; border:1px solid #cbd5e1;"/>` : ''}
        <div style="font-size:11px; color:#334155; margin-bottom:6px;">${report.description || 'No additional notes.'}</div>
        <div style="border-top:1px solid #e2e8f0; padding-top:4px; font-size:10.5px; color:#64748b; display:flex; justify-content:space-between;">
          <span>${report.citizenName || 'Verified Citizen'}</span>
          <span>${new Date(report.reportedAt).toLocaleDateString()}</span>
        </div>
      </div>
    `;

    marker.bindPopup(popupHtml);
    markersLayer.addLayer(marker);
  });
}

// Thermal Heatmap Rendering
function renderHeatmap(points) {
  if (heatmapLayer) {
    map.removeLayer(heatmapLayer);
  }

  if (typeof L.heatLayer === "function" && points && points.length > 0) {
    heatmapLayer = L.heatLayer(points, {
      radius: 28,
      blur: 18,
      maxZoom: 17,
      max: 1.0,
      gradient: {
        0.2: '#0284c7', // Blue
        0.4: '#06b6d4', // Cyan
        0.6: '#eab308', // Yellow
        0.8: '#f97316', // Orange
        1.0: '#dc2626'  // Red
      }
    });

    if (document.getElementById("layer-toggle-heatmap")?.classList.contains("active")) {
      heatmapLayer.addTo(map);
    }
  }
}

// Render DBSCAN Clusters & JTS Convex Hull Polygons
function renderClusters() {
  clustersLayer.clearLayers();

  allClusters.forEach(cluster => {
    const isCritical = cluster.severity === "CRITICAL";
    const color = isCritical ? "#dc2626" : cluster.severity === "HIGH" ? "#ea580c" : "#0284c7";

    if (cluster.boundaryGeoJson) {
      try {
        const geojson = JSON.parse(cluster.boundaryGeoJson);
        const polyLayer = L.geoJSON(geojson, {
          style: {
            color: color,
            weight: 2,
            opacity: 0.85,
            dashArray: "6, 6",
            fillColor: color,
            fillOpacity: 0.18
          }
        });

        polyLayer.bindPopup(createClusterPopupHtml(cluster));
        clustersLayer.addLayer(polyLayer);
      } catch (e) {
        console.warn("Could not parse cluster GeoJSON boundary:", e);
      }
    }

    const centroidIcon = L.divIcon({
      className: "cluster-centroid-marker",
      html: `
        <div style="background:${color}; color:white; font-size:10px; font-weight:800; padding:3px 7px; border-radius:12px; border:2px solid white; box-shadow:0 2px 6px rgba(0,0,0,0.3); text-align:center; white-space:nowrap;">
          <i class="fa-solid fa-circle-exclamation"></i> ${cluster.clusterCode} (${cluster.reportCount})
        </div>
      `,
      iconSize: [95, 24],
      iconAnchor: [47, 12]
    });

    const centroidMarker = L.marker([cluster.centroidLat, cluster.centroidLon], { icon: centroidIcon });
    centroidMarker.bindPopup(createClusterPopupHtml(cluster));
    clustersLayer.addLayer(centroidMarker);
  });
}

function createClusterPopupHtml(cluster) {
  const isCritical = cluster.severity === "CRITICAL";
  const sevClass = isCritical ? "badge-critical" : cluster.severity === "HIGH" ? "badge-high" : "badge-medium";

  return `
    <div style="min-width: 270px; font-family:var(--font-sans);">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
        <strong style="color:var(--secondary); font-size:14px;">${cluster.clusterCode}</strong>
        <span class="badge ${sevClass}">${cluster.severity}</span>
      </div>
      <div style="font-weight:700; color:#b91c1c; font-size:12px; margin-bottom:4px;">
        <i class="fa-solid fa-triangle-exclamation"></i> ${cluster.rootCauseHypothesis || 'Jal Board Incident Cluster'}
      </div>
      <div style="font-size:11px; color:#334155; margin-bottom:8px;">
        ${cluster.technicalAnalysis || 'Geospatial correlation confirms infrastructure breakdown.'}
      </div>
      <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:4px; padding:6px; font-size:11px; margin-bottom:8px;">
        <div><strong>Ward:</strong> ${cluster.wardName || 'Ward ' + cluster.wardNumber}</div>
        <div><strong>Body:</strong> ${cluster.municipalBody || 'Delhi Jal Board'}</div>
        <div><strong>Correlated Grievances:</strong> ${cluster.reportCount} reports (${cluster.radiusMeters}m radius)</div>
        <div><strong>AI Confidence:</strong> ${(cluster.confidenceScore || 90).toFixed(1)}%</div>
      </div>
      <div style="display:flex; gap:6px; flex-direction:column;">
        <a href="/api/clusters/${cluster.id}/pdf" target="_blank" class="btn btn-primary btn-sm" style="text-decoration:none; color:white; text-align:center;">
          <i class="fa-solid fa-file-pdf"></i> Download Official Govt PDF Dossier
        </a>
        <button class="btn btn-secondary btn-sm" onclick="openStatusModal(${cluster.id}, '${cluster.clusterCode}', '${cluster.status}')">
          <i class="fa-solid fa-pen-to-square"></i> Update Status &amp; Notify Citizens
        </button>
      </div>
    </div>
  `;
}

// Sidebar Report List
function renderReportsList() {
  const container = document.getElementById("reports-list-container");
  if (!container) return;

  if (allReports.length === 0) {
    container.innerHTML = `<div style="text-align:center; padding:20px; color:var(--text-muted); font-size:12px;">No citizen grievances registered yet.</div>`;
    return;
  }

  container.innerHTML = allReports.slice(0, 20).map(r => `
    <div class="incident-item" onclick="zoomToReport(${r.latitude}, ${r.longitude})">
      <div class="incident-header">
        <span class="incident-code">${r.reportCode}</span>
        <span class="badge ${r.status === 'RESOLVED' ? 'badge-status resolved' : r.status === 'ESCALATED' ? 'badge-status escalated' : 'badge-status active'}">${r.status}</span>
      </div>
      <div style="font-weight:600; font-size:12px; color:var(--primary-dark);">${r.issueTypeName}</div>
      <div style="font-size:11px; color:var(--text-muted); margin-top:2px;">
        <i class="fa-solid fa-location-dot"></i> ${r.address || 'Delhi Precinct'}
      </div>
    </div>
  `).join("");
}

// Sidebar Clusters Panel
function renderClustersPanel() {
  const container = document.getElementById("clusters-list-container");
  if (!container) return;

  if (allClusters.length === 0) {
    container.innerHTML = `<div style="text-align:center; padding:20px; color:var(--text-muted); font-size:12px;">No spatial clusters active. Click "Execute Spatial DBSCAN" above to group nearby reports!</div>`;
    return;
  }

  container.innerHTML = allClusters.map(c => `
    <div class="cluster-card">
      <div class="cluster-card-header" onclick="zoomToReport(${c.centroidLat}, ${c.centroidLon})">
        <div>
          <strong style="color:var(--secondary); font-size:13px;">${c.clusterCode}</strong>
          <div style="font-size:11px; color:var(--text-muted);">${c.wardName || 'Ward ' + c.wardNumber} (${c.municipalBody || 'Jal Board'})</div>
        </div>
        <span class="badge ${c.severity === 'CRITICAL' ? 'badge-critical' : c.severity === 'HIGH' ? 'badge-high' : 'badge-medium'}">${c.severity}</span>
      </div>
      <div class="cluster-card-body">
        <div style="font-weight:700; color:#991b1b; margin-bottom:4px; font-size:12px;">
          <i class="fa-solid fa-brain"></i> ${c.rootCauseHypothesis}
        </div>
        <p style="color:#334155; font-size:11.5px; margin-bottom:8px;">${c.technicalAnalysis}</p>
        
        <div style="background:#f1f5f9; padding:8px; border-radius:4px; margin-bottom:8px; font-size:11px;">
          <strong>Action Directive:</strong> ${c.recommendedAction}
        </div>

        <div style="display:flex; justify-content:space-between; font-size:11px; color:var(--text-muted); margin-bottom:10px;">
          <span>${c.reportCount} Verified Reports</span>
          <span>AI Confidence: ${(c.confidenceScore || 90).toFixed(1)}%</span>
          <span>Status: <strong>${c.status}</strong></span>
        </div>

        <div class="cluster-actions">
          <a href="/api/clusters/${c.id}/pdf" target="_blank" class="btn btn-primary btn-sm" style="text-decoration:none; color:white;">
            <i class="fa-solid fa-file-pdf"></i> Download PDF
          </a>
          <button class="btn btn-secondary btn-sm" onclick="escalateCluster(${c.id})">
            <i class="fa-solid fa-paper-plane"></i> Escalate to Jal Board
          </button>
          <button class="btn btn-secondary btn-sm" onclick="openStatusModal(${c.id}, '${c.clusterCode}', '${c.status}')">
            <i class="fa-solid fa-pen-to-square"></i> Status &amp; Feedback
          </button>
        </div>
      </div>
    </div>
  `).join("");
}

// Wards Tab (Indian Municipal Wards)
async function loadWards() {
  const container = document.getElementById("wards-list-container");
  if (!container) return;

  try {
    const res = await fetch("/api/wards");
    allWards = await res.json();

    container.innerHTML = allWards.map(w => `
      <div class="cluster-card" style="margin-bottom:10px;">
        <div class="cluster-card-header" style="background:#f8fafc;">
          <div>
            <strong>${w.wardName}</strong>
            <div style="font-size:11px; color:var(--text-muted);">${w.municipalBody} | ${w.zoneName} (PIN: ${w.pincode || '110001'})</div>
          </div>
          <span class="badge badge-low">Ward #${w.wardNumber}</span>
        </div>
        <div class="cluster-card-body" style="font-size:11.5px;">
          <div><i class="fa-solid fa-user-tie"></i> Officer: <strong>${w.officerName}</strong> (${w.officerDesignation || 'EE - Water'})</div>
          <div><i class="fa-solid fa-envelope"></i> Email: ${w.contactEmail}</div>
          <div><i class="fa-solid fa-phone"></i> Emergency Helpline: <strong>${w.emergencyHotline}</strong></div>
          <div style="margin-top:6px; font-size:10.5px; color:var(--text-muted);">
            <i class="fa-solid fa-satellite-dish"></i> Webhook: <code>${w.webhookUrl}</code>
          </div>
        </div>
      </div>
    `).join("");
  } catch (e) {
    container.innerHTML = `<div style="color:var(--danger); font-size:12px;">Failed to load municipal wards.</div>`;
  }
}

// Dispatch Logs Tab
async function loadDispatchLogs() {
  const container = document.getElementById("dispatch-logs-container");
  if (!container) return;

  try {
    const res = await fetch("/api/dispatch/logs");
    const logs = await res.json();

    if (logs.length === 0) {
      container.innerHTML = `<div style="text-align:center; padding:20px; color:var(--text-muted); font-size:12px;">No automated dispatches logged yet.</div>`;
      return;
    }

    container.innerHTML = logs.map(l => `
      <div style="background:white; border:1px solid var(--border); border-radius:6px; padding:10px; margin-bottom:8px; font-size:11.5px;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
          <strong><i class="fa-solid fa-broadcast-tower"></i> ${l.dispatchChannel} Alert</strong>
          <span class="badge ${l.status === 'DELIVERED' ? 'badge-status resolved' : 'badge-status active'}">${l.status}</span>
        </div>
        <div>Recipient: <strong>${l.recipient}</strong> (Cluster ${l.clusterCode || 'General'})</div>
        <div style="color:#475569; margin-top:2px;">${l.payloadSummary}</div>
        <div style="font-size:10px; color:var(--text-muted); margin-top:4px;">
          Dispatched: ${new Date(l.dispatchedAt).toLocaleString()} IST
        </div>
      </div>
    `).join("");
  } catch (e) {
    container.innerHTML = `<div style="color:var(--danger); font-size:12px;">Failed to load dispatch logs.</div>`;
  }
}

// Status Update Modal
window.openStatusModal = function(clusterId, clusterCode, currentStatus) {
  document.getElementById("modal-cluster-id").value = clusterId;
  document.getElementById("status-modal-title").textContent = `Update Status: ${clusterCode}`;
  document.getElementById("modal-new-status").value = currentStatus === "ACTIVE" ? "IN_PROGRESS" : currentStatus;
  document.getElementById("modal-status-notes").value = "";
  document.getElementById("status-modal").classList.add("active");
};

function closeModal() {
  document.getElementById("status-modal").classList.remove("active");
}

async function submitStatusUpdate() {
  const clusterId = document.getElementById("modal-cluster-id").value;
  const status = document.getElementById("modal-new-status").value;
  const notes = document.getElementById("modal-status-notes").value;

  try {
    const res = await fetch(`/api/clusters/${clusterId}/status`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status, notes })
    });

    if (res.ok) {
      alert(`Cluster status updated to ${status}!\nCitizens who submitted grievances in this cluster have been automatically notified via SMS/WhatsApp feedback loop.`);
      closeModal();
      await loadAllData();
      renderClustersPanel();
    } else {
      alert("Failed to update status.");
    }
  } catch (err) {
    alert("Error updating status: " + err.message);
  }
}

// Manual Escalate
window.escalateCluster = async function(clusterId) {
  try {
    const res = await fetch(`/api/clusters/${clusterId}/escalate`, { method: "POST" });
    const data = await res.json();
    alert(`Cluster Escalated!\n${data.message}`);
    await loadAllData();
    renderClustersPanel();
  } catch (err) {
    alert("Escalation error: " + err.message);
  }
};

window.zoomToReport = function(lat, lon) {
  if (lat && lon) {
    map.flyTo([lat, lon], 16, { animate: true });
  }
};

// Helper Helpers
function getPinClassForIssue(type) {
  switch (type) {
    case "BURST_PIPE": return "pin-burst";
    case "LEAKAGE": return "pin-leak";
    case "CONTAMINATION": return "pin-contamination";
    case "OPEN_SEWAGE": return "pin-sewage";
    case "WATER_SCARCITY":
    case "BOREWELL_DEPLETION": return "pin-scarcity";
    case "SEVERE_WATERLOGGING":
    case "DRAINAGE_OVERFLOW": return "pin-waterlogging";
    default: return "pin-leak";
  }
}

function getIconForIssue(type) {
  switch (type) {
    case "BURST_PIPE": return "fa-burst";
    case "LEAKAGE": return "fa-faucet-drip";
    case "CONTAMINATION": return "fa-biohazard";
    case "OPEN_SEWAGE": return "fa-triangle-exclamation";
    case "WATER_SCARCITY": return "fa-faucet";
    case "BOREWELL_DEPLETION": return "fa-water";
    case "SEVERE_WATERLOGGING": return "fa-cloud-showers-heavy";
    case "DRAINAGE_OVERFLOW": return "fa-arrow-down-up-across-line";
    default: return "fa-droplet";
  }
}

// ==========================================================
// Indian Mobile OTP Authentication & Grievance Tracking
// ==========================================================
function initAuthAndTracking() {
  const authModal = document.getElementById("auth-modal");
  const btnHeaderAuth = document.getElementById("btn-header-auth");
  const authCloseBtn = document.getElementById("auth-modal-close-btn");
  const authCancelBtn = document.getElementById("auth-cancel-btn");
  const btnSendOtp = document.getElementById("btn-send-otp");
  const btnVerifyOtp = document.getElementById("btn-verify-otp");
  const btnTrackCode = document.getElementById("btn-track-code");
  const trackCodeInput = document.getElementById("track-code-input");

  // Restore saved session
  const savedToken = localStorage.getItem("crowdflow_jwt");
  const savedRole = localStorage.getItem("crowdflow_role");
  const savedPhone = localStorage.getItem("crowdflow_phone");
  if (savedToken && savedPhone) {
    document.getElementById("auth-status-text").textContent = `${savedRole === "ROLE_WARD_OFFICER" ? "Officer" : "Citizen"}: ${savedPhone}`;
  }

  // Open / Close Auth Modal
  if (btnHeaderAuth) {
    btnHeaderAuth.addEventListener("click", () => {
      authModal.style.display = "flex";
    });
  }
  if (authCloseBtn) authCloseBtn.addEventListener("click", () => authModal.style.display = "none");
  if (authCancelBtn) authCancelBtn.addEventListener("click", () => authModal.style.display = "none");

  // Send OTP
  if (btnSendOtp) {
    btnSendOtp.addEventListener("click", async () => {
      const phone = document.getElementById("auth-phone").value.trim();
      if (!phone) return alert("Please enter mobile number (+91)");

      btnSendOtp.disabled = true;
      btnSendOtp.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Sending...`;

      try {
        const res = await fetch("/api/auth/send-otp", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ phoneNumber: phone })
        });
        const data = await res.json();
        document.getElementById("auth-otp-sent-hint").style.display = "block";
        document.getElementById("auth-demo-otp-val").textContent = data.demoOtp || "123456";
        document.getElementById("auth-otp").value = data.demoOtp || "123456";
      } catch (e) {
        alert("Failed to send OTP: " + e.message);
      } finally {
        btnSendOtp.disabled = false;
        btnSendOtp.innerHTML = `<i class="fa-solid fa-sms"></i> Send OTP`;
      }
    });
  }

  // Verify OTP & Sign In
  if (btnVerifyOtp) {
    btnVerifyOtp.addEventListener("click", async () => {
      const phone = document.getElementById("auth-phone").value.trim();
      const otp = document.getElementById("auth-otp").value.trim();
      const role = document.getElementById("auth-role").value;

      if (!phone || !otp) return alert("Please enter phone and OTP");

      try {
        const res = await fetch("/api/auth/verify-otp", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ phoneNumber: phone, otp: otp, role: role })
        });

        if (!res.ok) {
          throw new Error("Invalid verification code");
        }

        const data = await res.json();
        localStorage.setItem("crowdflow_jwt", data.token);
        localStorage.setItem("crowdflow_role", data.role);
        localStorage.setItem("crowdflow_phone", data.phoneNumber);

        document.getElementById("auth-status-text").textContent = `${data.role === "ROLE_WARD_OFFICER" ? "Officer" : "Citizen"}: ${data.phoneNumber}`;
        authModal.style.display = "none";
        alert(`Signed in successfully as ${data.role} (${data.phoneNumber})`);
      } catch (e) {
        alert("Authentication failed: " + e.message);
      }
    });
  }

  // Track Grievance
  if (btnTrackCode) {
    btnTrackCode.addEventListener("click", () => trackGrievance());
  }
  if (trackCodeInput) {
    trackCodeInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter") trackGrievance();
    });
  }
}

async function trackGrievance() {
  const code = document.getElementById("track-code-input").value.trim();
  const container = document.getElementById("track-result-container");
  if (!code) return alert("Please enter a Tracking Code (e.g. IND-H2O-1686)");

  container.innerHTML = `<div style="text-align:center; padding:20px; color:var(--text-muted);"><i class="fa-solid fa-spinner fa-spin"></i> Querying Jal Board Grievance Registry...</div>`;

  try {
    const res = await fetch(`/api/reports/track/${encodeURIComponent(code)}`);
    if (!res.ok) {
      container.innerHTML = `
        <div style="background:#fef2f2; border:1px solid #f87171; border-radius:6px; padding:12px; color:#991b1b; font-size:12px;">
          <i class="fa-solid fa-circle-exclamation"></i> <strong>Report not found.</strong> Verify your tracking code (e.g., IND-H2O-1686).
        </div>`;
      return;
    }

    const report = await res.json();
    const isResolved = report.status === "RESOLVED" || report.status === "CLOSED";
    const statusColor = isResolved ? "#16a34a" : (report.status === "ESCALATED" ? "#dc2626" : "#0284c7");

    let html = `
      <div style="background:white; border:1px solid var(--border-color); border-radius:8px; padding:14px; box-shadow:0 2px 4px rgba(0,0,0,0.05); margin-top:8px;">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
          <span style="font-weight:700; color:var(--primary); font-size:14px;">${report.reportCode}</span>
          <span class="badge" style="background:${statusColor}; color:white;">${report.statusLabel || report.status}</span>
        </div>

        <div style="font-size:12px; margin-bottom:8px;">
          <strong>Issue:</strong> ${report.issueLabel || report.issueType}
        </div>
        <div style="font-size:12px; color:var(--text-muted); margin-bottom:8px;">
          <i class="fa-solid fa-location-dot"></i> ${report.address || "Delhi NCT, India"}
        </div>
        <div style="font-size:12px; margin-bottom:8px;">
          <strong>Jurisdiction:</strong> ${report.municipalBody || "Delhi Jal Board (DJB)"} (Ward ${report.wardNumber || "Central"})
        </div>
        <div style="font-size:12px; margin-bottom:10px; color:#334155;">
          <strong>Description:</strong> ${report.description || "Field report submitted by citizen."}
        </div>

        ${report.clusterId ? `
          <div style="background:#eff6ff; border-left:3px solid #3b82f6; padding:8px; border-radius:4px; font-size:11px; margin-bottom:10px;">
            <i class="fa-solid fa-circle-nodes" style="color:#2563eb;"></i> <strong>Correlated into Cluster #${report.clusterId}</strong><br/>
            Your complaint has been aggregated with nearby citizen reports for expedited municipal work order dispatch.
          </div>
        ` : ''}

        ${report.imageUrl ? `
          <div style="margin-bottom:10px;">
            <img src="${report.imageUrl}" alt="Evidence" style="width:100%; max-height:160px; object-fit:cover; border-radius:6px; border:1px solid #e2e8f0;"/>
          </div>
        ` : ''}

        <div style="display:flex; justify-content:space-between; align-items:center; margin-top:10px;">
          <span style="font-size:11px; color:var(--text-muted);">Filed: ${report.reportedAt ? new Date(report.reportedAt).toLocaleString('en-IN') : 'Recent'}</span>
          <button class="btn btn-secondary btn-sm" onclick="zoomToReport(${report.latitude}, ${report.longitude})">
            <i class="fa-solid fa-crosshairs"></i> View on Map
          </button>
        </div>
      </div>
    `;

    container.innerHTML = html;
  } catch (err) {
    container.innerHTML = `<div style="color:#dc2626; font-size:12px;">Error tracking grievance: ${err.message}</div>`;
  }
}
