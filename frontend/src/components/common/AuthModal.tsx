import React, { useState } from "react";
import { useAuth, AUTHORITY_PRESETS } from "../../context/AuthContext";
import { authApi } from "../../services/api";
import type { UserRole } from "../../types";
import {
  ShieldCheckIcon,
  UserIcon,
  KeyIcon,
  PhoneIcon,
  ArrowRightIcon,
  XIcon,
  CircleNotchIcon,
  SparkleIcon
} from "@phosphor-icons/react";

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  defaultRole?: UserRole;
}

function getRoleBadgeStyle(role: UserRole): string {
  if (role === "ROLE_SUPER_ADMIN") {
    return "bg-purple-950 text-purple-300 border border-purple-800/60";
  }
  if (role === "ROLE_WARD_OFFICER") {
    return "bg-emerald-950 text-emerald-300 border border-emerald-800/60";
  }
  return "bg-blue-950 text-blue-300 border-blue-800/60";
}

export const AuthModal: React.FC<AuthModalProps> = ({ isOpen, onClose, defaultRole }) => {
  const { login, showToast, selectPreset } = useAuth();
  const [phone, setPhone] = useState("+91 98110 23412");
  const [role, setRole] = useState<UserRole>(defaultRole || "ROLE_WARD_OFFICER");
  const [step, setStep] = useState<"PHONE" | "OTP">("PHONE");
  const [otp, setOtp] = useState("123456");
  const [demoOtpHint, setDemoOtpHint] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleSendOtp = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await authApi.sendOtp(phone);
      const dispatchedOtp = res.demoOtp || "123456";
      setDemoOtpHint(dispatchedOtp);
      setOtp(dispatchedOtp);
      setStep("OTP");
      showToast(`SMS OTP dispatched to ${phone}. (Demo OTP: ${dispatchedOtp})`, "info");
    } catch (err: unknown) {
      const error = err as Error;
      showToast(error.message || "Failed to dispatch OTP", "error");
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e: React.SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await authApi.verifyOtp(phone, otp, role);
      login({
        token: res.token,
        phoneNumber: res.phoneNumber,
        role: res.role,
        name: res.name
      });
      onClose();
    } catch (err: unknown) {
      const error = err as Error;
      showToast(error.message || "Invalid OTP code", "error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-700/80 rounded-2xl w-full max-w-md overflow-hidden shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-800 bg-slate-800/40">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/40 flex items-center justify-center text-blue-400">
              <ShieldCheckIcon size={22} weight="bold" />
            </div>
            <div>
              <h3 className="font-bold text-white text-base">Civic ID &amp; Authority Access</h3>
              <p className="text-xs text-slate-400">Delhi Jal Board &amp; Citizen OTP Gateway</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white p-1 rounded-lg transition-colors"
          >
            <XIcon size={20} />
          </button>
        </div>

        {/* Quick Demo Presets */}
        <div className="p-5 border-b border-slate-800/80 bg-slate-950/40">
          <div className="flex items-center gap-1.5 mb-2.5 text-xs font-semibold text-slate-400 uppercase tracking-wider">
            <SparkleIcon size={14} className="text-amber-400" weight="fill" />
            <span>Instant Demo Authority Login</span>
          </div>
          <div className="grid grid-cols-1 gap-2">
            {AUTHORITY_PRESETS.map((p) => (
              <button
                key={p.phone}
                onClick={async () => {
                  setLoading(true);
                  await selectPreset(p);
                  setLoading(false);
                  onClose();
                }}
                disabled={loading}
                className="flex items-center justify-between p-2.5 rounded-xl border border-slate-800 bg-slate-900/60 hover:border-blue-500/50 hover:bg-slate-850 text-left transition-all group"
              >
                <div>
                  <div className="text-sm font-semibold text-slate-200 group-hover:text-blue-400 flex items-center gap-2">
                    {p.name}
                    <span className={`text-[10px] px-2 py-0.5 rounded font-mono font-medium ${getRoleBadgeStyle(p.role)}`}>
                      {p.role.replace("ROLE_", "")}
                    </span>
                  </div>
                  <div className="text-xs text-slate-400 mt-0.5">{p.designation}</div>
                </div>
                <ArrowRightIcon size={16} className="text-slate-500 group-hover:text-blue-400 group-hover:translate-x-0.5 transition-transform" />
              </button>
            ))}
          </div>
        </div>

        {/* Manual OTP Form */}
        <div className="p-5">
          {step === "PHONE" ? (
            <form onSubmit={handleSendOtp} className="space-y-4">
              <div>
                <label htmlFor="auth-phone-input" className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Mobile Number (India)
                </label>
                <div className="relative">
                  <PhoneIcon size={18} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
                  <input
                    id="auth-phone-input"
                    type="tel"
                    required
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="+91 98110 23412"
                    className="w-full pl-10 pr-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors font-mono"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="auth-role-select" className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Requested Access Scope
                </label>
                <div className="relative">
                  <UserIcon size={18} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
                  <select
                    id="auth-role-select"
                    value={role}
                    onChange={(e) => setRole(e.target.value as UserRole)}
                    className="w-full pl-10 pr-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-sm text-white focus:outline-none focus:border-blue-500 transition-colors"
                  >
                    <option value="ROLE_WARD_OFFICER">Executive Engineer (Ward Officer)</option>
                    <option value="ROLE_SUPER_ADMIN">Delhi Jal Board HQ / Super Admin</option>
                    <option value="ROLE_CITIZEN">Citizen / Resident</option>
                  </select>
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-800 text-white rounded-xl font-semibold text-sm transition-all shadow-lg shadow-blue-600/20 flex items-center justify-center gap-2"
              >
                {loading ? <CircleNotchIcon size={18} className="animate-spin" /> : <ArrowRightIcon size={18} />}
                <span>Dispatch Verification OTP</span>
              </button>
            </form>
          ) : (
            <form onSubmit={handleVerifyOtp} className="space-y-4">
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label htmlFor="auth-otp-input" className="text-xs font-semibold text-slate-300">
                    6-Digit One-Time Password
                  </label>
                  <button
                    type="button"
                    onClick={() => setStep("PHONE")}
                    className="text-xs text-blue-400 hover:underline"
                  >
                    Change Phone
                  </button>
                </div>
                <div className="relative">
                  <KeyIcon size={18} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
                  <input
                    id="auth-otp-input"
                    type="text"
                    required
                    maxLength={6}
                    value={otp}
                    onChange={(e) => setOtp(e.target.value)}
                    placeholder="123456"
                    className="w-full pl-10 pr-4 py-2.5 bg-slate-950 border border-slate-700 rounded-xl text-base tracking-widest text-center text-white placeholder-slate-600 focus:outline-none focus:border-blue-500 font-mono font-bold"
                  />
                </div>
                {demoOtpHint && (
                  <p className="text-xs text-emerald-400 mt-1.5">
                    Demo OTP auto-filled: <strong>{demoOtpHint}</strong>
                  </p>
                )}
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 px-4 bg-emerald-600 hover:bg-emerald-500 disabled:bg-emerald-800 text-white rounded-xl font-semibold text-sm transition-all shadow-lg shadow-emerald-600/20 flex items-center justify-center gap-2"
              >
                {loading ? <CircleNotchIcon size={18} className="animate-spin" /> : <ShieldCheckIcon size={18} />}
                <span>Verify &amp; Enter Portal</span>
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
