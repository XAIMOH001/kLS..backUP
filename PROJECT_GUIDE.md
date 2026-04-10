# Drinks POS — Project Guide

A distributed Java point-of-sale system for a drinks business with one HQ (Nairobi)
and three branches (Nakuru, Mombasa, Kisumu). Customers order from a browser; the
admin sees live reports. Everything runs on a single Java process over a local WiFi
network — no external services, no build tools beyond `javac`.

---

## Table of Contents

1. How This System Works
2. Project Structure
3. Recommended Libraries
4. Testing Strategy
5. Debugging Decision Tree

---

## 1. How This System Works

### The Core Problem

Multiple devices on the same WiFi need to share state — inventory levels, order history,
revenue totals. The "distributed" part is the network: four physical machines all talking
to one Java process that owns the database. There is no peer-to-peer communication and no
replication. The server is the single source of truth.

```
[Customer Device A]──┐
[Customer Device B]──┼──► [Java HttpServer :8080] ──► [SQLite drinks.db]
[Customer Device C]──┘              │
[Admin Device]───────────────────────┘
```

All four devices are just browsers. The Java process handles every request.

### Runtime Flow

```
STARTUP
  App.java
    └─ Store.init()           ← creates schema + seeds branches/drinks/stock
    └─ HttpGateway.start()    ← binds port 8080, registers all routes

CUSTOMER ORDERS A DRINK
  Browser                  HttpGateway           OrderRepository         StockRepository
  ─────────────────────────────────────────────────────────────────────────────────────
  GET /                 →  serve index.html
  GET /api/branches     →  BranchRepository.getAll()  → [{id,name,city}, ...]
  GET /api/drinks
      ?branch_id=3      →  DrinkRepository.withStock(3) → [{id,name,price,qty,low},...]
  POST /api/orders
  {customer,branch,
   drink,qty}           →  OrderRepository.place()
                              └─ StockRepository.checkQty()  → available qty
                              └─ DrinkRepository.getPrice()  → unit price
                              └─ INSERT INTO orders
                              └─ UPDATE stock SET qty = qty - N
                              └─ return {order_id, total, low_stock, remaining}
  ← {success, total, low_stock}

LOW STOCK SIGNAL
  The response to POST /api/orders always includes:
    "low_stock": true/false
    "remaining": N
  The browser shows a warning on-screen immediately.
  The admin panel polls GET /api/reports/lowstock every 30 s and
  shows a red alert banner listing every item at or below threshold.

ADMIN VIEWS REPORTS
  GET /api/reports/customers  →  ReportRepository.customerOrders()
  GET /api/reports/branches   →  ReportRepository.branchSales()
  GET /api/reports/total      →  ReportRepository.grandTotal()
  GET /api/reports/lowstock   →  StockRepository.getLow()
  POST /api/restock           →  StockRepository.transferFromHQ(branch,drink,qty)

SHUTDOWN
  Ctrl+C — JVM exits, SQLite WAL is flushed, no cleanup needed.
```

### SQLite and Concurrent Access

Java's built-in `HttpServer` dispatches each request on a thread-pool thread. SQLite
in WAL (Write-Ahead Log) mode allows multiple concurrent readers with one writer. The
`Store` class holds **one shared `Connection`** behind a `synchronized` factory method.
This is the simplest safe approach: all reads and writes are serialised through one
connection, WAL mode is set once at startup.

Do not create a new `Connection` per request — SQLite file locking will cause
`SQLITE_BUSY` errors under concurrent load.

### Schema

```
branches  (id INTEGER PK, name TEXT, city TEXT)
drinks    (id INTEGER PK AUTOINCREMENT, name TEXT, brand TEXT, price REAL)
stock     (id, branch_id FK, drink_id FK, quantity INT, threshold INT,
           UNIQUE(branch_id, drink_id))
orders    (id, customer_name TEXT, customer_phone TEXT,
           branch_id FK, drink_id FK,
           quantity INT, unit_price REAL, total_price REAL,
           order_time DATETIME DEFAULT CURRENT_TIMESTAMP)
```

