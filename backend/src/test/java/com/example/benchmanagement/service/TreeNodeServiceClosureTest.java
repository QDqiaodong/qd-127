package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.PointClosureRequest;
import com.example.benchmanagement.dto.PointReopenRequest;
import com.example.benchmanagement.dto.TreeNodeDTO;
import com.example.benchmanagement.entity.NodeClosureLog;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.BenchRepository;
import com.example.benchmanagement.repository.NodeCapacityLogRepository;
import com.example.benchmanagement.repository.NodeClosureLogRepository;
import com.example.benchmanagement.repository.PointLightingInspectionRepository;
import com.example.benchmanagement.repository.PointSunshadeInspectionRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 点位临时封闭/解封的持久化与状态判断回归测试：
 * 封闭状态落库（关掉页面/重启仍封闭）、树和详情必须带封闭标记、到期自动解封。
 */
@ExtendWith(MockitoExtension.class)
class TreeNodeServiceClosureTest {

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

    @InjectMocks
    private TreeNodeService treeNodeService;

    private TreeNode point;

    /** fillPointStatus 会批量查询点位容量/照明/遮阳棚，统一返回空，让封闭标记仅来自节点本身 */
    @BeforeEach
    void stubPointStatusQueries() {
        point = TreeNode.builder()
                .id(30L).parentId(20L).level(3).name("施工点位")
                .sortOrder(0).capacity(10).closed(0)
                .build();
        // 模拟 JPA 一级缓存：按 id 回填对应实例，save 后再查询能看到封闭字段
        lenient().when(treeNodeRepository.findByIdsAndIsDeletedFalse(anyList()))
                .thenAnswer(inv -> {
                    List<?> ids = inv.getArgument(0);
                    return ids.contains(30L) ? List.of(point) : List.of();
                });        lenient().when(benchRepository.countActiveByNodeIds(anyList())).thenReturn(List.of());
        lenient().when(lightingInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(anyList()))
                .thenReturn(List.of());
        lenient().when(sunshadeInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(anyList()))
                .thenReturn(List.of());
        lenient().when(additionalBenchPlanService.getSectionSummaries(anyList()))
                .thenReturn(java.util.Map.of());
    }

    private TreeNode point() {
        return point;
    }

