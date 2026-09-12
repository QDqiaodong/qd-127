<template>
  <div class="tree-management">
    <el-card>
      <template #header>
        <div class="header">
          <h3>街区树形分类管理</h3>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            添加节点
          </el-button>
        </div>
      </template>

      <div class="tree-wrapper">
        <el-tree
          :data="treeData"
          :props="treeProps"
          node-key="id"
          :expand-on-click-node="false"
          :default-expand-all="true"
          @node-click="handleNodeClick"
          @node-contextmenu="handleRightClick"
        >
          <template #default="{ node, data }">
            <span class="tree-node">
              <el-icon v-if="data.level === 1"><Location /></el-icon>
              <el-icon v-else-if="data.level === 2"><Grid /></el-icon>
              <el-icon v-else><CirclePlus /></el-icon>
              {{ data.name }}
              <span class="node-level">
                {{ data.level === 1 ? '街区' : data.level === 2 ? '路段' : '点位' }}
              </span>
              <el-tag
                v-if="data.level === 3"
                size="small"
                :type="capacityTagType(data)"
                class="capacity-tag"
                effect="plain"
              >
                容量 {{ data.occupiedCount ?? 0 }}/{{ data.capacity }}
                <span v-if="data.remainingCount === 0" class="capacity-full">已满</span>
                <span v-else>余{{ data.remainingCount }}</span>
              </el-tag>
              <el-tag
                v-if="data.level === 3 && data.closed"
                size="small"
                type="danger"
                class="capacity-tag"
              >
                封闭中
              </el-tag>
              <el-tag
                v-if="data.level === 3 && data.lightingResult === 1"
                size="small"
                type="success"
                effect="plain"
                class="capacity-tag"
              >
                照明完好
              </el-tag>
              <el-tag
                v-else-if="data.level === 3 && data.lightingResult === 0"
                size="small"
                type="danger"
                effect="plain"
                class="capacity-tag"
              >
                照明异常·{{ data.lightingProblemType || '异常' }}
              </el-tag>
              <el-tag
                v-else-if="data.level === 3"
                size="small"
                type="info"
                effect="plain"
                class="capacity-tag"
              >
                照明未巡查
              </el-tag>
            </span>
          </template>
        </el-tree>
      </div>

      <el-pagination
        v-if="selectedNode && selectedNode.level === 2"
        class="export-pagination"
        layout="->, total, prev, pager, next"
        :total="sectionBenchCount"
        @current-change="handleExport"
      >
        <template #prev>
          <el-button @click="handleExport">导出路段资产</el-button>
        </template>
      </el-pagination>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑节点' : '添加节点'" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="节点名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入节点名称" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="上级节点">
          <el-select v-model="form.parentId" placeholder="请选择上级节点" @change="handleParentChange">
            <el-option :value="null" label="无（创建街区）" />
            <el-option
              v-for="node in parentOptions"
              :key="node.id"
              :value="node.id"
              :label="node.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item v-if="!isEdit && formLevel === 3" label="长凳容量">
          <el-input-number v-model="form.capacity" :min="0" />
          <span class="form-tip">该点位最多可摆放的长凳数量（默认10）</span>
        </el-form-item>
        <template v-if="isEdit && editingPoint">
          <el-form-item label="当前容量状态">
            <el-tag size="small" :type="capacityTagType(editingPoint)" effect="plain">
              已摆 {{ editingPoint.occupiedCount ?? 0 }} / 上限 {{ editingPoint.capacity }}
              ，剩余 {{ editingPoint.remainingCount ?? 0 }}
            </el-tag>
          </el-form-item>
          <el-form-item label="新容量">
            <el-input-number v-model="form.capacity" :min="0" />
          </el-form-item>
          <el-form-item label="调整原因">
            <el-input
              v-model="form.adjustReason"
              type="textarea"
              :rows="2"
              placeholder="修改容量时必填，将记录到容量调整台账"
            />
          </el-form-item>
          <el-form-item v-if="editingPoint.capacityUpdatedAt" label="最近调整">
            <span class="form-tip">
              {{ editingPoint.capacityUpdatedAt }}
              {{ editingPoint.capacityUpdatedReason ? '｜' + editingPoint.capacityUpdatedReason : '' }}
            </span>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="capacityLogVisible" :title="`容量调整记录 - ${capacityLogNodeName}`" width="640px">
      <el-table :data="capacityLogs" border size="small">
        <el-table-column prop="adjustedAt" label="调整时间" width="170" />
        <el-table-column label="容量变化" width="110">
          <template #default="{ row }">
            {{ row.oldCapacity === null ? '—' : row.oldCapacity }} → {{ row.newCapacity }}
          </template>
        </el-table-column>
        <el-table-column prop="occupiedCount" label="调整时占用" width="100" />
        <el-table-column prop="adjustReason" label="调整原因" />
        <el-table-column prop="adjustedBy" label="操作人" width="90" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="exportDialogVisible" title="导出路段资产" width="420px">
      <el-form label-width="90px">
        <el-form-item label="路段">
          <span>{{ exportTargetNode ? exportTargetNode.name : '' }}</span>
        </el-form-item>
        <el-form-item label="导出范围">
          <el-radio-group v-model="exportScope">
            <el-radio value="all">全部（在用 + 停用）</el-radio>
            <el-radio value="active">只导出在用</el-radio>
            <el-radio value="disabled">只导出停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="统计口径">
          <span class="form-tip">
            文件中“点位占用/剩余”按在用长凳统计，与树上点位占用一致；停用凳数单独成列
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmExport">导出</el-button>
      </template>
    </el-dialog>

    <el-menu
      v-if="menuVisible"
      :default-active="''"
      class="context-menu"
      :style="{ left: menuPosition.x + 'px', top: menuPosition.y + 'px' }"
    >
      <el-menu-item @click="handleEdit">
        <el-icon><Edit /></el-icon>
        编辑
      </el-menu-item>
      <el-menu-item v-if="rightClickedNode && rightClickedNode.level === 3" @click="handleCapacityLogs">
        <el-icon><Tickets /></el-icon>
        容量调整记录
      </el-menu-item>
      <el-menu-item @click="handleDelete" style="color: #f56c6c">
        <el-icon><Delete /></el-icon>
        删除
      </el-menu-item>
      <el-menu-item
        v-if="rightClickedNode && rightClickedNode.level === 2"
        @click="handleExportSection"
      >
        <el-icon><Download /></el-icon>
        导出资产
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Edit, Delete, Download, Location, Grid, CirclePlus, Tickets } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTree, createNode, updateNode, deleteNode, adjustCapacity, getCapacityLogs } from '../api/tree'
import { getBenchesByNode, exportAssets } from '../api/bench'

