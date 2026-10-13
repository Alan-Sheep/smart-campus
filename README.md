<div align="center">

# 🎓 SmartCampus · 智慧校园

**基于 Spring Boot + uni-app 的校园服务一站式平台**

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5-blue)
![Redis](https://img.shields.io/badge/Redis-6-red)
![Vue](https://img.shields.io/badge/Vue-3.x-42b883)
![uni-app](https://img.shields.io/badge/uni--app-H5%20%7C%20微信小程序-2b9939)
![MySQL](https://img.shields.io/badge/MySQL-8-4479a1)
![License](https://img.shields.io/badge/License-MIT-yellow)

[![Backend](https://img.shields.io/badge/后端-147%20Java文件-59666c?style=flat-square&logo=spring)](backend/)
[![Frontend](https://img.shields.io/badge/用户端-40%20Vue组件-4FC08D?style=flat-square&logo=vuedotjs)](frontend/)
[![API](https://img.shields.io/badge/接口-26%20控制器-009688?style=flat-square)](backend/src/main/java/com/campus/smart/controller)
[![DB](https://img.shields.io/badge/数据库-23%20张表-336791?style=flat-square&logo=mysql)](sql/)
[![Lines](https://img.shields.io/badge/代码-8.0万%2B%20行-important?style=flat-square)](#)

*独立完成的前后端全栈项目 · 从需求分析、数据库设计到接口测试全流程*

</div>

---

## 📖 项目简介

SmartCampus 是一个面向高校场景的校园服务平台，覆盖 **10 大业务模块**，支持 **多学校数据隔离**（内置 15 所重庆高校演示数据）。项目完整实现了从注册登录、内容发布、订单撮合到站内消息的闭环业务链路。

### 🖼️ 业务全景

```
┌─────────────────────────────────────────────────────────┐
│                      用户端 (uni-app)                     │
│        H5 · 微信小程序 · 多学校切换 · 跨校访客只读          │
├──────────┬──────────┬──────────┬──────────┬─────────────┤
│ 📢 校园频道│ 📦 快递代取│ 🖨️ 打印服务│ 🏃 校园跑腿│ 📚 代课服务  │
│ 发帖·评论 │ 发布/接单 │ 文件上传  │ 多类型单   │ 发布/接单   │
│ 点赞·收藏 │ 状态机    │ 价格配置  │ 接单闭环   │ 报酬结算    │
├──────────┼──────────┼──────────┼──────────┼─────────────┤
│ 🛒 二手市场│ 🎉 校园活动│ 🎮 游戏搭子│ 🔍 失物招领│ ✉️ 站内消息  │
│ 分类/收藏 │ 报名/取消 │ 组队筛选  │ 招领/寻物  │ 私信·互动   │
├──────────┴──────────┴──────────┴──────────┴─────────────┤
│         🔐 邮箱验证码登录 · JWT 认证 · 学校上下文           │
├─────────────────────────────────────────────────────────┤
│                  管理端 (Vue3 + Element Plus)             │
│      内容审核 · 订单管理 · 用户管理 · 数据看板(ECharts)      │
└─────────────────────────────────────────────────────────┘
```

---

## 📸 项目效果图

### 用户端（uni-app · H5 / 微信小程序）

<div align="center">
<table>
  <tr>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/index.png" width="200"/><br/><b>🏠 首页</b></td>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/login.png" width="200"/><br/><b>🔐 邮箱验证码登录</b></td>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/channel.png" width="200"/><br/><b>📢 校园频道</b></td>
  </tr>
  <tr>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/goods.png" width="200"/><br/><b>🛒 二手市场</b></td>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/express.png" width="200"/><br/><b>📦 快递代取</b></td>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/activity.png" width="200"/><br/><b>🎉 校园活动</b></td>
  </tr>
  <tr>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/game.png" width="200"/><br/><b>🎮 游戏搭子</b></td>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/lostfound.png" width="200"/><br/><b>🔍 失物招领</b></td>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/message.png" width="200"/><br/><b>✉️ 消息中心</b></td>
  </tr>
  <tr>
    <td colspan="3" align="center">
      <img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/ai-chat-2.png" width="320"/>
      <img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/ai-chat.png" width="320"/>
      <br/><b>🤖 AI 智能问答助手（RAG · 多轮对话 · 幻觉抑制 · 流式输出）</b>
    </td>
  </tr>
</table>
<p><i>另有：打印服务 · 校园跑腿 · 代课服务 · 学校切换 · 我的收藏 · 个人中心（详见源码）</i></p>
</div>

### 管理端（Vue3 + Element Plus + ECharts）

<div align="center">
<table>
  <tr>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/admin-dashboard.png" width="420"/><br/><b>📊 数据概览（用户增长趋势 · 业务占比）</b></td>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/admin-audit-post.png" width="420"/><br/><b>📝 内容审核 · 帖子审核</b></td>
  </tr>
  <tr>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/admin-user.png" width="420"/><br/><b>👥 用户管理</b></td>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/admin-school.png" width="420"/><br/><b>🏫 学校管理</b></td>
  </tr>
  <tr>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/admin-order-express.png" width="420"/><br/><b>📦 订单管理 · 快递代取</b></td>
    <td align="center"><img src="https://cdn.jsdelivr.net/gh/Alan-Sheep/smart-campus@main/docs/screenshots/admin-audit-goods.png" width="420"/><br/><b>🛒 商品审核</b></td>
  </tr>
</table>
</div>

---

## ✨ 核心设计与亮点---|---|---|
| 邮箱验证码登录 | SMTP 真实发送 + 开发模式 Mock 切换（配置驱动） | [EmailCodeService](backend/src/main/java/com/campus/smart/service/EmailCodeService.java) |
| 三层防刷 | 同邮箱 60s 重发间隔 + 单邮箱单日上限 + 同 IP 单日上限（全部 Redis 后端强校验） | 同上 |
| 防暴力破解 | 验证码错误 5 次锁定 30 分钟，锁定期间禁止重发 | 同上 |
| 一次性消费 | 验证码校验通过立即删除，杜绝重放 | 同上 |
| 认证漏洞修复 | 修复「验证码参数为空即跳过校验」的认证绕过漏洞 | [UserController](backend/src/main/java/com/campus/smart/controller/UserController.java) |
| JWT 无状态认证 | 拦截器统一鉴权；公开接口白名单 + 尽力身份识别（匿名页也能展示个人点赞/收藏态） | [JwtInterceptor](backend/src/main/java/com/campus/smart/interceptor/JwtInterceptor.java) |

### 🏫 多学校数据隔离（本项目最有特色的业务设计）

- 15 所学校数据按 `school_id` 隔离，覆盖全部业务表
- **切校即刷新**：10 个页面统一接入 uni-app `onShow` 生命周期，切换学校返回后自动重拉数据
- **跨校访客只读**：切到非本人学校时，仅校园频道可互动（发帖/评论/点赞/收藏），其余业务写操作统一守卫拦截 —— [school-permission.js](frontend/src/utils/school-permission.js)
- 学校上下文通过拦截器 + ThreadLocal 贯穿请求链路，业务层零侵入

### ⚡ 性能优化

| 问题 | 优化 | 效果 |
|---|---|---|
| 帖子列表 N+1 查询 | 逐条 COUNT 改为批量聚合（GROUP BY 一次性统计点赞/评论数） | 21 次查询 → **4 次** |
| 热点数据缓存 | 首页 banner/公告、帖子与商品浏览计数走 Redis | 缓存异常自动**降级查库**，主流程不受影响 |
| 数据库索引 | 高频查询列建复合索引（如 `school_id+category+create_time`） | 见 [perf_indexes.sql](sql/perf_indexes.sql) |

### 🧱 工程化实践

- **幂等迁移脚本**：SQL 增量迁移（v2→v5）按「school_id + 判重键」设计，可重复执行不产生脏数据
- **系统化接口测试**：多学校 × 多轮回归、防刷/锁定/过期边界场景、跨校隔离专项验证
- **配置分离**：真实密钥走 `application-local.yml`（不入库），主配置占位符 `${EMAIL_AUTH_CODE:}`
- **统一响应与全局异常**：`Result<T>` 包装 + `GlobalExceptionHandler`，业务错误不暴露堆栈

---

## 🛠️ 技术栈

### 后端

| 分类 | 技术 |
|---|---|
| 框架 | Spring Boot 2.7.18 · Spring AOP · Spring Validation · Spring Mail |
| ORM | MyBatis-Plus 3.5（逻辑删除 · 自动填充 · 条件构造器） |
| 安全 | JWT (jjwt) · 拦截器鉴权 · BCrypt |
| 中间件 | Redis（缓存 + 验证码 + 限流计数）· MySQL 8 |
| 接口文档 | Knife4j (OpenAPI 3) |
| 其他 | 阿里云 OSS（文件上传）· Java 21 |

### 前端

| 端 | 技术 |
|---|---|
| 用户端 | uni-app + Vue3 (`<script setup>`) + Pinia，编译 H5 / 微信小程序 |
| 管理端 | Vue3 + Element Plus + ECharts + Vue Router |
| 工程 | Vite |

### 数据库

23 张表，含外键约束、唯一索引（如 `uk_user_email`）、复合索引（多校隔离查询）、逻辑删除字段。完整建表与迁移脚本见 [sql/](sql/)。

---

## 🚀 快速开始

### 环境要求

```
JDK 21+ · MySQL 8+ · Redis 6+ · Node.js 16+ · Maven 3.6+
```

### 1️⃣ 初始化数据库

**方式一（推荐）：一键导入完整快照**

```bash
mysql -uroot -p < sql/smart_campus_full_dump.sql
```

> 包含 23 张表结构 + 全部演示数据（60 用户 / 33 帖子 / 570+ 订单 / 300 活动 / 聊天互动），一条命令即可跑起完整项目

**方式二：分步执行迁移脚本**

```bash
mysql -uroot -p < sql/init.sql
mysql -uroot -p < sql/full_init.sql
mysql -uroot -p < sql/update_v3_user_features.sql
mysql -uroot -p < sql/update_v4_school_data_seed.sql
mysql -uroot -p < sql/update_v5_email_login.sql
mysql -uroot -p < sql/update_v6_perf_indexes.sql
mysql -uroot -p < sql/update_v7a_users.sql
mysql -uroot -p < sql/update_v7b_posts.sql
mysql -uroot -p < sql/update_v7c_interactions.sql
mysql -uroot -p < sql/update_v7d_orders.sql
mysql -uroot -p < sql/update_v7e_signup_chat.sql
```

> 内置 15 所重庆高校 + **600+ 条业务演示数据**（60 个用户、33 篇帖子、570+ 订单、300 场活动、391 件商品、聊天/评论/点赞互动），启动即可看到完整页面效果

### 2️⃣ 配置并启动后端（8080）

```bash
# 修改 backend/src/main/resources/application.yml 中的数据库/Redis 密码
cd backend
mvn spring-boot:run
```

### 3️⃣ 启动用户端 H5（5173）

```bash
cd frontend
npm install
npm run dev:h5
# 浏览器访问 http://localhost:5173
```

### 4️⃣（可选）启动管理端（8081）

```bash
cd admin
npm install
npm run dev
# 默认账号 admin / admin123
```

### 5️⃣（可选）配置真实邮箱验证码

在后端创建 `application-local.yml`（此文件不入库）：

```yaml
spring:
  mail:
    username: your-qq@qq.com
    password: 你的16位SMTP授权码   # QQ邮箱设置→账户→开启IMAP/SMTP服务获取

email-code:
  enabled: true
```

> ❗ **不配置也能跑**：`email-code.enabled=false` 时验证码走开发模式（直接回显给前端），全部功能不受影响

---

## 📂 项目结构

```
SmartCampus
├── backend/                        # Spring Boot 后端
│   └── src/main/java/com/campus/smart
│       ├── controller/             # 26 个控制器（用户/频道/订单类/管理端）
│       ├── service/                # 业务层（EmailCodeService 安全设计是亮点）
│       ├── entity/                 # MyBatis-Plus 实体（23 张表）
│       ├── interceptor/            # JWT 鉴权 / 学校上下文 / 管理员权限
│       ├── config/                 # 拦截链 · Redis 配置 · 全局异常
│       └── utils/                  # Redis 工具 · JWT · ThreadLocal 上下文
├── frontend/                       # uni-app 用户端
│   └── src
│       ├── pages/                  # 15 个模块 40+ 页面
│       ├── utils/                  # request 封装 · 跨校权限守卫
│       └── store/                  # Pinia（用户态 · 学校态）
├── admin/                          # Vue3 管理端
│   └── src/views                   # 审核中心 · 订单管理 · 数据看板
├── sql/                            # 初始化 + v2~v5 幂等迁移
└── docs/                           # 设计文档与测试报告
```

---

## 🔑 演示账号

| 端 | 账号 | 说明 |
|---|---|---|
| 用户端 | 任意 QQ 邮箱 | 验证码登录，新邮箱自动注册 |
| 用户端（开发模式） | 未配置邮箱时 | 验证码直接回显在弹窗中 |
| 管理端 | admin / admin123 | 内容审核与数据看板 |

---

## 📌 Roadmap

- [ ] WebSocket 站内私信实时推送（当前轮询方案）
- [ ] 微信小程序支付（跑腿/代课报酬闭环）
- [ ] Redis 分布式锁防止订单并发接单
- [ ] 单元测试覆盖核心 Service 层
- [ ] Docker Compose 一键部署

---

## 📄 License

[MIT](LICENSE) · 仅供学习交流使用
