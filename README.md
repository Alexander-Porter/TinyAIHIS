# TinyHIS - 基于AI的智能医院信息系统

TinyHIS 是一个现代化的医院信息系统（HIS），集成了人工智能辅助诊断、智能分诊、电子病历管理等功能，旨在提高医疗服务效率和质量。

## 核心功能

### 1. 智能患者服务
- **在线预约挂号**：支持按科室、医生、时段预约，实时显示号源余量
- **AI智能分诊**：基于 RAG（检索增强生成）技术，根据患者症状智能推荐就诊科室
- **移动端支持**：响应式设计，支持手机、平板等移动设备
- **电子健康档案**：完整的就诊记录、检查报告、用药历史查询

### 2. 医生工作站
- **电子病历（EMR）**：结构化病历录入，支持模板快速生成
- **AI辅助诊断**：集成医学知识库，提供疾病诊断建议和治疗方案
- **智能处方**：药品知识库支持，自动检查配伍禁忌
- **检查申请**：一键开具检验检查单，实时跟踪结果

### 3. 医学知识库
- **混合检索系统**：结合关键词检索和向量语义检索
- **中文医学文档**：支持医学疾病、症状、治疗方案的结构化存储
- **Lucene全文索引**：快速检索医学文档内容
- **向量化嵌入**：使用深度学习模型进行语义理解

### 4. 排队叫号系统
- **电子排队**：自动生成排队序号，实时显示等待人数
- **多科室支持**：每个科室独立的排队队列
- **大屏显示**：候诊区大屏实时显示叫号信息
- **WebSocket推送**：实时更新排队状态

### 5. 药房管理
- **处方审核**：药师审核电子处方
- **药品库存管理**：实时库存监控，低库存预警
- **发药记录**：完整的发药流程追溯

### 6. 检验科管理
- **检验申请接收**：接收医生开具的检验申请
- **结果录入**：支持文字和图片格式的检验结果
- **报告查询**：患者和医生可在线查看检验报告

### 7. 系统管理
- **用户权限管理**：细粒度的角色权限控制（患者、医生、主任、药师、检验师、管理员）
- **科室管理**：科室信息维护、排班模板管理
- **数据统计**：就诊量、挂号量、收入等多维度统计
- **系统配置**：灵活的系统参数配置

## 技术架构

### 后端技术栈
- **Spring Boot 3.2.0**：核心框架
- **Spring Security**：安全认证
- **MyBatis-Plus 3.5.5**：ORM框架
- **MySQL 8.0**：关系型数据库
- **Redis 7.0**：缓存和消息队列
- **H2 Database**：测试环境数据库
- **Apache Lucene 10.3**：全文检索引擎
- **JWT**：无状态身份认证

### 前端技术栈
- **Vue 3**：渐进式JavaScript框架
- **Vue Router**：路由管理
- **Pinia**：状态管理
- **Vite**：构建工具
- **Sass**：CSS预处理器

### AI/ML技术
- **RAG (Retrieval-Augmented Generation)**：检索增强生成
- **向量数据库**：医学文档向量化存储
- **SiliconFlow API**：大语言模型接口（支持 DeepSeek-V3.2）
- **混合检索**：关键词 + 向量语义检索

### DevOps
- **Docker & Docker Compose**：容器化部署
- **Nginx**：反向代理和负载均衡
- **Maven**：项目构建管理
- **JUnit 5**：单元测试框架

## 快速开始

### 环境要求
- **Java**: JDK 21 或更高版本
- **Node.js**: 16.x 或更高版本
- **Docker**: 20.10 或更高版本
- **Docker Compose**: 2.x 或更高版本

### 本地开发部署

#### 1. 克隆项目
```bash
git clone https://github.com/Alexander-Porter/TinyHIS.git
cd TinyHIS
```

#### 2. 配置环境变量
复制环境变量模板并配置：
```bash
cp .env.example .env
```

