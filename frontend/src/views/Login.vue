<template>
  <div class="login-container">
    <!-- 左侧品牌区域 -->
    <div class="brand-section">
      <div class="brand-content">
        <div class="logo-area">
          <div class="logo-icon">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h1>供应链金融平台</h1>
        </div>
        <p class="brand-desc">基于 FISCO-BCOS 区块链的应收账款数字化融资解决方案</p>

        <div class="features">
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
            </div>
            <div class="feature-text">
              <h3>安全可信</h3>
              <p>区块链技术保障交易安全</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12,6 12,12 16,14"/>
              </svg>
            </div>
            <div class="feature-text">
              <h3>高效便捷</h3>
              <p>快速完成融资审批流程</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                <polyline points="3.27,6.96 12,12.01 20.73,6.96"/>
                <line x1="12" y1="22.08" x2="12" y2="12"/>
              </svg>
            </div>
            <div class="feature-text">
              <h3>通证流转</h3>
              <p>应收账款自由拆分转让</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 动态背景 -->
      <div class="bg-animation">
        <div class="cube"></div>
        <div class="cube"></div>
        <div class="cube"></div>
        <div class="cube"></div>
        <div class="cube"></div>
      </div>
    </div>

    <!-- 右侧登录区域 -->
    <div class="login-section">
      <div class="login-box">
        <div class="login-header">
          <h2>欢迎登录</h2>
          <p>请输入您的账号信息</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="demo-accounts">
          <div class="demo-title">
            <span class="line"></span>
            <span class="text">快速体验</span>
            <span class="line"></span>
          </div>
          <div class="demo-tags">
            <div class="demo-tag core" @click="fillDemo('core')">
              <span class="icon">🏢</span>
              <span class="label">核心企业</span>
            </div>
            <div class="demo-tag supplier" @click="fillDemo('supplier')">
              <span class="icon">🏭</span>
              <span class="label">供应商</span>
            </div>
            <div class="demo-tag bank" @click="fillDemo('bank')">
              <span class="icon">🏦</span>
              <span class="label">金融机构</span>
            </div>
          </div>
        </div>

        <div class="login-footer">
          <p>© 2026 供应链金融平台 · 区块链赋能产业金融</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demoAccounts = {
  core: { username: 'huawei_admin', password: '123456' },
  supplier: { username: 'supplier1', password: '123456' },
  bank: { username: 'icbc_admin', password: '123456' }
}

function fillDemo(type) {
  const account = demoAccounts[type]
  form.username = account.username
  form.password = account.password
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  display: flex;
  background: #0f172a;
}

// 左侧品牌区域
.brand-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #1e3a5f 0%, #0f172a 100%);
}

.brand-content {
  position: relative;
  z-index: 10;
  padding: 60px;
  max-width: 500px;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;

  .logo-icon {
    width: 56px;
    height: 56px;
    background: linear-gradient(135deg, #3b82f6, #8b5cf6);
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;

    svg {
      width: 32px;
      height: 32px;
      color: white;
    }
  }

  h1 {
    font-size: 32px;
    font-weight: 700;
    color: #fff;
    margin: 0;
    background: linear-gradient(90deg, #fff, #94a3b8);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.brand-desc {
  font-size: 16px;
  color: #94a3b8;
  line-height: 1.8;
  margin-bottom: 48px;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    transform: translateX(8px);
  }

  .feature-icon {
    width: 48px;
    height: 48px;
    background: linear-gradient(135deg, #3b82f6, #8b5cf6);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    svg {
      width: 24px;
      height: 24px;
      color: white;
    }
  }

  .feature-text {
    h3 {
      font-size: 16px;
      font-weight: 600;
      color: #fff;
      margin: 0 0 4px 0;
    }

    p {
      font-size: 13px;
      color: #64748b;
      margin: 0;
    }
  }
}

// 动态背景
.bg-animation {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;

  .cube {
    position: absolute;
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(139, 92, 246, 0.1));
    border: 1px solid rgba(255, 255, 255, 0.05);
    animation: float 20s infinite;

    &:nth-child(1) {
      width: 80px;
      height: 80px;
      top: 10%;
      left: 10%;
      animation-delay: 0s;
      border-radius: 20px;
    }

    &:nth-child(2) {
      width: 60px;
      height: 60px;
      top: 60%;
      left: 20%;
      animation-delay: -5s;
      border-radius: 15px;
    }

    &:nth-child(3) {
      width: 100px;
      height: 100px;
      top: 30%;
      right: 10%;
      animation-delay: -10s;
      border-radius: 25px;
    }

    &:nth-child(4) {
      width: 50px;
      height: 50px;
      bottom: 20%;
      left: 40%;
      animation-delay: -15s;
      border-radius: 12px;
    }

    &:nth-child(5) {
      width: 70px;
      height: 70px;
      bottom: 40%;
      right: 30%;
      animation-delay: -7s;
      border-radius: 18px;
    }
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
    opacity: 0.5;
  }
  50% {
    transform: translateY(-30px) rotate(180deg);
    opacity: 0.8;
  }
}

// 右侧登录区域
.login-section {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.login-box {
  width: 100%;
  max-width: 360px;
  padding: 40px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;

  h2 {
    font-size: 28px;
    font-weight: 700;
    color: #1e293b;
    margin: 0 0 8px 0;
  }

  p {
    font-size: 14px;
    color: #64748b;
    margin: 0;
  }
}

.login-form {
  .el-input {
    --el-input-border-radius: 10px;
  }

  :deep(.el-input__wrapper) {
    padding: 4px 16px;
    box-shadow: 0 0 0 1px #e2e8f0;

    &:hover {
      box-shadow: 0 0 0 1px #3b82f6;
    }

    &.is-focus {
      box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.3);
    }
  }

  .el-form-item {
    margin-bottom: 24px;
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border: none;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(59, 130, 246, 0.4);
  }

  &:active {
    transform: translateY(0);
  }
}

.demo-accounts {
  margin-top: 32px;

  .demo-title {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    margin-bottom: 20px;

    .line {
      flex: 1;
      height: 1px;
      background: #e2e8f0;
    }

    .text {
      font-size: 13px;
      color: #94a3b8;
      white-space: nowrap;
    }
  }

  .demo-tags {
    display: flex;
    justify-content: center;
    gap: 12px;
  }

  .demo-tag {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 12px 16px;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.3s ease;
    border: 1px solid #e2e8f0;

    .icon {
      font-size: 24px;
    }

    .label {
      font-size: 12px;
      color: #64748b;
      font-weight: 500;
    }

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
    }

    &.core {
      &:hover {
        border-color: #3b82f6;
        background: rgba(59, 130, 246, 0.05);
        .label { color: #3b82f6; }
      }
    }

    &.supplier {
      &:hover {
        border-color: #10b981;
        background: rgba(16, 185, 129, 0.05);
        .label { color: #10b981; }
      }
    }

    &.bank {
      &:hover {
        border-color: #f59e0b;
        background: rgba(245, 158, 11, 0.05);
        .label { color: #f59e0b; }
      }
    }
  }
}

.login-footer {
  margin-top: 40px;
  text-align: center;

  p {
    font-size: 12px;
    color: #94a3b8;
    margin: 0;
  }
}

// 响应式适配
@media (max-width: 900px) {
  .login-container {
    flex-direction: column;
  }

  .brand-section {
    display: none;
  }

  .login-section {
    width: 100%;
    min-height: 100vh;
  }
}
</style>
