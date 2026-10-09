# 智慧校园 · 管理后台（admin）

> Vue 3 + Vite + Element Plus + ECharts 构建的管理后台前端，对接后端 81 个管理端接口。

---

## 一、快速开始

```bash
cd admin
npm install          # 首次运行
npm run dev          # 开发模式，默认 http://localhost:8081
npm run build        # 生产构建，产物在 admin/dist
npm run preview      # 预览构建产物
```

**默认账号**：`admin` / `admin123`

> 开发模式下 Vite 已配置 `/api` 反向代理到 `http://localhost:8080`，
> 因此**必须先启动后端**（`spring-boot:run` 或 `java -jar`），且后端需在 8080 端口。

---

## 二、功能模块

| 模块 | 路由 | 说明 |
|---|---|---|
| 数据概览 | `/dashboard` | 核心指标卡 + 用户增长/订单趋势/模块占比/消息分布图表 |
| 用户管理 | `/user` | 用户列表、详情、编辑、启用禁用 |
| 学校管理 | `/school` | 学校增删改查、LOGO、排序、状态 |
| 轮播图管理 | `/banner` | 轮播图增删改、上移下移排序、启停 |
| 公告管理 | `/notice` | 公告增删改、置顶、查看全文 |
| 内容审核 | `/audit/*` | 商品 / 帖子 / 活动 / 失物招领 四类审核（通过 / 驳回 / 下架） |
| 订单管理 | `/order/*` | 快递 / 跑腿 / 打印 / 代课 四类订单（接单 / 完成 / 取消 / 退款） |
| 意见反馈 | `/feedback` | 反馈列表、处理状态流转、回复 |
| 管理员管理 | `/manage` | 管理员增删改、重置密码 |
| 个人中心 | `/profile` | 账号信息、修改密码 |

---

## 三、技术要点

### 请求封装（`src/utils/request.js`）
- 自动注入 `Authorization: Bearer <token>`
- 统一解包后端 `{ code, msg, data }`：`code === 200` 返回 `data`
- `401` 自动清除登录态并跳转登录页（带防重复跳转保护）
- 其余业务错误码统一 `ElMessage` 提示

### 鉴权（`src/store/auth.js` + `src/router/index.js`）
- token 与管理员信息持久化到 `localStorage`
- 全局路由守卫：未登录访问任意页面 → 跳转 `/login`，并携带 `redirect` 参数
- 已登录访问 `/login` → 直接进 `/dashboard`

### 权限边界
- 请求头中的 `School-Id` 未在管理端使用；管理端以 JWT 中的 `role=admin` 为准
- 后端 `AdminJwtInterceptor` 校验 `role === 'admin'`，否则返回 403

### 状态码约定（与后端一致）

**审核状态 `auditStatus`**：`0` 待审核 · `1` 已通过 · `2` 已驳回

**订单状态 `status`**：`1` 待接单 · `2` 已接单 · `3` 已完成 · `4` 已取消 · `5` 已退款

**反馈状态 `status`**：`0` 待处理 · `1` 处理中 · `2` 已完成
**反馈类型 `type`**：`0` 功能建议 · `1` BUG反馈 · `2` 投诉举报 · `3` 其他

**失物招领类型 `type`**：`0` 寻物 · `1` 招领

---

## 四、目录结构

```
admin/
├── index.html
├── vite.config.js          # 含 /api 代理、按需引入、分包配置
├── package.json
└── src/
    ├── main.js             # 入口（Element Plus 中文语言包）
    ├── App.vue
    ├── api/index.js        # 全部管理端接口（按模块划分）
    ├── utils/request.js    # axios 封装
    ├── router/index.js     # 路由 + 登录守卫
    ├── store/auth.js       # 登录态
    ├── styles/index.scss   # 全局样式与主题变量
    ├── layout/index.vue    # 主布局（侧边栏 + 顶栏 + 面包屑）
    ├── components/
    │   └── BaseChart.vue   # ECharts 封装（自适应 + 自动销毁）
    └── views/
        ├── login/          # 登录（左右分栏品牌页）
        ├── dashboard/      # 数据概览
        ├── user/ school/ banner/ notice/
        ├── audit/          # goods / post / activity / lostfound
        ├── order/          # OrderPage.vue 通用组件 + 四个薄封装
        ├── feedback/ manage/ profile/
```

