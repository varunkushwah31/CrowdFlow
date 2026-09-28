import React, { useState } from "react";
import { reportsApi, wardsApi } from "../../services/api";
import { useAuth } from "../../context/AuthContext";
import type { IssueType, SeverityLevel, ExifExtractionResult, WaterReport } from "../../types";
import {
  CameraIcon,
  UploadSimpleIcon,
  MapPinIcon,
  CircleNotchIcon,
  PaperPlaneTiltIcon,
  SparkleIcon
} from "@phosphor-icons/react";

interface ReportHazardFormProps {
  onSuccess: (report: WaterReport) => void;
  clickedCoords?: { lat: number; lon: number } | null;
}

export const ReportHazardForm: React.FC<ReportHazardFormProps> = ({ onSuccess, clickedCoords }) => {
  const { showToast } = useAuth();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [issueType, setIssueType] = useState<IssueType>("PIPELINE_BURST");
  const [severity, setSeverity] = useState<SeverityLevel>("CRITICAL");
  const [latitude, setLatitude] = useState(clickedCoords ? clickedCoords.lat.toFixed(6) : "28.644500");
  const [longitude, setLongitude] = useState(clickedCoords ? clickedCoords.lon.toFixed(6) : "77.195000");
  const [wardNumber, setWardNumber] = useState(85);
  const [phone, setPhone] = useState("+91 98110 23412");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [exifData, setExifData] = useState<ExifExtractionResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [exifLoading, setExifLoading] = useState(false);

  // Sync if clicked coordinates change
  React.useEffect(() => {
    if (clickedCoords) {
      setLatitude(clickedCoords.lat.toFixed(6));
      setLongitude(clickedCoords.lon.toFixed(6));
      // Auto-lookup ward
      wardsApi
        .lookup(clickedCoords.lat, clickedCoords.lon)
        .then((w) => setWardNumber(w.wardNumber))
        .catch(() => {});
    }
  }, [clickedCoords]);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setSelectedFile(file);
    setPreviewUrl(URL.createObjectURL(file));

    // Automated EXIF extraction
    setExifLoading(true);
    try {
      const result = await reportsApi.extractExif(file);
      setExifData(result);
      if (result.hasGps && result.latitude && result.longitude) {
        setLatitude(result.latitude.toFixed(6));
        setLongitude(result.longitude.toFixed(6));
        showToast("GPS coordinates extracted from camera EXIF metadata!", "success");
        // Auto-lookup municipal ward
        const ward = await wardsApi.lookup(result.latitude, result.longitude);
        if (ward) setWardNumber(ward.wardNumber);
      } else {
        showToast("No GPS EXIF in image. Location defaults to Karol Bagh / Delhi.", "info");
      }
    } catch {
      showToast("EXIF extraction unavailable. Using manual coordinates.", "info");
    } finally {
      setExifLoading(false);
    }
  };

  const handleGetLocation = () => {
    if (!navigator.geolocation) {
      showToast("Browser geolocation is not supported", "error");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const lat = pos.coords.latitude;
        const lon = pos.coords.longitude;
        setLatitude(lat.toFixed(6));
        setLongitude(lon.toFixed(6));
        showToast("Device coordinates acquired via GPS!", "success");
        try {
          const ward = await wardsApi.lookup(lat, lon);
          if (ward) setWardNumber(ward.wardNumber);
        } catch {
          // ignore
        }
      },
      (err) => {
        showToast(`Geolocation error: ${err.message}`, "error");
      }
    );
  };

  const handleSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!title.trim()) {
      showToast("Please enter an incident title", "warning");
      return;
    }

    setLoading(true);
    try {
      const fd = new FormData();
      fd.append("title", title);
      fd.append("description", description);
      fd.append("issueType", issueType);
      fd.append("severity", severity);
      fd.append("latitude", latitude);
      fd.append("longitude", longitude);
      fd.append("wardNumber", wardNumber.toString());
      fd.append("citizenPhone", phone);
      if (selectedFile) {
        fd.append("file", selectedFile);
      }

      const created = await reportsApi.create(fd);
      showToast(`Incident Report ${created.reportCode} logged successfully!`, "success");
      onSuccess(created);
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Submission failed: ${error.message}`, "error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl max-w-2xl mx-auto text-slate-100">
      <div className="flex items-center gap-3 pb-5 border-b border-slate-800">
        <div className="w-12 h-12 rounded-xl bg-blue-600/20 border border-blue-500/40 flex items-center justify-center text-blue-400">
          <CameraIcon size={26} weight="fill" />
        </div>
        <div>
          <h2 className="text-lg font-bold text-white">Log Civic Water Infrastructure Hazard</h2>
          <p className="text-xs text-slate-400">
            Automated EXIF Geolocation &amp; Spatial Clustering Intake (Delhi Jal Board)
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="mt-6 space-y-5">
        {/* Photo Upload & EXIF Preview */}
        <div>
          <label htmlFor="hazard-photo-input" className="block text-xs font-semibold text-slate-300 mb-2">
            Incident Photo Evidence (Auto-strips PII &amp; Extracts GPS)
          </label>
          <div className="border-2 border-dashed border-slate-700 hover:border-blue-500 rounded-xl p-4 bg-slate-950/50 transition-colors text-center">
            {previewUrl ? (
              <div className="space-y-3">
                <img
                  src={previewUrl}
                  alt="Preview"
                  className="max-h-48 rounded-lg mx-auto object-cover border border-slate-700 shadow"
                />
                {exifLoading && (
                  <div className="text-xs text-blue-400 flex items-center justify-center gap-2">
                    <CircleNotchIcon size={16} className="animate-spin" />
                    <span>Extracting camera hardware metadata...</span>
                  </div>
                )}
                {exifData && (
                  <div className="bg-slate-900 p-2.5 rounded-lg border border-slate-800 text-[11px] text-left grid grid-cols-2 gap-2 font-mono">
                    <div>
                      <span className="text-slate-500 block">Camera:</span>
                      <strong className="text-slate-200">
                        {exifData.cameraMake || "Mobile"} {exifData.cameraModel || "Sensor"}
                      </strong>
                    </div>
                    <div>
                      <span className="text-slate-500 block">EXIF GPS:</span>
                      <strong className="text-emerald-400">
                        {exifData.hasGps
                          ? `${exifData.latitude?.toFixed(4)}, ${exifData.longitude?.toFixed(4)}`
                          : "No embedded GPS"}
                      </strong>
                    </div>
                  </div>
                )}
                <button
                  type="button"
                  onClick={() => {
                    setSelectedFile(null);
                    setPreviewUrl(null);
                    setExifData(null);
                  }}
                  className="text-xs text-rose-400 hover:underline"
                >
                  Remove &amp; select another photo
                </button>
              </div>
            ) : (
              <label htmlFor="hazard-photo-input" className="cursor-pointer block py-4">
                <UploadSimpleIcon size={32} className="mx-auto text-slate-500 mb-2" />
                <span className="text-sm font-semibold text-blue-400 hover:underline block">
                  Click to attach camera photo
                </span>
                <span className="text-xs text-slate-500 block mt-1">
                  Supports JPEG/PNG with EXIF GPS tags (up to 30MB)
                </span>
                <input
                  id="hazard-photo-input"
                  type="file"
                  accept="image/*"
                  onChange={handleFileChange}
                  className="hidden"
                />
              </label>
            )}
          </div>
        </div>

        {/* Title */}
        <div>
          <label htmlFor="hazard-title-input" className="block text-xs font-semibold text-slate-300 mb-1.5">
            Hazard Summary / Title *
          </label>
          <input
            id="hazard-title-input"
            type="text"
            required
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. 450mm Arterial Feeder Rupture flooding roadway"
            className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />
        </div>

        {/* Issue Type & Severity */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="hazard-issue-type-select" className="block text-xs font-semibold text-slate-300 mb-1.5">
              Issue Classification
            </label>
            <select
              id="hazard-issue-type-select"
              value={issueType}
              onChange={(e) => setIssueType(e.target.value as IssueType)}
              className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500"
            >
              <option value="PIPELINE_BURST">Main Pipeline Burst / Rupture</option>
              <option value="WATER_CONTAMINATION">Sewage Ingress / Contamination</option>
              <option value="SEWAGE_OVERFLOW">Sewage Line Overflow</option>
              <option value="LOW_PRESSURE">Chronic Low Pressure</option>
              <option value="ILLEGAL_EXTRACTION">Illegal Commercial Boring / Extraction</option>
              <option value="BOREWELL_DEPLETION">Municipal Borewell Depleted</option>
              <option value="WATER_TANKER_IRREGULARITY">Water Tanker Irregularity</option>
              <option value="OTHER">Other Water Infrastructure Hazard</option>
            </select>
          </div>

          <div>
            <label htmlFor="hazard-severity-select" className="block text-xs font-semibold text-slate-300 mb-1.5">
              Urgency &amp; Severity Level
            </label>
            <select
              id="hazard-severity-select"
              value={severity}
              onChange={(e) => setSeverity(e.target.value as SeverityLevel)}
              className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500"
            >
              <option value="CRITICAL">CRITICAL (Gushing Water / Health Emergency)</option>
              <option value="HIGH">HIGH (Severe Localized Supply Disruption)</option>
              <option value="MEDIUM">MEDIUM (Moderate Pressure / Valve Leakage)</option>
              <option value="LOW">LOW (Minor Seepage / Non-critical)</option>
            </select>
          </div>
        </div>

        {/* Geographic Coordinates & Ward */}
        <div className="p-4 bg-slate-950 rounded-xl border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-300 flex items-center gap-1.5 uppercase tracking-wider text-[10px]">
              <MapPinIcon size={14} className="text-rose-400" />
              <span>Geospatial Verification (WGS84)</span>
            </span>
            <button
              type="button"
              onClick={handleGetLocation}
              className="text-xs text-blue-400 hover:text-blue-300 font-semibold flex items-center gap-1"
            >
              <SparkleIcon size={14} />
              <span>Use My GPS</span>
            </button>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            <div>
              <label htmlFor="hazard-latitude-input" className="text-[11px] text-slate-400 block mb-1">Latitude</label>
              <input
                id="hazard-latitude-input"
                type="text"
                required
                value={latitude}
                onChange={(e) => setLatitude(e.target.value)}
                className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs font-mono text-white"
              />
            </div>
            <div>
              <label htmlFor="hazard-longitude-input" className="text-[11px] text-slate-400 block mb-1">Longitude</label>
              <input
                id="hazard-longitude-input"
                type="text"
                required
                value={longitude}
                onChange={(e) => setLongitude(e.target.value)}
                className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs font-mono text-white"
              />
            </div>
            <div className="col-span-2 sm:col-span-1">
              <label htmlFor="hazard-ward-input" className="text-[11px] text-slate-400 block mb-1">Municipal Ward</label>
              <input
                id="hazard-ward-input"
                type="number"
                value={wardNumber}
                onChange={(e) => setWardNumber(Number.parseInt(e.target.value, 10))}
                className="w-full px-2.5 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-xs font-mono text-white"
              />
            </div>
          </div>
        </div>

        {/* Narrative Description */}
        <div>
          <label htmlFor="hazard-description-textarea" className="block text-xs font-semibold text-slate-300 mb-1.5">
            Citizen Narrative / Specific Landmark Details
          </label>
          <textarea
            id="hazard-description-textarea"
            rows={3}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Specify street landmark, duration of leak, nearby junction, impact on traffic..."
            className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />
        </div>

        {/* Mobile Number for SMS updates */}
        <div>
          <label htmlFor="hazard-phone-input" className="block text-xs font-semibold text-slate-300 mb-1.5">
            Citizen Contact (For Automated SMS / WhatsApp Milestone Updates)
          </label>
          <input
            id="hazard-phone-input"
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-sm text-white font-mono focus:outline-none focus:border-blue-500"
          />
        </div>

        {/* Submit Button */}
        <button
          type="submit"
          disabled={loading}
          className="w-full py-3 px-4 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-800 text-white rounded-xl font-bold text-sm transition-all shadow-lg shadow-blue-600/20 flex items-center justify-center gap-2"
        >
          {loading ? (
            <CircleNotchIcon size={18} className="animate-spin" />
          ) : (
            <PaperPlaneTiltIcon size={18} weight="fill" />
          )}
          <span>Submit Grievance to Delhi Jal Board</span>
        </button>
      </form>
    </div>
  );
};
