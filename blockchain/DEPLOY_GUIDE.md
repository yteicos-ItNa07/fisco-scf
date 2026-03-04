# FISCO-BCOS 区块链网络部署指南

## 环境要求

- WSL2 (Ubuntu 20.04/22.04)
- Docker Desktop for Windows (启用 WSL2 后端)
- curl, openssl

## 一、WSL2 环境准备

### 1.1 启用 Windows 功能

在 Windows 中启用以下功能（控制面板 → 程序和功能 → 启用或关闭 Windows 功能）：
- Hyper-V
- 适用于 Linux 的 Windows 子系统
- 虚拟机平台

### 1.2 安装 WSL2

```powershell
# PowerShell 管理员模式
wsl --install -d Ubuntu-22.04
wsl --set-version Ubuntu-22.04 2
```

### 1.3 安装 Docker Desktop

1. 下载安装 Docker Desktop for Windows
2. 设置 → Resources → WSL Integration → 启用 Ubuntu-22.04
3. 重启 Docker Desktop

### 1.4 WSL2 中验证 Docker

```bash
docker --version
docker run hello-world
```

## 二、部署 FISCO-BCOS 网络

### 2.1 创建工作目录

```bash
cd ~ && mkdir -p fisco && cd fisco
```

### 2.2 下载构建脚本 (FISCO-BCOS 2.x)

```bash
# FISCO-BCOS 2.11.0 (与你的 Java SDK 版本匹配)
curl -#LO https://github.com/FISCO-BCOS/FISCO-BCOS/releases/download/v2.11.0/build_chain.sh
chmod u+x build_chain.sh
```

### 2.3 使用 Docker 模式构建 4 节点网络

```bash
# -d 参数表示 Docker 模式
# -l 127.0.0.1:4 表示在本机部署 4 个节点
# -p 指定端口起始值: P2P端口30300, Channel端口20200, RPC端口8545
bash build_chain.sh -d -l 127.0.0.1:4 -p 30300,20200,8545

# 成功后会显示:
# [INFO] Generate ca cert successfully!
# [INFO] Generate node0 cert successfully!
# ...
# [INFO] Generate docker-compose.yaml for 127.0.0.1 successfully!
# [INFO] All completed.
```

### 2.4 启动节点

```bash
# 启动所有节点
bash nodes/127.0.0.1/start_all.sh

# 首次启动会自动下载 Docker 镜像 fiscoorg/fiscobcos:v2.11.0
# 等待几分钟...

# 查看节点状态
docker ps | grep fisco
```

### 2.5 验证节点运行

```bash
# 检查节点日志，确认共识正常
tail -f nodes/127.0.0.1/node0/log/log* | grep "g]+"

# 正常输出类似:
# info|2026-01-31 10:00:00|[g:1][CONSENSUS][SEALER]++++++++Generating
```

### 2.6 停止/重启节点

```bash
# 停止所有节点
bash nodes/127.0.0.1/stop_all.sh

# 重启所有节点
bash nodes/127.0.0.1/start_all.sh
```

## 三、部署智能合约

### 3.1 下载控制台

```bash
cd ~/fisco
curl -#LO https://github.com/FISCO-BCOS/console/releases/download/v2.9.2/console.tar.gz
tar -xzf console.tar.gz
cd console
```

### 3.2 配置控制台

```bash
# 复制节点证书到控制台
cp -r ~/fisco/nodes/127.0.0.1/sdk/* conf/

# 修改配置文件
cp conf/config-example.toml conf/config.toml
```

编辑 `conf/config.toml`，修改节点连接地址：
```toml
[network]
peers=["127.0.0.1:20200", "127.0.0.1:20201", "127.0.0.1:20202", "127.0.0.1:20203"]
```

### 3.3 复制合约文件

```bash
# 将项目中的合约复制到控制台
cp /mnt/e/dapp/制造企业/blockchain/contracts/*.sol contracts/solidity/
```

### 3.4 启动控制台

```bash
bash start.sh

# 进入控制台后可以执行:
# 查看区块高度
getBlockNumber

# 部署合约
deploy AccessControl
deploy ReceivablesToken
deploy Settlement
```

### 3.5 记录合约地址

部署成功后，记录每个合约的地址，更新到 `application.yml`:

```yaml
fisco:
  contracts:
    access-control: "0x..."
    receivables-token: "0x..."
    settlement: "0x..."
```

## 四、配置后端连接

### 4.1 复制证书到后端

```bash
# 创建证书目录
mkdir -p /mnt/e/dapp/制造企业/backend/src/main/resources/fisco/conf

# 复制 SDK 证书
cp ~/fisco/nodes/127.0.0.1/sdk/* /mnt/e/dapp/制造企业/backend/src/main/resources/fisco/conf/
```

### 4.2 创建 config.toml

在 `backend/src/main/resources/` 下创建 `config.toml`:

```toml
[cryptoMaterial]
certPath = "fisco/conf"
useSMCrypto = "false"

[network]
peers = ["127.0.0.1:20200", "127.0.0.1:20201"]

[account]
keyStoreDir = "account"
accountFilePath = ""
accountFileFormat = "pem"
accountAddress = ""
password = ""

[threadPool]
channelProcessorThreadSize = "16"
receiptProcessorThreadSize = "16"
maxBlockingQueueSize = "102400"
```

### 4.3 启动后端验证

```bash
cd /mnt/e/dapp/制造企业/backend
mvn spring-boot:run
```

## 五、常用命令

```bash
# 查看 Docker 容器
docker ps | grep fisco

# 查看节点日志
docker logs -f node0

# 进入节点容器
docker exec -it node0 /bin/bash

# 检查区块高度
cd ~/fisco/console && bash start.sh -l
```

## 六、故障排查

### 问题1: 端口被占用
```bash
# 检查端口
netstat -tlnp | grep 30300
# 停止占用进程或修改 build_chain.sh 的 -p 参数
```

### 问题2: Docker 镜像下载失败
```bash
# 手动拉取镜像
docker pull fiscoorg/fiscobcos:v2.11.0
```

### 问题3: WSL2 网络不通
```bash
# 检查 WSL2 网络
ip addr show eth0
# 使用 127.0.0.1 而非 localhost
```

## 参考链接

- [FISCO-BCOS 官方文档](https://fisco-bcos-documentation.readthedocs.io/zh-cn/latest/)
- [Docker 部署教程](https://fisco-bcos-documentation.readthedocs.io/zh-cn/latest/docs/tutorial/docker.html)
- [WSL2 部署教程](https://fisco-bcos-documentation.readthedocs.io/zh-cn/latest/docs/articles/7_practice/build_chain_with_wsl_on_windows.html)
