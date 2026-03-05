# 供应链金融平台

基于 FISCO-BCOS 区块链的应收账款通证化融资平台

## 项目概述

本项目是一个面向制造业供应链的数字化金融服务平台，通过区块链技术实现应收账款的通证化、确权、流转和融资。平台连接核心企业、供应商和金融机构，解决中小企业融资难、融资贵的问题。

### 核心价值

- **信用穿透**：核心企业信用通过区块链向供应链上游传递
- **资产数字化**：应收账款转化为可拆分、可流转的数字通证
- **智能风控**：基于多维度数据的自动化风险评估
- **全程可追溯**：所有交易记录上链，不可篡改

## 技术架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3 + Element Plus)               │
│                         http://localhost:5173                    │
└─────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────┐
│                     后端 (Spring Boot 2.7)                       │
│                      http://localhost:8080/api                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐    │
│  │Controller│→│ Service  │→│Repository│→│   MySQL DB   │    │
│  └──────────┘  └────┬─────┘  └──────────┘  └──────────────┘    │
│                     │                                            │
│                     ▼                                            │
│              ┌─────────────┐                                     │
│              │Blockchain   │                                     │
│              │Service      │                                     │
│              └──────┬──────┘                                     │
└─────────────────────┼───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                   FISCO-BCOS 区块链网络                          │
│                    4节点联盟链 (127.0.0.1)                        │
│  ┌────────────────┐  ┌────────────────────┐                     │
│  │ AccessControl  │  │  ReceivablesToken  │                     │
│  │    企业管理     │  │   通证与融资管理    │                     │
│  └────────────────┘  └────────────────────┘                     │
└─────────────────────────────────────────────────────────────────┘
```

### 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + Vite | 3.4.0 / 5.0.10 |
| UI组件库 | Element Plus | 2.4.4 |
| 状态管理 | Pinia | 2.1.7 |
| 图表库 | ECharts | 5.4.3 |
| 后端框架 | Spring Boot | 2.7.18 |
| 安全框架 | Spring Security + JWT | 0.11.5 |
| 持久层 | Spring Data JPA + MySQL | 8.0 |
| 区块链 | FISCO-BCOS | 2.11.0 |
| 区块链SDK | fisco-bcos-java-sdk | 2.9.2 |
| Java版本 | OpenJDK | 11 (必须) |

## 目录结构

```
制造企业/
├── README.md                         # 项目文档
├── backend/                          # Spring Boot 后端
│   ├── pom.xml                       # Maven 依赖配置
│   ├── config.toml                   # FISCO-BCOS SDK 配置
│   ├── fisco/conf/                   # SDK 证书
│   ├── abi/                          # 合约 ABI
│   ├── bin/                          # 合约 BIN
│   └── src/main/
│       ├── java/com/scf/
│       │   ├── ScfApplication.java   # 启动类
│       │   ├── config/               # 配置类
│       │   │   ├── FiscoConfig.java
│       │   │   └── SecurityConfig.java
│       │   ├── controller/           # REST 控制器
│       │   │   ├── AuthController.java
│       │   │   ├── EnterpriseController.java
│       │   │   ├── ReceivableController.java
│       │   │   ├── FinancingController.java
│       │   │   ├── TransferController.java
│       │   │   └── DashboardController.java
│       │   ├── service/              # 业务服务
│       │   │   ├── UserService.java
│       │   │   ├── EnterpriseService.java
│       │   │   ├── ReceivableService.java
│       │   │   ├── RiskService.java
│       │   │   ├── TransferService.java
│       │   │   └── DashboardService.java
│       │   ├── repository/           # 数据访问
│       │   ├── entity/               # JPA 实体
│       │   │   ├── User.java
│       │   │   ├── Enterprise.java
│       │   │   ├── Receivable.java
│       │   │   ├── FinancingRecord.java
│       │   │   └── TransferRecord.java
│       │   ├── dto/                  # 数据传输对象
│       │   │   ├── RequestDTO.java
│       │   │   └── ResponseDTO.java
│       │   ├── blockchain/           # 区块链服务
│       │   │   └── BlockchainService.java
│       │   ├── security/             # 安全组件
│       │   │   ├── JwtTokenProvider.java
│       │   │   ├── JwtAuthenticationFilter.java
│       │   │   └── SecurityUtils.java
│       │   └── exception/            # 异常处理
│       └── resources/
│           ├── application.yml       # 应用配置
│           ├── fisco/                # FISCO 配置
│           ├── abi/                  # 合约接口
│           └── bin/                  # 合约字节码
│
├── frontend/                         # Vue 3 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── main.js                   # 入口文件
│       ├── App.vue                   # 根组件
│       ├── router/                   # 路由配置
│       ├── store/                    # Pinia 状态
│       ├── views/                    # 页面组件
│       │   ├── Login.vue
│       │   ├── Layout.vue
│       │   ├── Dashboard.vue
│       │   ├── Enterprises.vue
│       │   ├── Receivables.vue
│       │   ├── ReceivableDetail.vue
│       │   ├── Financing.vue
│       │   └── Transfer.vue
│       ├── api/                      # API 封装
│       └── utils/                    # 工具函数
│
└── blockchain/                       # 智能合约
    └── contracts/
        ├── AccessControl.sol         # 企业访问控制
        ├── ReceivablesToken.sol      # 应收账款通证
        └── Settlement.sol            # 结算管理
