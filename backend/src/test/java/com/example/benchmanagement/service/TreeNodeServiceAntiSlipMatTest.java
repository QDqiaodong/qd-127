package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.PointAntiSlipMatDTO;
import com.example.benchmanagement.dto.TreeNodeDTO;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.BenchRepository;
import com.example.benchmanagement.repository.NodeCapacityLogRepository;
import com.example.benchmanagement.repository.NodeClosureLogRepository;
import com.example.benchmanagement.repository.PointLightingInspectionRepository;
import com.example.benchmanagement.repository.PointSunshadeInspectionRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * 街区树防滑垫未还标记：点位未还数量与路段未还清点位数、未还数量加总同源。
 */
@ExtendWith(MockitoExtension.class)
class TreeNodeServiceAntiSlipMatTest {

    @Mock
    private TreeNodeRepository treeNodeRepository;
    @Mock
    private BenchRepository benchRepository;
    @Mock
    private NodeCapacityLogRepository capacityLogRepository;
    @Mock
    private NodeClosureLogRepository closureLogRepository;
    @Mock
    private PointLightingInspectionRepository lightingInspectionRepository;
    @Mock
    private PointSunshadeInspectionRepository sunshadeInspectionRepository;
    @Mock
    private AdditionalBenchPlanService additionalBenchPlanService;
    @Mock
    private BenchSponsorshipService benchSponsorshipService;
    @Mock
    private AntiSlipMatService antiSlipMatService;

    @InjectMocks
    private TreeNodeService treeNodeService;

    @Test
    void getTree_marksOutstandingPointAndAggregatesSection() {
        TreeNode district = TreeNode.builder().id(10L).level(1).name("A街区").sortOrder(0).build();
        TreeNode section = TreeNode.builder().id(20L).parentId(10L).level(2).name("主干道").sortOrder(0).build();
        // 点位30领10还4破损1未还6；点位31领5还5已还清
        TreeNode pointA = TreeNode.builder().id(30L).parentId(20L).level(3).name("广场前")
                .sortOrder(0).capacity(10).closed(0).build();
        TreeNode pointB = TreeNode.builder().id(31L).parentId(20L).level(3).name("商店旁")
                .sortOrder(1).capacity(10).closed(0).build();

        when(treeNodeRepository.findAllActiveNodes()).thenReturn(List.of(district, section, pointA, pointB));
        when(treeNodeRepository.findByIdsAndIsDeletedFalse(anyList())).thenReturn(List.of(pointA, pointB));
        when(benchRepository.countActiveByNodeIds(anyList())).thenReturn(List.of());
        when(lightingInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(anyList()))
                .thenReturn(List.of());
        when(sunshadeInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(anyList()))
                .thenReturn(List.of());
        when(additionalBenchPlanService.getSectionSummaries(anyList())).thenReturn(Map.of());
        when(antiSlipMatService.getLedgerMapByPointIds(anyList())).thenReturn(Map.of(
                30L, ledger(30L, 10, 4, 1, 6, true),
                31L, ledger(31L, 5, 5, 0, 0, false)));

        List<TreeNodeDTO> tree = treeNodeService.getTree();

        TreeNodeDTO sectionDto = tree.get(0).getChildren().get(0);
        TreeNodeDTO pointADto = sectionDto.getChildren().stream()
                .filter(p -> p.getId() == 30L).findFirst().orElseThrow();
        TreeNodeDTO pointBDto = sectionDto.getChildren().stream()
                .filter(p -> p.getId() == 31L).findFirst().orElseThrow();

        // 点位标记
        assertEquals(10, pointADto.getAntiSlipMatIssuedCount());
        assertEquals(4, pointADto.getAntiSlipMatReturnedCount());
        assertEquals(1, pointADto.getAntiSlipMatDamagedCount());
        assertEquals(6, pointADto.getAntiSlipMatOutstandingCount());
        assertEquals(0, pointBDto.getAntiSlipMatOutstandingCount());

        // 路段汇总：1 个点位未还清、未还数量加总 6
        assertEquals(1, sectionDto.getAntiSlipMatOutstandingPointCount());
        assertEquals(6, sectionDto.getAntiSlipMatOutstandingMatCount());
    }

    private PointAntiSlipMatDTO ledger(long pointId, int issued, int returned, int damaged,
                                       int outstanding, boolean outstandingFlag) {
        return PointAntiSlipMatDTO.builder()
                .pointId(pointId).issuedCount(issued).returnedCount(returned)
                .intactCount(returned - damaged).damagedCount(damaged)
                .outstandingCount(outstanding).outstanding(outstandingFlag).hasRecord(true).build();
    }
}
