<template>
  <div class="page-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <div class="card-box stats-card">
          <div class="stats-value">{{ stats.totalEnterprises || 0 }}</div>
          <div class="stats-label">注册企业</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="card-box stats-card">
          <div class="stats-value">{{ stats.activeReceivables || 0 }}</div>
          <div class="stats-label">活跃通证</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="card-box stats-card">
          <div class="stats-value" style="color: #67C23A">
            {{ formatMoney(stats.totalReceivableValue) }}
          </div>
          <div class="stats-label">应收账款总额</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="card-box stats-card">
          <div class="stats-value" style="color: #E6A23C">
            {{ formatMoney(stats.totalFinancedValue) }}
          </div>
          <div class="stats-label">融资总额</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="16">
        <div class="card-box">
          <h3 style="margin-bottom: 20px">
            {{ userStore.enterpriseRole === 'FINANCIAL_INST' ? '可融资的应收账款' :
               userStore.enterpriseRole === 'CORE_ENTERPRISE' ? '我签发的应收账款' : '我的应收账款' }}
          </h3>
          <el-table :data="receivables" v-loading="loading" style="width: 100%">
            <el-table-column prop="tokenId" label="通证ID" width="100" />
            <el-table-column prop="issuer.name" label="签发方" />
            <el-table-column prop="faceValue" label="金额" width="150">
              <template #default="{ row }">
                ¥{{ formatMoney(row.faceValue) }}
              </template>
            </el-table-column>
            <el-table-column prop="maturityDate" label="到期日" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusType[row.status]">{{ statusLabels[row.status] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="txHash" label="交易哈希" width="150">
              <template #default="{ row }">
                <el-tooltip v-if="row.txHash" :content="row.txHash" placement="top">
                  <span class="hash-text">{{ row.txHash?.slice(0, 10) }}...</span>
                </el-tooltip>
                <span v-else style="color: #909399">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" @click="viewDetail(row)">详情</el-button>
                <el-button
                  v-if="row.status === 'CONFIRMED'"
                  type="primary"
                  size="small"
                  @click="openFinancing(row)"
                >
                  融资
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="receivables.length === 0 && !loading" class="empty-state">
            <el-empty description="暂无应收账款" />
          </div>
        </div>
      </el-col>

      <el-col :span="8">
        <div class="card-box">
          <h3 style="margin-bottom: 20px">快捷操作</h3>
          <el-space direction="vertical" fill style="width: 100%">
            <el-button
              v-if="userStore.enterpriseRole === 'CORE_ENTERPRISE'"
              type="primary"
              style="width: 100%"
              @click="showIssueDialog = true"
            >
              <el-icon><Plus /></el-icon>
              签发应收账款
            </el-button>
            <el-button
              v-if="userStore.enterpriseRole === 'SUPPLIER'"
              type="success"
              style="width: 100%"
              @click="router.push('/financing')"
            >
              <el-icon><Money /></el-icon>
              申请融资
            </el-button>
            <el-button style="width: 100%" @click="router.push('/receivables')">
              <el-icon><Document /></el-icon>
              查看全部通证
            </el-button>
          </el-space>
        </div>

        <div class="card-box" style="margin-top: 20px" v-if="pendingFinancing.length > 0">
          <h3 style="margin-bottom: 20px">待处理融资申请</h3>
          <el-timeline>
            <el-timeline-item
              v-for="item in pendingFinancing"
              :key="item.id"
              :timestamp="formatDateTime(item.createdAt)"
            >
              <p>{{ item.borrower?.name }}</p>
              <p style="color: #909399; font-size: 12px">
                申请金额: ¥{{ formatMoney(item.financingAmount) }}
              </p>
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-col>
    </el-row>

    <!-- 签发应收账款对话框 -->
    <el-dialog v-model="showIssueDialog" title="签发应收账款" width="500px">
      <el-form :model="issueForm" label-width="100px">
        <el-form-item label="持有方">
          <el-select v-model="issueForm.holderId" placeholder="选择供应商" style="width: 100%">
            <el-option
              v-for="e in suppliers"
              :key="e.id"
              :label="e.name"
              :value="e.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="金额(元)">
          <el-input-number
            v-model="issueForm.faceValue"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="到期日期">
          <el-date-picker
            v-model="issueForm.maturityDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="年化利率">
          <el-input-number
            v-model="issueForm.annualRate"
            :min="0"
            :max="0.3"
            :step="0.01"
            :precision="4"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="issueForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showIssueDialog = false">取消</el-button>
        <el-button type="primary" @click="handleIssue" :loading="issueLoading">确认签发</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getEnterprisesByRole } from '@/api/enterprise'
import { getMyHoldingReceivables, getMyIssuedReceivables, getAvailableForFinancing, issueReceivable, getMyPendingFinancing } from '@/api/receivable'
import { getGlobalStats, getMyStats } from '@/api/dashboard'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

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

const statusLabels = {
  CREATED: '待确权',
  CONFIRMED: '已确权',
  FINANCED: '已融资',
  PARTIALLY_FINANCED: '部分融资',
  SETTLED: '已结算',
  EXPIRED: '已过期',
  CANCELLED: '已取消'
}

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
