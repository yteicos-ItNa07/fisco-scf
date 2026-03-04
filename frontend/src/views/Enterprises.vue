<template>
  <div class="page-container">
    <div class="page-header">
      <h2>企业管理</h2>
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        添加企业
      </el-button>
    </div>

    <div class="card-box">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部企业" name="all" />
        <el-tab-pane label="核心企业" name="CORE_ENTERPRISE" />
        <el-tab-pane label="供应商" name="SUPPLIER" />
        <el-tab-pane label="金融机构" name="FINANCIAL_INST" />
      </el-tabs>

      <el-table :data="enterprises" v-loading="loading" style="width: 100%">
        <el-table-column prop="creditCode" label="统一社会信用代码" width="200" />
        <el-table-column prop="name" label="企业名称" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="roleType[row.role]">{{ roleLabels[row.role] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creditScore" label="信用评分" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="row.creditScore / 10"
              :color="getScoreColor(row.creditScore)"
              :format="() => row.creditScore"
            />
          </template>
        </el-table-column>
        <el-table-column prop="blockchainAddress" label="区块链地址" width="160">
          <template #default="{ row }">
            <el-tooltip :content="row.blockchainAddress" placement="top">
              <span>{{ row.blockchainAddress?.slice(0, 10) }}...</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="isActive" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'">
              {{ row.isActive ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="viewEnterprise(row)">详情</el-button>
            <el-button size="small" type="warning" @click="editScore(row)">调整评分</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加企业对话框 -->
    <el-dialog v-model="showAddDialog" title="添加企业" width="500px">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="120px">
        <el-form-item label="统一信用代码" prop="creditCode">
          <el-input v-model="addForm.creditCode" maxlength="18" show-word-limit placeholder="18位统一社会信用代码" />
        </el-form-item>
        <el-form-item label="企业名称" prop="name">
          <el-input v-model="addForm.name" placeholder="请输入企业名称" />
        </el-form-item>
        <el-form-item label="法人代表">
          <el-input v-model="addForm.legalPerson" placeholder="请输入法人代表" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="addForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="企业地址">
          <el-input v-model="addForm.address" placeholder="请输入企业地址" />
        </el-form-item>
        <el-form-item label="企业角色" prop="role">
          <el-select v-model="addForm.role" style="width: 100%">
            <el-option label="核心企业" value="CORE_ENTERPRISE" />
            <el-option label="供应商" value="SUPPLIER" />
            <el-option label="金融机构" value="FINANCIAL_INST" />
            <el-option label="监管机构" value="REGULATOR" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAdd" :loading="addLoading">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 企业详情对话框 -->
    <el-dialog v-model="showDetailDialog" :title="currentEnterprise?.name + ' - 详情'" width="650px">
      <el-descriptions :column="2" border v-if="currentEnterprise">
        <el-descriptions-item label="企业名称" :span="2">{{ currentEnterprise.name }}</el-descriptions-item>
        <el-descriptions-item label="统一信用代码" :span="2">{{ currentEnterprise.creditCode }}</el-descriptions-item>
        <el-descriptions-item label="法人代表">{{ currentEnterprise.legalPerson || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentEnterprise.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="企业地址" :span="2">{{ currentEnterprise.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="企业角色">
          <el-tag :type="roleType[currentEnterprise.role]">{{ roleLabels[currentEnterprise.role] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentEnterprise.isActive ? 'success' : 'danger'">
            {{ currentEnterprise.isActive ? '正常' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="信用评分">
          <el-progress
            :percentage="currentEnterprise.creditScore / 10"
            :color="getScoreColor(currentEnterprise.creditScore)"
            :format="() => currentEnterprise.creditScore"
            style="width: 200px"
          />
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">
          {{ formatDateTime(currentEnterprise.createdAt) }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">区块链信息</el-divider>
      <el-descriptions :column="1" border v-if="currentEnterprise">
        <el-descriptions-item label="区块链地址">
          <span class="hash-text" :class="{ 'offline': isOfflineAddress(currentEnterprise.blockchainAddress) }">
            {{ currentEnterprise.blockchainAddress || '未注册' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="上链状态">
          <el-tag v-if="isOfflineAddress(currentEnterprise.blockchainAddress)" type="warning">离线模式</el-tag>
          <el-tag v-else-if="currentEnterprise.blockchainAddress" type="success">已上链注册</el-tag>
          <el-tag v-else type="info">未注册</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 调整评分对话框 -->
    <el-dialog v-model="showScoreDialog" title="调整信用评分" width="400px">
      <div style="text-align: center; margin-bottom: 20px">
        <p>{{ currentEnterprise?.name }}</p>
        <p style="color: #909399; font-size: 14px">当前评分: {{ currentEnterprise?.creditScore }}</p>
      </div>
      <el-slider v-model="newScore" :min="0" :max="1000" :step="10" show-input />
      <template #footer>
        <el-button @click="showScoreDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateScore" :loading="scoreLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getEnterprises, getEnterprisesByRole, registerEnterprise, updateCreditScore } from '@/api/enterprise'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const enterprises = ref([])
const activeTab = ref('all')

const showAddDialog = ref(false)
const addLoading = ref(false)
const addFormRef = ref()
const addForm = reactive({
  creditCode: '',
  name: '',
  legalPerson: '',
  phone: '',
  address: '',
  role: 'SUPPLIER'
})

const addRules = {
  creditCode: [
    { required: true, message: '请输入统一社会信用代码', trigger: 'blur' },
    { len: 18, message: '统一社会信用代码必须为18位', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入企业名称', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择企业角色', trigger: 'change' }
  ]
}

const showScoreDialog = ref(false)
const scoreLoading = ref(false)
const currentEnterprise = ref(null)
const newScore = ref(700)
const showDetailDialog = ref(false)

const roleLabels = {
  CORE_ENTERPRISE: '核心企业',
  SUPPLIER: '供应商',
  FINANCIAL_INST: '金融机构',
  REGULATOR: '监管机构'
}

const roleType = {
  CORE_ENTERPRISE: 'danger',
  SUPPLIER: 'success',
  FINANCIAL_INST: 'warning',
  REGULATOR: 'info'
}

function getScoreColor(score) {
  if (score >= 800) return '#67C23A'
  if (score >= 600) return '#E6A23C'
  return '#F56C6C'
}

async function loadEnterprises() {
  loading.value = true
  try {
    let res
    if (activeTab.value === 'all') {
      res = await getEnterprises()
    } else {
      res = await getEnterprisesByRole(activeTab.value)
    }
    enterprises.value = res.data || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  loadEnterprises()
}

function viewEnterprise(row) {
  currentEnterprise.value = row
  showDetailDialog.value = true
}

function isOfflineAddress(address) {
  return address && address.startsWith('OFFLINE_')
}

function formatDateTime(dateTime) {
  if (!dateTime) return '-'
  return String(dateTime).replace('T', ' ').substring(0, 19)
}

function editScore(row) {
  currentEnterprise.value = row
  newScore.value = row.creditScore
  showScoreDialog.value = true
}

async function handleAdd() {
  const valid = await addFormRef.value.validate().catch(() => false)
  if (!valid) return

  addLoading.value = true
  try {
    await registerEnterprise(addForm)
    ElMessage.success('企业添加成功')
    showAddDialog.value = false
    loadEnterprises()
    // 重置表单
    addFormRef.value.resetFields()
  } catch (error) {
    console.error(error)
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    }
  } finally {
    addLoading.value = false
  }
}

async function handleUpdateScore() {
  scoreLoading.value = true
  try {
    await updateCreditScore(currentEnterprise.value.id, newScore.value)
    ElMessage.success('信用评分更新成功')
    showScoreDialog.value = false
    loadEnterprises()
  } catch (error) {
    console.error(error)
  } finally {
    scoreLoading.value = false
  }
}

onMounted(() => {
  loadEnterprises()
})
</script>

<style scoped>
.hash-text {
  font-family: monospace;
  word-break: break-all;
  color: #409eff;
}

.hash-text.offline {
  color: #E6A23C;
}
</style>
