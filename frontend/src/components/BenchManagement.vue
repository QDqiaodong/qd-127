<template>
  <div class="bench-management">
    <el-card>
      <template #header>
        <div class="header">
          <h3>长凳档案管理</h3>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            添加长凳
          </el-button>
        </div>
      </template>

      <div class="filter-section">
        <el-select v-model="filterDistrict" placeholder="选择街区" class="filter-item" @change="handleDistrictChange">
          <el-option label="全部" :value="null" />
          <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
        </el-select>
        <el-select v-model="filterSection" placeholder="选择路段" class="filter-item" :disabled="!filterDistrict" @change="handleSectionChange">
          <el-option label="全部" :value="null" />
          <el-option v-for="s in sections" :key="s.id" :value="s.id" :label="s.name" />
        </el-select>
        <el-select v-model="filterPoint" placeholder="选择点位" class="filter-item" :disabled="!filterSection">
          <el-option label="全部" :value="null" />
          <el-option v-for="p in points" :key="p.id" :value="p.id" :label="p.name" />
        </el-select>
        <el-input v-model="searchKeyword" placeholder="搜索编号/材质" class="filter-item" @keyup.enter="handleSearch" />
        <el-button @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="benchList" border class="bench-table" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="code" label="长凳编号" />
        <el-table-column prop="material" label="材质" />
        <el-table-column prop="length" label="长度(cm)" />
        <el-table-column prop="width" label="宽度(cm)" />
        <el-table-column prop="height" label="高度(cm)" />
        <el-table-column prop="districtName" label="所属街区" />
        <el-table-column prop="sectionName" label="所属路段" />
        <el-table-column prop="nodeName" label="所属点位" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            <el-button size="small" @click="handleViewLogs(row)">变更记录</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="batch-action" v-if="selectedBenches.length > 0">
        <span>已选择 {{ selectedBenches.length }} 条记录</span>
        <el-button type="warning" @click="handleBatchChange">批量调整点位</el-button>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑长凳' : '添加长凳'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="长凳编号" prop="code">
          <el-input v-model="form.code" placeholder="请输入长凳编号" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="材质" prop="material">
          <el-input v-model="form.material" placeholder="请输入材质" />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="8">
            <el-form-item label="长度(cm)">
              <el-input-number v-model="form.length" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="宽度(cm)">
              <el-input-number v-model="form.width" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="高度(cm)">
              <el-input-number v-model="form.height" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="所属街区">
          <el-select v-model="form.districtId" placeholder="请选择街区" @change="handleFormDistrictChange">
            <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属路段">
          <el-select v-model="form.sectionId" placeholder="请选择路段" :disabled="!form.districtId" @change="handleFormSectionChange">
            <el-option v-for="s in formSections" :key="s.id" :value="s.id" :label="s.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属点位">
          <el-select v-model="form.nodeId" placeholder="请选择点位" :disabled="!form.sectionId">
            <el-option v-for="p in formPoints" :key="p.id" :value="p.id" :label="p.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" title="批量调整点位" width="500px">
      <el-form :model="batchForm" label-width="100px">
        <el-form-item label="目标街区">
          <el-select v-model="batchForm.districtId" placeholder="请选择街区" @change="handleBatchDistrictChange">
            <el-option v-for="d in districts" :key="d.id" :value="d.id" :label="d.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标路段">
          <el-select v-model="batchForm.sectionId" placeholder="请选择路段" :disabled="!batchForm.districtId" @change="handleBatchSectionChange">
            <el-option v-for="s in batchSections" :key="s.id" :value="s.id" :label="s.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标点位">
          <el-select v-model="batchForm.newNodeId" placeholder="请选择点位" :disabled="!batchForm.sectionId">
            <el-option v-for="p in batchPoints" :key="p.id" :value="p.id" :label="p.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更原因">
          <el-input v-model="batchForm.changeReason" type="textarea" placeholder="请输入变更原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchSubmit">确定变更</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logDialogVisible" title="变更记录" width="600px">
      <el-table :data="changeLogs" border>
        <el-table-column prop="changedAt" label="变更时间" />
        <el-table-column prop="oldNodeName" label="原点位" />
        <el-table-column prop="newNodeName" label="新点位" />
        <el-table-column prop="changeReason" label="变更原因" />
        <el-table-column prop="changedBy" label="操作人" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { Plus, Search, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllBenches, getBenchById, createBench, updateBench, deleteBench, changeBenchNode, getChangeLogs, getBenchesByNode } from '../api/bench'
