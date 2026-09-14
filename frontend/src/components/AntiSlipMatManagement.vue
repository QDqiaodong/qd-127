<template>
  <div class="anti-slip-mat-management">
    <el-card>
      <template #header>
        <div class="header">
          <h3>雨天防滑垫领用台账</h3>
          <div>
            <el-button type="primary" @click="openIssueDialog()">
              <el-icon><Top /></el-icon>
              领出登记
            </el-button>
            <el-button type="success" @click="openReturnDialog()">
              <el-icon><Back /></el-icon>
              归还登记
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
        title="雨天把防滑垫发到各点位：按点位登记领出数量、归还数量和破损数。未还 = 领出 - 归还（归还含完好与破损），未还大于0的点位会在台账和街区树标红，可按“只看未还清”筛出。"
        class="rule-alert"
      />

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- ============ 点位领用台账 ============ -->
        <el-tab-pane label="点位领用台账" name="ledger">
          <div class="filter-section">
            <el-select v-model="filters.districtId" placeholder="选择街区" class="filter-item" clearable @change="onDistrictFilterChange">
              <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
            </el-select>
            <el-select v-model="filters.sectionId" placeholder="选择路段" class="filter-item" clearable :disabled="!filters.districtId" @change="onSectionFilterChange">
              <el-option v-for="s in filterSections" :key="s.id" :value="s.id" :label="s.name" />
            </el-select>
            <el-select v-model="filters.pointId" placeholder="选择点位" class="filter-item" clearable :disabled="!filters.sectionId">
              <el-option v-for="p in filterPoints" :key="p.id" :value="p.id" :label="p.name" />
            </el-select>
            <el-checkbox v-model="filters.outstanding" border class="filter-check">只看未还清</el-checkbox>
            <el-button type="primary" @click="loadLedgers">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>

          <div v-if="ledgers.length > 0" class="result-summary">
            共 <b>{{ ledgers.length }}</b> 个点位，其中未还清 <b class="outstanding-text">{{ outstandingCount }}</b> 个，
            未还防滑垫合计 <b class="outstanding-text">{{ outstandingMatSum }}</b> 张
          </div>

          <el-table
            :data="ledgers"
            v-loading="loading"
            border
            class="data-table"
            :row-class-name="ledgerRowClass"
            @row-click="openDetail"
          >
            <el-table-column prop="districtName" label="街区" width="140" show-overflow-tooltip />
            <el-table-column prop="sectionName" label="路段" width="150" show-overflow-tooltip />
            <el-table-column prop="pointName" label="点位" min-width="170" show-overflow-tooltip />
            <el-table-column prop="issuedCount" label="领出" width="80" align="center" />
            <el-table-column label="归还（完好）" width="110" align="center">
              <template #default="{ row }">{{ row.returnedCount }}（{{ row.intactCount }}）</template>
            </el-table-column>
            <el-table-column label="破损" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.damagedCount > 0" size="small" type="warning" effect="plain">{{ row.damagedCount }}</el-tag>
                <span v-else>0</span>
              </template>
            </el-table-column>
            <el-table-column label="未还" width="80" align="center">
              <template #default="{ row }">
                <span :class="{ 'outstanding-text': row.outstandingCount > 0 }" style="font-weight: 600">
                  {{ row.outstandingCount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.outstanding" size="small" type="danger">未还清</el-tag>
                <el-tag v-else-if="row.hasRecord" size="small" type="success" effect="plain">已还清</el-tag>
                <el-tag v-else size="small" type="info" effect="plain">未领用</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最近操作" width="160">
              <template #default="{ row }">
                <span v-if="row.lastOperatedAt">{{ row.lastOperatedAt }}</span>
                <span v-else class="muted-text">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click.stop="openDetail(row)">详情</el-button>
                <el-button
                  link
                  type="success"
                  size="small"
                  :disabled="!row.outstanding"
                  @click.stop="openReturnDialog(row)"
                >
                  归还
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty :description="emptyDescription" :image-size="70" />
            </template>
          </el-table>
        </el-tab-pane>

        <!-- ============ 领用/归还流水 ============ -->
        <el-tab-pane label="领用/归还流水" name="records">
          <div class="filter-section">
            <el-select v-model="recFilter.districtId" placeholder="选择街区" class="filter-item" clearable @change="onRecDistrictChange">
              <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
            </el-select>
            <el-select v-model="recFilter.sectionId" placeholder="选择路段" class="filter-item" clearable :disabled="!recFilter.districtId" @change="onRecSectionChange">
              <el-option v-for="s in recSections" :key="s.id" :value="s.id" :label="s.name" />
            </el-select>
            <el-select v-model="recFilter.pointId" placeholder="选择点位" class="filter-item" clearable :disabled="!recFilter.sectionId" @change="loadRecords">
              <el-option v-for="p in recPoints" :key="p.id" :value="p.id" :label="p.name" />
            </el-select>
            <el-select v-model="recFilter.actionType" placeholder="动作类型" class="filter-item" clearable @change="loadRecords">
              <el-option label="领出" :value="1" />
              <el-option label="归还" :value="2" />
            </el-select>
            <el-input v-model="recKeyword" placeholder="搜索点位/经办人/备注" class="filter-item wide" @keyup.enter="loadRecords" />
            <el-button type="primary" @click="loadRecords">查询</el-button>
            <el-button @click="resetRecFilter">重置</el-button>
          </div>

          <el-table :data="filteredRecords" v-loading="recordLoading" border class="data-table">
            <el-table-column prop="operatedAt" label="操作时间" width="170" />
            <el-table-column prop="districtName" label="街区" width="130" show-overflow-tooltip />
            <el-table-column prop="sectionName" label="路段" width="140" show-overflow-tooltip />
            <el-table-column prop="pointName" label="点位" min-width="150" show-overflow-tooltip />
            <el-table-column label="动作" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="antiSlipMatActionTagType(row.actionType)">
                  {{ row.actionTypeLabel || antiSlipMatActionLabel(row.actionType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="本次数量" width="90" align="center" />
            <el-table-column label="其中完好" width="90" align="center">
              <template #default="{ row }">{{ row.intactQuantity ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="其中破损" width="90" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.actionType === 2 && row.damagedQuantity > 0" size="small" type="warning" effect="plain">
                  {{ row.damagedQuantity }}
                </el-tag>
                <span v-else>{{ row.actionType === 2 ? 0 : '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="operator" label="经办人" width="100" />
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无防滑垫领用/归还流水" :image-size="70" />
            </template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ============ 领出登记弹窗 ============ -->
    <el-dialog v-model="issueDialogVisible" title="防滑垫领出登记" width="560px" @closed="resetIssueForm">
      <el-form :model="issueForm" label-width="100px">
        <el-form-item label="街区" required>
          <el-select v-model="issueForm.districtId" placeholder="请选择街区" style="width: 100%" @change="onIssueDistrictChange">
            <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="路段" required>
          <el-select v-model="issueForm.sectionId" placeholder="请选择路段" style="width: 100%" :disabled="!issueForm.districtId" @change="onIssueSectionChange">
            <el-option v-for="s in issueSections" :key="s.id" :value="s.id" :label="s.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="点位" required>
          <el-select v-model="issueForm.pointId" placeholder="请选择点位（必选）" style="width: 100%" :disabled="!issueForm.sectionId">
            <el-option
              v-for="p in issuePoints"
              :key="p.id"
              :value="p.id"
              :label="p.closed ? `${p.name}（封闭中）` : p.name"
              :disabled="!!p.closed"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="领出数量" required>
          <el-input-number v-model="issueForm.quantity" :min="1" :max="9999" />
          <span class="form-tip">雨天发到该点位的防滑垫数量</span>
        </el-form-item>
        <el-form-item label="领出时间" required>
          <el-date-picker
            v-model="issueForm.operatedAt"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择领出时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="经办人" required>
          <el-input v-model="issueForm.operator" placeholder="请输入经办人" maxlength="50" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="issueForm.remark" type="textarea" :rows="2" placeholder="选填" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleIssueSubmit">提交领出</el-button>
      </template>
    </el-dialog>

    <!-- ============ 归还登记弹窗 ============ -->
    <el-dialog v-model="returnDialogVisible" title="防滑垫归还登记" width="560px" @closed="resetReturnForm">
      <el-alert
        v-if="returnTargetOutstanding > 0"
        type="warning"
        :closable="false"
        show-icon
        class="return-alert"
        :title="`该点位当前未还 ${returnTargetOutstanding} 张，本次归还不能超过未还数量`"
      />
      <el-form :model="returnForm" label-width="100px">
        <el-form-item label="街区" required>
          <el-select v-model="returnForm.districtId" placeholder="请选择街区" style="width: 100%" @change="onReturnDistrictChange">
            <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="路段" required>
          <el-select v-model="returnForm.sectionId" placeholder="请选择路段" style="width: 100%" :disabled="!returnForm.districtId" @change="onReturnSectionChange">
            <el-option v-for="s in returnSections" :key="s.id" :value="s.id" :label="s.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="点位" required>
          <el-select v-model="returnForm.pointId" placeholder="请选择点位（必选）" style="width: 100%" :disabled="!returnForm.sectionId" @change="onReturnPointChange">
            <el-option
              v-for="p in returnPoints"
              :key="p.id"
              :value="p.id"
              :label="p.closed ? `${p.name}（封闭中）` : p.name"
              :disabled="!!p.closed"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="归还数量" required>
          <el-input-number v-model="returnForm.quantity" :min="1" :max="9999" />
          <span class="form-tip">本次实际收回总数（完好 + 破损）</span>
        </el-form-item>
        <el-form-item label="其中破损" required>
          <el-input-number v-model="returnForm.damagedQuantity" :min="0" :max="returnForm.quantity || 0" />
          <span class="form-tip">完好归还 {{ returnIntact }} 张，破损 {{ returnForm.damagedQuantity || 0 }} 张</span>
        </el-form-item>
        <el-form-item label="归还时间" required>
          <el-date-picker
            v-model="returnForm.operatedAt"
            type="datetime"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择归还时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="经办人" required>
          <el-input v-model="returnForm.operator" placeholder="请输入经办人" maxlength="50" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="returnForm.remark" type="textarea" :rows="2" placeholder="可填写破损情况说明" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="handleReturnSubmit">提交归还</el-button>
      </template>
    </el-dialog>

    <!-- ============ 点位领用详情弹窗 ============ -->
    <el-dialog v-model="detailDialogVisible" title="点位防滑垫领用详情" width="760px">
      <el-descriptions v-if="detail" :column="3" border size="small" class="detail-desc">
        <el-descriptions-item label="街区">{{ detail.districtName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="路段">{{ detail.sectionName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="点位">{{ detail.pointName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="领出合计">
          <b>{{ detail.issuedCount }}</b> 张
        </el-descriptions-item>
        <el-descriptions-item label="归还合计">
          {{ detail.returnedCount }} 张（完好 {{ detail.intactCount }}）
        </el-descriptions-item>
        <el-descriptions-item label="其中破损">
          <el-tag v-if="detail.damagedCount > 0" size="small" type="warning" effect="plain">{{ detail.damagedCount }} 张</el-tag>
          <span v-else>0 张</span>
        </el-descriptions-item>
        <el-descriptions-item label="未还数量" :span="3">
          <el-tag :type="detail.outstanding ? 'danger' : 'success'" size="small">
            {{ detail.outstandingCount }} 张{{ detail.outstanding ? '（未还清）' : '（已还清）' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="detail && detail.outstanding" class="detail-action">
        <el-button type="success" size="small" @click="openReturnFromDetail">去登记归还</el-button>
      </div>

      <el-table v-loading="detailLoading" :data="detailRecords" border size="small" max-height="320" class="detail-table">
        <el-table-column prop="operatedAt" label="操作时间" width="170" />
        <el-table-column label="动作" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="antiSlipMatActionTagType(row.actionType)">
              {{ row.actionTypeLabel || antiSlipMatActionLabel(row.actionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="本次数量" width="80" align="center" />
        <el-table-column label="其中完好" width="80" align="center">
          <template #default="{ row }">{{ row.intactQuantity ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="其中破损" width="80" align="center">
          <template #default="{ row }">{{ row.actionType === 2 ? row.damagedQuantity : '-' }}</template>
        </el-table-column>
        <el-table-column prop="operator" label="经办人" width="90" />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="该点位暂无领用/归还流水" :image-size="60" />
        </template>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Top, Back, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getTree } from '../api/tree'
import {
  getAntiSlipMatLedgers,
  issueAntiSlipMat,
  returnAntiSlipMat,
  getAntiSlipMatRecords,
  getAntiSlipMatRecordsByPoint
} from '../api/antiSlipMat'
import { antiSlipMatActionLabel, antiSlipMatActionTagType } from '../constants/antiSlipMat'

const activeTab = ref('ledger')

// 树形数据
const districts = ref([])
const sectionMap = ref({})
const pointMap = ref({})
const pointParentMap = ref({})
const pointClosedMap = ref({})

const loadTreeData = async () => {
  const tree = await getTree()
  districts.value = tree
  const sm = {}
  const pm = {}
  const parent = {}
  const closed = {}
  tree.forEach(d => {
    d.children?.forEach(s => {
      sm[d.id] = sm[d.id] || []
      sm[d.id].push(s)
      s.children?.forEach(p => {
        pm[s.id] = pm[s.id] || []
        pm[s.id].push(p)
        parent[p.id] = { sectionId: s.id, districtId: d.id }
        closed[p.id] = !!p.closed
      })
    })
  })
  sectionMap.value = sm
  pointMap.value = pm
  pointParentMap.value = parent
  pointClosedMap.value = closed
  // 恢复的上次筛选可能因节点删除而失效，清掉悬空项避免查出空结果却说不清原因
  return pruneDanglingFilters()
}

// ============ 点位领用台账 ============
const ledgers = ref([])
const loading = ref(false)
const filters = ref({ districtId: null, sectionId: null, pointId: null, outstanding: false })

// 筛选项持久化：关掉页面再打开时恢复上次筛选（含“只看未还清”），台账与街区树标记仍对得上
const FILTER_STORAGE_KEY = 'anti-slip-mat-filters'

const restoreFilters = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(FILTER_STORAGE_KEY))
    if (saved && typeof saved === 'object') {
      filters.value = {
        districtId: saved.districtId ?? null,
        sectionId: saved.sectionId ?? null,
        pointId: saved.pointId ?? null,
        outstanding: !!saved.outstanding
      }
    }
  } catch (error) {
    localStorage.removeItem(FILTER_STORAGE_KEY)
  }
}

const persistFilters = () => {
  localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(filters.value))
}

const filterSections = computed(() => sectionMap.value[filters.value.districtId] || [])
const filterPoints = computed(() => pointMap.value[filters.value.sectionId] || [])

const onDistrictFilterChange = () => {
  filters.value.sectionId = null
  filters.value.pointId = null
}
const onSectionFilterChange = () => {
  filters.value.pointId = null
}

const outstandingCount = computed(() => ledgers.value.filter(r => r.outstanding).length)
const outstandingMatSum = computed(() =>
  ledgers.value.reduce((sum, r) => sum + (r.outstanding ? r.outstandingCount : 0), 0))

const ledgerRowClass = ({ row }) => (row.outstanding ? 'outstanding-row' : '')

const emptyDescription = computed(() => {
  if (filters.value.outstanding) return '当前筛选范围内没有未还清的点位'
  if (filters.value.pointId || filters.value.sectionId || filters.value.districtId) {
    return '所选街区/路段/点位暂无防滑垫领用记录'
  }
  return '暂无防滑垫领用记录，可点击右上角“领出登记”创建'
})

// 返回是否清掉了失效的恢复筛选：清掉后需用纠正后的条件重查，保持筛选框与台账一致
const pruneDanglingFilters = () => {
  if (filters.value.districtId && (sectionMap.value[filters.value.districtId] || []).length === 0) {
    filters.value.districtId = null
    filters.value.sectionId = null
    filters.value.pointId = null
    return true
  }
  if (filters.value.sectionId && (pointMap.value[filters.value.sectionId] || []).length === 0) {
    filters.value.sectionId = null
    filters.value.pointId = null
    return true
  }
  const points = pointMap.value[filters.value.sectionId] || []
  if (filters.value.pointId && !points.some(p => p.id === filters.value.pointId)) {
    filters.value.pointId = null
    return true
  }
  return false
}

const loadLedgers = async () => {
  ledgers.value = await getAntiSlipMatLedgers({
    outstanding: filters.value.outstanding ? true : undefined,
    districtId: filters.value.districtId || undefined,
    sectionId: filters.value.sectionId || undefined,
    pointId: filters.value.pointId || undefined
  })
  persistFilters()
}

const resetFilters = () => {
  filters.value = { districtId: null, sectionId: null, pointId: null, outstanding: false }
  loadLedgers()
}

// ============ 领用/归还流水 ============
const allRecords = ref([])
const recordLoading = ref(false)
const recFilter = ref({ districtId: null, sectionId: null, pointId: null, actionType: null })
const recKeyword = ref('')
const recSections = ref([])
const recPoints = ref([])

const loadRecords = async () => {
  recordLoading.value = true
  try {
    allRecords.value = await getAntiSlipMatRecords({
      pointId: recFilter.value.pointId || undefined,
      actionType: recFilter.value.actionType || undefined,
      keyword: recKeyword.value || undefined
    })
  } catch (error) {
    ElMessage.error(error.message || '加载防滑垫领用/归还流水失败')
  } finally {
    recordLoading.value = false
  }
}

// 街区/路段范围在前端按点位归属过滤（点位/动作/关键字已由后端过滤）
const filteredRecords = computed(() => allRecords.value.filter(row => {
  const parent = pointParentMap.value[row.pointId] || {}
  if (recFilter.value.districtId && parent.districtId !== recFilter.value.districtId) return false
  if (recFilter.value.sectionId && parent.sectionId !== recFilter.value.sectionId) return false
  return true
}))

const onRecDistrictChange = () => {
  recFilter.value.sectionId = null
  recFilter.value.pointId = null
  recSections.value = sectionMap.value[recFilter.value.districtId] || []
  recPoints.value = []
  loadRecords()
}
const onRecSectionChange = () => {
  recFilter.value.pointId = null
  recPoints.value = pointMap.value[recFilter.value.sectionId] || []
  loadRecords()
}
const resetRecFilter = () => {
  recFilter.value = { districtId: null, sectionId: null, pointId: null, actionType: null }
  recKeyword.value = ''
  recSections.value = []
  recPoints.value = []
  loadRecords()
}

// ============ 通用时间 ============
const formatNow = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// ============ 领出登记 ============
const issueDialogVisible = ref(false)
const submitting = ref(false)
const issueForm = ref({ districtId: null, sectionId: null, pointId: null, quantity: 1, operatedAt: '', operator: '', remark: '' })
const issueSections = computed(() => sectionMap.value[issueForm.value.districtId] || [])
const issuePoints = computed(() => pointMap.value[issueForm.value.sectionId] || [])

const resetIssueForm = () => {
  issueForm.value = { districtId: null, sectionId: null, pointId: null, quantity: 1, operatedAt: formatNow(), operator: '', remark: '' }
}
const onIssueDistrictChange = () => {
  issueForm.value.sectionId = null
  issueForm.value.pointId = null
}
const onIssueSectionChange = () => {
  issueForm.value.pointId = null
}

const openIssueDialog = () => {
  resetIssueForm()
  issueDialogVisible.value = true
}

const handleIssueSubmit = async () => {
  const form = issueForm.value
  if (!form.pointId) {
    ElMessage.warning('请先选择街区、路段和点位，没有点位不能提交')
    return
  }
  if (pointClosedMap.value[form.pointId]) {
    ElMessage.warning('该点位处于临时封闭期，封闭期内不办理防滑垫领用')
    return
  }
  if (!form.quantity || form.quantity <= 0) {
    ElMessage.warning('领出数量必须大于0')
    return
  }
  if (!form.operatedAt) {
    ElMessage.warning('请选择领出时间')
    return
  }
  if (!form.operator.trim()) {
    ElMessage.warning('请填写经办人')
    return
  }
  try {
    submitting.value = true
    await issueAntiSlipMat({
      pointId: form.pointId,
      quantity: form.quantity,
      operatedAt: form.operatedAt,
      operator: form.operator.trim(),
      remark: form.remark || null
    })
    ElMessage.success('防滑垫领出登记成功')
    issueDialogVisible.value = false
    await reloadAll()
  } catch (error) {
    ElMessage.error(error.message || '领出登记失败')
  } finally {
    submitting.value = false
  }
}

// ============ 归还登记 ============
const returnDialogVisible = ref(false)
const returnForm = ref({ districtId: null, sectionId: null, pointId: null, quantity: 1, damagedQuantity: 0, operatedAt: '', operator: '', remark: '' })
const returnSections = computed(() => sectionMap.value[returnForm.value.districtId] || [])
const returnPoints = computed(() => pointMap.value[returnForm.value.sectionId] || [])
const returnTargetOutstanding = ref(0)

const returnIntact = computed(() => Math.max(0, (returnForm.value.quantity || 0) - (returnForm.value.damagedQuantity || 0)))

const resetReturnForm = () => {
  returnForm.value = { districtId: null, sectionId: null, pointId: null, quantity: 1, damagedQuantity: 0, operatedAt: formatNow(), operator: '', remark: '' }
  returnTargetOutstanding.value = 0
}
const onReturnDistrictChange = () => {
  returnForm.value.sectionId = null
  returnForm.value.pointId = null
  returnTargetOutstanding.value = 0
}
const onReturnSectionChange = () => {
  returnForm.value.pointId = null
  returnTargetOutstanding.value = 0
}

const prefillPoint = (pointId) => {
  const parent = pointParentMap.value[pointId]
  if (!parent) return
  returnForm.value.districtId = parent.districtId
  returnForm.value.sectionId = parent.sectionId
  returnForm.value.pointId = pointId
}

// 从台账行或详情进入时带点位，并拉取该点位当前未还数量作为归还上限提示
const openReturnDialog = async (row) => {
  resetReturnForm()
  if (row && row.pointId) {
    prefillPoint(row.pointId)
    await loadReturnTarget(row.pointId)
  }
  returnDialogVisible.value = true
}

const onReturnPointChange = async () => {
  returnTargetOutstanding.value = 0
  if (returnForm.value.pointId) {
    await loadReturnTarget(returnForm.value.pointId)
  }
}

const loadReturnTarget = async (pointId) => {
  try {
    const rows = await getAntiSlipMatLedgers({ pointId })
    returnTargetOutstanding.value = rows[0]?.outstandingCount || 0
  } catch (error) {
    returnTargetOutstanding.value = 0
  }
}

const handleReturnSubmit = async () => {
  const form = returnForm.value
  if (!form.pointId) {
    ElMessage.warning('请先选择街区、路段和点位，没有点位不能提交')
    return
  }
  if (pointClosedMap.value[form.pointId]) {
    ElMessage.warning('该点位处于临时封闭期，封闭期内不办理防滑垫归还')
    return
  }
  if (!form.quantity || form.quantity <= 0) {
    ElMessage.warning('归还数量必须大于0')
    return
  }
  if (form.damagedQuantity === null || form.damagedQuantity === undefined || form.damagedQuantity < 0) {
    ElMessage.warning('破损数量必须为大于等于0的整数')
    return
  }
  if (form.damagedQuantity > form.quantity) {
    ElMessage.warning('破损数量不能大于本次归还数量')
    return
  }
  if (returnTargetOutstanding.value > 0 && form.quantity > returnTargetOutstanding.value) {
    ElMessage.warning(`本次归还${form.quantity}张超过该点位当前未还数量${returnTargetOutstanding.value}张`)
    return
  }
  if (!form.operatedAt) {
    ElMessage.warning('请选择归还时间')
    return
  }
  if (!form.operator.trim()) {
    ElMessage.warning('请填写经办人')
    return
  }
  try {
    submitting.value = true
    await returnAntiSlipMat({
      pointId: form.pointId,
      quantity: form.quantity,
      damagedQuantity: form.damagedQuantity,
      operatedAt: form.operatedAt,
      operator: form.operator.trim(),
      remark: form.remark || null
    })
    ElMessage.success('防滑垫归还登记成功')
    returnDialogVisible.value = false
    await reloadAll()
  } catch (error) {
    ElMessage.error(error.message || '归还登记失败')
  } finally {
    submitting.value = false
  }
}

// ============ 点位详情 ============
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const detailRecords = ref([])

// 详情弹窗内点“去登记归还”：先关详情再打开归还，避免归还成功后详情仍显示旧的未还数
const openReturnFromDetail = () => {
  const row = detail.value
  detailDialogVisible.value = false
  openReturnDialog(row)
}

const openDetail = async (row) => {
  detail.value = row
  detailRecords.value = []
  detailDialogVisible.value = true
  detailLoading.value = true
  try {
    detailRecords.value = await getAntiSlipMatRecordsByPoint(row.pointId)
  } catch (error) {
    ElMessage.error(error.message || '加载点位防滑垫领用详情失败')
  } finally {
    detailLoading.value = false
  }
}

// ============ 整体刷新 ============
const handleTabChange = (name) => {
  if (name === 'records' && allRecords.value.length === 0) {
    loadRecords()
  }
}

const reloadAll = async () => {
  // 整段加载期间保持 loading，避免树接口未回来时表格先闪“暂无记录”空态
  loading.value = true
  try {
    // 台账列表与街区树并行加载、各自兜底：树接口慢或失败不影响列表直接展示
    const [pruned] = await Promise.all([loadTreeData(), loadLedgers()])
    if (pruned) {
      await loadLedgers()
    }
    if (activeTab.value === 'records') {
      await loadRecords()
    }
  } catch (error) {
    ElMessage.error(error.message || '刷新失败')
  } finally {
    loading.value = false
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
  align-items: center;
}

.filter-item {
  width: 160px;
}

.filter-item.wide {
  width: 220px;
}

.filter-check {
  height: 32px;
}

.result-summary {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}

.result-summary b {
  color: #303133;
}

.outstanding-text {
  color: #f56c6c;
}

.data-table {
  width: 100%;
}

:deep(.outstanding-row) {
  background-color: #fef0f0;
  cursor: pointer;
}

:deep(.el-table__row) {
  cursor: pointer;
}

.muted-text {
  color: #909399;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.return-alert {
  margin-bottom: 14px;
}

.detail-desc {
  margin-bottom: 12px;
}

.detail-action {
  margin-bottom: 10px;
}

.detail-table {
  margin-top: 4px;
}
</style>
