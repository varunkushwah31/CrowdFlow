import React, { useState, useEffect, useRef } from "react";
import { reportsApi } from "../../services/api";
import { resolveApiUrl } from "../../config";
import { useAuth } from "../../context/AuthContext";
import type { LiveTrackingData } from "../../types";
import {
  MagnifyingGlassIcon,
  BroadcastIcon,
  CircleNotchIcon,
  StarIcon,
  PaperPlaneTiltIcon,
  CheckIcon
} from "@phosphor-icons/react";

interface TrackGrievanceViewProps {
  initialCode?: string;
}

function getStageBadgeClass(isComplete: boolean, isCurrent: boolean): string {
  if (isComplete) return "bg-emerald-600 text-white";
  if (isCurrent) return "bg-blue-600 text-white ring-4 ring-blue-600/20";
  return "bg-slate-800 text-slate-500";
}

function getStageTextClass(isCurrent: boolean, isComplete: boolean): string {
  if (isCurrent) return "text-blue-300";
  if (isComplete) return "text-slate-200";
  return "text-slate-500";
}

export const TrackGrievanceView: React.FC<TrackGrievanceViewProps> = ({ initialCode = "" }) => {
  const { showToast } = useAuth();

  const [code, setCode] = useState(initialCode);
  const [loading, setLoading] = useState(false);
  const [trackingData, setTrackingData] = useState<LiveTrackingData | null>(null);
  const [sseConnected, setSseConnected] = useState(false);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [submittingFeedback, setSubmittingFeedback] = useState(false);

  const eventSourceRef = useRef<EventSource | null>(null);

  const fetchSnapshotAndStream = async (targetCode: string) => {
    if (!targetCode.trim()) return;

    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }

    setLoading(true);
    try {
      const data = await reportsApi.track(targetCode.trim());
      setTrackingData(data);

      // Connect SSE
      const sseUrl = resolveApiUrl(`/api/reports/track/${encodeURIComponent(targetCode.trim())}/live-stream`);
      const es = new EventSource(sseUrl);

      es.onopen = () => {
        setSseConnected(true);
      };

      es.addEventListener("grievance-status", (event: MessageEvent) => {
        try {
          const liveData = JSON.parse(event.data);
          setTrackingData(liveData);
          showToast(`Live status update received: ${liveData.currentStage}`, "info");
        } catch (e) {
          console.error("Failed to parse live SSE event", e);
        }
      });

      es.onerror = () => {
        setSseConnected(false);
      };

      eventSourceRef.current = es;
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Tracking error: ${error.message}`, "error");
      setTrackingData(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (initialCode) {
      setCode(initialCode);
      fetchSnapshotAndStream(initialCode);
    }
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, [initialCode]);

  const handleSearch = (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    fetchSnapshotAndStream(code);
  };

  const handleSubmitFeedback = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!trackingData) return;

    setSubmittingFeedback(true);
    try {
      const updated = await reportsApi.submitFeedback(trackingData.reportCode, rating, comment);
      setTrackingData(updated);
      showToast("Thank you! Your citizen satisfaction rating has been recorded.", "success");
      setComment("");
    } catch (err: unknown) {
      const error = err as Error;
      showToast(`Failed to submit feedback: ${error.message}`, "error");
    } finally {
      setSubmittingFeedback(false);
    }
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl max-w-3xl mx-auto text-slate-100">
      <div className="flex items-center gap-3 pb-5 border-b border-slate-800">
        <div className="w-12 h-12 rounded-xl bg-blue-600/20 border border-blue-500/40 flex items-center justify-center text-blue-400">
          <MagnifyingGlassIcon size={26} weight="bold" />
        </div>
        <div>
          <h2 className="text-lg font-bold text-white">Track Civic Water Grievance</h2>
          <p className="text-xs text-slate-400">
            Real-time Server-Sent Events (SSE) direct telemetry with Delhi Jal Board dispatchers
          </p>
        </div>
      </div>

      {/* Code Input */}
      <form onSubmit={handleSearch} className="mt-6 flex gap-2">
        <div className="relative flex-1">
          <MagnifyingGlassIcon size={18} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            id="grievance-track-code-input"
            aria-label="Grievance Tracking Code"
            type="text"
            required
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder="Enter Grievance Tracking Code (e.g. IND-H2O-1686)"
            className="w-full pl-10 pr-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 font-mono focus:outline-none focus:border-blue-500 uppercase"
          />
        </div>
        <button
          type="submit"
          disabled={loading}
          className="py-2.5 px-5 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-800 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-blue-600/20 flex items-center gap-2"
        >
          {loading ? <CircleNotchIcon size={16} className="animate-spin" /> : <BroadcastIcon size={16} />}
          <span>Track Live</span>
        </button>
      </form>

      {/* Tracking Card */}
      {trackingData && (
        <div className="mt-6 space-y-6 animate-in fade-in duration-200">
          {/* Status Header */}
          <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 flex flex-wrap items-center justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <span className="font-mono font-bold text-base text-white">
                  {trackingData.reportCode}
                </span>
                <span className="px-2.5 py-0.5 rounded text-[11px] font-bold uppercase bg-blue-950 text-blue-300 border border-blue-800/60">
                  {trackingData.currentStage}
                </span>
              </div>
              <h3 className="text-sm font-semibold text-slate-200 mt-1">{trackingData.title}</h3>
              <div className="text-xs text-slate-400 mt-0.5">
                Ward #{trackingData.wardNumber} • Logged on {new Date(trackingData.createdAt).toLocaleDateString()}
              </div>
            </div>

            <div className="flex items-center gap-2 text-xs">
              <span className={`w-2 h-2 rounded-full ${sseConnected ? "bg-emerald-500 animate-pulse" : "bg-slate-600"}`} />
              <span className="text-slate-400 font-mono text-[11px]">
                {sseConnected ? "Live Field Stream Active" : "Snapshot Cached"}
              </span>
            </div>
          </div>

          {/* Stepper Progression */}
          <div className="bg-slate-950 p-5 rounded-xl border border-slate-800">
            <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-4">
              Delhi Jal Board Resolution Pipeline
            </h4>
            <div className="space-y-4">
              {(trackingData.stages || []).map((stage, idx) => {
                const isComplete = stage.completed;
                const isCurrent = trackingData.currentStage === stage.stage;
                const badgeClass = getStageBadgeClass(isComplete, isCurrent);
                const textClass = getStageTextClass(isCurrent, isComplete);

                return (
                  <div key={stage.stage || idx} className="flex items-start gap-3 relative">
                    <div
                      className={`w-7 h-7 rounded-full flex items-center justify-center shrink-0 text-xs font-bold ${badgeClass}`}
                    >
                      {isComplete ? <CheckIcon size={14} weight="bold" /> : idx + 1}
                    </div>

                    <div className="flex-1">
                      <div className="flex items-center justify-between">
                        <span className={`font-semibold text-xs ${textClass}`}>
                          {stage.stage}
                        </span>
                        {stage.timestamp && (
                          <span className="text-[10px] text-slate-500 font-mono">
                            {new Date(stage.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-slate-400 mt-0.5">{stage.description}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Resolution Feedback */}
          {trackingData.resolutionNotes && (
            <div className="p-4 bg-emerald-950/30 border border-emerald-800/50 rounded-xl text-xs">
              <span className="font-bold text-emerald-300 block mb-1">
                Official Municipal Resolution Notes:
              </span>
              <p className="text-emerald-100">{trackingData.resolutionNotes}</p>
            </div>
          )}

          {/* Citizen Star Rating & Feedback */}
          <div className="bg-slate-950 p-5 rounded-xl border border-slate-800">
            <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">
              Citizen Feedback &amp; Verification
            </h4>
            <p className="text-xs text-slate-400 mb-3">
              Did the municipal field team resolve the water hazard satisfactorily?
            </p>

            <form onSubmit={handleSubmitFeedback} className="space-y-3">
              <div className="flex items-center gap-1.5">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    className="p-1 text-amber-400 hover:scale-110 transition-transform"
                  >
                    <StarIcon size={24} weight={star <= rating ? "fill" : "regular"} />
                  </button>
                ))}
                <span className="text-xs text-slate-400 ml-2 font-mono">{rating} / 5 Stars</span>
              </div>

              <div>
                <textarea
                  id="citizen-feedback-textarea"
                  aria-label="Additional citizen feedback for Delhi Jal Board"
                  rows={2}
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="Additional citizen feedback for Delhi Jal Board leadership..."
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
                />
              </div>

              <button
                type="submit"
                disabled={submittingFeedback}
                className="py-2 px-4 bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-800 text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5"
              >
                {submittingFeedback ? (
                  <CircleNotchIcon size={14} className="animate-spin" />
                ) : (
                  <PaperPlaneTiltIcon size={14} weight="fill" />
                )}
                <span>Submit Citizen Feedback</span>
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