import { getTree } from '../api/tree'

const benchList = ref([])
const districts = ref([])
const sections = ref([])
const points = ref([])

const filterDistrict = ref(null)
const filterSection = ref(null)
const filterPoint = ref(null)
const searchKeyword = ref('')

const selectedBenches = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null,
  code: '',
  material: '',
  length: null,
  width: null,
  height: null,
  nodeId: null,
  districtId: null,
  sectionId: null,
  status: 1
})

const formSections = ref([])
const formPoints = ref([])

const batchDialogVisible = ref(false)
const batchForm = ref({
  districtId: null,
  sectionId: null,
  newNodeId: null,
  changeReason: ''
})

const batchSections = ref([])
const batchPoints = ref([])

const logDialogVisible = ref(false)
const changeLogs = ref([])

const loadBenches = async () => {
  try {
    benchList.value = await getAllBenches()
  } catch (error) {
    ElMessage.error('加载长凳数据失败')
  }
}

const loadTreeData = async () => {
  try {
    const tree = await getTree()
    districts.value = tree
    buildSectionMap(tree)
  } catch (error) {
    ElMessage.error('加载树形数据失败')
  }
}

const sectionMap = ref({})
const pointMap = ref({})

const buildSectionMap = (tree) => {
  const sm = {}
  const pm = {}
  tree.forEach(district => {
    if (district.children) {
      district.children.forEach(section => {
        sm[district.id] = sm[district.id] || []
        sm[district.id].push(section)
        if (section.children) {
          section.children.forEach(point => {
            pm[section.id] = pm[section.id] || []
            pm[section.id].push(point)
          })
        }
      })
    }
  })
  sectionMap.value = sm
  pointMap.value = pm
}

const handleDistrictChange = () => {
  filterSection.value = null
  filterPoint.value = null
  sections.value = filterDistrict.value ? sectionMap.value[filterDistrict.value] || [] : []
  points.value = []
  filterBenches()
}

const handleSectionChange = () => {
  filterPoint.value = null
  points.value = filterSection.value ? pointMap.value[filterSection.value] || [] : []
  filterBenches()
}

const filterBenches = async () => {
  try {
    if (filterPoint.value) {
      benchList.value = await getBenchesByNode(filterPoint.value)
    } else if (filterSection.value) {
      benchList.value = await getBenchesByNode(filterSection.value)
    } else if (filterDistrict.value) {
      benchList.value = await getBenchesByNode(filterDistrict.value)
    } else {
      benchList.value = await getAllBenches()
    }
    if (searchKeyword.value) {
      benchList.value = benchList.value.filter(b =>
        b.code.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
        (b.material && b.material.toLowerCase().includes(searchKeyword.value.toLowerCase()))
      )
    }
  } catch (error) {
    ElMessage.error('筛选失败')
  }
}

const handleSearch = () => {
  filterBenches()
}

const handleReset = () => {
  filterDistrict.value = null
  filterSection.value = null
  filterPoint.value = null
  searchKeyword.value = ''
  sections.value = []
  points.value = []
  loadBenches()
}