```

## 核心功能

### 1. 企业管理

| 角色 | 说明 | 默认信用分 |
|------|------|-----------|
| CORE_ENTERPRISE | 核心企业，应收账款签发方 | 850 |
| SUPPLIER | 供应商，应收账款持有方 | 700 |
| FINANCIAL_INST | 金融机构，融资提供方 | 900 |
| REGULATOR | 监管机构 | - |

### 2. 应收账款通证

**通证状态流转**：
```
CREATED → CONFIRMED → FINANCED/PARTIALLY_FINANCED → SETTLED
                ↓
            CANCELLED
```

**核心操作**：
- **签发 (Issue)**：核心企业向供应商签发应收账款通证
- **确权 (Confirm)**：通证在区块链上确认
- **拆分 (Split)**：将大额通证拆分为多个小额通证
- **转让 (Transfer)**：供应商之间转让通证
- **融资 (Financing)**：以通证为质押申请融资

### 3. 融资管理

**融资流程**：
```
供应商申请 → 金融机构审批 → 放款 → 到期还款
  PENDING      APPROVED      DISBURSED   REPAID
```

**风险评估因子**：
| 因子 | 权重 | 说明 |
|------|------|------|
| 签发方信用分 | 40% | 核心企业信用评级 |
| 持有方信用分 | 30% | 供应商信用评级 |
| 到期天数 | 20% | 距离到期日天数 |
| 面值金额 | 10% | 通证面值大小 |

**利率限制**：
- 基准利率：6%
- 最高利率：8%
- 最低信用分要求：600

### 4. 区块链存证

所有关键操作均上链存证：
- 企业注册与信用评分更新
- 通证签发、转让、拆分
- 融资申请、审批、放款

## 数据库设计

### ER 关系图

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│     User     │       │  Enterprise  │       │  Receivable  │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id           │       │ id           │       │ id           │
│ username     │   ┌──→│ creditCode   │←──┐   │ tokenId      │
│ password     │   │   │ name         │   │   │ parentTokenId│
│ realName     │   │   │ role         │   │   │ issuer_id ───┼──┐
│ enterprise_id├───┘   │ creditScore  │   │   │ holder_id ───┼──┤
│ role         │       │ blockchain   │   │   │ faceValue    │  │
└──────────────┘       │ Address      │   │   │ status       │  │
                       └──────────────┘   │   └──────────────┘  │
                              ▲           │          │          │
                              │           │          │          │
┌──────────────┐              │           │          ▼          │
│TransferRecord│              │           │   ┌──────────────┐  │
├──────────────┤              │           │   │FinancingRecord│ │
│ id           │              │           │   ├──────────────┤  │
│ from_ent_id ─┼──────────────┤           │   │ id           │  │
│ to_ent_id ───┼──────────────┘           │   │ receivable_id│  │
│ amount       │                          │   │ financier_id─┼──┘
│ type         │                          └───┤ borrower_id  │
│ txHash       │                              │ amount       │
└──────────────┘                              │ status       │
                                              └──────────────┘
```

