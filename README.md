# BreakEven

**Finance and Audit Tracking Platform** for Student Councils 
*Where every claim checks out.*

BreakEven is a Java Swing desktop app that tracks a student council's budget from allocation to liquidation. Officers log expense claims, auditors review them, and the moderator grants clearance and monitors budget trends. Every action is written to a timestamped audit log.

- **Version:** 1.0.0
- **Language / UI:** Java 17, Swing (no UI libraries; charts are drawn with Java2D)
- **Build tool:** Maven
- **Database:** MySQL (via XAMPP), accessed through JDBC (`mysql-connector-j` 9.1.0)

---

## Features

### Roles

| Role | Sidebar pages | What they can do |
|---|---|---|
| **Treasurer** | Dashboard, Budget Allocation, Liquidation | Log expense claims, edit and resubmit flagged claims, delete pending/flagged claims, create/edit/delete event allocations, file liquidation reports |
| **Auditor** | Dashboard, Budget Allocation, Review Queue, Event History, Liquidation, Audit Log | Approve or flag pending claims, view claim and receipt history per event, open receipts |
| **Moderator** | Dashboard, Budget Allocation, Clearance, Liquidation, Variance Trends, Audit Log | Grant clearance on approved claims, set the semester total and contingency, draw from contingency, start a new semester, review liquidation reports, export the audit log |

### Claim lifecycle

```
PENDING --(Auditor approves)--> APPROVED --(Moderator clears)--> CLEARED
   |                                |
   +-----(Auditor flags)------------+--> FLAGGED --(Treasurer edits & resubmits)--> PENDING / FLAGGED
```

- A claim that exceeds its event's remaining allocation is **flagged automatically** on submission, with a system discrepancy note.
- Clearance is refused if the amount exceeds the event's remaining allocation.
- Only **PENDING** and **FLAGGED** claims can be deleted.
- Only **CLEARED** claims count as "spent"; **FLAGGED** claims do not count toward an event's actuals.

### Other highlights

- **Budget per event:** each semester has a total budget split into event allocations, a contingency reserve, and whatever remains unallocated.
- **Semesters:** starting a new semester closes the previous one. Closed semesters stay viewable but are fully read-only. Switch semesters from the sidebar.
- **Receipt vault:** attached receipts (png, jpg, pdf) are copied into a local `receipts/` folder as `<ref>_<filename>`, so they survive if the original is moved or deleted.
- **Liquidation reports:** approved/cleared claims are compiled into one report per event. Reports can be exported as `.txt` or `.csv`; once reviewed by the moderator, they are locked.
- **Variance trends:** utilization, net planning variance, and over/under-shoot per event, with two bar charts.
- **Audit trail:** every submit, review, flag, clearance, export and receipt view is logged with the user's name and role. Each claim's history is visible in its detail view.
- **CSV export** for claims, liquidation reports and the audit log.

---

## Requirements

- **JDK 17 or newer** (the project is compiled for Java 17)
- **Apache Maven 3.8+**
- **XAMPP** (or any MySQL server) running on `localhost:3306`

---

## Setup

### 1. Start MySQL

Open the XAMPP Control Panel and start the **MySQL** service.

### 2. Create the database

Open phpMyAdmin (`http://localhost/phpmyadmin`) or the MySQL console and run [`database/schema.sql`](database/schema.sql) (the same script is shown below).

> **Warning:** the script begins by **dropping** the existing tables. Re-running it erases all data in `breakeven_db`.

```sql
-- 1. Create and switch to database
CREATE DATABASE IF NOT EXISTS breakeven_db;
USE breakeven_db;

-- 2. Drop old tables if re-creating
DROP TABLE IF EXISTS claims;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS budgets;
DROP TABLE IF EXISTS semesters;
DROP TABLE IF EXISTS logs;

-- 3. Semesters Table
CREATE TABLE semesters (
    id VARCHAR(50) PRIMARY KEY,
    label VARCHAR(100),
    is_closed BOOLEAN
);

-- 4. Budgets Table
CREATE TABLE budgets (
    id VARCHAR(50) PRIMARY KEY,
    semester_id VARCHAR(50),
    total_amount DOUBLE,
    contingency DOUBLE
);

-- 5. Events Table
CREATE TABLE events (
    name VARCHAR(100) PRIMARY KEY,
    allocated DOUBLE
);

-- 6. Claims Table (All 12 fields required by Claim.java)
CREATE TABLE claims (
    ref VARCHAR(50) PRIMARY KEY,
    description VARCHAR(255),
    claimant VARCHAR(100),
    amount DOUBLE,
    category VARCHAR(100),
    date VARCHAR(50),
    receipt_no VARCHAR(100),
    event_name VARCHAR(100),
    status VARCHAR(50),
    discrepancy TEXT,
    notes TEXT,
    receipt_file VARCHAR(255)
);

-- 7. Reports Table
CREATE TABLE reports (
    id VARCHAR(50) PRIMARY KEY,
    event_name VARCHAR(100),
    count INT,
    total DOUBLE,
    projected DOUBLE,
    filed_by VARCHAR(100),
    date VARCHAR(50),
    reviewed_by VARCHAR(100)
);

-- 8. Audit Logs Table
CREATE TABLE logs (
    id VARCHAR(50) PRIMARY KEY,
    timestamp VARCHAR(50),
    user VARCHAR(100),
    action TEXT
);
```

