<template>
  <div class="change-log-management">
    <el-card>
      <template #header>
        <div class="header">
          <h3>分类变更台账</h3>
          <el-button @click="loadLogs">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <div class="filter-section">
        <el-input v-model="searchKeyword" placeholder="搜索长凳编号" class="filter-item" @keyup.enter="handleSearch" />
        <el-button @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="filteredLogs" border class="log-table">
        <el-table-column prop="benchCode" label="长凳编号" />
        <el-table-column prop="oldNodeName" label="原点位" />
        <el-table-column prop="newNodeName" label="新点位" />
        <el-table-column prop="changeReason" label="变更原因" />
        <el-table-column prop="changedAt" label="变更时间" />
        <el-table-column prop="changedBy" label="操作人" />
      </el-table>

      <el-pagination
        class="pagination"
        layout="total, prev, pager, next"
        :total="filteredLogs.length"
        :page-size="pageSize"
        v-model:current-page="currentPage"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getAllChangeLogs } from '../api/bench'

const logs = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const filteredLogs = computed(() => {
  let result = logs.value
  if (searchKeyword.value) {
    result = result.filter(log =>
      log.benchCode.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
  }
  return result
})

const loadLogs = async () => {
  try {
    logs.value = await getAllChangeLogs()
    currentPage.value = 1
  } catch (error) {
    console.error('加载变更日志失败', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
}

const handleReset = () => {
  searchKeyword.value = ''
  currentPage.value = 1
}

onMounted(() => {
  loadLogs()
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
}

.filter-item {
  width: 200px;
}

.log-table {
  margin-top: 10px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}
</style>