    @Test
    void closePoint_persistsClosedStateAndLog() {
        TreeNode node = point();
        when(treeNodeRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(node));
        when(treeNodeRepository.save(any(TreeNode.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusDays(3);
        TreeNodeDTO result = treeNodeService.closePoint(30L, PointClosureRequest.builder()
                .startAt(start).endAt(end).reason("道路施工").build());

        ArgumentCaptor<TreeNode> nodeCaptor = ArgumentCaptor.forClass(TreeNode.class);
        verify(treeNodeRepository).save(nodeCaptor.capture());
        TreeNode saved = nodeCaptor.getValue();
        assertEquals(1, saved.getClosed(), "封闭状态必须落库，关掉页面/重启后依然封闭");
        assertEquals(start, saved.getClosedStartAt());
        assertEquals(end, saved.getClosedEndAt());
        assertEquals("道路施工", saved.getClosedReason());

        ArgumentCaptor<NodeClosureLog> logCaptor = ArgumentCaptor.forClass(NodeClosureLog.class);
        verify(closureLogRepository).save(logCaptor.capture());
        assertEquals(NodeClosureLog.ACTION_CLOSE, logCaptor.getValue().getActionType());

        assertTrue(result.getClosed());
        assertEquals("道路施工", result.getClosedReason());
    }

    @Test
    void closePoint_alreadyClosed_isRejected() {
        TreeNode node = point();
        node.setClosed(1);
        node.setClosedEndAt(LocalDateTime.now().plusDays(1));
        when(treeNodeRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(node));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> treeNodeService.closePoint(30L, PointClosureRequest.builder()
                        .startAt(LocalDateTime.now())
                        .endAt(LocalDateTime.now().plusDays(1))
                        .reason("再次封闭")
                        .build()));
        assertTrue(ex.getMessage().contains("已处于封闭状态"));
        verify(treeNodeRepository, never()).save(any(TreeNode.class));
    }

    @Test
    void isPointClosed_reflectsPeriod() {
        TreeNode closed = point();
        closed.setClosed(1);
        closed.setClosedEndAt(LocalDateTime.now().plusDays(1));
        when(treeNodeRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(closed));
        assertTrue(treeNodeService.isPointClosed(30L));

        // 到期但定时任务尚未落库：实时判断按未封闭处理，调入恢复
        closed.setClosedEndAt(LocalDateTime.now().minusMinutes(1));
        assertFalse(treeNodeService.isPointClosed(30L));

        TreeNode open = TreeNode.builder().id(31L).parentId(20L).level(3)
                .name("正常点位").capacity(10).closed(0).build();
        when(treeNodeRepository.findByIdAndIsDeletedFalse(31L)).thenReturn(Optional.of(open));
        assertFalse(treeNodeService.isPointClosed(31L));
    }

    @Test
    void tree_marksClosedPoint() {
        TreeNode district = TreeNode.builder().id(10L).level(1).name("街区").sortOrder(0).build();
        TreeNode section = TreeNode.builder().id(20L).parentId(10L).level(2).name("路段").sortOrder(0).build();
        TreeNode closedPoint = point();
        closedPoint.setClosed(1);
        closedPoint.setClosedReason("道路施工");
        closedPoint.setClosedEndAt(LocalDateTime.now().plusDays(2));

        when(treeNodeRepository.findAllActiveNodes())
                .thenReturn(List.of(district, section, closedPoint));

        List<TreeNodeDTO> tree = treeNodeService.getTree();

        TreeNodeDTO pointDto = tree.get(0).getChildren().get(0).getChildren().get(0);
        assertTrue(pointDto.getClosed(), "树上点位必须能看出正在封闭");
        assertEquals("道路施工", pointDto.getClosedReason());
        assertNotNull(pointDto.getClosedEndAt());
    }

    @Test
    void autoExpireClosedPoints_clearsExpiredAndWritesLog() {
        TreeNode expired = point();
        expired.setClosed(1);
        expired.setClosedStartAt(LocalDateTime.now().minusDays(5));
        expired.setClosedEndAt(LocalDateTime.now().minusMinutes(1));
        expired.setClosedReason("施工完成");
        when(treeNodeRepository.findByLevelAndIsDeletedFalse(3)).thenReturn(List.of(expired));

        int count = treeNodeService.autoExpireClosedPoints();

        assertEquals(1, count);
        assertEquals(0, expired.getClosed(), "到期后封闭标记必须落库清除");
        assertNull(expired.getClosedEndAt());
        verify(closureLogRepository).save(argThat(l ->
                l.getActionType() != null && l.getActionType() == NodeClosureLog.ACTION_REOPEN_AUTO));
    }

    @Test
    void reopenPoint_clearsClosedStateAndLogsReason() {
        TreeNode node = point();
        node.setClosed(1);
        node.setClosedStartAt(LocalDateTime.now().minusDays(1));
        node.setClosedEndAt(LocalDateTime.now().plusDays(2));
        node.setClosedReason("道路施工");
        when(treeNodeRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(node));
        when(treeNodeRepository.save(any(TreeNode.class))).thenAnswer(inv -> inv.getArgument(0));

        TreeNodeDTO result = treeNodeService.reopenPoint(30L,
                PointReopenRequest.builder().reason("施工提前结束").build());

        assertEquals(0, node.getClosed());
        assertNull(node.getClosedEndAt());
        assertNull(node.getClosedReason());
        assertFalse(result.getClosed());
        verify(closureLogRepository).save(argThat(l ->
                l.getActionType() != null
                        && l.getActionType() == NodeClosureLog.ACTION_REOPEN_MANUAL
                        && "施工提前结束".equals(l.getReopenReason())));
    }
}
