# Web Interface — Reference Implementation

A line-by-line walkthrough of every class in the Web Interface layer.
Files are covered in dependency order — bottom layer first, top layer last.

---

## Layer Overview

```
HTTP Request (browser)
       │
       ▼
PhoneBookController      ← JAX-RS, HTTP only
       │
       ▼
IPhoneBookService        ← business rules live here
       │
       ▼
IPhoneBookRepository     ← DB access only
       │
       ▼
IDatabaseConnection      ← connection abstraction
       │
       ▼
PostgreSQL
```

Each layer only knows about the layer directly below it — through an interface, never a concrete class.

---

## `PhoneRecord.java` — Domain Object

```java
public class PhoneRecord {
    private final String msisdn;
    private final BigDecimal balance;

    public PhoneRecord(String msisdn, BigDecimal balance) { ... }
    public String getMsisdn() { return msisdn; }
    public BigDecimal getBalance() { return balance; }
}
```

| Decision | Explanation |
|---|---|
| `final` fields | `PhoneRecord` is immutable — it represents a snapshot of a DB row at a point in time. Nothing should mutate it after construction. |
| No setters | Immutability enforced. To "update" a record you create a new one — this prevents accidental state mutation. |
| `BigDecimal` for balance | Never use `float` or `double` for money — they have rounding errors. `BigDecimal` is exact. |
| No business logic | This is a pure data carrier. **SRP** — it has one job: hold the data. |

---

## `IDatabaseConnection.java`

```java
public interface IDatabaseConnection {
    Connection getConnection() throws SQLException;
}
```

| Line | Explanation |
|---|---|
| `interface` | This is the abstraction that sits between all repositories and the actual database. **DIP** — nothing above this layer depends on PostgreSQL directly. |
| `Connection getConnection()` | Returns a standard `java.sql.Connection`. The caller decides when to open and close it — giving each repository control over its own connection lifecycle. |
| `throws SQLException` | Honest contract — callers are forced to handle connection failure explicitly. |

---

## `PostgreSQLConnection.java`

```java
public class PostgreSQLConnection implements IDatabaseConnection {

    private static final String URL      = "jdbc:postgresql://localhost:5432/charging_db";
    private static final String USER     = "postgres";
    private static final String PASSWORD = "secret";

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
```

| Line | Explanation |
|---|---|
| `implements IDatabaseConnection` | This is the only class that knows PostgreSQL exists. Everything else sees `IDatabaseConnection`. |
| `static final` constants | Connection config in one place. In production these would come from environment variables or a config file, not hardcoded strings. |
| `DriverManager.getConnection(...)` | Standard JDBC connection. Each call opens a new connection — for production you would replace this with a connection pool (HikariCP is the standard choice for Java). |
| Swappability | To switch from PostgreSQL to MySQL tomorrow, you write `MySQLConnection implements IDatabaseConnection` and change one wiring line. Nothing else touches. |

---

## `IPhoneBookRepository.java`

```java
public interface IPhoneBookRepository {
    void addNumber(String msisdn, BigDecimal initialBalance);
    void deleteNumber(String msisdn);
    void updateBalance(String msisdn, BigDecimal newBalance);
    List<PhoneRecord> getAllNumbers();
    boolean exists(String msisdn);
}
```

| Decision | Explanation |
|---|---|
| Separate from `IBalanceRepository` | **ISP** — the MSC's charging concern and the web admin's CRUD concern are segregated. Neither carries methods it doesn't need. |
| `exists(String msisdn)` | Added here rather than in the service to keep the DB-level check close to the DB layer. The service calls it to enforce business rules without writing raw SQL. |
| Returns `List<PhoneRecord>` not `ResultSet` | The repository translates raw DB results into domain objects. Nothing above this layer ever sees JDBC types. |

---

## `PhoneBookRepository.java`

### Constructor
```java
public PhoneBookRepository(IDatabaseConnection db) {
    this.db = db;
}
```
`IDatabaseConnection` is injected — the repository never calls `new PostgreSQLConnection()`. **DIP** maintained.

---

### `addNumber()`
```java
String sql = "INSERT INTO phone_numbers (msisdn, balance) VALUES (?, ?)";
try (Connection conn = db.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    stmt.setString(1, msisdn);
    stmt.setBigDecimal(2, initialBalance);
    stmt.executeUpdate();
}
```

| Line | Explanation |
|---|---|
| `?` placeholders | Never concatenate user input into SQL strings — that's an SQL injection vulnerability. `PreparedStatement` with `?` placeholders is always correct. |
| `try-with-resources` | `Connection` and `PreparedStatement` both implement `AutoCloseable`. The `try(...)` block guarantees they are closed even if an exception is thrown — no resource leaks. |
| `stmt.setBigDecimal(2, initialBalance)` | Maps the Java `BigDecimal` to a PostgreSQL `NUMERIC` column precisely. |

