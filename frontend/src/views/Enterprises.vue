<template>
  <div class="page-container">
    <div class="page-header">
      <h2>{{ localeStore.t('enterprises.title') }}</h2>
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon>
        {{ localeStore.t('enterprises.addEnterprise') }}
      </el-button>
    </div>

    <div class="card-box">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane :label="localeStore.t('enterprises.tabs.all')" name="all" />
        <el-tab-pane :label="localeStore.t('enterprises.tabs.CORE_ENTERPRISE')" name="CORE_ENTERPRISE" />
        <el-tab-pane :label="localeStore.t('enterprises.tabs.SUPPLIER')" name="SUPPLIER" />
        <el-tab-pane :label="localeStore.t('enterprises.tabs.FINANCIAL_INST')" name="FINANCIAL_INST" />
      </el-tabs>

      <el-table :data="enterprises" v-loading="loading" style="width: 100%">
        <el-table-column prop="creditCode" :label="localeStore.t('enterprises.columns.creditCode')" width="200" />
        <el-table-column prop="name" :label="localeStore.t('enterprises.columns.name')" />
        <el-table-column prop="role" :label="localeStore.t('enterprises.columns.role')" width="120">
          <template #default="{ row }">
            <el-tag :type="roleType[row.role]">{{ roleLabels[row.role] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creditScore" :label="localeStore.t('enterprises.columns.creditScore')" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="row.creditScore / 10"
              :color="getScoreColor(row.creditScore)"
              :format="() => row.creditScore"
            />
          </template>
        </el-table-column>
        <el-table-column prop="blockchainAddress" :label="localeStore.t('enterprises.columns.address')" width="160">
          <template #default="{ row }">
            <el-tooltip :content="row.blockchainAddress" placement="top">
              <span>{{ row.blockchainAddress?.slice(0, 10) }}...</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="isActive" :label="localeStore.t('enterprises.columns.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'">
              {{ row.isActive ? localeStore.t('enterprises.status.normal') : localeStore.t('enterprises.status.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="localeStore.t('enterprises.columns.action')" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="viewEnterprise(row)">{{ localeStore.t('common.details') }}</el-button>
            <el-button size="small" type="warning" @click="editScore(row)">{{ localeStore.t('enterprises.dialog.scoreTitle') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showAddDialog" :title="localeStore.t('enterprises.dialog.addTitle')" width="500px">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="120px">
        <el-form-item :label="localeStore.t('enterprises.columns.creditCode')" prop="creditCode">
          <el-input v-model="addForm.creditCode" maxlength="18" show-word-limit :placeholder="localeStore.t('enterprises.columns.creditCode')" />
        </el-form-item>
        <el-form-item :label="localeStore.t('enterprises.columns.name')" prop="name">
          <el-input v-model="addForm.name" :placeholder="localeStore.t('enterprises.validation.nameRequired')" />
        </el-form-item>
        <el-form-item :label="localeStore.t('enterprises.columns.legalPerson')">
          <el-input v-model="addForm.legalPerson" :placeholder="localeStore.t('enterprises.dialog.legalPersonPlaceholder')" />
        </el-form-item>
        <el-form-item :label="localeStore.t('enterprises.columns.phone')">
          <el-input v-model="addForm.phone" :placeholder="localeStore.t('enterprises.dialog.phonePlaceholder')" />
        </el-form-item>
        <el-form-item :label="localeStore.t('enterprises.columns.enterpriseAddress')">
          <el-input v-model="addForm.address" :placeholder="localeStore.t('enterprises.dialog.addressPlaceholder')" />
        </el-form-item>
        <el-form-item :label="localeStore.t('enterprises.columns.role')" prop="role">
          <el-select v-model="addForm.role" style="width: 100%">
            <el-option :label="localeStore.t('roles.CORE_ENTERPRISE')" value="CORE_ENTERPRISE" />
            <el-option :label="localeStore.t('roles.SUPPLIER')" value="SUPPLIER" />
            <el-option :label="localeStore.t('roles.FINANCIAL_INST')" value="FINANCIAL_INST" />
            <el-option :label="localeStore.t('roles.REGULATOR')" value="REGULATOR" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">{{ localeStore.t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleAdd" :loading="addLoading">{{ localeStore.t('enterprises.dialog.confirmAdd') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" :title="currentEnterprise?.name + ' - ' + localeStore.t('enterprises.dialog.detailTitle')" width="650px">
      <el-descriptions :column="2" border v-if="currentEnterprise">
        <el-descriptions-item :label="localeStore.t('enterprises.columns.name')" :span="2">{{ currentEnterprise.name }}</el-descriptions-item>
        <el-descriptions-item :label="localeStore.t('enterprises.columns.creditCode')" :span="2">{{ currentEnterprise.creditCode }}</el-descriptions-item>
        <el-descriptions-item :label="localeStore.t('enterprises.columns.legalPerson')">{{ currentEnterprise.legalPerson || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="localeStore.t('enterprises.columns.phone')">{{ currentEnterprise.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="localeStore.t('enterprises.columns.enterpriseAddress')" :span="2">{{ currentEnterprise.address || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="localeStore.t('enterprises.columns.role')">
          <el-tag :type="roleType[currentEnterprise.role]">{{ roleLabels[currentEnterprise.role] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="localeStore.t('enterprises.columns.status')">
          <el-tag :type="currentEnterprise.isActive ? 'success' : 'danger'">
            {{ currentEnterprise.isActive ? localeStore.t('enterprises.status.normal') : localeStore.t('enterprises.status.disabled') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="localeStore.t('enterprises.columns.creditScore')">
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

      <el-divider :content-position="'left'">{{ localeStore.t('enterprises.blockchain.title') }}</el-divider>
      <el-descriptions :column="1" border v-if="currentEnterprise">
        <el-descriptions-item :label="localeStore.t('enterprises.blockchain.address')">
          <span class="hash-text" :class="{ 'offline': isOfflineAddress(currentEnterprise.blockchainAddress) }">
            {{ currentEnterprise.blockchainAddress || localeStore.t('enterprises.status.unregistered') }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item :label="localeStore.t('enterprises.blockchain.onChainStatus')">
          <el-tag v-if="isOfflineAddress(currentEnterprise.blockchainAddress)" type="warning">{{ localeStore.t('enterprises.status.offline') }}</el-tag>
          <el-tag v-else-if="currentEnterprise.blockchainAddress" type="success">{{ localeStore.t('enterprises.status.online') }}</el-tag>
          <el-tag v-else type="info">{{ localeStore.t('enterprises.status.unregistered') }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="showDetailDialog = false">{{ localeStore.t('enterprises.dialog.close') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showScoreDialog" :title="localeStore.t('enterprises.dialog.scoreTitle')" width="400px">
      <div style="text-align: center; margin-bottom: 20px">
        <p>{{ currentEnterprise?.name }}</p>
        <p style="color: #909399; font-size: 14px">{{ localeStore.t('enterprises.columns.currentScore') }}: {{ currentEnterprise?.creditScore }}</p>
      </div>
      <el-slider v-model="newScore" :min="0" :max="1000" :step="10" show-input />
      <template #footer>
        <el-button @click="showScoreDialog = false">{{ localeStore.t('common.cancel') }}</el-button>
        <el-button type="primary" @click="handleUpdateScore" :loading="scoreLoading">{{ localeStore.t('enterprises.dialog.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getEnterprises, getEnterprisesByRole, registerEnterprise, updateCreditScore } from '@/api/enterprise'
import { ElMessage } from 'element-plus'
import { useLocaleStore } from '@/store/locale'

const localeStore = useLocaleStore()
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

const addRules = computed(() => ({
  creditCode: [
    { required: true, message: localeStore.t('enterprises.validation.creditCodeRequired'), trigger: 'blur' },
    { len: 18, message: localeStore.t('enterprises.validation.creditCodeLen'), trigger: 'blur' }
  ],
  name: [
    { required: true, message: localeStore.t('enterprises.validation.nameRequired'), trigger: 'blur' }
  ],
  role: [
    { required: true, message: localeStore.t('enterprises.validation.roleRequired'), trigger: 'change' }
  ]
}))

const showScoreDialog = ref(false)
const scoreLoading = ref(false)
const currentEnterprise = ref(null)
const newScore = ref(700)
const showDetailDialog = ref(false)

const roleLabels = computed(() => ({
  CORE_ENTERPRISE: localeStore.t('roles.CORE_ENTERPRISE'),
  SUPPLIER: localeStore.t('roles.SUPPLIER'),
  FINANCIAL_INST: localeStore.t('roles.FINANCIAL_INST'),
  REGULATOR: localeStore.t('roles.REGULATOR')
}))

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
    ElMessage.success(localeStore.t('enterprises.messages.addSuccess'))
    showAddDialog.value = false
    loadEnterprises()
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
    ElMessage.success(localeStore.t('enterprises.messages.scoreSuccess'))
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
