package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.SectionCapacityAlarmDTO;
import com.example.benchmanagement.dto.TreeNodeDTO;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.BenchRepository;
import com.example.benchmanagement.repository.NodeCapacityLogRepository;
import com.example.benchmanagement.repository.NodeClosureLogRepository;
import com.example.benchmanagement.repository.PointLightingInspectionRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 街区树路段容量告警：路段下各点位剩余容量加总低于阈值即告警，
 * 告警标记、已满/将满点位与点位容量标记同源；没有点位的路段不出告警。
 */
@ExtendWith(MockitoExtension.class)
class TreeNodeServiceCapacityAlarmTest {

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

    @InjectMocks
    private TreeNodeService treeNodeService;

    private TreeNode district() {
        return TreeNode.builder().id(10L).level(1).name("中心街区").sortOrder(0).build();
    }

    private TreeNode section(Long id, String name, int sortOrder) {
        return TreeNode.builder().id(id).parentId(10L).level(2).name(name).sortOrder(sortOrder).build();
    }

    private TreeNode point(Long id, Long sectionId, String name, int sortOrder, int capacity) {
        return TreeNode.builder().id(id).parentId(sectionId).level(3).name(name).sortOrder(sortOrder)
                .capacity(capacity).build();
    }

    private Object[] occupiedRow(Long pointId, long count) {
        return new Object[]{pointId, count};
    }

    @Test
    void tree_sectionAlarm_whenRemainingSumBelowThreshold() {
        TreeNode district = district();
        TreeNode sectionA = section(20L, "主街路段", 0);
        TreeNode sectionB = section(21L, "副街路段", 1);
        // 路段A：点位A余0（已满）+ 点位B余5 = 5 < 阈值10 → 告警
        TreeNode pointA = point(30L, 20L, "点位A", 0, 2);
        TreeNode pointB = point(31L, 20L, "点位B", 1, 10);
        // 路段B：点位C余10 = 10，不低于阈值 → 不告警
        TreeNode pointC = point(32L, 21L, "点位C", 0, 10);

        when(treeNodeRepository.findAllActiveNodes())
                .thenReturn(List.of(district, sectionA, sectionB, pointA, pointB, pointC));
        when(treeNodeRepository.findByIdsAndIsDeletedFalse(List.of(30L, 31L, 32L)))
                .thenReturn(List.of(pointA, pointB, pointC));
        when(benchRepository.countActiveByNodeIds(List.of(30L, 31L, 32L)))
                .thenReturn(List.of(occupiedRow(30L, 2L), occupiedRow(31L, 5L)));
        when(lightingInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(List.of(30L, 31L, 32L)))
                .thenReturn(List.of());

        List<TreeNodeDTO> tree = treeNodeService.getTree();

        TreeNodeDTO sectionADto = tree.get(0).getChildren().get(0);
        assertEquals(5L, sectionADto.getCapacityRemainingSum());
        assertTrue(sectionADto.getCapacityAlarm());
        assertEquals(TreeNode.SECTION_CAPACITY_ALARM_THRESHOLD, sectionADto.getCapacityAlarmThreshold());
        // 告警统计与点位容量标记同源：点位A已满，点位B充足
        assertEquals(1, sectionADto.getCapacityFullCount());
        assertEquals(0, sectionADto.getCapacityNearlyFullCount());
        assertEquals(TreeNode.CAPACITY_STATUS_FULL, sectionADto.getChildren().get(0).getCapacityStatus());
        assertNull(sectionADto.getChildren().get(1).getCapacityStatus());

        TreeNodeDTO sectionBDto = tree.get(0).getChildren().get(1);
        assertEquals(10L, sectionBDto.getCapacityRemainingSum());
        assertFalse(sectionBDto.getCapacityAlarm());
    }

    @Test
    void tree_sectionWithoutPoints_noAlarm() {
        TreeNode district = district();
        TreeNode sectionA = section(20L, "空路段", 0);

        when(treeNodeRepository.findAllActiveNodes()).thenReturn(List.of(district, sectionA));

        List<TreeNodeDTO> tree = treeNodeService.getTree();

        TreeNodeDTO sectionDto = tree.get(0).getChildren().get(0);
        assertFalse(sectionDto.getCapacityAlarm());
        assertNull(sectionDto.getCapacityRemainingSum());
        assertEquals(0, sectionDto.getCapacityFullCount());
        assertEquals(0, sectionDto.getCapacityNearlyFullCount());
    }

