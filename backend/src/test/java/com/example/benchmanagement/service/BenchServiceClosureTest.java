package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.BenchDTO;
import com.example.benchmanagement.dto.BenchChangeRequest;
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
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 临时封闭点位禁止调入长凳的回归测试：
 * 新增建档、编辑移入、批量调入三条路径都必须被拦截并返回明确原因；
 * 未封闭（含已到期）的点位不受影响。
 */
@ExtendWith(MockitoExtension.class)
class BenchServiceClosureTest {

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

    private TreeNode closedPoint;

    @BeforeEach
    void setUp() {
        ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        closedPoint = TreeNode.builder()
                .id(30L).parentId(20L).level(3).name("封闭点位")
                .capacity(10)
                .closed(1)
                .closedStartAt(LocalDateTime.now().minusDays(1))
                .closedEndAt(LocalDateTime.now().plusDays(2))
                .closedReason("道路施工")
                .build();
    }

    private void stubPoint(TreeNode point, boolean closed) {
        when(treeNodeRepository.findByIdAndIsDeletedFalse(point.getId())).thenReturn(Optional.of(point));
        when(treeNodeService.isPointClosed(point.getId())).thenReturn(closed);
    }

    @Test
    void createBench_intoClosedPoint_isBlockedWithClearReason() {
        stubPoint(closedPoint, true);

        BenchDTO request = BenchDTO.builder().code("B001").nodeId(30L).status(1).build();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> benchService.createBench(request));

        String message = ex.getMessage();
        assertTrue(message.contains("调入失败"), "错误信息应以明确的失败原因开头: " + message);
        assertTrue(message.contains("封闭点位"), "错误信息应包含点位名称: " + message);
        assertTrue(message.contains("禁止调入"), "错误信息应明确禁止调入: " + message);
        assertTrue(message.contains("道路施工"), "错误信息应包含封闭原因: " + message);
        assertTrue(message.contains("至"), "错误信息应包含封闭期: " + message);
        // 关键：长凳没有落库，封闭期内不会出现新凳
        verify(benchRepository, never()).existsByCode(anyString());
        verify(benchRepository, never()).save(any(Bench.class));
    }

    @Test
    void updateBench_moveIntoClosedPoint_isBlockedAndBenchStays() {
        stubPoint(closedPoint, true);
        Bench existing = Bench.builder().id(1L).code("B001").nodeId(31L).status(1).build();
        when(benchRepository.findById(1L)).thenReturn(Optional.of(existing));

        BenchDTO request = BenchDTO.builder().code("B001").nodeId(30L)
                .changeReason("尝试移入封闭点位").status(1).build();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> benchService.updateBench(1L, request));

        assertTrue(ex.getMessage().contains("禁止调入"));
        assertEquals(31L, existing.getNodeId(), "被拦截后长凳应留在原点位");
        verify(changeLogRepository, never()).save(any());
    }

    @Test
    void changeBenchNode_intoClosedPoint_isBlocked() {
        stubPoint(closedPoint, true);

        BenchChangeRequest request = BenchChangeRequest.builder()
                .benchIds(List.of(1L)).newNodeId(30L).changeReason("批量调入").build();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> benchService.changeBenchNode(request));

        assertTrue(ex.getMessage().contains("禁止调入"));
        // 拦截发生在校验阶段：不加载长凳、不落库变更、不写台账，封闭点位不会出现新凳
        verify(benchRepository, never()).findById(anyLong());
        verify(benchRepository, never()).save(any(Bench.class));
        verify(changeLogRepository, never()).save(any());
    }

    @Test
    void createBench_intoOpenPoint_succeeds() {
        TreeNode openPoint = TreeNode.builder().id(31L).parentId(20L).level(3)
                .name("正常点位").capacity(10).closed(0).build();
        stubPoint(openPoint, false);
        when(benchRepository.existsByCode("B002")).thenReturn(false);
        when(benchRepository.countActiveByNodeId(31L)).thenReturn(0L);
        when(benchRepository.save(any(Bench.class))).thenAnswer(inv -> {
            Bench b = inv.getArgument(0);
            b.setId(9L);
            return b;
        });

        BenchDTO created = benchService.createBench(
                BenchDTO.builder().code("B002").nodeId(31L).status(1).build());

        assertEquals(31L, created.getNodeId());
        verify(benchRepository).save(any(Bench.class));
    }

    @Test
    void createBench_whenClosureExpired_isAllowed() {
        // 封闭记录已到期：isPointClosed 实时判断为 false（定时任务或启动任务尚未落库也不影响）
        closedPoint.setClosedEndAt(LocalDateTime.now().minusMinutes(1));
        stubPoint(closedPoint, false);
        when(benchRepository.existsByCode("B003")).thenReturn(false);
        when(benchRepository.countActiveByNodeId(anyLong())).thenReturn(0L);
        when(benchRepository.save(any(Bench.class))).thenAnswer(inv -> {
            Bench b = inv.getArgument(0);
            b.setId(10L);
            return b;
        });

        assertDoesNotThrow(() -> benchService.createBench(
                BenchDTO.builder().code("B003").nodeId(30L).status(1).build()));
    }
}
