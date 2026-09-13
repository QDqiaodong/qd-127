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

      <el-table :data="pagedLogs" v-loading="loading" border class="log-table">
        <el-table-column prop="benchCode" label="长凳编号" />
        <el-table-column prop="oldNodeName" label="原点位" />
        <el-table-column prop="newNodeName" label="新点位" />
        <el-table-column prop="changeReason" label="变更原因" />
        <el-table-column prop="changedAt" label="变更时间" />
        <el-table-column prop="changedBy" label="操作人" />
        <template #empty>
          <el-empty :description="emptyDescription" :image-size="60" />
        </template>
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
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getAllChangeLogs } from '../api/bench'

const logs = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

// 搜索词与页码持久化：关掉页面再打开时恢复，列表条数与页码保持一致
const FILTER_STORAGE_KEY = 'change-log-filters'

const restoreFilters = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(FILTER_STORAGE_KEY))
    if (saved && typeof saved === 'object') {
      searchKeyword.value = typeof saved.keyword === 'string' ? saved.keyword : ''
      currentPage.value = Number.isInteger(saved.page) && saved.page > 0 ? saved.page : 1
    }
  } catch (error) {
    localStorage.removeItem(FILTER_STORAGE_KEY)
  }
}

const persistFilters = () => {
  localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify({
    keyword: searchKeyword.value,
    page: currentPage.value
  }))
}

const filteredLogs = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return logs.value
  }
  return logs.value.filter(log =>
    (log.benchCode || '').toLowerCase().includes(keyword)
  )
})

// 当前页数据：翻页时只展示本页的调位点位记录，与分页器页码、条数对得上
const pagedLogs = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredLogs.value.slice(start, start + pageSize.value)
})

// 无命中时明确提示为空，并区分“没有任何台账”与“关键字没有命中”
const emptyDescription = computed(() => {
  if (logs.value.length === 0) {
    return '暂无分类变更台账记录'
  }
  return `没有找到长凳编号包含“${searchKeyword.value.trim()}”的变更记录`
})

const loadLogs = async () => {
  loading.value = true
  try {
    logs.value = await getAllChangeLogs()
    // 刷新只重新拉取数据，保留当前搜索词与页码；
    // 若刷新后命中条数变少导致页码越界，夹取到最后一页，避免出现空白页
    const maxPage = Math.max(1, Math.ceil(filteredLogs.value.length / pageSize.value))
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage
    }
    persistFilters()
  } catch (error) {
    ElMessage.error(error.message || '加载变更日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  persistFilters()
}

const handleReset = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  persistFilters()
}

onMounted(() => {
  restoreFilters()
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
