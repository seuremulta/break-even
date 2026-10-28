# BreakEven

**Finance and Audit Tracking Platform** for the CCS Student Council.
*Where every claim checks out.*

BreakEven is a Java Swing desktop app that tracks a student council's budget from allocation to liquidation. Officers log expense claims, auditors review them, and the moderator grants clearance and monitors budget trends. Every action is written to a timestamped audit log, and everything is stored in MySQL.

- **Version:** 1.0.0
- **Language / UI:** Java 17, Swing (no UI libraries; charts are drawn with Java2D)
- **Build tool:** Maven
- **Database:** MySQL / MariaDB (XAMPP), accessed through JDBC (`mysql-connector-j` 9.1.0)

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
- **Duplicate receipt warning:** if the Receipt / OR No. is already used on another claim (in any semester; case, spaces and dashes are ignored), the officer sees which claims use it and must confirm. If they go ahead, the claim stays PENDING but carries a system note for the auditor and the event is written to the audit log.
- Only **PENDING** and **FLAGGED** claims can be deleted.
- Only **CLEARED** claims count as "spent"; **FLAGGED** claims do not count toward an event's actuals.

### Other highlights

- **Budget per event:** each semester has a total budget split into event allocations, a contingency reserve, and whatever remains unallocated.
- **Semesters:** starting a new semester closes the previous one. Closed semesters stay viewable but are fully read-only. Switch semesters from the sidebar.
- **Saved data:** everything is stored in MySQL and loaded again the next time the app starts.
- **Receipt vault:** attached receipts (png, jpg, pdf) are copied into a local `receipts/` folder as `<ref>_<filename>`, so they survive if the original is moved or deleted.
- **Liquidation reports:** approved/cleared claims are compiled into one report per event. Reports can be exported as `.txt` or `.csv`; once reviewed by the moderator, they are locked.
- **Variance trends:** utilization, net planning variance, and over/under-shoot per event, with two bar charts.
- **Audit trail:** every submit, review, flag, clearance, export and receipt view is logged with the user's name and role. Each claim's history is visible in its detail view.
- **CSV export** for claims, liquidation reports and the audit log.

---

## Requirements

- **JDK 17 or newer** (the project is compiled for Java 17)
- **Apache Maven 3.8+**
- **XAMPP** (or any MySQL / MariaDB server) running on `localhost:3306`

---

## Setup

### 1. Start MySQL

Open the XAMPP Control Panel and start the **MySQL** service.

### 2. Create the database

Open phpMyAdmin (`http://localhost/phpmyadmin`) or the MySQL console and run [`database/schema.sql`](database/schema.sql) (the same script is shown below).

> **Warning:** the script begins by **dropping** the existing tables. Re-running it erases all data in `breakeven_db`.
> If you used an earlier version of BreakEven, run this script again: the tables changed (see *Database* below).

<details>
<summary>Show the SQL</summary>

```sql
-- BreakEven database schema (v1.1)
-- Works on MySQL 5.7+/8.x and on the MariaDB that ships with XAMPP.
--
-- WARNING: this script DROPS the existing tables. Re-running it erases all data in breakeven_db.
-- The app fills the tables with demo data the first time it starts against an empty database.

-- 1. Create and switch to database
CREATE DATABASE IF NOT EXISTS breakeven_db;
USE breakeven_db;

-- 2. Drop old tables if re-creating (children first because of the foreign keys)
DROP TABLE IF EXISTS claims;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS budgets;
DROP TABLE IF EXISTS semesters;
DROP TABLE IF EXISTS logs;

-- 3. Semesters
CREATE TABLE semesters (
    id        VARCHAR(50)  PRIMARY KEY,
    label     VARCHAR(100) NOT NULL UNIQUE,
    is_closed BOOLEAN      NOT NULL DEFAULT FALSE
);

-- 4. Budgets (one per semester)
CREATE TABLE budgets (
    id           VARCHAR(50)   PRIMARY KEY,
    semester_id  VARCHAR(50)   NOT NULL UNIQUE,
    total_amount DECIMAL(12,2) NOT NULL,
    contingency  DECIMAL(12,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (semester_id) REFERENCES semesters(id)
);

-- 5. Events (an allocation of the semester budget; a name is unique within its semester)
--    seq keeps the order in which events were created.
CREATE TABLE events (
    semester_id VARCHAR(50)   NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    allocated   DECIMAL(12,2) NOT NULL,
    seq         INT           NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (semester_id, name),
    UNIQUE KEY uq_events_seq (seq),
    FOREIGN KEY (semester_id) REFERENCES semesters(id)
);

-- 6. Claims (all fields of Claim.java, plus the semester and event it belongs to)
CREATE TABLE claims (
    ref          VARCHAR(50)   PRIMARY KEY,
    semester_id  VARCHAR(50)   NOT NULL,
    description  VARCHAR(255)  NOT NULL,
    claimant     VARCHAR(100)  NOT NULL,
    amount       DECIMAL(12,2) NOT NULL,
    category     VARCHAR(100),
    `date`       DATE          NOT NULL,
    receipt_no   VARCHAR(100),
    event_name   VARCHAR(100)  NOT NULL,
    status       VARCHAR(20)   NOT NULL,   -- PENDING / APPROVED / FLAGGED / CLEARED
    discrepancy  TEXT,
    notes        TEXT,
    receipt_file VARCHAR(255),
    FOREIGN KEY (semester_id) REFERENCES semesters(id),
    FOREIGN KEY (semester_id, event_name) REFERENCES events(semester_id, name),
    INDEX idx_claims_event_status (semester_id, event_name, status),
    INDEX idx_claims_receipt_no (receipt_no)
);

-- 7. Liquidation reports
CREATE TABLE reports (
    id          VARCHAR(50)   PRIMARY KEY,
    semester_id VARCHAR(50)   NOT NULL,
    event_name  VARCHAR(100)  NOT NULL,
    `count`     INT           NOT NULL,
    total       DECIMAL(12,2) NOT NULL,
    projected   DECIMAL(12,2) NOT NULL,   -- the event's allocation when the report was filed
    filed_by    VARCHAR(100),
    `date`      DATE          NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'FILED',   -- FILED / REVIEWED
    reviewed_by VARCHAR(100),
    FOREIGN KEY (semester_id) REFERENCES semesters(id)
);

-- 8. Audit log
CREATE TABLE logs (
    id          VARCHAR(50)  PRIMARY KEY,
    `timestamp` DATETIME     NOT NULL,
    `user`      VARCHAR(100),
    action      TEXT
);
```

