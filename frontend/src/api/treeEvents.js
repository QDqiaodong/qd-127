// 全局树数据变更事件总线：
// 加凳预案核销/新建、防滑垫领用归还等会改动街区树标记的操作完成后广播 'tree-data-changed'，
// 街区树页面监听到后重新拉取整棵树，保证列表/台账与树上标记在同一次会话内即时一致；
// 关掉页面再打开时树由后端按数据库实时重算，仍然一致。
const listeners = new Set()

export const treeEvents = {
  onChange(listener) {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
  emitTreeChanged() {
    listeners.forEach(listener => {
      try {
        listener()
      } catch (error) {
        // 单个监听者异常不影响其他监听者刷新
        console.error('树变更事件监听执行失败', error)
      }
    })
  }
}
