# 🌊 CrowdFlow - Production-Grade Community Water Monitoring Platform

> **Enterprise Civic-Tech Municipal Water Infrastructure Crowdsourcing, Spatial Clustering & Automated Escalation Platform**  
> *Anchored by Java 25 (Project Loom / Virtual Threads), Spring Boot 3.4 Monolith, PostGIS 16, Redis 7, MinIO S3, Nginx Reverse Proxy, and Flutter Mobile Client.*  
> *Built exclusively for Indian Municipal Governance: Delhi Jal Board (DJB), Municipal Corporation of Delhi (MCD), NDMC, Jal Jeevan Mission, and AMRUT 2.0.*

---

## 🏛️ System Architecture Blueprint

```
                       ┌──────────────────────────────────────────────────┐
                       │          MOBILE CLIENT (Flutter / Dart)          │
                       │   Camera + EXIF • Offline Queue • MapLibre GL    │
                       └────────────────────────┬─────────────────────────┘
                                                │ HTTPS / REST (Multipart)
                                                ▼
                       ┌──────────────────────────────────────────────────┐
                       │          REVERSE PROXY & GATEWAY (Nginx)         │
                       └────────────────────────┬─────────────────────────┘
                                                │
                                                ▼
                       ┌──────────────────────────────────────────────────┐
                       │           BACKEND API (Spring Boot 3.4)          │
                       │         Java 25 (Virtual Threads / Loom)         │
                       │  Spring Security • JTS Suite • Metadata-Extract  │
                       └──────┬──────────────────────┬────────────────────┘
                              │                      │
            ┌─────────────────┴──────┐        ┌──────┴────────────────────┐
            ▼                        ▼        ▼                           ▼
┌────────────────────────┐  ┌──────────────┐  ┌──────────────┐  ┌───────────────────┐
│       POSTGRESQL       │  │ S3 / MINIO   │  │   TASK QUEUE │  │    REDIS 7.x      │
│     with PostGIS       │  │ (Image Store)│  │ (Async Loom) │  │  (Auth, Tokens,   │
│ (Spatial Clustering &  │  └──────────────┘  └──────┬───────┘  │  Geo-Tile Cache)  │
│  Geom Indexed Tables)  │                           │          └───────────────────┘
└────────────────────────┘                           ▼
                                      ┌──────────────────────────────┐
                                      │  ASYNC REPORTING & DISPATCH  │
                                      │   ST_ClusterDBSCAN / DBSCAN  │
                                      │   Thymeleaf + OpenHTMLtoPDF  │
                                      │   Webhooks (ICCC) + Fast2SMS │
                                      └──────────────────────────────┘
```

---

## 🏗️ 7-Layer Production Specification