---

### `getAllNumbers()`
```java
List<PhoneRecord> records = new ArrayList<>();
try (Connection conn = db.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql);
     ResultSet rs = stmt.executeQuery()) {
    while (rs.next()) {
        records.add(new PhoneRecord(
            rs.getString("msisdn"),
            rs.getBigDecimal("balance")
        ));
    }
}
return records;
```

| Line | Explanation |
|---|---|
| `ResultSet` in try-with-resources | `ResultSet` is also `AutoCloseable` — included in the same `try` block to ensure it closes with the connection. |
| `rs.next()` loop | Iterates one row at a time. Each row is immediately mapped to a `PhoneRecord` so the `ResultSet` is never exposed outside this method. |
| `new PhoneRecord(...)` | Repository translates DB rows into domain objects here. The service and controller never see JDBC. |

---

### `exists()`
```java
String sql = "SELECT 1 FROM phone_numbers WHERE msisdn = ?";
try (ResultSet rs = stmt.executeQuery()) {
    return rs.next();
}
```

| Line | Explanation |
|---|---|
| `SELECT 1` | We don't need any column data — just whether a row exists. `SELECT 1` is faster than `SELECT *` because the DB doesn't fetch column values. |
| `return rs.next()` | `rs.next()` returns `true` if at least one row was found, `false` otherwise. Clean and direct. |

---

## `PhoneBookService.java` — Business Rules

### Constructor
```java
public PhoneBookService(IPhoneBookRepository repository) {
    this.repository = repository;
}
```
Receives `IPhoneBookRepository` — never `PhoneBookRepository`. The service doesn't know or care which DB is behind it.

---

### `addNumber()` — where rules live
```java
if (msisdn == null || msisdn.isBlank()) {
    throw new IllegalArgumentException("MSISDN cannot be empty.");
}
if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
    throw new IllegalArgumentException("Initial balance cannot be negative.");
}
if (repository.exists(msisdn)) {
    throw new IllegalStateException("Number already exists: " + msisdn);
}
repository.addNumber(msisdn, initialBalance);
```

| Line | Explanation |
|---|---|
| Validation before DB call | Input is checked before touching the database. Fail fast — no point making a DB round trip for invalid input. |
| `msisdn.isBlank()` | Catches both empty strings and whitespace-only strings. |
| `compareTo(BigDecimal.ZERO) < 0` | Correct way to compare `BigDecimal` — never use `==` or `<` on `BigDecimal` objects. |
| `repository.exists(msisdn)` | Business rule: no duplicate MSISDNs. This logic belongs in the service — the repository just checks; the service decides what to do about it. |
| Exceptions, not return codes | Throwing typed exceptions lets the controller translate them into HTTP status codes cleanly. |

---

### Why the service layer earns its place

Right now the service looks thin. But notice what happens when requirements grow:

- "A number can only be deleted if no active call is in progress" → service checks with the MSC before calling `repository.deleteNumber()`
- "Balance top-ups must be logged to an audit table" → service calls both `repository.updateBalance()` and `auditRepository.log()`
- "MSISDNs must match a country-specific format" → service validates the format before accepting

None of these belong in the controller (HTTP concern) or the repository (DB concern). The service layer is where business complexity lives as it grows.

---

## `PhoneBookController.java` — HTTP Layer

```java
@Path("/phonebook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PhoneBookController {
```

| Annotation | Explanation |
|---|---|
| `@Path("/phonebook")` | All endpoints in this class are under `/phonebook`. JAX-RS reads this at startup to build the routing table. |
| `@Produces(APPLICATION_JSON)` | All responses are JSON. JAX-RS serializes return values automatically. |
| `@Consumes(APPLICATION_JSON)` | All request bodies are expected as JSON. JAX-RS deserializes them automatically. |

---

### `GET /phonebook`
```java
@GET
public Response getAllNumbers() {
    List<PhoneRecord> records = service.getAllNumbers();
    return Response.ok(records).build();
}
```

| Line | Explanation |
|---|---|
| `@GET` | Maps HTTP GET to this method. No path parameter — the base path `/phonebook` returns all records. |
| `Response.ok(records)` | Wraps the list in an HTTP 200 response. JAX-RS serializes `List<PhoneRecord>` to a JSON array. |
| No try-catch | `getAllNumbers()` has no business rule exceptions to handle — a read operation either works or throws a runtime exception (which the server returns as 500). |

---

