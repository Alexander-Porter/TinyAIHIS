-- TinyHIS Database Initialization Script
-- This script is automatically run when the MySQL container starts

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

-- Doctor Schedule Table (with optimistic locking for flash sale protection)
CREATE TABLE IF NOT EXISTS schedule (
    schedule_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    schedule_date DATE NOT NULL,
    shift_type VARCHAR(10) NOT NULL COMMENT 'AM or PM',
    max_quota INT DEFAULT 30 COMMENT 'Maximum appointments allowed for this shift',
    current_count INT DEFAULT 0 COMMENT 'Current booked appointment count',
    status INT DEFAULT 1,
    version INT DEFAULT 0 COMMENT 'Optimistic lock version for preventing overselling',
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
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTTyU3VxqW6', '系统管理员', 'ADMIN', 1);

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

-- Insert sample doctors (password: doctor123)
INSERT INTO sys_user (username, password, real_name, role, dept_id, phone, status) VALUES
('zhang_chief', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTTyU3VxqW6', '张主任', 'CHIEF', 1, '13800138001', 1),
('li_doctor', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTTyU3VxqW6', '李医生', 'DOCTOR', 1, '13800138002', 1),
('wang_chief', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTTyU3VxqW6', '王主任', 'CHIEF', 2, '13800138003', 1),
('zhao_doctor', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTTyU3VxqW6', '赵医生', 'DOCTOR', 2, '13800138004', 1),
('lab_user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTTyU3VxqW6', '检验员', 'LAB', 14, '13800138005', 1),
('pharmacy_user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTTyU3VxqW6', '药剂师', 'PHARMACY', 15, '13800138006', 1);

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

-- Insert sample schedules for the next 7 days
INSERT INTO schedule (doctor_id, schedule_date, shift_type, max_quota, current_count, status, version) VALUES
(2, CURDATE(), 'AM', 30, 0, 1, 0),
(2, CURDATE(), 'PM', 25, 0, 1, 0),
(3, CURDATE(), 'AM', 20, 0, 1, 0),
(4, CURDATE(), 'PM', 30, 0, 1, 0),
(5, CURDATE(), 'AM', 25, 0, 1, 0),
(2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'AM', 30, 0, 1, 0),
(3, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 'PM', 20, 0, 1, 0),
(4, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'AM', 30, 0, 1, 0),
(5, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'PM', 25, 0, 1, 0);

-- Insert sample EMR templates
INSERT INTO emr_template (dept_id, creator_id, name, content, type, status) VALUES
(1, 2, '感冒发热模板', '主诉: 发热、咳嗽3天\n现病史: 患者3天前出现发热、咳嗽症状，体温38.5°C\n既往史: 无特殊\n查体: T38.5°C，咽部充血\n诊断: 急性上呼吸道感染\n处理: 对症治疗', 'EMR', 1),
(1, 2, '高血压复诊模板', '主诉: 血压控制情况复诊\n现病史: 患者长期服用降压药，血压控制平稳\n查体: BP 130/85mmHg\n诊断: 高血压病\n处理: 继续原方案治疗', 'EMR', 1),
(2, 4, '外科术后复查模板', '主诉: 术后复查\n现病史: 术后恢复良好\n查体: 切口愈合良好，无红肿渗出\n处理: 继续观察', 'EMR', 1);
