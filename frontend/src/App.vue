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
          <el-menu-item index="lighting">
            <el-icon><Moon /></el-icon>
            <span>夜间照明巡查</span>
          </el-menu-item>
          <el-menu-item index="plan">
            <el-icon><Calendar /></el-icon>
            <span>巡检计划与任务</span>
          </el-menu-item>
          <el-menu-item index="additional-bench">
            <el-icon><Flag /></el-icon>
            <span>节假日加凳预案</span>
          </el-menu-item>
          <el-menu-item index="sponsorship">
            <el-icon><GoldMedal /></el-icon>
            <span>商户冠名台账</span>
          </el-menu-item>
          <el-menu-item index="anti-slip-mat">
            <el-icon><Umbrella /></el-icon>
            <span>防滑垫领用台账</span>
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
        <LightingInspectionManagement v-else-if="activeMenu === 'lighting'" />
        <InspectionPlanManagement v-else-if="activeMenu === 'plan'" />
        <AdditionalBenchPlanManagement v-else-if="activeMenu === 'additional-bench'" />
        <BenchSponsorshipManagement v-else-if="activeMenu === 'sponsorship'" />
        <AntiSlipMatManagement v-else-if="activeMenu === 'anti-slip-mat'" />
        <ChangeLogManagement v-else-if="activeMenu === 'change'" />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Menu, Box, RefreshRight, Tools, Calendar, Moon, Flag, GoldMedal, Umbrella } from '@element-plus/icons-vue'
import TreeManagement from './components/TreeManagement.vue'
import BenchManagement from './components/BenchManagement.vue'
import InspectionManagement from './components/InspectionManagement.vue'
import LightingInspectionManagement from './components/LightingInspectionManagement.vue'
import InspectionPlanManagement from './components/InspectionPlanManagement.vue'
import AdditionalBenchPlanManagement from './components/AdditionalBenchPlanManagement.vue'
import BenchSponsorshipManagement from './components/BenchSponsorshipManagement.vue'
import AntiSlipMatManagement from './components/AntiSlipMatManagement.vue'
import ChangeLogManagement from './components/ChangeLogManagement.vue'

// 记住当前菜单：关掉页面再打开时回到原页面，页面内筛选项由各自组件恢复
const MENU_STORAGE_KEY = 'bench-management-active-menu'
const MENU_KEYS = ['tree', 'bench', 'inspection', 'lighting', 'plan', 'additional-bench', 'sponsorship', 'anti-slip-mat', 'change']

const restoreActiveMenu = () => {
  const saved = localStorage.getItem(MENU_STORAGE_KEY)
  return MENU_KEYS.includes(saved) ? saved : 'tree'
}

const activeMenu = ref(restoreActiveMenu())

const handleMenuSelect = (index) => {
  activeMenu.value = index
  localStorage.setItem(MENU_STORAGE_KEY, index)
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
