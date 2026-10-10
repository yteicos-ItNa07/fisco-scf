<template>
  <el-config-provider :locale="localeStore.currentLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { useLocaleStore } from '@/store/locale'

const localeStore = useLocaleStore()

if (typeof document !== 'undefined') {
  document.documentElement.lang = localeStore.locale
}
</script>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
