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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑节点' : '添加节点'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="节点名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入节点名称" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="上级节点">
          <el-select v-model="form.parentId" placeholder="请选择上级节点">
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
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
import { Plus, Edit, Delete, Download, Location, Grid, CirclePlus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTree, createNode, updateNode, deleteNode, getChildren } from '../api/tree'
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
  sortOrder: 0
})

const selectedNode = ref(null)
const menuVisible = ref(false)
const menuPosition = ref({ x: 0, y: 0 })
const rightClickedNode = ref(null)
const sectionBenchCount = ref(0)

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
    ElMessage.error('加载树形数据失败')
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
    sortOrder: 0
  }
  dialogVisible.value = true
}

const handleEdit = () => {
  if (!rightClickedNode.value) return
  isEdit.value = true
  form.value = {
    id: rightClickedNode.value.id,
    name: rightClickedNode.value.name,
    parentId: rightClickedNode.value.parentId,
    level: rightClickedNode.value.level,
    sortOrder: rightClickedNode.value.sortOrder || 0
  }
  menuVisible.value = false
  dialogVisible.value = true
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
      ElMessage.success('更新成功')
    } else {
      const level = form.value.parentId ? getParentLevel(form.value.parentId) + 1 : 1
      await createNode({
        name: form.value.name,
        parentId: form.value.parentId,
        level,
        sortOrder: form.value.sortOrder
      })
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

const handleExportSection = async () => {
  if (!rightClickedNode.value || rightClickedNode.value.level !== 2) return
  try {
    const assets = await exportAssets(rightClickedNode.value.id)
    const csv = convertToCSV(assets)
    downloadCSV(csv, `路段_${rightClickedNode.value.name}_资产.csv`)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  }
  menuVisible.value = false
}

const handleExport = async () => {
  if (!selectedNode.value || selectedNode.value.level !== 2) return
  try {
    const assets = await exportAssets(selectedNode.value.id)
    const csv = convertToCSV(assets)
    downloadCSV(csv, `路段_${selectedNode.value.name}_资产.csv`)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  }
}

const convertToCSV = (data) => {
  if (!data || data.length === 0) return ''
  const headers = Object.keys(data[0])
  const rows = data.map(row => headers.map(header => `"${row[header] || ''}"`).join(','))
  return [headers.join(','), ...rows].join('\n')
}

const downloadCSV = (content, filename) => {
  const blob = new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8;' })
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
