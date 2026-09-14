package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.TreeNodeDTO;
import com.example.benchmanagement.entity.PointLightingInspection;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 街区树路段照明异常点数：与点位照明标记同源（每点位取最近一次巡查结论）。
 */
@ExtendWith(MockitoExtension.class)
class TreeNodeServiceLightingTest {

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
    private BenchSponsorshipService benchSponsorshipService;
    @Mock
    private AdditionalBenchPlanService additionalBenchPlanService;
    @Mock
    private AntiSlipMatService antiSlipMatService;

    @InjectMocks
    private TreeNodeService treeNodeService;

    private TreeNode district() {
        return TreeNode.builder().id(10L).level(1).name("中心街区").sortOrder(0).build();
    }

    private TreeNode section(Long id, String name, int sortOrder) {
        return TreeNode.builder().id(id).parentId(10L).level(2).name(name).sortOrder(sortOrder).build();
    }

    private TreeNode point(Long id, Long sectionId, String name, int sortOrder) {
        return TreeNode.builder().id(id).parentId(sectionId).level(3).name(name).sortOrder(sortOrder)
                .capacity(3).build();
    }

    private PointLightingInspection inspection(Long id, Long pointId, int result, String problemType) {
        return PointLightingInspection.builder()
                .id(id).pointId(pointId).result(result).lampCount(3)
                .problemType(problemType).inspector("张三")
                .inspectedAt(LocalDateTime.of(2026, 9, 12, 21, 0))
                .build();
    }

    @Test
    void tree_sectionLightingAbnormalCount_matchesPointMarkers() {
        TreeNode district = district();
        TreeNode sectionA = section(20L, "主街路段", 0);
        TreeNode sectionB = section(21L, "副街路段", 1);
        TreeNode pointA = point(30L, 20L, "点位A", 0);
        TreeNode pointB = point(31L, 20L, "点位B", 1);
        TreeNode pointC = point(32L, 21L, "点位C", 0);

        when(treeNodeRepository.findAllActiveNodes())
                .thenReturn(List.of(district, sectionA, sectionB, pointA, pointB, pointC));
        when(treeNodeRepository.findByIdsAndIsDeletedFalse(List.of(30L, 31L, 32L)))
                .thenReturn(List.of(pointA, pointB, pointC));
        when(benchRepository.countActiveByNodeIds(List.of(30L, 31L, 32L))).thenReturn(List.of());
        // 点位A缺灯、点位B完好、点位C损坏（均为各自最近一次结论）
        when(lightingInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(List.of(30L, 31L, 32L)))
                .thenReturn(List.of(
                        inspection(3L, 32L, PointLightingInspection.RESULT_ABNORMAL, "损坏"),
                        inspection(2L, 31L, PointLightingInspection.RESULT_INTACT, null),
                        inspection(1L, 30L, PointLightingInspection.RESULT_ABNORMAL, "缺灯")));

        List<TreeNodeDTO> tree = treeNodeService.getTree();

        assertEquals(1, tree.size());
        TreeNodeDTO districtDto = tree.get(0);
        assertNull(districtDto.getLightingAbnormalCount());

        TreeNodeDTO sectionADto = districtDto.getChildren().get(0);
        TreeNodeDTO sectionBDto = districtDto.getChildren().get(1);
        assertEquals(1, sectionADto.getLightingAbnormalCount());
        assertEquals(1, sectionBDto.getLightingAbnormalCount());

        // 路段计数与点位标记同源：异常点位正好是被标记为异常的点位
        TreeNodeDTO pointADto = sectionADto.getChildren().get(0);
        assertEquals(0, pointADto.getLightingResult());
        assertEquals("缺灯", pointADto.getLightingProblemType());
        TreeNodeDTO pointBDto = sectionADto.getChildren().get(1);
        assertEquals(1, pointBDto.getLightingResult());
        TreeNodeDTO pointCDto = sectionBDto.getChildren().get(0);
        assertEquals(0, pointCDto.getLightingResult());
        assertEquals("损坏", pointCDto.getLightingProblemType());
    }

    @Test
    void tree_sectionWithoutAbnormalPoint_hasZeroCount() {
        TreeNode district = district();
        TreeNode sectionA = section(20L, "主街路段", 0);
        TreeNode pointA = point(30L, 20L, "点位A", 0);

        when(treeNodeRepository.findAllActiveNodes()).thenReturn(List.of(district, sectionA, pointA));
        when(treeNodeRepository.findByIdsAndIsDeletedFalse(List.of(30L))).thenReturn(List.of(pointA));
        when(benchRepository.countActiveByNodeIds(List.of(30L))).thenReturn(List.of());
        when(lightingInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(List.of(30L)))
                .thenReturn(List.of(inspection(1L, 30L, PointLightingInspection.RESULT_INTACT, null)));

        List<TreeNodeDTO> tree = treeNodeService.getTree();

        TreeNodeDTO sectionDto = tree.get(0).getChildren().get(0);
        assertEquals(0, sectionDto.getLightingAbnormalCount());
    }
}
