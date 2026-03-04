<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon size="28"><Connection /></el-icon>
        <span>供应链金融</span>
      </div>

      <el-menu
        :default-active="route.path"
        router
        background-color="#001529"
        text-color="#ffffffb3"
        active-text-color="#fff"
      >
        <template v-for="item in menuItems" :key="item.path">
          <el-menu-item :index="item.path" v-if="!item.hidden">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentRoute?.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tag v-if="userStore.enterprise" type="info" size="small">
            {{ roleLabels[userStore.enterprise.role] }}
          </el-tag>
          <span class="username">{{ userStore.user?.realName || userStore.user?.username }}</span>
          <el-dropdown @command="handleCommand">
            <el-avatar :size="36" style="cursor: pointer">
              {{ userStore.user?.username?.[0]?.toUpperCase() }}
            </el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { Connection } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menuItems = [
  { path: '/dashboard', title: '工作台', icon: 'Odometer' },
  { path: '/enterprises', title: '企业管理', icon: 'OfficeBuilding' },
  { path: '/receivables', title: '应收账款', icon: 'Document' },
  { path: '/financing', title: '融资管理', icon: 'Money' },
  { path: '/transfer', title: '流转记录', icon: 'Switch' }
]

const roleLabels = {
  CORE_ENTERPRISE: '核心企业',
  SUPPLIER: '供应商',
  FINANCIAL_INST: '金融机构',
  REGULATOR: '监管机构'
}

const currentRoute = computed(() => {
  return menuItems.find(item => item.path === route.path)
})

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.aside {
  background: #001529;
  overflow-y: auto;

  .logo {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    color: #fff;
    font-size: 18px;
    font-weight: 600;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  :deep(.el-menu) {
    border-right: none;
  }
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;

  .username {
    font-size: 14px;
    color: #606266;
  }
}

.main {
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
