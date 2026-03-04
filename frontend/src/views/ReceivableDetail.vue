<template>
  <div class="page-container">
    <el-page-header @back="router.back()" title="返回">
      <template #content>
        <span>通证详情 #{{ receivable?.tokenId }}</span>
      </template>
    </el-page-header>

    <div v-loading="loading">
      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="16">
          <div class="card-box">
            <h3 style="margin-bottom: 20px">基本信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="通证ID">{{ receivable?.tokenId }}</el-descriptions-item>
              <el-descriptions-item label="父通证ID">{{ receivable?.parentTokenId || '无(原始通证)' }}</el-descriptions-item>
              <el-descriptions-item label="签发方">{{ receivable?.issuer?.name }}</el-descriptions-item>
              <el-descriptions-item label="当前持有方">{{ receivable?.holder?.name }}</el-descriptions-item>
              <el-descriptions-item label="票面金额">
                <span style="font-size: 18px; font-weight: 600; color: #409EFF">
                  ¥{{ formatMoney(receivable?.faceValue) }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="剩余金额">
                ¥{{ formatMoney(receivable?.remainingValue) }}
              </el-descriptions-item>
              <el-descriptions-item label="签发日期">{{ receivable?.issueDate }}</el-descriptions-item>
              <el-descriptions-item label="到期日期">
                {{ receivable?.maturityDate }}
                <el-tag :type="receivable?.daysToMaturity <= 30 ? 'danger' : 'success'" size="small" style="margin-left: 8px">
                  剩余{{ receivable?.daysToMaturity }}天
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="年化利率">{{ ((receivable?.annualRate || 0) * 100).toFixed(2) }}%</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="statusType[receivable?.status]">{{ statusLabels[receivable?.status] }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="交易哈希" :span="2">
                <span class="hash-text">{{ receivable?.txHash || '-' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">
                {{ receivable?.description || '无' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 电子合同存证 -->
          <div class="card-box" style="margin-top: 20px">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px">
              <h3>电子合同存证</h3>
              <el-button
                v-if="!receivable?.contractHash && userStore.enterprise?.id === receivable?.issuer?.id"
                type="primary"
                size="small"
                @click="showUploadDialog = true"
              >
                上传合同
              </el-button>
            </div>

            <div v-if="contracts.length > 0">
              <el-table :data="contracts" style="width: 100%">
                <el-table-column prop="fileName" label="文件名" min-width="200">
                  <template #default="{ row }">
                    <el-icon><Document /></el-icon>
                    {{ row.fileName }}
                  </template>
                </el-table-column>
                <el-table-column prop="fileType" label="类型" width="80">
                  <template #default="{ row }">
                    <el-tag size="small">{{ row.fileType.toUpperCase() }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="fileSizeFormatted" label="大小" width="100" />
                <el-table-column label="SHA256哈希" min-width="200">
                  <template #default="{ row }">
                    <el-tooltip :content="row.fileHash" placement="top">
                      <span class="hash-text">{{ row.fileHash.slice(0, 16) }}...</span>
                    </el-tooltip>
                  </template>
                </el-table-column>
                <el-table-column label="IPFS存证" width="160">
                  <template #default="{ row }">
                    <template v-if="row.ipfsCid">
                      <el-tooltip :content="row.ipfsCid" placement="top">
                        <a :href="row.ipfsUrl" target="_blank" class="ipfs-link">
                          {{ row.ipfsCid.slice(0, 12) }}...
                        </a>
                      </el-tooltip>
                    </template>
                    <span v-else style="color: #909399">未上传</span>
                  </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="上传时间" width="160">
                  <template #default="{ row }">
                    {{ formatDateTime(row.createdAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="180" fixed="right">
                  <template #default="{ row }">
                    <el-button size="small" type="primary" @click="downloadContract(row)">
                      下载
                    </el-button>
                    <el-button size="small" type="success" @click="verifyContract(row)" :loading="verifyingId === row.id">
                      验证
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <el-empty v-else description="暂无合同存证">
              <template v-if="!receivable?.contractHash && userStore.enterprise?.id === receivable?.issuer?.id">
                <el-button type="primary" @click="showUploadDialog = true">上传合同</el-button>
              </template>
            </el-empty>
          </div>

          <div class="card-box" style="margin-top: 20px">
            <h3 style="margin-bottom: 20px">流转历史</h3>
            <el-timeline v-if="transferHistory.length > 0">
              <el-timeline-item
                v-for="item in transferHistory"
                :key="item.id"
                :timestamp="formatDateTime(item.createdAt)"
                :type="timelineType[item.type]"
              >
                <el-card>
                  <template #header>
                    <el-tag :type="typeColors[item.type]">{{ typeLabels[item.type] }}</el-tag>
                  </template>
                  <p>{{ item.fromEnterprise?.name || '-' }} → {{ item.toEnterprise?.name || '-' }}</p>
                  <p style="color: #409EFF">金额: ¥{{ formatMoney(item.amount) }}</p>
                  <p v-if="item.reason" style="color: #909399; font-size: 12px">{{ item.reason }}</p>
                  <p v-if="item.txHash" style="color: #909399; font-size: 12px">
                    TxHash: {{ item.txHash.slice(0, 20) }}...
                  </p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无流转记录" />
          </div>

          <!-- 融资记录 -->
          <div class="card-box" style="margin-top: 20px" v-if="financingRecords.length > 0">
            <h3 style="margin-bottom: 20px">融资记录</h3>
            <el-table :data="financingRecords" style="width: 100%">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column label="金融机构">
                <template #default="{ row }">{{ row.financier?.name }}</template>
              </el-table-column>
              <el-table-column label="融资金额" width="130">
                <template #default="{ row }">¥{{ formatMoney(row.financingAmount) }}</template>
              </el-table-column>
              <el-table-column label="利率" width="90">
                <template #default="{ row }">{{ ((row.annualRate || 0) * 100).toFixed(2) }}%</template>
              </el-table-column>
              <el-table-column label="实际放款" width="130">
                <template #default="{ row }">
                  <span style="color: #67C23A">¥{{ formatMoney(row.actualAmount) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
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
            </el-table>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="card-box">
            <h3 style="margin-bottom: 20px">签发企业信息</h3>
            <el-descriptions :column="1" size="small">
              <el-descriptions-item label="企业名称">{{ receivable?.issuer?.name }}</el-descriptions-item>
              <el-descriptions-item label="统一信用代码">{{ receivable?.issuer?.creditCode }}</el-descriptions-item>
              <el-descriptions-item label="信用评分">
                <el-progress
                  :percentage="(receivable?.issuer?.creditScore || 0) / 10"
                  :color="getScoreColor(receivable?.issuer?.creditScore)"
                  :format="() => receivable?.issuer?.creditScore"
                />
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="card-box" style="margin-top: 20px">
            <h3 style="margin-bottom: 20px">持有企业信息</h3>
            <el-descriptions :column="1" size="small">
              <el-descriptions-item label="企业名称">{{ receivable?.holder?.name }}</el-descriptions-item>
              <el-descriptions-item label="统一信用代码">{{ receivable?.holder?.creditCode }}</el-descriptions-item>
              <el-descriptions-item label="信用评分">
                <el-progress
                  :percentage="(receivable?.holder?.creditScore || 0) / 10"
                  :color="getScoreColor(receivable?.holder?.creditScore)"
                  :format="() => receivable?.holder?.creditScore"
                />
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="card-box" style="margin-top: 20px" v-if="riskAssessment">
            <h3 style="margin-bottom: 20px">风险评估</h3>
            <el-descriptions :column="1" size="small">
              <el-descriptions-item label="风险等级">
                <el-tag :type="riskLevelType[riskAssessment.riskLevel]">
                  {{ riskLevelLabels[riskAssessment.riskLevel] }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="建议利率">
                {{ ((riskAssessment.suggestedRate || 0) * 100).toFixed(2) }}%
              </el-descriptions-item>
              <el-descriptions-item label="最大融资额">
                ¥{{ formatMoney(riskAssessment.maxFinancingAmount) }}
              </el-descriptions-item>
            </el-descriptions>
            <div v-if="riskAssessment.riskFactors?.length" style="margin-top: 10px">
              <p style="font-size: 12px; color: #909399; margin-bottom: 8px">风险因素:</p>
              <el-tag
                v-for="(f, i) in riskAssessment.riskFactors"
                :key="i"
                size="small"
                type="warning"
                style="margin: 2px"
              >
                {{ f }}
              </el-tag>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 上传合同对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传电子合同" width="500px">
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
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word、图片，大小不超过 10MB
          </div>
        </template>
      </el-upload>

      <el-form-item label="合同描述" style="margin-top: 20px">
        <el-input v-model="contractDescription" type="textarea" placeholder="输入合同描述（可选）" />
      </el-form-item>

      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading" :disabled="!selectedFile">
          上传并存证
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getReceivableDetail } from '@/api/receivable'
import { getContractsByReceivable, uploadContract, verifyContract as verifyContractApi, getContractDownloadUrl } from '@/api/contract'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, UploadFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const receivable = ref(null)
const riskAssessment = ref(null)
const transferHistory = ref([])
const financingRecords = ref([])
const contracts = ref([])

// 上传相关
const showUploadDialog = ref(false)
const uploadRef = ref()
const selectedFile = ref(null)
const contractDescription = ref('')
const uploading = ref(false)
const verifyingId = ref(null)

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

const timelineType = {
  ISSUE: 'primary',
  TRANSFER: 'success',
  SPLIT: 'warning',
  FINANCING: 'danger',
  SETTLEMENT: 'info'
}

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

const riskLevelLabels = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' }
const riskLevelType = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' }

function formatMoney(value) {
  if (!value) return '0'
  return Number(value).toLocaleString('zh-CN')
}

function formatDateTime(dateTime) {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').substring(0, 19)
}

function getScoreColor(score) {
  if (score >= 800) return '#67C23A'
  if (score >= 600) return '#E6A23C'
  return '#F56C6C'
}

// 合同相关
function handleFileChange(file) {
  selectedFile.value = file.raw
}

function handleFileRemove() {
  selectedFile.value = null
}

async function handleUpload() {
  if (!selectedFile.value) return

  uploading.value = true
  try {
    await uploadContract(selectedFile.value, receivable.value.id, contractDescription.value)
    ElMessage.success('合同上传成功')
    showUploadDialog.value = false
    selectedFile.value = null
    contractDescription.value = ''
    if (uploadRef.value) {
      uploadRef.value.clearFiles()
    }
    loadContracts()
    loadData()  // 刷新通证详情
  } catch (error) {
    console.error(error)
  } finally {
    uploading.value = false
  }
}

function downloadContract(contract) {
  const token = localStorage.getItem('token')
  const url = getContractDownloadUrl(contract.id)
  // 创建隐藏的a标签下载
  const link = document.createElement('a')
  link.href = url + '?token=' + token
  link.target = '_blank'
  link.click()
}

async function verifyContract(contract) {
  verifyingId.value = contract.id
  try {
    const res = await verifyContractApi(contract.id)
    const result = res.data

    if (result.valid) {
      ElMessageBox.alert(
        `<div>
          <p style="color: #67C23A; font-size: 16px; margin-bottom: 10px;">✓ 验证通过</p>
          <p>文件哈希: <code>${result.fileHash}</code></p>
          <p style="color: #909399; font-size: 12px;">文件未被篡改，与存证记录一致</p>
        </div>`,
        '合同验证结果',
        { dangerouslyUseHTMLString: true, type: 'success' }
      )
    } else {
      ElMessageBox.alert(
        `<div>
          <p style="color: #F56C6C; font-size: 16px; margin-bottom: 10px;">✗ 验证失败</p>
          <p style="color: #909399;">文件可能已被篡改！</p>
        </div>`,
        '合同验证结果',
        { dangerouslyUseHTMLString: true, type: 'error' }
      )
    }
  } catch (error) {
    console.error(error)
  } finally {
    verifyingId.value = null
  }
}

async function loadContracts() {
  const id = route.params.id
  try {
    const res = await getContractsByReceivable(id)
    contracts.value = res.data || []
  } catch (error) {
    console.error(error)
  }
}

async function loadData() {
  const id = route.params.id
  loading.value = true

  try {
    const res = await getReceivableDetail(id)
    if (res.data) {
      receivable.value = res.data.receivable
      transferHistory.value = res.data.transferHistory || []
      financingRecords.value = res.data.financingRecords || []
      riskAssessment.value = res.data.riskAssessment
    }
  } catch (error) {
    console.error('加载通证详情失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
  loadContracts()
})
</script>

<style scoped>
.hash-text {
  font-family: monospace;
  word-break: break-all;
}

.ipfs-link {
  font-family: monospace;
  color: #409eff;
  text-decoration: none;
  cursor: pointer;
}
.ipfs-link:hover {
  text-decoration: underline;
}

.contract-upload {
  width: 100%;
}

.contract-upload :deep(.el-upload-dragger) {
  width: 100%;
}
</style>
