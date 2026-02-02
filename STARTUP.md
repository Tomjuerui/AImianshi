# 项目启动指南

## 已完成的工作

### 1. 依赖管理 ✅
- 已更新 `pom.xml`，添加了所有必要的依赖：
  - Spring Boot 3.2.2
  - MyBatis-Plus 3.5.5
  - MySQL Connector
  - Redis
  - JWT (io.jsonwebtoken)
  - WebSocket
  - MinIO
  - PDFBox (简历解析)
  - Reactor (流式响应)
  - FastJSON2
  - Hutool

### 2. 项目结构 ✅
已创建完整的目录结构：
```
org.itjuerui/
├── api/                    # API层
│   ├── controller/         # ResumeController, InterviewController, ReportController
│   └── dto/                # 请求/响应DTO
├── domain/                 # 领域层
│   ├── user/entity/        # User实体
│   ├── resume/entity/      # Resume实体
│   ├── interview/          # InterviewSession, InterviewTurn实体 + 枚举
│   └── report/entity/      # Report实体
├── service/                # 服务层
│   ├── ResumeService
│   ├── InterviewService
│   ├── ReportService
│   └── impl/               # 服务实现类
├── infra/                  # 基础设施层
│   ├── repo/               # MyBatis Mapper接口
│   └── llm/                # LLM客户端接口和实现
└── common/                 # 通用组件
    ├── config/             # MyBatisPlusConfig, WebConfig, SecurityConfig
    ├── exception/          # 全局异常处理
    └── dto/                # ApiResponse统一响应
```

### 3. 配置文件 ✅
- `application.properties` 已配置：
  - 数据库连接
  - Redis连接
  - LLM配置（Qwen/DeepSeek）
  - MinIO配置
  - 文件上传配置
  - 日志配置

### 4. 数据库脚本 ✅
- 已创建 `src/main/resources/db/schema.sql`
- 包含所有表结构：user, resume, interview_session, interview_turn, report

### 5. 基础代码框架 ✅
- 所有实体类已创建（使用 MyBatis-Plus 注解）
- 所有 Mapper 接口已创建
- 所有 Controller 已创建（包含基础路由）
- 所有 Service 接口和实现已创建（包含 TODO 标记）
- 全局异常处理器已配置
- 统一响应格式已实现

## 启动步骤

### 方式一：使用开发环境 Profile（推荐，一键启动）🚀

开发环境使用 H2 内存数据库，无需安装 MySQL、Redis 等外部服务，适合快速启动和开发。

**前置条件：**
- JDK 17+ 已安装
- Maven 3.6+ 已安装

**一键启动：**
```bash
# Windows PowerShell
cd aimian
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或使用 Maven Wrapper
.\mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**开发环境特性：**
- ✅ 使用 H2 内存数据库，启动时自动建表（无需手动初始化）
- ✅ Redis 可选（未配置时不阻塞启动）
- ✅ LLM 配置可选（未配置时接口返回明确错误，但应用可正常启动）
- ✅ H2 控制台：访问 `http://localhost:8080/h2-console` 查看数据库
  - JDBC URL: `jdbc:h2:mem:aimian`
  - 用户名: `sa`
  - 密码: （空）

**注意事项：**
- 开发环境数据存储在内存中，重启后数据会丢失
- LLM 相关接口需要配置 API Key 才能使用，否则会返回明确的错误信息（如：`LLM 服务未配置：请在 application.properties 中设置 llm.provider`）
- 如需使用 Redis，可在 `application-dev.properties` 中取消注释 Redis 配置

---

### 方式二：使用生产环境配置

### 前置条件
1. **MySQL 8.0+** 已安装并运行
2. **Redis 6.0+** 已安装并运行（可选）
3. **JDK 17+** 已安装
4. **Maven 3.6+** 已安装

### 步骤 1: 初始化数据库
```sql
-- 执行数据库初始化脚本
mysql -u root -p < src/main/resources/db/schema.sql
```

或者手动执行 `src/main/resources/db/schema.sql` 中的 SQL 语句。

### 步骤 2: 配置应用
编辑 `src/main/resources/application.properties`，修改以下配置：

