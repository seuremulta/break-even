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
