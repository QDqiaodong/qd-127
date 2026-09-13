<template>
  <div class="bench-sponsorship-management">
    <el-card>
      <template #header>
        <div class="header">
          <h3>商户长凳冠名台账</h3>
          <div>
            <el-button type="primary" @click="openCreateDialog">
              <el-icon><Plus /></el-icon>
              新增冠名登记
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
        title="按点位登记商户冠名：提交后先进入待生效，到达开始日期自动变为生效中，结束日期过后自动标记已过期。"
        class="rule-alert"
      />

      <div class="filter-section">
        <el-select v-model="filters.districtId" placeholder="选择街区" class="filter-item" clearable @change="handleDistrictFilterChange">
          <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
        </el-select>
        <el-select v-model="filters.sectionId" placeholder="选择路段" class="filter-item" clearable :disabled="!filters.districtId" @change="handleSectionFilterChange">
          <el-option v-for="s in filteredSections" :key="s.id" :value="s.id" :label="s.name" />
        </el-select>
        <el-select v-model="filters.pointId" placeholder="选择点位" class="filter-item" clearable :disabled="!filters.sectionId">
          <el-option v-for="p in filteredPoints" :key="p.id" :value="p.id" :label="p.name" />
        </el-select>
        <el-select v-model="filters.status" placeholder="冠名状态" class="filter-item filter-status" clearable @change="loadSponsorships">
          <el-option label="待生效" :value="1" />
          <el-option label="生效中" :value="2" />
          <el-option label="已过期" :value="3" />
        </el-select>
        <el-button type="primary" @click="loadSponsorships">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <div v-if="sponsorships.length > 0" class="result-summary">
        共 <b>{{ sponsorships.length }}</b> 条{{ filters.status ? `（${statusLabel(filters.status)}）` : '' }}冠名记录
      </div>

      <el-table :data="sponsorships" v-loading="loading" border class="data-table">
        <el-table-column prop="districtName" label="街区" width="150" show-overflow-tooltip />
        <el-table-column prop="sectionName" label="路段" width="160" show-overflow-tooltip />
        <el-table-column prop="pointName" label="冠名点位" width="180" show-overflow-tooltip />
        <el-table-column prop="merchantName" label="商户名称" width="150" show-overflow-tooltip />
        <el-table-column prop="sponsorshipText" label="冠名文案" min-width="200" show-overflow-tooltip />
        <el-table-column label="冠名起止日期" width="200">
          <template #default="{ row }">{{ row.startDate }} 至 {{ row.endDate }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagType(row.effectiveStatus)">
              {{ statusLabel(row.effectiveStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="登记时间" width="170" />
        <template #empty>
          <el-empty :description="emptyDescription" :image-size="70" />
        </template>
      </el-table>
    </el-card>

    <el-dialog v-model="createDialogVisible" title="新增商户冠名登记" width="620px" @closed="resetCreateForm">
      <el-form :model="createForm" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="所属街区" required>
              <el-select v-model="createForm.districtId" placeholder="请选择街区" style="width: 100%" @change="handleCreateDistrictChange">
                <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="路段" required>
              <el-select v-model="createForm.sectionId" placeholder="请选择路段" style="width: 100%" :disabled="!createForm.districtId" @change="handleCreateSectionChange">
                <el-option v-for="s in createSections" :key="s.id" :value="s.id" :label="s.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="冠名点位" required>
              <el-select v-model="createForm.pointId" placeholder="请选择点位" style="width: 100%" :disabled="!createForm.sectionId">
                <el-option v-for="p in createPoints" :key="p.id" :value="p.id" :label="p.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="商户名称" required>
          <el-input v-model="createForm.merchantName" placeholder="请输入冠名商户名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="冠名文案" required>
          <el-input v-model="createForm.sponsorshipText" type="textarea" :rows="3" placeholder="请输入将展示在长凳上的冠名文案" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="冠名起止" required>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="提交后状态">
          <el-tag type="warning" effect="plain">待生效（到达开始日期自动生效，结束后自动标记过期）</el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreateSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getTree } from '../api/tree'
import { createBenchSponsorship, getBenchSponsorships } from '../api/benchSponsorship'

const districts = ref([])
const sponsorships = ref([])
const loading = ref(false)
const submitting = ref(false)

const filters = ref({
  districtId: null,
  sectionId: null,
  pointId: null,
  status: null
})

// 筛选项持久化：关掉页面再打开时恢复上次筛选，列表与状态仍对得上
const FILTER_STORAGE_KEY = 'bench-sponsorship-filters'

const restoreFilters = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(FILTER_STORAGE_KEY))
    if (saved && typeof saved === 'object') {
      filters.value = {
        districtId: saved.districtId ?? null,
        sectionId: saved.sectionId ?? null,
        pointId: saved.pointId ?? null,
        status: [1, 2, 3].includes(saved.status) ? saved.status : null
      }
    }
  } catch (error) {
    localStorage.removeItem(FILTER_STORAGE_KEY)
  }
}

const persistFilters = () => {
  localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(filters.value))
}