```properties
# 数据库配置（必须修改）
spring.datasource.url=jdbc:mysql://localhost:3306/aimian?...
spring.datasource.username=your_username
spring.datasource.password=your_password

# Redis配置（可选，如果Redis有密码）
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=your_redis_password

# LLM配置（可选，未配置时接口返回明确错误，但应用可正常启动）
llm.provider=QWEN  # 或 DEEPSEEK
llm.qwen.api-key=your-qwen-api-key
llm.deepseek.api-key=your-deepseek-api-key

# MinIO配置（可选，如果使用文件存储）
minio.endpoint=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin
```

### 步骤 3: 编译项目
```bash
# Windows PowerShell（如果路径有中文，可能需要使用短路径）
cd aimian
mvn clean compile

# 或者使用 Maven Wrapper
.\mvnw clean compile
```

### 步骤 4: 启动项目
```bash
# 方式1: 使用 Maven
mvn spring-boot:run

# 方式2: 使用 Maven Wrapper
.\mvnw spring-boot:run

# 方式3: 打包后运行
mvn clean package
java -jar target/interview-system-0.0.1-SNAPSHOT.jar
```

### 步骤 5: 验证启动
项目启动后，访问：
- 健康检查：`http://localhost:8080/actuator/health`（如果配置了 actuator）
- API 文档：查看 Controller 中的路由定义

## 测试 API

### 1. 上传简历
```bash
curl -X POST http://localhost:8080/api/resumes/upload \
  -F "file=@your_resume.pdf" \
  -F "userId=1"
```

### 2. 创建面试
```bash
curl -X POST http://localhost:8080/api/interviews \
  -H "Content-Type: application/json" \
  -d '{"resumeId": 1, "durationMinutes": 30}'
```

### 3. 开始面试
```bash
curl -X POST http://localhost:8080/api/interviews/1/start
```

## 常见问题

### 1. 编译失败
- 检查 JDK 版本是否为 17+
- 检查 Maven 版本是否为 3.6+
- 检查网络连接（需要下载依赖）

### 2. 启动失败 - 数据库连接错误
- 检查 MySQL 是否运行
- 检查数据库配置是否正确
- 检查数据库是否已创建

### 3. 启动失败 - Redis 连接错误
- 检查 Redis 是否运行
- 检查 Redis 配置是否正确
- **推荐使用 dev profile**：开发环境可以不配置 Redis，应用可以正常启动
- 如果使用生产环境且不需要 Redis，可以暂时注释相关配置，或使用 `@ConditionalOnProperty` 使 Redis 配置可选

### 4. 路径包含中文导致的问题
- 如果 PowerShell 无法识别中文路径，可以：
  - 使用短路径（8.3格式）
  - 使用 IDE（如 IntelliJ IDEA）直接运行
  - 将项目移动到英文路径

## 下一步开发

项目框架已搭建完成，接下来需要实现：

1. **简历解析服务** (`ResumeServiceImpl`)
   - PDF 文本提取
   - 技术栈识别
   - 项目信息抽取

2. **面试编排器** (`InterviewOrchestrator`)
   - 状态机实现
   - 阶段推进逻辑
   - 追问策略

3. **LLM 客户端实现** (`QwenClient`, `DeepSeekClient`)
   - API 调用封装
   - 流式输出处理
   - 错误重试机制

4. **报告生成服务** (`ReportServiceImpl`)
   - 结构化报告生成
   - JSON Schema 验证
   - PDF/Markdown 导出

5. **前端页面**（可选）
   - 简历上传界面
   - 面试对话界面
   - 报告展示界面

## 注意事项

### 开发环境（dev profile）
- ✅ 使用 H2 内存数据库，无需安装 MySQL
- ✅ Redis 可选，未配置时不阻塞启动
- ✅ LLM 配置可选，未配置时接口返回明确错误，但应用可正常启动
- ⚠️ 数据存储在内存中，重启后数据会丢失

### 生产环境
- 所有 Service 实现类中都有 `TODO` 标记，需要逐步实现
- LLM API Key 需要从对应平台获取（可选，未配置时接口返回明确错误）
- 数据库表结构已设计完成，可以直接使用
- 安全配置暂时禁用了认证，后续需要实现 JWT
