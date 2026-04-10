# ROADMAP.md — Drinks POS v2

Phase-by-phase plan. Each phase ends with a working, testable system state.

---

## Phases Overview

| Phase | Name | Goal | Est. Files |
|-------|------|------|-----------|
| 1 | Foundation | DB initialises, schema seeded, compile pipeline works | `Store.java`, `App.java`, scripts |
| 2 | HTTP Layer | Server starts, `/api/branches` responds | `HttpGateway.java`, `Router.java`, `Json.java`, `BranchRepository.java` |
| 3 | All Repositories | Every API endpoint works | All `repo/*.java`, all routes in `Router.java` |
| 4 | Customer UI | Full order flow from a browser | `web/order.html` |
| 5 | Admin Dashboard | All 4 required reports demonstrable | `web/dashboard.html` |

---

## Not In Scope (Ever)

| Item | Reason |
|------|--------|
| Authentication / login | Not required by brief; adds complexity with no demo value |
| HTTPS / TLS | Local WiFi demo; certificate management is unnecessary overhead |
| Multiple Java processes | Single process is the correct architecture — "distributed" refers to the network of client devices, not server-side distribution |
| Database replication or sync | One SQLite file on one machine is the intended data layer |
| Maven / Gradle / any build tool | Students compile with `javac` — keep the build trivial |
| A frontend framework (React, Vue, etc.) | Plain HTML/JS is sufficient and keeps the zip size minimal |
| Pagination on reports | Not required; demo datasets are small |
| User-created branches or drinks | Data is seeded; schema changes are out of scope for the presentation |
| Order deletion or cancellation | Not in the project brief |

---

## Phase 1 — Foundation

**Goal:** The project compiles cleanly and the SQLite database initialises with
the full schema and seed data.

**In scope:**
- Directory structure: `src/repo/`, `web/`, `lib/`, `out/`
- `Store.java` with shared connection factory and `init()` method
- `App.java` that calls `Store.init()` and exits
- `compile.sh`, `compile.bat`, `run.sh`, `run.bat`
- Placing the three required JARs in `lib/`

**Out of scope this phase:**
- HTTP server
- Any repository other than the init logic
- Frontend files

**Success criteria:**
```bash
./compile.sh                                    # exit 0, no warnings
java -cp "out:lib/sqlite-jdbc.jar:lib/slf4j-api.jar:lib/slf4j-simple.jar" App
sqlite3 drinks.db "SELECT COUNT(*) FROM stock;"  # → 40
sqlite3 drinks.db "SELECT COUNT(*) FROM drinks;" # → 10
sqlite3 drinks.db "SELECT * FROM branches;"      # → 4 rows
```

**Key implementation notes:**
- `Store.init()` must use its own `DriverManager.getConnection()`, not `Store.connection()`,
  to avoid holding the shared connection during the multi-statement seed sequence.
- `INSERT OR IGNORE` on all seed data — safe to run on every startup.
- WAL mode must be set in `Store.init()` before any other statement.

---

## Phase 2 — HTTP Layer

**Goal:** The HTTP server starts on port 8080. `GET /api/branches` returns a JSON
array of 4 branches. `GET /` serves `order.html`.

**In scope:**
- `Json.java` — `toJson()` and `parseBody()`
- `BranchRepository.java` — `getAll()`
- `HttpGateway.java` — server init, CORS, static file serving, one API context
- `Router.java` — `branches()` method only
- Startup banner printed to stdout showing the server URL

**Out of scope this phase:**
- All routes except `/api/branches`
- Order, stock, or report endpoints
- Any frontend beyond confirming static serving works

**Success criteria:**
```bash
curl -s http://localhost:8080/api/branches | python3 -m json.tool
# → valid JSON array, 4 objects with id/name/city
curl -s http://localhost:8080/
# → HTML content (order.html served)
```

**Key implementation notes:**
- `HttpGateway`'s catch-all context (`/`) must check if the file exists before
  serving — return 404 with a plain text body if not found.
- CORS headers (`Access-Control-Allow-Origin: *`) must be set on every response,
  including errors. The browser will silently drop responses without them when the
  request comes from a different device's IP.
- The `OPTIONS` preflight method must return 204 immediately, before the handler runs.

---

## Phase 3 — All Repositories

**Goal:** Every API endpoint is registered and returns correct data. A full order
can be placed via `curl` and appears in all three report endpoints.

**In scope:**
- `DrinkRepository` — `withStock()`, `getPrice()`
- `StockRepository` — `checkQty()`, `getThreshold()`, `deduct()`, `transferFromHQ()`, `getLow()`, `getAll()`
- `OrderRepository` — `place()`, `all()`
- `ReportRepository` — `customerOrders()`, `branchSales()`, `grandTotal()`
- All routes added to `Router.java` and registered in `HttpGateway.java`

