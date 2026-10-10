<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ localeStore.t('transfer.title') }}</h2>
    </div>

    <div class="card-box">
      <el-table :data="records" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="type" :label="localeStore.t('transfer.columns.type')" width="100">
          <template #default="{ row }">
            <el-tag :type="typeColors[row.type]">{{ typeLabels[row.type] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="localeStore.t('transfer.columns.from')">
          <template #default="{ row }">{{ row.fromEnterprise?.name || '-' }}</template>
        </el-table-column>
        <el-table-column :label="localeStore.t('transfer.columns.to')">
          <template #default="{ row }">{{ row.toEnterprise?.name || '-' }}</template>
        </el-table-column>
        <el-table-column :label="localeStore.t('transfer.columns.amount')" width="150">
          <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="reason" :label="localeStore.t('transfer.columns.reason')" />
        <el-table-column prop="txHash" :label="localeStore.t('transfer.columns.txHash')" width="160">
          <template #default="{ row }">
            <el-tooltip v-if="row.txHash" :content="row.txHash" placement="top">
              <span class="hash-text">{{ row.txHash?.slice(0, 10) }}...</span>
            </el-tooltip>
            <span v-else style="color: #909399">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="localeStore.t('transfer.columns.time')" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <div v-if="records.length === 0 && !loading" class="empty-state">
        <el-empty :description="localeStore.t('transfer.empty')" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getMyTransfers } from '@/api/transfer'
import { useLocaleStore } from '@/store/locale'

const localeStore = useLocaleStore()
const loading = ref(false)
const records = ref([])

const typeLabels = computed(() => ({
  ISSUE: localeStore.t('transfer.typeLabels.ISSUE'),
  TRANSFER: localeStore.t('transfer.typeLabels.TRANSFER'),
  SPLIT: localeStore.t('transfer.typeLabels.SPLIT'),
  FINANCING: localeStore.t('transfer.typeLabels.FINANCING'),
  SETTLEMENT: localeStore.t('transfer.typeLabels.SETTLEMENT')
}))

const typeColors = {
  ISSUE: 'primary',
  TRANSFER: 'success',
  SPLIT: 'warning',
  FINANCING: 'danger',
  SETTLEMENT: 'info'
}

function formatMoney(value) {
  if (!value) return '0'
  const formatter = new Intl.NumberFormat(localeStore.locale === 'en' ? 'en-US' : localeStore.locale === 'zh-TW' ? 'zh-Hant-TW' : 'zh-CN')
  return formatter.format(Number(value))
}

function formatDateTime(dateTime) {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').substring(0, 19)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMyTransfers()
    records.value = res.data || []
  } catch (error) {
    console.error('加载流转记录失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.hash-text {
  font-family: monospace;
  color: #409eff;
  cursor: pointer;
}

.empty-state {
  padding: 40px 0;
}
</style>
