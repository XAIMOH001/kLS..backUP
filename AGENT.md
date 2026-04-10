# AGENT.md — Drinks POS v2

Agent instructions for implementing the drinks POS system from scratch.
Read PROJECT_GUIDE.md fully before writing any code.

---

## Current Phase

**Phase 1** — not started

---

## Identity & Constraints

You are implementing a Java HTTP server with SQLite persistence. No build tools (no
Maven, Gradle, or Ant). No web frameworks. No JSON libraries. Every dependency is a
plain `.jar` placed in `lib/`. The classpath is set manually in `compile.sh` /
`compile.bat`.

**Language:** Java 17+  
**Database:** SQLite via `xerial/sqlite-jdbc`  
**HTTP:** `com.sun.net.httpserver.HttpServer` (JDK built-in)  
**Frontend:** Plain HTML + CSS + JS in `web/` — no npm, no bundlers  

---

## What NOT To Do

- **Do not open multiple `Connection` objects.** Use only `Store.connection()`. SQLite
  will return `SQLITE_BUSY` if two connections write concurrently.
- **Do not run PRAGMAs inside `Store.connection()`.** They belong only in `Store.init()`
  using a short-lived, separate `DriverManager.getConnection()`.
- **Do not use `localhost` in API URLs on the frontend.** Use a relative path (`/api/…`)
  so the page works from any device on the WiFi network without hardcoding IPs.
- **Do not add Maven/Gradle.** Students are compiling with plain `javac`. Keep it that way.
- **Do not add a frontend framework.** Vanilla JS with `fetch()` is the full stack here.
- **Do not create a `Connection` inside a Repository method.** Each repository gets the
  shared connection via `Store.connection()` at the start of its method.
- **Do not forget the classpath separator.** It is `:` on Linux/Mac and `;` on Windows.
  The compile and run scripts must have platform-correct versions.

---

## Code Style

- Class names: `PascalCase`. Methods and variables: `camelCase`.
- Repository classes live in `repo/` directory, compiled to `out/repo/` automatically.
  On the classpath, `out/` is the root — Java will find `repo/BranchRepository.class`
  as long as the source uses `// no package declaration` or a matching `package repo;`.
  **Recommendation: use `package repo;` at the top of every file in `repo/`** and import
  with `import repo.*;` in `Router.java`.
- Text blocks (`"""…"""`) for all multi-line SQL strings.
- Every SQL-executing method declares `throws SQLException` — no swallowed exceptions.
- `HttpGateway.route()` is the single try/catch boundary. All exceptions bubble up to it.
- Prefer `LinkedHashMap` for API response maps so key order is stable in JSON output.

---

## File Naming Reference

| File | Purpose |
|------|---------|
| `src/App.java` | Entry point — `main()` wires Store + Gateway |
| `src/HttpGateway.java` | Owns `HttpServer`, registers routes, serves static files |
| `src/Router.java` | One static method per API route |
| `src/Store.java` | Shared `Connection`, schema init, seeding |
| `src/Json.java` | `toJson(Object)` + `parseBody(String)` |
| `src/repo/BranchRepository.java` | Branch queries |
| `src/repo/DrinkRepository.java` | Drink + stock queries |
| `src/repo/StockRepository.java` | Stock checks, deductions, restock, low-stock |
| `src/repo/OrderRepository.java` | Order insertion + full order list |
| `src/repo/ReportRepository.java` | Report aggregation queries |
| `web/order.html` | Customer-facing ordering page |
| `web/dashboard.html` | Admin dashboard with all reports |

---

## Phase 1 — Foundation (Store + compile pipeline)

**Goal:** Project compiles and DB initialises cleanly.

- [ ] Create directory structure: `src/repo/`, `web/`, `lib/`, `out/`
- [ ] Place `sqlite-jdbc.jar`, `slf4j-api.jar`, `slf4j-simple.jar` in `lib/`
- [ ] Write `src/Store.java`
  - [ ] `private static Connection conn` field
  - [ ] `public static synchronized Connection connection()` — returns shared conn,
        opens if null or closed via `DriverManager.getConnection("jdbc:sqlite:drinks.db")`
  - [ ] `public static void init()` — opens its OWN disposable connection, runs:
        - `PRAGMA journal_mode=WAL`
        - `PRAGMA foreign_keys=ON`
        - `CREATE TABLE IF NOT EXISTS` for all 4 tables
        - `INSERT OR IGNORE` seeds for branches, drinks, stock (4×10 = 40 rows)
- [ ] Write `src/App.java`
  - [ ] `Class.forName("org.sqlite.JDBC")` before any DB call
  - [ ] Call `Store.init()`
  - [ ] Print "DB initialised" to stdout then exit (no server yet)
- [ ] Write `compile.sh` and `compile.bat`
  - [ ] `javac -cp "lib/sqlite-jdbc.jar" -d out src/*.java src/repo/*.java`
