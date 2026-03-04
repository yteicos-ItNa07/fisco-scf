<template>
  <div class="page-container">
    <div class="page-header">
      <h2>融资管理</h2>
    </div>

    <el-row :gutter="20">
      <!-- 融资申请 (供应商视角) -->
      <el-col :span="userStore.enterpriseRole === 'SUPPLIER' ? 16 : 24">
        <div class="card-box">
          <h3 style="margin-bottom: 20px">
            {{ userStore.enterpriseRole === 'FINANCIAL_INST' ? '待审批融资申请' :
               userStore.enterpriseRole === 'CORE_ENTERPRISE' ? '我签发通证的融资记录' :
               '我的融资记录' }}
          </h3>

          <el-table :data="financingList" v-loading="loading">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="receivable.tokenId" label="通证ID" width="80" />
            <el-table-column v-if="userStore.enterpriseRole === 'FINANCIAL_INST'" label="借款方">
              <template #default="{ row }">{{ row.borrower?.name }}</template>
            </el-table-column>
            <el-table-column v-else-if="userStore.enterpriseRole === 'CORE_ENTERPRISE'" label="借款方">
              <template #default="{ row }">{{ row.borrower?.name }}</template>
            </el-table-column>
            <el-table-column v-else label="金融机构">
              <template #default="{ row }">{{ row.financier?.name }}</template>
            </el-table-column>
            <el-table-column v-if="userStore.enterpriseRole === 'CORE_ENTERPRISE'" label="金融机构">
              <template #default="{ row }">{{ row.financier?.name }}</template>
            </el-table-column>
            <el-table-column label="融资金额" width="110">
              <template #default="{ row }">¥{{ formatMoney(row.financingAmount) }}</template>
            </el-table-column>
            <el-table-column label="利率" width="80">
              <template #default="{ row }">{{ (row.annualRate * 100).toFixed(2) }}%</template>
            </el-table-column>
            <el-table-column label="折价" width="100">
              <template #default="{ row }">¥{{ formatMoney(row.discountAmount) }}</template>
            </el-table-column>
            <el-table-column label="实际放款" width="120">
              <template #default="{ row }">
                <span style="color: #67C23A; font-weight: 600">¥{{ formatMoney(row.actualAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="financingStatusType[row.status]">{{ financingStatusLabels[row.status] }}</el-tag>
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
            <el-table-column v-if="userStore.enterpriseRole === 'FINANCIAL_INST'" label="操作" width="220">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button size="small" type="success" @click="handleApprove(row, true)">批准</el-button>
                  <el-button size="small" type="danger" @click="handleApprove(row, false)">拒绝</el-button>
                </template>
                <template v-if="row.status === 'APPROVED'">
                  <el-button size="small" type="primary" @click="handleDisburse(row)" :loading="row.disbursing">放款</el-button>
                </template>
              </template>
            </el-table-column>
            <el-table-column v-if="userStore.enterpriseRole === 'SUPPLIER'" label="操作" width="120">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'DISBURSED'"
                  size="small"
                  type="warning"
                  @click="handleRepay(row)"
                  :loading="row.repaying"
                >还款</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <!-- 新建融资申请 (仅供应商可见) -->
      <el-col :span="8" v-if="userStore.enterpriseRole === 'SUPPLIER'">
        <div class="card-box">
          <h3 style="margin-bottom: 20px">申请融资</h3>

          <el-form :model="applyForm" label-width="100px">
            <el-form-item label="选择通证">
              <el-select v-model="applyForm.tokenId" placeholder="选择可融资的通证" style="width: 100%" @change="handleTokenChange">
                <el-option
                  v-for="r in availableReceivables"
                  :key="r.tokenId"
                  :label="`#${r.tokenId} - ¥${formatMoney(r.remainingValue)}`"
                  :value="r.tokenId"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="金融机构">
              <el-select v-model="applyForm.financierId" placeholder="选择金融机构" style="width: 100%">
                <el-option
                  v-for="f in financialInsts"
                  :key="f.id"
                  :label="f.name"
                  :value="f.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="融资金额">
              <el-input-number
                v-model="applyForm.amount"
                :min="0"
                :max="selectedReceivable?.remainingValue || 0"
                style="width: 100%"
              />
            </el-form-item>

            <!-- 风险评估结果 -->
            <div v-if="riskAssessment" class="risk-box">
              <el-divider>风险评估结果</el-divider>
              <el-descriptions :column="1" size="small">
                <el-descriptions-item label="风险等级">
                  <el-tag :type="riskLevelType[riskAssessment.riskLevel]">
                    {{ riskLevelLabels[riskAssessment.riskLevel] }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="建议利率">
                  {{ (riskAssessment.suggestedRate * 100).toFixed(2) }}%
                </el-descriptions-item>
                <el-descriptions-item label="预计折价">
                  ¥{{ formatMoney(riskAssessment.estimatedDiscount) }}
                </el-descriptions-item>
                <el-descriptions-item label="预计实际放款">
                  <span style="color: #67C23A; font-weight: 600">
                    ¥{{ formatMoney(applyForm.amount - riskAssessment.estimatedDiscount) }}
                  </span>
                </el-descriptions-item>
              </el-descriptions>
              <div v-if="riskAssessment.riskFactors?.length" style="margin-top: 10px">
                <p style="font-size: 12px; color: #909399">风险因素:</p>
                <el-tag v-for="(f, i) in riskAssessment.riskFactors" :key="i" size="small" style="margin: 2px">
                  {{ f }}
                </el-tag>
              </div>
            </div>

            <el-form-item style="margin-top: 20px">
              <el-button type="primary" style="width: 100%" @click="handleApply" :loading="applyLoading">
                提交融资申请
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getEnterprisesByRole } from '@/api/enterprise'
import {
  getMyHoldingReceivables,
  getMyBorrowingRecords,
  getMyPendingFinancing,
  getMyLendingRecords,
  getMyIssuedFinancing,
  applyFinancing,
  approveFinancing,
  disburseFinancing,
  assessRisk,
  repayFinancing
} from '@/api/receivable'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const financingList = ref([])
const availableReceivables = ref([])
const financialInsts = ref([])
const riskAssessment = ref(null)
const applyLoading = ref(false)

const applyForm = reactive({
  tokenId: null,
  financierId: null,
  amount: 0
})

const selectedReceivable = computed(() => {
  return availableReceivables.value.find(r => r.tokenId === applyForm.tokenId)
})

const financingStatusLabels = {
  PENDING: '待审批',
  APPROVED: '已批准',
  REJECTED: '已拒绝',
  DISBURSED: '已放款',
  REPAID: '已还款'
}

const financingStatusType = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  DISBURSED: 'primary',
  REPAID: 'info'
}

const riskLevelLabels = {
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险'
}

const riskLevelType = {
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'danger'
}

function formatMoney(value) {
  if (!value) return '0'
  return Number(value).toLocaleString('zh-CN')
}

async function loadData() {
  loading.value = true
  try {
    if (userStore.enterpriseRole === 'FINANCIAL_INST') {
      const res = await getMyPendingFinancing()
      financingList.value = res.data || []
    } else if (userStore.enterpriseRole === 'CORE_ENTERPRISE') {
      // 核心企业：查看其签发通证的融资记录
      const res = await getMyIssuedFinancing()
      financingList.value = res.data || []
    } else {
      // 供应商：查看借款记录 + 可融资通证 + 金融机构列表
      const [financingRes, receivablesRes, instsRes] = await Promise.all([
        getMyBorrowingRecords(),
        getMyHoldingReceivables(),
        getEnterprisesByRole('FINANCIAL_INST')
      ])
      financingList.value = financingRes.data || []
      availableReceivables.value = (receivablesRes.data || []).filter(r => r.status === 'CONFIRMED')
      financialInsts.value = instsRes.data || []
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

async function handleTokenChange() {
  if (!applyForm.tokenId) {
    riskAssessment.value = null
    return
  }

  const receivable = selectedReceivable.value
  if (receivable) {
    applyForm.amount = receivable.remainingValue

    try {
      const res = await assessRisk(receivable.id)
      riskAssessment.value = res.data
    } catch (error) {
      console.error(error)
    }
  }
}

async function handleApply() {
  if (!applyForm.tokenId || !applyForm.financierId || !applyForm.amount) {
    ElMessage.warning('请填写完整信息')
    return
  }

  applyLoading.value = true
  try {
    await applyFinancing(applyForm)
    ElMessage.success('融资申请已提交')
    applyForm.tokenId = null
    applyForm.financierId = null
    applyForm.amount = 0
    riskAssessment.value = null
    loadData()
  } catch (error) {
    console.error(error)
  } finally {
    applyLoading.value = false
  }
}

async function handleApprove(row, approved) {
  const action = approved ? '批准' : '拒绝'
  try {
    await ElMessageBox.confirm(`确定${action}该融资申请？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await approveFinancing({
      financingId: row.id,
      approved
    })
    ElMessage.success(`融资申请已${action}`)
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

async function handleDisburse(row) {
  try {
    await ElMessageBox.confirm(
      `确定向 ${row.borrower?.name} 放款 ¥${formatMoney(row.actualAmount)}？`,
      '确认放款',
      {
        confirmButtonText: '确定放款',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    row.disbursing = true
    await disburseFinancing(row.id)
    ElMessage.success('放款成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  } finally {
    row.disbursing = false
  }
}

async function handleRepay(row) {
  try {
    await ElMessageBox.confirm(
      `确定向 ${row.financier?.name} 还款 ¥${formatMoney(row.financingAmount)}？`,
      '确认还款',
      {
        confirmButtonText: '确定还款',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    row.repaying = true
    await repayFinancing(row.id)
    ElMessage.success('还款成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  } finally {
    row.repaying = false
  }
}

onMounted(() => {
  loadData()
  // 从URL参数获取tokenId
  if (route.query.tokenId) {
    applyForm.tokenId = Number(route.query.tokenId)
    handleTokenChange()
  }
})
</script>

<style scoped>
.risk-box {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  margin-top: 10px;
}

.hash-text {
  font-family: monospace;
  color: #409eff;
  cursor: pointer;
}
</style>
