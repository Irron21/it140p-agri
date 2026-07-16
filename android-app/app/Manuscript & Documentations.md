# **ILMS Technical Reference Guide**

## **Introduction**

The Integrated Logistics Management System (ILMS) is a web-based platform designed to digitize and streamline end‑to‑end logistics operations. It supports dispatch planning, real-time shipment tracking (including multi-drop routes), KPI reporting, and payroll computation for drivers and helpers. The system is built with a modern web stack to balance usability for field personnel and robustness for operations and finance teams.

 

**Purpose of this document** 

This Technical Guide provides a unified, implementation-level view of ILMS for engineers, architects, and technical stakeholders. It explains how the system is structured, how data flows through the stack, and how key business rules are enforced in code. The document is intended to:

* Onboard new developers quickly by exposing the main modules and patterns.

* Serve as a reference during feature development, debugging, and refactoring.

* Provide a basis for technical reviews, audits, and future architectural decisions.

 

**Scope**

This guide focuses on the core technical components of ILMS:

* **React Frontend (client/)**

  * SPA built with React and Vite.

  * Desktop interface for Admin/Operations users and a mobile-oriented interface for Drivers/Helpers.

  * Key features: shipment monitoring, multi-drop timelines, KPI dashboards, and payroll review.

* **Node/Express Backend (server/)**

  * RESTful API built on Express.

  * Implements authentication with JWT and Redis-backed single-session control.

  * Encodes business logic for shipment lifecycle, KPI aggregation, and payroll calculation.

* **Data Layer (MySQL & Redis)**

  * **MySQL** as the primary relational data store:

    * Core entities: Users, Vehicles, Shipments, ShipmentDrops, ShipmentStatusLog, and payroll tables.

    * Strict referential integrity for multi-drop shipments and audit logs.

  * **Redis** as a supporting in-memory data layer:

    * Caching of frequently-read API responses.

    * Storage of active auth sessions for enforcing one active login per user.

Infrastructure topics such as Docker, Nginx, and environment configuration are covered at a practical level, with emphasis on how they support the application’s runtime behavior rather than acting as an exhaustive operations manual.

 

## **1\. Project Analysis**

* **Monorepo layout**

  * Root: docker-compose, top-level README.md, base package.json, and environment for client \+ server \+ MySQL \+ Redis \+ Nginx.

  * client/: React \+ Vite SPA.

  * server/: Node.js \+ Express API.

  * database/: init\_db.sql for schema and seed data.

  * nginx-proxy/: Nginx reverse proxy config for production.

* **Client (React \+ Vite)**

  * Entry points:

    * client/src/main.jsx → bootstraps React app.

    * client/src/App.jsx → top-level routing/layout.

  * Page structure:

    * pages/LoginPage.jsx – login UI.

    * pages/desktop/DesktopApp.jsx – main desktop shell, menu, route switching.

    * pages/mobile/MobileApp.jsx – mobile-optimized view.

  * Feature modules (under src/features/):

    * shipments/: ShipmentView.jsx, Dashboard.jsx, ShipmentDetails.jsx, ShipmentHistoryModal.jsx.

    * analytics/: KPIView.jsx.

    * payroll/: PayrollView.jsx plus ledger/payment modal components.

    * resources/: UserManagement.jsx, RatesManager.jsx.

    * profile/: ProfileModal.jsx.

  * Shared and utilities:

    * Shared UI: shared/FeedbackModal.jsx, Icons.jsx, PaginationControls.jsx.

    * Utilities: utils/api.js (Axios client), utils/queueManager.js.

    * Domain constants: constants/phases.js, constants/dates.js.

