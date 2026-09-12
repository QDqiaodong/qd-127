package com.example.benchmanagement.service;

import com.example.benchmanagement.entity.Bench;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.BenchChangeLogRepository;
import com.example.benchmanagement.repository.BenchRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BenchServiceExportTest {

    @Mock
    private BenchRepository benchRepository;
    @Mock
    private TreeNodeRepository treeNodeRepository;
    @Mock
    private BenchChangeLogRepository changeLogRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private InspectionService inspectionService;
    @Mock
    private RepairOrderService repairOrderService;
    @Mock
    private TreeNodeService treeNodeService;

    @InjectMocks
    private BenchService benchService;

    private TreeNode district;
    private TreeNode section;
    private TreeNode pointA;
    private TreeNode pointB;

    @BeforeEach
    void setUp() {
        district = TreeNode.builder().id(10L).level(1).name("中心街区").sortOrder(0).build();
        section = TreeNode.builder().id(20L).parentId(10L).level(2).name("主街路段").sortOrder(0).build();
        pointA = TreeNode.builder().id(30L).parentId(20L).level(3).name("点位A").sortOrder(1).capacity(3).build();
        pointB = TreeNode.builder().id(31L).parentId(20L).level(3).name("点位B").sortOrder(2).capacity(2).build();
    }

    private Bench bench(Long id, String code, Long nodeId, int status) {
        return Bench.builder().id(id).code(code).nodeId(nodeId).status(status).build();
    }

    /** 点位A：在用1+停用1（容量3）；点位B：在用2（容量2） */
    private void stubSectionData() {
        when(treeNodeRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(section));
        // 空范围场景在构建行之前抛出异常，不会查询街区名称
        lenient().when(treeNodeRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(district));
        when(treeNodeRepository.findByParentIdAndIsDeletedFalse(20L))
                .thenReturn(List.of(pointA, pointB));
        when(benchRepository.findByNodeIds(List.of(30L, 31L))).thenReturn(List.of(
                bench(1L, "B001", 30L, Bench.STATUS_NORMAL),
                bench(2L, "B002", 30L, Bench.STATUS_DISABLED),
                bench(3L, "B003", 31L, Bench.STATUS_NORMAL),
                bench(4L, "B004", 31L, Bench.STATUS_NORMAL)));
        // 与树上点位占用同一数据源：仅统计在用
        when(benchRepository.countActiveByNodeIds(List.of(30L, 31L))).thenReturn(List.of(
                new Object[]{30L, 1L},
                new Object[]{31L, 2L}));
    }

    @Test
    void exportAll_returnsEveryBenchWithPointStatsMatchingTree() {
        stubSectionData();

        List<Map<String, Object>> assets = benchService.exportBenchAssets(20L, "all");

        assertEquals(4, assets.size());
        // 按点位排序号、编号排序：B001、B002（点位A）在前
        assertEquals("B001", assets.get(0).get("长凳编号"));
        assertEquals("B002", assets.get(1).get("长凳编号"));

        Map<String, Object> rowActive = assets.get(0);
        assertEquals("在用", rowActive.get("状态"));
        assertEquals(3, rowActive.get("点位容量"));
        assertEquals(1L, rowActive.get("点位占用(在用)"));
        assertEquals(2L, rowActive.get("点位剩余"));
        assertEquals(1L, rowActive.get("点位停用凳数"));
        assertEquals("中心街区", rowActive.get("所属街区"));
        assertEquals("主街路段", rowActive.get("所属路段"));
        assertEquals("点位A", rowActive.get("所属点位"));

        Map<String, Object> rowDisabled = assets.get(1);
        assertEquals("停用", rowDisabled.get("状态"));
        // 停用凳不计入占用，占用/剩余与在用行一致
        assertEquals(1L, rowDisabled.get("点位占用(在用)"));
        assertEquals(2L, rowDisabled.get("点位剩余"));
        assertEquals(1L, rowDisabled.get("点位停用凳数"));

        Map<String, Object> rowPointB = assets.get(2);
        assertEquals(2L, rowPointB.get("点位占用(在用)"));
        assertEquals(0L, rowPointB.get("点位剩余"));
        assertEquals(0L, rowPointB.get("点位停用凳数"));
    }

    @Test
    void exportActive_returnsOnlyInUseBenches() {
        stubSectionData();

        List<Map<String, Object>> assets = benchService.exportBenchAssets(20L, "active");

        assertEquals(3, assets.size());
        assertTrue(assets.stream().allMatch(a -> "在用".equals(a.get("状态"))));
        assertTrue(assets.stream().noneMatch(a -> "B002".equals(a.get("长凳编号"))));
    }

    @Test
    void exportDisabled_returnsOnlyDisabledBenches() {
        stubSectionData();

        List<Map<String, Object>> assets = benchService.exportBenchAssets(20L, "disabled");

        assertEquals(1, assets.size());
        assertEquals("B002", assets.get(0).get("长凳编号"));
        assertEquals("停用", assets.get(0).get("状态"));
    }

    @Test
    void exportBlankScope_defaultsToAll() {
        stubSectionData();

        assertEquals(4, benchService.exportBenchAssets(20L, null).size());
        assertEquals(4, benchService.exportBenchAssets(20L, "  ").size());
    }

    @Test
    void exportEmptyScope_throwsClearMessage() {
        stubSectionData();
        // 全部长凳改为在用，停用范围为空
        when(benchRepository.findByNodeIds(List.of(30L, 31L))).thenReturn(List.of(
                bench(1L, "B001", 30L, Bench.STATUS_NORMAL),
                bench(3L, "B003", 31L, Bench.STATUS_NORMAL)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> benchService.exportBenchAssets(20L, "disabled"));
        assertTrue(ex.getMessage().contains("主街路段"));
        assertTrue(ex.getMessage().contains("停用"));
        assertTrue(ex.getMessage().contains("导出范围为空"));
    }

    @Test
    void exportInvalidScope_throws() {
        when(treeNodeRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(section));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> benchService.exportBenchAssets(20L, "bogus"));
        assertTrue(ex.getMessage().contains("无效的导出范围"));
    }

    @Test
    void exportNonSectionNode_throws() {
        when(treeNodeRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(district));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> benchService.exportBenchAssets(10L, "all"));
        assertTrue(ex.getMessage().contains("只能导出路段级别的资产"));
    }
}
