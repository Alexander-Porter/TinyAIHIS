# TinyHIS - 轻量级医院信息系统

## 项目设计报告

---

## 目录

1. [需求分析](#1-需求分析)
2. [概要设计](#2-概要设计)
3. [详细设计](#3-详细设计)
4. [部署指南](#4-部署指南)
5. [API文档](#5-api文档)

---

## 1. 需求分析

### 1.1 项目背景

TinyHIS是一个面向中小型医疗机构的轻量级医院信息系统，旨在实现从患者挂号到就诊、检验、取药的完整就医流程数字化管理。

### 1.2 用户角色

| 角色 | 描述 | 主要功能 |
|------|------|----------|
| 患者 | 就医人员 | 注册、登录、挂号、缴费、签到、查看报告 |
| 医生 | 普通坐诊医生 | 叫号、开具病历/处方/检查单 |
| 主任 | 科室主任 | 医生权限 + 管理病历模板 |
| 检验员 | 检验科工作人员 | 查看待检单、上传检验结果 |
| 药剂师 | 药房工作人员 | 发药、管理库存 |
| 管理员 | 系统管理员 | 用户管理、科室管理、排班管理、审计 |

### 1.3 功能需求

#### 1.3.1 患者端功能

```
┌─────────────────────────────────────────────────────────────┐
│                        患者端功能                            │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │
│  │ 注册登录 │  │ AI分诊  │  │ 预约挂号 │  │ 门诊缴费 │       │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘       │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │
│  │ 到院签到 │  │ 就诊记录 │  │ 检查报告 │  │ 费用明细 │       │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘       │
└─────────────────────────────────────────────────────────────┘
```

- **用户注册/登录**: 手机号+密码注册，支持JWT token认证
- **AI智能分诊**: 基于RAG的智能分诊系统，根据症状描述推荐科室
- **预约挂号**: 选择科室→选择医生→选择时段→确认预约
- **门诊缴费**: 支持挂号费、药费、检查费的模拟缴费
- **到院签到**: GPS定位签到，确保患者在医院范围内
- **就诊记录**: 查看历史就诊信息
- **检查报告**: 查看检验结果

#### 1.3.2 医生端功能

```
┌─────────────────────────────────────────────────────────────┐
│                       医生工作站                             │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────┐ │
│  │                    患者队列管理                         │ │
│  │   [叫号] [跳过] [完成就诊]                              │ │
│  └───────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────┐ │
│  │                    电子病历编辑                         │ │
│  │   主诉 | 现病史 | 既往史 | 诊断 | [模板]               │ │
│  └───────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────┐ │
│  │                    处方/检查单                          │ │
│  │   [开具处方] [开具检查单]                               │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

- **叫号系统**: 从队列中叫号，支持WebSocket实时通知叫号大屏
- **电子病历**: 记录患者症状、诊断、治疗方案
- **处方开具**: 选择药品、设置用量用法
- **检查单开具**: 开具检验项目
- **模板管理**: 主任可创建/编辑科室病历模板

#### 1.3.3 检验科功能

- **待检列表**: 查看待检验的检查单
- **结果录入**: 上传检验结果文本和图片

#### 1.3.4 药房功能

- **发药窗口**: 查看待发药处方，确认发药
- **库存管理**: 药品库存查询和管理

#### 1.3.5 管理后台功能

- **仪表盘**: 系统统计数据展示
- **用户管理**: 管理医生、检验员、药剂师账户
- **科室管理**: 维护科室信息，设置科室主任
- **排班管理**: 设置医生排班，**指定每个时段最大挂号数**
- **审计日志**: 系统操作日志查询

#### 1.3.6 叫号大屏

- **实时更新**: WebSocket推送，无需刷新
- **语音播报**: Web Speech API TTS语音通知
- **科室分屏**: 每个科室独立叫号屏

### 1.4 非功能需求

| 需求类型 | 描述 |
|----------|------|
| 性能 | 支持50并发用户，响应时间<2s |
| 并发控制 | **秒杀场景下防止超卖**（乐观锁） |
| 可用性 | 支持Docker容器化部署 |
| 安全性 | JWT认证，密码BCrypt加密 |
| 响应式 | 患者端适配移动设备 |

---

## 2. 概要设计

### 2.1 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                          用户层                                  │
│   ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐ │
│   │  患者APP    │ │ 医生工作站  │ │  管理后台   │ │ 叫号大屏  │ │
│   │(Vue3+Vant)  │ │(Vue3+ElemUI)│ │(Vue3+ElemUI)│ │(WebSocket)│ │
│   └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └─────┬─────┘ │
└──────────┼───────────────┼───────────────┼─────────────┬┴───────┘
           │               │               │             │
           ▼               ▼               ▼             ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Nginx (反向代理)                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Boot 3.x Backend                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                     Controller Layer                      │   │
│  │  AuthCtrl│RegCtrl│DocCtrl│LabCtrl│PharmCtrl│AdminCtrl    │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      Service Layer                        │   │
│  │  AuthSvc│RegSvc│QueueSvc│PaymentSvc│RagTriageSvc│...     │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      Mapper Layer (MyBatis-Plus)          │   │
│  │  UserMapper│PatientMapper│ScheduleMapper│DrugMapper|...  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
       ┌───────────┐   ┌───────────┐   ┌───────────┐
       │  MySQL 8  │   │  Redis 7  │   │SiliconFlow│
       │  (持久化)  │   │  (队列)   │   │  (AI API) │
       └───────────┘   └───────────┘   └───────────┘
```

### 2.2 技术选型

| 层次 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 后端框架 | Spring Boot | 3.2.0 | 主框架 |
| ORM | MyBatis-Plus | 3.5.5 | 数据库访问 |
| 数据库 | MySQL | 8.0 | 生产环境 |
| 数据库 | H2 | - | 开发/测试 |
| 缓存 | Redis | 7.x | 队列管理 |
| 认证 | JWT | 0.12.3 | Token认证 |
| 前端框架 | Vue | 3.x | SPA框架 |
| UI组件 | Element Plus | - | 桌面端UI |
| UI组件 | Vant | - | 移动端UI |
| 构建工具 | Vite | - | 前端构建 |
| AI | SiliconFlow | - | RAG分诊 |

### 2.3 模块划分

```
tinyhis/
├── src/main/java/com/tinyhis/
│   ├── config/          # 配置类
│   ├── controller/      # REST API控制器
│   ├── dto/             # 数据传输对象
│   ├── entity/          # 实体类
│   ├── exception/       # 异常处理
│   ├── mapper/          # MyBatis Mapper
│   ├── service/         # 业务服务
│   ├── ai/              # AI分诊模块
│   └── util/            # 工具类
└── frontend/
    └── src/
        ├── views/
        │   ├── patient/   # 患者端页面
        │   ├── doctor/    # 医生端页面
        │   ├── lab/       # 检验科页面
        │   ├── pharmacy/  # 药房页面
        │   ├── admin/     # 管理后台页面
        │   └── screen/    # 叫号大屏
        ├── stores/        # Pinia状态管理
        ├── router/        # Vue Router
        └── utils/         # 工具函数
```

---

## 3. 详细设计

### 3.1 数据库设计

#### 3.1.1 E-R图

```
                         ┌─────────────┐
                         │  department │
                         │─────────────│
                         │ dept_id PK  │
                         │ dept_name   │
                         │ location    │
                         └──────┬──────┘
                                │1
                                │
                    ┌───────────┼───────────┐
                    │n          │n          │n
            ┌───────┴───────┐   │   ┌───────┴───────┐
            │   sys_user    │   │   │   schedule    │
            │───────────────│   │   │───────────────│
            │ user_id PK    │   │   │ schedule_id PK│
            │ username      │   │   │ doctor_id FK  │
            │ role          │   │   │ schedule_date │
            │ dept_id FK    │   │   │ max_quota     │◄── 最大挂号数
            └───────┬───────┘   │   │ current_count │
                    │1          │   │ version       │◄── 乐观锁
                    │           │   └───────┬───────┘
                    │           │           │1
    ┌───────────────┴───────────┴───────────┤
    │n                                       │n
┌───┴───────────┐                   ┌───────┴───────┐
│ medical_record│                   │ registration  │
│───────────────│                   │───────────────│
│ record_id PK  │◄──────────────────│ reg_id PK     │
│ reg_id FK     │         1        n│ patient_id FK │
│ patient_id FK │                   │ doctor_id FK  │
│ doctor_id FK  │                   │ schedule_id FK│
│ symptom       │                   │ status        │
│ diagnosis     │                   │ fee           │
└───────┬───────┘                   └───────────────┘
        │1                                  │
        │                                   │n
        ├─────────────┐             ┌───────┴───────┐
        │n            │n            │ patient_info  │
┌───────┴───────┐ ┌───┴─────────┐  │───────────────│
│ prescription  │ │  lab_order  │  │ patient_id PK │
│───────────────│ │─────────────│  │ name          │
│ pres_id PK    │ │ order_id PK │  │ phone         │
│ record_id FK  │ │ record_id FK│  │ password      │
│ drug_id FK    │ │ item_name   │  └───────────────┘
│ quantity      │ │ price       │
│ status        │ │ result_text │
└───────┬───────┘ └─────────────┘
        │n
        │1
┌───────┴───────┐
│  drug_dict    │
│───────────────│
│ drug_id PK    │
│ name          │
│ spec          │
│ price         │
│ stock_quantity│
└───────────────┘
```

#### 3.1.2 表结构设计

**schedule 表 - 排班表（支持秒杀防超卖）**

| 字段 | 类型 | 说明 |
|------|------|------|
| schedule_id | BIGINT PK | 排班ID |
| doctor_id | BIGINT FK | 医生ID |
| schedule_date | DATE | 排班日期 |
| shift_type | VARCHAR(10) | 班次(AM/PM) |
| **max_quota** | INT | **最大挂号数量** |
| current_count | INT | 当前已挂号数量 |
| **version** | INT | **乐观锁版本号** |
| status | INT | 状态 |

### 3.2 接口设计

#### 3.2.1 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/patient/register | 患者注册 |
| POST | /api/auth/patient/login | 患者登录 |
| POST | /api/auth/staff/login | 员工登录 |

#### 3.2.2 排班与挂号接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/schedule/departments | 获取科室列表 |
| GET | /api/schedule/doctors/{deptId} | 获取科室医生 |
| GET | /api/schedule/list | 获取排班列表(含max_quota) |
| POST | /api/registration | 创建挂号(秒杀保护) |
| POST | /api/registration/{regId}/pay | 挂号缴费 |
| POST | /api/registration/{regId}/checkin | GPS签到 |

#### 3.2.3 缴费接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/payment/registration/{regId} | 挂号费缴费 |
| POST | /api/payment/prescription/record/{recordId} | 药费缴费 |
| POST | /api/payment/lab/{orderId} | 检查费缴费 |

### 3.3 算法设计

#### 3.3.1 秒杀防超卖算法（乐观锁）

```java
/**
 * 挂号秒杀场景防超卖实现
 * 使用MyBatis-Plus乐观锁机制
 */
@Override
@Transactional
public boolean incrementCount(Long scheduleId) {
    for (int i = 0; i < MAX_RETRY_TIMES; i++) {
        // 1. 读取当前排班信息（含version）
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        
        // 2. 检查号源是否充足
        if (schedule.getCurrentCount() >= schedule.getMaxQuota()) {
            return false; // 已满
        }
        
        // 3. 尝试更新（CAS操作）
        schedule.setCurrentCount(schedule.getCurrentCount() + 1);
        // MyBatis-Plus自动添加 WHERE version = ? 条件
        int rows = scheduleMapper.updateById(schedule);
        
        if (rows > 0) {
            return true; // 成功
        }
        
        // 4. 版本冲突，重试
        log.debug("乐观锁冲突，重试第{}次", i + 1);
    }
    return false; // 重试次数用尽
}
```

**并发场景示例**:
```
时刻T: 号源剩余1个，version=5

线程A读取: current_count=29, version=5
线程B读取: current_count=29, version=5

线程A更新: SET current_count=30, version=6 WHERE version=5 → 成功
线程B更新: SET current_count=30, version=6 WHERE version=5 → 失败(version已变)

线程B重试: 读取发现 current_count=30=max_quota → 返回"号源不足"
```

#### 3.3.2 RAG智能分诊算法

```
输入: 患者症状描述 (如: "咳嗽、发热3天，有黄痰")
                    │
                    ▼
        ┌─────────────────────────┐
        │    知识库检索 (Top-K)    │
        │   加载医学JSON文档       │
        │   TF-IDF相似度匹配      │
        └───────────┬─────────────┘
                    │
                    ▼ 相关文档片段
        ┌─────────────────────────┐
        │    Prompt构建            │
        │   系统角色 + 知识上下文  │
        │   + 用户症状描述         │
        └───────────┬─────────────┘
                    │
                    ▼
        ┌─────────────────────────┐
        │  SiliconFlow API调用     │
        │  deepseek-ai/DeepSeek-V3.2│
        └───────────┬─────────────┘
                    │
                    ▼
        ┌─────────────────────────┐
        │    结果解析              │
        │   推荐科室 + 置信度      │
        │   + 就医建议             │
        └───────────┬─────────────┘
                    │
                    ▼
输出: {department: "呼吸内科", confidence: 0.85, suggestion: "..."}
```

#### 3.3.3 GPS签到算法（Haversine公式）

```java
/**
 * 计算两个经纬度点之间的距离（米）
 */
public static double calculateDistance(double lat1, double lon1, 
                                        double lat2, double lon2) {
    final double R = 6371000; // 地球半径（米）
    
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    
    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
               Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
               Math.sin(dLon/2) * Math.sin(dLon/2);
    
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    
    return R * c; // 返回距离（米）
}

// 签到验证: distance <= 500米 即可签到
```

### 3.4 界面设计

#### 3.4.1 患者端界面（移动端）

```
┌──────────────────────────────────────┐
│ ◀  智能分诊                          │
├──────────────────────────────────────┤
│                                      │
│  请描述您的症状：                     │
│  ┌────────────────────────────────┐  │
│  │ 咳嗽、发热3天，有黄痰，感觉     │  │
│  │ 胸闷...                        │  │
│  └────────────────────────────────┘  │
│                                      │
│       [ 🔍 开始分诊 ]                 │
│                                      │
│  ─────────── 分诊结果 ───────────    │
│                                      │
│  推荐科室: 呼吸内科                   │
│  置信度: 85%                         │
│                                      │
│  💡 建议: 您的症状可能与呼吸道感染    │
│     相关，建议尽快就诊...            │
│                                      │
│       [ 立即挂号 → ]                 │
│                                      │
└──────────────────────────────────────┘
```

#### 3.4.2 医生工作站界面

```
┌─────────────────────────────────────────────────────────────────┐
│  TinyHIS 医生工作站                           张主任 │ 内科 │ 退出 │
├───────────────┬─────────────────────────────────────────────────┤
│  等候队列      │                                                │
│ ─────────────  │            电子病历                            │
│ ▶ 001 张三     │  ─────────────────────────────────────────────  │
│   002 李四     │  患者: 张三  性别: 男  年龄: 45                 │
│   003 王五     │                                                │
│               │  主诉: [咳嗽发热3天，伴黄痰           ]        │
│ [叫号] [跳过]  │                                                │
│               │  现病史: [患者3天前受凉后出现...      ]        │
│ ─────────────  │                                                │
│  已叫号        │  既往史: [无特殊                     ]        │
│ ─────────────  │                                                │
│   000 赵六     │  诊断: [急性支气管炎                 ]        │
│               │                                                │
│ [完成就诊]     │  [使用模板▼] [保存病历]                       │
├───────────────┴─────────────────────────────────────────────────┤
│  处方开具                     │  检查单开具                      │
│ ────────────────────────────  │ ────────────────────────────────  │
│  药品: [阿莫西林▼] 数量: [2]  │  项目: [血常规          ]       │
│  用法: [每日3次，每次1粒  ]   │  价格: ¥25.00                   │
│  [添加药品]                   │  [添加检查]                      │
│                              │                                  │
│  已开药品:                    │  已开检查:                       │
│  • 阿莫西林 x2 ¥50.00        │  • 血常规 ¥25.00                │
│                              │  • 胸片 ¥120.00                  │
│  [提交处方]                   │  [提交检查单]                    │
└──────────────────────────────┴──────────────────────────────────┘
```

#### 3.4.3 叫号大屏界面

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│                    内  科  叫  号  大  屏                        │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                    ┌─────────────────────┐                      │
│                    │                     │                      │
│     正在就诊       │       001           │      请到1号诊室     │
│                    │                     │                      │
│                    └─────────────────────┘                      │
│                                                                 │
│  🔊 请 001 号 张 先生/女士 到 1 号诊室就诊                       │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│     等候队列:  002 → 003 → 004 → 005 → 006                     │
│                                                                 │
│     前方还有 5 人等候                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. 部署指南

### 4.1 Docker部署（推荐）

#### 4.1.1 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 内存 4GB+

#### 4.1.2 快速部署

```bash
# 1. 克隆项目
git clone https://github.com/your-repo/TinyHIS.git
cd TinyHIS

# 2. 配置环境变量（可选）
cp .env.example .env
# 编辑 .env 设置 SILICONFLOW_API_KEY 等

# 3. 构建后端
./mvnw clean package -DskipTests

# 4. 启动服务
docker-compose up -d

# 5. 查看日志
docker-compose logs -f

# 访问地址
# 前端: http://localhost:80
# 后端API: http://localhost:8080
```

#### 4.1.3 服务说明

| 服务 | 端口 | 说明 |
|------|------|------|
| frontend | 80 | Vue前端(Nginx) |
| backend | 8080 | Spring Boot API |
| mysql | 3306 | MySQL数据库 |
| redis | 6379 | Redis缓存 |

#### 4.1.4 数据持久化

```yaml
volumes:
  mysql_data:     # MySQL数据目录
  redis_data:     # Redis数据目录
  backend_logs:   # 后端日志目录
```

### 4.2 手动部署

#### 4.2.1 后端部署

```bash
# 安装JDK 17
# 安装MySQL 8.0
# 安装Redis 7

# 配置数据库
mysql -u root -p < docker/init.sql

# 修改配置
vim src/main/resources/application.yml

# 构建并运行
./mvnw clean package
java -jar target/tinyhis-1.0.0-SNAPSHOT.jar
```

#### 4.2.2 前端部署

```bash
cd frontend

# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
# 部署 dist/ 目录到Nginx
```

### 4.3 配置说明

#### 4.3.1 后端配置 (application.yml)

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tinyhis
    username: root
    password: your_password

# Redis配置
  data:
    redis:
      host: localhost
      port: 6379

# AI配置（SiliconFlow）
  ai:
    openai:
      api-key: ${SILICONFLOW_API_KEY:}
      base-url: https://api.siliconflow.cn

# JWT配置
jwt:
  secret: your_jwt_secret_key
  expiration: 86400000

# 医院GPS位置（用于签到）
hospital:
  latitude: 39.9042
  longitude: 116.4074
  check-in-radius: 500
```

---

## 5. API文档

### 5.1 认证相关

#### 患者注册
```http
POST /api/auth/patient/register
Content-Type: application/json

{
  "name": "张三",
  "phone": "13800138000",
  "password": "123456",
  "idCard": "110101199001011234",
  "gender": 1,
  "age": 30
}
```

#### 患者登录
```http
POST /api/auth/patient/login
Content-Type: application/json

{
  "phone": "13800138000",
  "password": "123456"
}

Response:
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR...",
    "patientId": 1,
    "name": "张三"
  }
}
```

### 5.2 挂号相关

#### 获取排班列表
```http
GET /api/schedule/list?deptId=1&startDate=2024-01-15&endDate=2024-01-21

Response:
{
  "code": 200,
  "data": [
    {
      "scheduleId": 1,
      "doctorId": 2,
      "doctorName": "张主任",
      "deptName": "内科",
      "date": "2024-01-15",
      "shift": "AM",
      "maxQuota": 30,      // 最大挂号数
      "currentCount": 5,   // 已挂号数
      "quotaLeft": 25,     // 剩余号源
      "fee": 50.00
    }
  ]
}
```

#### 创建挂号（带秒杀保护）
```http
POST /api/registration
Authorization: Bearer <token>
Content-Type: application/json

{
  "patientId": 1,
  "scheduleId": 1
}

Response (成功):
{
  "code": 200,
  "data": {
    "regId": 1,
    "queueNumber": 6,
    "fee": 50.00,
    "status": 0
  }
}

Response (号源不足):
{
  "code": 400,
  "message": "预约失败，号源不足"
}
```

### 5.3 缴费相关

#### 挂号费缴费
```http
POST /api/payment/registration/1
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "data": {
    "success": true,
    "message": "挂号费支付成功",
    "totalAmount": 50.00,
    "paymentType": "REGISTRATION"
  }
}
```

#### 药费缴费
```http
POST /api/payment/prescription/record/1
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "data": {
    "success": true,
    "message": "药费支付成功",
    "totalAmount": 75.00,
    "paymentType": "PRESCRIPTION"
  }
}
```

### 5.4 AI分诊

#### 智能分诊
```http
POST /api/triage
Content-Type: application/json

{
  "symptoms": "咳嗽、发热3天，有黄痰，胸闷"
}

Response:
{
  "code": 200,
  "data": {
    "recommendedDept": "呼吸内科",
    "deptId": 12,
    "confidence": 0.85,
    "suggestion": "您的症状可能与呼吸道感染相关，建议尽快就诊，注意保暖多喝水",
    "relatedDiseases": ["急性支气管炎", "肺炎", "上呼吸道感染"]
  }
}
```

---

## 附录

### A. 默认账户

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 系统管理员 |
| 主任 | zhang_chief | doctor123 | 内科主任 |
| 医生 | li_doctor | doctor123 | 内科医生 |
| 检验员 | lab_user | doctor123 | 检验科 |
| 药剂师 | pharmacy_user | doctor123 | 药房 |

### B. 状态码说明

**挂号状态 (registration.status)**:
- 0: 待支付
- 1: 已支付/待签到
- 2: 已签到/等候中
- 3: 就诊中
- 4: 已完成
- 5: 已取消

**处方状态 (prescription.status)**:
- 0: 待支付
- 1: 已支付
- 2: 已发药

**检查单状态 (lab_order.status)**:
- 0: 待支付
- 1: 已支付/待检验
- 2: 已完成

---

*文档版本: 1.0.0*
*最后更新: 2024年*
