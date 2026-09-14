import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 记录防滑垫台账接口收到的查询参数
const getLedgersMock = vi.fn()
const getTreeMock = vi.fn()

vi.mock('../src/api/antiSlipMat', () => ({
  getAntiSlipMatLedgers: (...args) => getLedgersMock(...args),
  issueAntiSlipMat: vi.fn(),
  returnAntiSlipMat: vi.fn(),
  getAntiSlipMatRecords: vi.fn().mockResolvedValue([]),
  getAntiSlipMatRecordsByPoint: vi.fn().mockResolvedValue([])
}))
vi.mock('../src/api/tree', () => ({
  getTree: (...args) => getTreeMock(...args)
}))

import AntiSlipMatManagement from '../src/components/AntiSlipMatManagement.vue'

const TREE = [
  {
    id: 10, level: 1, name: 'A街区', children: [
      { id: 20, level: 2, name: '主干道', parentId: 10, children: [
        { id: 30, level: 3, name: '广场前', parentId: 20, children: [], closed: false },
        { id: 31, level: 3, name: '商店旁', parentId: 20, children: [], closed: false }
      ] }
    ]
  }
]

const OUTSTANDING_ROW = {
  pointId: 30, pointName: '广场前', sectionId: 20, sectionName: '主干道',
  districtId: 10, districtName: 'A街区', issuedCount: 10, returnedCount: 4,
  intactCount: 3, damagedCount: 1, outstandingCount: 6, outstanding: true,
  hasRecord: true, lastOperatedAt: '2026-09-14T09:00:00'
}
const SETTLED_ROW = {
  pointId: 31, pointName: '商店旁', sectionId: 20, sectionName: '主干道',
  districtId: 10, districtName: 'A街区', issuedCount: 5, returnedCount: 5,
  intactCount: 5, damagedCount: 0, outstandingCount: 0, outstanding: false,
  hasRecord: true, lastOperatedAt: '2026-09-14T10:00:00'
}

describe('防滑垫领用台账 - 只看未还清筛选持久化后重进', () => {
  beforeEach(() => {
    localStorage.clear()
    getLedgersMock.mockReset()
    getTreeMock.mockReset()
    getTreeMock.mockResolvedValue(TREE)
    // 模拟后端：outstanding=true 时只返回未还清点位
    getLedgersMock.mockImplementation((params) => {
      const rows = [OUTSTANDING_ROW, SETTLED_ROW]
      const filtered = params.outstanding ? rows.filter(r => r.outstanding) : rows
      return Promise.resolve(filtered)
    })
  })

  it('勾上“只看未还清”后关掉页面再打开，首屏应带 outstanding=true 且只展示未还清点位', async () => {
    // 上次勾选了“只看未还清”后关掉页面：localStorage 留下筛选条件
    localStorage.setItem('anti-slip-mat-filters',
      JSON.stringify({ districtId: null, sectionId: null, pointId: null, outstanding: true }))

    const wrapper = mount(AntiSlipMatManagement, {
      global: { plugins: [ElementPlus] }
    })
    await flushPromises()
    await flushPromises()

    // 首屏请求应带上恢复出来的 outstanding=true
    expect(getLedgersMock).toHaveBeenCalled()
    expect(getLedgersMock.mock.calls[0][0]).toMatchObject({ outstanding: true })

    // 表格应只展示未还清行
    expect(wrapper.text()).toContain('广场前')
    expect(wrapper.text()).not.toContain('商店旁')
    expect(wrapper.text()).toContain('未还清')
  })

  it('未还清点位行应展示领出与未还数量，合计只统计未还清', async () => {
    localStorage.setItem('anti-slip-mat-filters',
      JSON.stringify({ districtId: null, sectionId: null, pointId: null, outstanding: true }))

    const wrapper = mount(AntiSlipMatManagement, {
      global: { plugins: [ElementPlus] }
    })
    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('10')
    expect(wrapper.text()).toContain('6')
    // 汇总：1 个点位未还清，未还合计 6 张
    expect(wrapper.text()).toContain('未还防滑垫合计')
    expect(wrapper.text()).toMatch(/未还防滑垫合计[\s\S]*?6\s*张/)
  })
})