</details>

### 3. Check the connection settings (optional)

By default the app connects to `localhost:3306` as `root` with an empty password (the XAMPP default).
To use different settings, copy `db.properties.example` to `db.properties` in the folder you run the app from and edit it:

```properties
db.url=jdbc:mysql://localhost:3306/breakeven_db?useSSL=false&allowPublicKeyRetrieval=true
db.user=root
db.password=
```

`db.properties` is listed in `.gitignore`, so your real password is never committed.

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
java -jar target/breakeven-1.0.0.jar
```

The package step bundles the MySQL driver and sets the main class, so a single JAR is all you need. Run it from the folder that contains your `db.properties` (if you use one).

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

The first time the app starts against an **empty** database, it fills it with sample data:

- a closed semester (*A.Y. 2025-2026 - 2nd Sem*) with two events, three cleared claims and one reviewed liquidation report;
- an open semester (*A.Y. 2026-2027 - 1st Sem*, total budget PHP 485,000) with three events and a mix of pending, approved, cleared and flagged claims, plus one filed report waiting for review.

That gives each role something to do right away: the Treasurer has a flagged claim to fix, the Auditor has pending claims to review, and the Moderator has an approved claim to clear and a report to review.

After that first run, the app loads whatever is stored in the database. To start over with the demo data, re-run `database/schema.sql`.

---

## Database

- **Loading:** at startup the app reads all semesters, budgets, events, claims, reports and the audit log from MySQL.
- **Saving:** every change (claims, allocations, budget, contingency, reports, audit log) is written as it happens. Starting a new semester, and drawing from contingency, are saved as a single all-or-nothing transaction.
- **If MySQL is not running** (or the tables are from an older version), the app shows a warning and runs with demo data **in memory only**; nothing is saved. Start MySQL (or re-run `database/schema.sql`) and restart the app.
- **Tables:** `semesters`, `budgets`, `events`, `claims`, `reports`, `logs`. Events, claims and reports belong to a semester (`semester_id`), so the same event name can be used in different semesters. Money is stored as `DECIMAL(12,2)`, dates as `DATE`, and audit timestamps as `DATETIME`.

---

## Project structure

All source files are in `src/main/java/breakeven/`.

| Area | Files |
|---|---|
| **Entry point** | `Main` |
| **Window and navigation** | `App`, `LoginPanel`, `Page` (abstract base of every screen) |
| **Pages** | `DashboardPage`, `BudgetPage`, `ReviewPage` (Review Queue / Clearance), `EventHistoryPage`, `LiquidationPage`, `TrendsPage`, `AuditPage` |
| **Dialogs and details** | `ExpenseDialog`, `ClaimDetails` |
| **Domain model** | `Entity` (abstract), `Claim`, `Report`, `LogEntry`, `Event`, `Budget`, `Semester` |
| **Statuses** | `ClaimStatus`, `ReportStatus`, `Badged` (interface for colored status pills) |
| **Users and permissions** | `User` (abstract), `Treasurer`, `Auditor`, `Moderator` |
| **State and rules** | `Store` (in-memory state, budget maths, review workflow, startup loading) |
| **Persistence** | `Database` (all SQL), `DBConnection` (connection settings and reuse) |
| **UI helpers** | `Theme`, `Ui`, `StatusRenderer`, `BarChart`, `PH` (placeholder text field) |
| **Files** | `ReceiptVault`, `Exportable` (CSV export interface) |

Other files: `database/schema.sql`, `db.properties.example`, `pom.xml`, `.gitignore`.

### OOP concepts used

The source is annotated with `OOP [...]` comments. In short:

- **Inheritance:** `Treasurer` / `Auditor` / `Moderator` extend `User`; `Claim` / `Report` / `LogEntry` extend `Entity`; every screen extends `Page`.
- **Polymorphism:** the sidebar menu, login notice, review queue, and permissions come from the current `User` object, with no role-name checks. Status enums carry their own behavior (`canDelete()`, `canEdit()`, ...). CSV export calls `toCsvRow()` on any `Exportable`.
- **Abstraction:** `User`, `Entity` and `Page` are abstract; `Badged` and `Exportable` are interfaces; `Store` asks `Database` to "save this claim" without knowing any SQL.
- **Encapsulation:** private fields with validated setters; claim status changes only through `approve()` / `flag()` / `clear()` / `resubmit()`; closed semesters reject all changes.

---

## Known limitations

- **Passwords** are hard-coded and stored in plain text for demo purposes only. A real login (hashed passwords in a `users` table) is the next step.
- Database calls run on the Swing UI thread. With a local XAMPP database this is not noticeable, but a slow or remote database could make the window pause briefly.
- A claim's audit history is found by searching the log text for its reference number.
- There are no automated tests yet.

---

## Authors

Created for the CCS Student Council finance and audit workflow.
