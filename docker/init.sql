-- TinyHIS Database Initialization Script
-- This script is automatically run when the MySQL container starts

SET NAMES utf8mb4;

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS tinyhis CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE tinyhis;

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

-- Insert default admin user (password: admin123)
INSERT INTO sys_user (username, password, real_name, role, status) VALUES
('admin', '$2b$12$yQGPk2NMixiNrfeT2cK9uOyju6gfhyic9XfbyR7iZkfV3xzpc7B.a', '系统管理员', 'ADMIN', 1);

-- Insert sample departments
INSERT INTO department (dept_name, location, screen_id, description, status) VALUES
('内科', '门诊楼1楼', 'SCREEN_001', '内科诊室', 1),
('外科', '门诊楼2楼', 'SCREEN_002', '外科诊室', 1),
('儿科', '门诊楼1楼', 'SCREEN_003', '儿科诊室', 1),
('妇产科', '门诊楼3楼', 'SCREEN_004', '妇产科诊室', 1),
('骨科', '门诊楼2楼', 'SCREEN_005', '骨科诊室', 1),
('皮肤科', '门诊楼1楼', 'SCREEN_006', '皮肤科诊室', 1),
('眼科', '门诊楼3楼', 'SCREEN_007', '眼科诊室', 1),
('耳鼻喉科', '门诊楼3楼', 'SCREEN_008', '耳鼻喉科诊室', 1),
('口腔科', '门诊楼4楼', 'SCREEN_009', '口腔科诊室', 1),
('神经内科', '门诊楼2楼', 'SCREEN_010', '神经内科诊室', 1),
('心血管内科', '门诊楼2楼', 'SCREEN_011', '心血管内科诊室', 1),
('呼吸内科', '门诊楼1楼', 'SCREEN_012', '呼吸内科诊室', 1),
('消化内科', '门诊楼1楼', 'SCREEN_013', '消化内科诊室', 1),
('检验科', '医技楼1楼', NULL, '检验科', 1),
('药房', '门诊楼1楼', NULL, '门诊药房', 1);

-- Insert consulting rooms
INSERT INTO consulting_room (room_name, room_code, location, description, status) VALUES
('内科1号诊室', 'NK-01', '门诊楼1楼A区', '内科普通诊室', 1),
('内科2号诊室', 'NK-02', '门诊楼1楼A区', '内科专家诊室', 1),
('内科3号诊室', 'NK-03', '门诊楼1楼A区', '内科急诊诊室', 1),
('外科1号诊室', 'WK-01', '门诊楼2楼B区', '外科普通诊室', 1),
('外科2号诊室', 'WK-02', '门诊楼2楼B区', '外科专家诊室', 1),
('儿科1号诊室', 'EK-01', '门诊楼1楼C区', '儿科诊室', 1),
('妇产科1号诊室', 'FK-01', '门诊楼3楼D区', '妇产科诊室', 1),
('急诊1号诊室', 'JZ-01', '急诊楼1楼', '急诊室', 1),
('急诊2号诊室', 'JZ-02', '急诊楼1楼', '急诊室', 1);

-- Insert sample doctors (password: doctor123)
INSERT INTO sys_user (username, password, real_name, role, dept_id, phone, status) VALUES
('zhang_chief', '$2b$12$oZWQgp6WlFlRQ0QxoX5mEOYWMCZYEaicJKNyDkZK9C1It6XYslhsa', '张主任', 'CHIEF', 1, '13800138001', 1),
('li_doctor', '$2b$12$oZWQgp6WlFlRQ0QxoX5mEOYWMCZYEaicJKNyDkZK9C1It6XYslhsa', '李医生', 'DOCTOR', 1, '13800138002', 1),
('wang_chief', '$2b$12$oZWQgp6WlFlRQ0QxoX5mEOYWMCZYEaicJKNyDkZK9C1It6XYslhsa', '王主任', 'CHIEF', 2, '13800138003', 1),
('zhao_doctor', '$2b$12$oZWQgp6WlFlRQ0QxoX5mEOYWMCZYEaicJKNyDkZK9C1It6XYslhsa', '赵医生', 'DOCTOR', 2, '13800138004', 1),
('lab_user', '$2b$12$oZWQgp6WlFlRQ0QxoX5mEOYWMCZYEaicJKNyDkZK9C1It6XYslhsa', '检验员', 'LAB', 14, '13800138005', 1),
('pharmacy_user', '$2b$12$oZWQgp6WlFlRQ0QxoX5mEOYWMCZYEaicJKNyDkZK9C1It6XYslhsa', '药剂师', 'PHARMACY', 15, '13800138006', 1);

