# 智慧校园 SmartCampus

> 个人全栈练手项目：校园服务一站式平台（校园频道 / 快递代取 / 打印服务 / 跑腿 / 代课 / 二手市场 / 活动报名 / 游戏搭子 / 失物招领 / 站内私信），支持多学校数据隔离与跨校只读。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 2.7.18 · Java 21 · MyBatis-Plus 3.5 · JWT · Redis · Spring Mail |
| 用户端 | uni-app (Vue3 `<script setup>`)，编译到 H5 + 微信小程序 |
| 管理端 | Vue3 + Element Plus + ECharts |
| 数据库 | MySQL 8（外键约束 + 唯一索引 + 复合索引设计） |
| 安全 | JWT 无状态认证 · 邮箱验证码（防刷/锁定/一次性消费）· 角色权限拦截 |

## 功能亮点（面试可深挖的点）

- **邮箱验证码登录**：Redis TTL 5 分钟 + 一次性消费防重放；60 秒重发间隔 + 单邮箱单日上限 + 同 IP 单日上限三层防刷；错误 5 次锁定 30 分钟防暴力破解。邮件服务抽象（SMTP 实现），开发模式 mock 与生产模式配置切换。并修复了“验证码参数为空即跳过校验”的认证漏洞
- **多学校数据隔离**：15 所学校按 `school_id` 隔离，切学校自动刷新各页面数据（uni-app `onShow` 生命周期统一接入）；跨校访客只读（仅校园频道可互动），写操作统一守卫拦截
- **接口性能**：频道帖子列表 N+1 查询优化（21 次 → 4 次批量聚合）；Redis 缓存热点数据（首页/详情计数），缓存异常自动降级查库
- **订单类业务闭环**：发布→接单→完成→评价全链路，状态机与金额计算在服务端强校验
- **工程化**：系统化接口级测试（多校×多轮回归、防刷/锁定/过期边界场景）、git 历史干净、密钥与代码分离

## 快速启动（本地跑起来）

### 前置要求
- JDK 21、MySQL 8、Redis、Node.js 16+

### 步骤

```bash
# 1. 初始化数据库（依次执行）
mysql -uroot -p < sql/init.sql
mysql -uroot -p < sql/full_init.sql
mysql -uroot -p < sql/update_v3_user_features.sql
mysql -uroot -p < sql/update_v4_school_data_seed.sql
mysql -uroot -p < sql/update_v5_email_login.sql

# 2. 改后端数据库/Redis 密码：backend/src/main/resources/application.yml
#    （可选）配置邮箱验证码：复制说明见 application-local.yml 样例，
#    不配置则验证码走开发模式（直接返回给前端，功能不受影响）

# 3. 启动后端（8080）
cd backend
mvn spring-boot:run

# 4. 启动用户端 H5（5173）
cd frontend
npm install
npm run dev:h5
# 浏览器访问 http://localhost:5173

# 5.（可选）管理端（8081）
cd admin && npm install && npm run dev
```

### 邮箱验证码（可选配置）
在 `backend/src/main/resources/application-local.yml` 填入 QQ 邮箱与授权码（见该文件内注释），并把 `email-code.enabled` 改为 `true`。**不配置也能跑**：验证码为开发模式，直接回显在前端。

## 目录结构

```
SmartCampus
├── backend/          # Spring Boot 后端（8080）
│   └── src/main/java/com/campus/smart
│       ├── controller/   # 12+ 业务控制器
│       ├── service/      # 业务层（含邮箱验证码、邮件发送）
│       ├── entity/       # MyBatis-Plus 实体
│       ├── interceptor/  # JWT / 学校上下文拦截器
│       └── config/       # WebMvc（拦截链）等
├── frontend/         # uni-app 用户端（H5:5173 / 小程序）
│   └── src/pages     # 20+ 页面：频道/快递/打印/跑腿/代课/市场/活动/游戏/失物/消息/我的
├── admin/            # Vue3 管理端（8081）
├── sql/              # 建库脚本 + 增量迁移（幂等）
└── docs/             # 启动指南、修复报告
```

## 安全说明

- 所有密钥（邮箱授权码等）通过 `application-local.yml` 注入，该文件不入库
- `application.yml` 中仅含开发环境默认值（本地 MySQL/Redis 密码），生产部署时替换
