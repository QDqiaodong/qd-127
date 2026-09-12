<template>
  <div class="lighting-management">
    <el-card>
      <template #header>
        <div class="header">
          <h3>夜间照明巡查管理</h3>
          <div>
            <el-button type="primary" @click="openCreateDialog()">
              <el-icon><Plus /></el-icon>
              登记照明巡查
            </el-button>
            <el-button @click="reloadAll">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- ============ 点位照明状态 ============ -->
        <el-tab-pane label="点位照明状态" name="status">
          <div class="filter-section">
            <el-select v-model="statusFilterDistrict" placeholder="选择街区" class="filter-item" @change="onStatusDistrictChange">
              <el-option label="全部街区" :value="null" />
              <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
            </el-select>
            <el-select v-model="statusFilterSection" placeholder="选择路段" class="filter-item" :disabled="!statusFilterDistrict" @change="onStatusSectionChange">
              <el-option label="全部路段" :value="null" />
              <el-option v-for="s in statusSections" :key="s.id" :value="s.id" :label="s.name" />
            </el-select>
            <el-select v-model="statusFilterPoint" placeholder="选择点位" class="filter-item" :disabled="!statusFilterSection">
              <el-option label="全部点位" :value="null" />
              <el-option v-for="p in statusPoints" :key="p.id" :value="p.id" :label="p.name" />
            </el-select>
            <el-select v-model="statusFilterInspected" placeholder="巡查状态" class="filter-item" @change="loadStatuses">
              <el-option label="全部点位" :value="null" />
              <el-option label="已巡查" :value="true" />
              <el-option label="未巡查" :value="false" />
            </el-select>
            <el-select v-model="statusFilterResult" placeholder="照明结论" class="filter-item">
              <el-option label="全部结论" :value="null" />
              <el-option label="完好" :value="1" />
              <el-option label="异常" :value="0" />
            </el-select>
            <el-button @click="resetStatusFilter">重置</el-button>
          </div>

          <el-table :data="filteredStatuses" border class="data-table">
            <el-table-column prop="districtName" label="街区" />
            <el-table-column prop="sectionName" label="路段" />
            <el-table-column prop="pointName" label="点位" />
            <el-table-column label="巡查状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.inspected ? 'primary' : 'info'" size="small">
                  {{ row.inspected ? '已巡查' : '未巡查' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最近照明结论" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.inspected" :type="lightingResultTagType(row.latestResult)" size="small">
                  {{ lightingResultLabel(row.latestResult) }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="异常类型" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.inspected && row.latestResult === 0" type="danger" size="small" effect="plain">
                  {{ row.problemType }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="灯具数量" width="90">
              <template #default="{ row }">{{ row.inspected ? row.lampCount : '-' }}</template>
            </el-table-column>
            <el-table-column label="巡查人" width="100">
              <template #default="{ row }">{{ row.inspector || '-' }}</template>
            </el-table-column>
            <el-table-column label="最近巡查时间" width="170">
              <template #default="{ row }">{{ row.latestInspectedAt || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openCreateDialog(row)">登记</el-button>
                <el-button size="small" link type="primary" @click="openPointDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ============ 照明巡查记录 ============ -->
        <el-tab-pane label="照明巡查记录" name="records">
          <div class="filter-section">
            <el-select v-model="recFilterDistrict" placeholder="选择街区" class="filter-item" @change="onRecDistrictChange">
              <el-option label="全部街区" :value="null" />
              <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
            </el-select>
            <el-select v-model="recFilterSection" placeholder="选择路段" class="filter-item" :disabled="!recFilterDistrict" @change="onRecSectionChange">
              <el-option label="全部路段" :value="null" />
              <el-option v-for="s in recSections" :key="s.id" :value="s.id" :label="s.name" />
            </el-select>
            <el-select v-model="recFilterPoint" placeholder="选择点位" class="filter-item" :disabled="!recFilterSection" @change="loadRecords">
              <el-option label="全部点位" :value="null" />
              <el-option v-for="p in recPoints" :key="p.id" :value="p.id" :label="p.name" />
            </el-select>
            <el-select v-model="recFilterResult" placeholder="照明结论" class="filter-item" @change="loadRecords">
              <el-option label="全部结论" :value="null" />
              <el-option label="完好" :value="1" />
              <el-option label="异常" :value="0" />
            </el-select>
            <el-input v-model="recKeyword" placeholder="搜索点位/巡查人/异常类型" class="filter-item wide" @keyup.enter="loadRecords" />
            <el-button type="primary" @click="loadRecords">查询</el-button>
            <el-button @click="resetRecFilter">重置</el-button>
          </div>

          <el-table :data="filteredRecords" border class="data-table">
            <el-table-column prop="districtName" label="街区" />
            <el-table-column prop="sectionName" label="路段" />
            <el-table-column prop="pointName" label="点位" />
            <el-table-column prop="inspectedAt" label="巡查时间" width="170" />
            <el-table-column label="照明结论" width="90">
              <template #default="{ row }">
                <el-tag :type="lightingResultTagType(row.result)" size="small">
                  {{ lightingResultLabel(row.result) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="异常类型" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.result === 0" type="danger" size="small" effect="plain">{{ row.problemType }}</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="lampCount" label="灯具数量" width="90" />
            <el-table-column prop="description" label="问题描述" show-overflow-tooltip>
              <template #default="{ row }">{{ row.description || '-' }}</template>
            </el-table-column>
            <el-table-column prop="inspector" label="巡查人" width="100" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ============ 登记照明巡查弹窗 ============ -->
    <el-dialog v-model="createDialogVisible" title="登记夜间照明巡查" width="560px" @closed="resetCreateForm">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="街区" required>
          <el-select v-model="createForm.districtId" placeholder="请选择街区" style="width: 100%" @change="onCreateDistrictChange">
            <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="路段" required>
          <el-select v-model="createForm.sectionId" placeholder="请选择路段" style="width: 100%" :disabled="!createForm.districtId" @change="onCreateSectionChange">
            <el-option v-for="s in createSections" :key="s.id" :value="s.id" :label="s.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="点位" required>
          <el-select v-model="createForm.pointId" placeholder="请选择点位（必选）" style="width: 100%" :disabled="!createForm.sectionId">
            <el-option v-for="p in createPoints" :key="p.id" :value="p.id" :label="p.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="巡查时间" required>
          <el-date-picker
            v-model="createForm.inspectedAt"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择巡查时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="照明结论" required>
          <el-radio-group v-model="createForm.result">
            <el-radio-button :value="1">完好</el-radio-button>
            <el-radio-button :value="0">异常</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="灯具数量" required>
          <el-input-number v-model="createForm.lampCount" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="巡查人" required>
          <el-input v-model="createForm.inspector" placeholder="请输入巡查人" />
        </el-form-item>
        <el-form-item v-if="createForm.result === 0" label="异常类型" required>
          <el-select v-model="createForm.problemType" placeholder="请点名异常类型：缺灯 或 损坏" style="width: 100%">
            <el-option v-for="t in LIGHTING_PROBLEM_TYPES" :key="t" :value="t" :label="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            :placeholder="createForm.result === 0 ? '请补充说明缺灯或损坏情况（选填）' : '选填'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交登记</el-button>
      </template>
    </el-dialog>

    <!-- ============ 点位照明详情弹窗 ============ -->
    <el-dialog v-model="detailDialogVisible" title="点位照明详情" width="720px">
      <el-descriptions :column="2" border size="small" class="detail-desc">
        <el-descriptions-item label="街区">{{ detailStatus?.districtName }}</el-descriptions-item>
        <el-descriptions-item label="路段">{{ detailStatus?.sectionName }}</el-descriptions-item>
        <el-descriptions-item label="点位" :span="2">{{ detailStatus?.pointName }}</el-descriptions-item>
        <el-descriptions-item label="最近照明结论">
          <el-tag :type="lightingResultTagType(detailStatus?.inspected ? detailStatus?.latestResult : null)" size="small">
            {{ detailStatus?.inspected ? lightingResultLabel(detailStatus?.latestResult) : '未巡查' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="异常类型">
          <span v-if="detailStatus?.inspected && detailStatus?.latestResult === 0">{{ detailStatus?.problemType }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="灯具数量">{{ detailStatus?.inspected ? detailStatus?.lampCount : '-' }}</el-descriptions-item>
        <el-descriptions-item label="巡查人">{{ detailStatus?.inspector || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近巡查时间" :span="2">{{ detailStatus?.latestInspectedAt || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="detailRecords.length" :data="detailRecords" border size="small" max-height="320" class="detail-table">
        <el-table-column prop="inspectedAt" label="巡查时间" width="170" />
        <el-table-column label="照明结论" width="90">
          <template #default="{ row }">
            <el-tag :type="lightingResultTagType(row.result)" size="small">
              {{ lightingResultLabel(row.result) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="异常类型" width="90">
          <template #default="{ row }">{{ row.problemType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="lampCount" label="灯具数量" width="90" />
        <el-table-column prop="description" label="问题描述" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column prop="inspector" label="巡查人" width="100" />
      </el-table>
      <el-empty v-if="!detailRecords.length" description="该点位暂无照明巡查记录" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getTree } from '../api/tree'
import {
  createLightingInspection,
  getLightingInspections,
  getPointLightingStatuses,
  getLightingInspectionsByPoint
} from '../api/lighting'
import {
  LIGHTING_PROBLEM_TYPES,
  lightingResultLabel,
  lightingResultTagType
} from '../constants/lighting'

const activeTab = ref('status')

// 树形数据
const districts = ref([])
const sectionMap = ref({})
const pointMap = ref({})
// 点位 -> 所属路段/街区，用于按上级范围筛选
const pointParentMap = ref({})

// 点位照明状态
const statuses = ref([])
const statusFilterDistrict = ref(null)
const statusFilterSection = ref(null)
const statusFilterPoint = ref(null)
const statusFilterInspected = ref(null)
const statusFilterResult = ref(null)
const statusSections = ref([])
const statusPoints = ref([])

// 巡查记录
const records = ref([])
const recFilterDistrict = ref(null)
const recFilterSection = ref(null)
const recFilterPoint = ref(null)
const recFilterResult = ref(null)
const recKeyword = ref('')
const recSections = ref([])
const recPoints = ref([])

// 登记弹窗
const createDialogVisible = ref(false)
const submitting = ref(false)
const createForm = ref({
  districtId: null,
  sectionId: null,
  pointId: null,
  inspectedAt: '',
  result: 1,
  lampCount: 0,
  inspector: '',
  problemType: null,
  description: ''
})
const createSections = ref([])
const createPoints = ref([])

// 点位详情弹窗
const detailDialogVisible = ref(false)
const detailStatus = ref(null)
const detailRecords = ref([])

const loadTreeData = async () => {
  const tree = await getTree()
  districts.value = tree
  const sm = {}
  const pm = {}
  const parent = {}
  tree.forEach(d => {
    d.children?.forEach(s => {
      sm[d.id] = sm[d.id] || []
      sm[d.id].push(s)
      s.children?.forEach(p => {
        pm[s.id] = pm[s.id] || []
        pm[s.id].push(p)
        parent[p.id] = { sectionId: s.id, districtId: d.id }
      })
    })
  })
  sectionMap.value = sm
  pointMap.value = pm
  pointParentMap.value = parent
}

// ============ 点位照明状态 ============
const loadStatuses = async () => {
  try {
    const params = {}
    if (statusFilterInspected.value !== null) {
      params.inspected = statusFilterInspected.value
    }
    statuses.value = await getPointLightingStatuses(params)
  } catch (e) {
    ElMessage.error(e.message || '加载点位照明状态失败')
  }
}

const filteredStatuses = computed(() => statuses.value.filter(row => {
  const parent = pointParentMap.value[row.pointId] || {}
  if (statusFilterDistrict.value && parent.districtId !== statusFilterDistrict.value) return false
  if (statusFilterSection.value && parent.sectionId !== statusFilterSection.value) return false
  if (statusFilterPoint.value && row.pointId !== statusFilterPoint.value) return false
  if (statusFilterResult.value !== null && row.latestResult !== statusFilterResult.value) return false
  return true
}))

const onStatusDistrictChange = () => {
  statusFilterSection.value = null
  statusFilterPoint.value = null
  statusSections.value = sectionMap.value[statusFilterDistrict.value] || []
  statusPoints.value = []
}
const onStatusSectionChange = () => {
  statusFilterPoint.value = null
  statusPoints.value = pointMap.value[statusFilterSection.value] || []
}
const resetStatusFilter = () => {
  statusFilterDistrict.value = null
  statusFilterSection.value = null
  statusFilterPoint.value = null
  statusFilterInspected.value = null
  statusFilterResult.value = null
  statusSections.value = []
  statusPoints.value = []
  loadStatuses()
}

// ============ 巡查记录 ============
const loadRecords = async () => {
  try {
    const params = {
      pointId: recFilterPoint.value || null,
      result: recFilterResult.value,
      keyword: recKeyword.value || null
    }
    records.value = await getLightingInspections(params)
  } catch (e) {
    ElMessage.error(e.message || '加载照明巡查记录失败')
  }
}

const filteredRecords = computed(() => records.value.filter(row => {
  const parent = pointParentMap.value[row.pointId] || {}
  if (recFilterDistrict.value && parent.districtId !== recFilterDistrict.value) return false
  if (recFilterSection.value && parent.sectionId !== recFilterSection.value) return false
  return true
}))

const onRecDistrictChange = () => {
  recFilterSection.value = null
  recFilterPoint.value = null
  recSections.value = sectionMap.value[recFilterDistrict.value] || []
  recPoints.value = []
  loadRecords()
}
const onRecSectionChange = () => {
  recFilterPoint.value = null
  recPoints.value = pointMap.value[recFilterSection.value] || []
  loadRecords()
}
const resetRecFilter = () => {
  recFilterDistrict.value = null
  recFilterSection.value = null
  recFilterPoint.value = null
  recFilterResult.value = null
  recKeyword.value = ''
  recSections.value = []
  recPoints.value = []
  loadRecords()
}

// ============ 登记照明巡查 ============
const formatNow = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const openCreateDialog = (statusRow) => {
  resetCreateForm()
  if (statusRow && statusRow.pointId) {
    const parent = pointParentMap.value[statusRow.pointId] || {}
    createForm.value.districtId = parent.districtId || null
    createSections.value = sectionMap.value[createForm.value.districtId] || []
    createForm.value.sectionId = parent.sectionId || null
    createPoints.value = pointMap.value[createForm.value.sectionId] || []
    createForm.value.pointId = statusRow.pointId
  }
  createDialogVisible.value = true
}

const resetCreateForm = () => {
  createForm.value = {
    districtId: null,
    sectionId: null,
    pointId: null,
    inspectedAt: formatNow(),
    result: 1,
    lampCount: 0,
    inspector: '',
    problemType: null,
    description: ''
  }
  createSections.value = []
  createPoints.value = []
}

const onCreateDistrictChange = () => {
  createForm.value.sectionId = null
  createForm.value.pointId = null
  createSections.value = sectionMap.value[createForm.value.districtId] || []
  createPoints.value = []
}
const onCreateSectionChange = () => {
  createForm.value.pointId = null
  createPoints.value = pointMap.value[createForm.value.sectionId] || []
}

const handleSubmit = async () => {
  const form = createForm.value
  if (!form.pointId) {
    ElMessage.warning('请先选择点位，没有点位不能提交')
    return
  }
  if (!form.inspectedAt) {
    ElMessage.warning('请选择巡查时间')
    return
  }
  if (form.lampCount === null || form.lampCount === undefined || form.lampCount < 0) {
    ElMessage.warning('灯具数量必须为大于等于0的整数')
    return
  }
  if (!form.inspector || !form.inspector.trim()) {
    ElMessage.warning('请填写巡查人')
    return
  }
  if (form.result === 0 && !form.problemType) {
    ElMessage.warning('照明异常时请点名异常类型：缺灯 或 损坏')
    return
  }

  const payload = {
    pointId: form.pointId,
    inspectedAt: form.inspectedAt,
    result: form.result,
    lampCount: form.lampCount,
    problemType: form.result === 0 ? form.problemType : null,
    description: form.description || null,
    inspector: form.inspector.trim()
  }
  try {
    submitting.value = true
    await createLightingInspection(payload)
    ElMessage.success('照明巡查登记成功')
    createDialogVisible.value = false
    await reloadAll()
  } catch (e) {
    ElMessage.error(e.message || '照明巡查登记失败')
  } finally {
    submitting.value = false
  }
}

// ============ 点位详情 ============
const openPointDetail = async (row) => {
  detailStatus.value = row
  detailRecords.value = []
  detailDialogVisible.value = true
  try {
    detailRecords.value = await getLightingInspectionsByPoint(row.pointId)
  } catch (e) {
    ElMessage.error(e.message || '加载点位照明详情失败')
  }
}

const handleTabChange = (name) => {
  if (name === 'status') loadStatuses()
  if (name === 'records') loadRecords()
}

const reloadAll = async () => {
  await Promise.all([loadTreeData(), loadStatuses(), loadRecords()])
}

onMounted(async () => {
  await loadTreeData()
  await Promise.all([loadStatuses(), loadRecords()])
})
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h3 {
  font-size: 16px;
  font-weight: 600;
}

.filter-section {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-item {
  width: 150px;
}

.filter-item.wide {
  width: 220px;
}

.data-table {
  margin-top: 6px;
}

.detail-desc {
  margin-bottom: 12px;
}

.detail-table {
  margin-top: 8px;
}
</style>
