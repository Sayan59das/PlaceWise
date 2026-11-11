-- PlaceWise schema. The same file runs on MySQL 8 and on H2 in MySQL mode (used by the tests).
-- Every statement ends with a semicolon at the end of a line; SchemaInitializer splits on that.

CREATE TABLE IF NOT EXISTS students (
    roll_no         VARCHAR(30)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    branch          VARCHAR(20)  NOT NULL,
    semester        INT          NOT NULL,
    cgpa            DECIMAL(4,2) NOT NULL,
    active_backlogs INT          NOT NULL,
    email           VARCHAR(120) NULL,
    PRIMARY KEY (roll_no),
    -- Serves "students of one branch ordered/filtered by CGPA" without scanning the table.
    INDEX idx_students_branch_cgpa (branch, cgpa),
    CONSTRAINT chk_students_cgpa     CHECK (cgpa >= 0 AND cgpa <= 10),
    CONSTRAINT chk_students_semester CHECK (semester >= 1 AND semester <= 8),
    CONSTRAINT chk_students_backlogs CHECK (active_backlogs >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS companies (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100)  NOT NULL,
    min_cgpa     DECIMAL(4,2)  NOT NULL,
    max_backlogs INT           NOT NULL,
    package_lpa  DECIMAL(8,2)  NOT NULL,
    seats        INT           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_companies_name UNIQUE (name),
    CONSTRAINT chk_companies_cgpa  CHECK (min_cgpa >= 0 AND min_cgpa <= 10),
    CONSTRAINT chk_companies_seats CHECK (seats > 0),
    CONSTRAINT chk_companies_backlogs CHECK (max_backlogs >= 0)
) ENGINE=InnoDB;

-- One row per allowed branch; a company with no rows here accepts every branch.
CREATE TABLE IF NOT EXISTS company_branches (
    company_id BIGINT      NOT NULL,
    branch     VARCHAR(20) NOT NULL,
    PRIMARY KEY (company_id, branch),
    CONSTRAINT fk_company_branches_company FOREIGN KEY (company_id)
        REFERENCES companies (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS applications (
    roll_no    VARCHAR(30) NOT NULL,
    company_id BIGINT      NOT NULL,
    status     VARCHAR(12) NOT NULL,
    PRIMARY KEY (roll_no, company_id),
    -- Serves "how many SELECTED / WAITLISTED applications does a company have".
    INDEX idx_applications_company_status (company_id, status),
    CONSTRAINT fk_applications_student FOREIGN KEY (roll_no)
        REFERENCES students (roll_no) ON DELETE CASCADE,
    CONSTRAINT fk_applications_company FOREIGN KEY (company_id)
        REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT chk_applications_status CHECK
        (status IN ('APPLIED', 'SELECTED', 'REJECTED', 'WAITLISTED', 'WITHDRAWN'))
) ENGINE=InnoDB;