-- Insert sample drugs
INSERT INTO drug_dict (name, spec, price, stock_quantity, unit, manufacturer, status) VALUES
('阿莫西林胶囊', '0.5g*24粒', 25.00, 1000, '盒', '华北制药', 1),
('布洛芬缓释片', '0.3g*20片', 18.50, 800, '盒', '中美史克', 1),
('复方甘草片', '100片', 12.00, 500, '瓶', '广州白云山', 1),
('维生素C片', '0.1g*100片', 8.00, 2000, '瓶', '东北制药', 1),
('头孢克肟分散片', '0.1g*12片', 35.00, 600, '盒', '海南先声', 1),
('奥美拉唑肠溶胶囊', '20mg*14粒', 28.00, 400, '盒', '阿斯利康', 1),
('氨氯地平片', '5mg*28片', 45.00, 300, '盒', '辉瑞制药', 1),
('二甲双胍缓释片', '0.5g*30片', 22.00, 500, '盒', '中美合资', 1);

-- Insert sample check items
INSERT INTO check_item (item_name, item_code, price, category, description, status) VALUES
('血常规', 'XCG001', 20.00, '检验', '血液常规检查', 1),
('尿常规', 'NCG001', 15.00, '检验', '尿液常规检查', 1),
('肝功能', 'GGN001', 80.00, '检验', '肝功能全项', 1),
('肾功能', 'SGN001', 60.00, '检验', '肾功能全项', 1),
('心电图', 'XDT001', 30.00, '检查', '常规心电图', 1),
('胸部CT', 'XCT001', 280.00, '影像', '胸部CT平扫', 1),
('腹部B超', 'FBC001', 120.00, '影像', '腹部B超检查', 1),
('核磁共振(MRI)', 'MRI001', 600.00, '影像', '核磁共振检查', 1);

-- Insert sample EMR templates
INSERT INTO emr_template (dept_id, creator_id, name, content, type, status) VALUES
(1, 2, '感冒发热模板', '主诉: 发热、咳嗽3天\n现病史: 患者3天前出现发热、咳嗽症状，体温38.5°C\n既往史: 无特殊\n查体: T38.5°C，咽部充血\n诊断: 急性上呼吸道感染\n处理: 对症治疗', 'EMR', 1),
(1, 2, '高血压复诊模板', '主诉: 血压控制情况复诊\n现病史: 患者长期服用降压药，血压控制平稳\n查体: BP 130/85mmHg\n诊断: 高血压病\n处理: 继续原方案治疗', 'EMR', 1),
(2, 4, '外科术后复查模板', '主诉: 术后复查\n现病史: 术后恢复良好\n查体: 切口愈合良好，无红肿渗出\n处理: 继续观察', 'EMR', 1);

-- Insert sample schedule templates (weekly recurring patterns)
-- 张主任(user_id=2) 内科(dept_id=1): 周一AM/PM, 周三AM, 周五PM, 每天急诊
-- 李医生(user_id=3) 内科(dept_id=1): 周二AM, 周四PM
-- 王主任(user_id=4) 外科(dept_id=2): 周一PM, 周三PM, 周五AM
-- 赵医生(user_id=5) 外科(dept_id=2): 周二PM, 周四AM
INSERT INTO schedule_template (dept_id, doctor_id, room_id, day_of_week, shift_type, max_quota, status) VALUES
(1, 2, 2, 0, 'AM', 30, 1),
(1, 2, 2, 0, 'PM', 25, 1),
(1, 2, 3, 0, 'ER', 50, 1),
(1, 2, 3, 1, 'ER', 50, 1),
(1, 2, 2, 2, 'AM', 30, 1),
(1, 2, 3, 2, 'ER', 50, 1),
(1, 2, 3, 3, 'ER', 50, 1),
(1, 2, 2, 4, 'PM', 25, 1),
(1, 2, 3, 4, 'ER', 50, 1),
(1, 2, 3, 5, 'ER', 50, 1),
(1, 2, 3, 6, 'ER', 50, 1),
(1, 3, 1, 1, 'AM', 20, 1),
(1, 3, 1, 3, 'PM', 20, 1),
(2, 4, 5, 0, 'PM', 30, 1),
(2, 4, 5, 2, 'PM', 30, 1),
(2, 4, 5, 4, 'AM', 25, 1),
(2, 5, 4, 1, 'PM', 25, 1),
(2, 5, 4, 3, 'AM', 25, 1);