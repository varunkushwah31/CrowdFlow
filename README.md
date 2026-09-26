# 🌊 CrowdFlow - Community Water Issue Crowdsourcing Platform

> **Civic-Tech Municipal Water Infrastructure Crowdsourcing, Geospatial Clustering & Automated Escalation Platform**  
> *Tailored specifically for Indian Municipal Governance (Delhi Jal Board - DJB, Municipal Corporation of Delhi - MCD, NDMC, and Jal Jeevan Mission / AMRUT 2.0).*

---

## 📌 Overview

**CrowdFlow** bridges the critical gap between citizens experiencing water crises and municipal engineering departments. By leveraging crowdsourced incident reports, automated EXIF metadata parsing, geodesic spatial clustering, root-cause correlation diagnostics, and automated municipal PDF dossier generation, CrowdFlow accelerates incident response from days to minutes.

---

## 🚀 Key Features

### 1. 📷 Data Collection & EXIF Intake
- **Zero-Friction Incident Reporting**: Citizens upload photos or videos of water emergencies (Burst Main, Sewage Ingress, Low Pressure, Contamination, Open Drain).
- **Automated EXIF Metadata Extraction**: Extracts precise GPS latitude/longitude, altitude, timestamp, and device details via Drew Noakes `metadata-extractor`.
- **Reverse Geocoding**: Resolves raw coordinates into Indian localities, municipal wards (e.g., Ward 85 Karol Bagh, Ward 142 Lajpat Nagar), and Indian PIN codes (e.g., 110005, 110024).

### 2. 🗺️ Geospatial Visualization & Leaflet Dashboard
- **Interactive OpenStreetMap Interface**: Responsive web console with live incident pins color-coded by issue type.
- **Thermal Density Heatmap**: Built using `Leaflet.heat` for instant visual hotspot identification.
- **Convex Hull Envelopes**: Visualizes cluster boundaries computed geometrically via Java Topology Suite (JTS).
- **Incident Inspector**: Real-time inspection drawer displaying reverse-geocoded addresses, evidence photos, status, and citizen verification counts.

### 3. 🧠 Geodesic DBSCAN Spatial Clustering & Root Cause Diagnostics
- **Haversine DBSCAN Algorithm**: Groups reports within a tunable 300-meter radius into unified emergency clusters.
- **Root Cause Correlation Engine**: Automatically recognizes underlying infrastructure failure patterns:
  - *Main Feeder Pipe Fracture* (Co-located high-pressure leaks & low-pressure drops)
  - *Sewerage Cross-Contamination / Ingress* (Simultaneous dirty water reports near drainage sumps)
  - *Pumping Station Operational Failure* (Widespread zero-pressure reports across an entire ward)
  - *Monsoon Drain Siltation & Backflow* (Localized road waterlogging)
