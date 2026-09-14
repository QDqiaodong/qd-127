import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 街区树接口：第一次返回挂着“待加凳5·逾期1”的旧树，核销事件后应被再次拉取
const getTreeMock = vi.fn()
const getPlansMock = vi.fn()
const verifyMock = vi.fn()

vi.mock('../src/api/tree', () => ({
  getTree: (...args) => getTreeMock(...args)
}))
vi.mock('../src/api/additionalBench', () => ({
  getAdditionalBenchPlans: (...args) => getPlansMock(...args),
  createAdditionalBenchPlan: vi.fn(),
  getAdditionalBenchPlanDetail: vi.fn(),
  verifyAdditionalBenchPlan: (...args) => verifyMock(...args)
}))

import TreeManagement from '../src/components/TreeManagement.vue'
import AdditionalBenchPlanManagement from '../src/components/AdditionalBenchPlanManagement.vue'
import { treeEvents } from '../src/api/treeEvents'

const treeWithPending = [
  {
    id: 10, level: 1, name: 'A街区', children: [
      {
        id: 20, level: 2, name: '主干道', parentId: 10, children: [],
        additionalBenchPendingCount: 5, additionalBenchOverduePlanCount: 1
      }
    ]
  }
]
const treeCleared = [
  {
    id: 10, level: 1, name: 'A街区', children: [
      {
        id: 20, level: 2, name: '主干道', parentId: 10, children: [],
        additionalBenchPendingCount: 0, additionalBenchOverduePlanCount: 0
      }
    ]
  }
]

const planRow = {
  id: 1, districtId: 10, districtName: 'A街区', sectionId: 20, sectionName: '主干道',
  planDate: '2026-09-10', benchCount: 5, verifiedCount: 0, pendingCount: 5,
  status: 1, effectiveStatus: 3, overdue: true, remark: null,
  lastVerifiedBy: null, lastVerifiedAt: null, createdAt: '2026-09-01T10:00:00', logs: []
}
const deployedRow = {
  ...planRow,
  verifiedCount: 5, pendingCount: 0, status: 2, effectiveStatus: 2, overdue: false,
  lastVerifiedBy: '张三', lastVerifiedAt: '2026-09-14T11:00:00'
}

const mountPlugins = { global: { plugins: [ElementPlus] } }

describe('节假日加凳核销后街区树标记同步', () => {
  let wrappers = []

  const mountWithPlugins = (component) => {
    const wrapper = mount(component, mountPlugins)
    wrappers.push(wrapper)
    return wrapper
  }

  beforeEach(() => {
    wrappers = []
    localStorage.clear()
    getTreeMock.mockReset()
    getPlansMock.mockReset()
    verifyMock.mockReset()
    getTreeMock.mockResolvedValue(treeWithPending)
    getPlansMock.mockResolvedValue([planRow])
    verifyMock.mockResolvedValue(deployedRow)
  })

  afterEach(() => {
    wrappers.forEach(w => w.unmount())
    wrappers = []
  })

  it('全部核销后预案页广播树变更事件，街区树重新拉取并清零待加凳、去掉逾期标', async () => {
    // 核销前树接口返回挂着“待加凳5·逾期1”的旧树；核销后后端实时口径返回清零树
    let verified = false
    getTreeMock.mockImplementation(() =>
      Promise.resolve(verified ? treeCleared : treeWithPending))
    getPlansMock.mockImplementation(() =>
      Promise.resolve(verified ? [deployedRow] : [planRow]))

    // 树页面挂载时先拿到旧树：路段挂着 待加凳5·逾期1
    const treeWrapper = mountWithPlugins(TreeManagement)
    await flushPromises()
    expect(treeWrapper.text()).toContain('待加凳 5')
    expect(treeWrapper.text()).toContain('逾期1')
    expect(treeWrapper.text()).toContain('加凳逾期 1 处')

    // 在加凳预案页对该预案做最后一批核销（5 张全部核销）
    const planWrapper = mountWithPlugins(AdditionalBenchPlanManagement)
    await flushPromises()
    await flushPromises()

    // 找到“核销”按钮并打开弹窗
    const verifyEntry = planWrapper.findAll('button').find(b => b.text().includes('核销'))
    expect(verifyEntry).toBeTruthy()
    await verifyEntry.trigger('click')
    await flushPromises()

    // 填写本次投放数量与操作人
    const numberInput = planWrapper.find('.el-input-number input')
    await numberInput.setValue('5')
    const operatorInput = planWrapper.findAll('input')
      .find(i => i.attributes('placeholder') === '请输入核销操作人')
    await operatorInput.setValue('张三')

    const confirmBtn = planWrapper.findAll('button').find(b => b.text().includes('确认核销'))
    await confirmBtn.trigger('click')
    verified = true
    await flushPromises()
    await flushPromises()

    expect(verifyMock).toHaveBeenCalledWith(1, expect.objectContaining({ verifiedCount: 5 }))

    // 核销后预案列表状态变为已投放、待投放为0
    expect(planWrapper.text()).toContain('已投放')

    // 街区树监听到事件后重新拉取，待加凳清零、逾期标消失
    expect(treeWrapper.text()).not.toContain('待加凳 5')
    expect(treeWrapper.text()).not.toContain('逾期1')
    expect(treeWrapper.text()).not.toContain('加凳逾期')
  })

  it('部分核销后树上待加凳数量应与剩余待投放一致', async () => {
    const partialTree = [
      {
        id: 10, level: 1, name: 'A街区', children: [
          {
            id: 20, level: 2, name: '主干道', parentId: 10, children: [],
            additionalBenchPendingCount: 3, additionalBenchOverduePlanCount: 1
          }
        ]
      }
    ]
    // 初始旧树为待加凳5，核销2张后后端返回剩余3
    const treeResponses = [treeWithPending, partialTree]
    getTreeMock.mockImplementation(() => Promise.resolve(treeResponses.shift()))

    const treeWrapper = mountWithPlugins(TreeManagement)
    await flushPromises()
    expect(treeWrapper.text()).toContain('待加凳 5')
    expect(getTreeMock).toHaveBeenCalledTimes(1)

    treeEvents.emitTreeChanged()
    await flushPromises()

    expect(getTreeMock).toHaveBeenCalledTimes(2)
    expect(treeWrapper.text()).toContain('待加凳 3')
    // 计划日期仍过期且未投放完，逾期标保留
    expect(treeWrapper.text()).toContain('逾期1')
  })
})