### 主要表结构

#### enterprise (企业表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| credit_code | VARCHAR(18) | 统一社会信用代码 |
| name | VARCHAR(100) | 企业名称 |
| role | ENUM | 企业角色 |
| credit_score | INT | 信用评分 (0-1000) |
| blockchain_address | VARCHAR(42) | 区块链账户地址 |

#### receivable (应收账款表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| token_id | BIGINT | 区块链通证ID |
| parent_token_id | BIGINT | 父通证ID (拆分时) |
| issuer_id | BIGINT | 签发企业ID |
| holder_id | BIGINT | 持有企业ID |
| face_value | DECIMAL(18,2) | 面值 (元) |
| remaining_value | DECIMAL(18,2) | 剩余可用金额 |
| maturity_date | DATE | 到期日 |
| annual_rate | DECIMAL(6,4) | 年化利率 |
| status | ENUM | 通证状态 |
| tx_hash | VARCHAR(66) | 区块链交易哈希 |

#### financing_record (融资记录表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| record_id | BIGINT | 区块链记录ID |
| receivable_id | BIGINT | 关联通证ID |
| financier_id | BIGINT | 金融机构ID |
| borrower_id | BIGINT | 借款企业ID |
| financing_amount | DECIMAL(18,2) | 融资金额 |
| annual_rate | DECIMAL(6,4) | 融资利率 |
| discount_amount | DECIMAL(18,2) | 贴现金额 |
| actual_amount | DECIMAL(18,2) | 实际到账金额 |
| status | ENUM | 融资状态 |

## API 接口

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 用户登录 |
| POST | /api/auth/register | 用户注册 |

### 企业接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/enterprises | 获取企业列表 |
| GET | /api/enterprises/{id} | 获取企业详情 |
| POST | /api/enterprises | 注册企业 |
| GET | /api/enterprises/role/{role} | 按角色筛选企业 |
| PUT | /api/enterprises/{id}/credit-score | 更新信用评分 |

### 应收账款接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/receivables/issue | 签发通证 |
| POST | /api/receivables/split | 拆分通证 |
| POST | /api/receivables/transfer | 转让通证 |
| GET | /api/receivables/my/holding | 我持有的通证 |
| GET | /api/receivables/my/issued | 我签发的通证 |
| GET | /api/receivables/{id}/detail | 通证详情 |
| GET | /api/receivables/{id}/risk-assessment | 风险评估 |

### 融资接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/financing/apply | 申请融资 |
| POST | /api/financing/approve | 审批融资 |
| POST | /api/financing/{id}/disburse | 放款 |
| GET | /api/financing/my/borrowing | 我的借款 |
| GET | /api/financing/my/lending | 我的放款 |
| GET | /api/financing/my/pending | 待审批融资 |

### 统计接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/dashboard/stats | 平台统计 |
| GET | /api/dashboard/my/stats | 我的统计 |

## 智能合约

### AccessControl.sol

企业访问控制与信用管理合约。

```solidity
// 企业角色常量
bytes32 public constant ROLE_CORE_ENTERPRISE = "CORE_ENTERPRISE";
bytes32 public constant ROLE_SUPPLIER = "SUPPLIER";
bytes32 public constant ROLE_FINANCIAL_INST = "FINANCIAL_INST";

// 核心函数
function registerEnterprise(address, string name, string creditCode, bytes32 role);
function updateCreditScore(address, uint256 newScore);
function getEnterprise(address) returns (string, string, bytes32, uint256, bool);
function hasRole(address, bytes32 role) returns (bool);
```

### ReceivablesToken.sol

应收账款通证与融资管理合约。

