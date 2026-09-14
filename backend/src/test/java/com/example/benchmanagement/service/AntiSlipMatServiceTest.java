package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.AntiSlipMatIssueRequest;
import com.example.benchmanagement.dto.AntiSlipMatReturnRequest;
import com.example.benchmanagement.dto.PointAntiSlipMatDTO;
import com.example.benchmanagement.entity.AntiSlipMatRecord;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.AntiSlipMatRecordRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 防滑垫领用台账：领出/归还/破损汇总、未还数量与“未还清”标记，以及归还超额校验。
 */
@ExtendWith(MockitoExtension.class)
class AntiSlipMatServiceTest {

    @Mock
    private AntiSlipMatRecordRepository recordRepository;
    @Mock
    private TreeNodeRepository treeNodeRepository;
    @Mock
    private TreeNodeService treeNodeService;

    @InjectMocks
    private AntiSlipMatService antiSlipMatService;

    private TreeNode point(long id, long sectionId) {
        return TreeNode.builder().id(id).parentId(sectionId).level(3).name("点位" + id)
                .sortOrder((int) id).capacity(10).closed(0).build();
    }

    private TreeNode section(long id, long districtId) {
        return TreeNode.builder().id(id).parentId(districtId).level(2).name("路段" + id)
                .sortOrder((int) id).build();
    }

    private TreeNode district(long id) {
        return TreeNode.builder().id(id).level(1).name("街区" + id).sortOrder((int) id).build();
    }

    private AntiSlipMatRecord issue(long id, long pointId, int qty) {
        return AntiSlipMatRecord.builder().id(id).pointId(pointId)
                .actionType(AntiSlipMatRecord.ACTION_ISSUE).quantity(qty).damagedQuantity(0)
                .operatedAt(LocalDateTime.of(2026, 9, 14, 8, 0)).operator("张三").build();
    }

    private AntiSlipMatRecord ret(long id, long pointId, int qty, int damaged) {
        return AntiSlipMatRecord.builder().id(id).pointId(pointId)
                .actionType(AntiSlipMatRecord.ACTION_RETURN).quantity(qty).damagedQuantity(damaged)
                .operatedAt(LocalDateTime.of(2026, 9, 14, 18, 0)).operator("李四").build();
    }

    private void stubTree() {
        TreeNode district = district(10L);
        TreeNode section = section(20L, 10L);
        TreeNode pointA = point(30L, 20L);
        TreeNode pointB = point(31L, 20L);
        when(treeNodeRepository.findByLevelAndIsDeletedFalse(3)).thenReturn(List.of(pointA, pointB));
        when(treeNodeRepository.findAllActiveNodes())
                .thenReturn(List.of(district, section, pointA, pointB));
    }

    @Test
    void ledgers_aggregateIssuedReturnedDamagedAndOutstanding() {
        stubTree();
        // 点位30：领出10，归还4（其中破损1）→ 完好3、未还6，未还清
        // 点位31：领出5，归还5（无破损）→ 已还清
        when(recordRepository.findAllByOrderByOperatedAtDescIdDesc()).thenReturn(List.of(
                ret(4L, 30L, 4, 1), issue(3L, 30L, 10),
                ret(2L, 31L, 5, 0), issue(1L, 31L, 5)));

        List<PointAntiSlipMatDTO> ledgers = antiSlipMatService.getPointLedgers(null, null, null, null);

        PointAntiSlipMatDTO a = ledgers.stream().filter(l -> l.getPointId() == 30L).findFirst().orElseThrow();
        assertEquals(10, a.getIssuedCount());
        assertEquals(4, a.getReturnedCount());
        assertEquals(3, a.getIntactCount());
        assertEquals(1, a.getDamagedCount());
        assertEquals(6, a.getOutstandingCount());
        assertTrue(a.getOutstanding());

        PointAntiSlipMatDTO b = ledgers.stream().filter(l -> l.getPointId() == 31L).findFirst().orElseThrow();
        assertEquals(5, b.getIssuedCount());
        assertEquals(5, b.getReturnedCount());
        assertEquals(0, b.getOutstandingCount());
        assertFalse(b.getOutstanding());
    }

