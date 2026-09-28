import React, { useEffect, useRef, useState } from "react";
import L from "leaflet";
import type { WaterReport, IncidentCluster, MunicipalWard, WaterTankerUnit, EmergencyDepot } from "../../types";
import { StackIcon } from "@phosphor-icons/react";

interface IncidentMapProps {
  reports: WaterReport[];
  clusters: IncidentCluster[];
  wards: MunicipalWard[];
  tankers?: WaterTankerUnit[];
  depots?: EmergencyDepot[];
  onMapClick?: (lat: number, lon: number) => void;
  selectedReport?: WaterReport | null;
}

function getReportMarkerColor(isResolved: boolean, isCritical: boolean): string {
  if (isResolved) return "#10b981";
  if (isCritical) return "#ef4444";
  return "#0284c7";
}

function getReportBadgeBgColor(isResolved: boolean, isCritical: boolean): string {
  if (isResolved) return "rgba(16, 185, 129, 0.15)";
  if (isCritical) return "rgba(239, 68, 68, 0.15)";
  return "rgba(2, 132, 199, 0.15)";
}

export const IncidentMap: React.FC<IncidentMapProps> = ({
  reports,
  clusters,
  wards,
  tankers = [],
  depots = [],
  onMapClick,
  selectedReport
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const markersLayerRef = useRef<L.LayerGroup | null>(null);
  const clustersLayerRef = useRef<L.LayerGroup | null>(null);
  const wardsLayerRef = useRef<L.LayerGroup | null>(null);
  const fleetLayerRef = useRef<L.LayerGroup | null>(null);
  const clickMarkerRef = useRef<L.Marker | null>(null);

  const [activeLayers, setActiveLayers] = useState({
    reports: true,
    clusters: true,
    wards: true,
    fleet: true
  });

  const [basemap, setBasemap] = useState<"osm" | "dark">("dark");

  // Initialize Map
  useEffect(() => {
    if (!mapContainerRef.current || mapInstanceRef.current) return;

    // National Capital Region (NCR - Delhi) center
    const map = L.map(mapContainerRef.current, {
      center: [28.6448, 77.2167],
      zoom: 12,
      zoomControl: false
    });

    L.control.zoom({ position: "topright" }).addTo(map);

    // Default Dark Basemap
    const darkTile = L.tileLayer("https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png", {
      attribution: '&copy; <a href="https://carto.com/">CARTO</a> | Jal Jeevan Mission',
      maxZoom: 19
    }).addTo(map);

    const osmTile = L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: '&copy; OpenStreetMap contributors'
    });

    (map as unknown as { _tileLayers?: Record<string, L.TileLayer> })._tileLayers = {
      dark: darkTile,
      osm: osmTile
    };

    markersLayerRef.current = L.layerGroup().addTo(map);
    clustersLayerRef.current = L.layerGroup().addTo(map);
    wardsLayerRef.current = L.layerGroup().addTo(map);
    fleetLayerRef.current = L.layerGroup().addTo(map);

    map.on("click", (e: L.LeafletMouseEvent) => {
      const { lat, lng } = e.latlng;
      if (clickMarkerRef.current) {
        map.removeLayer(clickMarkerRef.current);
      }
      clickMarkerRef.current = L.marker([lat, lng], {
        icon: L.divIcon({
          className: "custom-click-pin",
          html: `<div style="background:#ef4444; width:16px; height:16px; border-radius:50%; border:2px solid white; box-shadow:0 0 10px #ef4444;"></div>`,
          iconSize: [16, 16],
          iconAnchor: [8, 8]
        })
      }).addTo(map);

      if (onMapClick) {
        onMapClick(lat, lng);
      }
    });

    mapInstanceRef.current = map;

    return () => {
      map.remove();
      mapInstanceRef.current = null;
    };
  }, []);

  // Switch basemap
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map) return;
    const tileLayers = (map as unknown as { _tileLayers?: Record<string, L.TileLayer> })._tileLayers;
    if (!tileLayers) return;

    if (basemap === "dark") {
      map.removeLayer(tileLayers.osm);
      tileLayers.dark.addTo(map);
    } else {
      map.removeLayer(tileLayers.dark);
      tileLayers.osm.addTo(map);
    }
  }, [basemap]);

  // Render Reports Markers
  useEffect(() => {
    const layer = markersLayerRef.current;
    if (!layer) return;
    layer.clearLayers();

    if (!activeLayers.reports) return;

    reports.forEach((r) => {
      if (!r.latitude || !r.longitude) return;

      const isCritical = r.severity === "CRITICAL";
      const isResolved = r.status === "RESOLVED";

      const color = getReportMarkerColor(isResolved, isCritical);
      const bgBadgeColor = getReportBadgeBgColor(isResolved, isCritical);

      const marker = L.circleMarker([r.latitude, r.longitude], {
        radius: isCritical ? 9 : 7,
        fillColor: color,
        color: "#ffffff",
        weight: 2,
        opacity: 0.9,
        fillOpacity: 0.85
      });

      marker.bindPopup(`
        <div style="font-family:sans-serif; min-width:200px; color:#1e293b;">
          <div style="font-weight:bold; font-size:13px; margin-bottom:4px;">${r.title || "Water Hazard"}</div>
          <div style="font-size:11px; margin-bottom:4px;">
            <span style="background:${bgBadgeColor}; color:${color}; padding:2px 6px; border-radius:4px; font-weight:bold; font-size:10px;">
              ${r.status}
            </span>
            <span style="margin-left:6px; color:#64748b; font-family:monospace;">${r.reportCode}</span>
          </div>
          <div style="font-size:11px; color:#475569; margin-bottom:6px;">${r.description || "No description provided."}</div>
          <div style="font-size:10px; color:#94a3b8;">Ward #${r.wardNumber} • ${new Date(r.createdAt).toLocaleDateString()}</div>
        </div>
      `);

      layer.addLayer(marker);
    });
  }, [reports, activeLayers.reports]);

  // Render Clusters
  useEffect(() => {
    const layer = clustersLayerRef.current;
    if (!layer) return;
    layer.clearLayers();

    if (!activeLayers.clusters) return;

    clusters.forEach((c) => {
      if (!c.centerLatitude || !c.centerLongitude) return;

      const radius = c.radiusMeters || 150;
      const isCritical = c.severity === "CRITICAL";
      const color = isCritical ? "#ef4444" : "#f59e0b";

      const circle = L.circle([c.centerLatitude, c.centerLongitude], {
        radius,
        fillColor: color,
        fillOpacity: 0.25,
        color,
        weight: 2,
        dashArray: "4, 6"
      });

      circle.bindPopup(`
        <div style="font-family:sans-serif; min-width:220px; color:#1e293b;">
          <div style="font-weight:bold; font-size:13px; color:#b45309;">
            Cluster ${c.clusterCode} (${c.incidentCount} Grievances)
          </div>
          <div style="font-size:11px; margin:4px 0;"><strong>Root Cause:</strong> ${c.correlatedRootCause || "Underground Pipe Rupture"}</div>
          <div style="font-size:11px; color:#475569;">Est. Affected: <strong>${c.estimatedAffectedPopulation || "15,000"} residents</strong></div>
          <div style="font-size:10px; color:#94a3b8; margin-top:4px;">Radius: ${radius}m • Status: ${c.status}</div>
        </div>
      `);

      layer.addLayer(circle);
    });
  }, [clusters, activeLayers.clusters]);

  // Render Fleet (Tankers & Depots)
  useEffect(() => {
    const layer = fleetLayerRef.current;
    if (!layer) return;
    layer.clearLayers();

    if (!activeLayers.fleet) return;

    // Depots
    depots.forEach((d) => {
      if (!d.latitude || !d.longitude) return;
      const marker = L.circleMarker([d.latitude, d.longitude], {
        radius: 8,
        fillColor: "#8b5cf6",
        color: "#ffffff",
        weight: 2,
        fillOpacity: 0.9
      });
      marker.bindPopup(`
        <div style="font-family:sans-serif; color:#1e293b;">
          <strong>${d.depotName}</strong>
          <div style="font-size:11px; color:#64748b;">Emergency Jal Board Depot</div>
          <div style="font-size:11px; margin-top:4px;">Available Tankers: <strong>${d.availableTankers}/${d.totalTankers}</strong></div>
        </div>
      `);
      layer.addLayer(marker);
    });

    // Tankers
    tankers.forEach((t) => {
      if (!t.currentLatitude || !t.currentLongitude) return;
      const marker = L.circleMarker([t.currentLatitude, t.currentLongitude], {
        radius: 6,
        fillColor: "#06b6d4",
        color: "#ffffff",
        weight: 1.5,
        fillOpacity: 0.9
      });
      marker.bindPopup(`
        <div style="font-family:sans-serif; color:#1e293b;">
          <strong>Tanker: ${t.vehicleNumber}</strong>
          <div style="font-size:11px; color:#64748b;">Capacity: ${t.capacityLiters} L • Status: ${t.status}</div>
          <div style="font-size:11px; color:#475569;">Driver: ${t.driverName || "Assigned"}</div>
        </div>
      `);
      layer.addLayer(marker);
    });
  }, [depots, tankers, activeLayers.fleet]);

  // Render Municipal Wards
  useEffect(() => {
    const layer = wardsLayerRef.current;
    if (!layer) return;
    layer.clearLayers();

    if (!activeLayers.wards) return;

    wards.forEach((w) => {
      if (!w.centerLatitude || !w.centerLongitude) return;

      const marker = L.circleMarker([w.centerLatitude, w.centerLongitude], {
        radius: 6,
        fillColor: "#6366f1",
        color: "#ffffff",
        weight: 1.5,
        fillOpacity: 0.8
      });

      marker.bindPopup(`
        <div style="font-family:sans-serif; min-width:180px; color:#1e293b;">
          <div style="font-weight:bold; font-size:12px; color:#3730a3;">
            Ward #${w.wardNumber} — ${w.wardName}
          </div>
          <div style="font-size:11px; color:#64748b; margin-top:2px;">Zone: ${w.zoneName || "NCR"}</div>
          <div style="font-size:11px; color:#475569; margin-top:2px;">Officer: ${w.officerName || "Ward Engineer"}</div>
          <div style="font-size:10px; color:#94a3b8; margin-top:4px;">Emergency Hotline: ${w.emergencyHotline || "1916"}</div>
        </div>
      `);

      layer.addLayer(marker);
    });
  }, [wards, activeLayers.wards]);

  // Center on selected report
  useEffect(() => {
    if (!selectedReport || !mapInstanceRef.current) return;
    mapInstanceRef.current.flyTo([selectedReport.latitude, selectedReport.longitude], 16, {
      duration: 1.5
    });
  }, [selectedReport]);

  return (
    <div className="relative w-full h-[620px] rounded-2xl overflow-hidden border border-slate-800 shadow-2xl bg-slate-950">
      <div ref={mapContainerRef} className="w-full h-full z-0" />

      {/* Map Control Overlay */}
      <div className="absolute top-4 left-4 z-10 flex flex-col gap-2">
        <div className="bg-slate-900/90 backdrop-blur-md border border-slate-800 rounded-xl p-2.5 shadow-xl flex flex-col gap-2 text-xs">
          <span className="font-bold text-slate-300 uppercase tracking-wider text-[10px] flex items-center gap-1.5">
            <StackIcon size={14} className="text-blue-400" />
            <span>Map Layers</span>
          </span>

          <label htmlFor="layer-reports-checkbox" className="flex items-center gap-2 cursor-pointer text-slate-300 hover:text-white">
            <input
              id="layer-reports-checkbox"
              type="checkbox"
              checked={activeLayers.reports}
              onChange={(e) => setActiveLayers((p) => ({ ...p, reports: e.target.checked }))}
              className="rounded bg-slate-950 border-slate-700 text-blue-600 focus:ring-0"
            />
            <span className="flex items-center gap-1">
              <span className="w-2.5 h-2.5 rounded-full bg-blue-500 inline-block" />
              <span>Grievance Incidents ({reports.length})</span>
            </span>
          </label>

          <label htmlFor="layer-clusters-checkbox" className="flex items-center gap-2 cursor-pointer text-slate-300 hover:text-white">
            <input
              id="layer-clusters-checkbox"
              type="checkbox"
              checked={activeLayers.clusters}
              onChange={(e) => setActiveLayers((p) => ({ ...p, clusters: e.target.checked }))}
              className="rounded bg-slate-950 border-slate-700 text-blue-600 focus:ring-0"
            />
            <span className="flex items-center gap-1">
              <span className="w-2.5 h-2.5 rounded-full bg-amber-500 inline-block" />
              <span>DBSCAN Clusters ({clusters.length})</span>
            </span>
          </label>

          <label htmlFor="layer-wards-checkbox" className="flex items-center gap-2 cursor-pointer text-slate-300 hover:text-white">
            <input
              id="layer-wards-checkbox"
              type="checkbox"
              checked={activeLayers.wards}
              onChange={(e) => setActiveLayers((p) => ({ ...p, wards: e.target.checked }))}
              className="rounded bg-slate-950 border-slate-700 text-blue-600 focus:ring-0"
            />
            <span className="flex items-center gap-1">
              <span className="w-2.5 h-2.5 rounded-full bg-indigo-500 inline-block" />
              <span>Municipal Wards ({wards.length})</span>
            </span>
          </label>

          <label htmlFor="layer-fleet-checkbox" className="flex items-center gap-2 cursor-pointer text-slate-300 hover:text-white">
            <input
              id="layer-fleet-checkbox"
              type="checkbox"
              checked={activeLayers.fleet}
              onChange={(e) => setActiveLayers((p) => ({ ...p, fleet: e.target.checked }))}
              className="rounded bg-slate-950 border-slate-700 text-blue-600 focus:ring-0"
            />
            <span className="flex items-center gap-1">
              <span className="w-2.5 h-2.5 rounded-full bg-purple-500 inline-block" />
              <span>Depots &amp; Tanker Fleet</span>
            </span>
          </label>
        </div>

        {/* Basemap Toggle */}
        <div className="bg-slate-900/90 backdrop-blur-md border border-slate-800 rounded-xl p-1.5 shadow-xl flex items-center gap-1 text-xs">
          <button
            onClick={() => setBasemap("dark")}
            className={`px-2.5 py-1 rounded-lg font-medium transition-all ${
              basemap === "dark" ? "bg-slate-800 text-white shadow-sm" : "text-slate-400 hover:text-white"
            }`}
          >
            Dark Carto
          </button>
          <button
            onClick={() => setBasemap("osm")}
            className={`px-2.5 py-1 rounded-lg font-medium transition-all ${
              basemap === "osm" ? "bg-slate-800 text-white shadow-sm" : "text-slate-400 hover:text-white"
            }`}
          >
            OpenStreetMap
          </button>
        </div>
      </div>

      {/* Map Legend */}
      <div className="absolute bottom-4 right-4 z-10 bg-slate-900/90 backdrop-blur-md border border-slate-800 rounded-xl p-3 shadow-xl text-xs space-y-1.5 pointer-events-auto">
        <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Incident Legend</div>
        <div className="flex items-center gap-2 text-slate-300">
          <span className="w-3 h-3 rounded-full bg-rose-500 border border-white" />
          <span>Critical Rupture / Contamination</span>
        </div>
        <div className="flex items-center gap-2 text-slate-300">
          <span className="w-3 h-3 rounded-full bg-blue-500 border border-white" />
          <span>Submitted / In Progress</span>
        </div>
        <div className="flex items-center gap-2 text-slate-300">
          <span className="w-3 h-3 rounded-full bg-emerald-500 border border-white" />
          <span>Resolved / Clean Normal</span>
        </div>
      </div>
    </div>
  );
};