### 3. Check the connection settings

The connection is configured in `src/main/java/breakeven/DBConnection.java`:

```java
private static final String URL = "jdbc:mysql://localhost:3306/breakeven_db?useSSL=false&allowPublicKeyRetrieval=true";
private static final String USER = "root";
private static final String PASSWORD = "";   // XAMPP default: empty
```

Change `USER` / `PASSWORD` if your MySQL server uses different credentials.

### 4. Build and run

From the project root:

```bash
mvn clean compile
mvn exec:java
```

`exec:java` launches `breakeven.Main`, which is configured in `pom.xml`.

#### Running the packaged JAR

```bash
mvn clean package
```

The JAR's manifest has no `Main-Class` and the MySQL driver is not bundled, so `java -jar` will not work as-is. Run it with the driver on the classpath instead:

```bash
# Linux / macOS
java -cp target/breakeven-1.0.0.jar:path/to/mysql-connector-j-9.1.0.jar breakeven.Main

# Windows
java -cp target\breakeven-1.0.0.jar;path\to\mysql-connector-j-9.1.0.jar breakeven.Main
```

The driver JAR can be found in your local Maven repository (`~/.m2/repository/com/mysql/mysql-connector-j/9.1.0/`).

---

## Demo accounts

The login screen has quick-fill chips for each role. The credentials are:

| Role | Username | Password |
|---|---|---|
| Treasurer | `treasurer` | `treasurer123` |
| Auditor | `auditor` | `auditor123` |
| Moderator | `moderator` | `moderator123` |

These are demo-only and are hard-coded in `Store.java`.

### Demo data

On every launch, `Store.seed()` loads sample data: a closed semester (*A.Y. 2025-2026 - 2nd Sem*) with two events, three cleared claims and one reviewed report, and an open semester (*A.Y. 2026-2027 - 1st Sem*, total budget PHP 485,000) with three events and a mix of pending, cleared and flagged claims.

---

## Project structure

All source files are in the `breakeven` package.

| Area | Files |
|---|---|
| **Entry point** | `Main` (starts the app); `BreakEven` is a leftover stub and can be ignored |
| **Window and navigation** | `App`, `LoginPanel`, `Page` (abstract base of every screen) |
| **Pages** | `DashboardPage`, `BudgetPage`, `ReviewPage` (Review Queue / Clearance), `EventHistoryPage`, `LiquidationPage`, `TrendsPage`, `AuditPage` |
| **Dialogs and details** | `ExpenseDialog`, `ClaimDetails` |
| **Domain model** | `Entity` (abstract), `Claim`, `Report`, `LogEntry`, `Event`, `Budget`, `Semester` |
| **Statuses** | `ClaimStatus`, `ReportStatus`, `Badged` (interface for colored status pills) |
| **Users and permissions** | `User` (abstract), `Treasurer`, `Auditor`, `Moderator` |
| **State and persistence** | `Store` (in-memory state, budget maths, workflow, MySQL writes), `DBConnection` |
| **UI helpers** | `Theme`, `Ui`, `StatusRenderer`, `BarChart`, `PH` (placeholder text field) |
| **Files** | `ReceiptVault`, `Exportable` (CSV export interface) |

### OOP concepts used

The source is annotated with `OOP [...]` comments. In short:

- **Inheritance:** `Treasurer` / `Auditor` / `Moderator` extend `User`; `Claim` / `Report` / `LogEntry` extend `Entity`; every screen extends `Page`.
- **Polymorphism:** the sidebar menu, login notice, review queue, and permissions come from the current `User` object, with no role-name checks. Status enums carry their own behavior (`canDelete()`, `canEdit()`, ...). CSV export calls `toCsvRow()` on any `Exportable`.
- **Abstraction:** `User`, `Entity` and `Page` are abstract; `Badged` and `Exportable` are interfaces.
- **Encapsulation:** private fields with validated setters; claim status changes only through `approve()` / `flag()` / `clear()` / `resubmit()`; closed semesters reject all changes.

---

## Database notes and known limitations

The database layer is a work in progress. Please be aware of the following:

- **The app does not read from MySQL yet.** All data lives in memory and is re-seeded on each launch. The database currently receives writes only.
- **Some changes are not yet written to the database:** editing or resubmitting a claim, editing an event allocation, setting the total budget or contingency, drawing from contingency, and marking a liquidation report as reviewed.
- **If MySQL is not running,** the app can throw a `NullPointerException` on actions that save data, because `DBConnection.getConnection()` returns `null` and the save methods use the connection before checking it.
- **Schema gaps:**
  - `claims` and `events` have no `semester_id`, and `events` is keyed by name alone, so two semesters with an event of the same name would overwrite each other.
  - `reports` has no status column (status is implied by `reviewed_by`).
- **Log id collisions:** audit log ids (`LOG-0001`, ...) restart on every launch, so after the first run, inserts into `logs` will fail on duplicate keys. The error is printed to the console; the app itself keeps working. Re-run the SQL script to reset the tables.
- **Passwords** are hard-coded and stored in plain text for demo purposes only.

---

## Authors

Created for Student Councils and finance and audit workflow.