    @Test
    void outstandingFilter_onlyReturnsUnsettledPoints() {
        stubTree();
        when(recordRepository.findAllByOrderByOperatedAtDescIdDesc()).thenReturn(List.of(
                ret(2L, 30L, 4, 1), issue(1L, 30L, 10)));

        List<PointAntiSlipMatDTO> onlyOutstanding =
                antiSlipMatService.getPointLedgers(true, null, null, null);

        assertEquals(1, onlyOutstanding.size());
        assertEquals(30L, onlyOutstanding.get(0).getPointId());
        assertTrue(onlyOutstanding.get(0).getOutstanding());
    }

    @Test
    void ledgerMapByPointIds_matchesLedgerAggregation() {
        // 街区树批量填充与点位台账同源：同一套聚合结果
        when(recordRepository.findByPointIdInOrderByOperatedAtDescIdDesc(List.of(30L, 31L)))
                .thenReturn(List.of(
                        ret(2L, 30L, 4, 1), issue(1L, 30L, 10),
                        ret(4L, 31L, 5, 0), issue(3L, 31L, 5)));

        Map<Long, PointAntiSlipMatDTO> map = antiSlipMatService.getLedgerMapByPointIds(List.of(30L, 31L));

        assertEquals(6, map.get(30L).getOutstandingCount());
        assertTrue(map.get(30L).getOutstanding());
        assertEquals(0, map.get(31L).getOutstandingCount());
        assertFalse(map.get(31L).getOutstanding());
    }

    @Test
    void return_moreThanOutstanding_isRejected() {
        when(treeNodeRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(point(30L, 20L)));
        when(treeNodeService.isPointClosed(30L)).thenReturn(false);
        when(recordRepository.findByPointIdOrderByOperatedAtDescIdDesc(30L))
                .thenReturn(List.of(issue(1L, 30L, 5)));

        AntiSlipMatReturnRequest request = AntiSlipMatReturnRequest.builder()
                .pointId(30L).quantity(6).damagedQuantity(0)
                .operatedAt(LocalDateTime.now()).operator("李四").build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> antiSlipMatService.recordReturn(request));
        assertTrue(ex.getMessage().contains("未还数量5张"));
    }

    @Test
    void return_damagedGreaterThanQuantity_isRejected() {
        when(treeNodeRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(point(30L, 20L)));
        when(treeNodeService.isPointClosed(30L)).thenReturn(false);

        AntiSlipMatReturnRequest request = AntiSlipMatReturnRequest.builder()
                .pointId(30L).quantity(3).damagedQuantity(4)
                .operatedAt(LocalDateTime.now()).operator("李四").build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> antiSlipMatService.recordReturn(request));
        assertTrue(ex.getMessage().contains("破损数量不能大于本次归还数量"));
    }

    @Test
    void issue_closedPoint_isRejected() {
        when(treeNodeRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(point(30L, 20L)));
        when(treeNodeService.isPointClosed(30L)).thenReturn(true);

        AntiSlipMatIssueRequest request = AntiSlipMatIssueRequest.builder()
                .pointId(30L).quantity(5).operatedAt(LocalDateTime.now()).operator("张三").build();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> antiSlipMatService.issue(request));
        assertTrue(ex.getMessage().contains("封闭期"));
    }

    @Test
    void issue_setsDefaultsAndPersists() {
        when(treeNodeRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(point(30L, 20L)));
        when(treeNodeService.isPointClosed(30L)).thenReturn(false);
        when(treeNodeRepository.findAllActiveNodes())
                .thenReturn(List.of(district(10L), section(20L, 10L), point(30L, 20L)));
        when(recordRepository.save(any(AntiSlipMatRecord.class))).thenAnswer(inv -> {
            AntiSlipMatRecord r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });

        AntiSlipMatIssueRequest request = AntiSlipMatIssueRequest.builder()
                .pointId(30L).quantity(8).operatedAt(LocalDateTime.now())
                .operator("  ").build();

        var dto = antiSlipMatService.issue(request);
        assertEquals(99L, dto.getId());
        assertEquals(8, dto.getQuantity());
        assertEquals(AntiSlipMatRecord.ACTION_ISSUE, dto.getActionType());
        assertEquals("领出", dto.getActionTypeLabel());
        // 经办人为空时兜底 system
        assertEquals("system", dto.getOperator());
    }
}
