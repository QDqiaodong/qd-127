<template>
  <div class="plan-management">
    <el-card>
      <template #header>
        <div class="header">
          <h3>巡检计划与任务派发</h3>
          <div>
            <el-button type="primary" @click="openPlanDialog()">
              <el-icon><Plus /></el-icon>
              新建计划
            </el-button>
            <el-button @click="reloadAll">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- ============ 巡检计划 ============ -->
        <el-tab-pane label="巡检计划" name="plan">
          <el-table :data="plans" border class="data-table">
            <el-table-column prop="name" label="计划名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="scopePath" label="巡检范围" min-width="200" show-overflow-tooltip />
            <el-table-column label="周期" width="80">
              <template #default="{ row }">{{ cycleTypeLabel(row.cycleType) }}</template>
            </el-table-column>
            <el-table-column prop="planDate" label="首次计划日期" width="110" />
            <el-table-column prop="inspector" label="检查人" width="90" />
            <el-table-column label="启用状态" width="90">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.enabled"
                  :loading="row._toggling"
                  @change="handleToggle(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="任务统计" width="200">
              <template #default="{ row }">
                <span class="stat executed">已执行 {{ row.executedCount }}</span>
                <span class="stat pending">待执行 {{ row.pendingCount }}</span>
                <span class="stat overdue">逾期 {{ row.overdueCount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button size="small" link type="primary" @click="openDetailDialog(row)">详情</el-button>
                <el-button size="small" link type="primary" @click="openPlanDialog(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ============ 巡检任务 ============ -->
        <el-tab-pane label="巡检任务" name="task">
          <div class="filter-section">
            <el-select v-model="taskFilterPlanId" placeholder="所属计划" class="filter-item" clearable @change="loadTasks">
              <el-option v-for="p in plans" :key="p.id" :value="p.id" :label="p.name" />
            </el-select>
            <el-select v-model="taskFilterStatus" placeholder="任务状态" class="filter-item" clearable @change="loadTasks">
              <el-option label="待执行" :value="1" />
              <el-option label="已执行" :value="2" />
              <el-option label="逾期" :value="3" />
            </el-select>
            <el-button type="primary" @click="loadTasks">查询</el-button>
            <el-button @click="resetTaskFilter">重置</el-button>
          </div>

          <el-table :data="tasks" border class="data-table">
            <el-table-column prop="planName" label="所属计划" min-width="130" show-overflow-tooltip />
            <el-table-column prop="scopePath" label="巡检范围" min-width="200" show-overflow-tooltip />
            <el-table-column prop="planDate" label="计划日期" width="110" />
            <el-table-column prop="inspector" label="检查人" width="90" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="taskStatusTagType(row)" size="small">{{ taskStatusLabel(row) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="executedAt" label="执行时间" width="170">
              <template #default="{ row }">{{ row.executedAt || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  size="small"
                  type="primary"
                  :disabled="row.status !== 1"
                  @click="openExecuteDialog(row)"
                >
                  执行
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ============ 新建/编辑计划弹窗 ============ -->
    <el-dialog v-model="planDialogVisible" :title="editingPlan ? '编辑巡检计划' : '新建巡检计划'" width="640px" @closed="resetPlanForm">
      <el-form :model="planForm" label-width="100px">
        <el-form-item label="计划名称" required>
          <el-input v-model="planForm.name" placeholder="请输入计划名称" maxlength="100" />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="8">
            <el-form-item label="巡检街区" required>
              <el-select v-model="planForm.districtId" placeholder="请选择街区" @change="onPlanDistrictChange">
                <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="巡检路段">
              <el-select v-model="planForm.sectionId" placeholder="不选则巡检整个街区" :disabled="!planForm.districtId" clearable @change="onPlanSectionChange">
                <el-option v-for="s in planSections" :key="s.id" :value="s.id" :label="s.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="巡检点位">
              <el-select v-model="planForm.nodeId" placeholder="不选则巡检整条路段" :disabled="!planForm.sectionId" clearable>
                <el-option v-for="p in planPoints" :key="p.id" :value="p.id" :label="p.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="巡检周期" required>
              <el-select v-model="planForm.cycleType" placeholder="请选择周期" style="width: 100%">
                <el-option label="每天" :value="1" />
                <el-option label="每周" :value="2" />
                <el-option label="每月" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="首次计划日期" required>
              <el-date-picker
                v-model="planForm.planDate"
                type="date"
                placeholder="首次生成任务的日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="检查人" required>
              <el-input v-model="planForm.inspector" placeholder="请输入检查人" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用状态">
              <el-switch v-model="planForm.enabled" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="planForm.remark" type="textarea" :rows="2" placeholder="备注（选填）" maxlength="500" />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          title="启用后系统按周期自动生成待执行任务；停用后不再生成新任务。"
        />
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handlePlanSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- ============ 计划详情弹窗 ============ -->
    <el-dialog v-model="detailDialogVisible" title="计划详情" width="860px">
      <template v-if="planDetail">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="计划名称" :span="2">{{ planDetail.plan.name }}</el-descriptions-item>
          <el-descriptions-item label="启用状态">
            <el-tag :type="planDetail.plan.enabled ? 'success' : 'info'" size="small">
              {{ planDetail.plan.enabled ? '启用' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="巡检范围" :span="2">{{ planDetail.plan.scopePath }}</el-descriptions-item>
          <el-descriptions-item label="巡检周期">{{ cycleTypeLabel(planDetail.plan.cycleType) }}</el-descriptions-item>
          <el-descriptions-item label="首次计划日期">{{ planDetail.plan.planDate }}</el-descriptions-item>
          <el-descriptions-item label="检查人">{{ planDetail.plan.inspector }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ planDetail.plan.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">{{ planDetail.plan.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="stat-cards">
          <div class="stat-card executed">
            <div class="stat-num">{{ planDetail.plan.executedCount }}</div>
            <div class="stat-label">已执行</div>
          </div>
          <div class="stat-card pending">
            <div class="stat-num">{{ planDetail.plan.pendingCount }}</div>
            <div class="stat-label">待执行</div>
          </div>
          <div class="stat-card overdue">
            <div class="stat-num">{{ planDetail.plan.overdueCount }}</div>
            <div class="stat-label">逾期</div>
          </div>
        </div>

        <el-table :data="planDetail.tasks" border size="small" max-height="320" class="data-table">
          <el-table-column prop="planDate" label="计划日期" width="110" />
          <el-table-column prop="inspector" label="检查人" width="100" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="taskStatusTagType(row)" size="small">{{ taskStatusLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="executedAt" label="执行时间" width="170">
            <template #default="{ row }">{{ row.executedAt || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button
                size="small"
                type="primary"
                :disabled="row.status !== 1"
                @click="openExecuteDialog(row)"
              >
                执行
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>

    <!-- ============ 执行任务弹窗 ============ -->
    <el-dialog v-model="executeDialogVisible" title="执行巡检任务" width="900px" @closed="resetExecuteForm">
      <template v-if="currentTask">
        <el-descriptions :column="3" border size="small" class="task-desc">
          <el-descriptions-item label="所属计划">{{ currentTask.planName }}</el-descriptions-item>
          <el-descriptions-item label="计划日期">{{ currentTask.planDate }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="taskStatusTagType(currentTask)" size="small">{{ taskStatusLabel(currentTask) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="巡检范围" :span="3">{{ currentTask.scopePath }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-width="100px" class="execute-form" v-loading="benchesLoading" element-loading-text="正在加载范围内长凳...">
          <el-row :gutter="10">
            <el-col :span="12">
              <el-form-item label="检查人">
                <el-input v-model="executeForm.inspector" placeholder="默认为任务检查人" />
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

        <div class="scope-benches" v-if="inspectionItems.length">
          <div class="scope-tip">
            已自动带入该范围内 <b>{{ inspectionItems.length }}</b> 张长凳，请逐张记录检查情况。
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
        <el-empty v-else-if="benchesResolved" description="该任务范围内暂无长凳，无法执行" :image-size="60" />
      </template>

      <template #footer>
        <el-button @click="executeDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="!inspectionItems.length"
          @click="handleExecuteSubmit"
        >
          提交执行
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getTree } from '../api/tree'
import {
  getPlans,
  createPlan,
  updatePlan,
  togglePlan,
  getPlanDetail,
  getPlanTasks,
  getTaskBenches,
  executeTask
} from '../api/plan'
import { PROBLEM_TYPES } from '../constants/inspection'
import { cycleTypeLabel, taskStatusLabel, taskStatusTagType } from '../constants/plan'

const activeTab = ref('plan')

// 树形数据
const districts = ref([])
const sectionMap = ref({})
const pointMap = ref({})

// 计划
const plans = ref([])
const planDialogVisible = ref(false)
const editingPlan = ref(null)
const planForm = ref({
  name: '',
  districtId: null,
  sectionId: null,
  nodeId: null,
  cycleType: null,
  planDate: '',
  inspector: '',
  enabled: true,
  remark: ''
})
const planSections = ref([])
const planPoints = ref([])
const submitting = ref(false)

// 计划详情
const detailDialogVisible = ref(false)
const planDetail = ref(null)

// 任务
const tasks = ref([])
const taskFilterPlanId = ref(null)
const taskFilterStatus = ref(null)

// 执行任务
const executeDialogVisible = ref(false)
const currentTask = ref(null)
const executeForm = ref({ inspector: '' })
const inspectionItems = ref([])
const defaultInspectedAt = ref('')
const benchesLoading = ref(false)
const benchesResolved = ref(false)

// 统一检查时间变化时批量应用到全部巡检项
watch(defaultInspectedAt, (val) => {
  if (val) {
    inspectionItems.value.forEach(item => { item.inspectedAt = val })
  }
})

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

// ============ 计划 ============
const loadPlans = async () => {
  try {
    plans.value = await getPlans()
  } catch (e) {
    ElMessage.error(e.message || '加载巡检计划失败')
  }
}

const openPlanDialog = (row) => {
  resetPlanForm()
  if (row) {
    editingPlan.value = row
    locateScope(row.scopeNodeId)
    planForm.value = {
      name: row.name,
      districtId: planForm.value.districtId,
      sectionId: planForm.value.sectionId,
      nodeId: planForm.value.nodeId,
      cycleType: row.cycleType,
      planDate: row.planDate,
      inspector: row.inspector,
      enabled: row.enabled,
      remark: row.remark || ''
    }
  }
  planDialogVisible.value = true
}

// 根据范围节点ID回填街区/路段/点位联动选择
const locateScope = (scopeNodeId) => {
  for (const d of districts.value) {
    if (d.id === scopeNodeId) {
      planForm.value.districtId = d.id
      planSections.value = sectionMap.value[d.id] || []
      return
    }
    for (const s of d.children || []) {
      if (s.id === scopeNodeId) {
        planForm.value.districtId = d.id
        planForm.value.sectionId = s.id
        planSections.value = sectionMap.value[d.id] || []
        planPoints.value = pointMap.value[s.id] || []
        return
      }
      for (const p of s.children || []) {
        if (p.id === scopeNodeId) {
          planForm.value.districtId = d.id
          planForm.value.sectionId = s.id
          planForm.value.nodeId = p.id
          planSections.value = sectionMap.value[d.id] || []
          planPoints.value = pointMap.value[s.id] || []
          return
        }
      }
    }
  }
}

const onPlanDistrictChange = () => {
  planForm.value.sectionId = null
  planForm.value.nodeId = null
  planSections.value = sectionMap.value[planForm.value.districtId] || []
  planPoints.value = []
}
const onPlanSectionChange = () => {
  planForm.value.nodeId = null
  planPoints.value = pointMap.value[planForm.value.sectionId] || []
}

const resetPlanForm = () => {
  editingPlan.value = null
  planForm.value = {
    name: '',
    districtId: null,
    sectionId: null,
    nodeId: null,
    cycleType: null,
    planDate: '',
    inspector: '',
    enabled: true,
    remark: ''
  }
  planSections.value = []
  planPoints.value = []
}

const handlePlanSubmit = async () => {
  const scopeNodeId = planForm.value.nodeId || planForm.value.sectionId || planForm.value.districtId
  if (!planForm.value.name.trim()) {
    ElMessage.warning('请输入计划名称')
    return
  }
  if (!scopeNodeId) {
    ElMessage.warning('请选择巡检范围（街区/路段/点位）')
    return
  }
  if (!planForm.value.cycleType) {
    ElMessage.warning('请选择巡检周期')
    return
  }
  if (!planForm.value.planDate) {
    ElMessage.warning('请选择首次计划日期')
    return
  }
  if (!planForm.value.inspector.trim()) {
    ElMessage.warning('请输入检查人')
    return
  }

  const payload = {
    name: planForm.value.name.trim(),
    scopeNodeId,
    cycleType: planForm.value.cycleType,
    planDate: planForm.value.planDate,
    inspector: planForm.value.inspector.trim(),
    enabled: planForm.value.enabled,
    remark: planForm.value.remark || null
  }

  try {
    submitting.value = true
    if (editingPlan.value) {
      await updatePlan(editingPlan.value.id, payload)
      ElMessage.success('巡检计划已更新')
    } else {
      await createPlan(payload)
      ElMessage.success('巡检计划创建成功')
    }
    planDialogVisible.value = false
    await Promise.all([loadPlans(), loadTasks()])
  } catch (e) {
    ElMessage.error(e.message || '保存巡检计划失败')
  } finally {
    submitting.value = false
  }
}

const handleToggle = async (row) => {
  row._toggling = true
  try {
    await togglePlan(row.id)
    ElMessage.success(row.enabled ? '计划已停用，将不再生成新任务' : '计划已启用，将按周期生成待执行任务')
    await Promise.all([loadPlans(), loadTasks()])
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    row._toggling = false
  }
}

const openDetailDialog = async (row) => {
  try {
    planDetail.value = await getPlanDetail(row.id)
    detailDialogVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载计划详情失败')
  }
}

// ============ 任务 ============
const loadTasks = async () => {
  try {
    const params = {
      planId: taskFilterPlanId.value || null,
      status: taskFilterStatus.value || null
    }
    tasks.value = await getPlanTasks(params)
  } catch (e) {
    ElMessage.error(e.message || '加载巡检任务失败')
  }
}

const resetTaskFilter = () => {
  taskFilterPlanId.value = null
  taskFilterStatus.value = null
  loadTasks()
}

const openExecuteDialog = async (row) => {
  currentTask.value = row
  executeForm.value = { inspector: row.inspector || '' }
  inspectionItems.value = []
  defaultInspectedAt.value = ''
  benchesResolved.value = false
  executeDialogVisible.value = true

  benchesLoading.value = true
  try {
    const list = await getTaskBenches(row.id)
    inspectionItems.value = list.map(b => ({
      benchId: b.id,
      code: b.code,
      location: b.nodeName,
      inspectedAt: formatNow(),
      result: 1,
      problemType: null,
      severity: null,
      description: '',
      suggestion: '',
      createRepairOrder: false
    }))
    benchesResolved.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载范围内长凳失败')
  } finally {
    benchesLoading.value = false
  }
}

const resetExecuteForm = () => {
  currentTask.value = null
  executeForm.value = { inspector: '' }
  inspectionItems.value = []
  defaultInspectedAt.value = ''
  benchesLoading.value = false
  benchesResolved.value = false
}

const handleExecuteSubmit = async () => {
  if (!currentTask.value) return
  if (!inspectionItems.value.length) {
    ElMessage.warning('该任务范围内暂无长凳，无法执行')
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
    inspector: executeForm.value.inspector,
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
    const created = await executeTask(currentTask.value.id, payload)
    const orderCount = created.filter(i => i.repairOrderId).length
    ElMessage.success(
      orderCount > 0
        ? `任务执行成功，已关联${created.length}条巡检记录，并生成${orderCount}张维修工单`
        : `任务执行成功，已关联${created.length}条巡检记录`
    )
    executeDialogVisible.value = false
    await Promise.all([loadPlans(), loadTasks()])
  } catch (e) {
    ElMessage.error(e.message || '任务执行失败')
    // 重复执行等状态冲突时刷新任务列表，同步最新状态
    loadTasks()
  } finally {
    submitting.value = false
  }
}

const formatNow = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const handleTabChange = (name) => {
  if (name === 'task') loadTasks()
  if (name === 'plan') loadPlans()
}

const reloadAll = async () => {
  await Promise.all([loadPlans(), loadTasks()])
}

onMounted(async () => {
  await loadTreeData()
  await Promise.all([loadPlans(), loadTasks()])
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
  width: 180px;
}

.data-table {
  margin-top: 6px;
}

.stat {
  margin-right: 8px;
  font-size: 13px;
}

.stat.executed {
  color: #67c23a;
}

.stat.pending {
  color: #e6a23c;
}

.stat.overdue {
  color: #f56c6c;
}

.stat-cards {
  display: flex;
  gap: 16px;
  margin: 16px 0;
}

.stat-card {
  flex: 1;
  text-align: center;
  padding: 14px 0;
  border-radius: 6px;
  background: #f5f7fa;
}

.stat-card .stat-num {
  font-size: 24px;
  font-weight: 600;
}

.stat-card .stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.stat-card.executed .stat-num {
  color: #67c23a;
}

.stat-card.pending .stat-num {
  color: #e6a23c;
}

.stat-card.overdue .stat-num {
  color: #f56c6c;
}

.task-desc {
  margin-bottom: 12px;
}

.execute-form {
  margin-top: 4px;
}

.scope-benches {
  margin-top: 8px;
}

.scope-tip {
  margin-bottom: 8px;
  color: #606266;
  font-size: 13px;
}
</style>