const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: null,
    code: '',
    material: '',
    length: null,
    width: null,
    height: null,
    nodeId: null,
    districtId: null,
    sectionId: null,
    status: 1
  }
  formSections.value = []
  formPoints.value = []
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  try {
    const bench = await getBenchById(row.id)
    form.value = {
      id: bench.id,
      code: bench.code,
      material: bench.material,
      length: bench.length,
      width: bench.width,
      height: bench.height,
      nodeId: bench.nodeId,
      districtId: null,
      sectionId: null,
      status: bench.status
    }
    await loadFormNodeInfo(bench.nodeId)
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取长凳信息失败')
  }
}

const loadFormNodeInfo = async (nodeId) => {
  const tree = await getTree()
  const findPath = (nodes, targetId, path = []) => {
    for (const node of nodes) {
      if (node.id === targetId) return [...path, node]
      if (node.children) {
        const result = findPath(node.children, targetId, [...path, node])
        if (result) return result
      }
    }
    return null
  }
  const path = findPath(tree, nodeId)
  if (path && path.length >= 3) {
    form.value.districtId = path[0].id
    form.value.sectionId = path[1].id
    formSections.value = sectionMap.value[path[0].id] || []
    formPoints.value = pointMap.value[path[1].id] || []
  }
}

const handleFormDistrictChange = () => {
  form.value.sectionId = null
  form.value.nodeId = null
  formSections.value = form.value.districtId ? sectionMap.value[form.value.districtId] || [] : []
  formPoints.value = []
}

const handleFormSectionChange = () => {
  form.value.nodeId = null
  formPoints.value = form.value.sectionId ? pointMap.value[form.value.sectionId] || [] : []
}

const handleSubmit = async () => {
  if (!form.value.code.trim()) {
    ElMessage.warning('请输入长凳编号')
    return
  }
  if (!form.value.nodeId) {
    ElMessage.warning('请选择所属点位')
    return
  }

  try {
    const data = {
      code: form.value.code,
      material: form.value.material,
      length: form.value.length,
      width: form.value.width,
      height: form.value.height,
      nodeId: form.value.nodeId,
      status: form.value.status
    }

    if (isEdit.value) {
      await updateBench(form.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await createBench(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadBenches()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该长凳吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteBench(row.id)
    ElMessage.success('删除成功')
    loadBenches()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleSelectionChange = (val) => {
  selectedBenches.value = val
}

const handleBatchChange = () => {
  batchForm.value = {
    districtId: null,
    sectionId: null,
    newNodeId: null,
    changeReason: ''
  }
  batchSections.value = []
  batchPoints.value = []
  batchDialogVisible.value = true
}

const handleBatchDistrictChange = () => {
  batchForm.value.sectionId = null
  batchForm.value.newNodeId = null
  batchSections.value = batchForm.value.districtId ? sectionMap.value[batchForm.value.districtId] || [] : []
  batchPoints.value = []
}

const handleBatchSectionChange = () => {
  batchForm.value.newNodeId = null
  batchPoints.value = batchForm.value.sectionId ? pointMap.value[batchForm.value.sectionId] || [] : []
}

const handleBatchSubmit = async () => {
  if (!batchForm.value.newNodeId) {
    ElMessage.warning('请选择目标点位')
    return
  }

  try {
    const benchIds = selectedBenches.value.map(b => b.id)
    await changeBenchNode({
      benchIds,
      newNodeId: batchForm.value.newNodeId,
      changeReason: batchForm.value.changeReason
    })
    ElMessage.success('批量变更成功')
    batchDialogVisible.value = false
    selectedBenches.value = []
    loadBenches()
  } catch (error) {
    ElMessage.error(error.message || '批量变更失败')
  }
}

const handleViewLogs = async (row) => {
  try {
    changeLogs.value = await getChangeLogs(row.id)
    logDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取变更记录失败')
  }
}

onMounted(() => {
  loadBenches()
  loadTreeData()
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
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-item {
  width: 180px;
}

.bench-table {
  margin-top: 10px;
}

.batch-action {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 15px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
}

.batch-action span {
  color: #606266;
}
</style>