- [ ] Write `run.sh` and `run.bat`
  - [ ] `java -cp "out:lib/sqlite-jdbc.jar:lib/slf4j-api.jar:lib/slf4j-simple.jar" App`
- [ ] Compile and run — verify:
  - [ ] No compile errors
  - [ ] `drinks.db` created
  - [ ] `sqlite3 drinks.db "SELECT COUNT(*) FROM stock;"` returns `40`

**Phase 1 complete when:** `javac` exits 0 and `drinks.db` contains 40 stock rows.

---

## Phase 2 — HTTP Layer (HttpGateway + Router + Json)

**Goal:** Server starts, `GET /api/branches` returns valid JSON.

- [ ] Write `src/Json.java`
  - [ ] `public static String toJson(Object value)` — handles `null`, `Boolean`,
        `Number`, `String` (with escaping), `Map`, `List` recursively
  - [ ] `public static Map<String,Object> parseBody(String body)` — flat JSON only,
        splits on `,` outside quotes, then `:`, trims quotes from key and value,
        auto-detects int/double/boolean/null
- [ ] Write `src/repo/BranchRepository.java` (`package repo;`)
  - [ ] `public static List<Map<String,Object>> getAll()` — SELECT id,name,city FROM branches
- [ ] Write `src/HttpGateway.java`
  - [ ] Constructor: `HttpServer.create(new InetSocketAddress(port), 0)`
  - [ ] `public void start()` — registers all contexts, calls `server.start()`, prints startup banner
  - [ ] `private void route(HttpExchange ex, Handler h)` — adds CORS headers, calls `h.handle(ex)`,
        serialises result with `Json.toJson()`, writes `Content-Type: application/json`
  - [ ] One context `/api/branches` → `Router.branches`
  - [ ] One context `/` (catch-all) → static file serving from `web/` directory,
        map `/` to `order.html`, `/admin` to `dashboard.html`
  - [ ] Thread pool: `Executors.newFixedThreadPool(8)`
- [ ] Write `src/Router.java`
  - [ ] `public static Object branches(HttpExchange ex)` — calls `BranchRepository.getAll()`
- [ ] Update `App.java` to start the gateway instead of exiting
- [ ] Compile and test:
  - [ ] `curl -s http://localhost:8080/api/branches` → 4-element JSON array
  - [ ] `curl -s http://localhost:8080/` → HTML response (order.html content)

**Phase 2 complete when:** `/api/branches` returns valid JSON with 4 branches.

---

## Phase 3 — All Repositories

**Goal:** Every API endpoint returns correct data.

- [ ] Write `src/repo/DrinkRepository.java`
  - [ ] `withStock(int branchId)` — LEFT JOIN drinks + stock, include `low` boolean field
  - [ ] `getPrice(int drinkId)` — single SELECT
- [ ] Write `src/repo/StockRepository.java`
  - [ ] `checkQty(int branchId, int drinkId)` → int
  - [ ] `getThreshold(int branchId, int drinkId)` → int
  - [ ] `deduct(int branchId, int drinkId, int qty)` — UPDATE stock SET quantity=quantity-?
  - [ ] `transferFromHQ(int branchId, int drinkId, int qty)` → Map with success/error
  - [ ] `getLow()` — WHERE quantity <= threshold, ORDER BY quantity ASC
  - [ ] `getAll()` — full inventory with low boolean
- [ ] Write `src/repo/OrderRepository.java`
  - [ ] `place(name, phone, branchId, drinkId, qty)` → Map
    - [ ] Call `StockRepository.checkQty()` — return error map if insufficient
    - [ ] Call `DrinkRepository.getPrice()`
    - [ ] INSERT INTO orders
    - [ ] Call `StockRepository.deduct()`
    - [ ] Return `{success, order_id, total, low_stock, remaining}`
  - [ ] `all()` — SELECT with JOINs, ORDER BY order_time DESC
- [ ] Write `src/repo/ReportRepository.java`
  - [ ] `customerOrders()` — same as `OrderRepository.all()` but for the report panel
  - [ ] `branchSales()` — GROUP BY branch_id, SUM(total_price), COUNT(id)
  - [ ] `grandTotal()` — SUM(total_price), COUNT(id) across all branches
- [ ] Add all routes to `Router.java`:
  - [ ] `drinks(ex)` — reads `branch_id` from query string
  - [ ] `placeOrder(ex)` — POST only, reads JSON body via `Json.parseBody()`
  - [ ] `restock(ex)` — POST only
  - [ ] `stock(ex)` — full inventory
  - [ ] `reportCustomers(ex)`, `reportBranches(ex)`, `reportTotal(ex)`, `reportLowStock(ex)`
- [ ] Register all routes in `HttpGateway`:
  - [ ] `/api/drinks`, `/api/orders`, `/api/restock`, `/api/stock`
  - [ ] `/api/reports/customers`, `/api/reports/branches`, `/api/reports/total`, `/api/reports/lowstock`