### 设计说明：订单模块为何只有一个组件
快递 / 跑腿 / 打印 / 代课四类订单的**列表结构、状态机与操作完全一致**，
仅字段不同。因此抽出 `order/OrderPage.vue`，通过 `module` prop 传入配置
（列定义、详情字段、标题），四个路由页面只是 6 行的薄封装。
这样状态语义与交互只有一份实现，避免四处重复导致行为不一致。

---

## 五、UI 设计

- **主色**：紫蓝渐变 `#6c4ee6 → #8b5cf6`（与用户端一致）
- **布局**：深色渐变侧边栏 + 白色顶栏 + 浅灰内容区
- **卡片化**：指标卡带顶部渐变条与悬浮上浮效果
- **图表**：ECharts 折线（带渐变面积）、环形饼图、堆叠柱状图
- **响应式**：栅格基于 Element Plus `xs/sm/md/lg`，窄屏自动堆叠
- **细节**：自定义滚动条、页面切换过渡、表格行悬浮高亮、状态标签配色

---

## 六、已验证内容

构建与运行：
- `npm run build` 成功（2350 modules，产物含 vue / element / echarts 三个独立分包）
- 18 个路由全部可访问；20 个 Vue 组件全部通过 Vite 真实编译管线（无模板/脚本错误）
- 开发服务器 `/api` 代理到后端 8080 正常

管理端接口端到端验证（走 Vite 代理，模拟前端真实调用）：
- 登录鉴权：正确/错误密码、无效 token、无 token → 401
- 概览 + 5 个图表接口全部返回 200
- 用户：列表 / 详情 / 编辑（写操作）
- 学校：**新增 → 按名筛选 → 修改 → 删除** 完整闭环
- 轮播图 / 公告：新增成功
- 内容审核：商品 / 帖子 / 活动 / 失物招领 四类列表 + `type` 服务端筛选
- 订单：快递 / 跑腿 / 打印 / 代课 四类列表 + `status` 筛选
- 反馈：列表 / 回复（回复后 `status=2`）/ 处理状态流转
- 管理员：列表 / 当前信息

> 后端 153 个端点全量扫描：所有响应均为规范业务码，
> `GlobalExceptionHandler` 捕获 31 条非法入参异常，**0 条** 逃逸到 Spring 兜底 500。

---

## 七、已知说明

1. **后端 `password` 字段回传**：`/admin/manage/list` 等接口返回体包含 `password`
   （BCrypt 哈希）。前端未展示，但建议后端统一置空
   （`AdminAuthController` 的 `login`/`getInfo` 已置空，`AdminManageController.list` 未置空）。
2. **role 是数字不是字符串**：`admin.role` 为 TINYINT（`1` 普通管理员 / `2` 超级管理员）。
   前端用 `Number(role) === 2` 判断，勿写成 `role === 'super'`。
3. **轮播图排序**：`上移/下移` 仅在**当前页内**交换并提交当前页 id 序列。
4. **图片上传**：管理端暂以「填写图片 URL」录入（后端 `/api/file/upload` 目前为模拟实现）。
5. **构建临时目录**：`vite.config.js` 会把 esbuild 临时目录指向项目内 `.tmp-esbuild/`，
   以规避部分 Windows 环境下系统临时目录不可删除导致的构建失败。该目录已加入 `.gitignore`。

---

## 八、本次一并修复的后端缺陷

搭管理后台时，端到端验证暴露出 3 个后端缺陷，已修复：

| 缺陷 | 现象 | 修复 |
|---|---|---|
| `banner` / `notice` / `category` 的 `school_id` 列默认值为 `0` 但有外键约束 | 不传 `schoolId` 新增时抛 `Field 'school_id'` / 外键冲突 → 500 | 实体字段加 `FieldStrategy.IGNORED`，让 null 写成真正的 NULL |
| 反馈回复把 `status` 写死为 `3` | 库中语义是 `0/1/2`，`3` 为非法值；用户端按 status 判断是否展示回复 → **用户看不到回复** | 改为 `2`（已完成）；用户端改为「有 reply 就展示」 |
| `lostfoundList` 缺少 `type` 参数 | 前端只能在本页做客户端过滤，**总数与分页不准确** | 控制器补 `type` 参数，改为服务端筛选 |

