-- TinyHIS Database Schema (Synced with docker/init.sql)

-- System User Table (Doctor, Chief, Admin, Pharmacy, Lab)
CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    role VARCHAR(20) NOT NULL COMMENT 'DOCTOR, CHIEF, ADMIN, PHARMACY, LAB',
    dept_id BIGINT,
    phone VARCHAR(20),
    status INT DEFAULT 1 COMMENT '0-disabled, 1-enabled',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Patient Information Table
CREATE TABLE IF NOT EXISTS patient_info (
    patient_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    id_card VARCHAR(18),
    phone VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    gender INT COMMENT '0-female, 1-male',
    age INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Department Table
CREATE TABLE IF NOT EXISTS department (
    dept_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(100) NOT NULL,
    location VARCHAR(255),
    screen_id VARCHAR(50) COMMENT 'Associated screen device ID',
    description TEXT,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Consulting Room Table (诊室表)
CREATE TABLE IF NOT EXISTS consulting_room (
    room_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_name VARCHAR(100) NOT NULL COMMENT 'e.g. 1号诊室',
    dept_ids VARCHAR(255) COMMENT 'JSON array of allowed department IDs',
    room_code VARCHAR(50) COMMENT 'Room code for display',
    location VARCHAR(255) COMMENT 'e.g. 门诊楼1楼A区',
    description TEXT,
    status INT DEFAULT 1 COMMENT '0-disabled, 1-enabled',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Drug Dictionary Table
CREATE TABLE IF NOT EXISTS drug_dict (
    drug_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    spec VARCHAR(100) COMMENT 'Specification',
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INT DEFAULT 0,
    unit VARCHAR(20),
    manufacturer VARCHAR(200),
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Check Item Table (检查项目)
CREATE TABLE IF NOT EXISTS check_item (
    item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_name VARCHAR(200) NOT NULL,
    item_code VARCHAR(50),
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100),
    description TEXT,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Doctor Schedule Table (with optimistic locking for flash sale protection)
CREATE TABLE IF NOT EXISTS schedule (
    schedule_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL COMMENT 'Department ID',
    room_id BIGINT COMMENT 'Consulting room ID',
    schedule_date DATE NOT NULL,
    shift_type VARCHAR(10) NOT NULL COMMENT 'AM, PM or ER',
    max_quota INT DEFAULT 30 COMMENT 'Maximum appointments allowed for this shift',
    current_count INT DEFAULT 0 COMMENT 'Current booked appointment count',
    status INT DEFAULT 1,
    version INT DEFAULT 0 COMMENT 'Optimistic lock version for preventing overselling',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Schedule Template Table (weekly recurring patterns)
CREATE TABLE IF NOT EXISTS schedule_template (
    template_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_id BIGINT NOT NULL COMMENT 'Department ID',
    doctor_id BIGINT NOT NULL COMMENT 'Doctor ID',
    room_id BIGINT COMMENT 'Default consulting room ID',
    day_of_week INT NOT NULL COMMENT '0=Monday, 1=Tuesday, ..., 6=Sunday',
    shift_type VARCHAR(10) NOT NULL COMMENT 'AM, PM or ER',
    max_quota INT DEFAULT 30 COMMENT 'Maximum appointments per shift',
    status INT DEFAULT 1 COMMENT '1=active, 0=inactive',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Registration Table
CREATE TABLE IF NOT EXISTS registration (
    reg_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    status INT DEFAULT 0 COMMENT '0-pending payment, 1-paid/waiting check-in, 2-checked in/waiting, 3-in consultation, 4-completed, 5-cancelled',
    queue_number INT,
    fee DECIMAL(10,2),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Medical Record Table
CREATE TABLE IF NOT EXISTS medical_record (
    record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reg_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    symptom TEXT COMMENT 'Chief complaint',
    diagnosis TEXT,
    content TEXT COMMENT 'Medical history details',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Prescription Table
CREATE TABLE IF NOT EXISTS prescription (
    pres_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL,
    drug_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    usage_instruction TEXT,
    status INT DEFAULT 0 COMMENT '0-pending payment, 1-paid, 2-dispensed',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Lab Order Table
CREATE TABLE IF NOT EXISTS lab_order (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    price DECIMAL(10,2),
    status INT DEFAULT 0 COMMENT '0-pending payment, 1-paid/pending, 2-completed',
    result_text TEXT,
    result_images TEXT COMMENT 'JSON array of image URLs',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- EMR Template Table
CREATE TABLE IF NOT EXISTS emr_template (
    tpl_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_id BIGINT COMMENT 'NULL for hospital-wide template',
    creator_id BIGINT,
    name VARCHAR(200) NOT NULL,
    content TEXT,
    type VARCHAR(20) COMMENT 'EMR or PRESCRIPTION',
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