- [ ] Run full API smoke test from PROJECT_GUIDE.md Section 4 Layer 2

**Phase 3 complete when:** All curl tests in PROJECT_GUIDE.md Layer 2 return valid JSON
and placed orders appear in all three report endpoints.

---

## Phase 4 — Customer UI (`web/order.html`)

**Goal:** Customers on other WiFi devices can browse and place orders end-to-end.

- [ ] All API calls use relative URLs (`/api/branches` not `http://localhost/api/branches`)
- [ ] On load: `GET /api/branches` → render 4 branch selector buttons
- [ ] Selecting a branch: `GET /api/drinks?branch_id=N` → render drink cards
  - [ ] Show drink name, brand, price, current stock count
  - [ ] Visually disable / mark drinks with `quantity === 0`
  - [ ] Show "LOW" badge if `low === true` and qty > 0
- [ ] Selecting a drink: show quantity stepper (min 1, max = available stock)
- [ ] Quantity stepper updates total price live
- [ ] Customer name field (required) + phone field (optional)
- [ ] Submit button disabled until name + branch + drink are all selected
- [ ] On submit: POST `/api/orders` with JSON body
  - [ ] On success: show receipt (order id, total, drink, branch)
  - [ ] If `low_stock === true` in response: show "⚠ Low stock" warning on receipt
  - [ ] On error (insufficient stock): show inline error, do not clear selections
- [ ] "Place another order" button resets drink selection (keeps branch + name)
- [ ] Page title and header show "Order — Drinks POS" or similar

**Phase 4 complete when:** A phone on the same WiFi can complete a full order and see
the receipt without touching the server machine.

---

## Phase 5 — Admin Dashboard (`web/dashboard.html`)

**Goal:** Admin can demonstrate all 4 required report types from the project brief.

- [ ] Sidebar navigation with sections: Overview, Customer Orders, Branch Sales, Total Revenue, Inventory, Low Stock
- [ ] **Overview panel** (loads on open):
  - [ ] KPI cards: Grand Total Revenue, Total Orders, Low Stock Alert count
  - [ ] Branch sales cards (one per branch, shows revenue + order count)
  - [ ] Recent orders table (last 10)
  - [ ] Auto-refresh every 30 s
- [ ] **Customer Orders panel** — full table: `#`, customer name, phone, branch, drink, brand, qty, unit, total, timestamp
- [ ] **Branch Sales panel** — one card per branch + detail table with totals row
- [ ] **Total Revenue panel** — large KPI showing grand total, breakdown table with
      percentage bar per branch
- [ ] **Inventory panel** — full stock table with per-branch filter buttons, quantity
      progress bar, OK/LOW badge per row
- [ ] **Low Stock panel** — filtered view of only items at or below threshold; red
      alert count badge; "✅ All levels healthy" message when empty
- [ ] **Restock modal** — triggered by top-right button:
  - [ ] Branch selector (Nakuru, Mombasa, Kisumu only — not HQ)
  - [ ] Drink selector (populated from `GET /api/drinks?branch_id=1` showing HQ stock)
  - [ ] Quantity input
  - [ ] On submit: POST `/api/restock`, alert success/error, refresh current panel
- [ ] All panels use `GET` only (no state mutation except the restock modal)
- [ ] Low stock alert banner at top of every panel if `GET /api/reports/lowstock` returns any items

**Phase 5 complete when:** All 4 report types from the project brief are accessible
from the dashboard, and the admin can demonstrate them live with orders placed by
three customer devices.

---

## Debugging Decision Tree

| Symptom | Check |
|---------|-------|
| `NoClassDefFoundError: org/slf4j/LoggerFactory` | Add `slf4j-api.jar` + `slf4j-simple.jar` to `-cp` |
| `SQLITE_BUSY` | Two connections opened — ensure all repos call `Store.connection()` |
| `ClassNotFoundException: org.sqlite.JDBC` | `Class.forName("org.sqlite.JDBC")` missing in `App.java` |
| Port 8080 in use | `pkill -f App` or pass different port as arg |
| 404 on static files | Run JVM from project root (`cd drinks-pos-v2 && java …`) |
| Other devices get "Connection refused" | Check firewall; `HttpServer` binds to `0.0.0.0` by default |
| Reports show 0 revenue | No orders placed yet — place one, then refresh |
| Restock fails with HQ error | HQ stock depleted — don't order from HQ in demo |

---

## References

- PROJECT_GUIDE.md — full domain knowledge, schema, runtime flow, testing strategy
- ROADMAP.md — phase milestones and scope boundaries
- Java HttpServer javadoc: https://docs.oracle.com/en/java/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html
- SQLite WAL mode: https://www.sqlite.org/wal.html
- sqlite-jdbc releases: https://github.com/xerial/sqlite-jdbc/releases
