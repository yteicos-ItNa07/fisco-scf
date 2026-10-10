<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon size="28"><Connection /></el-icon>
        <span>{{ localeStore.t('login.title') }}</span>
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
            <el-breadcrumb-item :to="{ path: '/' }">{{ localeStore.t('nav.home') }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentRoute?.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-select
            class="lang-switcher"
            :model-value="localeStore.locale"
            size="small"
            @change="localeStore.setLocale"
          >
            <el-option label="English" value="en" />
            <el-option label="繁體中文" value="zh-TW" />
            <el-option label="简体中文" value="zh-CN" />
          </el-select>

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
                <el-dropdown-item command="logout">{{ localeStore.t('nav.logout') }}</el-dropdown-item>
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
import { useLocaleStore } from '@/store/locale'
import { Connection } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const localeStore = useLocaleStore()

const menuItems = computed(() => [
  { path: '/dashboard', title: localeStore.t('nav.dashboard'), icon: 'Odometer' },
  { path: '/enterprises', title: localeStore.t('nav.enterprises'), icon: 'OfficeBuilding' },
  { path: '/receivables', title: localeStore.t('nav.receivables'), icon: 'Document' },
  { path: '/financing', title: localeStore.t('nav.financing'), icon: 'Money' },
  { path: '/transfer', title: localeStore.t('nav.transfer'), icon: 'Switch' }
])

const roleLabels = computed(() => ({
  CORE_ENTERPRISE: localeStore.t('roles.CORE_ENTERPRISE'),
  SUPPLIER: localeStore.t('roles.SUPPLIER'),
  FINANCIAL_INST: localeStore.t('roles.FINANCIAL_INST'),
  REGULATOR: localeStore.t('roles.REGULATOR')
}))

const currentRoute = computed(() => {
  return menuItems.value.find(item => item.path === route.path)
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

  .lang-switcher {
    width: 140px;
  }

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
