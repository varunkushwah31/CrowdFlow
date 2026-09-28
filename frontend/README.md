# CrowdFlow React TypeScript Frontend & Admin Command Center

This directory contains the completely decoupled, **100% React + TypeScript** frontend application for the **WaterWatch India Civic Monitoring & Spatial Monolith**.

All UI dashboard elements, Citizen Grievance Reporting, and Government Admin Control Room are unified into a high-performance Single Page Application (SPA) powered by **React 19**, **TypeScript**, **Vite**, **@phosphor-icons/react**, **Leaflet GIS**, and **Chart.js**.

---

## Architecture & Directory Layout

```
frontend/
├── src/
│   ├── main.tsx                    # React application bootstrap
│   ├── App.tsx                     # Main layout & view routing (Citizen vs Admin)
│   ├── index.css                   # Custom styles, animations, and Leaflet CSS
│   ├── config.ts                   # Dynamic API base URL resolution & configuration
│   ├── vite-env.d.ts               # Vite client types & asset definitions
│   │
│   ├── types/
│   │   └── index.ts                # Strict TypeScript interfaces for all domain models
│   │
│   ├── services/
│   │   └── api.ts                  # Type-safe API client (Reports, Clusters, Fleet, Auth, Wards)
│   │
│   ├── context/
│   │   └── AuthContext.tsx         # User authentication, role enforcement, and Toast notifications
│   │
│   └── components/
│       ├── common/
│       │   ├── Navbar.tsx          # Navigation header, view switcher, auth profile (Phosphor Icons)
│       │   ├── AuthModal.tsx       # SMS OTP login modal & Quick Demo Authority Presets
│       │   └── Toast.tsx           # Floating feedback toast notifications
│       │
│       ├── citizen/                # Citizen Civic Portal
│       │   ├── CitizenPortal.tsx   # Master citizen container & KPI metric overview
│       │   ├── IncidentMap.tsx     # Interactive Leaflet GIS map (markers, clusters, fleet)
│       │   ├── ReportHazardForm.tsx# Photo upload with Camera EXIF GPS extraction
│       │   ├── MunicipalOpsView.tsx# Spatial DBSCAN clustering status & AMRUT dossiers
│       │   ├── WardsView.tsx       # Municipal wards directory & EE contact helplines
│       │   ├── DispatchLogsView.tsx# Automated SMS, Email, and Webhook dispatch audit logs
│       │   └── TrackGrievanceView.tsx# Real-time SSE live stream & 5-Star Citizen Rating feedback
│       │
│       └── admin/                  # Government Admin Command Room (Delhi Jal Board)
│           ├── AdminPortal.tsx     # Master admin container with strict role protection
│           ├── AdminOverview.tsx   # Executive command KPIs & Chart.js status distribution
│           ├── AdminTriage.tsx     # Grievance triage table, photo inspection & status transition
│           ├── AdminClusters.tsx   # DBSCAN clustering runner & AMRUT 2.0 PDF downloads
│           ├── AdminFleet.tsx      # Emergency water tanker fleet dispatch & GIS routing
│           ├── AdminWards.tsx      # Polygon geocoding & EE jurisdiction lookup
│           ├── AdminAudit.tsx      # Tamper-evident administrative audit trail
│           └── AdminDiagnostics.tsx# Redis cache eviction & GEORADIUS spatial queries
│
├── index.html                      # Single clean HTML entry point
├── package.json                    # Scripts for dev, build, preview, typecheck
├── tsconfig.json                   # Strict TypeScript compiler options
├── vite.config.ts                  # Vite config with React plugin, API proxy, and code-splitting
├── Dockerfile                      # Multi-stage production container build (Node 22 + Nginx Alpine)
└── nginx.conf                      # Standalone Nginx server config with SPA route fallback
```

---

## Quick Start (Local Development)

### 1. Install Dependencies
```bash
cd frontend
npm install
```

### 2. Run Vite Dev Server
```bash
npm run dev
```
* **Citizen Portal**: `http://localhost:3000/`
* **Gov Command Room**: `http://localhost:3000/admin` (or toggle "Gov Command Center" in the top bar)
* Requests to `/api/*`, `/actuator/*`, and `/swagger-ui/*` are automatically proxied to the Spring Boot backend on `http://localhost:8085`.

### 3. Type Checking & Production Build
```bash
npm run typecheck    # Runs tsc --noEmit
npm run build        # Typechecks with tsc and compiles minified bundle with Vite into dist/
npm run preview      # Previews the production build locally
```

---

## Standalone Production Deployment

### Option A: Docker Multi-Stage Build
```bash
docker build -t crowdflow-frontend .
docker run -d -p 80:80 -e BACKEND_HOST=your-backend-host --name crowdflow-frontend crowdflow-frontend
```

### Option B: Cloud Hosting (Vercel, Netlify, Cloudflare Pages, AWS S3)
Deploy the `frontend/dist` directory as a static SPA. Set `window.__CROWDFLOW_API_BASE__ = "https://api.yourdomain.com"` or configure it in `config.ts` to connect to your backend API.