编辑 `.env` 文件，配置必要的参数：
```env
# MySQL配置
MYSQL_PASSWORD=your_mysql_password

# JWT密钥
JWT_SECRET=your_jwt_secret_key_at_least_256_bits

# SiliconFlow API KEY（可选，用于AI功能）
SILICONFLOW_API_KEY=your_api_key
```

#### 3. 启动服务
使用 Docker Compose 一键启动所有服务：
```bash
# 给启动脚本添加执行权限
chmod +x up.sh

# 启动服务
./up.sh
```

或者手动启动：
```bash
docker-compose up -d
```

#### 4. 访问系统
- **前端页面**: http://localhost:5173
- **后端API**: http://localhost:8080/api
- **H2控制台** (开发模式): http://localhost:8080/h2-console

### 默认账号

系统预置了以下测试账号（密码均为 `123456`）：

| 角色 | 用户名 | 说明 |
|------|--------|------|
| 管理员 | admin | 系统管理员，拥有所有权限 |
| 医生 | doctor1 | 内科医生 |
| 医生 | doctor2 | 消化内科医生 |
| 主任 | chief1 | 内科主任 |
| 药师 | pharmacy | 药房工作人员 |
| 检验师 | lab | 检验科工作人员 |
| 患者 | 13800138000 | 测试患者（手机号登录） |

### 生产环境部署

#### 1. 准备服务器
确保服务器已安装 Docker 和 Docker Compose。

#### 2. 配置生产环境变量
```bash
cp .env.example .env
# 编辑 .env，设置安全的密码和密钥
nano .env
```

#### 3. 构建并启动
```bash
# 构建镜像
docker-compose build

# 启动服务（后台运行）
docker-compose up -d

# 查看日志
docker-compose logs -f
```

#### 4. 配置域名和SSL（可选）
修改 `docker/nginx.conf` 添加域名配置和SSL证书。

### 运行单元测试

#### 使用 Docker 运行测试
```bash
docker-compose -f docker-compose.test.yml up --abort-on-container-exit
```

#### 本地运行测试
```bash
mvn test -Dspring.profiles.active=test
```

## 项目结构

```
TinyHIS/
├── docker/                      # Docker配置文件
│   ├── init.sql                # 数据库初始化脚本
│   ├── nginx.conf              # Nginx配置
│   └── settings.xml            # Maven配置
├── docs/                        # 项目文档
├── frontend/                    # Vue 3 前端项目
│   ├── src/
│   │   ├── components/         # 可复用组件
│   │   ├── views/              # 页面视图
│   │   ├── router/             # 路由配置
│   │   ├── stores/             # Pinia状态管理
│   │   └── utils/              # 工具函数
│   └── package.json
├── src/
│   ├── main/
│   │   ├── java/com/tinyhis/
│   │   │   ├── ai/             # AI相关功能
│   │   │   ├── config/         # Spring配置类
│   │   │   ├── controller/     # REST API控制器
│   │   │   ├── dto/            # 数据传输对象
│   │   │   ├── entity/         # 实体类
│   │   │   ├── mapper/         # MyBatis Mapper
│   │   │   ├── service/        # 业务逻辑层
│   │   │   ├── task/           # 定时任务
│   │   │   └── websocket/      # WebSocket处理
│   │   └── resources/
│   │       ├── application.yml # 应用配置
│   │       ├── schema.sql      # 数据库表结构
│   │       └── data.sql        # 初始化数据
│   └── test/                   # 单元测试和集成测试
├── medical-knowledge/          # 医学知识库文档
├── vector-index/               # 向量索引存储
├── docker-compose.yml          # Docker Compose配置
├── docker-compose.test.yml     # 测试环境配置
├── pom.xml                     # Maven项目配置
└── README.md                   # 项目说明文档
```

## 核心功能演示

### 1. 患者挂号流程
1. 患者注册/登录系统
2. 选择科室和医生
3. 选择就诊日期和时段
4. 提交挂号申请并支付挂号费
5. 到院后签到并进入排队队列
6. 候诊区大屏显示叫号信息

