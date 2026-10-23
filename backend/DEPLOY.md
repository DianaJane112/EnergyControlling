# 智慧水电能耗管理平台 — 部署与运维说明书

> **版本**: v1.0.0
> **日期**: 2026-06-17
> **目标域名**: `https://mingsifuwu.com`
> **适用对象**: 甲方 IT 运维人员
> **文档说明**: 本文档涵盖初次部署、日常运维、监控备份、故障排查、系统升级等全部内容。

---

## 目录

**第一部分：初次部署**

1. [服务器环境要求](#1-服务器环境要求)
2. [部署前准备清单](#2-部署前准备清单)
3. [环境变量配置说明](#3-环境变量配置说明)
4. [第三方平台参数获取指南](#4-第三方平台参数获取指南)
5. [参数配置速查表（含源码定位）](#5-参数配置速查表含源码定位)
6. [数据库部署](#6-数据库部署)
7. [文件存储配置](#7-文件存储配置)
8. [前端构建与部署](#8-前端构建与部署)
9. [后端构建与部署](#9-后端构建与部署)
10. [Nginx 配置与 SSL 证书](#10-nginx-配置与-ssl-证书)
11. [启动与停止服务](#11-启动与停止服务)
12. [首次登录与账号管理](#12-首次登录与账号管理)
13. [验证部署成功](#13-验证部署成功)

**第二部分：日常运维**

14. [系统架构概览](#14-系统架构概览)
15. [日常运维检查清单](#15-日常运维检查清单)
16. [监控与告警](#16-监控与告警)
17. [备份与恢复](#17-备份与恢复)
18. [日志管理](#18-日志管理)
19. [性能调优](#19-性能调优)
20. [系统升级流程](#20-系统升级流程)
21. [故障排查手册](#21-故障排查手册)
22. [安全管理](#22-安全管理)
23. [数据库维护](#23-数据库维护)
24. [应急响应流程](#24-应急响应流程)

**附录**

- [附录 A: 快速部署检查表](#附录-a-快速部署检查表)
- [附录 B: 运维命令速查表](#附录-b-运维命令速查表)
- [附录 C: 数据库表结构速查](#附录-c-数据库表结构速查)
- [附录 D: 环境变量完整清单](#附录-d-环境变量完整清单)
- [附录 E: 紧急联系方式](#附录-e-紧急联系方式)

---

## 1. 服务器环境要求

### 1.1 硬件配置

| 项目      | 最低要求       | 推荐配置            | 当前服务器                |
| ------- | ---------- | --------------- | -------------------- |
| CPU 核心数 | 4 核        | 8 核+            | —                    |
| 内存      | 8 GB       | 16 GB+          | 125 GB（剩余~62 GB ✅）   |
| 磁盘      | 100 GB SSD | 500 GB SSD+     | 3.5 TB（剩余~1.75 TB ✅） |
| 网络      | 固定公网 IP    | 固定公网 IP + 已备案域名 | ✅                    |

### 1.2 软件依赖

| 软件          | 最低版本                                  | 说明                   |
| ----------- | ------------------------------------- | -------------------- |
| **操作系统**    | CentOS 7.9+ / Ubuntu 20.04+ / RHEL 8+ | Linux 64-bit         |
| **JDK**     | **21**                                | 必须使用 JDK 21          |
| **MySQL**   | 8.0+                                  | 用于存储所有业务数据           |
| **Redis**   | 6.0+（Lua 脚本兼容至 2.6+）                  | 用于缓存、验证码、分布式锁        |
| **Nginx**   | 1.20+                                 | 反向代理 + SSL 终止 + 静态资源 |
| **Node.js** | 20 LTS+                               | 仅用于前端构建（构建后可移除）      |

### 1.3 技术栈总览

| 层级     | 技术                           | 版本              |
| ------ | ---------------------------- | --------------- |
| 后端框架   | Spring Boot                  | 4.0.6           |
| Java   | JDK                          | 21              |
| ORM    | MyBatis-Plus                 | 3.5.16          |
| 安全框架   | Spring Security + JWT (jjwt) | 0.12.6          |
| API 文档 | Knife4j + SpringDoc OpenAPI  | 4.5.0 / 2.8.0   |
| TCP 网关 | Netty                        | Spring Boot 自带  |
| 前端框架   | Vue 3 + Vite                 | —               |
| 报表导出   | EasyExcel (Alibaba)          | 4.0.3           |
| 二维码    | ZXing (Google)               | 3.5.3           |
| 文件检测   | Apache Tika                  | 2.9.2           |
| 对象存储   | 阿里云 OSS SDK / MinIO Client   | 3.18.1 / 8.5.10 |

### 1.4 端口要求

| 端口      | 用途                      | 对外开放？              |
| ------- | ----------------------- | ------------------ |
| `80`    | HTTP → HTTPS 重定向        | ✅ 对公网开放            |
| `443`   | HTTPS (Nginx SSL 终止)    | ✅ 对公网开放            |
| `8080`  | Spring Boot 后端 API（局域网） | ❌ 仅 localhost / 内网 |
| `18080` | Netty TCP 网关（积成水表 GPRS） | ✅ 需对集中器设备开放        |
| `6379`  | Redis（局域网）              | ❌ 仅 localhost      |
| `3306`  | MySQL（局域网）              | ❌ 仅 localhost      |

> ⚠️ **安全提示**: 端口 `8080`、`6379`、`3306` 千万不要开放到公网。
> 所有公网请求通过 Nginx 80/443 代理。只有 `18080` 需要在水表集中器需要主动连接时对外开放。

### 1.5 系统拓扑

```
公网用户 (https://mingsifuwu.com)
         │
         ▼
┌─────────────────────────────────┐
│     Nginx (:80, :443)           │
│  - SSL 终止                      │
│  - 前端静态资源托管               │
│  - API 反向代理 → :8080          │
│  - 文件代理 → /data/uploads/     │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│   Spring Boot 后端 (:8080)       │
│  - REST API 服务                 │
│  - Netty TCP 网关 (:18080)       │
│  - 定时任务 (计费/文件清理)       │
│  - Actuator 健康检查             │
└──────┬──────────────┬───────────┘
       │              │
       ▼              ▼
┌──────────┐  ┌──────────────┐
│ MySQL    │  │ Redis        │
│ (:3306)  │  │ (:6379)      │
└──────────┘  └──────────────┘
```

---

## 2. 部署前准备清单

请甲方 IT 人员在开始部署前，逐项确认以下内容已准备就绪：

### ✅ 基础设施

- [ ] **服务器操作系统已安装**（推荐 CentOS 7.9+ 或 Ubuntu 22.04 LTS）
- [ ] **JDK 21 已安装**，验证命令: `java -version`（应显示 `21.x.x`）
- [ ] **MySQL 8.0+ 已安装并启动**，记录 root 密码
- [ ] **Redis 6.0+ 已安装并启动**，设置访问密码
- [ ] **Nginx 1.20+ 已安装**
- [ ] **Node.js 20 LTS+ 已安装**（仅构建阶段需要）
- [ ] **域名 DNS 解析已配置**: `mingsifuwu.com` → 服务器固定公网 IP
- [ ] **SSL 证书文件已准备**（`.crt` 和 `.key` 文件，放在服务器安全目录）

### ✅ 厂商账号与参数

- [ ] **辰鳗电表**: 已获取 `appId` 和 `appSecret`（辰鳗平台 API 密钥）
- [ ] **积成水表**: 已获取 `sign`（通讯签名），确认集中器 `base-url`
- [ ] **捷先水表**: 已获取 `companyCode`（厂家编号），确认 API `base-url`
- [ ] **昊普电表**: 已获取 `appId` 和 `appSecret`，配置好 webhook 推送地址

### ✅ 微信小程序（如有）

- [ ] **微信小程序 AppID 和 AppSecret** 已获取（[微信公众平台](https://mp.weixin.qq.com/)）
- [ ] 小程序已配置 **服务器域名白名单**（request 合法域名: `https://mingsifuwu.com`）

### ✅ 安全凭证

- [ ] **管理员初始密码** 已确定（至少 8 位，含大小写字母+数字+特殊字符）
- [ ] **运营人员初始密码** 已确定
- [ ] **JWT 签名密钥** 已生成（至少 44 字符 Base64），生成命令见下文

---

## 3. 环境变量配置说明

在服务器上创建环境变量文件 `/etc/energy-platform/env.sh`。

> **使用说明**：以下模板中，标注 `⬅️ 甲方填写` 的行需要你替换为实际值；
> 标注 `✅ 已填好` 的行已给出推荐值，通常直接使用即可。
> 标注 `⏭️ 跳过` 的行是可选项，不接入对应厂商/服务时保持注释状态（行首有 `#`）即可。

```bash
#!/bin/bash
# =============================================================================
# 智慧水电能耗管理平台 — 生产环境变量配置
# 文件位置: /etc/energy-platform/env.sh
# 使用方式: source /etc/energy-platform/env.sh
# =============================================================================

# ──────────────────────────────────────────────────────────────
# 一、数据库连接（必填）
# ──────────────────────────────────────────────────────────────
export DB_HOST="127.0.0.1"                     # ✅ 已填好：MySQL 与本应用同机部署
export DB_PORT="3306"                           # ✅ 已填好：MySQL 默认端口
export DB_NAME="energy_platform"                # ✅ 已填好：数据库名（与部署脚本一致）
export DB_USERNAME="energy_platform"            # ⬅️ 甲方填写：创建数据库时设置的用户名
export DB_PASSWORD="MyDB@pwd2026!"              # ⬅️ 甲方填写：创建数据库时设置的密码
                                                #    格式无限制，建议 12 位以上含大小写+数字+符号

# ──────────────────────────────────────────────────────────────
# 二、Redis 连接（必填）
# ──────────────────────────────────────────────────────────────
export REDIS_HOST="127.0.0.1"                   # ✅ 已填好：Redis 与本应用同机部署
export REDIS_PORT="6379"                         # ✅ 已填好：Redis 默认端口
export REDIS_PASSWORD="MyRedis@2026!"            # ⬅️ 甲方填写：Redis 的 requirepass 密码
                                                #    在 redis.conf 中设置: requirepass <密码>

# ──────────────────────────────────────────────────────────────
# 三、JWT 签名密钥（必填）
#    ⚠️ 两个密钥必须不同！生成命令: openssl rand -base64 32
# ──────────────────────────────────────────────────────────────
# ⬅️ 甲方填写：在服务器终端执行以下命令生成，把输出粘贴到下面引号内
#    $ openssl rand -base64 32
#    示例输出: dGhpcyBpcyBhbiBleGFtcGxlIG9mIGEgd2VhayBKV1Qgc2VjcmV0IGtleSBmb3Igd2Vi
export JWT_WEB_SECRET=""                        # ⬅️ 填入上面命令生成的管理端密钥（44 字符 Base64）

#    $ openssl rand -base64 32     （再执行一次，用不同的输出）
export JWT_MINIAPP_SECRET=""                    # ⬅️ 填入上面命令生成的小程序端密钥（与上面不同！）

# ──────────────────────────────────────────────────────────────
# 四、管理员初始密码（必填）
#    要求: 至少 8 位，含大小写字母 + 数字 + 特殊字符
#    仅在首次启动且账号不存在时创建。已存在则忽略，不会覆盖。
# ──────────────────────────────────────────────────────────────
export ADMIN_INITIAL_PASSWORD="Admin@2026!"      # ⬅️ 甲方填写：管理员 admin 的初始密码
export OPERATOR_INITIAL_PASSWORD="Oper@2026!"    # ⬅️ 甲方填写：运营人员 operator 的初始密码

# ──────────────────────────────────────────────────────────────
# 五、CORS 跨域域名（必填）
# ──────────────────────────────────────────────────────────────
export CORS_ALLOWED_ORIGINS="https://mingsifuwu.com"   # ⬅️ 甲方确认：填写实际的访问域名

# ──────────────────────────────────────────────────────────────
# 六、微信小程序配置（有小程序则必填，无则保持注释即可）
# ──────────────────────────────────────────────────────────────
export WECHAT_APPID="wx1234567890abcdef"         # ⬅️ 甲方填写：小程序 AppID
export WECHAT_SECRET="a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"  # ⬅️ 甲方填写：小程序 AppSecret

# ──────────────────────────────────────────────────────────────
# 七、文件存储配置（必填）
# ──────────────────────────────────────────────────────────────
export STORAGE_ACTIVE="local"                    # ✅ 已填好：使用本地磁盘存储（推荐起步方案）
export STORAGE_BASE_URL="https://mingsifuwu.com" # ✅ 已填好：文件对外访问的基础 URL
export STORAGE_LOCAL_ROOT="/data/uploads"        # ✅ 已填好：文件存放目录

# ── 如果改用阿里云 OSS，取消下面注释并填写 ──
# export STORAGE_ACTIVE="oss"
# export OSS_ENDPOINT="oss-cn-hangzhou.aliyuncs.com"          # ⬅️ 甲方填写
# export OSS_ACCESS_KEY_ID="LTAI5txxxxxxxxxxxxxx"             # ⬅️ 甲方填写
# export OSS_ACCESS_KEY_SECRET="xxxxxxxxxxxxxxxxxxxxxxxxxx"   # ⬅️ 甲方填写
# export OSS_BUCKET_NAME="energy-platform"
# export OSS_BASE_URL="https://cdn.mingsifuwu.com"

# ── 如果改用 MinIO，取消下面注释并填写 ──
# export STORAGE_ACTIVE="minio"
# export MINIO_ENDPOINT="http://127.0.0.1:9000"
# export MINIO_ACCESS_KEY="minioadmin"
# export MINIO_SECRET_KEY="minioadmin"
# export MINIO_BUCKET_NAME="energy-platform"

# ──────────────────────────────────────────────────────────────
# 八、短信服务配置（可选，不接入则保持注释）
# ──────────────────────────────────────────────────────────────
# export SMS_DEV_MODE="false"
# export SMS_PROVIDER="aliyun"
# export SMS_AK="LTAI5txxxxxxxxxxxx"
# export SMS_SK="xxxxxxxxxxxxxxxxxxxxxxxx"
# export SMS_TEMPLATE="SMS_123456789"

# ──────────────────────────────────────────────────────────────
# 九、服务端口（可选，有默认值）
# ──────────────────────────────────────────────────────────────
# export SERVER_PORT="8080"

# =============================================================================
# 以下为厂商集成配置 —— 按实际接入的厂商填写，不接入则保持注释状态
# =============================================================================

# ── 辰鳗电表 ──
# export CHENMAN_APP_ID="sm_chenman_xxxxxxxx"
# export CHENMAN_APP_SECRET="xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

# ── 积成水表 ⚠️ TCP GPRS 长连接，集中器主动连接本平台 :18080 ──
# export JICHENG_BASE_URL="http://10.xx.xx.xx:8080/ws/rsyw"
# export JICHENG_SIGN="JCSIGN2024ABCD1234"
# export JICHENG_TCP_PORT="18080"

# ── 捷先水表 ──
# export JIEXIAN_BASE_URL="http://10.xx.xx.xx:30002/camrgo/interface"
# export JIEXIAN_COMPANY_CODE="0001"

# ── 昊普电表 ⚠️ 还需在昊普后台配置 webhook 推送地址 ──
# export HAOPU_BASE_URL="https://api.haopu-xxx.com"
# export HAOPU_APP_ID="hp_app_xxxxxxxx"
# export HAOPU_APP_SECRET="xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
```

注：若使用 CentOS 7，默认 yum 源可能无 JDK 21。请前往 Oracle 官网或 Adoptium 下载 JDK 21 的 Linux x64 `.tar.gz` 压缩包进行手动解压配置。

---

## 4. 第三方平台参数获取指南

### 4.1 辰鳗电表（Chenman）

**需要参数**: `appId`、`appSecret`

**获取方式**:

1. 登录 [辰鳗管理平台](https://smalleel.com/)
2. 进入「系统设置」→「API 密钥管理」
3. 生成或查看 API 密钥

**本平台调用辰鳗的接口**:

- `GET /pub/v1/info/dbPage` — 电表信息分页
- `GET /pub/v1/info/getDbRealTime` — 电表实时数据
- `POST /pub/v1/info/recharge` — 电表充值
- `GET /pub/v1/record/dbRechargePage` — 充值记录查询

**录入辰鳗电表时需填写的关键字段**:

| 字段名  | 数据库字段        | 说明                     |
| ---- | ------------ | ---------------------- |
| 表号   | `db_no`      | 12 位电表编号字符串（辰鳗侧的表号）    |
| 厂商   | `vender`     | 填写 `chenman`           |
| 倍率   | `bei`        | 电流互感器变比，默认 1           |
| 附加电费 | `plus_price` | 电费附加，单位元/kWh           |
| 商户名  | `name`       | 电表关联的商户名称              |
| 表计大类 | `meter_type` | 电表填 `1`                |
| 表计类型 | `bjlx`       | 1=租户, 2=总表, 3=公共, 4=其他 |
| 透支额度 | `credit`     | 允许透支的金额（元）             |
| 告警额度 | `warnline`   | 余额低于此值告警（元）            |
| 付费方式 | `feetype`    | 1=预付费, 2=后付费           |

### 4.2 积成水表（Jicheng）

**需要参数**: `sign`（通讯签名）、`base-url`
**通讯方式**: TCP 长连接（集中器通过 GPRS 主动连接本平台）

**获取方式**:

1. 联系积成电子技术支持
2. 申请通讯签名（sign）和 API 接口地址
3. 确认集中器的服务器地址指向: `mingsifuwu.com:18080`

**录入积成水表时需填写的关键字段**:

| 字段名        | 数据库字段           | 说明               |
| ---------- | --------------- | ---------------- |
| 钢印号/通讯地址   | `steal_no`      | 水表的唯一标识（积成侧表钢印号） |
| 厂商         | `vender`        | 填写 `jicheng`     |
| 表通道类型      | `channel_type`  | 如 `GPRS`         |
| 终端名称       | `terminal_name` | 安装的集中器名称         |
| 通讯序号       | `telecomm_sn`   | 集中器通讯序列号         |
| IMEI       | `imei`          | GPRS 模块 IMEI 码   |
| ICCID/IMSI | `iccid`         | SIM 卡 ICCID      |
| 表计大类       | `meter_type`    | 水表填 `0`          |

### 4.3 捷先水表（Jiexian）

**需要参数**: `base-url`、`companyCode`（厂家编号）

**获取方式**: 登录捷先水表管理平台获取 API 接口地址和厂家编号

**录入捷先水表时需填写的关键字段**:

| 字段名  | 数据库字段          | 说明           |
| ---- | -------------- | ------------ |
| 水表编号 | `meteraddress` | 捷先侧的水表唯一标识   |
| 厂家编号 | `companycode`  | 捷先分配的厂家编号    |
| 厂商   | `vender`       | 填写 `jiexian` |
| 用户编号 | `customerno`   | 营收户号（可选）     |
| 表计大类 | `meter_type`   | 水表填 `0`      |

### 4.4 昊普电表（Haopu）

**需要参数**: `base-url`、`appId`、`appSecret`

**Webhook 推送地址**（需在昊普后台配置）:

```
https://mingsifuwu.com/api/v1/integration/haopu/webhook
```

**录入昊普电表时需填写的关键字段**:

| 字段名    | 数据库字段           | 说明                          |
| ------ | --------------- | --------------------------- |
| 采集器编码  | `collectorCode` | 昊普采集器唯一编码                   |
| 电表设备编号 | `meterCode`     | 昊普电表唯一设备编号（数字）              |
| 厂商     | `vender`        | 填写 `haopu`                  |
| 串口波特率  | `bpsType`       | 如 `B2400`, `B4800`, `B9600` |
| 串口校验位  | `bitType`       | `EVEN` / `ODD` / `NONE`     |
| 通信协议   | `protoType`     | 如 `DLT645_2007`             |
| 数据序号   | `serial`        | 昊普侧分配的数据序号                  |
| 表计大类   | `meter_type`    | 电表填 `1`                     |

---

## 5. 参数配置速查表（含源码定位）

> 本章是**甲方 IT 运维的核心参考**：每个参数从 `env.sh` → `application-prod.yml` → **后端 Java 源码**的完整流向都清楚列出。

### 5.1 参数流向说明

```
甲方填写                  Spring 加载                Java 代码读取
───────                 ──────────               ──────────────
/etc/energy-platform/   application-prod.yml     @Value("${...}")
env.sh                  使用 ${ENV_VAR} 占位符     @ConfigurationProperties
   ↓                         ↓                        ↓
export DB_HOST=...    →   ${DB_HOST:localhost}  →   DataInitializer.java:39
```

### 5.2 配置文件总览

| 配置文件       | 路径                                            | 作用                      | 甲方是否需要修改 |
| ---------- | --------------------------------------------- | ----------------------- |:--------:|
| 环境变量文件     | `/etc/energy-platform/env.sh`                 | 所有敏感信息（密码、密钥、凭证）        | 🔴 新建并填写 |
| 生产配置       | `application-prod.yml`（JAR 内）                 | Spring Boot 配置入口，引用环境变量 | 🟢 无需修改  |
| 默认配置       | `application.yml`（JAR 内）                      | 开发默认值                   | 🟢 无需修改  |
| Nginx 配置   | `/etc/nginx/conf.d/energy-platform.conf`      | HTTPS + SSL + 反向代理      | 🔴 新建    |
| systemd 服务 | `/etc/systemd/system/energy-platform.service` | 应用进程管理                  | 🔴 新建    |
| 数据库脚本      | `database/deploy_schema.sql`                  | 建表 + 初始数据               | 🟢 直接执行  |
| 前端构建       | `frontend/` → `dist/`                         | 前端静态文件                  | 🔴 构建后部署 |

### 5.3 数据库连接（6 个参数）

| 参数       | env.sh 变量     | 填写示例               | yml 路径                       | 后端 Java 源码                            |
| -------- | ------------- | ------------------ | ---------------------------- | ------------------------------------- |
| MySQL 主机 | `DB_HOST`     | `127.0.0.1`        | `spring.datasource.url`      | Spring Boot 自动配置 → `HikariDataSource` |
| MySQL 端口 | `DB_PORT`     | `3306`             | `spring.datasource.url`      | Spring Boot 自动配置                      |
| 数据库名     | `DB_NAME`     | `energy_platform`  | `spring.datasource.url`      | Spring Boot 自动配置                      |
| 数据库用户名   | `DB_USERNAME` | `energy_platform`  | `spring.datasource.username` | Spring Boot 自动配置                      |
| 数据库密码    | `DB_PASSWORD` | `MyDB@pwd2026!`    | `spring.datasource.password` | Spring Boot 自动配置                      |
| 连接池大小    | —             | 默认 min=10 / max=30 | `spring.datasource.hikari`   | Spring Boot 自动配置                      |

### 5.4 Redis 连接（3 个参数）

| 参数       | env.sh 变量        | 填写示例            | yml 路径                       | 后端 Java 源码                 |
| -------- | ---------------- | --------------- | ---------------------------- | -------------------------- |
| Redis 主机 | `REDIS_HOST`     | `127.0.0.1`     | `spring.data.redis.host`     | `LettuceConnectionFactory` |
| Redis 端口 | `REDIS_PORT`     | `6379`          | `spring.data.redis.port`     | `LettuceConnectionFactory` |
| Redis 密码 | `REDIS_PASSWORD` | `MyRedis@2026!` | `spring.data.redis.password` | `LettuceConnectionFactory` |

### 5.5 JWT 签名密钥（2 个参数）⚠️ 安全核心

| 参数     | env.sh 变量            | 生成方式                      | 后端 Java 源码                                 |
| ------ | -------------------- | ------------------------- | ------------------------------------------ |
| 管理端密钥  | `JWT_WEB_SECRET`     | `openssl rand -base64 32` | `security/JwtUtils.java:35`                |
| 小程序端密钥 | `JWT_MINIAPP_SECRET` | 再执行一次，用不同输出               | `miniapp/security/MiniappJwtUtils.java:38` |

> ⚠️ **不填的后果**：应用启动时检测到密钥为空或使用默认值，**直接拒绝启动**。

### 5.6 管理员初始密码（2 个参数）

| 参数     | env.sh 变量                   | 格式要求                  | 后端 Java 源码                       |
| ------ | --------------------------- | --------------------- | -------------------------------- |
| 管理员密码  | `ADMIN_INITIAL_PASSWORD`    | 至少 8 位，含大写+小写+数字+特殊字符 | `config/DataInitializer.java:39` |
| 运营人员密码 | `OPERATOR_INITIAL_PASSWORD` | 同上                    | `config/DataInitializer.java:42` |

### 5.7 厂商集成参数速查

| 厂商  | env.sh 变量                                              | 后端 Java 源码                                              |
| --- | ------------------------------------------------------ | ------------------------------------------------------- |
| 辰鳗  | `CHENMAN_APP_ID`, `CHENMAN_APP_SECRET`                 | `VendorApiProperties.java:26-28`                        |
| 积成  | `JICHENG_BASE_URL`, `JICHENG_SIGN`, `JICHENG_TCP_PORT` | `VendorApiProperties.java:37-39`, `NettyServer.java:37` |
| 捷先  | `JIEXIAN_BASE_URL`, `JIEXIAN_COMPANY_CODE`             | `VendorApiProperties.java:46-48`                        |
| 昊普  | `HAOPU_BASE_URL`, `HAOPU_APP_ID`, `HAOPU_APP_SECRET`   | `VendorApiProperties.java:56-60`                        |

### 5.8 定时任务参数

| 任务     | cron 默认值                 | 说明      | 后端 Java 源码                          |
| ------ | ------------------------ | ------- | ----------------------------------- |
| 自动计费   | `0 0 2 * * ?`（每天凌晨 2:00） | 全量扣费    | `AutomatedBillingScheduler.java:59` |
| 孤儿文件清理 | `0 0 3 * * ?`（每天凌晨 3:00） | 清除未关联文件 | `OrphanFileCleanupTask.java:62`     |

### 5.9 参数分类速查

**🔴 必填（8 个）**: `DB_HOST` `DB_PORT` `DB_NAME` `DB_USERNAME` `DB_PASSWORD` `REDIS_HOST` `REDIS_PORT` `REDIS_PASSWORD`

**🔴 强烈建议（5 个）**: `JWT_WEB_SECRET` `JWT_MINIAPP_SECRET` `ADMIN_INITIAL_PASSWORD` `OPERATOR_INITIAL_PASSWORD` `CORS_ALLOWED_ORIGINS`

**🔵 文件存储（至少 3 个）**: `STORAGE_ACTIVE` `STORAGE_BASE_URL` `STORAGE_LOCAL_ROOT`

**🟡 厂商集成（按实际接入填写）**: 每家 2-3 个参数，详见 3-4 节。

### 5.10 表计编号格式配置（可选）

> 控制新增表计时自动生成的 `db_no` 编号格式。甲方如需自定义编号规则，修改 `application.yml` 中以下配置即可，**无需改动 Java 代码**。

**配置位置**: `src/main/resources/application.yml` 中的 `meter.no` 段（打包在 JAR 内，如需修改需重新构建）。

```yaml
# ── 表计编号生成配置 ──
# 编号规则：前缀 + 零填充序列号，总长度固定为 total-length
# 示例：前缀 "ELEC" + 总长 12 + 序号 1 → "ELEC000000001"
meter:
  no:
    # 电表编号前缀（meterType=1）
    electric-prefix: "ELEC"
    # 水表编号前缀（meterType=0）
    water-prefix: "WATER"
    # 编号总长度（包含前缀），不足左侧补0
    total-length: 12
```

| 参数                         | 默认值  | 说明                                |
| -------------------------- | ---- | --------------------------------- |
| `meter.no.electric-prefix` | `01` | 电表编号前缀（meterType=1），建议用字母避免与纯数字混淆 |
| `meter.no.water-prefix`    | `02` | 水表编号前缀（meterType=0）               |
| `meter.no.total-length`    | `12` | 编号总长度（含前缀），不足位数左侧补零               |

**编号生成规则**:

```
编号 = 前缀 + 零填充(序列号, 总长 - 前缀长度)
```

例如配置 `electric-prefix: "ELEC"` + `total-length: 12`：

- 第 1 个电表 → `ELEC000000001`
- 第 25 个电表 → `ELEC000000025`
- 第 9999 个电表 → `ELEC000009999`

**序列号来源**: 系统自动查询数据库中该前缀下的最大序号 +1，确保连续递增不重复。

**后端源码定位**:

| 组件     | 文件路径                                              | 说明                          |
| ------ | ------------------------------------------------- | --------------------------- |
| 编号生成器  | `common/DbNoGenerator.java`                       | 读取 `meter.no.*` 配置，拼接前缀+序列号 |
| 最大序号查询 | `mapper/MeterMapper.java` → `getMaxSeqByPrefix()` | SQL 查询当前最大序号                |
| 自动生成入口 | `service/impl/MeterServiceImpl.java` → `create()` | 新增表计时若未传 dbNo 则自动生成         |

**变更编号格式的操作步骤**:

1. 修改 `application.yml` 中 `meter.no.*` 配置项
2. 重新构建 JAR: `mvn clean package -DskipTests`
3. 按第 20 节「系统升级流程」部署新版本
4. 现有表计的编号不受影响；仅新创建的表计使用新格式

**常见定制示例**:

```yaml
# 示例1: 纯数字格式（传统水电表编号）
meter:
  no:
    electric-prefix: "01"
    water-prefix: "02"
    total-length: 12
# 产生: 010000000001, 020000000001

# 示例2: 含项目代码前缀
meter:
  no:
    electric-prefix: "XM-E-"
    water-prefix: "XM-W-"
    total-length: 16
# 产生: XM-E-0000000001, XM-W-0000000001

# 示例3: 含年份前缀
meter:
  no:
    electric-prefix: "E2026"
    water-prefix: "W2026"
    total-length: 14
# 产生: E202600000001, W202600000001
```

---

## 6. 数据库部署

### 6.1 创建数据库和用户

```bash
mysql -u root -p
```

```sql
CREATE DATABASE IF NOT EXISTS energy_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'energy_platform'@'127.0.0.1' IDENTIFIED BY '<强密码>';
GRANT ALL PRIVILEGES ON energy_platform.* TO 'energy_platform'@'127.0.0.1';
FLUSH PRIVILEGES;
```

### 6.2 执行数据库初始化脚本

交付包中 `database/` 目录包含三个 SQL 脚本，按需执行：

| 脚本 | 用途 | 何时执行 |
|------|------|----------|
| `deploy_schema.sql` | 建表 + 默认角色/费率 | 🔴 必须执行（首次部署） |
| `demo_data.sql` | 演示数据（智慧能源示范园区） | 🟡 甲方验收测试时执行 |
| `demo_cleanup.sql` | 清空演示数据 | 🟢 验收完成后执行，恢复空白系统 |

#### ① 建表（必须）

```bash
# 将 database/deploy_schema.sql 上传到服务器后执行
mysql -u energy_platform -p energy_platform < deploy_schema.sql
```

```bash
# 验证
mysql -u energy_platform -p energy_platform -e "SHOW TABLES;"
```

预期看到 25+ 张表：`t_user`、`t_meter`、`t_customer`、`t_meter_reading`、`t_collector`、`t_price_strategy`、`t_bill`、`t_file_record` 等。

建表后系统包含的默认数据（可直接投入使用）：
- 3 个角色（系统管理员 / 运营人员 / 审计员）
- 用户账号由应用首次启动时通过环境变量创建
- 1 条全局费率策略（固定电价 0.85 元/kWh）
- 1 条全局费率配置

#### ② 导入演示数据（可选，验收测试时使用）

如果甲方希望在验收阶段看到图表、报表等效果，可导入演示数据：

```bash
mysql -u energy_platform -p energy_platform < demo_data.sql
```

演示数据包含一个完整的 **智慧能源示范园区** 场景：

| 数据类型 | 数量 | 说明 |
|----------|------|------|
| 项目 | 1 个 | 智慧能源示范园区 |
| 商户 | 5 个 | 嘉和超市、园区美食广场、创客咖啡馆、便民洗衣中心、邻里便利店 |
| 分组 | 3 个 | 按楼栋 B13 / B15 / B17 划分 |
| 采集器 | 2 台 | 4G 无线集中器 |
| 电表 | 6 个 | 分布在三个楼栋，含商户表和总表 |
| 水表 | 2 个 | B13 栋超市和美食广场 |
| 日冻结记录 | ~180 条 | 最近 30 天，工作日/周末用电差异 |
| 日报表 | ~180 条 | 按日预聚合，含分时电量和扣费金额 |
| 月报表 | 6 条 | 2026 年 5 月各电表月度汇总 |
| 水表读数 | 20 条 | 最近 30 天，每 3 天一条 |
| 账单 | 6 张 | 覆盖已支付 / 未支付 / 待审核三种状态 |
| 充值记录 | 4 笔 | 含充值订单和资金流水 |
| 银行账户 | 2 个 | 商户收款账户 |

所有演示数据以 `demo` 标记，与正式数据完全隔离。

#### ③ 验收后清理（可选）

甲方验收通过后，执行清理脚本清空所有演示数据，系统恢复为空白生产状态：

```bash
mysql -u energy_platform -p energy_platform < demo_cleanup.sql
```

清理脚本精确删除所有 `demo` 标记的数据，**不会影响**：
- 系统默认角色（ADMIN / OPERATOR / AUDITOR）
- 全局费率策略和费率配置
- 环境变量创建的初始账号

---

## 7. 文件存储配置

### 7.1 本地存储（推荐起步方案）

```bash
export STORAGE_ACTIVE="local"
export STORAGE_LOCAL_ROOT="/data/uploads"
export STORAGE_BASE_URL="https://mingsifuwu.com"
```

```bash
sudo mkdir -p /data/uploads
sudo chown -R <运行应用的用户> /data/uploads
sudo chmod 755 /data/uploads
```

> 文件以日期目录组织（如 `2026/06/17/uuid.jpg`），避免单目录文件过多。每日凌晨 3:00 自动清理孤儿文件。

### 7.2 阿里云 OSS / MinIO（可选）

详见 `env.sh` 中对应注释块，取消注释并填写即可切换存储后端。

---

## 9. 后端构建与部署

### 9.1 构建 JAR 包

```bash
mvn clean package -DskipTests
# 产物: target/EnergyControllerPlatform-1.0.0-RELEASE.jar
```

### 9.2 部署到服务器

```bash
sudo mkdir -p /opt/energy-platform/logs
# 上传 JAR 到 /opt/energy-platform/
```

### 9.3 配置 systemd 服务（推荐）

创建 `/etc/systemd/system/energy-platform.service`:

```ini
[Unit]
Description=智慧水电能耗管理平台
After=network.target mysql.service redis.service
Wants=mysql.service redis.service

[Service]
Type=simple
User=energy-platform
WorkingDirectory=/opt/energy-platform
EnvironmentFile=/etc/energy-platform/env.sh
ExecStart=/usr/bin/java -jar \
    -Dspring.profiles.active=prod \
    -Xms4g -Xmx8g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/opt/energy-platform/logs/ \
    /opt/energy-platform/EnergyControllerPlatform-1.0.0-RELEASE.jar
ExecStop=/bin/kill -15 $MAINPID
Restart=on-failure
RestartSec=10
StandardOutput=append:/opt/energy-platform/logs/sys_stdout.log
StandardError=append:/opt/energy-platform/logs/sys_stderr.log

[Install]
WantedBy=multi-user.target
```

启用服务:

```bash
sudo useradd -r -s /bin/false energy-platform
sudo chown -R energy-platform:energy-platform /opt/energy-platform
sudo chown -R energy-platform:energy-platform /data/uploads
sudo systemctl daemon-reload
sudo systemctl enable energy-platform
sudo systemctl start energy-platform
```

> **JVM 内存建议**: 当前服务器 125GB 内存，建议 `-Xms4g -Xmx8g`，可视业务量调整。

---

## 10. Nginx 配置与 SSL 证书

### 10.1 上传 SSL 证书

```bash
sudo mkdir -p /etc/nginx/ssl
sudo cp mingsifuwu.com.pem /etc/nginx/ssl/
sudo cp mingsifuwu.com.key /etc/nginx/ssl/
sudo chmod 600 /etc/nginx/ssl/mingsifuwu.com.key
sudo chmod 644 /etc/nginx/ssl/mingsifuwu.com.pem
```

### 10.2 Nginx 配置

创建 `/etc/nginx/conf.d/energy-platform.conf`:

```nginx
upstream energy_backend {
    server 127.0.0.1:8080 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

# HTTP → HTTPS 重定向
server {
    listen 80;
    server_name mingsifuwu.com www.mingsifuwu.com;
    return 301 https://$host$request_uri;
}

# HTTPS 主服务
server {
    listen 443 ssl http2;
    server_name mingsifuwu.com www.mingsifuwu.com;

    ssl_certificate     /etc/nginx/ssl/mingsifuwu.com.pem;
    ssl_certificate_key /etc/nginx/ssl/mingsifuwu.com.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers on;
    add_header Strict-Transport-Security "max-age=63072000" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;

    access_log /var/log/nginx/energy-platform.access.log;
    error_log  /var/log/nginx/energy-platform.error.log;
    client_max_body_size 50m;

    root /var/www/energy-platform;
    index index.html;

    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    location /api/ {
        proxy_pass http://energy_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
        proxy_request_buffering off;
    }

    location /files/ {
        alias /data/uploads/;
        expires 7d;
        autoindex off;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /actuator/health {
        proxy_pass http://energy_backend;
        access_log off;
    }
}
```

### 10.3 验证并重载

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 11. 启动与停止服务

### 11.1 启动顺序

```bash
sudo systemctl status mysql     # 1. 确保 MySQL 已启动
sudo systemctl status redis     # 2. 确保 Redis 已启动
sudo systemctl start energy-platform  # 3. 启动后端应用
sudo systemctl status nginx     # 4. 确保 Nginx 已启动
```

### 11.2 常用命令

| 操作       | 命令                                       |
| -------- | ---------------------------------------- |
| 启动后端     | `sudo systemctl start energy-platform`   |
| 停止后端     | `sudo systemctl stop energy-platform`    |
| 重启后端     | `sudo systemctl restart energy-platform` |
| 查看状态     | `sudo systemctl status energy-platform`  |
| 查看日志     | `sudo journalctl -u energy-platform -f`  |
| 重载 Nginx | `sudo systemctl reload nginx`            |

### 11.3 验证运行

```bash
ss -tlnp | grep -E "8080|18080|80|443"
curl http://127.0.0.1:8080/actuator/health
curl -I https://mingsifuwu.com
```

---

## 12. 首次登录与账号管理

### 12.1 首次登录

1. 浏览器访问: `https://mingsifuwu.com`
2. 使用管理员账号登录:
   - **用户名**: `admin`
   - **密码**: 环境变量 `ADMIN_INITIAL_PASSWORD` 中设置的值
3. **登录后立即修改密码**（个人设置 → 修改密码）

### 12.2 默认角色与权限

| 角色              | 用户名        | 权限范围                             |
| --------------- | ---------- | -------------------------------- |
| 系统管理员 (ADMIN)   | `admin`    | 全部功能：表计管理、商户管理、报表、用户管理、角色管理、系统配置 |
| 运营人员 (OPERATOR) | `operator` | 表计查看/编辑、商户查看/编辑、报表查看/导出          |
| 审计员 (AUDITOR)   | 需手动创建      | 表计查看、商户查看、报表查看/导出、操作日志查看         |

---

## 13. 验证部署成功

### 13.1 后端 API 验证

```bash
# 健康检查
curl https://mingsifuwu.com/actuator/health
# 预期: {"status":"UP",...}

# 登录测试
curl -X POST https://mingsifuwu.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<你的密码>"}'
# 预期: {"code":200,"data":{"token":"...","username":"admin",...}}
```

### 13.2 前端页面 + 文件上传 + 厂商集成验证

- 浏览器访问 `https://mingsifuwu.com` 确认登录页正常
- 登录后上传测试文件，确认上传/下载功能正常
- 查看日志确认各厂商数据拉取正常: `tail -f /opt/energy-platform/logs/sys_stdout.log | grep "辰鳗\|积成\|捷先\|昊普"`

---

---

# 第二部分：日常运维

---

## 14. 系统架构概览

### 14.1 核心业务模块

| 模块   | 主要功能              | 关键 Controller                                                    |
| ---- | ----------------- | ---------------------------------------------------------------- |
| 用户认证 | 登录/登出/JWT 鉴权/密码重置 | `AuthController`, `PasswordResetController`                      |
| 表计管理 | 电表/水表 CRUD、分组管理   | `MeterController`, `MeterGroupController`, `CollectorController` |
| 商户管理 | 商户档案、账户余额         | `CustomerController`, `PropertyController`                       |
| 读数管理 | 表计读数采集与查询         | `MeterReadingController`                                         |
| 计费系统 | 费率策略、自动扣费、账单      | `BillingController`, `PriceStrategyController`, `BillController` |
| 财务报表 | 能耗报表、财务统计、导出      | `ReportController`, `FinancialController`, `DashboardController` |
| 充值审核 | 电表远程充值、审核流程       | `RechargeAuditController`, `ChargeController`                    |
| 文件管理 | 上传/下载/预览、存储切换     | `FileController`                                                 |
| 厂商集成 | 辰鳗/积成/捷先/昊普数据对接   | `integration/` 包下各 Client                                        |
| 操作日志 | 全操作审计记录           | `OperationLogController`                                         |

---

## 15. 日常运维检查清单

### 15.1 每日检查（建议每天早上 9:00 执行）

```bash
# 1. 所有服务运行状态
sudo systemctl status energy-platform mysql redis nginx --no-pager -l | grep -E "Active|Loaded"

# 2. 端口监听
ss -tlnp | grep -E "80|443|8080|18080|3306|6379"

# 3. 后端健康状态
curl -s http://127.0.0.1:8080/actuator/health

# 4. 磁盘使用率
df -h | grep -E "Filesystem|/data|/opt|/"

# 5. 内存使用
free -h

# 6. 最近 30 分钟错误日志
sudo journalctl -u energy-platform --since "30 min ago" -p err --no-pager

# 7. TCP 网关（积成水表）
ss -tlnp | grep 18080
```

### 15.2 每周检查（建议每周一执行）

```bash
# 1. 数据库核心统计
mysql -u energy_platform -p energy_platform -e "
  SELECT COUNT(*) AS total_meters FROM t_meter WHERE is_deleted = 0;
  SELECT COUNT(*) AS total_customers FROM t_customer WHERE is_deleted = 0;
  SELECT COUNT(*) AS today_readings FROM t_meter_reading WHERE DATE(create_time) = CURDATE();
"

# 2. SSL 证书有效期
echo | openssl s_client -connect mingsifuwu.com:443 -servername mingsifuwu.com 2>/dev/null | openssl x509 -noout -dates

# 3. 系统更新检查
sudo yum check-update 2>/dev/null || sudo apt list --upgradable 2>/dev/null
```

### 15.3 每月检查

```bash
# 1. 备份完整性
ls -lh /backup/energy_platform_$(date +%Y%m)*.sql.gz

# 2. 日志文件大小
du -sh /opt/energy-platform/logs/
du -sh /var/log/nginx/energy-platform.*

# 3. Redis 内存
redis-cli -a "$REDIS_PASSWORD" INFO memory | grep used_memory_human

# 4. 操作日志审计
mysql -u energy_platform -p energy_platform -e "
  SELECT operation, COUNT(*) as cnt FROM t_operation_log
  WHERE create_time > DATE_SUB(NOW(), INTERVAL 30 DAY) AND is_deleted = 0
  GROUP BY operation ORDER BY cnt DESC LIMIT 20;
"
```

---

## 16. 监控与告警

### 16.1 健康检查端点

| 端点                 | 用途                    |
| ------------------ | --------------------- |
| `/actuator/health` | 综合健康状态（含 DB/Redis/磁盘） |
| `/actuator/info`   | 应用版本信息                |

### 16.2 自动监控脚本

创建 `/opt/energy-platform/monitor.sh`：

```bash
#!/bin/bash
set -e
LOG_FILE="/opt/energy-platform/logs/monitor.log"
APP_URL="http://127.0.0.1:8080"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"; }
alert() { log "🚨 告警: $1"; }

# 检查后端进程
if ! systemctl is-active --quiet energy-platform; then
    alert "后端服务已停止！尝试自动重启..."
    sudo systemctl restart energy-platform
    sleep 10
    if ! systemctl is-active --quiet energy-platform; then
        alert "后端服务重启失败！请立即检查！"
    fi
fi

# 检查健康端点
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$APP_URL/actuator/health" 2>/dev/null || echo "000")
if [ "$HEALTH" != "200" ]; then
    alert "健康检查失败 (HTTP $HEALTH)"
fi

# 检查端口
if ! ss -tlnp | grep -q ":8080 "; then
    alert "端口 8080 未监听！"
fi

# 检查磁盘
DISK_USAGE=$(df /data | tail -1 | awk '{print $5}' | sed 's/%//')
if [ "$DISK_USAGE" -gt 85 ]; then
    alert "磁盘使用率超过 85%（当前: ${DISK_USAGE}%）"
fi

# 检查内存
MEM_USAGE=$(free | grep Mem | awk '{printf "%.0f", $3/$2 * 100}')
if [ "$MEM_USAGE" -gt 90 ]; then
    alert "内存使用率超过 90%（当前: ${MEM_USAGE}%）"
fi

log "监控检查完成 - 健康: $HEALTH, 磁盘: ${DISK_USAGE}%, 内存: ${MEM_USAGE}%"
```

加入 crontab:

```bash
chmod +x /opt/energy-platform/monitor.sh
(crontab -l 2>/dev/null; echo "*/5 * * * * /opt/energy-platform/monitor.sh") | crontab -
```

### 16.3 关键监控指标

| 指标            | 正常范围             | 告警阈值            |
| ------------- | ---------------- | --------------- |
| 后端服务状态        | active (running) | inactive/failed |
| HTTP 健康检查     | 200              | 非 200           |
| 端口 8080/18080 | LISTEN           | 未监听             |
| 磁盘使用率         | < 70%            | > 85%           |
| 内存使用率         | < 80%            | > 90%           |
| SSL 证书有效期     | > 30 天           | < 15 天          |

---

## 17. 备份与恢复

### 17.1 数据库自动备份

创建 `/opt/energy-platform/backup.sh`：

```bash
#!/bin/bash
set -e
BACKUP_DIR="/backup"
DB_NAME="energy_platform"
RETENTION_DAYS=30

source /etc/energy-platform/env.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_${DATE}.sql.gz"
mkdir -p "$BACKUP_DIR"

echo "[$(date)] 开始备份数据库 $DB_NAME..."
mysqldump \
    --host="$DB_HOST" --port="$DB_PORT" \
    --user="$DB_USERNAME" --password="$DB_PASSWORD" \
    --single-transaction --routines --triggers --events \
    --set-gtid-purged=OFF \
    "$DB_NAME" | gzip > "$BACKUP_FILE"

if [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
    echo "[$(date)] ✅ 备份成功: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"
else
    echo "[$(date)] ❌ 备份失败！"
    exit 1
fi

# 清理旧备份
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime +$RETENTION_DAYS -delete
echo "[$(date)] 备份任务完成"
```

设置定时备份：

```bash
chmod +x /opt/energy-platform/backup.sh
# 每天凌晨 2:00 执行
(crontab -l 2>/dev/null; echo "0 2 * * * /opt/energy-platform/backup.sh >> /opt/energy-platform/logs/backup.log 2>&1") | crontab -
```

### 17.2 配置文件备份

```bash
mkdir -p /backup/configs/$(date +%Y%m%d)
cp /etc/energy-platform/env.sh /backup/configs/$(date +%Y%m%d)/
cp /etc/nginx/conf.d/energy-platform.conf /backup/configs/$(date +%Y%m%d)/
cp /etc/systemd/system/energy-platform.service /backup/configs/$(date +%Y%m%d)/
```

### 17.3 数据恢复流程

```bash
# 1. 停止后端服务
sudo systemctl stop energy-platform

# 2. 先备份当前数据库（安全起见）
mysqldump -u energy_platform -p energy_platform | gzip > /backup/energy_platform_BEFORE_RESTORE_$(date +%Y%m%d_%H%M%S).sql.gz

# 3. 解压并恢复
BACKUP_FILE="/backup/energy_platform_20260617_020000.sql.gz"
zcat "$BACKUP_FILE" | mysql -u energy_platform -p energy_platform

# 4. 重启服务
sudo systemctl start energy-platform

# 5. 验证
curl http://127.0.0.1:8080/actuator/health
```

---

## 18. 日志管理

### 18.1 日志文件位置

| 日志类型       | 路径                                          | 说明                |
| ---------- | ------------------------------------------- | ----------------- |
| 应用运行日志     | `/opt/energy-platform/logs/sys_stdout.log`  | Spring Boot 控制台输出 |
| 应用错误日志     | `/opt/energy-platform/logs/sys_stderr.log`  | 标准错误输出            |
| Nginx 访问日志 | `/var/log/nginx/energy-platform.access.log` | HTTP 请求记录         |
| Nginx 错误日志 | `/var/log/nginx/energy-platform.error.log`  | Nginx 错误信息        |
| systemd 日志 | `journalctl -u energy-platform`             | systemd 管理日志      |

### 18.2 日志轮转配置

创建 `/etc/logrotate.d/energy-platform`:

```
/opt/energy-platform/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    copytruncate
    maxsize 100M
}

/var/log/nginx/energy-platform.*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    postrotate
        /bin/kill -USR1 $(cat /var/run/nginx.pid 2>/dev/null) 2>/dev/null || true
    endscript
}
```

### 18.3 常用日志分析命令

```bash
# 查看最近的错误
sudo journalctl -u energy-platform -p err -n 50 --no-pager

# 实时跟踪
tail -f /opt/energy-platform/logs/sys_stdout.log

# 搜索特定关键词
grep -i "error\|exception\|fail" /opt/energy-platform/logs/sys_stdout.log | tail -50

# 统计 Nginx HTTP 状态码
awk '{print $9}' /var/log/nginx/energy-platform.access.log | sort | uniq -c | sort -rn

# 查看 502/503/504 错误
grep -E " 502 | 503 | 504 " /var/log/nginx/energy-platform.access.log | tail -20
```

---

## 19. 性能调优

### 19.1 JVM 调优

```ini
# /etc/systemd/system/energy-platform.service 中的 ExecStart:
-Xms4g -Xmx8g                    # 堆内存（按服务器 125GB 可设更高）
-XX:+UseG1GC                     # G1 垃圾回收器
-XX:MaxGCPauseMillis=200         # 最大 GC 暂停时间
-XX:ParallelGCThreads=4          # GC 并行线程数
-XX:InitiatingHeapOccupancyPercent=45
-XX:+HeapDumpOnOutOfMemoryError  # OOM 时自动 dump
-XX:HeapDumpPath=/opt/energy-platform/logs/
```

### 19.2 MySQL 调优

在 `/etc/my.cnf` 中：

```ini
[mysqld]
innodb_buffer_pool_size = 64G    # 设为内存的 50%-70%
innodb_buffer_pool_instances = 8
max_connections = 200
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2
innodb_log_file_size = 1G
```

### 19.3 Redis 调优

在 `/etc/redis/redis.conf` 中：

```
maxmemory 4gb
maxmemory-policy allkeys-lru
appendonly yes
appendfsync everysec
bind 127.0.0.1
```

### 19.4 Nginx 调优

```nginx
worker_processes auto;
worker_connections 2048;
gzip on;
gzip_types text/plain text/css application/json application/javascript text/xml;
```

---

## 20. 系统升级流程

### 20.1 升级前准备

```bash
# 1. 备份数据库
/opt/energy-platform/backup.sh

# 2. 备份当前 JAR
cp /opt/energy-platform/EnergyControllerPlatform-1.0.0-RELEASE.jar \
   /opt/energy-platform/EnergyControllerPlatform-1.0.0-RELEASE.jar.bak.$(date +%Y%m%d)

# 3. 确认当前状态
sudo systemctl status energy-platform --no-pager
```

### 20.2 后端升级步骤

```bash
# 1. 上传新版本 JAR 到 /opt/energy-platform/
# 2. 优雅停止服务
sudo systemctl stop energy-platform
timeout 60 bash -c 'while ss -tlnp | grep -q ":8080 "; do sleep 1; done'

# 3. 更新 systemd 服务中 JAR 文件名（如适用）
sudo vi /etc/systemd/system/energy-platform.service
sudo systemctl daemon-reload

# 4. 启动新版本
sudo systemctl start energy-platform

# 5. 验证
sleep 10
curl http://127.0.0.1:8080/actuator/health
tail -50 /opt/energy-platform/logs/sys_stdout.log | grep -i "error" || echo "✅ 无异常"

# 6. 如新版有问题 → 回滚
# sudo systemctl stop energy-platform
# 恢复旧 JAR → sudo systemctl start energy-platform
```

### 20.3 前端升级

```bash
# 备份当前前端
sudo cp -r /var/www/energy-platform /var/www/energy-platform.bak.$(date +%Y%m%d)
# 替换新前端
sudo rm -rf /var/www/energy-platform/*
sudo cp -r dist/* /var/www/energy-platform/
sudo chown -R nginx:nginx /var/www/energy-platform/
```

### 20.4 数据库迁移

```bash
# 备份 → 在低峰期执行新 SQL → 验证
/opt/energy-platform/backup.sh
mysql -u energy_platform -p energy_platform < database/migration_xxx.sql
```

---

## 21. 故障排查手册

### 21.1 后端服务无法启动

```bash
# 1. 查看详细错误
sudo journalctl -u energy-platform -n 100 --no-pager

# 2. 排查常见原因
mysql -u energy_platform -p -h 127.0.0.1 energy_platform -e "SELECT 1"  # 数据库连接
redis-cli -a "$REDIS_PASSWORD" ping                                      # Redis 连接
source /etc/energy-platform/env.sh && echo "$JWT_WEB_SECRET" | wc -c      # JWT 密钥
ss -tlnp | grep ":8080 "                                                  # 端口占用
cat /etc/energy-platform/env.sh | head -20                                # 环境变量文件
```

### 21.2 用户无法登录

```bash
# 测试登录 API
curl -X POST http://127.0.0.1:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@2026!"}'

# 检查用户状态
mysql -u energy_platform -p energy_platform -e "
  SELECT id, username, role, status FROM t_user WHERE username='admin' AND is_deleted=0;
"
```

### 21.3 表计数据不更新

```bash
# 检查厂商集成日志
tail -500 /opt/energy-platform/logs/sys_stdout.log | grep -i "chenman\|jicheng\|jiexian\|haopu"

# 积成水表：检查 TCP 端口
ss -tlnp | grep 18080

# 捷先：检查 API 可达性
source /etc/energy-platform/env.sh
curl -I "$JIEXIAN_BASE_URL" 2>/dev/null || echo "❌ 无法访问"

# 昊普：检查 webhook
curl -X POST https://mingsifuwu.com/api/v1/integration/haopu/webhook \
  -H "Content-Type: application/json" -d '[]'
```

### 21.4 Nginx 502 Bad Gateway

```bash
curl http://127.0.0.1:8080/actuator/health      # 确认后端运行
sudo nginx -t                                     # 检查 Nginx 配置
tail -30 /var/log/nginx/energy-platform.error.log # 查看错误日志
sudo systemctl restart energy-platform            # 重启后端
```

### 21.5 磁盘空间不足

```bash
df -h
du -sh /opt/energy-platform/logs/*
du -sh /data/uploads/
find /opt/energy-platform/logs/ -name "*.log" -mtime +30 -delete   # 清理旧日志
ls -t /backup/energy_platform_*.sql.gz | tail -n +11 | xargs rm -f # 清理旧备份
```

### 21.6 Redis 内存不足

```bash
redis-cli -a "$REDIS_PASSWORD" INFO memory | grep -E "used_memory_human|maxmemory_human"
# 如需调整: vi /etc/redis/redis.conf → maxmemory 改为更大值 → systemctl restart redis
```

---

## 22. 安全管理

### 22.1 定期安全审计

```bash
# 检查异常登录
mysql -u energy_platform -p energy_platform -e "
  SELECT username, operation, create_time FROM t_operation_log
  WHERE operation LIKE '%登录%' ORDER BY create_time DESC LIMIT 20;
"

# 检查最近创建的用户
mysql -u energy_platform -p energy_platform -e "
  SELECT id, username, role, create_time FROM t_user
  WHERE create_time > DATE_SUB(NOW(), INTERVAL 30 DAY) ORDER BY create_time DESC;
"

# 检查开放端口（只应有 80, 443, 18080 对外）
ss -tlnp | grep -E "0.0.0.0|:::"
```

### 22.2 防火墙配置

```bash
# firewalld (CentOS/RHEL)
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --permanent --add-port=18080/tcp
sudo firewall-cmd --reload

# ufw (Ubuntu)
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 18080/tcp
sudo ufw enable
```

> ⚠️ 严禁开放 8080、6379、3306 到公网！

### 22.3 SSL 证书管理

```bash
# 检查证书过期时间
echo | openssl s_client -servername mingsifuwu.com -connect mingsifuwu.com:443 2>/dev/null | openssl x509 -noout -dates

# Let's Encrypt 自动续期
sudo certbot --nginx -d mingsifuwu.com -d www.mingsifuwu.com
sudo systemctl enable certbot-renew.timer
```

---

## 23. 数据库维护

### 23.1 定期优化（每月执行，建议凌晨低峰期）

```sql
-- 查看表大小
SELECT table_name,
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS total_mb
FROM information_schema.tables
WHERE table_schema = 'energy_platform'
ORDER BY total_mb DESC LIMIT 20;

-- 优化表（回收碎片）
OPTIMIZE TABLE t_meter_reading;
OPTIMIZE TABLE t_operation_log;
```

### 23.2 慢查询分析

```bash
mysql -u energy_platform -p energy_platform -e "
  SELECT start_time, query_time, rows_examined, LEFT(sql_text, 200) AS sql_preview
  FROM mysql.slow_log WHERE db = 'energy_platform'
  ORDER BY query_time DESC LIMIT 20;
" 2>/dev/null
```

### 23.3 数据归档建议

长期运行后，对 `t_meter_reading` 和 `t_operation_log` 等增长较大的表，建议定期将 1 年前的数据迁移到归档表。

---

## 24. 应急响应流程

### 24.1 服务完全宕机

```
① 通知相关人员
② 快速诊断: ping 服务器 → systemctl status mysql redis nginx energy-platform → ss -tlnp
③ 按顺序重启: MySQL → Redis → 后端应用 → Nginx
④ 验证恢复: curl /actuator/health → 浏览器访问首页
⑤ 分析根因: 检查系统日志、应用日志、磁盘/内存状态
⑥ 事后总结
```

### 24.2 数据库损坏

```
① sudo systemctl stop energy-platform
② mysql -u root -p -e "CHECK TABLE t_meter, t_customer, t_user, t_bill, t_meter_reading;"
③ 如有损坏: REPAIR TABLE <表名>;
④ 如修复失败: 从最近备份恢复（参考第 17.3 节）
⑤ 重启服务并验证
```

### 24.3 安全事件响应

```
① 立即隔离: 暂停受影响账户，必要时临时关闭公网访问
② 收集证据: 导出操作日志、Nginx 访问日志、检查异常登录
③ 评估影响: 确定被访问/修改的数据范围
④ 修复加固: 重置所有密码、更新 JWT 密钥、修复漏洞
⑤ 事后报告: 记录事件经过、制定防范措施
```

---

---

## 附录 A: 快速部署检查表

部署完成后逐项确认:

- [ ] MySQL 数据库 `energy_platform` 已创建，25+ 张表已建好
- [ ] Redis 已启动且设置了密码
- [ ] 环境变量文件 `/etc/energy-platform/env.sh` 已配置并加载
- [ ] JAR 包已上传到 `/opt/energy-platform/`
- [ ] systemd 服务已配置并启动
- [ ] Nginx 已配置 SSL 证书并重载
- [ ] 防火墙已开放 80/443/18080 端口
- [ ] 可通过 `https://mingsifuwu.com` 访问登录页
- [ ] 管理员账号可正常登录
- [ ] 文件上传功能正常
- [ ] 各厂商数据拉取/推送正常（如有接入）
- [ ] 监控脚本 `monitor.sh` 已加入 crontab
- [ ] 备份脚本 `backup.sh` 已加入 crontab
- [ ] 日志轮转已配置

## 附录 B: 运维命令速查表

### 服务管理

| 操作       | 命令                                       |
| -------- | ---------------------------------------- |
| 启动后端     | `sudo systemctl start energy-platform`   |
| 停止后端     | `sudo systemctl stop energy-platform`    |
| 重启后端     | `sudo systemctl restart energy-platform` |
| 查看状态     | `sudo systemctl status energy-platform`  |
| 查看实时日志   | `sudo journalctl -u energy-platform -f`  |
| 重载 Nginx | `sudo systemctl reload nginx`            |
| 重启 MySQL | `sudo systemctl restart mysql`           |
| 重启 Redis | `sudo systemctl restart redis`           |

### 应用诊断

| 操作         | 命令                                           |
| ---------- | -------------------------------------------- |
| 健康检查       | `curl http://127.0.0.1:8080/actuator/health` |
| 检查端口       | `ss -tlnp \| grep -E "8080\|18080\|80\|443"` |
| 查看 Java 进程 | `ps aux \| grep java`                        |
| 查看磁盘       | `df -h`                                      |
| 查看内存       | `free -h`                                    |

### 数据库操作

| 操作    | 命令                                                   |
| ----- | ---------------------------------------------------- |
| 数据库备份 | `/opt/energy-platform/backup.sh`                     |
| 查看表大小 | 见第 23.1 节 SQL                                        |
| 查看连接数 | `mysql -u energy_platform -p -e "SHOW PROCESSLIST;"` |

## 附录 C: 数据库表结构速查

| 表名                  | 说明    | 关键字段                                     |
| ------------------- | ----- | ---------------------------------------- |
| `t_user`            | 用户表   | username, password, role, status         |
| `t_role`            | 角色权限表 | role_name, role_code, permissions(JSON)  |
| `t_project`         | 项目表   | project_name, project_code               |
| `t_customer`        | 商户档案表 | customer_code, customer_name, balance    |
| `t_meter`           | 表计主表  | meter_code, vender, meter_type, steal_no |
| `t_meter_reading`   | 读数记录表 | meter_id, reading_value, reading_time    |
| `t_collector`       | 采集器表  | collector_code, vender                   |
| `t_price_strategy`  | 费率策略表 | strategy_name, unit_price                |
| `t_bill`            | 账单表   | customer_id, bill_amount, bill_status    |
| `t_charge_record`   | 充值记录表 | meter_id, charge_amount                  |
| `t_account_flow`    | 账户流水表 | customer_id, amount, flow_type           |
| `t_refund`          | 退款表   | bill_id, refund_amount                   |
| `t_meter_group`     | 表计分组表 | group_name, parent_id                    |
| `t_operation_log`   | 操作日志表 | username, operation, create_time         |
| `t_file_record`     | 文件记录表 | file_name, file_path, storage_type       |
| `t_daily_report`    | 日报表   | report_date, total_consumption           |
| `t_monthly_summary` | 月度汇总表 | summary_month, total_amount              |

## 附录 D: 环境变量完整清单

详见第 3 节 `env.sh` 完整模板。

**必填项速查（8 个核心变量）**:

```bash
DB_HOST          # MySQL 主机地址
DB_PORT          # MySQL 端口
DB_NAME          # 数据库名称
DB_USERNAME      # 数据库用户名
DB_PASSWORD      # 数据库密码
REDIS_HOST       # Redis 主机地址
REDIS_PORT       # Redis 端口
REDIS_PASSWORD   # Redis 密码
```

## 附录 E: 紧急联系方式

| 角色     | 联系方式    | 负责范围          |
| ------ | ------- | ------------- |
| 系统管理员  | ⬅️ 甲方填写 | 服务器/网络/系统     |
| 数据库管理员 | ⬅️ 甲方填写 | 数据库维护         |
| 业务负责人  | ⬅️ 甲方填写 | 业务决策          |
| 开发技术支持 | ⬅️ 甲方填写 | 代码级问题         |
| 辰鳗技术支持 | 辰鳗平台客服  | 电表数据问题        |
| 积成技术支持 | 积成电子客服  | 水表集中器问题       |
| 捷先技术支持 | 捷先客服    | 水表数据问题        |
| 昊普技术支持 | 昊普客服    | 电表 webhook 问题 |

---

> **📌 文档使用说明**: 
> 
> - **初次部署**: 从第 1 节开始按顺序执行，每节结束后验证。
> - **日常运维**: 重点参考第 15-18 节（每日/周/月检查、监控、备份、日志）。
> - **故障处理**: 直接跳转到第 21 节对应症状排查。
> - **系统升级**: 参考第 20 节升级流程。
> - 如遇到文档未覆盖的问题，请提供系统版本、Java 版本、应用日志以便排查。