**Seeded at startup (INSERT OR IGNORE):**
- 4 branches (Nairobi HQ id=1, Nakuru id=2, Mombasa id=3, Kisumu id=4)
- 10 drinks
- 50 units of each drink at every branch (threshold = 10)

### The Restock Flow

Branches are stocked through HQ. The restock endpoint deducts from HQ's stock
(`branch_id = 1`) and adds to the target branch. Both updates happen in a single
implicit transaction (SQLite autocommit). If HQ has insufficient stock the request
is rejected before any write occurs.

---

## 2. Project Structure

```
drinks-pos-v2/
├── src/
│   ├── App.java               ← main() — init DB, start server
│   ├── HttpGateway.java       ← HttpServer wiring, route registration, CORS
│   ├── Router.java            ← dispatches path → handler, reads body, writes JSON
│   ├── Store.java             ← single Connection factory, schema init, seeding
│   ├── Json.java              ← map/list → JSON string; JSON body → Map (no deps)
│   ├── repo/
│   │   ├── BranchRepository.java  ← getAll()
│   │   ├── DrinkRepository.java   ← withStock(branchId), getPrice(drinkId)
│   │   ├── StockRepository.java   ← checkQty(), deduct(), transferFromHQ(), getLow(), getAll()
│   │   ├── OrderRepository.java   ← place(…), all()
│   │   └── ReportRepository.java  ← customerOrders(), branchSales(), grandTotal()
├── web/
│   ├── order.html             ← customer ordering UI
│   └── dashboard.html         ← admin dashboard
├── lib/
│   ├── sqlite-jdbc.jar        ← xerial/sqlite-jdbc 3.44.x
│   ├── slf4j-api.jar          ← required by sqlite-jdbc at runtime
│   └── slf4j-simple.jar       ← slf4j binding (silences warnings)
├── compile.sh / compile.bat   ← javac one-liner
└── run.sh / run.bat           ← java one-liner
```

### Module Specs

#### `App.java`
```java
public class App {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");     // register driver
        Store.init();                          // schema + seed
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new HttpGateway(port).start();
    }
}
```
No logic. Wires the three layers together and exits.

#### `HttpGateway.java`
Owns the `com.sun.net.httpserver.HttpServer` instance. Registers one context per
API path and one catch-all context for static files from `web/`. Adds CORS headers.
Delegates all actual request handling to `Router`.

Key method signature:
```java
public class HttpGateway {
    public HttpGateway(int port) throws IOException { … }
    public void start() { … }
    private void route(HttpExchange ex, Handler h) { … }  // try/catch + JSON write
}
```

#### `Router.java`
One static method per route. Reads path and query string, calls the appropriate
Repository method, returns `Object` (Map or List). `HttpGateway` serialises the
return value to JSON and writes the response.

```java
public class Router {
    public static Object branches(HttpExchange ex) throws Exception { … }
    public static Object drinks(HttpExchange ex) throws Exception { … }
    public static Object placeOrder(HttpExchange ex) throws Exception { … }
    public static Object restock(HttpExchange ex) throws Exception { … }
    public static Object reportCustomers(HttpExchange ex) throws Exception { … }
    public static Object reportBranches(HttpExchange ex) throws Exception { … }
    public static Object reportTotal(HttpExchange ex) throws Exception { … }
    public static Object reportLowStock(HttpExchange ex) throws Exception { … }
    public static Object stock(HttpExchange ex) throws Exception { … }
}
```

#### `Store.java`
```java
public class Store {
    private static Connection conn;
    public static synchronized Connection connection() throws SQLException { … }
    public static void init() throws SQLException { … }   // CREATE TABLE IF NOT EXISTS + seeds
}
```
`init()` uses its own `DriverManager.getConnection()` call (not the shared one) to
avoid holding the lock during the multi-statement seed sequence.

