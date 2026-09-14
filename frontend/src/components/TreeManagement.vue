<template>
  <div class="tree-management">
    <el-card>
      <template #header>
        <div class="header">
          <h3>街区树形分类管理</h3>
          <div class="header-right">
            <el-tag
              v-if="capacityAlarmSectionCount > 0"
              type="warning"
              effect="dark"
              class="alarm-count-tag"
            >
              容量告警 {{ capacityAlarmSectionCount }} 处
            </el-tag>
            <el-tag
              v-if="additionalBenchOverdueSectionCount > 0"
              type="danger"
              effect="dark"
              class="alarm-count-tag"
            >
              加凳逾期 {{ additionalBenchOverdueSectionCount }} 处
            </el-tag>
            <el-tag
              v-if="sponsorshipExpiringSectionCount > 0"
              type="danger"
              effect="plain"
              class="alarm-count-tag"
            >
              冠名临期 {{ sponsorshipExpiringSectionCount }} 处
            </el-tag>
            <el-tag
              v-if="antiSlipMatOutstandingSectionCount > 0"
              type="danger"
              effect="dark"
              class="alarm-count-tag"
            >
              防滑垫未还 {{ antiSlipMatOutstandingSectionCount }} 处
            </el-tag>
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              添加节点
            </el-button>
          </div>
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
                v-if="data.level === 2 && data.lightingAbnormalCount > 0"
                size="small"
                type="danger"
                effect="plain"
                class="capacity-tag section-lighting-tag"
                @click.stop="openSectionLightingDetail(data)"
              >
                照明异常 {{ data.lightingAbnormalCount }} 处
              </el-tag>
              <el-tag
                v-if="data.level === 2 && data.capacityAlarm"
                size="small"
                type="warning"
                effect="plain"
                class="capacity-tag section-capacity-tag"
                @click.stop="openSectionCapacityAlarm(data)"
              >
                容量告警·余{{ data.capacityRemainingSum }}
              </el-tag>
              <el-tag
                v-if="data.level === 2 && data.additionalBenchPendingCount > 0"
                size="small"
                :type="data.additionalBenchOverduePlanCount > 0 ? 'danger' : 'warning'"
                effect="plain"
                class="capacity-tag additional-bench-tag"
              >
                待加凳 {{ data.additionalBenchPendingCount }}
                <template v-if="data.additionalBenchOverduePlanCount > 0">
                  ·逾期{{ data.additionalBenchOverduePlanCount }}
                </template>
              </el-tag>
              <el-tag
                v-if="data.level === 2 && data.sponsorshipExpiringSectionCount > 0"
                size="small"
                type="danger"
                effect="plain"
                class="capacity-tag"
              >
                冠名临期 {{ data.sponsorshipExpiringSectionCount }}
              </el-tag>
              <el-tooltip
                v-if="data.level === 2 && data.antiSlipMatOutstandingPointCount > 0"
                effect="dark"
                placement="top"
              >
                <template #content>
                  <div>{{ data.antiSlipMatOutstandingPointCount }} 个点位未还清，共 {{ data.antiSlipMatOutstandingMatCount }} 张未还</div>
                </template>
                <el-tag
                  size="small"
                  type="danger"
                  effect="plain"
                  class="capacity-tag section-anti-slip-tag"
                  @click.stop="openSectionAntiSlipDetail(data)"
                >
                  防滑垫未还 {{ data.antiSlipMatOutstandingMatCount }}
                </el-tag>
              </el-tooltip>
              <el-tooltip
                v-if="data.level === 3 && data.sponsorshipExpiringCount > 0"
                effect="dark"
                placement="top"
              >
                <template #content>
                  <div>{{ sponsorshipTooltip(data) }}</div>
                </template>
                <el-tag size="small" type="danger" effect="plain" class="capacity-tag">
                  冠名临期·{{ sponsorshipRemainingText(data) }}
                </el-tag>
              </el-tooltip>
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
              <el-tooltip
                v-if="data.level === 3 && data.closed"
                effect="dark"
                placement="top"
              >
                <template #content>
                  <div>封闭期：{{ closurePeriodText(data) }}</div>
                  <div>封闭原因：{{ data.closedReason || '未填写' }}</div>
                </template>
                <el-tag
                  size="small"
                  type="danger"
                  class="capacity-tag"
                >
                  封闭中（{{ formatDateTimeMinute(data.closedEndAt) }} 止）
                </el-tag>
              </el-tooltip>
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
              <el-tooltip
                v-if="data.level === 3 && data.antiSlipMatOutstandingCount > 0"
                effect="dark"
                placement="top"
              >
                <template #content>
                  <div>领出 {{ data.antiSlipMatIssuedCount }} / 归还 {{ data.antiSlipMatReturnedCount }}（破损 {{ data.antiSlipMatDamagedCount }}），仍有 {{ data.antiSlipMatOutstandingCount }} 张未还</div>
                </template>
                <el-tag size="small" type="danger" effect="plain" class="capacity-tag">
                  防滑垫未还 {{ data.antiSlipMatOutstandingCount }}
                </el-tag>
              </el-tooltip>
              <el-tag
                v-else-if="data.level === 3 && data.antiSlipMatIssuedCount > 0"
                size="small"
                type="success"
                effect="plain"
                class="capacity-tag"
              >
                防滑垫已还清
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

    <el-dialog
      v-model="sectionLightingVisible"
      :title="`路段照明异常点位 - ${sectionLightingNode ? sectionLightingNode.name : ''}`"
      width="640px"
    >
      <el-table v-loading="sectionLightingLoading" :data="sectionLightingPoints" border size="small" max-height="420">
        <el-table-column prop="pointName" label="点位" />
        <el-table-column label="异常类型" width="100">
          <template #default="{ row }">
            <el-tag type="danger" size="small" effect="plain">{{ row.problemType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="灯具数量" width="90">
          <template #default="{ row }">{{ row.lampCount ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="巡查人" width="100">
          <template #default="{ row }">{{ row.inspector || '-' }}</template>
        </el-table-column>
        <el-table-column label="最近巡查时间" width="170">
          <template #default="{ row }">{{ row.latestInspectedAt || '-' }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="该路段暂无照明异常点位" :image-size="60" />
        </template>
      </el-table>
    </el-dialog>

    <el-dialog
      v-model="sectionAntiSlipVisible"
      :title="`防滑垫未还清点位 - ${sectionAntiSlipNode ? sectionAntiSlipNode.name : ''}`"
      width="720px"
    >
      <el-alert
        v-if="sectionAntiSlipNode"
        type="error"
        :closable="false"
        show-icon
        class="alarm-summary"
        :title="`${sectionAntiSlipNode.antiSlipMatOutstandingPointCount} 个点位未还清，未还防滑垫合计 ${sectionAntiSlipNode.antiSlipMatOutstandingMatCount} 张`"
      />
      <el-table v-loading="sectionAntiSlipLoading" :data="sectionAntiSlipPoints" border size="small" max-height="420">
        <el-table-column prop="pointName" label="点位" />
        <el-table-column prop="issuedCount" label="领出" width="80" align="center" />
        <el-table-column label="归还（完好）" width="120" align="center">
          <template #default="{ row }">{{ row.returnedCount }}（{{ row.intactCount }}）</template>
        </el-table-column>
        <el-table-column prop="damagedCount" label="破损" width="80" align="center" />
        <el-table-column label="未还" width="80" align="center">
          <template #default="{ row }">
            <el-tag type="danger" size="small">{{ row.outstandingCount }}</el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="该路段暂无防滑垫未还清点位" :image-size="60" />
        </template>
      </el-table>
    </el-dialog>

    <el-dialog
      v-model="sectionCapacityVisible"
      :title="`路段容量告警 - ${sectionCapacityDetail ? sectionCapacityDetail.sectionName : ''}`"
      width="640px"
    >
      <div v-loading="sectionCapacityLoading">
        <el-alert
          v-if="sectionCapacityDetail"
          type="warning"
          :closable="false"
          show-icon
          class="alarm-summary"
        >
          <template #title>
            剩余容量加总 {{ sectionCapacityDetail.remainingSum }}，低于阈值
            {{ sectionCapacityDetail.threshold }}（已满 {{ sectionCapacityDetail.fullCount }} 处，将满
            {{ sectionCapacityDetail.nearlyFullCount }} 处）
          </template>
        </el-alert>
        <el-table :data="sectionCapacityDetail ? sectionCapacityDetail.points : []" border size="small" max-height="420">
          <el-table-column prop="name" label="点位" />
          <el-table-column label="占用/上限" width="110">
            <template #default="{ row }">{{ row.occupiedCount ?? 0 }}/{{ row.capacity }}</template>
          </el-table-column>
          <el-table-column label="剩余" width="80">
            <template #default="{ row }">{{ row.remainingCount ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="容量状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="capacityStatusTagType(row.capacityStatus)" effect="plain">
                {{ capacityStatusLabel(row.capacityStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="该路段暂无已满或将满点位" :image-size="60" />
          </template>
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="capacityLogVisible" :title="`容量调整记录 - ${capacityLogNodeName}`" width="640px"><el-table :data="capacityLogs" border size="small">
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

    <el-dialog v-model="closeDialogVisible" :title="`临时封闭点位 - ${closeTargetNode ? closeTargetNode.name : ''}`" width="520px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        class="closure-alert"
        title="封闭期内禁止调入长凳（新增、编辑移入、批量调入均会被阻止），且不能以该点位发起巡检；到期自动解封，也可人工解封"
      />
      <el-form :model="closeForm" label-width="90px">
        <el-form-item label="开始时间" required>
          <el-date-picker
            v-model="closeForm.startAt"
            type="datetime"
            placeholder="选择封闭开始时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-date-picker
            v-model="closeForm.endAt"
            type="datetime"
            placeholder="选择封闭结束时间（到期自动解封）"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="封闭原因" required>
          <el-input
            v-model="closeForm.reason"
            type="textarea"
            :rows="2"
            placeholder="请填写封闭原因，将展示在树和资产列表并记入台账"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="closeSubmitting" @click="confirmClosePoint">确认封闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="closureLogVisible"
      :title="`封闭/解封记录 - ${closureLogNodeName}`"
      width="760px"
    >
      <el-table :data="closureLogs" border size="small" max-height="420">
        <el-table-column prop="operatedAt" label="操作时间" width="165">
          <template #default="{ row }">{{ formatDateTimeMinute(row.operatedAt) }}</template>
        </el-table-column>
        <el-table-column label="动作" width="100">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.actionType === 1 ? 'danger' : 'success'"
              effect="plain"
            >
              {{ row.actionTypeLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="封闭期" width="260">
          <template #default="{ row }">
            {{ formatDateTimeMinute(row.closedStartAt) }} 至 {{ formatDateTimeMinute(row.closedEndAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="closedReason" label="封闭原因" show-overflow-tooltip>
          <template #default="{ row }">{{ row.closedReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="reopenReason" label="解封原因" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reopenReason || '-' }}</template>
        </el-table-column>
        <el-table-column prop="operatedBy" label="操作人" width="90" />
        <template #empty>
          <el-empty description="该点位暂无封闭/解封记录" :image-size="60" />
        </template>
      </el-table>
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
      <el-menu-item
        v-if="rightClickedNode && rightClickedNode.level === 3 && !rightClickedNode.closed"
        @click="handleClosePoint"
      >
        <el-icon><Lock /></el-icon>
        临时封闭
      </el-menu-item>
      <el-menu-item
        v-if="rightClickedNode && rightClickedNode.level === 3 && rightClickedNode.closed"
        @click="handleReopenPoint"
        style="color: #67c23a"
      >
        <el-icon><Unlock /></el-icon>
        人工解封
      </el-menu-item>
      <el-menu-item
        v-if="rightClickedNode && rightClickedNode.level === 3"
        @click="handleClosureLogs"
      >
        <el-icon><Document /></el-icon>
        封闭/解封记录
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
import { Plus, Edit, Delete, Download, Location, Grid, CirclePlus, Tickets, Lock, Unlock, Document } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTree, createNode, updateNode, deleteNode, adjustCapacity, getCapacityLogs, getSectionCapacityAlarm, closePoint, reopenPoint, getClosureLogs } from '../api/tree'
import { getBenchesByNode, exportAssets } from '../api/bench'
import { getPointLightingStatuses } from '../api/lighting'
import { getAntiSlipMatLedgers } from '../api/antiSlipMat'
import { capacityStatusLabel, capacityStatusTagType } from '../constants/capacity'
import { formatDateTimeMinute, closurePeriodText } from '../constants/closure'

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

// 路段照明异常下钻
const sectionLightingVisible = ref(false)
const sectionLightingNode = ref(null)
const sectionLightingPoints = ref([])
const sectionLightingLoading = ref(false)

// 点开路段异常标记，下钻到该路段的异常点位（与点位照明状态同源）
const openSectionLightingDetail = async (node) => {
  sectionLightingNode.value = node
  sectionLightingPoints.value = []
  sectionLightingVisible.value = true
  sectionLightingLoading.value = true
  try {
    sectionLightingPoints.value = await getPointLightingStatuses({ sectionId: node.id, result: 0 })
  } catch (error) {
    ElMessage.error(error.message || '加载路段照明异常点位失败')
  } finally {
    sectionLightingLoading.value = false
  }
}

// 路段容量告警下钻
const sectionCapacityVisible = ref(false)
const sectionCapacityDetail = ref(null)
const sectionCapacityLoading = ref(false)

// 点开路段容量告警标记，下钻到该路段已满/将满点位（与树上告警标记同源）
const openSectionCapacityAlarm = async (node) => {
  sectionCapacityDetail.value = null
  sectionCapacityVisible.value = true
  sectionCapacityLoading.value = true
  try {
    sectionCapacityDetail.value = await getSectionCapacityAlarm(node.id)
  } catch (error) {
    ElMessage.error(error.message || '加载路段容量告警详情失败')
  } finally {
    sectionCapacityLoading.value = false
  }
}

// 容量告警条数：直接统计树上标记为告警的路段，与树标记同源，刷新后保持一致
const capacityAlarmSectionCount = computed(() => {
  let count = 0
  const walk = (nodes) => {
    nodes.forEach(node => {
      if (node.level === 2 && node.capacityAlarm) count++
      if (node.children && node.children.length > 0) walk(node.children)
    })
  }
  walk(treeData.value)
  return count
})

// 有加凳逾期预案的路段数：直接统计街区树返回字段，和路段待加凳标记同源
const additionalBenchOverdueSectionCount = computed(() => {
  let count = 0
  const walk = (nodes) => {
    nodes.forEach(node => {
      if (node.level === 2 && node.additionalBenchOverduePlanCount > 0) count++
      if (node.children && node.children.length > 0) walk(node.children)
    })
  }
  walk(treeData.value)
  return count
})

// 有冠名临期记录的路段数：直接统计街区树返回字段，和路段临期标记同源
const sponsorshipExpiringSectionCount = computed(() => {
  let count = 0
  const walk = (nodes) => {
    nodes.forEach(node => {
      if (node.level === 2 && node.sponsorshipExpiringSectionCount > 0) count++
      if (node.children && node.children.length > 0) walk(node.children)
    })
  }
  walk(treeData.value)
  return count
})

// 防滑垫未还清的路段数：直接统计街区树返回字段，与路段/点位未还标记同源，刷新后保持一致
const antiSlipMatOutstandingSectionCount = computed(() => {
  let count = 0
  const walk = (nodes) => {
    nodes.forEach(node => {
      if (node.level === 2 && node.antiSlipMatOutstandingPointCount > 0) count++
      if (node.children && node.children.length > 0) walk(node.children)
    })
  }
  walk(treeData.value)
  return count
})

// 路段防滑垫未还清点位下钻
const sectionAntiSlipVisible = ref(false)
const sectionAntiSlipNode = ref(null)
const sectionAntiSlipPoints = ref([])
const sectionAntiSlipLoading = ref(false)

// 点开路段防滑垫未还标记，下钻到该路段未还清点位（与点位领用台账同源）
const openSectionAntiSlipDetail = async (node) => {
  sectionAntiSlipNode.value = node
  sectionAntiSlipPoints.value = []
  sectionAntiSlipVisible.value = true
  sectionAntiSlipLoading.value = true
  try {
    sectionAntiSlipPoints.value = await getAntiSlipMatLedgers({ sectionId: node.id, outstanding: true })
  } catch (error) {
    ElMessage.error(error.message || '加载路段防滑垫未还清点位失败')
  } finally {
    sectionAntiSlipLoading.value = false
  }
}

const sponsorshipRemainingText = (point) => {
  const days = Number(point.sponsorshipNearestDaysRemaining)
  return days === 0 ? '今日到期' : `剩${days}天`
}

const sponsorshipTooltip = (point) => {
  const merchant = point.sponsorshipNearestMerchantName || '冠名商户'
  const text = point.sponsorshipNearestText ? `｜${point.sponsorshipNearestText}` : ''
  const countPrefix = point.sponsorshipExpiringCount > 1
    ? `${point.sponsorshipExpiringCount}条冠名即将到期，最早：`
    : ''
  return `${countPrefix}${merchant}冠名${text}，${point.sponsorshipNearestEndDate} 到期（${sponsorshipRemainingText(point)}）`
}

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

// 点位容量标记颜色：优先取后端 capacityStatus（与路段告警、下钻明细同源），无则按剩余数兜底
const capacityTagType = (point) => {
  if (!point) return 'info'
  if (point.capacityStatus) return capacityStatusTagType(point.capacityStatus)
  if (point.remainingCount === undefined || point.remainingCount === null) return 'info'
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

// 临时封闭点位
const closeDialogVisible = ref(false)
const closeSubmitting = ref(false)
const closeTargetNode = ref(null)
const closeForm = ref({
  startAt: '',
  endAt: '',
  reason: ''
})

const buildDefaultCloseRange = () => {
  const pad = (n) => String(n).padStart(2, '0')
  const format = (d) =>
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  const start = new Date()
  const end = new Date(start.getTime() + 7 * 24 * 60 * 60 * 1000)
  return { startAt: format(start), endAt: format(end) }
}

const handleClosePoint = () => {
  if (!rightClickedNode.value || rightClickedNode.value.level !== 3) return
  closeTargetNode.value = rightClickedNode.value
  closeForm.value = { ...buildDefaultCloseRange(), reason: '' }
  menuVisible.value = false
  closeDialogVisible.value = true
}

const confirmClosePoint = async () => {
  const node = closeTargetNode.value
  if (!node) return
  if (!closeForm.value.startAt || !closeForm.value.endAt) {
    ElMessage.warning('请选择封闭开始时间和结束时间')
    return
  }
  if (new Date(closeForm.value.endAt) <= new Date(closeForm.value.startAt)) {
    ElMessage.warning('封闭结束时间必须晚于开始时间')
    return
  }
  if (!closeForm.value.reason || !closeForm.value.reason.trim()) {
    ElMessage.warning('请填写封闭原因')
    return
  }
  closeSubmitting.value = true
  try {
    await closePoint(node.id, {
      startAt: closeForm.value.startAt,
      endAt: closeForm.value.endAt,
      reason: closeForm.value.reason.trim()
    })
    ElMessage.success(`点位【${node.name}】已临时封闭，封闭期内禁止调入长凳`)
    closeDialogVisible.value = false
    await loadTree()
  } catch (error) {
    ElMessage.error(error.message || '封闭失败')
  } finally {
    closeSubmitting.value = false
  }
}

// 人工解封
const handleReopenPoint = async () => {
  if (!rightClickedNode.value || rightClickedNode.value.level !== 3) return
  const node = rightClickedNode.value
  menuVisible.value = false
  try {
    const { value } = await ElMessageBox.prompt('请填写解封原因（将记入封闭/解封台账）', `人工解封点位 - ${node.name}`, {
      confirmButtonText: '确认解封',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '例如：施工提前完成，恢复使用',
      inputValidator: (val) => !!(val && val.trim()) || '解封原因不能为空'
    })
    await reopenPoint(node.id, { reason: value.trim() })
    ElMessage.success(`点位【${node.name}】已解封，可正常调入长凳`)
    await loadTree()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '解封失败')
    }
  }
}

// 封闭/解封台账
const closureLogVisible = ref(false)
const closureLogs = ref([])
const closureLogNodeName = ref('')

const handleClosureLogs = async () => {
  if (!rightClickedNode.value || rightClickedNode.value.level !== 3) return
  const node = rightClickedNode.value
  menuVisible.value = false
  try {
    closureLogNodeName.value = node.name
    closureLogs.value = await getClosureLogs(node.id)
    closureLogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载封闭/解封记录失败')
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

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.alarm-count-tag {
  font-weight: 600;
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

.section-lighting-tag {
  cursor: pointer;
}

.section-anti-slip-tag {
  cursor: pointer;
}

.section-capacity-tag {
  cursor: pointer;
}

.alarm-summary {
  margin-bottom: 12px;
}

.closure-alert {
  margin-bottom: 14px;
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
