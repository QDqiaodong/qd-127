<template>
  <div class="app-container">
    <el-container>
      <el-aside width="220px" class="aside">
        <div class="logo">
          <h2>长凳管理系统</h2>
        </div>
        <el-menu :default-active="activeMenu" class="menu" @select="handleMenuSelect">
          <el-menu-item index="tree">
            <el-icon><Menu /></el-icon>
            <span>街区树形管理</span>
          </el-menu-item>
          <el-menu-item index="bench">
            <el-icon><Box /></el-icon>
            <span>长凳档案管理</span>
          </el-menu-item>
          <el-menu-item index="inspection">
            <el-icon><Tools /></el-icon>
            <span>巡检与维修管理</span>
          </el-menu-item>
          <el-menu-item index="plan">
            <el-icon><Calendar /></el-icon>
            <span>巡检计划与任务</span>
          </el-menu-item>
          <el-menu-item index="change">
            <el-icon><RefreshRight /></el-icon>
            <span>分类变更台账</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main">
        <TreeManagement v-if="activeMenu === 'tree'" />
        <BenchManagement v-else-if="activeMenu === 'bench'" />
        <InspectionManagement v-else-if="activeMenu === 'inspection'" />
        <InspectionPlanManagement v-else-if="activeMenu === 'plan'" />
        <ChangeLogManagement v-else-if="activeMenu === 'change'" />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Menu, Box, RefreshRight, Tools, Calendar } from '@element-plus/icons-vue'
import TreeManagement from './components/TreeManagement.vue'
import BenchManagement from './components/BenchManagement.vue'
import InspectionManagement from './components/InspectionManagement.vue'
import InspectionPlanManagement from './components/InspectionPlanManagement.vue'
import ChangeLogManagement from './components/ChangeLogManagement.vue'

const activeMenu = ref('tree')

const handleMenuSelect = (index) => {
  activeMenu.value = index
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.app-container {
  height: 100vh;
}

.el-container {
  height: 100%;
}

.aside {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
}

.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h2 {
  font-size: 16px;
  font-weight: 600;
}

.menu {
  border-right: none;
}

.menu .el-menu-item {
  color: rgba(255, 255, 255, 0.7);
}

.menu .el-menu-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}

.menu .el-menu-item.is-active {
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
}

.main {
  padding: 20px;
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