**Out of scope this phase:**
- Frontend — test everything with `curl`
- UI presentation of the data

**Success criteria:**
Run the full Layer 2 + Layer 3 smoke tests from PROJECT_GUIDE.md Section 4:
- Place 3 orders across different branches
- `GET /api/reports/branches` shows non-zero revenue for each branch that received an order
- `GET /api/reports/total` shows combined revenue
- `GET /api/reports/customers` shows all 3 orders with correct customer names
- Drain one item below threshold → `GET /api/reports/lowstock` lists it

**Key implementation notes:**
- `OrderRepository.place()` is the most critical method. Order of operations matters:
  1. `checkQty()` — fail fast before any write
  2. `getPrice()` — read price at order time (not cached)
  3. `INSERT INTO orders` — get generated key
  4. `deduct()` — update stock
  5. Compute `low_stock` flag by comparing new qty to threshold
  - All four DB calls use the same shared connection. SQLite autocommit is on.
    If step 4 fails, step 3 has already committed. This is acceptable for a demo system.
    Do not add manual transaction management — it complicates the code without adding
    value for a local-WiFi presentation.
- `DrinkRepository.withStock()` must LEFT JOIN so drinks with no stock row are still
  returned (quantity = 0). Use `COALESCE(s.quantity, 0)`.
- `ReportRepository.branchSales()` must return all 4 branches even if they have zero
  orders — use LEFT JOIN branches → orders, GROUP BY branch.

---

## Phase 4 — Customer UI

**Goal:** A customer on any device connected to the same WiFi can place a full order
from `http://<server-IP>:8080/` without typing anything into a URL bar except the
initial address.

**In scope:**
- `web/order.html` — complete single-file page (HTML + CSS + JS)
- Branch selector → drink grid → quantity stepper → name/phone inputs → submit → receipt
- Live total price calculation
- Inline error on insufficient stock
- Low stock badge on drink cards
- "Place another order" flow

**Out of scope this phase:**
- Admin features
- Order history visible to customers
- Any persistence in the browser (no localStorage)

**Success criteria:**
- Open `http://<IP>:8080/` on a phone on the same WiFi
- Complete a full order — receipt shows correct total
- Immediately check `curl http://<IP>:8080/api/reports/customers` — order appears

**Key implementation notes:**
- All `fetch()` calls use relative paths (`/api/orders` not `http://localhost/api/orders`).
  Absolute URLs with `localhost` will fail when accessed from another device.
- The submit button must be disabled until customer name, branch, and drink are all
  selected. This prevents incomplete POST bodies.
- After a successful order, clear drink selection but keep branch and customer name
  pre-filled — customers at one branch order multiple drinks during the demo.

---

## Phase 5 — Admin Dashboard

**Goal:** The administrator device can show all 4 required report types from the
project brief in a single browser tab, with live data reflecting orders placed
by the three customer devices during the presentation.

**In scope:**
- `web/dashboard.html` — complete single-file page (HTML + CSS + JS)
- 6 navigation sections: Overview, Customer Orders, Branch Sales, Total Revenue, Inventory, Low Stock
- Auto-refresh every 30 s on Overview and Low Stock panels
- Restock modal (HQ → branch transfer)
- Low stock alert banner visible on all panels when items are below threshold

**Out of scope this phase:**
- Order editing or deletion
- Exporting reports to PDF/CSV
- Authentication to access the dashboard

**Success criteria:**
Demonstrate live during a 4-device session:
1. Admin opens `/admin` — Overview shows KES 0 revenue, 0 orders
2. Three customers each place 2+ orders from different branches
3. Admin refreshes (or waits 30 s auto-refresh) — Overview shows updated revenue per branch
4. Admin opens Customer Orders — all orders listed with names, branches, amounts
5. Admin opens Branch Sales — each branch shows its individual revenue
6. Admin opens Total Revenue — grand total matches sum of branch totals
7. Admin opens Low Stock — shows any depleted items
8. Admin clicks Restock, transfers 30 units from HQ to a depleted branch
9. Admin opens Inventory — stock count updated, LOW badge gone

**Key implementation notes:**
- The admin page URL path `/admin` must be mapped to `dashboard.html` in `HttpGateway`'s
  static file handler (the browser requests path `/admin`, not `/dashboard.html`).
- The auto-refresh interval (30 s) is sufficient for a demo — do not use WebSockets
  or long-polling.
- Restock modal should only offer branches 2, 3, 4 (not HQ) to avoid students
  accidentally restocking HQ with itself.