```solidity
// 通证结构
struct Token {
    uint256 tokenId;
    uint256 parentTokenId;
    address issuer;
    address holder;
    uint256 faceValue;
    uint256 remainingValue;
    uint256 maturityDate;
    uint256 annualRate;
    string contractHash;
    uint8 status;      // 0:CREATED, 1:CONFIRMED, 2:FINANCED, 3:PARTIALLY_FINANCED, 4:SETTLED
    uint256 createdAt;
}

// 融资记录结构
struct FinancingRecord {
    uint256 recordId;
    uint256 tokenId;
    address borrower;
    address financier;
    uint256 financingAmount;
    uint256 annualRate;
    uint256 discountAmount;
    uint256 actualAmount;
    uint8 status;      // 0:PENDING, 1:APPROVED, 2:REJECTED, 3:DISBURSED
    uint256 createdAt;
}

// 核心函数
function issueToken(address holder, uint256 faceValue, uint256 maturityDate,
                    uint256 annualRate, string contractHash) returns (uint256);
function confirmToken(uint256 tokenId);
function transferToken(uint256 tokenId, address to);
function applyFinancing(uint256 tokenId, address financier,
                        uint256 amount, uint256 rate) returns (uint256);
function approveFinancing(uint256 recordId, bool approved);
function disburseFinancing(uint256 recordId);
```

### 已部署合约地址

| 合约 | 地址 |
|------|------|
| AccessControl | `0x8fc93985af95350f7a40851d0206f59147492788` |
| ReceivablesToken | `0x26b7a2b15a84429175873d81fac12f2910d3380c` |

## 部署指南

### 环境要求

- **操作系统**：Linux (WSL2 Ubuntu 推荐)
- **Java**：OpenJDK 11 (必须，JDK 17+ 不兼容 FISCO-BCOS)
- **Node.js**：18.x 或更高
- **MySQL**：8.0
- **Maven**：3.8+

### 1. 启动 FISCO-BCOS 区块链

```bash
# 启动4节点区块链网络
cd ~/fisco/nodes/127.0.0.1
bash start_all.sh

# 检查节点状态
bash check_status.sh
# 应显示: node0 is running, node1 is running, ...

# 检查共识状态
tail -f node0/log/* | grep "g]"
# 应显示: Sealer=... 且 connected nodes 应为 3
```

### 2. 配置数据库

```sql
-- 创建数据库
CREATE DATABASE scf_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用默认配置 (root/123456)
-- 或修改 application.yml 中的数据库配置
```

### 3. 启动后端服务

```bash
cd /mnt/e/dapp/制造企业/backend

# 使用 JDK 11 启动 (重要！JDK 17+ 会导致 secp256k1 兼容性问题)
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 \
/path/to/maven/bin/mvn spring-boot:run

# 例如:
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 \
/mnt/d/ProgramFiles/apache-maven-3.9.9/bin/mvn spring-boot:run
```

后端启动成功日志：
```
ssl handshake success 127.0.0.1:20200
ssl handshake success 127.0.0.1:20201
FISCO-BCOS SDK initialized successfully
区块链服务初始化完成
Started ScfApplication in XX seconds
Tomcat started on port(s): 8080 (http) with context path '/api'
```

### 4. 启动前端服务

```bash
cd /mnt/e/dapp/制造企业/frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端访问地址：`http://localhost:5173`

### 5. 验证部署

```bash
# 测试后端健康检查
curl http://localhost:8080/api/dashboard/stats

# 测试区块链连接
curl http://localhost:8080/api/enterprises
```

## 配置说明

### 后端配置 (application.yml)

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/scf_db
    username: root
    password: "123456"

fisco:
  enabled: true
  group-id: 1
  contracts:
    access-control: "0x8fc93985af95350f7a40851d0206f59147492788"
    receivables-token: "0x26b7a2b15a84429175873d81fac12f2910d3380c"

jwt:
  secret: your-secret-key
  expiration: 86400000  # 24小时

risk:
  base-rate: 0.06       # 基准利率 6%
  max-rate: 0.08        # 最高利率 8%
  min-credit-score: 600
```

### FISCO-BCOS SDK 配置 (config.toml)

```toml
[cryptoMaterial]
certPath = "fisco/conf"

[network]
peers=["127.0.0.1:20200", "127.0.0.1:20201"]

[account]
keyStoreDir = "account"
accountFileFormat = "pem"