#### `repo/BranchRepository.java`
```java
public class BranchRepository {
    public static List<Map<String,Object>> getAll() throws SQLException { … }
}
```

#### `repo/DrinkRepository.java`
```java
public class DrinkRepository {
    public static List<Map<String,Object>> withStock(int branchId) throws SQLException { … }
    public static double getPrice(int drinkId) throws SQLException { … }
}
```
`withStock` LEFT JOINs drinks → stock for the given branch and adds a `"low"` boolean field.

#### `repo/StockRepository.java`
```java
public class StockRepository {
    public static int checkQty(int branchId, int drinkId) throws SQLException { … }
    public static void deduct(int branchId, int drinkId, int qty) throws SQLException { … }
    public static Map<String,Object> transferFromHQ(int branchId, int drinkId, int qty) throws SQLException { … }
    public static List<Map<String,Object>> getLow() throws SQLException { … }
    public static List<Map<String,Object>> getAll() throws SQLException { … }
    public static int getThreshold(int branchId, int drinkId) throws SQLException { … }
}
```

#### `repo/OrderRepository.java`
```java
public class OrderRepository {
    public static Map<String,Object> place(String name, String phone,
                                            int branchId, int drinkId,
                                            int qty) throws SQLException { … }
    public static List<Map<String,Object>> all() throws SQLException { … }
}
```
`place()` contains the full order transaction: check stock → get price → INSERT order →
deduct stock → return result with `low_stock` flag.

#### `repo/ReportRepository.java`
```java
public class ReportRepository {
    public static List<Map<String,Object>> customerOrders() throws SQLException { … }
    public static List<Map<String,Object>> branchSales() throws SQLException { … }
    public static Map<String,Object> grandTotal() throws SQLException { … }
}
```

#### `Json.java`
No external dependencies. Converts `Map<String,Object>` and `List<?>` to a JSON string.
Parses a flat JSON body into `Map<String,Object>` (sufficient for all API inputs in this
project — no nested objects in POST bodies).

---

## 3. Recommended Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| `org.xerial:sqlite-jdbc` | 3.44.1.0 | SQLite driver + native binaries bundled |
| `org.slf4j:slf4j-api` | 1.7.32 | Required at runtime by sqlite-jdbc |
| `org.slf4j:slf4j-simple` | 1.7.32 | Binds slf4j so it doesn't print warnings |
| `com.sun.net.httpserver` | JDK built-in | HTTP server, no extra jar needed |

**Why these and nothing else:**

- `sqlite-jdbc` bundles the native SQLite binary — no system SQLite install needed,
  works identically on Windows, macOS, Linux.
- `slf4j-simple` is the minimal binding. Without it, sqlite-jdbc prints
  `SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder"` noise on startup.
- `com.sun.net.httpserver` is part of every JDK since Java 6. Serving static files and
  JSON from it is enough for a local-WiFi demo. No Jetty, no Spring.
- No JSON library needed — `Json.java` covers everything this project sends and receives.

**Getting the jars (no Maven/Gradle):**

On Ubuntu/Debian:
```bash
apt-get install libxerial-sqlite-jdbc-java libslf4j-java
cp /usr/share/java/sqlite-jdbc.jar lib/
cp /usr/share/java/slf4j-api.jar lib/
cp /usr/share/java/slf4j-simple.jar lib/
```

On any OS — download directly:
```
https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.44.1.0/sqlite-jdbc-3.44.1.0.jar
https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.32/slf4j-api-1.7.32.jar
https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.32/slf4j-simple-1.7.32.jar
```

**Java version:** JDK 17+ required (text blocks `"""…"""` are used throughout). JDK 21
is fine. OpenJDK works identically to Oracle JDK.

---

## 4. Testing Strategy

### Layer 1 — Schema Correctness

Delete `drinks.db`, run `App.main()`, then query with any SQLite browser:
```bash
sqlite3 drinks.db ".tables"
sqlite3 drinks.db "SELECT COUNT(*) FROM stock;"   -- must be 40 (4 branches × 10 drinks)
```

