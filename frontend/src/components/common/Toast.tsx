import React from "react";
import { useAuth } from "../../context/AuthContext";
import { CheckCircleIcon, WarningCircleIcon, InfoIcon, XIcon, WarningIcon } from "@phosphor-icons/react";

function getToastBgClass(type: string): string {
  if (type === "error") return "bg-rose-950/90 border-rose-600 text-rose-100";
  if (type === "success") return "bg-emerald-950/90 border-emerald-600 text-emerald-100";
  if (type === "warning") return "bg-amber-950/90 border-amber-600 text-amber-100";
  return "bg-slate-900/90 border-blue-500 text-slate-100";
}

function renderToastIcon(type: string) {
  if (type === "error") return <WarningCircleIcon size={20} weight="fill" className="text-rose-400" />;
  if (type === "success") return <CheckCircleIcon size={20} weight="fill" className="text-emerald-400" />;
  if (type === "warning") return <WarningIcon size={20} weight="fill" className="text-amber-400" />;
  return <InfoIcon size={20} weight="fill" className="text-blue-400" />;
}

export const ToastContainer: React.FC = () => {
  const { toasts, removeToast } = useAuth();

  if (toasts.length === 0) return null;

  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-md w-full pointer-events-none">
      {toasts.map((toast) => {
        const bgClass = getToastBgClass(toast.type);

        return (
          <div
            key={toast.id}
            className={`pointer-events-auto flex items-start gap-3 p-4 rounded-xl border shadow-xl backdrop-blur-md transition-all animate-in fade-in slide-in-from-top-2 duration-200 ${bgClass}`}
          >
            <div className="mt-0.5 shrink-0">
              {renderToastIcon(toast.type)}
            </div>
            <div className="flex-1 text-sm font-medium leading-relaxed">{toast.message}</div>
            <button
              onClick={() => removeToast(toast.id)}
              className="text-slate-400 hover:text-white transition-colors p-1"
            >
              <XIcon size={16} />
            </button>
          </div>
        );
      })}
    </div>
  );
};