const treeData = ref([])
const treeProps = {
  children: 'children',
  label: 'name'
}

const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null,
  name: '',
  parentId: null,
  level: 1,
  sortOrder: 0,
  capacity: 10,
  adjustReason: ''
})

const selectedNode = ref(null)
const menuVisible = ref(false)
const menuPosition = ref({ x: 0, y: 0 })
const rightClickedNode = ref(null)
const sectionBenchCount = ref(0)

const capacityLogVisible = ref(false)
const capacityLogs = ref([])
const capacityLogNodeName = ref('')

const exportDialogVisible = ref(false)
const exportScope = ref('all')
const exportTargetNode = ref(null)
const exportScopeOptions = [
  { value: 'all', label: '全部' },
  { value: 'active', label: '在用' },
  { value: 'disabled', label: '停用' }
]

const formLevel = computed(() => {
  if (isEdit.value) {
    return form.value.level
  }
  return form.value.parentId ? getParentLevel(form.value.parentId) + 1 : 1
})

const editingPoint = computed(() =>
  isEdit.value && form.value.level === 3 ? rightClickedNode.value : null
)

const capacityTagType = (point) => {
  if (!point || point.remainingCount === undefined || point.remainingCount === null) return 'info'
  if (point.remainingCount === 0) return 'danger'
  if (point.remainingCount <= 2) return 'warning'
  return 'success'
}

const parentOptions = computed(() => {
  const options = []
  const collectOptions = (nodes) => {
    nodes.forEach(node => {
      if (node.level < 3) {
        options.push(node)
      }
      if (node.children && node.children.length > 0) {
        collectOptions(node.children)
      }
    })
  }
  collectOptions(treeData.value)
  return options
})

const loadTree = async () => {
  try {
    treeData.value = await getTree()
  } catch (error) {
    ElMessage.error(error.message || '加载树形数据失败')
  }
}

const handleNodeClick = (data) => {
  selectedNode.value = data
  if (data.level === 2) {
    loadSectionBenches(data.id)
  }
}

const loadSectionBenches = async (sectionId) => {
  try {
    const benches = await getBenchesByNode(sectionId)
    sectionBenchCount.value = benches.length
  } catch (error) {
    sectionBenchCount.value = 0
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: null,
    name: '',
    parentId: null,
    level: 1,
    sortOrder: 0,
    capacity: 10,
    adjustReason: ''
  }
  dialogVisible.value = true
}

const handleParentChange = () => {
  form.value.level = form.value.parentId ? getParentLevel(form.value.parentId) + 1 : 1
  if (form.value.level === 3 && form.value.capacity === null) {
    form.value.capacity = 10
  }
}

const handleEdit = () => {
  if (!rightClickedNode.value) return
  isEdit.value = true
  form.value = {
    id: rightClickedNode.value.id,
    name: rightClickedNode.value.name,
    parentId: rightClickedNode.value.parentId,
    level: rightClickedNode.value.level,
    sortOrder: rightClickedNode.value.sortOrder || 0,
    capacity: rightClickedNode.value.capacity ?? 10,
    adjustReason: ''
  }
  menuVisible.value = false
  dialogVisible.value = true
}