### 2. AI智能分诊
1. 患者描述症状："发热、咳嗽、流鼻涕"
2. AI系统基于医学知识库检索相关疾病
3. 推荐就诊科室：呼吸内科/内科
4. 显示相关疾病信息和注意事项

### 3. 医生诊疗流程
1. 医生登录工作站
2. 查看待诊患者队列
3. 叫号并开始接诊
4. 填写电子病历（支持模板）
5. AI辅助诊断提供参考建议
6. 开具处方和检查申请
7. 完成诊疗

### 4. 药房发药流程
1. 药师接收电子处方
2. 审核处方合理性
3. 调配药品
4. 患者凭处方取药
5. 确认发药并记录

## API文档

系统提供 RESTful API，主要接口包括：

### 认证相关
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 患者注册
- `POST /api/auth/logout` - 退出登录

### 患者相关
- `GET /api/patient/info` - 获取患者信息
- `GET /api/patient/records` - 查询就诊记录
- `GET /api/patient/reports` - 查询检查报告

### 挂号相关
- `GET /api/departments` - 获取科室列表
- `GET /api/schedules` - 获取排班信息
- `POST /api/registration` - 创建挂号
- `POST /api/registration/{id}/pay` - 支付挂号费
- `POST /api/registration/check-in` - 签到

### AI相关
- `POST /api/triage/ai-stream` - AI智能分诊（SSE流式响应）
- `GET /api/knowledge/search` - 搜索医学知识

### 医生工作站
- `GET /api/doctor/queue` - 获取待诊队列
- `POST /api/doctor/call` - 叫号
- `POST /api/emr` - 保存电子病历
- `POST /api/prescription` - 开具处方
- `POST /api/lab-order` - 开具检查单

## 高级特性

### 1. 并发控制
- 使用 Redis + Lua 脚本实现挂号秒杀功能
- 乐观锁防止号源超卖
- 分布式锁保证数据一致性

### 2. 实时通信
- WebSocket 实现排队叫号实时推送
- Server-Sent Events (SSE) 实现 AI 对话流式响应

### 3. 混合检索
- 关键词检索：基于 Lucene 的全文检索
- 语义检索：基于向量嵌入的语义相似度匹配
- 自适应权重：根据场景动态调整检索策略

### 4. 数据安全
- JWT 无状态认证
- BCrypt 密码加密
- HTTPS 传输加密（生产环境）
- SQL 注入防护

## 性能优化

- **Redis 缓存**：热点数据缓存，减少数据库压力
- **连接池**：HikariCP 高性能数据库连接池
- **异步处理**：耗时操作异步化，提升响应速度
- **索引优化**：数据库索引优化，加快查询速度
- **CDN 加速**：静态资源 CDN 分发（可选）

## 监控和日志

- **日志管理**：Logback 分级日志输出
- **健康检查**：Spring Boot Actuator 健康检查接口
- **性能监控**：可集成 Prometheus + Grafana

## 常见问题

### 1. 如何重置数据库？
```bash
# 停止服务
docker-compose down -v

# 重新启动（会自动初始化数据库）
docker-compose up -d
```

### 2. 如何配置 AI 功能？
需要申请 SiliconFlow API KEY，在 `.env` 文件中配置 `SILICONFLOW_API_KEY`。

### 3. 如何查看日志？
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
```

### 4. 端口冲突怎么办？
修改 `.env` 文件中的端口配置：
```env
SERVER_PORT=8081  # 修改后端端口
VITE_APP_URL=http://localhost:5174  # 修改前端端口
```

## 许可证

本项目仅供学习和研究使用。

## 联系方式

- **项目维护者**: Alexander Porter
- **GitHub**: https://github.com/Alexander-Porter/TinyHIS
- **Email**: your.email@example.com

## 致谢

感谢以下开源项目和服务：
- Spring Boot
- Vue.js
- Apache Lucene
- SiliconFlow
- Docker

---

**注意**: 本系统仅供教学和演示用途，不得用于实际医疗场景。医疗信息系统的开发和部署需要符合相关法律法规和医疗标准。
