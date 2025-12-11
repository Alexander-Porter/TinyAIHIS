-- Initial Data for TinyHIS

-- Departments
INSERT INTO department (dept_id, dept_name, location, description) VALUES
(1, '内科', '门诊楼1层', '内科综合门诊'),
(2, '外科', '门诊楼2层', '外科综合门诊'),
(3, '消化内科', '门诊楼1层', '消化系统疾病诊治'),
(4, '心内科', '门诊楼1层', '心血管疾病诊治'),
(5, '骨科', '门诊楼2层', '骨骼肌肉系统疾病'),
(6, '儿科', '门诊楼3层', '儿童疾病诊治'),
(7, '妇产科', '门诊楼3层', '妇产科疾病诊治'),
(8, '眼科', '门诊楼4层', '眼科疾病诊治'),
(9, '检验科', '医技楼1层', '临床检验'),
(10, '药房', '门诊楼1层', '门诊药房');

-- System Users (password: 123456 - BCrypt encoded would be used in production)
INSERT INTO sys_user (user_id, username, password, real_name, role, dept_id) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'ADMIN', NULL),
(2, 'doctor1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张医生', 'DOCTOR', 1),
(3, 'doctor2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李医生', 'DOCTOR', 3),
(4, 'chief1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王主任', 'CHIEF', 1),
(5, 'pharmacy', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '药房人员', 'PHARMACY', 10),
(6, 'lab', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '检验人员', 'LAB', 9);

-- Drug Dictionary
INSERT INTO drug_dict (drug_id, name, spec, price, stock_quantity, unit, manufacturer) VALUES
(1, '阿莫西林胶囊', '0.5g*24粒', 25.00, 1000, '盒', '华北制药'),
(2, '布洛芬缓释胶囊', '0.3g*20粒', 18.50, 500, '盒', '中美史克'),
(3, '奥美拉唑肠溶胶囊', '20mg*14粒', 35.00, 800, '盒', '阿斯利康'),
(4, '氯雷他定片', '10mg*6片', 22.00, 600, '盒', '扬子江药业'),
(5, '感冒灵颗粒', '10g*9袋', 15.00, 1200, '盒', '三九药业'),
(6, '阿司匹林肠溶片', '100mg*30片', 12.00, 900, '盒', '拜耳'),
(7, '蒙脱石散', '3g*10袋', 28.00, 400, '盒', '博福-益普生'),
(8, '头孢克洛胶囊', '0.25g*12粒', 32.00, 350, '盒', '礼来'),
(9, '复方甘草片', '100片', 8.00, 500, '瓶', '哈药集团'),
(10, '维生素C片', '100mg*100片', 6.50, 1000, '瓶', '东北制药');

-- Sample Patients
INSERT INTO patient_info (patient_id, name, id_card, phone, password, gender, age) VALUES
(1, '测试患者', '110101199001011234', '13800138000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1, 34);

-- Sample Schedules (use future dates to avoid expiry checks in tests)
INSERT INTO schedule (schedule_id, doctor_id, dept_id, schedule_date, shift_type, max_quota, current_count) VALUES
(1, 2, 1, DATEADD('DAY', 1, CURRENT_DATE), 'AM', 30, 0),
(2, 2, 1, DATEADD('DAY', 1, CURRENT_DATE), 'PM', 30, 0),
(3, 3, 3, DATEADD('DAY', 1, CURRENT_DATE), 'AM', 20, 0),
(4, 3, 3, DATEADD('DAY', 1, CURRENT_DATE), 'PM', 20, 0);

-- EMR Templates
INSERT INTO emr_template (tpl_id, dept_id, creator_id, name, content, type) VALUES
(1, 1, 4, '普通感冒模板', '主诉：发热、咳嗽、流涕\n现病史：患者于X天前出现发热，体温最高XX℃，伴有咳嗽、流涕\n查体：咽部充血，扁桃体不大\n诊断：急性上呼吸道感染', 'EMR'),
(2, 3, NULL, '胃炎模板', '主诉：上腹部疼痛X天\n现病史：患者于X天前无明显诱因出现上腹部疼痛，呈XX性\n查体：上腹部压痛\n诊断：慢性胃炎', 'EMR'),
(3, 1, 4, '感冒处方套餐', '阿莫西林胶囊 0.5g tid\n布洛芬缓释胶囊 0.3g bid\n感冒灵颗粒 1袋 tid', 'PRESCRIPTION');
