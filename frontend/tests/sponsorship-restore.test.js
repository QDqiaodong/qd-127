import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 记录冠名台账接口收到的查询参数
const getBenchSponsorshipsMock = vi.fn()
const getTreeMock = vi.fn()

vi.mock('../src/api/benchSponsorship', () => ({
  getBenchSponsorships: (...args) => getBenchSponsorshipsMock(...args),
  createBenchSponsorship: vi.fn()
}))
vi.mock('../src/api/tree', () => ({
  getTree: (...args) => getTreeMock(...args)
}))

import BenchSponsorshipManagement from '../src/components/BenchSponsorshipManagement.vue'

const TREE = [
  {
    id: 10, level: 1, name: 'A街区', children: [
      { id: 20, level: 2, name: '主干道', parentId: 10, children: [
        { id: 30, level: 3, name: '广场前', parentId: 20, children: [] }
      ] }
    ]
  }
]

const PENDING_ROW = {
  id: 1, pointId: 30, pointName: '广场前', sectionId: 20, sectionName: '主干道',
  districtId: 10, districtName: 'A街区', merchantName: '张三奶茶',
  sponsorshipText: '张三奶茶请你歇脚', startDate: '2099-01-01', endDate: '2099-12-31',
  effectiveStatus: 1, expired: false, createdAt: '2026-09-01T10:00:00'
}

describe('商户冠名台账 - 筛选持久化后重进', () => {
  beforeEach(() => {
    localStorage.clear()
    getBenchSponsorshipsMock.mockReset()
    getTreeMock.mockReset()
    getTreeMock.mockResolvedValue(TREE)
    // 模拟后端：按 status 过滤
    getBenchSponsorshipsMock.mockImplementation((params) => {
      const rows = [PENDING_ROW]
      const filtered = params.status ? rows.filter(r => r.effectiveStatus === params.status) : rows
      return Promise.resolve(filtered)
    })
  })

  it('筛成待生效后关掉页面再打开，首屏应直接请求并展示待生效记录', async () => {
    // 上次筛了“待生效”后关掉页面：localStorage 里留下筛选条件
    localStorage.setItem('bench-sponsorship-filters',
      JSON.stringify({ districtId: null, sectionId: null, pointId: null, status: 1 }))

    const wrapper = mount(BenchSponsorshipManagement, {
      global: { plugins: [ElementPlus] }
    })
    await flushPromises()
    await flushPromises()

    // 首屏请求应带上恢复出来的 status=1
    console.log('getBenchSponsorships 调用次数:', getBenchSponsorshipsMock.mock.calls.length)
    console.log('getBenchSponsorships 收到参数:', JSON.stringify(getBenchSponsorshipsMock.mock.calls))
    expect(getBenchSponsorshipsMock).toHaveBeenCalled()
    expect(getBenchSponsorshipsMock.mock.calls[0][0]).toMatchObject({ status: 1 })

    // 表格应直接展示待生效行，而不是空提示
    console.log('表格文本:', wrapper.find('.el-table').text())
    expect(wrapper.text()).toContain('张三奶茶')
    expect(wrapper.text()).not.toContain('暂无待生效的冠名记录')
  })
})