### Layer 2 — API Smoke Tests (curl)

```bash
# Branches
curl -s http://localhost:8080/api/branches | python3 -m json.tool

# Drinks at Kisumu
curl -s "http://localhost:8080/api/drinks?branch_id=4" | python3 -m json.tool

# Place an order
curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customer_name":"Test User","customer_phone":"0700000000","branch_id":3,"drink_id":1,"quantity":2}'

# All reports
curl -s http://localhost:8080/api/reports/customers | python3 -m json.tool
curl -s http://localhost:8080/api/reports/branches  | python3 -m json.tool
curl -s http://localhost:8080/api/reports/total     | python3 -m json.tool
curl -s http://localhost:8080/api/reports/lowstock  | python3 -m json.tool

# Restock Mombasa (branch 3) with 30 units of drink 1 from HQ
curl -s -X POST http://localhost:8080/api/restock \
  -H "Content-Type: application/json" \
  -d '{"branch_id":3,"drink_id":1,"quantity":30}'
```

### Layer 3 — Low Stock Signal Test

Deplete stock at one branch past threshold, then verify the alert appears:
```bash
# Drain stock of drink 6 (Water) at Nakuru (branch 2) — starts at 50, threshold 10
for i in $(seq 1 5); do
  curl -s -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d '{"customer_name":"Tester","branch_id":2,"drink_id":6,"quantity":9}'
done
# Now qty=5, below threshold=10
curl -s http://localhost:8080/api/reports/lowstock | python3 -m json.tool
# Should list Nakuru / Water
```

### Layer 4 — WiFi Multi-Device Test

1. Start server on one laptop. Note its IP (`ip addr` / `ipconfig`).
2. Open `http://<IP>:8080/order.html` on a phone.
3. Place an order from the phone.
4. Refresh `http://<IP>:8080/dashboard.html` on the laptop admin screen.
5. Verify the order appears in Customer Orders report and branch revenue updated.

---

## 5. Debugging Decision Tree

| Symptom | Most Likely Cause | Fix |
|---------|-------------------|-----|
| `NoClassDefFoundError: org/slf4j/LoggerFactory` | `slf4j-api.jar` not on classpath | Add `lib/slf4j-api.jar` and `lib/slf4j-simple.jar` to `-cp` |
| `SQLITE_BUSY: database is locked` | Two `Connection` objects opened simultaneously, or PRAGMA run inside `connection()` | Use one shared connection from `Store.connection()`; run PRAGMAs only in `Store.init()` with a separate disposable connection |
| `ClassNotFoundException: org.sqlite.JDBC` | `sqlite-jdbc.jar` missing or wrong classpath separator | Use `:` on Linux/Mac, `;` on Windows in the `-cp` argument |
| Port 8080 already in use | Previous server instance still running | `pkill -f "App"` or change port: `java … App 8081` |
| Browser shows "Connection refused" from other device | Server bound to `localhost` not `0.0.0.0` | `HttpServer.create(new InetSocketAddress(port), 0)` binds to all interfaces by default — verify firewall is off on the server machine |
| Order returns `{"error":"Insufficient stock…"}` | Customer tried to order more than available | Expected behaviour — check stock with `GET /api/drinks?branch_id=N` first |
| `404` on `GET /` or `/order.html` | `web/` directory not in working directory | Run the JVM from the project root: `cd drinks-pos-v2 && java …` |
| Reports show KES 0 for all branches | No orders placed yet | Place at least one order via the customer UI, then refresh |
| Restock fails with `"HQ has insufficient stock"` | HQ stock also depleted (starts at 50 per drink) | Expected — HQ stock is finite. In demo, avoid ordering from HQ itself to preserve its stock for restocking |
| `Address already in use` on restart | Previous process not yet dead | Wait ~5 s for OS to release the port, or use `SO_REUSEADDR` (already set by `HttpServer.create`) |