const handleCapacityLogs = async () => {
  if (!rightClickedNode.value || rightClickedNode.value.level !== 3) return
  const node = rightClickedNode.value
  menuVisible.value = false
  try {
    capacityLogNodeName.value = node.name
    capacityLogs.value = await getCapacityLogs(node.id)
    capacityLogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载容量调整记录失败')
  }
}

const handleDelete = async () => {
  if (!rightClickedNode.value) return
  try {
    await ElMessageBox.confirm('确定删除该节点吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteNode(rightClickedNode.value.id)
    ElMessage.success('删除成功')
    loadTree()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    menuVisible.value = false
  }
}

const handleSubmit = async () => {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入节点名称')
    return
  }

  try {
    if (isEdit.value) {
      await updateNode(form.value.id, {
        name: form.value.name,
        sortOrder: form.value.sortOrder
      })

      if (form.value.level === 3) {
        const point = rightClickedNode.value
        const newCapacity = form.value.capacity
        const oldCapacity = point.capacity ?? 10
        if (newCapacity !== oldCapacity) {
          if (!form.value.adjustReason || !form.value.adjustReason.trim()) {
            ElMessage.warning('调整容量必须填写调整原因')
            return
          }
          await adjustCapacity(form.value.id, {
            capacity: newCapacity,
            adjustReason: form.value.adjustReason.trim()
          })
          ElMessage.success('节点更新成功，容量已调整')
        } else {
          ElMessage.success('更新成功')
        }
      } else {
        ElMessage.success('更新成功')
      }
    } else {
      const level = formLevel.value
      const payload = {
        name: form.value.name,
        parentId: form.value.parentId,
        level,
        sortOrder: form.value.sortOrder
      }
      if (level === 3) {
        payload.capacity = form.value.capacity
      }
      await createNode(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTree()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  }
}

const getParentLevel = (parentId) => {
  const findLevel = (nodes) => {
    for (const node of nodes) {
      if (node.id === parentId) {
        return node.level
      }
      if (node.children && node.children.length > 0) {
        const level = findLevel(node.children)
        if (level) return level
      }
    }
    return 1
  }
  return findLevel(treeData.value) || 1
}

const handleRightClick = (event, data) => {
  event.preventDefault()
  rightClickedNode.value = data
  menuPosition.value = {
    x: event.clientX,
    y: event.clientY
  }
  menuVisible.value = true
  document.addEventListener('click', closeMenu)
}

const closeMenu = () => {
  menuVisible.value = false
  document.removeEventListener('click', closeMenu)
}

const handleExportSection = () => {
  if (!rightClickedNode.value || rightClickedNode.value.level !== 2) return
  menuVisible.value = false
  openExportDialog(rightClickedNode.value)
}

const handleExport = () => {
  if (!selectedNode.value || selectedNode.value.level !== 2) return
  openExportDialog(selectedNode.value)
}

const openExportDialog = (node) => {
  exportTargetNode.value = node
  exportScope.value = 'all'
  exportDialogVisible.value = true
}

const confirmExport = async () => {
  const node = exportTargetNode.value
  if (!node) return
  const scope = exportScope.value
  const scopeLabel = (exportScopeOptions.find(s => s.value === scope) || {}).label || ''
  try {
    const assets = await exportAssets(node.id, scope)
    if (!assets || assets.length === 0) {
      ElMessage.warning(`路段【${node.name}】在所选范围（${scopeLabel}）内没有可导出的长凳，未生成文件`)
      return
    }
    const csv = convertToCSV(assets)
    downloadCSV(csv, `路段_${node.name}_资产_${scopeLabel}.csv`)
    ElMessage.success(`导出成功，共 ${assets.length} 条（范围：${scopeLabel}）`)
    exportDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  }
}

const convertToCSV = (data) => {
  if (!data || data.length === 0) return ''
  const headers = Object.keys(data[0])
  const escapeCell = (val) => `"${String(val ?? '').replace(/"/g, '""')}"`
  const rows = data.map(row => headers.map(header => escapeCell(row[header])).join(','))
  return [headers.join(','), ...rows].join('\n')
}

const downloadCSV = (content, filename) => {
  const blob = new Blob(['\uFEFF', content], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

onMounted(() => {
  loadTree()
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

.tree-wrapper {
  max-height: 600px;
  overflow-y: auto;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-level {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.capacity-tag {
  margin-left: 4px;
}

.capacity-full {
  color: #f56c6c;
  margin-left: 4px;
  font-weight: 600;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.context-menu {
  position: fixed;
  z-index: 9999;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.export-pagination {
  margin-top: 20px;
}
</style>