### Layer 1: Mobile Client (Cross-Platform)
- **Framework**: Flutter 3.x (Dart 3.x) with 60 FPS vector map rendering.
- **Vector Maps**: `maplibre_gl` client-side GPU-accelerated rendering for cluster bubbles and vector tiles.
- **Geolocation**: `geolocator` capturing high-accuracy GPS coordinates at reporting sites.
- **Live Camera**: `camera` + `image_picker` enforcing live camera capture over stale gallery uploads.
- **Hardware EXIF**: `native_exif` reading raw GPS coordinates, altitude, and timestamp directly from device sensors.
- **Local Offline Cache**: `drift` (SQLite) offline queue auto-syncing when connectivity is restored.
- **Resilient Uploads**: `dio` chunked multipart HTTP uploads with background retries.
- *Scaffolded directory*: [`mobile_client/`](file:///d:/CrowdFlow/mobile_client/)

### Layer 2: API Gateway & Application Server (Java 25 Core)
- **Language & Runtime**: **Java 25 (LTS)** utilizing **Virtual Threads (Project Loom)** for handling concurrent multipart file uploads without OS thread exhaustion (`spring.threads.virtual.enabled: true`).
- **Core Framework**: Spring Boot 3.4.13 (`spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-data-jpa`).
- **Spatial Integration**: `org.hibernate.orm:hibernate-spatial` for native PostGIS mapping.
- **Geometry Engine**: `org.locationtech.jts:jts-core:1.19.0` (Points, Polygons, Bounding Envelopes, Convex Hulls).
- **EXIF Extraction**: `com.drewnoakes:metadata-extractor:2.19.0` extracting camera model, GPS coordinates, and exposure timestamps.
- **Image Sanitization & Optimization**: `net.coobird:thumbnailator:0.4.20` auto-orienting images, stripping personal identifiable metadata (PII), and generating 300x300 thumbnails.
- **Security & RBAC**: Spring Security 6.x + JJWT (`0.12.6`) supporting OTP mobile login (`+91`) and Role-Based Access Control (`ROLE_CITIZEN`, `ROLE_WARD_OFFICER`, `ROLE_SUPER_ADMIN`).

### Layer 3: Database, Geospatial Engine & Storage
- **Relational DB**: PostgreSQL 16+ with PostGIS 3.4+ extension (`postgis/postgis:16-3.4`) for production, and file-based H2 for embedded development.
- **Spatial Indexing & Functions**: GIST indexing, `ST_ClusterDBSCAN`, `ST_ConvexHull`, and `ST_Contains` for ward boundaries.
- **In-Memory Cache**: Redis 7.x (`redis:7.2-alpine`) for token storage, geo-tile caching, and rate-limiting counters.
- **Object Storage**: AWS S3 / MinIO (`minio/minio`) for secure image hosting and generated PDF dossiers.

### Layer 4: Asynchronous Processing & Clustering Execution
- **Task Execution**: Spring Task Execution backed by Java 25 Virtual Threads for decoupled intake.
- **Scheduling**: `@Scheduled` cluster evaluation running every 2 minutes across Indian municipal wards.
- **Clustering Algorithms**: Geodesic Haversine DBSCAN algorithm ($\EPSILON = 150\text{m}$, $\text{MinPts} = 3$) with root-cause correlation engine (Feeder Burst, Sewage Ingress, Pumping Station Trip, Storm Sump Choke).

### Layer 5: GIS, Reverse Geocoding & Mapping Services
- **Base Tiles & Heatmap**: OpenStreetMap and Leaflet.js with `Leaflet.heat` for dynamic thermal hotspot rendering.
- **Reverse Geocoding**: Resolves raw GPS coordinates into Indian localities, municipal wards (e.g. Ward 85 Karol Bagh, Ward 142 Lajpat Nagar), and PIN codes (110005, 110024).
- **Ward Polygons**: Spatial boundary containment routing incidents to Delhi Jal Board (DJB) Executive Engineers.

### Layer 6: Automated PDF Generation & Municipal Dispatch
- **PDF Engine**: OpenHtmlToPdf + Apache PDFBox compiling official bilingual (English / Hindi जल बोर्ड) Government of NCT of Delhi / Delhi Jal Board incident dossiers.
- **Municipal Dispatch**: Reactive HTTP webhooks pushing incident payloads to City Command & Control Centers (ICCC).
- **Citizen Communication**: SMS / WhatsApp status updates simulation (Fast2SMS / MSG91).
- **Email Dispatch**: `JavaMailSender` routing PDF dossiers to nodal engineers.

### Layer 7: DevOps, Infrastructure & Monitoring
- **Containerization**: Multi-stage [`Dockerfile`](file:///d:/CrowdFlow/Dockerfile) with secure non-root user and ZGC garbage collector flags.
- **Orchestration**: [`docker-compose.yml`](file:///d:/CrowdFlow/docker-compose.yml) deploying App, PostGIS 16, Redis 7, MinIO S3, and Nginx.
- **Reverse Proxy**: [`nginx/nginx.conf`](file:///d:/CrowdFlow/nginx/nginx.conf) with rate-limiting (`15r/m`), gzip compression for GeoJSON, and security headers.
- **CI/CD Pipeline**: [`.github/workflows/ci.yml`](file:///d:/CrowdFlow/.github/workflows/ci.yml) compiling and running test suites on Java 25.
- **Observability**: Spring Boot Actuator + Prometheus metrics at `/actuator/prometheus` tracking live reports, active clusters, and resolution velocity.

---

## 📡 REST API Reference

| Method | Endpoint                           | Access           | Description                                            |
|--------|------------------------------------|------------------|--------------------------------------------------------|
| `POST` | `/api/auth/send-otp`               | Public           | Send 6-digit OTP to Indian phone (`+91`)               |
| `POST` | `/api/auth/verify-otp`             | Public           | Verify OTP & obtain JWT Bearer Token                   |
| `POST` | `/api/reports`                     | Public / Citizen | Submit incident with multipart media & EXIF extraction |
| `POST` | `/api/reports/extract-exif`        | Public           | Preview EXIF GPS coordinates from photo                |
| `GET`  | `/api/reports`                     | Public           | List all Indian water incident reports                 |
| `GET`  | `/api/reports/geojson`             | Public           | Stream GeoJSON FeatureCollection for vector maps       |
| `GET`  | `/api/reports/heatmap`             | Public           | Retrieve weighted points for thermal heatmap           |
| `POST` | `/api/clusters/trigger-clustering` | Public / Officer | Trigger on-demand DBSCAN clustering run                |
| `GET`  | `/api/clusters`                    | Public           | List active emergency incident clusters                |
| `GET`  | `/api/clusters/{id}/pdf-report`    | Public / Officer | Download official DJB municipal escalation PDF dossier |
| `GET`  | `/api/wards`                       | Public           | List Delhi municipal wards and nodal officers          |
| `GET`  | `/api/municipal/dispatch-logs`     | Public / Officer | View dispatch logs and citizen notifications           |
| `GET`  | `/actuator/prometheus`             | Public / Admin   | Scrape Prometheus operational metrics                  |

---

## ⚡ Quickstart Guide

### 1. Run Locally (Embedded Dev Mode)
```bash
# Verify test suite on Java 25
mvn clean test

# Launch Spring Boot with Virtual Threads and embedded H2
mvn spring-boot:run
```
- Web Console: [http://localhost:8085](http://localhost:8085)
- Swagger UI: [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html)
- Prometheus Metrics: [http://localhost:8085/actuator/prometheus](http://localhost:8085/actuator/prometheus)

### 2. Run Multi-Container Stack with Docker Compose
```bash
docker compose up -d --build
```
This spins up:
- **Nginx API Gateway**: `http://localhost` (Port 80)
- **Spring Boot Monolith (Java 25)**: Port 8085
- **PostgreSQL 16 + PostGIS 3.4**: Port 5432
- **Redis 7.2**: Port 6379
- **MinIO S3 Console**: [http://localhost:9001](http://localhost:9001) (`minioadmin` / `minioadmin`)

---