[threadPool]
maxBlockingQueueSize = "102400"
```

### 工作目录结构

后端运行时需要以下目录结构（相对于工作目录）：

```
backend/
├── config.toml          # SDK 配置文件
├── fisco/conf/          # SDK 证书
│   ├── ca.crt
│   ├── sdk.crt
│   └── sdk.key
├── abi/                 # 合约 ABI 文件
│   ├── AccessControl.abi
│   └── ReceivablesToken.abi
├── bin/                 # 合约 BIN 文件
│   ├── AccessControl.bin
│   └── ReceivablesToken.bin
└── account/             # 账户密钥存储
```

## 常见问题

### Q1: JDK 版本兼容性问题

**错误**：`secp256k1 algorithm is disabled by the current jdk`

**原因**：JDK 17+ 禁用了 secp256k1 椭圆曲线算法，而 FISCO-BCOS 使用该算法进行 SSL 握手。

**解决**：必须使用 JDK 11
```bash
# 安装 JDK 11
sudo apt install openjdk-11-jdk-headless

# 启动时指定 JAVA_HOME
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 mvn spring-boot:run
```

### Q2: 区块链连接失败

**错误**：`Failed to connect to all the nodes`

**排查步骤**：
1. 确认节点已启动：`ps aux | grep fisco-bcos`
2. 检查端口监听：`netstat -tlnp | grep 20200`
3. 检查防火墙：`sudo ufw status`
4. 验证证书文件存在：`ls backend/fisco/conf/`

### Q3: 证书/ABI加载失败

**错误**：`Parameter 'directory' is not a directory`

**解决**：确保工作目录下存在 `abi/` 和 `bin/` 目录
```bash
cd backend
mkdir -p abi bin
cp src/main/resources/abi/*.abi abi/
cp src/main/resources/bin/*.bin bin/
```

### Q4: SSL 握手失败

**错误**：`General OpenSslEngine problem`

**解决**：
1. 检查证书是否正确复制
2. 验证证书链：`openssl verify -CAfile fisco/conf/ca.crt fisco/conf/sdk.crt`
3. 确保使用 JDK 11

### Q5: 数据库连接失败

**错误**：`Communications link failure`

**解决**：
1. 确认 MySQL 服务运行：`sudo service mysql status`
2. 检查连接参数：用户名、密码、数据库名
3. 创建数据库：`CREATE DATABASE scf_db`

## 测试账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | 123456 | 系统管理员 |
| 核心企业 | core_admin | 123456 | 可签发通证 |
| 供应商 | supplier_admin | 123456 | 可申请融资 |
| 金融机构 | bank_admin | 123456 | 可审批放款 |

## 业务流程示例

### 完整融资流程

```
1. 核心企业签发通证
   POST /api/receivables/issue
   {
     "holderId": 2,           // 供应商ID
     "faceValue": 1000000,    // 100万元
     "maturityDate": "2026-06-01",
     "annualRate": 0.05,      // 5%
     "description": "采购订单#12345"
   }

2. 供应商查看持有通证
   GET /api/receivables/my/holding

3. 供应商获取风险评估
   GET /api/receivables/{id}/risk-assessment
   返回: 建议利率、风险等级、最大融资额

4. 供应商申请融资
   POST /api/financing/apply
   {
     "tokenId": 1,
     "financierId": 3,        // 金融机构ID
     "amount": 800000         // 申请80万
   }

5. 金融机构审批
   POST /api/financing/approve
   {
     "financingId": 1,
     "approved": true,
     "rate": 0.065            // 6.5%利率
   }

6. 金融机构放款
   POST /api/financing/{id}/disburse

7. 供应商收到资金
   实际到账 = 融资金额 - 贴现金额
   贴现金额 = 融资金额 × 利率 × 剩余天数/365
```

## 项目统计

| 指标 | 数量 |
|------|------|
| 后端代码行数 | ~3,000 |
| 前端代码行数 | ~2,000 |
| 智能合约代码行数 | ~400 |
| REST API 端点 | 40+ |
| 数据库表 | 5 |
| Vue 页面组件 | 8 |

## 许可证

MIT License

---

**最后更新**: 2026-02-02
# fisco-scf
