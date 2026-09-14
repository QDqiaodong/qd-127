import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

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

const EXPIRING_ROW = {
  id: 2, pointId: 30, pointName: '广场前', sectionId: 20, sectionName: '主干道',
  districtId: 10, districtName: 'A街区', merchantName: '李四小店',
  sponsorshipText: '李四小店陪你等', startDate: '2026-08-01', endDate: '2026-09-15',
  effectiveStatus: 4, expired: false, expiringSoon: true, daysRemaining: 2,
  expiringSoonDays: 3, createdAt: '2026-09-01T10:00:00'
}

const PENDING_ROW = {
  id: 1, pointId: 30, pointName: '广场前', sectionId: 20, sectionName: '主干道',
  districtId: 10, districtName: 'A街区', merchantName: '张三奶茶',
  sponsorshipText: '张三奶茶请你歇脚', startDate: '2099-01-01', endDate: '2099-12-31',
  effectiveStatus: 1, expired: false, createdAt: '2026-09-01T10:00:00'
}

const mountWithRestoredFilter = () => {
  localStorage.setItem('bench-sponsorship-filters',
    JSON.stringify({ districtId: null, sectionId: null, pointId: null, status: 1 }))
  return mount(BenchSponsorshipManagement, {
    global: { plugins: [ElementPlus] }
  })
}

describe('商户冠名台账 - 重进时的首屏空态问题', () => {
  beforeEach(() => {
    localStorage.clear()
    getBenchSponsorshipsMock.mockReset()
    getTreeMock.mockReset()
    getBenchSponsorshipsMock.mockImplementation((params) => {
      const rows = [PENDING_ROW, EXPIRING_ROW]
      if (!params.status) return Promise.resolve(rows)
      if (params.status === 4) return Promise.resolve(rows.filter(r => r.expiringSoon))
      return Promise.resolve(rows.filter(r => r.effectiveStatus === params.status))
    })
  })

  it('树接口较慢时：加载期间不应先显示“没有待生效记录”空态', async () => {
    let resolveTree
    getTreeMock.mockReturnValue(new Promise(resolve => { resolveTree = resolve }))

    const wrapper = mountWithRestoredFilter()
    await wrapper.vm.$nextTick()

    // 树还没回来，表格此时显示了什么？
    const showsEmptyTip = wrapper.text().includes('暂无待生效的冠名记录')
    const showsLoadingMask = wrapper.find('.el-loading-mask').exists()
    console.log('加载中空态文案在DOM中:', showsEmptyTip, '| 有加载遮罩:', showsLoadingMask)

    // 期望：加载期间有加载遮罩盖住表格，而不是裸露出“没有待生效记录”空态
    expect(showsLoadingMask).toBe(true)

    resolveTree(TREE)
    await flushPromises()
    expect(wrapper.text()).toContain('张三奶茶')
  })

  it('按即将到期筛选时：请求 status=4，并展示剩余天数和到期日', async () => {
    getTreeMock.mockResolvedValue(TREE)
    localStorage.setItem('bench-sponsorship-filters',
      JSON.stringify({ districtId: null, sectionId: null, pointId: null, status: 4 }))

    const wrapper = mount(BenchSponsorshipManagement, {
      global: { plugins: [ElementPlus] }
    })
    await flushPromises()
    await flushPromises()

    expect(getBenchSponsorshipsMock.mock.calls[0][0]).toMatchObject({ status: 4 })
    expect(wrapper.text()).toContain('李四小店')
    expect(wrapper.text()).toContain('即将到期·剩2天')
    expect(wrapper.text()).not.toContain('张三奶茶')

    await wrapper.find('.el-table__row').trigger('click')
    expect(wrapper.text()).toContain('剩2天，2026-09-15 到期')
  })

  it('树接口失败/超时时：台账列表仍应加载出来，而不是一直空着等点查询', async () => {
    getTreeMock.mockRejectedValue(new Error('网络异常，请稍后重试'))

    const wrapper = mountWithRestoredFilter()
    await flushPromises()
    await flushPromises()

    console.log('树失败时 getBenchSponsorships 调用次数:', getBenchSponsorshipsMock.mock.calls.length)
    // 期望：树挂了不影响台账列表，待生效行仍直接展示
    expect(getBenchSponsorshipsMock).toHaveBeenCalled()
    expect(wrapper.text()).toContain('张三奶茶')
  })

  it('恢复的街区筛选已被删除时：清掉悬空项后用纠正后的条件重查列表', async () => {
    getTreeMock.mockResolvedValue(TREE)
    localStorage.setItem('bench-sponsorship-filters',
      JSON.stringify({ districtId: 999, sectionId: null, pointId: null, status: 1 }))

    const wrapper = mount(BenchSponsorshipManagement, {
      global: { plugins: [ElementPlus] }
    })
    await flushPromises()
    await flushPromises()

    // 第一次可能带着已失效的 districtId 查询，清掉后应再查一次且不带 districtId
    const calls = getBenchSponsorshipsMock.mock.calls
    console.log('调用次数:', calls.length, '| 参数:', JSON.stringify(calls))
    expect(calls.length).toBeGreaterThanOrEqual(2)
    expect(calls[calls.length - 1][0].districtId).toBeUndefined()
    expect(calls[calls.length - 1][0].status).toBe(1)
    expect(wrapper.text()).toContain('张三奶茶')
  })
})