* **Client dependencies (from \`client/package.json\`)**

  * React ecosystem: react, react-dom, react-router-dom.

  * HTTP: axios.

  * Charts: recharts.

  * Tooling: vite, ESLint with React hooks & refresh plugins.

* **Server (Node.js \+ Express)**

  * Entry point:

    * server/server.js.

  * Configuration:

    * DB: config/db.js – MySQL connection pool (env‑driven).

    * Redis: config/redis.js – cloud vs local Redis, non-fatal failure.

  * Middleware:

    * Auth: middleware/authMiddleware.js – JWT verification \+ single-session check via Redis/MySQL.

    * Cache: middleware/cacheMiddleware.js – GET response caching in Redis.

    * Security & infra in server.js: helmet, cors, compression, express-rate-limit, global error handler.

* **Server controllers & routes**

  * Auth: controllers/authController.js

    * Login, logout, JWT issuance, Redis session management, user activity logging.

  * Users: controllers/userController.js \+ routes/userRoutes.js

    * CRUD, soft delete/restore, password reset, caching of user list.

  * Shipments: controllers/shipmentController.js \+ routes/shipmentRoutes.js

    * Shipment creation (single/batch), multi-drop support, status logging, querying.

  * Vehicles: vehicleController.js \+ vehicleRoutes.js.

  * KPI/Analytics: kpiController.js \+ kpiRoutes.js.

  * Payroll: payrollController.js, ratesController.js, paymentsController.js with corresponding routes.

  * Adjustments: adjustmentsController.js \+ adjustmentsRoutes.js.

  * Logs: logController.js \+ logRoutes.js.

  * Utilities: utils/activityLogger.js, utils/cacheHelper.js.

* **Server dependencies (from \`server/package.json\`)**

  * Core: express, mysql2, dotenv.

  * Auth & security: bcryptjs, jsonwebtoken, helmet, cors, express-rate-limit, compression.

  * Data/infra: redis, multer, xlsx.

  * Observability: @google-cloud/error-reporting (optionally, in production).

* **Database**

  * Defined in database/init\_db.sql.

  * Includes:

    * Users, UserLogins, UserActivityLog.

    * Shipments, ShipmentDrops, ShipmentStatusLog (multi-drop model).

    * Vehicles and other resource tables.

    * Payroll-related tables (PayrollPeriods, PayrollRates, Adjustments, Payments).

* **Deployment**

  * Dockerized client and server (separate Dockerfiles).

  * Nginx configs:

    * Client static serving: client/nginx.conf.

    * Reverse proxy: nginx-proxy/conf.d/default.conf.

  * Orchestrated via docker-compose.yml.

 

 

## **2\. Key Functional Modules**

 

We can talk about the system in terms of these main modules: 

* **Authentication & Session Management**

  * Login/logout, JWT issuance and verification, single-session enforcement (Redis \+ MySQL), user activity logging.

* **User & Resource Management**

  * User CRUD, roles, activation/deactivation, password resets.

  * Vehicles, rates, and other resource data for operations and payroll.

* **Shipments & Multi-Drop Routing**

  * Shipment creation (including multi-drop), assignment to vehicles/crew.

  * Status tracking with phases (warehouse vs store).

  * Timeline views and dashboards in the frontend.

  * Status logs at shipment and per-drop level.

* **KPI & Analytics**

  * Upload/import of KPI Excel files.

  * Summary KPIs surfaced in KPIView.jsx with charts (Recharts).

  * Backend parsing and validation in kpiController.

* **Payroll & Finance**

  * Payroll periods, rates per route/vehicle, adjustments, and payments.

  * Integrated with route/cluster definitions from shipments.

* **Logging & Audit**

  * Central UserActivityLog via activityLogger.

  * API endpoints to query and display logs.

* **Infrastructure & Security**

  * Middleware stack (helmet, cors, rate-limit, compression).

  * Redis: caching and sessions, with safe fallbacks.

  * Error handling (Express global handler; optional GCP error reporting).

 

 

**3\. Table of Contents for the Technical Guide**

 

Below is a detailed ToC you can use as the skeleton for your document.

 

1\. **Introduction**

* 1.1 Purpose of This Document  

  * 1.2 Target Audience (Stakeholders, Developers, Ops)  

  * 1.3 High-Level Overview of ILMS  

  * 1.4 Key Features and Business Goals

2\. **System Architecture Overview**

* 2.1 High-Level Architecture Diagram  

  * 2.2 Components Summary (Client, API, DB, Redis, Nginx)  

  * 2.3 Technology Stack  

  * 2.4 Deployment Topology (Docker, docker-compose, Environments)

3\. **Frontend Architecture (Client)**

* 3.1 Tech Stack (React, Vite, Axios, Recharts)  

  * 3.2 Application Entry Points (main.jsx, App.jsx)  

  * 3.3 Routing & Navigation  

    * 3.3.1 Login Flow  

    * 3.3.2 DesktopApp Layout  

    * 3.3.3 MobileApp Layout

  * 3.4 Features & Modules  

    * 3.4.1 Shipments Module (Dashboard, ShipmentView, Details, History)  

    * 3.4.2 Analytics (KPIView)  

    * 3.4.3 Payroll (PayrollView & modals)  

    * 3.4.4 Resources (User Management, Rates Manager, Vehicles UI)  

    * 3.4.5 Profile & Settings

  * 3.5 Shared Components & Utilities  

    * 3.5.1 Icons, FeedbackModal, PaginationControls  

    * 3.5.2 Constants (phases, dates)  

    * 3.5.3 API Client (Axios wrapper)

  * 3.6 Styling & UX  

    * 3.6.1 CSS Structure (features, pages, shared)  

    * 3.6.2 Desktop vs Mobile Behavior

5\. **Authentication & Security**

* 5.1 Login Flow (End-to-End)  

  * 5.1.1 /api/login Request/Response  

    * 5.1.2 Password Verification (bcrypt)  

    * 5.1.3 JWT Generation and Claims (id, role, expiry)

  * 5.2 Session Management  

    * 5.2.1 Redis Session Storage (session:\<userID\>)  

    * 5.2.2 MySQL UserLogins.activeToken Fallback  

    * 5.2.3 Single-Device Session Enforcement

  * 5.3 Auth Middleware (verifyToken)  

    * 5.3.1 Token Extraction & Validation  

    * 5.3.2 Redis Path vs DB Fallback  

    * 5.3.3 Error Responses and Expired Sessions

  * 5.4 Logout Flow (/api/logout)  

  * 5.5 Additional Security Layers  

    * 5.5.1 Helmet HTTP Headers  

    * 5.5.2 Rate Limiting (Global & Login)  

    * 5.5.3 CORS Policy  

    * 5.5.4 Password Hashing & Storage

6\. **API Design & Routing**

* 6.1 Overall Routing Structure  

  * 6.1.1 Public Routes (/api/login)  

    * 6.1.2 Protected Routes (/api/shipments, /api/users, etc.)

  * 6.2 Shipments API  

    * 6.2.1 Main Endpoints (Create, Batch Create, Update Status, Fetch)  

    * 6.2.2 Multi-Drop Handling (ShipmentDrops, dropID)  

    * 6.2.3 Status Log & Timeline

  * 6.3 User Management API  

    * 6.3.1 Get Users (with Cache)  

    * 6.3.2 Create, Update, Soft Delete, Restore  

    * 6.3.3 Password Reset and Role Handling

  * 6.4 Vehicles API  

  * 6.5 KPI / Analytics API  

    * 6.5.1 Excel Upload & Parsing  

    * 6.5.2 Validation and Error Handling

  * 6.6 Payroll-Related APIs (Rates, Periods, Payments, Adjustments)  

  * 6.7 Logging API (/api/logs)  

    * 6.7.1 Activity Log Queries  

    * 6.7.2 Custom Log Entries

7\. **Database Design**

* 7.1 ER Diagram Overview  

  * 7.2 Core Tables  

    * 7.2.1 Users, UserLogins, UserActivityLog  

    * 7.2.2 Shipments, ShipmentDrops, ShipmentStatusLog  

    * 7.2.3 Vehicles

  * 7.3 Payroll Schema  

    * 7.3.1 PayrollPeriods  

    * 7.3.2 PayrollRates  

    * 7.3.3 Adjustments & Payments

  * 7.4 Key Relationships & Constraints  

    * 7.4.1 Foreign Keys and Cascades  

    * 7.4.2 Multi-Drop Relationships (ShipmentDrops ↔ Shipments)

  * 7.5 Seed Data & Initialization (init\_db.sql)

8\. **Caching & Performance**

* 8.1 Redis Integration Overview  

  * 8.2 Cache Middleware (Response Caching)  

    * 8.2.1 Key Strategy (cache:\<url\>)  

    * 8.2.2 Duration & Invalidation (cacheHelper)

  * 8.3 Session Storage in Redis  

  * 8.4 Performance Considerations  

    * 8.4.1 Query Optimization  

    * 8.4.2 Pagination Strategies (Shipments, Payroll, Logs)

9\. **Logging, Auditing & Observability**

* 9.1 User Activity Logging (activityLogger)  

  * 9.2 Log-Related Endpoints (/api/logs)  

  * 9.3 Error Reporting  

    * 9.3.1 Global Error Handler  

    * 9.3.2 Optional Google Cloud Error Reporting Integration

  * 9.4 Operational Logs (Server, DB, Redis, Nginx)

10\. **Data Flow Scenarios**

* 10.1 Login & Session Validation Flow  

  * 10.2 Create Shipment (Single vs Multi-Drop)  

  * 10.3 Shipment Status Update & Timeline Rendering  

  * 10.4 KPI File Upload to Dashboard Visualization  

  * 10.5 Payroll Period Generation and Payment Flow

11\. **Setup & Installation**

* 11.1 Prerequisites (Node, Docker, MySQL, Redis)  

  * 11.2 Local Development Setup  

    * 11.2.1 Environment Variables  

    * 11.2.2 Running Client and Server via npm

  * 11.3 Docker & Production Deployment  

    * 11.3.1 docker-compose Flow  

    * 11.3.2 Nginx Proxy Configuration

  * 11.4 Database Initialization (init\_db.sql)

14\. **Extensibility & Future Enhancements**

* 14.1 Adding New Features / Modules  

  * 14.2 Scaling Strategies (DB, Caching, Horizontal Scaling)  

  * 14.3 Potential Refactors (Domain boundaries, modularization)