    @Test
    void tree_nearlyFullPoint_countedAndAlarmed() {
        TreeNode district = district();
        TreeNode sectionA = section(20L, "主街路段", 0);
        // 点位A余2（将满）+ 点位B余6 = 8 < 阈值10 → 告警
        TreeNode pointA = point(30L, 20L, "点位A", 0, 3);
        TreeNode pointB = point(31L, 20L, "点位B", 1, 6);

        when(treeNodeRepository.findAllActiveNodes())
                .thenReturn(List.of(district, sectionA, pointA, pointB));
        when(treeNodeRepository.findByIdsAndIsDeletedFalse(List.of(30L, 31L)))
                .thenReturn(List.of(pointA, pointB));
        when(benchRepository.countActiveByNodeIds(List.of(30L, 31L)))
                .thenReturn(List.of(occupiedRow(30L, 1L)));
        when(lightingInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(List.of(30L, 31L)))
                .thenReturn(List.of());

        List<TreeNodeDTO> tree = treeNodeService.getTree();

        TreeNodeDTO sectionDto = tree.get(0).getChildren().get(0);
        assertEquals(8L, sectionDto.getCapacityRemainingSum());
        assertTrue(sectionDto.getCapacityAlarm());
        assertEquals(0, sectionDto.getCapacityFullCount());
        assertEquals(1, sectionDto.getCapacityNearlyFullCount());
        assertEquals(TreeNode.CAPACITY_STATUS_NEARLY_FULL, sectionDto.getChildren().get(0).getCapacityStatus());
    }

    @Test
    void drillDown_listsFullAndNearlyFullPointsOnly() {
        TreeNode sectionA = section(20L, "主街路段", 0);
        TreeNode pointA = point(30L, 20L, "点位A", 0, 2);
        TreeNode pointB = point(31L, 20L, "点位B", 1, 10);
        TreeNode pointC = point(32L, 20L, "点位C", 2, 4);

        when(treeNodeRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(sectionA));
        when(treeNodeRepository.findByParentIdAndIsDeletedFalse(20L))
                .thenReturn(List.of(pointA, pointB, pointC));
        when(treeNodeRepository.findByIdsAndIsDeletedFalse(List.of(30L, 31L, 32L)))
                .thenReturn(List.of(pointA, pointB, pointC));
        // 点位A余0（已满）、点位B余1（将满）、点位C余4（充足，不进明细）
        when(benchRepository.countActiveByNodeIds(List.of(30L, 31L, 32L)))
                .thenReturn(List.of(occupiedRow(30L, 2L), occupiedRow(31L, 9L)));

        SectionCapacityAlarmDTO detail = treeNodeService.getSectionCapacityAlarm(20L);

        assertEquals(20L, detail.getSectionId());
        assertEquals("主街路段", detail.getSectionName());
        assertEquals(TreeNode.SECTION_CAPACITY_ALARM_THRESHOLD, detail.getThreshold());
        assertEquals(5L, detail.getRemainingSum());
        assertTrue(detail.getAlarm());
        assertEquals(1, detail.getFullCount());
        assertEquals(1, detail.getNearlyFullCount());
        // 明细只含已满/将满点位，按剩余升序
        assertEquals(2, detail.getPoints().size());
        assertEquals(30L, detail.getPoints().get(0).getId());
        assertEquals(TreeNode.CAPACITY_STATUS_FULL, detail.getPoints().get(0).getCapacityStatus());
        assertEquals(31L, detail.getPoints().get(1).getId());
        assertEquals(TreeNode.CAPACITY_STATUS_NEARLY_FULL, detail.getPoints().get(1).getCapacityStatus());
    }

    @Test
    void drillDown_sectionWithoutPoints_noAlarmEmptyList() {
        TreeNode sectionA = section(20L, "空路段", 0);

        when(treeNodeRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(sectionA));
        when(treeNodeRepository.findByParentIdAndIsDeletedFalse(20L)).thenReturn(List.of());

        SectionCapacityAlarmDTO detail = treeNodeService.getSectionCapacityAlarm(20L);

        assertFalse(detail.getAlarm());
        assertNull(detail.getRemainingSum());
        assertEquals(0, detail.getFullCount());
        assertEquals(0, detail.getNearlyFullCount());
        assertTrue(detail.getPoints().isEmpty());
    }

    @Test
    void drillDown_rejectsNonSectionNode() {
        TreeNode pointA = point(30L, 20L, "点位A", 0, 2);
        when(treeNodeRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(pointA));

        assertThrows(IllegalArgumentException.class, () -> treeNodeService.getSectionCapacityAlarm(30L));
    }
}
