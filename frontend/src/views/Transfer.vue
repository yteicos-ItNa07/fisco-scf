<template>
  <div class="page-container">
    <div class="page-header">
      <h2>流转记录</h2>
    </div>

    <div class="card-box">
      <el-table :data="records" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeColors[row.type]">{{ typeLabels[row.type] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="转出方">
          <template #default="{ row }">{{ row.fromEnterprise?.name || '-' }}</template>
        </el-table-column>
        <el-table-column label="转入方">
          <template #default="{ row }">{{ row.toEnterprise?.name || '-' }}</template>
        </el-table-column>
        <el-table-column label="金额" width="150">
          <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因/用途" />
        <el-table-column prop="txHash" label="交易哈希" width="160">
          <template #default="{ row }">
            <el-tooltip v-if="row.txHash" :content="row.txHash" placement="top">
              <span class="hash-text">{{ row.txHash?.slice(0, 10) }}...</span>
            </el-tooltip>
            <span v-else style="color: #909399">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <div v-if="records.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无流转记录" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyTransfers } from '@/api/transfer'

const loading = ref(false)
const records = ref([])

const typeLabels = {
  ISSUE: '签发',
  TRANSFER: '转让',
  SPLIT: '拆分',
  FINANCING: '融资',
  SETTLEMENT: '结算'
}

const typeColors = {
  ISSUE: 'primary',
  TRANSFER: 'success',
  SPLIT: 'warning',
  FINANCING: 'danger',
  SETTLEMENT: 'info'
}

function formatMoney(value) {
  if (!value) return '0'
  return Number(value).toLocaleString('zh-CN')
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
