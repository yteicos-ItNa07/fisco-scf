<template>
  <div class="page-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <div class="card-box stats-card">
          <div class="stats-value">{{ stats.totalEnterprises || 0 }}</div>
          <div class="stats-label">{{ localeStore.t('dashboard.totalEnterprises') }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="card-box stats-card">
          <div class="stats-value">{{ stats.activeReceivables || 0 }}</div>
          <div class="stats-label">{{ localeStore.t('dashboard.activeReceivables') }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="card-box stats-card">
          <div class="stats-value" style="color: #67C23A">
            {{ formatMoney(stats.totalReceivableValue) }}
          </div>
          <div class="stats-label">{{ localeStore.t('dashboard.totalReceivableValue') }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="card-box stats-card">
          <div class="stats-value" style="color: #E6A23C">
            {{ formatMoney(stats.totalFinancedValue) }}
          </div>
          <div class="stats-label">{{ localeStore.t('dashboard.totalFinancingValue') }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="16">
        <div class="card-box">
          <h3 style="margin-bottom: 20px">
            {{ currentSectionTitle }}
          </h3>
          <el-table :data="receivables" v-loading="loading" style="width: 100%">
            <el-table-column prop="tokenId" :label="localeStore.t('dashboard.tokenId')" width="100" />
            <el-table-column prop="issuer.name" :label="localeStore.t('dashboard.issuer')" />
            <el-table-column prop="faceValue" :label="localeStore.t('dashboard.amount')" width="150">
              <template #default="{ row }">
                ¥{{ formatMoney(row.faceValue) }}
              </template>
            </el-table-column>
            <el-table-column prop="maturityDate" :label="localeStore.t('dashboard.maturity')" width="120" />
            <el-table-column prop="status" :label="localeStore.t('dashboard.status')" width="100">
              <template #default="{ row }">
                <el-tag :type="statusType[row.status]">{{ statusLabels[row.status] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="txHash" :label="localeStore.t('dashboard.txHash')" width="150">
              <template #default="{ row }">
                <el-tooltip v-if="row.txHash" :content="row.txHash" placement="top">
                  <span class="hash-text">{{ row.txHash?.slice(0, 10) }}...</span>
                </el-tooltip>
                <span v-else style="color: #909399">-</span>
              </template>
            </el-table-column>
            <el-table-column :label="localeStore.t('dashboard.action')" width="180">
              <template #default="{ row }">
                <el-button size="small" @click="viewDetail(row)">{{ localeStore.t('dashboard.details') }}</el-button>
                <el-button
                  v-if="row.status === 'CONFIRMED'"
                  type="primary"
                  size="small"
                  @click="openFinancing(row)"
                >
                  {{ localeStore.t('dashboard.finance') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="receivables.length === 0 && !loading" class="empty-state">
            <el-empty :description="localeStore.t('dashboard.noReceivables')" />
          </div>
        </div>
      </el-col>

      <el-col :span="8">
        <div class="card-box">
          <h3 style="margin-bottom: 20px">{{ localeStore.t('dashboard.quickActions') }}</h3>
          <el-space direction="vertical" fill style="width: 100%">
            <el-button
              v-if="userStore.enterpriseRole === 'CORE_ENTERPRISE'"
              type="primary"
              style="width: 100%"
              @click="showIssueDialog = true"
            >
              <el-icon><Plus /></el-icon>
              {{ localeStore.t('dashboard.issueReceivable') }}
            </el-button>
            <el-button
              v-if="userStore.enterpriseRole === 'SUPPLIER'"
              type="success"
              style="width: 100%"
              @click="router.push('/financing')"
            >
              <el-icon><Money /></el-icon>
              {{ localeStore.t('dashboard.applyFinancing') }}
            </el-button>
            <el-button style="width: 100%" @click="router.push('/receivables')">
              <el-icon><Document /></el-icon>
              {{ localeStore.t('dashboard.viewAllTokens') }}
            </el-button>
          </el-space>
        </div>

        <div class="card-box" style="margin-top: 20px" v-if="pendingFinancing.length > 0">
          <h3 style="margin-bottom: 20px">{{ localeStore.t('dashboard.pendingApplications') }}</h3>
          <el-timeline>
            <el-timeline-item
              v-for="item in pendingFinancing"
              :key="item.id"
              :timestamp="formatDateTime(item.createdAt)"
            >
              <p>{{ item.borrower?.name }}</p>
              <p style="color: #909399; font-size: 12px">
                {{ localeStore.t('dashboard.borrowAmount') }}: ¥{{ formatMoney(item.financingAmount) }}
              </p>
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="showIssueDialog" :title="localeStore.t('dashboard.issueDialog.title')" width="500px">
      <el-form :model="issueForm" label-width="100px">
        <el-form-item :label="localeStore.t('dashboard.issueDialog.holder')">
          <el-select v-model="issueForm.holderId" :placeholder="localeStore.t('dashboard.issueDialog.holderPlaceholder')" style="width: 100%">
            <el-option
              v-for="e in suppliers"
              :key="e.id"
              :label="e.name"
              :value="e.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="localeStore.t('dashboard.issueDialog.amount')">
          <el-input-number
            v-model="issueForm.faceValue"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="localeStore.t('dashboard.issueDialog.maturity')">
          <el-date-picker
            v-model="issueForm.maturityDate"
            type="date"
            :placeholder="localeStore.t('dashboard.issueDialog.maturityPlaceholder')"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="localeStore.t('dashboard.issueDialog.annualRate')">
          <el-input-number
            v-model="issueForm.annualRate"
            :min="0"
            :max="0.3"
            :step="0.01"
            :precision="4"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="localeStore.t('dashboard.issueDialog.remark')">
          <el-input v-model="issueForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showIssueDialog = false">{{ localeStore.t('dashboard.issueDialog.cancel') }}</el-button>
        <el-button type="primary" @click="handleIssue" :loading="issueLoading">{{ localeStore.t('dashboard.issueDialog.submit') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useLocaleStore } from '@/store/locale'
import { getEnterprisesByRole } from '@/api/enterprise'
import { getMyHoldingReceivables, getMyIssuedReceivables, getAvailableForFinancing, issueReceivable, getMyPendingFinancing } from '@/api/receivable'
import { getGlobalStats, getMyStats } from '@/api/dashboard'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const localeStore = useLocaleStore()

const loading = ref(false)
const stats = ref({})
const receivables = ref([])
const pendingFinancing = ref([])
const suppliers = ref([])

const showIssueDialog = ref(false)
const issueLoading = ref(false)

const issueForm = reactive({
  holderId: null,
  faceValue: 1000000,
  maturityDate: '',
  annualRate: 0.06,
  description: ''
})

const currentSectionTitle = computed(() => {
  const role = userStore.enterpriseRole
  if (role === 'FINANCIAL_INST') return localeStore.t('dashboard.receivablesForRole.FINANCIAL_INST')
  if (role === 'CORE_ENTERPRISE') return localeStore.t('dashboard.receivablesForRole.CORE_ENTERPRISE')
  return localeStore.t('dashboard.receivablesForRole.default')
})

const statusLabels = computed(() => ({
  CREATED: localeStore.t('dashboard.statusLabels.CREATED'),
  CONFIRMED: localeStore.t('dashboard.statusLabels.CONFIRMED'),
  FINANCED: localeStore.t('dashboard.statusLabels.FINANCED'),
  PARTIALLY_FINANCED: localeStore.t('dashboard.statusLabels.PARTIALLY_FINANCED'),
  SETTLED: localeStore.t('dashboard.statusLabels.SETTLED'),
  EXPIRED: localeStore.t('dashboard.statusLabels.EXPIRED'),
  CANCELLED: localeStore.t('dashboard.statusLabels.CANCELLED')
}))

const statusType = {
  CREATED: 'warning',
  CONFIRMED: 'success',
  FINANCED: 'primary',
  PARTIALLY_FINANCED: 'primary',
  SETTLED: 'info',
  EXPIRED: 'danger',
  CANCELLED: 'info'
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
    // 加载统计数据
    const statsRes = await getGlobalStats()
    stats.value = statsRes.data || {}

    // 根据角色加载应收账款
    if (userStore.enterprise?.id) {
      let receivablesRes
      if (userStore.enterpriseRole === 'CORE_ENTERPRISE') {
        receivablesRes = await getMyIssuedReceivables()
      } else if (userStore.enterpriseRole === 'FINANCIAL_INST') {
        receivablesRes = await getAvailableForFinancing()
      } else {
        receivablesRes = await getMyHoldingReceivables()
      }
      receivables.value = receivablesRes.data || []
    }

    // 核心企业加载供应商列表
    if (userStore.enterpriseRole === 'CORE_ENTERPRISE') {
      const suppliersRes = await getEnterprisesByRole('SUPPLIER')
      suppliers.value = suppliersRes.data || []
    }

    // 金融机构加载待审批融资
    if (userStore.enterpriseRole === 'FINANCIAL_INST') {
      const pendingRes = await getMyPendingFinancing()
      pendingFinancing.value = pendingRes.data || []
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

function viewDetail(row) {
  router.push(`/receivables/${row.id}`)
}

function openFinancing(row) {
  router.push({ path: '/financing', query: { tokenId: row.tokenId } })
}

async function handleIssue() {
  if (!issueForm.holderId || !issueForm.faceValue || !issueForm.maturityDate) {
    ElMessage.warning('请填写完整信息')
    return
  }

  issueLoading.value = true
  try {
    await issueReceivable({
      holderId: issueForm.holderId,
      faceValue: issueForm.faceValue,
      maturityDate: dayjs(issueForm.maturityDate).format('YYYY-MM-DD'),
      annualRate: issueForm.annualRate,
      description: issueForm.description
    })
    ElMessage.success('应收账款签发成功')
    showIssueDialog.value = false
    loadData()
  } catch (error) {
    console.error(error)
  } finally {
    issueLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.empty-state {
  padding: 40px 0;
}

.hash-text {
  font-family: monospace;
  color: #409eff;
  cursor: pointer;
}
</style>
