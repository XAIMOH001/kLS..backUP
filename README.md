# 🍾 Kamnyweso Liquor Store — Distributed POS System

A **distributed Java point-of-sale (POS) system** designed for **Kamnyweso Liquor Store**, enabling seamless order management and inventory tracking across four decentralized branches: **Nairobi (HQ)**, **Nakuru**, **Mombasa**, and **Kisumu**.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Usage Guide](#usage-guide)
- [Development](#development)
- [Troubleshooting](#troubleshooting)
- [Future Roadmap](#future-roadmap)

---

## Overview

Kamnyweso Liquor Store operates across multiple branches, each requiring independent order management and inventory tracking. This system provides a unified solution where:

- **Customers** at any branch can place orders via a web interface
- **Admin staff** monitor live sales reports, inventory levels, and low-stock alerts
- **HQ staff** manage stock transfers from the central warehouse to branches
- All data is centralized in a single SQLite database, eliminating synchronization issues

The entire system runs on **a single Java HTTP server** over a local WiFi network with **no external dependencies** and **no build tools** beyond vanilla `javac`.

---

## 🎯 Key Features

### For Customers
✅ **Branch Selection** — Choose from 4 locations (Nairobi, Nakuru, Mombasa, Kisumu)  
✅ **Live Stock Display** — See available quantities and prices in real-time  
✅ **Simple Ordering** — Name + phone + quantity selection → instant receipt  
✅ **Low Stock Warnings** — Know immediately if items are running low  
✅ **Multi-Device Access** — Any device on WiFi can access the system  

### For Admin
✅ **Real-Time Dashboard** — View live orders, revenue, and inventory  
✅ **4 Required Reports:**
   1. **Customer Orders** — Detailed transaction history with customer info
   2. **Branch Sales** — Revenue per location with order counts
   3. **Total Revenue** — Grand total with contribution breakdown
   4. **Low Stock Alerts** — Items below minimum threshold with visual warnings

✅ **Inventory Management** — HQ stock transfer to branches with validation  
✅ **Auto-Refresh** — 30-second polling for live data without manual refresh  
✅ **Print Reports** — Export reports to PDF/printer from browser  

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────┐
│           WiFi Network (Local)                      │
│                                                      │
│  ┌────────────────┐  ┌────────────────┐             │
│  │  Customer Dev  │  │  Customer Dev  │             │
│  │  (Phone/Tablet)│  │  (Phone/Tablet)│             │
│  └────────┬───────┘  └────────┬───────┘             │
│           │                    │                     │
│  ┌────────▼────────────────────▼────────┐          │
│  │  Java HttpServer (:8080)             │          │
│  │  ├─ Router (API endpoints)           │          │
│  │  ├─ HttpGateway (CORS, static files) │          │
│  │  └─ Repository Layer (DB access)     │          │
│  └──────────────┬──────────────────────┘          │
│                 │                                   │
│  ┌──────────────▼────────────────┐                │
│  │   SQLite Database             │                │
│  │   ├─ branches (4 rows)        │                │
│  │   ├─ drinks (10 products)     │                │
│  │   ├─ stock (40 rows: 4×10)    │                │
│  │   └─ orders (transaction log) │                │
│  └───────────────────────────────┘                │
│                                                      │
│           │                     │                   │
│  ┌────────▼──────┐    ┌────────▼────────┐         │
│  │ Customer Dev  │    │  Admin Device   │         │
│  │ (order.html)  │    │(dashboard.html) │         │
│  └───────────────┘    └─────────────────┘         │
│                                                      │
└─────────────────────────────────────────────────────┘
```

### Data Flow

```
CUSTOMER PLACES ORDER:
  order.html → /api/orders (POST)
    ↓
  OrderRepository.place()
    ↓
  Stock check → Price lookup → Insert order → Deduct inventory
    ↓
  Response with {success, order_id, total, low_stock, remaining}
    ↓
  Browser shows receipt

ADMIN VIEWS DASHBOARD:
  dashboard.html (polls every 30s)
    ↓
  /api/reports/* (GET)
    ↓
  ReportRepository aggregations
    ↓
  Display KPIs, tables, alerts
```

---

## 💻 Technology Stack

| Layer | Technology | Reason |
|-------|-----------|--------|
| **Backend** | Java 17+ | Fast execution, excellent stdlib |
| **HTTP Server** | `java.net.httpserver` (JDK built-in) | No external dependencies |
| **Database** | SQLite 3.44+ | Single file, no daemon, WAL mode for concurrent access |
| **JDBC Driver** | `xerial/sqlite-jdbc` | Bundles native binaries, platform-independent |
| **Logging** | SLF4J Simple | Minimal binding, no verbose noise |
| **Frontend** | HTML5 + CSS3 + Vanilla JS | No npm, no bundlers, single-file UIs |
| **Serialization** | Custom `Json.java` | Zero external dependencies for JSON |
| **Build** | Plain `javac` | No Maven/Gradle overhead |

### Why No Frameworks?

- **No Spring Boot** — Overkill for a demo; `HttpServer` is sufficient
- **No REST framework** — Manual routing is 50 lines of code
- **No ORM** — Raw SQL is clearer and more explicit
- **No npm** — HTML/CSS/JS are served as-is; no build step needed
- **No JSON library** — Custom parser/serializer handles all use cases

---

## 📁 Project Structure

```
kLS..backUP/
├── README.md                    ← This file
├── PROJECT_GUIDE.md             ← Deep technical documentation
├── AGENT.md                     ← Implementation phases
├── ROADMAP.md                   ← Phase milestones
│
├── src/
│   ├── App.java                 ← Entry point (main)
│   ├── HttpGateway.java         ← HTTP server + routing
│   ├── Router.java              ← API endpoint handlers
│   ├── Store.java               ← Shared DB connection + schema
│   ├── Json.java                ← JSON serialization/parsing
│   └── repo/
│       ├── BranchRepository.java    ← Branch queries
│       ├── DrinkRepository.java     ← Drink + stock queries
│       ├── StockRepository.java     ← Inventory management
│       ├── OrderRepository.java     ← Order placement + history
│       └── ReportRepository.java    ← Analytics aggregations
│
├── web/
│   ├── order.html               ← Customer ordering UI
│   ├── order.css                ← Styling for order page
│   ├── dashboard.html           ← Admin dashboard
│   ├── dashboard.css            ← Dashboard styling
│   ├── global.css               ← Shared styles
│   └── images/                  ← Assets (if any)
│
├── lib/
│   ├── sqlite-jdbc-3.44.1.0.jar    ← SQLite driver + binaries
│   ├── slf4j-api-1.7.32.jar        ← Logging API
│   └── slf4j-simple-1.7.32.jar     ← Logging implementation
│
├── compile.sh / compile.bat     ← Build scripts
├── run.sh / run.bat             ← Execution scripts
│
├── out/                         ← Compiled .class files (generated)
├── db/                          ← Database files (generated)
└── scratch/                     ← Development notes (generated)
```

---

## 🚀 Quick Start

### Prerequisites

- **Java 17+** (OpenJDK or Oracle JDK)
- **SQLite** CLI (optional, for debugging only)
- **Linux/macOS/Windows** — all platforms supported

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/XAIMOH001/kLS..backUP.git
   cd kLS..backUP
   ```

2. **Download dependencies** (if not in `lib/`)
   ```bash
   # On Ubuntu/Debian
   apt-get install libxerial-sqlite-jdbc-java libslf4j-java
   cp /usr/share/java/sqlite-jdbc*.jar lib/
   cp /usr/share/java/slf4j-*.jar lib/
   
   # Or download manually from Maven Central:
   # https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.44.1.0/
   # https://repo1.maven.org/maven2/org/slf4j/
   ```

3. **Compile**
   ```bash
   # macOS / Linux
   ./compile.sh
   
   # Windows
   compile.bat
   ```

4. **Run**
   ```bash
   # macOS / Linux (default port 8080)
   ./run.sh
   
   # macOS / Linux (custom port)
   ./run.sh 9000
   
   # Windows
   run.bat
   ```

5. **Access the system**
   - **Customer UI:** `http://localhost:8080/` (or `http://<server-IP>:8080/`)
   - **Admin Dashboard:** `http://localhost:8080/admin` (or `http://<server-IP>:8080/admin`)

---

## 📡 API Documentation

All API endpoints return JSON. Base URL: `http://<server>:8080/api`

### Branches

**GET** `/branches` — List all branches
```bash
curl http://localhost:8080/api/branches
```
**Response:**
```json
[
  {"id": 1, "name": "Nairobi HQ", "city": "Nairobi"},
  {"id": 2, "name": "Nakuru Branch", "city": "Nakuru"},
  {"id": 3, "name": "Mombasa Branch", "city": "Mombasa"},
  {"id": 4, "name": "Kisumu Branch", "city": "Kisumu"}
]
```

### Drinks (with stock)

**GET** `/drinks?branch_id=<N>` — Available drinks at a branch
```bash
curl "http://localhost:8080/api/drinks?branch_id=2"
```
**Response:**
```json
[
  {
    "id": 1, "name": "Tusker Beer", "brand": "Tusker", 
    "price": 150.0, "quantity": 45, "low": false
  },
  {
    "id": 6, "name": "Water", "brand": "Aqua", 
    "price": 50.0, "quantity": 5, "low": true
  }
]
```

### Place Order

**POST** `/orders` — Create a new order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customer_name": "John Doe",
    "customer_phone": "0712345678",
    "branch_id": 2,
    "drink_id": 1,
    "quantity": 2
  }'
```
**Response:**
```json
{
  "success": true,
  "order_id": 42,
  "total": 300.0,
  "low_stock": false,
  "remaining": 43
}
```

### Stock Management

**GET** `/stock` — Full inventory across all branches
```bash
curl http://localhost:8080/api/stock
```

**POST** `/restock` — Transfer stock from HQ to branch
```bash
curl -X POST http://localhost:8080/api/restock \
  -H "Content-Type: application/json" \
  -d '{"branch_id": 3, "drink_id": 1, "quantity": 30}'
```

### Reports

**GET** `/reports/customers` — All orders with customer details
```bash
curl http://localhost:8080/api/reports/customers
```

**GET** `/reports/branches` — Sales performance by branch
```bash
curl http://localhost:8080/api/reports/branches
```

**GET** `/reports/total` — Grand total revenue and order count
```bash
curl http://localhost:8080/api/reports/total
```

**GET** `/reports/lowstock` — Items below minimum threshold
```bash
curl http://localhost:8080/api/reports/lowstock
```

---

## 📖 Usage Guide

### For Customers

1. **Open the order page:**
   - From the same WiFi: `http://<server-IP>:8080/`
   - Desktop: `http://localhost:8080/`

2. **Select a branch** — Nakuru, Mombasa, or Kisumu

3. **Browse drinks** — View name, price, brand, and current stock

4. **Choose quantity** — Adjust the stepper (min 1, max = available)

5. **Enter details:**
   - **Name** (required) — For the order receipt
   - **Phone** (optional) — For customer follow-up

6. **Place order** — Click "Place Order"

7. **View receipt:**
   - Order ID, total, items
   - ⚠️ Warning if stock is low

8. **Place another order** — "Place Another Order" button resets drink selection

### For Admin

1. **Open dashboard:**
   - `http://localhost:8080/admin` (or use server IP)

2. **Monitor Overview panel** (auto-refreshes every 30s):
   - Total revenue, order count, alert count
   - Revenue breakdown per branch
   - Recent orders table

3. **View detailed reports:**
   - **Customer Orders** — Full transaction history
   - **Branch Sales** — Revenue + order count per branch
   - **Total Revenue** — Grand total with contribution %
   - **Inventory** — All stock levels with OK/LOW badges
   - **Low Stock Alerts** — Items below threshold

4. **Manage inventory:**
   - Click **"+ HQ Transfer"** button
   - Select destination branch
   - Choose drink (shows HQ stock available)
   - Enter transfer quantity
   - Click "Transfer Stock" — inventory updated instantly

5. **Print reports:**
   - Click "Print Document" button in Master Report
   - Browser print dialog opens
   - Save as PDF or print to paper

---

## 🔧 Development

### Building from Source

```bash
# Clean previous build
rm -rf out/ drinks.db

# Compile Java source
./compile.sh           # Linux/macOS
compile.bat            # Windows

# Run tests (Layer 2: API smoke test)
./run.sh &
sleep 2
curl -s http://localhost:8080/api/branches | python3 -m json.tool
kill %1
```

### Database Schema

```sql
branches
  ├─ id INTEGER PRIMARY KEY
  ├─ name TEXT
  └─ city TEXT

drinks
  ├─ id INTEGER PRIMARY KEY AUTOINCREMENT
  ├─ name TEXT
  ├─ brand TEXT
  └─ price REAL

stock
  ├─ id INTEGER
  ├─ branch_id INTEGER FK
  ├─ drink_id INTEGER FK
  ├─ quantity INTEGER
  ├─ threshold INTEGER
  └─ UNIQUE(branch_id, drink_id)

orders
  ├─ id INTEGER PRIMARY KEY AUTOINCREMENT
  ├─ customer_name TEXT
  ├─ customer_phone TEXT
  ├─ branch_id INTEGER FK
  ├─ drink_id INTEGER FK
  ├─ quantity INTEGER
  ├─ unit_price REAL
  ├─ total_price REAL
  └─ order_time DATETIME DEFAULT CURRENT_TIMESTAMP
```

### Code Style Guide

- **Java:** PascalCase for classes, camelCase for methods/fields
- **Packages:** `repo.*` for repositories, `src/` for core
- **SQL:** Text blocks (`"""…"""`) for multi-line queries
- **Exceptions:** All database methods throw `SQLException`
- **JSON:** Use `LinkedHashMap` for stable key ordering

### Testing Strategy

See **PROJECT_GUIDE.md** Section 4 for comprehensive testing layers:

1. **Schema Correctness** — Verify database tables and seed data
2. **API Smoke Tests** — curl commands for each endpoint
3. **Low Stock Signal Test** — Verify alerts trigger correctly
4. **WiFi Multi-Device Test** — Full end-to-end demo

---

## 🐛 Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| `ClassNotFoundException: org.sqlite.JDBC` | sqlite-jdbc.jar missing | Check `lib/` directory; verify classpath in compile.sh |
| `NoClassDefFoundError: org/slf4j/LoggerFactory` | SLF4J libraries missing | Add `slf4j-api.jar` + `slf4j-simple.jar` to classpath |
| `SQLITE_BUSY: database is locked` | Multiple connections created | Ensure all repos use `Store.connection()`; check for multiple `HttpServer` instances |
| `Address already in use: 8080` | Previous server still running | `pkill -f "App"` or wait 5s for OS cleanup; use different port |
| `404` on static files | Working directory wrong | Run JVM from project root: `cd kLS..backUP && java ...` |
| Browser shows "Connection refused" (from other device) | Server bound to localhost only | Verify `HttpServer` binds to `0.0.0.0` (default) |
| Reports show KES 0 | No orders placed | Place at least one order via customer UI; refresh dashboard |
| Restock fails: "HQ insufficient stock" | HQ stock exhausted | HQ stock starts at 50 per drink; avoid ordering from HQ during demo |
| `java: command not found` | Java not installed | Install JDK 17+ (OpenJDK or Oracle) |
| Compilation warnings | Unused imports or unchecked generics | Safe to ignore for demo; all functionality works |

---

## 📊 Performance & Scalability

### Current Limits

- **Concurrent Requests:** ~8 threads (fixed thread pool in HttpServer)
- **Data Volume:** ~10 drinks × 4 branches × 10,000 orders = manageable SQLite size
- **WiFi Devices:** Tested with 3-4 customer devices + 1 admin device
- **Network Latency:** Sub-100ms typical on local WiFi

### Optimization Notes

- SQLite WAL mode enables concurrent readers during writes
- One shared `Connection` object prevents `SQLITE_BUSY` errors
- 30-second auto-refresh reduces polling overhead on admin panel
- No database transactions (auto-commit mode) for simplicity

### NOT Included (By Design)

- Pagination (demo datasets are small)
- Caching layer (SQLite is already fast for this scale)
- WebSocket live updates (30s polling is sufficient)
- Horizontal scaling (single-process architecture is correct)

---

## 🗺️ Future Roadmap

### Phase 1–5 (COMPLETE) ✅
- ✅ Database foundation & schema
- ✅ HTTP server & routing
- ✅ All repositories & business logic
- ✅ Customer ordering UI
- ✅ Admin dashboard & reports

### Potential Enhancements (NOT in current scope)

- [ ] User authentication & role-based access
- [ ] Order history per customer (browser localStorage)
- [ ] Inventory forecasting based on historical trends
- [ ] SMS notifications for low stock
- [ ] Multi-currency support (KES / USD / etc.)
- [ ] Mobile app wrapper (React Native / Flutter)
- [ ] Backup & data export to CSV/Excel
- [ ] Barcode scanning for orders

### What Will NEVER be Added

- Microservices (single process is correct)
- Database replication (local SQLite is intended)
- Maven/Gradle (javac + manual classpath is intentional)
- Frontend framework overhead (vanilla JS is sufficient)
- HTTPS in demo mode (local WiFi only)

---

## 📚 Documentation Files

| File | Purpose |
|------|---------|
| **README.md** | Overview, quick start, API reference (this file) |
| **PROJECT_GUIDE.md** | Technical deep-dive: architecture, schema, testing |
| **AGENT.md** | Implementation phases and coding constraints |
| **ROADMAP.md** | Phase milestones and scope boundaries |

**Start with:** README.md (you are here)  
**Then read:** PROJECT_GUIDE.md for architectural details  
**Implementation:** Follow AGENT.md phases  
**Planning:** Reference ROADMAP.md for scope  

---

## 🤝 Contributing

### Code Organization

- All Java files follow the **AGENT.md** code style guide
- Repositories live in `src/repo/` with `package repo;` declaration
- Shared utilities (`Json.java`, `Store.java`) in `src/`
- Frontend files (HTML/CSS/JS) in `web/` as single-file pages

### Adding a New API Endpoint

1. Create repository method in `src/repo/SomethingRepository.java`
2. Add router method in `src/Router.java` (calls the repository)
3. Register route in `HttpGateway.java` (maps path → handler)
4. Test with `curl` before adding UI

### Testing Changes

```bash
./compile.sh
./run.sh &
# Test API with curl commands
# Test UI in browser
kill %1
```

---

## 📞 Support & Contact

For issues, questions, or contributions:

1. **Check PROJECT_GUIDE.md** — Most issues are covered in Section 5 (Debugging Decision Tree)
2. **Review AGENT.md** — Implementation constraints and best practices
3. **Run Layer 2 smoke tests** — Verify API is responding correctly
4. **Check firewall** — If devices can't connect, verify WiFi and port 8080 is open

---

## 📄 License

This project is provided as an educational demonstration system for the Kamnyweso Liquor Store. All source code is included and available for modification.

---

## ✨ Key Achievements

✅ **Zero external build tools** — Pure `javac` compilation  
✅ **Single process, single SQLite file** — No microservices complexity  
✅ **Distributed by design** — 4 WiFi devices, 1 shared database  
✅ **Real-time reporting** — Admin sees orders as they happen  
✅ **Low stock alerts** — Inventory management built-in  
✅ **Cross-platform** — Runs identically on Windows, macOS, Linux  
✅ **Fast startup** — Full system boots in <1 second  
✅ **No external services** — No cloud, no API calls, no internet needed  

---

**Last Updated:** May 17, 2026  
**Repository:** [XAIMOH001/kLS..backUP](https://github.com/XAIMOH001/kLS..backUP)  
**Status:** Production Ready for Local Demo
