#!/bin/bash

# FISCO-BCOS 快速部署脚本
# 适用于 WSL2 + Docker 环境

set -e

echo "============================================"
echo "FISCO-BCOS 区块链网络部署脚本"
echo "============================================"

# 配置
FISCO_VERSION="v2.11.0"
WORK_DIR=~/fisco
NODES_DIR=$WORK_DIR/nodes/127.0.0.1
PROJECT_DIR="/mnt/e/dapp/制造企业"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

# 检查 Docker
check_docker() {
    info "检查 Docker 环境..."
    if ! command -v docker &> /dev/null; then
        error "Docker 未安装，请先安装 Docker Desktop 并启用 WSL2 集成"
    fi

    if ! docker info &> /dev/null; then
        error "Docker 服务未运行，请启动 Docker Desktop"
    fi

    info "Docker 环境正常"
}

# 安装依赖
install_deps() {
    info "安装依赖..."
    sudo apt-get update
    sudo apt-get install -y curl openssl
}

# 下载 build_chain.sh
download_script() {
    info "创建工作目录..."
    mkdir -p $WORK_DIR
    cd $WORK_DIR

    if [ ! -f "build_chain.sh" ]; then
        info "下载 build_chain.sh (版本: $FISCO_VERSION)..."
        curl -#LO https://github.com/FISCO-BCOS/FISCO-BCOS/releases/download/$FISCO_VERSION/build_chain.sh
        chmod u+x build_chain.sh
    else
        info "build_chain.sh 已存在"
    fi
}

# 构建网络
build_network() {
    cd $WORK_DIR

    if [ -d "nodes" ]; then
        warn "节点目录已存在，是否重新构建？(y/n)"
        read -r answer
        if [ "$answer" != "y" ]; then
            info "跳过网络构建"
            return
        fi
        rm -rf nodes
    fi

    info "使用 Docker 模式构建 4 节点网络..."
    bash build_chain.sh -d -l 127.0.0.1:4 -p 30300,20200,8545

    if [ $? -eq 0 ]; then
        info "网络构建成功!"
    else
        error "网络构建失败"
    fi
}

# 启动节点
start_nodes() {
    info "启动节点..."
    cd $WORK_DIR

    if [ ! -d "nodes" ]; then
        error "节点目录不存在，请先构建网络"
    fi

    bash nodes/127.0.0.1/start_all.sh

    info "等待节点启动..."
    sleep 10

    # 检查节点状态
    running_nodes=$(docker ps | grep -c fisco || true)
    info "运行中的节点数: $running_nodes"

    if [ "$running_nodes" -eq 4 ]; then
        info "所有节点启动成功!"
    else
        warn "部分节点可能未启动，请检查 docker ps"
    fi
}

# 停止节点
stop_nodes() {
    info "停止所有节点..."
    cd $WORK_DIR

    if [ -f "nodes/127.0.0.1/stop_all.sh" ]; then
        bash nodes/127.0.0.1/stop_all.sh
        info "节点已停止"
    else
        warn "stop_all.sh 不存在"
    fi
}

# 下载控制台
download_console() {
    cd $WORK_DIR

    if [ -d "console" ]; then
        info "控制台已存在"
        return
    fi

    info "下载控制台..."
    curl -#LO https://github.com/FISCO-BCOS/console/releases/download/v2.9.2/console.tar.gz
    tar -xzf console.tar.gz
    rm console.tar.gz

    # 配置控制台
    info "配置控制台..."
    cp -r $NODES_DIR/sdk/* console/conf/
    cp console/conf/config-example.toml console/conf/config.toml

    info "控制台配置完成"
}

# 复制合约到控制台
copy_contracts() {
    info "复制合约文件..."

    if [ ! -d "$PROJECT_DIR/blockchain/contracts" ]; then
        error "合约目录不存在: $PROJECT_DIR/blockchain/contracts"
    fi

    cp $PROJECT_DIR/blockchain/contracts/*.sol $WORK_DIR/console/contracts/solidity/

    info "合约文件已复制到控制台"
}

# 复制证书到后端
copy_certs_to_backend() {
    info "复制证书到后端项目..."

    BACKEND_CONF="$PROJECT_DIR/backend/src/main/resources/fisco/conf"
    mkdir -p $BACKEND_CONF

    if [ -d "$NODES_DIR/sdk" ]; then
        cp $NODES_DIR/sdk/* $BACKEND_CONF/
        info "证书已复制到: $BACKEND_CONF"
    else
        error "SDK 证书目录不存在"
    fi
}

# 生成后端 config.toml
generate_backend_config() {
    info "生成后端 config.toml..."

    cat > "$PROJECT_DIR/backend/src/main/resources/config.toml" << 'EOF'
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
EOF

    info "config.toml 已生成"
}

# 显示帮助
show_help() {
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  install     - 安装依赖并下载脚本"
    echo "  build       - 构建区块链网络"
    echo "  start       - 启动所有节点"
    echo "  stop        - 停止所有节点"
    echo "  console     - 下载并配置控制台"
    echo "  contracts   - 复制合约到控制台"
    echo "  certs       - 复制证书到后端项目"
    echo "  config      - 生成后端 config.toml"
    echo "  all         - 执行完整部署流程"
    echo "  status      - 查看节点状态"
    echo ""
}

# 查看状态
show_status() {
    info "节点状态:"
    docker ps | grep fisco || echo "没有运行中的节点"
    echo ""

    info "区块高度检查:"
    if [ -f "$WORK_DIR/console/start.sh" ]; then
        cd $WORK_DIR/console
        echo "getBlockNumber" | bash start.sh 2>/dev/null | grep -E "^[0-9]+" || echo "无法获取区块高度"
    fi
}

# 完整部署
deploy_all() {
    check_docker
    install_deps
    download_script
    build_network
    start_nodes
    download_console
    copy_contracts
    copy_certs_to_backend
    generate_backend_config

    echo ""
    info "============================================"
    info "部署完成!"
    info "============================================"
    echo ""
    info "下一步操作:"
    echo "1. 启动控制台: cd ~/fisco/console && bash start.sh"
    echo "2. 部署合约:"
    echo "   deploy AccessControl"
    echo "   deploy ReceivablesToken"
    echo "   deploy Settlement"
    echo "3. 记录合约地址，更新 application.yml"
    echo "4. 启动后端: cd $PROJECT_DIR/backend && mvn spring-boot:run"
}

# 主函数
main() {
    case "$1" in
        install)
            check_docker
            install_deps
            download_script
            ;;
        build)
            build_network
            ;;
        start)
            start_nodes
            ;;
        stop)
            stop_nodes
            ;;
        console)
            download_console
            ;;
        contracts)
            copy_contracts
            ;;
        certs)
            copy_certs_to_backend
            ;;
        config)
            generate_backend_config
            ;;
        all)
            deploy_all
            ;;
        status)
            show_status
            ;;
        *)
            show_help
            ;;
    esac
}

main "$@"