- **Dynamic Severity Scoring**: Computes severity levels (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`) based on citizen volume, rate of incoming reports, and vulnerability zones (e.g., hospitals, schools).

### 4. 📄 Automated Official Municipal Dossier (PDF)
- **Executive Engineering Dossiers**: Generates downloadable Government of NCT of Delhi / Delhi Jal Board standard PDF reports via OpenHtmlToPdf.
- **Key Dossier Components**:
  - Official bilingual headers (Delhi Jal Board / जल बोर्ड)
  - Ward jurisdiction and nodal officer contact details
  - JTS-computed centroid coordinates and cluster bounding envelope
  - Citizen evidence photo sheet with timestamps and phone hashes
  - Work Order dispatch instructions under Jal Jeevan Mission & AMRUT 2.0 standards

### 5. ⚡ Automated Dispatch & Citizen Feedback Loop
- **Spatial Ward Boundary Containment**: Routes alerts directly to the designated Assistant Engineer (AE) / Executive Engineer (EE).
- **Municipal Webhook Integration**: Simulates immediate dispatch payloads to municipal CRM portals.
- **Citizen Notification Simulation**: Sends SMS/WhatsApp status updates and estimated resolution times to affected residents.

---

## 🛠️ Technology Stack

| Component                 | Technology                                            | Version / Spec                                                                       |
|---------------------------|-------------------------------------------------------|--------------------------------------------------------------------------------------|
| **Runtime & Language**    | Java (LTS)                                            | **Java 25**                                                                          |
| **Framework**             | Spring Boot                                           | **3.4.13**                                                                           |
| **Boilerplate Reduction** | Project Lombok                                        | **1.18.38** (`@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`) |
| **Spatial Engine**        | JTS (Java Topology Suite) & Hibernate Spatial         | `1.19.0`                                                                             |
| **EXIF Parsing**          | Drew Noakes Metadata Extractor                        | `2.19.0`                                                                             |
| **PDF Engine**            | OpenHtmlToPdf & Apache PDFBox                         | `1.0.10`                                                                             |
| **Mathematics**           | Apache Commons Math 3                                 | `3.6.1` (Haversine & DBSCAN clustering)                                              |
| **Frontend UI**           | HTML5, CSS3, Vanilla JS, Leaflet.js, Leaflet-heat     | Leaflet 1.9.4                                                                        |
| **Database**              | H2 Database (Dev) / PostgreSQL PostGIS (Prod profile) | Embedded File-based H2                                                               |
| **API Docs**              | Springdoc OpenAPI & Swagger UI                        | `2.6.0`                                                                              |

---

## 📋 Project Structure

```
CrowdFlow/
├── src/
│   ├── main/
│   │   ├── java/com/civic/waterwatch/
│   │   │   ├── WaterWatchApplication.java             # Main Application Entry Point
│   │   │   ├── config/
│   │   │   │   └── OpenApiConfig.java                 # Swagger / OpenAPI Configuration
│   │   │   ├── incident/
│   │   │   │   ├── controller/IncidentReportController.java
│   │   │   │   ├── controller/MediaStorageController.java
│   │   │   │   ├── dto/                               # Request, Response, and GeoJSON DTOs
│   │   │   │   ├── model/                             # WaterReport, IssueType, ReportStatus
│   │   │   │   ├── repository/WaterReportRepository.java
│   │   │   │   └── service/                           # EXIF Parser, Geocoding, Intake Service
│   │   │   ├── clustering/
│   │   │   │   ├── controller/IncidentClusterController.java
│   │   │   │   ├── model/                             # IncidentCluster, RootCauseAnalysis
│   │   │   │   ├── service/SpatialClusteringService.java      # Geodesic DBSCAN & Convex Hull
│   │   │   │   ├── service/RootCauseCorrelationEngine.java    # Diagnostic Patterns
│   │   │   │   └── service/ClusterScheduler.java              # Background Cluster Polling
│   │   │   ├── reporting/
│   │   │   │   └── service/PdfReportService.java      # OpenHtmlToPdf Municipal Dossier Service
│   │   │   ├── ward/
│   │   │   │   ├── model/MunicipalWard.java           # Delhi Ward Boundaries & Nodal Officers
│   │   │   │   └── service/WardRoutingService.java    # Spatial Ward Containment
│   │   │   └── dispatch/
│   │   │       ├── controller/MunicipalPortalController.java
│   │   │       ├── model/DispatchLog.java
│   │   │       └── service/                           # Dispatch & Citizen Notification
│   │   └── resources/
│   │       ├── application.yml                        # Configuration (Port 8085, H2)
│   │       ├── application-postgres.yml               # Production PostGIS profile
│   │       ├── static/
│   │       │   ├── index.html                         # Interactive Civic Dashboard
│   │       │   ├── css/style.css                      # Modern Civic-Tech Design System
│   │       │   └── js/app.js                          # Leaflet & Cluster Interaction Logic
│   │       └── templates/
│   │           └── incident-report.html               # Municipal PDF Dossier Template
│   └── test/
│       └── java/com/civic/waterwatch/
│           ├── WaterWatchIntegrationTest.java         # End-to-end integration tests
│           ├── clustering/SpatialClusteringDistanceTest.java
│           ├── clustering/RootCauseCorrelationTest.java
│           └── ward/WardRoutingTest.java
├── pom.xml                                            # Maven configuration (Java 25)
└── README.md
```

---

## ⚡ Getting Started

### Prerequisites
- **JDK 25** (Oracle JDK 25 or OpenJDK 25)
- **Apache Maven 3.9+**
- Git

### Build & Run Tests
```bash
mvn clean test
```

### Launch the Application
```bash
mvn spring-boot:run
```

Once running, navigate to:
- **Interactive Dashboard**: [http://localhost:8085](http://localhost:8085)
- **Swagger API Documentation**: [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html)
- **H2 Web Console**: [http://localhost:8085/h2-console](http://localhost:8085/h2-console) (JDBC URL: `jdbc:h2:file:./data/waterwatch`)

---

## 📡 REST API Reference

| Method | Endpoint                           | Description                                              |
|--------|------------------------------------|----------------------------------------------------------|
| `POST` | `/api/reports`                     | Submit incident report with JSON metadata                |
| `POST` | `/api/reports/upload-image`        | Upload incident photo with automated EXIF intake         |
| `GET`  | `/api/reports`                     | Retrieve list of citizen incident reports                |
| `GET`  | `/api/reports/geojson`             | Stream reports as RFC-7946 GeoJSON FeatureCollection     |
| `POST` | `/api/clusters/trigger-clustering` | Trigger on-demand Haversine DBSCAN clustering run        |
| `GET`  | `/api/clusters`                    | List active emergency incident clusters                  |
| `GET`  | `/api/clusters/{id}/pdf-report`    | Download official DJB municipal escalation PDF dossier   |
| `GET`  | `/api/wards`                       | List configured Delhi municipal wards and nodal officers |
| `GET`  | `/api/municipal/dispatch-logs`     | View automated dispatch logs and citizen notifications   |

---

## 🏛️ Indian Civic Context & Standards

- **Authorities Modeled**: Delhi Jal Board (DJB), Municipal Corporation of Delhi (MCD), New Delhi Municipal Council (NDMC).
- **Civic Hotlines**: Centralized DJB Helpline `1916`, MCD Centralized Toll-Free `1533`.
- **National Missions**: Aligned with Ministry of Jal Shakti guidelines, Jal Jeevan Mission (Urban), and AMRUT 2.0 service level benchmarks.

---
