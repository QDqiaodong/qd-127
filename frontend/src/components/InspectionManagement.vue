<template>
  <div class="inspection-management">
    <el-card>
      <template #header>
        <div class="header">
          <h3>巡检与维修管理</h3>
          <div>
            <el-button type="primary" @click="openCreateDialog">
              <el-icon><Plus /></el-icon>
              发起巡检
            </el-button>
            <el-button @click="reloadAll">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- ============ 巡检记录 ============ -->
        <el-tab-pane label="巡检记录" name="inspection">
          <div class="filter-section">
            <el-select v-model="inspFilterDistrict" placeholder="选择街区" class="filter-item" @change="onInspDistrictChange">
              <el-option label="全部街区" :value="null" />
              <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
            </el-select>
            <el-select v-model="inspFilterSection" placeholder="选择路段" class="filter-item" :disabled="!inspFilterDistrict" @change="onInspSectionChange">
              <el-option label="全部路段" :value="null" />
              <el-option v-for="s in inspSections" :key="s.id" :value="s.id" :label="s.name" />
            </el-select>
            <el-select v-model="inspFilterPoint" placeholder="选择点位" class="filter-item" :disabled="!inspFilterSection">
              <el-option label="全部点位" :value="null" />
              <el-option v-for="p in inspPoints" :key="p.id" :value="p.id" :label="p.name" />
            </el-select>
            <el-select v-model="inspFilterResult" placeholder="巡检结果" class="filter-item">
              <el-option label="全部结果" :value="null" />
              <el-option label="正常" :value="1" />
              <el-option label="异常" :value="0" />
            </el-select>
            <el-select v-model="inspFilterSeverity" placeholder="严重程度" class="filter-item">
              <el-option label="全部程度" :value="null" />
              <el-option label="低" :value="1" />
              <el-option label="中" :value="2" />
              <el-option label="高" :value="3" />
            </el-select>
            <el-input v-model="inspKeyword" placeholder="搜索长凳编号/问题类型" class="filter-item wide" @keyup.enter="loadInspections" />
            <el-button type="primary" @click="loadInspections">查询</el-button>
            <el-button @click="resetInspFilter">重置</el-button>
          </div>

          <el-table :data="inspections" border class="data-table">
            <el-table-column prop="benchCode" label="长凳编号" width="130" />
            <el-table-column prop="districtName" label="街区" />
            <el-table-column prop="sectionName" label="路段" />
            <el-table-column prop="nodeName" label="点位" />
            <el-table-column prop="inspectedAt" label="检查时间" width="170" />
            <el-table-column label="巡检结果" width="90">
              <template #default="{ row }">
                <el-tag :type="inspectionResultTagType(row.result)" size="small">
                  {{ inspectionResultLabel(row.result) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="严重程度" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.result === 0" :type="severityTagType(row.severity)" size="small">
                  {{ severityLabel(row.severity) }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="problemType" label="问题类型" width="100">
              <template #default="{ row }">{{ row.problemType || '-' }}</template>
            </el-table-column>
            <el-table-column prop="description" label="问题描述" show-overflow-tooltip>
              <template #default="{ row }">{{ row.description || '-' }}</template>
            </el-table-column>
            <el-table-column prop="suggestion" label="处理建议" show-overflow-tooltip>
              <template #default="{ row }">{{ row.suggestion || '-' }}</template>
            </el-table-column>
            <el-table-column prop="inspector" label="检查人" width="90" />
            <el-table-column label="来源任务" width="90">
              <template #default="{ row }">
                <span v-if="row.taskId">#{{ row.taskId }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="关联工单" width="100">
              <template #default="{ row }">
                <el-button
                  v-if="row.repairOrderId"
                  link
                  type="primary"
                  @click="goToOrder(row.repairOrderId)"
                >
                  {{ row.repairOrderCode }}
                </el-button>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ============ 维修工单 ============ -->
        <el-tab-pane label="维修工单" name="order">
          <div class="filter-section">
            <el-select v-model="orderFilterStatus" placeholder="工单状态" class="filter-item">
              <el-option label="全部状态" :value="null" />
              <el-option label="待处理" :value="1" />
              <el-option label="维修中" :value="2" />
              <el-option label="已完成" :value="3" />
              <el-option label="已关闭" :value="4" />
            </el-select>
            <el-select v-model="orderFilterSeverity" placeholder="严重程度" class="filter-item">
              <el-option label="全部程度" :value="null" />
              <el-option label="低" :value="1" />
              <el-option label="中" :value="2" />
              <el-option label="高" :value="3" />
            </el-select>
            <el-input v-model="orderKeyword" placeholder="搜索工单号/长凳编号" class="filter-item wide" @keyup.enter="loadOrders" />
            <el-button type="primary" @click="loadOrders">查询</el-button>
            <el-button @click="resetOrderFilter">重置</el-button>
          </div>

          <el-table
            :data="orders"
            border
            class="data-table"
            :row-class-name="orderRowClass"
          >
            <el-table-column prop="code" label="工单号" width="180" />
            <el-table-column prop="benchCode" label="长凳编号" width="130" />
            <el-table-column prop="districtName" label="街区" />
            <el-table-column prop="sectionName" label="路段" />
            <el-table-column prop="nodeName" label="点位" />
            <el-table-column prop="problemType" label="问题类型" width="100">
              <template #default="{ row }">{{ row.problemType || '-' }}</template>
            </el-table-column>
            <el-table-column label="严重程度" width="90">
              <template #default="{ row }">
                <el-tag :type="severityTagType(row.severity)" size="small">{{ severityLabel(row.severity) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="问题描述" show-overflow-tooltip>
              <template #default="{ row }">{{ row.description || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="orderStatusTagType(row.status)" size="small">{{ orderStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="170" />
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" :disabled="row.status !== 1" @click="handleStart(row)">开始维修</el-button>
                <el-button size="small" type="success" :disabled="row.status !== 2" @click="openCompleteDialog(row)">完成</el-button>
                <el-button size="small" :disabled="row.status !== 1 && row.status !== 3" @click="handleClose(row)">关闭</el-button>
                <el-button size="small" link type="primary" @click="openOrderDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ============ 发起巡检弹窗 ============ -->
    <el-dialog v-model="createDialogVisible" title="发起巡检" width="900px" @closed="resetCreateForm">
      <el-form :model="createForm" label-width="100px" v-loading="scopeLoading" element-loading-text="正在加载范围内长凳...">
        <el-row :gutter="10">
          <el-col :span="8">
            <el-form-item label="巡检街区">
              <el-select v-model="createForm.districtId" placeholder="请选择街区" @change="onCreateDistrictChange">
                <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="巡检路段">
              <el-select v-model="createForm.sectionId" placeholder="请选择路段" :disabled="!createForm.districtId" @change="onCreateSectionChange">
                <el-option v-for="s in createSections" :key="s.id" :value="s.id" :label="s.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="巡检点位">
              <el-select
                v-model="createForm.nodeId"
                placeholder="可选择点位（不选则巡检整条路段/街区）"
                :disabled="!createForm.sectionId"
                clearable
                @change="onPointChange"
              >
                <el-option v-for="p in createPoints" :key="p.id" :value="p.id" :label="p.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="检查人">
              <el-input v-model="createForm.inspector" placeholder="请输入检查人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="统一检查时间">
              <el-date-picker
                v-model="defaultInspectedAt"
                type="datetime"
                placeholder="设置后批量应用到全部长凳"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="scope-benches" v-if="scopeBenches.length">
        <div class="scope-tip">
          该范围内共 <b>{{ scopeBenches.length }}</b> 张长凳，请逐张记录检查情况。
        </div>
        <el-table :data="inspectionItems" border size="small" max-height="360">
          <el-table-column prop="code" label="长凳编号" width="120" />
          <el-table-column prop="location" label="所在点位" width="160" />
          <el-table-column label="检查时间" width="190">
            <template #default="{ row }">
              <el-date-picker
                v-model="row.inspectedAt"
                type="datetime"
                size="small"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="检查时间"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="结果" width="130">
            <template #default="{ row }">
              <el-radio-group v-model="row.result" size="small">
                <el-radio-button :value="1">正常</el-radio-button>
                <el-radio-button :value="0">异常</el-radio-button>
              </el-radio-group>
            </template>
          </el-table-column>
          <el-table-column label="严重程度" width="95">
            <template #default="{ row }">
              <el-select v-if="row.result === 0" v-model="row.severity" size="small" placeholder="程度">
                <el-option label="低" :value="1" />
                <el-option label="中" :value="2" />
                <el-option label="高" :value="3" />
              </el-select>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="问题类型" width="120">
            <template #default="{ row }">
              <el-select v-if="row.result === 0" v-model="row.problemType" size="small" placeholder="问题类型">
                <el-option v-for="t in PROBLEM_TYPES" :key="t" :value="t" :label="t" />
              </el-select>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="问题描述" min-width="150">
            <template #default="{ row }">
              <el-input v-if="row.result === 0" v-model="row.description" size="small" placeholder="问题描述（必填）" />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="处理建议" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.suggestion" size="small" placeholder="处理建议" />
            </template>
          </el-table-column>
          <el-table-column label="生成工单" width="90">
            <template #default="{ row }">
              <el-checkbox v-if="row.result === 0" v-model="row.createRepairOrder" />
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-empty v-else-if="scopeResolved" description="所选范围内暂无长凳，无法发起巡检" :image-size="60" />

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitInspection">提交巡检</el-button>
      </template>
    </el-dialog>

    <!-- ============ 完成维修弹窗 ============ -->
    <el-dialog v-model="completeDialogVisible" title="完成维修" width="520px">
      <el-descriptions :column="1" border size="small" class="complete-desc">
        <el-descriptions-item label="工单号">{{ currentOrder?.code }}</el-descriptions-item>
        <el-descriptions-item label="长凳编号">{{ currentOrder?.benchCode }}</el-descriptions-item>
        <el-descriptions-item label="问题类型">{{ currentOrder?.problemType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="问题描述">{{ currentOrder?.description || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="completeForm" label-width="100px" class="complete-form">
        <el-form-item label="维修结果" required>
          <el-input
            v-model="completeForm.repairResult"
            type="textarea"
            :rows="3"
            placeholder="请填写维修结果（必填）"
          />
        </el-form-item>
        <el-form-item label="维修完成时间" required>
          <el-date-picker
            v-model="completeForm.completedAt"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择完成时间"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCompleteSubmit">确认完成</el-button>
      </template>
    </el-dialog>

    <!-- ============ 工单详情弹窗 ============ -->
    <el-dialog v-model="detailDialogVisible" title="工单详情" width="640px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="工单号">{{ detailOrder?.code }}</el-descriptions-item>
        <el-descriptions-item label="长凳编号">{{ detailOrder?.benchCode }}</el-descriptions-item>
        <el-descriptions-item label="街区">{{ detailOrder?.districtName }}</el-descriptions-item>
        <el-descriptions-item label="路段">{{ detailOrder?.sectionName }}</el-descriptions-item>
        <el-descriptions-item label="点位" :span="2">{{ detailOrder?.nodeName }}</el-descriptions-item>
        <el-descriptions-item label="问题类型">{{ detailOrder?.problemType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="严重程度">
          <el-tag :type="severityTagType(detailOrder?.severity)" size="small">
            {{ severityLabel(detailOrder?.severity) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="问题描述" :span="2">{{ detailOrder?.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理建议" :span="2">{{ detailOrder?.suggestion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="orderStatusTagType(detailOrder?.status)" size="small">
            {{ orderStatusLabel(detailOrder?.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailOrder?.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detailOrder?.startedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ detailOrder?.completedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关闭时间" :span="2">{{ detailOrder?.closedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维修结果" :span="2">{{ detailOrder?.repairResult || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTree } from '../api/tree'
import { getBenchesByNode } from '../api/bench'
import {
  createInspection,
  getInspections,
  getRepairOrders,
  startRepairOrder,
  completeRepairOrder,
  closeRepairOrder
} from '../api/inspection'
import {
  PROBLEM_TYPES,
  severityLabel,
  severityTagType,
  orderStatusLabel,
  orderStatusTagType,
  inspectionResultLabel,
  inspectionResultTagType
} from '../constants/inspection'

const activeTab = ref('inspection')

// 树形数据
const districts = ref([])
const sectionMap = ref({})
const pointMap = ref({})

// 巡检筛选
const inspFilterDistrict = ref(null)
const inspFilterSection = ref(null)
const inspFilterPoint = ref(null)
const inspFilterResult = ref(null)
const inspFilterSeverity = ref(null)
const inspKeyword = ref('')
const inspSections = ref([])
const inspPoints = ref([])
const inspections = ref([])

// 工单筛选
const orderFilterStatus = ref(null)
const orderFilterSeverity = ref(null)
const orderKeyword = ref('')
const orders = ref([])
const highlightOrderId = ref(null)

// 发起巡检
const createDialogVisible = ref(false)
const submitting = ref(false)
const scopeResolved = ref(false)
const createForm = ref({
  districtId: null,
  sectionId: null,
  nodeId: null,
  inspector: ''
})
const createSections = ref([])
const createPoints = ref([])
const scopeBenches = ref([])
const inspectionItems = ref([])
const defaultInspectedAt = ref('')
const scopeLoading = ref(false)
// 范围加载序号：快速切换范围时丢弃过期响应，避免旧范围数据覆盖新范围
let scopeLoadSeq = 0

// 完成维修
const completeDialogVisible = ref(false)
const currentOrder = ref(null)
const completeForm = ref({ repairResult: '', completedAt: '' })

// 工单详情
const detailDialogVisible = ref(false)
const detailOrder = ref(null)

const currentScopeNodeId = computed(() =>
  createForm.value.nodeId || createForm.value.sectionId || createForm.value.districtId
)

// 统一检查时间变化时批量应用到全部巡检项
watch(defaultInspectedAt, (val) => applyDefaultTime(val))

const loadTreeData = async () => {
  const tree = await getTree()
  districts.value = tree
  const sm = {}
  const pm = {}
  tree.forEach(d => {
    d.children?.forEach(s => {
      sm[d.id] = sm[d.id] || []
      sm[d.id].push(s)
      s.children?.forEach(p => {
        pm[s.id] = pm[s.id] || []
        pm[s.id].push(p)
      })
    })
  })
  sectionMap.value = sm
  pointMap.value = pm
}

// 发起巡检：街区/路段/点位联动
const onCreateDistrictChange = () => {
  createForm.value.sectionId = null
  createForm.value.nodeId = null
  createSections.value = sectionMap.value[createForm.value.districtId] || []
  createPoints.value = []
  loadScopeBenches()
}
const onCreateSectionChange = () => {
  createForm.value.nodeId = null
  createPoints.value = pointMap.value[createForm.value.sectionId] || []
  loadScopeBenches()
}
const onPointChange = () => {
  loadScopeBenches()
}
const loadScopeBenches = async () => {
  const nodeId = currentScopeNodeId.value
  const seq = ++scopeLoadSeq
  if (!nodeId) {
    scopeBenches.value = []
    inspectionItems.value = []
    scopeResolved.value = false
    return
  }
  scopeLoading.value = true
  try {
    const list = await getBenchesByNode(nodeId)
    if (seq !== scopeLoadSeq) return
    scopeBenches.value = list
    inspectionItems.value = list.map(b => ({
      benchId: b.id,
      code: b.code,
      location: b.nodeName,
      inspectedAt: defaultInspectedAt.value || formatNow(),
      result: 1,
      problemType: null,
      severity: null,
      description: '',
      suggestion: '',
      createRepairOrder: false
    }))
    scopeResolved.value = true
  } catch (e) {
    if (seq !== scopeLoadSeq) return
    // 取数失败时清空旧范围数据，避免误提交到其他范围
    scopeBenches.value = []
    inspectionItems.value = []
    scopeResolved.value = false
    ElMessage.error(e.message || '加载范围内长凳失败，请重新选择范围')
  } finally {
    if (seq === scopeLoadSeq) scopeLoading.value = false
  }
}

// 统一检查时间应用到所有记录
const applyDefaultTime = (val) => {
  if (val) {
    inspectionItems.value.forEach(item => { item.inspectedAt = val })
  }
}

const formatNow = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const openCreateDialog = () => {
  resetCreateForm()
  createDialogVisible.value = true
}

const resetCreateForm = () => {
  scopeLoadSeq++
  scopeLoading.value = false
  createForm.value = { districtId: null, sectionId: null, nodeId: null, inspector: '' }
  createSections.value = []
  createPoints.value = []
  scopeBenches.value = []
  inspectionItems.value = []
  defaultInspectedAt.value = ''
  scopeResolved.value = false
}

const handleSubmitInspection = async () => {
  const scopeNodeId = currentScopeNodeId.value
  if (!scopeNodeId) {
    ElMessage.warning('请选择巡检范围（街区/路段/点位）')
    return
  }
  if (!inspectionItems.value.length) {
    ElMessage.warning('所选范围内没有长凳，无法提交巡检')
    return
  }
  const invalid = inspectionItems.value.find(item => {
    if (!item.inspectedAt) return true
    if (item.result === 0) {
      return !item.problemType || !item.severity || !item.description?.trim()
    }
    return false
  })
  if (invalid) {
    ElMessage.warning(
      `长凳【${invalid.code}】信息不完整：检查时间必填，异常巡检还必须选择问题类型、严重程度并填写问题描述`
    )
    return
  }

  const payload = {
    scopeNodeId,
    inspector: createForm.value.inspector,
    items: inspectionItems.value.map(item => ({
      benchId: item.benchId,
      inspectedAt: item.inspectedAt,
      result: item.result,
      problemType: item.result === 0 ? item.problemType : null,
      severity: item.result === 0 ? item.severity : null,
      description: item.result === 0 ? item.description : null,
      suggestion: item.suggestion || null,
      createRepairOrder: item.result === 0 ? item.createRepairOrder : false
    }))
  }

  try {
    submitting.value = true
    const created = await createInspection(payload)
    const orderCount = created.filter(i => i.repairOrderId).length
    ElMessage.success(
      orderCount > 0 ? `巡检提交成功，并生成${orderCount}张维修工单` : '巡检提交成功'
    )
    createDialogVisible.value = false
    await reloadAll()
  } catch (e) {
    ElMessage.error(e.message || '巡检提交失败')
  } finally {
    submitting.value = false
  }
}

// 巡检筛选联动
const onInspDistrictChange = () => {
  inspFilterSection.value = null
  inspFilterPoint.value = null
  inspSections.value = sectionMap.value[inspFilterDistrict.value] || []
  inspPoints.value = []
  loadInspections()
}
const onInspSectionChange = () => {
  inspFilterPoint.value = null
  inspPoints.value = pointMap.value[inspFilterSection.value] || []
  loadInspections()
}

const loadInspections = async () => {
  try {
    const params = {
      nodeId: inspFilterPoint.value || inspFilterSection.value || inspFilterDistrict.value || null,
      result: inspFilterResult.value,
      severity: inspFilterSeverity.value,
      keyword: inspKeyword.value || null
    }
    inspections.value = await getInspections(params)
  } catch (e) {
    ElMessage.error(e.message || '加载巡检记录失败')
  }
}

const resetInspFilter = () => {
  inspFilterDistrict.value = null
  inspFilterSection.value = null
  inspFilterPoint.value = null
  inspFilterResult.value = null
  inspFilterSeverity.value = null
  inspKeyword.value = ''
  inspSections.value = []
  inspPoints.value = []
  loadInspections()
}

// 工单
const loadOrders = async () => {
  try {
    const params = {
      status: orderFilterStatus.value,
      severity: orderFilterSeverity.value,
      keyword: orderKeyword.value || null
    }
    const list = await getRepairOrders(params)
    orders.value = highlightOrderId.value
      ? list.map(o => ({ ...o, _highlight: o.id === highlightOrderId.value }))
      : list
    highlightOrderId.value = null
  } catch (e) {
    ElMessage.error(e.message || '加载维修工单失败')
  }
}

const resetOrderFilter = () => {
  orderFilterStatus.value = null
  orderFilterSeverity.value = null
  orderKeyword.value = ''
  loadOrders()
}

const handleStart = async (row) => {
  try {
    await ElMessageBox.confirm(`确认开始维修工单【${row.code}】吗？`, '开始维修', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'info'
    })
    await startRepairOrder(row.id)
    ElMessage.success('已开始维修')
    loadOrders()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const openCompleteDialog = (row) => {
  currentOrder.value = row
  completeForm.value = { repairResult: '', completedAt: formatNow() }
  completeDialogVisible.value = true
}

const handleCompleteSubmit = async () => {
  if (!completeForm.value.repairResult.trim()) {
    ElMessage.warning('请填写维修结果')
    return
  }
  if (!completeForm.value.completedAt) {
    ElMessage.warning('请选择维修完成时间')
    return
  }
  try {
    submitting.value = true
    await completeRepairOrder(currentOrder.value.id, {
      repairResult: completeForm.value.repairResult,
      completedAt: completeForm.value.completedAt
    })
    ElMessage.success('工单已完成')
    completeDialogVisible.value = false
    loadOrders()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleClose = async (row) => {
  try {
    await ElMessageBox.confirm(`确认关闭工单【${row.code}】吗？关闭后不可再流转。`, '关闭工单', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await closeRepairOrder(row.id)
    ElMessage.success('工单已关闭')
    loadOrders()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const openOrderDetail = (row) => {
  detailOrder.value = orders.value.find(o => o.id === row.id) || row
  detailDialogVisible.value = true
}

const goToOrder = (orderId) => {
  activeTab.value = 'order'
  orderFilterStatus.value = null
  highlightOrderId.value = orderId
  loadOrders()
}

const orderRowClass = ({ row }) => (row._highlight ? 'highlight-row' : '')

const handleTabChange = (name) => {
  if (name === 'order') loadOrders()
  if (name === 'inspection') loadInspections()
}

const reloadAll = async () => {
  await Promise.all([loadTreeData(), loadInspections(), loadOrders()])
}

onMounted(async () => {
  await loadTreeData()
  await Promise.all([loadInspections(), loadOrders()])
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
  width: 160px;
}

.filter-item.wide {
  width: 220px;
}

.data-table {
  margin-top: 6px;
}

.scope-benches {
  margin-top: 8px;
}

.scope-tip {
  margin-bottom: 8px;
  color: #606266;
  font-size: 13px;
}

.complete-form {
  margin-top: 16px;
}

.complete-desc {
  margin-bottom: 4px;
}

:deep(.el-table .highlight-row) {
  background-color: #fdf6ec;
}
</style>
