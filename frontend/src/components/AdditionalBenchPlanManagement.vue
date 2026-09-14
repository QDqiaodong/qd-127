<template>
  <div class="additional-bench-management">
    <el-card>
      <template #header>
        <div class="header">
          <h3>节假日临时加凳预案</h3>
          <div>
            <el-button type="primary" @click="openCreateDialog">
              <el-icon><Plus /></el-icon>
              新增加凳预案
            </el-button>
            <el-button @click="reloadAll">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="预案提交后，街区树对应路段会展示待投放数量；计划日期已过且未投放完的预案自动标记逾期。"
        class="rule-alert"
      />

      <div class="filter-section">
        <el-select v-model="filters.districtId" placeholder="选择街区" class="filter-item" clearable @change="handleDistrictFilterChange">
          <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
        </el-select>
        <el-select v-model="filters.sectionId" placeholder="选择路段" class="filter-item" clearable :disabled="!filters.districtId">
          <el-option v-for="s in filteredSections" :key="s.id" :value="s.id" :label="s.name" />
        </el-select>
        <el-select v-model="filters.status" placeholder="预案状态" class="filter-item" clearable @change="loadPlans">
          <el-option label="待投放" :value="1" />
          <el-option label="已投放" :value="2" />
          <el-option label="逾期" :value="3" />
        </el-select>
        <el-button type="primary" @click="loadPlans">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <div v-if="plans.length > 0" class="result-summary">
        共 <b>{{ plans.length }}</b> 条{{ filters.status ? `（${statusLabel(filters.status)}）` : '' }}预案
        <template v-if="pendingBenchTotal > 0">
          ，待投放合计 <b>{{ pendingBenchTotal }}</b> 张
        </template>
        <span class="summary-tip">与街区树路段上的“待加凳/逾期”标记同一口径，可直接对照</span>
      </div>

      <el-table :data="plans" v-loading="loading" border class="data-table">
        <el-table-column prop="districtName" label="街区" width="150" show-overflow-tooltip />
        <el-table-column prop="sectionName" label="投放路段" width="170" show-overflow-tooltip />
        <el-table-column prop="planDate" label="计划日期" width="110" />
        <el-table-column label="加凳数量" width="100" align="center">
          <template #default="{ row }">{{ row.benchCount }}</template>
        </el-table-column>
        <el-table-column label="已核销/待投放" width="130" align="center">
          <template #default="{ row }">
            <span>{{ row.verifiedCount }} / {{ row.pendingCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预案状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.effectiveStatus)">
              {{ statusLabel(row.effectiveStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近核销人" width="110">
          <template #default="{ row }">{{ row.lastVerifiedBy || '-' }}</template>
        </el-table-column>
        <el-table-column prop="lastVerifiedAt" label="最近核销时间" width="170">
          <template #default="{ row }">{{ row.lastVerifiedAt || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDetailDialog(row)">详情</el-button>
            <el-button
              size="small"
              link
              type="primary"
              :disabled="row.status === 2"
              @click="openVerifyDialog(row)"
            >
              核销
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="emptyDescription" :image-size="60" />
        </template>
      </el-table>
    </el-card>

    <el-dialog v-model="createDialogVisible" title="新增加凳预案" width="620px" @closed="resetCreateForm">
      <el-form :model="createForm" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="所属街区" required>
              <el-select v-model="createForm.districtId" placeholder="请选择街区" style="width: 100%" @change="handleCreateDistrictChange">
                <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="投放路段" required>
              <el-select v-model="createForm.sectionId" placeholder="请选择路段" style="width: 100%" :disabled="!createForm.districtId">
                <el-option v-for="s in createSections" :key="s.id" :value="s.id" :label="s.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="计划日期" required>
              <el-date-picker
                v-model="createForm.planDate"
                type="date"
                placeholder="请选择计划日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="加凳数量" required>
              <el-input-number v-model="createForm.benchCount" :min="0" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="预案状态">
          <el-tag type="warning" effect="plain">待投放（提交后按计划日期自动判断逾期）</el-tag>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateSubmit">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="verifyDialogVisible" title="加凳投放核销" width="560px" @closed="resetVerifyForm">
      <template v-if="currentPlan">
        <el-descriptions :column="1" border size="small" class="verify-desc">
          <el-descriptions-item label="投放路段">{{ currentPlan.districtName }} / {{ currentPlan.sectionName }}</el-descriptions-item>
          <el-descriptions-item label="计划日期">{{ currentPlan.planDate }}</el-descriptions-item>
          <el-descriptions-item label="数量情况">
            计划 {{ currentPlan.benchCount }} 张，已核销 {{ currentPlan.verifiedCount }} 张，待投放
            <b :class="currentPlan.overdue ? 'overdue-text' : ''">{{ currentPlan.pendingCount }}</b> 张
          </el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag size="small" :type="statusTagType(currentPlan.effectiveStatus)">
              {{ statusLabel(currentPlan.effectiveStatus) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-form :model="verifyForm" label-width="100px" class="verify-form">
          <el-form-item label="本次投放" required>
            <el-input-number v-model="verifyForm.verifiedCount" :min="1" :max="currentPlan.pendingCount" :step="1" />
            <span class="form-tip">最多可核销 {{ currentPlan.pendingCount }} 张，支持分批核销</span>
          </el-form-item>
          <el-form-item label="操作人" required>
            <el-input v-model="verifyForm.operator" placeholder="请输入核销操作人" maxlength="50" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="verifyForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="verifyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleVerifySubmit">确认核销</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="加凳预案详情" width="760px">
      <template v-if="detail">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="街区">{{ detail.districtName }}</el-descriptions-item>
          <el-descriptions-item label="路段">{{ detail.sectionName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="statusTagType(detail.effectiveStatus)">
              {{ statusLabel(detail.effectiveStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="计划日期">{{ detail.planDate }}</el-descriptions-item>
          <el-descriptions-item label="计划数量">{{ detail.benchCount }}</el-descriptions-item>
          <el-descriptions-item label="待投放">{{ detail.pendingCount }}</el-descriptions-item>
          <el-descriptions-item label="最近核销人">{{ detail.lastVerifiedBy || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近核销时间">{{ detail.lastVerifiedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="log-title">投放核销记录</div>
        <el-table :data="detail.logs || []" border size="small" max-height="300">
          <el-table-column prop="verifiedAt" label="核销时间" width="170" />
          <el-table-column label="本次数量" width="90" align="center">
            <template #default="{ row }">{{ row.verifiedCount }}</template>
          </el-table-column>
          <el-table-column label="累计变化" width="120" align="center">
            <template #default="{ row }">{{ row.beforeCount }} → {{ row.afterCount }}</template>
          </el-table-column>
          <el-table-column prop="operator" label="操作人" width="110" />
          <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无核销记录" :image-size="60" />
          </template>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getTree } from '../api/tree'
import { treeEvents } from '../api/treeEvents'
import {
  createAdditionalBenchPlan,
  getAdditionalBenchPlanDetail,
  getAdditionalBenchPlans,
  verifyAdditionalBenchPlan
} from '../api/additionalBench'

const districts = ref([])
const sectionMap = ref({})
const plans = ref([])
const loading = ref(false)
const submitting = ref(false)

const filters = ref({
  districtId: null,
  sectionId: null,
  status: null
})

// 筛选项持久化：关掉页面再打开时恢复上次的筛选条件，保证列表条数与树上标记仍对得上
const FILTER_STORAGE_KEY = 'additional-bench-plan-filters'

const restoreFilters = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(FILTER_STORAGE_KEY))
    if (saved && typeof saved === 'object') {
      filters.value = {
        districtId: saved.districtId ?? null,
        sectionId: saved.sectionId ?? null,
        status: [1, 2, 3].includes(saved.status) ? saved.status : null
      }
    }
  } catch (error) {
    localStorage.removeItem(FILTER_STORAGE_KEY)
  }
}

const persistFilters = () => {
  localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify({
    districtId: filters.value.districtId,
    sectionId: filters.value.sectionId,
    status: filters.value.status
  }))
}

const createDialogVisible = ref(false)
const createForm = ref({
  districtId: null,
  sectionId: null,
  planDate: '',
  benchCount: 1,
  remark: ''
})

const verifyDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const currentPlan = ref(null)
const detail = ref(null)
const verifyForm = ref({
  verifiedCount: 1,
  operator: '',
  remark: ''
})

const createSections = computed(() => sectionMap.value[createForm.value.districtId] || [])
const filteredSections = computed(() => sectionMap.value[filters.value.districtId] || [])

const statusLabel = (status) => ({
  1: '待投放',
  2: '已投放',
  3: '逾期'
}[status] || '未知')

const statusTagType = (status) => ({
  1: 'warning',
  2: 'success',
  3: 'danger'
}[status] || 'info')

// 当前筛选结果的待投放合计：与街区树路段“待加凳 N”标记同源，便于逐路段对数
const pendingBenchTotal = computed(() =>
  plans.value.reduce((sum, plan) => sum + (plan.pendingCount || 0), 0)
)

// 空列表提示：明确区分是某类状态没有预案，还是所选街区/路段没有预案
const emptyDescription = computed(() => {
  if (filters.value.status) {
    return `暂无${statusLabel(filters.value.status)}的加凳预案`
  }
  if (filters.value.sectionId || filters.value.districtId) {
    return '所选街区/路段暂无加凳预案'
  }
  return '暂无加凳预案，可点击右上角“新增加凳预案”创建'
})

const loadTreeData = async () => {
  const tree = await getTree()
  districts.value = tree
  const map = {}
  tree.forEach(district => {
    map[district.id] = district.children || []
  })
  sectionMap.value = map
  // 恢复的上次筛选项可能已失效（街区/路段被删除），清空悬空项避免查出空结果却说不清原因
  if (filters.value.districtId && !map[filters.value.districtId]) {
    filters.value.districtId = null
    filters.value.sectionId = null
  } else if (filters.value.sectionId
    && !(map[filters.value.districtId] || []).some(s => s.id === filters.value.sectionId)) {
    filters.value.sectionId = null
  }
}

const loadPlans = async () => {
  loading.value = true
  try {
    plans.value = await getAdditionalBenchPlans({
      districtId: filters.value.districtId || undefined,
      sectionId: filters.value.sectionId || undefined,
      status: filters.value.status || undefined
    })
    persistFilters()
  } catch (error) {
    ElMessage.error(error.message || '加载加凳预案失败')
  } finally {
    loading.value = false
  }
}

const reloadAll = async () => {
  try {
    await loadTreeData()
    await loadPlans()
  } catch (error) {
    ElMessage.error(error.message || '刷新失败')
  }
}

const handleDistrictFilterChange = () => {
  const sections = sectionMap.value[filters.value.districtId] || []
  if (!sections.some(s => s.id === filters.value.sectionId)) {
    filters.value.sectionId = null
  }
}

const resetFilters = () => {
  filters.value = { districtId: null, sectionId: null, status: null }
  loadPlans()
}

const handleCreateDistrictChange = () => {
  createForm.value.sectionId = null
}

const openCreateDialog = () => {
  resetCreateForm()
  createDialogVisible.value = true
}

const resetCreateForm = () => {
  createForm.value = {
    districtId: null,
    sectionId: null,
    planDate: '',
    benchCount: 1,
    remark: ''
  }
}

const handleCreateSubmit = async () => {
  if (!createForm.value.districtId) {
    ElMessage.warning('请选择街区')
    return
  }
  if (!createForm.value.sectionId) {
    ElMessage.warning('请选择投放路段')
    return
  }
  if (!createForm.value.planDate) {
    ElMessage.warning('请选择计划日期')
    return
  }
  if (createForm.value.benchCount === null || createForm.value.benchCount === undefined) {
    ElMessage.warning('请填写加凳数量')
    return
  }
  if (createForm.value.benchCount <= 0) {
    ElMessage.warning('加凳数量必须大于0')
    return
  }

  try {
    submitting.value = true
    await createAdditionalBenchPlan({
      districtId: createForm.value.districtId,
      sectionId: createForm.value.sectionId,
      planDate: createForm.value.planDate,
      benchCount: createForm.value.benchCount,
      remark: createForm.value.remark?.trim() || null
    })
    ElMessage.success('加凳预案提交成功')
    createDialogVisible.value = false
    // 同步本页树下拉并通知街区树刷新：新预案会在路段上挂出待加凳数量
    await loadTreeData()
    await loadPlans()
    treeEvents.emitTreeChanged()
  } catch (error) {
    ElMessage.error(error.message || '提交加凳预案失败')
  } finally {
    submitting.value = false
  }
}

const openVerifyDialog = (row) => {
  currentPlan.value = row
  verifyForm.value = {
    verifiedCount: Math.min(1, row.pendingCount),
    operator: '',
    remark: ''
  }
  verifyDialogVisible.value = true
}

const resetVerifyForm = () => {
  currentPlan.value = null
  verifyForm.value = { verifiedCount: 1, operator: '', remark: '' }
}

const handleVerifySubmit = async () => {
  if (!currentPlan.value) return
  if (!verifyForm.value.verifiedCount || verifyForm.value.verifiedCount <= 0) {
    ElMessage.warning('核销数量必须大于0')
    return
  }
  if (verifyForm.value.verifiedCount > currentPlan.value.pendingCount) {
    ElMessage.warning('核销数量不能大于待投放数量')
    return
  }
  if (!verifyForm.value.operator.trim()) {
    ElMessage.warning('请填写核销操作人')
    return
  }

  try {
    submitting.value = true
    const updated = await verifyAdditionalBenchPlan(currentPlan.value.id, {
      verifiedCount: verifyForm.value.verifiedCount,
      operator: verifyForm.value.operator.trim(),
      remark: verifyForm.value.remark?.trim() || null
    })
    ElMessage.success(updated.status === 2 ? '已全部投放核销完成' : '核销成功')
    verifyDialogVisible.value = false
    if (detailDialogVisible.value && detail.value?.id === updated.id) {
      detail.value = updated
    }
    // 同步刷新列表与街区树：部分核销后树上待加凳数量=剩余待投放，全部核销后清零并去掉逾期标
    await reloadAll()
    treeEvents.emitTreeChanged()
  } catch (error) {
    ElMessage.error(error.message || '核销失败')
  } finally {
    submitting.value = false
  }
}

const openDetailDialog = async (row) => {
  detail.value = null
  detailDialogVisible.value = true
  try {
    detail.value = await getAdditionalBenchPlanDetail(row.id)
  } catch (error) {
    ElMessage.error(error.message || '加载预案详情失败')
  }
}

onMounted(() => {
  restoreFilters()
  reloadAll()
})
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h3 {
  margin: 0;
}

.rule-alert {
  margin-bottom: 16px;
}

.filter-section {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-item {
  width: 180px;
}

.result-summary {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}

.result-summary b {
  color: #303133;
}

.summary-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.data-table {
  width: 100%;
}

.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.verify-desc,
.verify-form {
  margin-top: 12px;
}

.overdue-text {
  color: #f56c6c;
}

.log-title {
  margin: 18px 0 10px;
  font-weight: 600;
}
</style>