const createDialogVisible = ref(false)
const createForm = ref({
  districtId: null,
  sectionId: null,
  pointId: null,
  merchantName: '',
  sponsorshipText: ''
})
const dateRange = ref([])

const getSections = (districtId) => {
  const district = districts.value.find(d => d.id === districtId)
  return district?.children || []
}

const getPoints = (districtId, sectionId) => {
  const section = getSections(districtId).find(s => s.id === sectionId)
  return section?.children || []
}

const createSections = computed(() => getSections(createForm.value.districtId))
const createPoints = computed(() => getPoints(createForm.value.districtId, createForm.value.sectionId))
const filteredSections = computed(() => getSections(filters.value.districtId))
const filteredPoints = computed(() => getPoints(filters.value.districtId, filters.value.sectionId))

const statusLabel = (status) => ({
  1: '待生效',
  2: '生效中',
  3: '已过期'
}[status] || '未知')

const statusTagType = (status) => ({
  1: 'warning',
  2: 'success',
  3: 'info'
}[status] || 'info')

// 空列表提示：明确区分“某状态没有记录”和“所选点位/范围没有记录”，避免空白说不清
const emptyDescription = computed(() => {
  if (filters.value.status) {
    return `暂无${statusLabel(filters.value.status)}的冠名记录`
  }
  if (filters.value.pointId || filters.value.sectionId || filters.value.districtId) {
    return '所选街区/路段/点位暂无商户冠名记录'
  }
  return '暂无商户冠名记录，可点击右上角“新增冠名登记”创建'
})

const loadTreeData = async () => {
  const tree = await getTree()
  districts.value = tree
  // 恢复的上次筛选可能因节点删除而失效，清掉悬空项避免查出空结果却说不清原因
  const sections = getSections(filters.value.districtId)
  if (filters.value.districtId && sections.length === 0) {
    filters.value.districtId = null
    filters.value.sectionId = null
    filters.value.pointId = null
  } else {
    const points = getPoints(filters.value.districtId, filters.value.sectionId)
    if (filters.value.sectionId && points.length === 0) {
      filters.value.sectionId = null
      filters.value.pointId = null
    } else if (filters.value.pointId && !points.some(p => p.id === filters.value.pointId)) {
      filters.value.pointId = null
    }
  }
}

const loadSponsorships = async () => {
  loading.value = true
  try {
    sponsorships.value = await getBenchSponsorships({
      districtId: filters.value.districtId || undefined,
      sectionId: filters.value.sectionId || undefined,
      pointId: filters.value.pointId || undefined,
      status: filters.value.status || undefined
    })
    persistFilters()
  } catch (error) {
    ElMessage.error(error.message || '加载冠名台账失败')
  } finally {
    loading.value = false
  }
}

const reloadAll = async () => {
  try {
    await loadTreeData()
    await loadSponsorships()
  } catch (error) {
    ElMessage.error(error.message || '刷新失败')
  }
}

const handleDistrictFilterChange = () => {
  filters.value.sectionId = null
  filters.value.pointId = null
}

const handleSectionFilterChange = () => {
  filters.value.pointId = null
}

const resetFilters = () => {
  filters.value = { districtId: null, sectionId: null, pointId: null, status: null }
  loadSponsorships()
}

const handleCreateDistrictChange = () => {
  createForm.value.sectionId = null
  createForm.value.pointId = null
}

const handleCreateSectionChange = () => {
  createForm.value.pointId = null
}

const openCreateDialog = () => {
  resetCreateForm()
  createDialogVisible.value = true
}

const resetCreateForm = () => {
  createForm.value = {
    districtId: null,
    sectionId: null,
    pointId: null,
    merchantName: '',
    sponsorshipText: ''
  }
  dateRange.value = []
}

const handleCreateSubmit = async () => {
  if (!createForm.value.districtId) {
    ElMessage.warning('请选择所属街区')
    return
  }
  if (!createForm.value.sectionId) {
    ElMessage.warning('请选择路段')
    return
  }
  if (!createForm.value.pointId) {
    ElMessage.warning('请选择冠名点位')
    return
  }
  if (!createForm.value.merchantName.trim()) {
    ElMessage.warning('请填写商户名称')
    return
  }
  if (!createForm.value.sponsorshipText.trim()) {
    ElMessage.warning('请填写冠名文案')
    return
  }
  if (!dateRange.value || dateRange.value.length !== 2 || !dateRange.value[0] || !dateRange.value[1]) {
    ElMessage.warning('请选择冠名起止日期')
    return
  }

  try {
    submitting.value = true
    await createBenchSponsorship({
      pointId: createForm.value.pointId,
      merchantName: createForm.value.merchantName.trim(),
      sponsorshipText: createForm.value.sponsorshipText.trim(),
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    })
    ElMessage.success('冠名记录提交成功')
    createDialogVisible.value = false
    await loadSponsorships()
  } catch (error) {
    ElMessage.error(error.message || '提交冠名记录失败')
  } finally {
    submitting.value = false
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
  width: 170px;
}

.filter-status {
  width: 140px;
}

.result-summary {
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}

.result-summary b {
  color: #303133;
}

.data-table {
  width: 100%;
}
</style>
