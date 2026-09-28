// =============================================================================
// CrowdFlow WaterWatch India - Auth Context
// =============================================================================

import React, { createContext, useContext, useState, useCallback, useMemo } from "react";
import type { AuthState, UserRole, ToastNotification } from "../types";
import { authApi } from "../services/api";

let nextToastId = 1;

export interface AuthorityPreset {
  name: string;
  designation: string;
  jurisdiction: string;
  phone: string;
  role: UserRole;
  badgeColor: string;
}

export const AUTHORITY_PRESETS: AuthorityPreset[] = [
  {
    name: "Er. Alok Sharma",
    designation: "Executive Engineer (EE - Water)",
    jurisdiction: "Ward 85 - Karol Bagh (Delhi Jal Board)",
    phone: "+91 98110 23412",
    role: "ROLE_WARD_OFFICER",
    badgeColor: "emerald"
  },
  {
    name: "Er. Rakesh K. Kaushik",
    designation: "Chief Engineer / Zonal Admin",
    jurisdiction: "Delhi Jal Board HQ (Varunalaya Phase II)",
    phone: "+91 98711 54321",
    role: "ROLE_SUPER_ADMIN",
    badgeColor: "purple"
  },
  {
    name: "Aarav Mehra",
    designation: "Citizen Reporter / Resident",
    jurisdiction: "Civil Lines, New Delhi",
    phone: "+91 98990 11223",
    role: "ROLE_CITIZEN",
    badgeColor: "blue"
  }
];

interface AuthContextType {
  auth: AuthState | null;
  isAdmin: boolean;
  isWardOfficer: boolean;
  isSuperAdmin: boolean;
  login: (authState: AuthState) => void;
  logout: () => void;
  toasts: ToastNotification[];
  showToast: (message: string, type?: "info" | "success" | "warning" | "error") => void;
  removeToast: (id: number) => void;
  selectPreset: (preset: AuthorityPreset) => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [auth, setAuth] = useState<AuthState | null>(() => {
    try {
      const stored = localStorage.getItem("civic_admin_auth");
      if (stored) return JSON.parse(stored);
      const jwt = localStorage.getItem("crowdflow_jwt");
      const role = localStorage.getItem("crowdflow_role") as UserRole;
      const phone = localStorage.getItem("crowdflow_phone");
      if (jwt && role && phone) {
        return { token: jwt, role, phoneNumber: phone };
      }
    } catch {
      // ignore
    }
    return null;
  });

  const [toasts, setToasts] = useState<ToastNotification[]>([]);

  const showToast = useCallback(
    (message: string, type: "info" | "success" | "warning" | "error" = "info") => {
      const id = Date.now() + nextToastId++;
      setToasts((prev) => [...prev, { id, message, type }]);
      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
      }, 5000);
    },
    []
  );

  const removeToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const login = useCallback(
    (authState: AuthState) => {
      setAuth(authState);
      localStorage.setItem("civic_admin_auth", JSON.stringify(authState));
      localStorage.setItem("crowdflow_jwt", authState.token);
      localStorage.setItem("crowdflow_role", authState.role);
      localStorage.setItem("crowdflow_phone", authState.phoneNumber);
      showToast(
        `Authenticated as ${authState.name || authState.role} (${authState.phoneNumber})`,
        "success"
      );
    },
    [showToast]
  );

  const logout = useCallback(() => {
    setAuth(null);
    localStorage.removeItem("civic_admin_auth");
    localStorage.removeItem("crowdflow_jwt");
    localStorage.removeItem("crowdflow_role");
    localStorage.removeItem("crowdflow_phone");
    showToast("Signed out successfully", "info");
  }, [showToast]);

  const selectPreset = useCallback(
    async (preset: AuthorityPreset) => {
      try {
        const otpRes = await authApi.sendOtp(preset.phone);
        const demoOtp = otpRes.demoOtp || "123456";
        const verifyRes = await authApi.verifyOtp(preset.phone, demoOtp, preset.role);
        login({
          token: verifyRes.token,
          phoneNumber: verifyRes.phoneNumber,
          role: verifyRes.role,
          name: preset.name,
          designation: preset.designation
        });
      } catch (err: unknown) {
        const error = err as Error;
        showToast(`Preset login failed: ${error.message}`, "error");
      }
    },
    [login, showToast]
  );

  const isAdmin = auth?.role === "ROLE_WARD_OFFICER" || auth?.role === "ROLE_SUPER_ADMIN";
  const isWardOfficer = auth?.role === "ROLE_WARD_OFFICER";
  const isSuperAdmin = auth?.role === "ROLE_SUPER_ADMIN";

  const contextValue = useMemo(
    () => ({
      auth,
      isAdmin,
      isWardOfficer,
      isSuperAdmin,
      login,
      logout,
      toasts,
      showToast,
      removeToast,
      selectPreset
    }),
    [auth, isAdmin, isWardOfficer, isSuperAdmin, login, logout, toasts, showToast, removeToast, selectPreset]
  );

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