### `POST /phonebook`
```java
@POST
public Response addNumber(Map<String, String> body) {
    try {
        String msisdn = body.get("msisdn");
        BigDecimal balance = new BigDecimal(body.get("balance"));
        service.addNumber(msisdn, balance);
        return Response.status(Response.Status.CREATED)
                       .entity(Map.of("message", "Number added successfully."))
                       .build();
    } catch (IllegalArgumentException | IllegalStateException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                       .entity(Map.of("error", e.getMessage()))
                       .build();
    }
}
```

| Line | Explanation |
|---|---|
| `Map<String, String> body` | JAX-RS deserializes the JSON request body into a map. Simple and dependency-free for a basic API. |
| `Response.Status.CREATED` | HTTP 201 — semantically correct for a successful resource creation, not just 200. |
| `catch (IllegalArgumentException \| IllegalStateException e)` | The service throws these for bad input or business rule violations. The controller's job is to translate them into the right HTTP status code — 400 Bad Request. |
| Controller never validates | Notice the controller doesn't check if msisdn is blank — that's the service's job. The controller only handles HTTP concerns. |

---

### `DELETE /phonebook/{msisdn}`
```java
@DELETE
@Path("/{msisdn}")
public Response deleteNumber(@PathParam("msisdn") String msisdn) {
```

| Line | Explanation |
|---|---|
| `@Path("/{msisdn}")` | `{msisdn}` is a path variable — e.g. `DELETE /phonebook/01012345678`. |
| `@PathParam("msisdn")` | JAX-RS extracts the path variable and injects it as the method parameter. |
| `Response.Status.NOT_FOUND` | If the service throws `IllegalStateException` (number doesn't exist), the controller returns HTTP 404 — semantically correct. |

---

### `PUT /phonebook/{msisdn}/balance`
```java
@PUT
@Path("/{msisdn}/balance")
public Response updateBalance(@PathParam("msisdn") String msisdn,
                              Map<String, String> body) {
```

| Line | Explanation |
|---|---|
| `/balance` sub-path | The URL makes it explicit that only the balance is being updated, not the whole record. Clean REST design. |
| Two parameters | `@PathParam` for the identifier, request body for the new value — standard REST pattern for partial updates. |

---

## `index.html` — Frontend

### Layout
The UI splits into two columns: a **form panel** on the left for all write operations, and a **data table** on the right for reading. Clicking a table row populates the form — no separate "edit" screen needed.

### Row selection
```javascript
function selectRow(msisdn, balance) {
    selectedMsisdn = msisdn;
    document.getElementById('sel-msisdn').value = msisdn;
    document.getElementById('sel-balance').value = parseFloat(balance).toFixed(2);
    document.querySelectorAll('tbody tr').forEach(r => r.classList.remove('selected'));
    document.getElementById(`row-${msisdn}`).classList.add('selected');
}
```
Clicking a row copies its data into the form fields and highlights it. `selectedMsisdn` is the in-memory reference used by update and delete operations.

### Fetch calls
```javascript
const res = await fetch(API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ msisdn, balance })
});
const data = await res.json();
res.ok ? toast('...') : toast(data.error, false);
```

| Line | Explanation |
|---|---|
| `async/await` | Modern JS for handling asynchronous HTTP calls without callback nesting. |
| `res.ok` | True for HTTP 2xx responses. The controller's correct status codes (201, 400, 404) flow naturally into the frontend's success/error branches. |
| `data.error` | The controller always returns `{ "error": "..." }` for failures — the frontend reads the same key consistently. |
| `loadNumbers()` after mutations | Every write operation reloads the table — the UI always reflects the true DB state after any change. |

### Toast notifications
```javascript
function toast(msg, ok = true) {
    const el = document.getElementById('toast');
    el.textContent = msg;
    el.className = ok ? 'ok' : 'err';
    el.style.display = 'block';
    setTimeout(() => el.style.display = 'none', 3000);
}
```
Single function handles both success and error feedback. Disappears after 3 seconds automatically. The `ok` parameter defaults to `true` — callers only pass `false` explicitly for errors.

---

## Full Dependency Chain

```
index.html (fetch)
     │  HTTP JSON
     ▼
PhoneBookController        → HTTP only, no business logic
     │  Java method call
     ▼
IPhoneBookService          → business rules, no HTTP, no SQL
     │  Java method call
     ▼
IPhoneBookRepository       → SQL only, no business rules
     │  Java method call
     ▼
IDatabaseConnection        → connection only
     │  JDBC
     ▼
PostgreSQL
```

Every `→` points to an **interface**, never a concrete class.
Every layer is independently testable by replacing the layer below it with a mock.
