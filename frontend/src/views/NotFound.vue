<template>
  <div class="not-found-container">
    <div class="content">
      <h1>404</h1>
      <p>{{ localeStore.t('common.pageNotFound') }}</p>
      <el-button type="primary" @click="router.push('/')">{{ localeStore.t('nav.backHome') }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useLocaleStore } from '@/store/locale'

const router = useRouter()
const localeStore = useLocaleStore()
</script>

<style scoped>
.not-found-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.content {
  text-align: center;
}

.content h1 {
  font-size: 120px;
  color: #409eff;
  margin: 0;
  line-height: 1;
}

.content p {
  font-size: 20px;
  color: #909399;
  margin: 20px 0 30px;
}
</style>
