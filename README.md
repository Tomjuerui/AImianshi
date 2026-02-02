# AI Java 校招模拟面试系统

## 项目简介

一个面向 Java 校招生的 AI 语音模拟面试系统，用户上传简历后，AI 作为面试官进行连续追问、压强盘问，并在结束后给出明确可执行的改进建议。

## 技术栈

- **JDK**: 17
- **框架**: Spring Boot 3.2.2
- **ORM**: MyBatis-Plus 3.5.5
- **数据库**: MySQL 8
- **缓存**: Redis
- **对象存储**: MinIO
- **AI模型**: 千问(Qwen) / DeepSeek
- **安全**: Spring Security + JWT

## 项目结构

```
src/main/java/org/itjuerui/
├── api/                    # API层（Controller）
│   ├── controller/         # 控制器
│   └── dto/                # 请求/响应DTO
├── domain/                 # 领域层
│   ├── user/               # 用户领域
│   ├── resume/             # 简历领域
│   ├── interview/          # 面试领域
│   └── report/             # 报告领域
├── service/                # 服务层
│   └── impl/               # 服务实现
├── infra/                  # 基础设施层
│   ├── repo/               # 数据访问（Mapper）
│   ├── llm/                # LLM客户端
│   ├── storage/             # 对象存储
│   └── cache/              # 缓存
└── common/                 # 通用组件
    ├── config/             # 配置类
    ├── exception/          # 异常处理
    └── dto/                # 通用DTO
```

## 环境要求

### 生产环境
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+（可选）
- MinIO（可选，用于文件存储）

### 开发环境（使用 dev profile）
- JDK 17+
- Maven 3.6+
- 无需 MySQL、Redis 等外部依赖（使用 H2 内存数据库）

## 快速开始

### 方式一：使用开发环境 Profile（推荐，一键启动）

开发环境使用 H2 内存数据库，无需安装 MySQL、Redis 等外部服务，适合快速启动和开发。

```bash
# 使用 dev profile 启动（无需任何外部依赖）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或使用 Maven Wrapper
.\mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**开发环境特性：**
- ✅ 使用 H2 内存数据库，启动时自动建表（无需手动初始化）
- ✅ Redis 可选（未配置时不阻塞启动）
- ✅ LLM 配置可选（未配置时接口返回明确错误，但应用可正常启动）
- ✅ H2 控制台：访问 `http://localhost:8080/h2-console` 查看数据库

**注意：**
- 开发环境数据存储在内存中，重启后数据会丢失
- LLM 相关接口需要配置 API Key 才能使用，否则会返回明确的错误信息
- 如需使用 Redis，可在 `application-dev.properties` 中取消注释 Redis 配置

### 方式二：使用生产环境配置

#### 1. 数据库初始化

```bash
# 执行数据库初始化脚本
mysql -u root -p < src/main/resources/db/schema.sql
```

#### 2. 配置应用

编辑 `src/main/resources/application.properties`，配置：

- 数据库连接信息
- Redis 连接信息（可选）
- LLM API Key（千问或DeepSeek，可选）
- MinIO 配置（如使用）

#### 3. 启动项目

```bash
# 使用 Maven 启动
mvn spring-boot:run

# 或打包后运行
mvn clean package
java -jar target/interview-system-0.0.1-SNAPSHOT.jar
```

### 4. 访问接口

项目启动后，默认端口为 `8080`，可以通过以下接口测试：

- `POST /api/resumes/upload` - 上传简历
- `POST /api/interviews` - 创建面试
- `POST /api/interviews/{sessionId}/start` - 开始面试
- `POST /api/interviews/{sessionId}/turn` - 提交对话
- `GET /api/reports/{sessionId}` - 获取报告

## 核心功能

### 1. 简历管理
- 支持 PDF/文本格式简历上传
- 自动解析简历内容
- 提取技术栈和项目信息

### 2. 面试流程
- 固定面试阶段（项目总览、Java基础、Spring、数据库、中间件、系统设计）
- AI 智能追问机制
- 流式对话输出（SSE）

### 3. 报告生成
- 结构化评分报告
- 分维度评价
- 改进建议清单

## 开发规范

- 代码风格：遵循阿里巴巴 Java 开发规范
- 命名规范：驼峰命名法
- 注释要求：公共方法必须有注释
- 异常处理：使用自定义异常类

## 注意事项

### 开发环境（dev profile）
1. **数据库**：使用 H2 内存数据库，无需配置，启动时自动建表
2. **Redis**：可选，未配置时不阻塞启动
3. **LLM API Key**：可选，未配置时接口返回明确错误，但应用可正常启动
4. **数据持久化**：开发环境数据存储在内存中，重启后数据会丢失

### 生产环境
1. **数据库配置**：确保 MySQL 已启动并创建了数据库
2. **Redis 配置**：可选，确保 Redis 已启动（用于会话状态管理）
3. **LLM API Key**：可选，需要在配置文件中设置有效的 API Key
4. **MinIO**：如果使用文件存储，需要先启动 MinIO 服务

## 后续开发

- [ ] 实现简历解析逻辑
- [ ] 实现面试编排器（状态机）
- [ ] 实现 LLM 客户端（Qwen/DeepSeek）
- [ ] 实现流式对话输出
- [ ] 实现报告生成服务
- [ ] 添加单元测试和集成测试

## 许可证

MIT License
