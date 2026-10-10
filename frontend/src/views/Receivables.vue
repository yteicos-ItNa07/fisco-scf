<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ localeStore.t('receivables.title') }}</h2>
      <el-button
        v-if="userStore.enterpriseRole === 'CORE_ENTERPRISE'"
        type="primary"
        @click="showIssueDialog = true"
      >
        <el-icon><Plus /></el-icon>
        {{ localeStore.t('receivables.issueButton') }}
      </el-button>
    </div>

    <div class="card-box">
      <el-table :data="receivables" v-loading="loading" style="width: 100%">
        <el-table-column prop="tokenId" :label="localeStore.t('receivables.columns.tokenId')" width="100" />
        <el-table-column prop="issuer.name" :label="localeStore.t('receivables.columns.issuer')" width="180" />
        <el-table-column prop="holder.name" :label="localeStore.t('receivables.columns.holder')" width="150" />
        <el-table-column :label="localeStore.t('receivables.columns.amount')" width="150">
          <template #default="{ row }">
            <div>¥{{ formatMoney(row.faceValue) }}</div>
            <div style="color: #909399; font-size: 12px" v-if="row.remainingValue !== row.faceValue">
              {{ localeStore.t('receivables.columns.remaining') }}: ¥{{ formatMoney(row.remainingValue) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="annualRate" :label="localeStore.t('receivables.columns.annualRate')" width="100">
          <template #default="{ row }">
            {{ (row.annualRate * 100).toFixed(2) }}%
          </template>
        </el-table-column>
        <el-table-column prop="maturityDate" :label="localeStore.t('receivables.columns.maturityDate')" width="120" />
        <el-table-column prop="daysToMaturity" :label="localeStore.t('receivables.columns.remainingDays')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.daysToMaturity <= 30 ? 'danger' : row.daysToMaturity <= 90 ? 'warning' : 'success'">
              {{ row.daysToMaturity }}{{ localeStore.t('receivables.columns.day') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="localeStore.t('receivables.columns.contract')" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.contractHash" type="success" size="small">{{ localeStore.t('receivables.columns.contractStatusRecorded') }}</el-tag>
            <el-tag v-else type="info" size="small">{{ localeStore.t('receivables.columns.contractStatusNone') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" :label="localeStore.t('receivables.columns.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status]">{{ statusLabels[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="txHash" :label="localeStore.t('receivables.columns.txHash')" width="150">
          <template #default="{ row }">
            <el-tooltip v-if="row.txHash" :content="row.txHash" placement="top">
              <span class="hash-text">{{ row.txHash?.slice(0, 10) }}...</span>
            </el-tooltip>
            <span v-else style="color: #909399">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="localeStore.t('receivables.columns.action')" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">{{ localeStore.t('receivables.columns.details') }}</el-button>
            <el-button
              v-if="row.status === 'CONFIRMED' && row.holder.id === userStore.enterprise?.id"
              size="small"
              type="success"
              @click="openSplit(row)"
            >
              {{ localeStore.t('receivables.columns.split') }}
            </el-button>
            <el-button
              v-if="row.status === 'CONFIRMED' && row.holder.id === userStore.enterprise?.id"
              size="small"
              type="primary"
              @click="openFinancing(row)"
            >
              {{ localeStore.t('receivables.columns.finance') }}
            </el-button>
            <el-button
              v-if="row.status === 'CONFIRMED' && row.holder.id === userStore.enterprise?.id"
              size="small"
              type="warning"
              @click="openTransfer(row)"
            >
              {{ localeStore.t('receivables.columns.transfer') }}
            </el-button>
            <el-button
              v-if="(row.status === 'CONFIRMED' || row.status === 'FINANCED' || row.status === 'PARTIALLY_FINANCED') && row.issuer.id === userStore.enterprise?.id && row.daysToMaturity <= 0"
              size="small"
              type="info"
              @click="handleSettle(row)"
              :loading="row.settling"
            >
              {{ localeStore.t('receivables.columns.settle') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showIssueDialog" :title="localeStore.t('receivables.issueDialog.title')" width="600px" @close="resetIssueForm">
      <el-form ref="issueFormRef" :model="issueForm" :rules="issueRules" label-width="100px">
        <el-form-item :label="localeStore.t('receivables.issueDialog.recipient')" prop="holderId">
          <el-select v-model="issueForm.holderId" :placeholder="localeStore.t('receivables.issueDialog.recipientPlaceholder')" style="width: 100%">
            <el-option v-for="e in suppliers" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>

        <el-form-item :label="localeStore.t('receivables.issueDialog.faceValue')" prop="faceValue">
          <el-input-number
            v-model="issueForm.faceValue"
            :min="1000"
            :max="100000000"
            :step="10000"
            style="width: 100%"
            :placeholder="localeStore.t('receivables.issueDialog.faceValuePlaceholder')"
          />
        </el-form-item>

        <el-form-item :label="localeStore.t('receivables.issueDialog.maturityDate')" prop="maturityDate">
          <el-date-picker
            v-model="issueForm.maturityDate"
            type="date"
            :placeholder="localeStore.t('receivables.issueDialog.maturityPlaceholder')"
            style="width: 100%"
            :disabled-date="(date) => date < new Date()"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item :label="localeStore.t('receivables.issueDialog.annualRate')" prop="annualRate">
          <el-slider
            v-model="issueForm.annualRate"
            :min="3"
            :max="10"
            :step="0.1"
            :format-tooltip="(val) => val + '%'"
          />
          <span style="margin-left: 10px; color: #409EFF">{{ issueForm.annualRate }}%</span>
        </el-form-item>

        <el-form-item :label="localeStore.t('receivables.issueDialog.description')" prop="description">
          <el-input v-model="issueForm.description" type="textarea" :placeholder="localeStore.t('receivables.issueDialog.descriptionPlaceholder')" />
        </el-form-item>

        <el-divider :content-position="'left'">{{ localeStore.t('receivables.issueDialog.contractTitle') }}</el-divider>

        <el-form-item :label="localeStore.t('receivables.issueDialog.contractFile')">
          <el-upload
            ref="uploadRef"
            class="contract-upload"
            drag
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              {{ localeStore.t('receivables.issueDialog.uploadPrompt') }}
            </div>
            <template #tip>
              <div class="el-upload__tip">
                {{ localeStore.t('receivables.issueDialog.uploadTip') }}
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item v-if="selectedFile" :label="localeStore.t('receivables.issueDialog.fileInfo')">
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item :label="localeStore.t('receivables.issueDialog.fileName')">{{ selectedFile.name }}</el-descriptions-item>
            <el-descriptions-item :label="localeStore.t('receivables.issueDialog.fileSize')">{{ formatFileSize(selectedFile.size) }}</el-descriptions-item>
          </el-descriptions>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showIssueDialog = false">{{ localeStore.t('receivables.issueDialog.cancel') }}</el-button>
        <el-button type="primary" @click="handleIssue" :loading="issueLoading">
          {{ selectedFile ? localeStore.t('receivables.issueDialog.submitWithContract') : localeStore.t('receivables.issueDialog.submit') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showSplitDialog" :title="localeStore.t('receivables.splitDialog.title')" width="600px">
      <el-alert type="info" :closable="false" style="margin-bottom: 20px">
        <p>{{ localeStore.t('receivables.splitDialog.originalAmount') }}: ¥{{ formatMoney(currentReceivable?.remainingValue) }}</p>
      </el-alert>

      <div v-for="(target, index) in splitTargets" :key="index" style="display: flex; gap: 10px; margin-bottom: 10px">
        <el-select v-model="target.recipientId" :placeholder="localeStore.t('receivables.splitDialog.recipient')" style="flex: 1">
          <el-option v-for="e in suppliers" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
        <el-input-number v-model="target.amount" :min="0" :placeholder="localeStore.t('receivables.splitDialog.amount')" style="width: 150px" />
        <el-input v-model="target.purpose" :placeholder="localeStore.t('receivables.splitDialog.purpose')" style="width: 150px" />
        <el-button type="danger" @click="splitTargets.splice(index, 1)" :disabled="splitTargets.length <= 1">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>

      <el-button @click="addSplitTarget" style="margin-top: 10px">
        <el-icon><Plus /></el-icon>
        {{ localeStore.t('receivables.splitDialog.addRecipient') }}
      </el-button>

      <template #footer>
        <el-button @click="showSplitDialog = false">{{ localeStore.t('receivables.splitDialog.cancel') }}</el-button>
        <el-button type="primary" @click="handleSplit" :loading="splitLoading">{{ localeStore.t('receivables.splitDialog.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showTransferDialog" :title="localeStore.t('receivables.transferDialog.title')" width="400px">
      <el-form :model="transferForm" label-width="80px">
        <el-form-item :label="localeStore.t('receivables.transferDialog.recipient')">
          <el-select v-model="transferForm.toEnterpriseId" :placeholder="localeStore.t('receivables.transferDialog.recipientPlaceholder')" style="width: 100%">
            <el-option v-for="e in suppliers" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="localeStore.t('receivables.transferDialog.reason')">
          <el-input v-model="transferForm.reason" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTransferDialog = false">{{ localeStore.t('receivables.transferDialog.cancel') }}</el-button>
        <el-button type="primary" @click="handleTransfer" :loading="transferLoading">{{ localeStore.t('receivables.transferDialog.confirm') }}</el-button>
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
import { getMyHoldingReceivables, getMyIssuedReceivables, getAvailableForFinancing, issueReceivable, splitReceivable, transferReceivable, settleReceivable } from '@/api/receivable'
import { uploadContract } from '@/api/contract'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const localeStore = useLocaleStore()

const loading = ref(false)
const receivables = ref([])
const suppliers = ref([])
const currentReceivable = ref(null)

const showIssueDialog = ref(false)
const issueLoading = ref(false)
const issueFormRef = ref()
const uploadRef = ref()
const selectedFile = ref(null)

const issueForm = reactive({
  holderId: null,
  faceValue: 100000,
  maturityDate: '',
  annualRate: 6,
  description: ''
})

const issueRules = computed(() => ({
  holderId: [{ required: true, message: localeStore.t('receivables.messages.selectReceiver'), trigger: 'change' }],
  faceValue: [{ required: true, message: localeStore.t('receivables.messages.infoRequired'), trigger: 'blur' }],
  maturityDate: [{ required: true, message: localeStore.t('receivables.messages.infoRequired'), trigger: 'change' }],
  annualRate: [{ required: true, message: localeStore.t('receivables.messages.infoRequired'), trigger: 'change' }]
}))

const showSplitDialog = ref(false)
const splitLoading = ref(false)
const splitTargets = ref([{ recipientId: null, amount: 0, purpose: '' }])

const showTransferDialog = ref(false)
const transferLoading = ref(false)
const transferForm = reactive({
  tokenId: null,
  toEnterpriseId: null,
  reason: ''
})

const statusLabels = computed(() => ({
  CREATED: localeStore.t('receivables.statusLabels.CREATED'),
  CONFIRMED: localeStore.t('receivables.statusLabels.CONFIRMED'),
  FINANCED: localeStore.t('receivables.statusLabels.FINANCED'),
  PARTIALLY_FINANCED: localeStore.t('receivables.statusLabels.PARTIALLY_FINANCED'),
  SETTLED: localeStore.t('receivables.statusLabels.SETTLED'),
  EXPIRED: localeStore.t('receivables.statusLabels.EXPIRED'),
  CANCELLED: localeStore.t('receivables.statusLabels.CANCELLED')
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
  const formatter = new Intl.NumberFormat(localeStore.locale === 'en' ? 'en-US' : localeStore.locale === 'zh-TW' ? 'zh-Hant-TW' : 'zh-CN')
  return formatter.format(Number(value))
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function loadData() {
  loading.value = true
  try {
    const role = userStore.enterpriseRole
    let res
    if (role === 'CORE_ENTERPRISE') {
      res = await getMyIssuedReceivables()
    } else if (role === 'FINANCIAL_INST') {
      // 金融机构看所有可融资的应收账款
      res = await getAvailableForFinancing()
    } else {
      res = await getMyHoldingReceivables()
    }
    receivables.value = res.data || []

    const suppliersRes = await getEnterprisesByRole('SUPPLIER')
    suppliers.value = suppliersRes.data || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

function viewDetail(row) {
  router.push(`/receivables/${row.id}`)
}

// ========== 签发相关 ==========
function handleFileChange(file) {
  selectedFile.value = file.raw
}

function handleFileRemove() {
  selectedFile.value = null
}

function resetIssueForm() {
  issueForm.holderId = null
  issueForm.faceValue = 100000
  issueForm.maturityDate = ''
  issueForm.annualRate = 6
  issueForm.description = ''
  selectedFile.value = null
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

async function handleIssue() {
  const valid = await issueFormRef.value.validate().catch(() => false)
  if (!valid) return

  issueLoading.value = true
  try {
    // 1. 先签发应收账款
    const issueRes = await issueReceivable({
      holderId: issueForm.holderId,
      faceValue: issueForm.faceValue,
      maturityDate: issueForm.maturityDate,
      annualRate: issueForm.annualRate / 100,  // 转换为小数
      description: issueForm.description
    })

    const receivableId = issueRes.data?.id

    // 2. 如果有合同文件，上传合同
    if (selectedFile.value && receivableId) {
      await uploadContract(selectedFile.value, receivableId, `应收账款#${receivableId}的电子合同`)
      ElMessage.success('签发成功，合同已上传存证')
    } else {
      ElMessage.success('签发成功')
    }

    showIssueDialog.value = false
    resetIssueForm()
    loadData()
  } catch (error) {
    console.error(error)
  } finally {
    issueLoading.value = false
  }
}

// ========== 拆分相关 ==========
function openSplit(row) {
  currentReceivable.value = row
  splitTargets.value = [{ recipientId: null, amount: 0, purpose: '' }]
  showSplitDialog.value = true
}

function addSplitTarget() {
  splitTargets.value.push({ recipientId: null, amount: 0, purpose: '' })
}

async function handleSplit() {
  const totalAmount = splitTargets.value.reduce((sum, t) => sum + (t.amount || 0), 0)
  if (totalAmount > currentReceivable.value.remainingValue) {
    ElMessage.warning('拆分金额超过剩余金额')
    return
  }

  splitLoading.value = true
  try {
    await splitReceivable({
      tokenId: currentReceivable.value.tokenId,
      targets: splitTargets.value
    })
    ElMessage.success('拆分成功')
    showSplitDialog.value = false
    loadData()
  } catch (error) {
    console.error(error)
  } finally {
    splitLoading.value = false
  }
}

// ========== 转让相关 ==========
function openTransfer(row) {
  currentReceivable.value = row
  transferForm.tokenId = row.tokenId
  transferForm.toEnterpriseId = null
  transferForm.reason = ''
  showTransferDialog.value = true
}

async function handleTransfer() {
  if (!transferForm.toEnterpriseId) {
    ElMessage.warning('请选择接收方')
    return
  }

  transferLoading.value = true
  try {
    await transferReceivable(transferForm)
    ElMessage.success('转让成功')
    showTransferDialog.value = false
    loadData()
  } catch (error) {
    console.error(error)
  } finally {
    transferLoading.value = false
  }
}

function openFinancing(row) {
  router.push({ path: '/financing', query: { tokenId: row.tokenId } })
}

async function handleSettle(row) {
  try {
    await ElMessageBox.confirm(
      `确定结算通证 #${row.tokenId}？结算金额 ¥${formatMoney(row.remainingValue)}，将向持有方 ${row.holder?.name} 付款。`,
      '确认到期结算',
      {
        confirmButtonText: '确定结算',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    row.settling = true
    await settleReceivable(row.id)
    ElMessage.success('结算成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  } finally {
    row.settling = false
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

.contract-upload {
  width: 100%;
}

.contract-upload :deep(.el-upload-dragger) {
  width: 100%;
  padding: 20px;
}
</style>
