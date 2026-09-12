package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.AdditionalBenchPlanDTO;
import com.example.benchmanagement.dto.AdditionalBenchPlanRequest;
import com.example.benchmanagement.dto.AdditionalBenchVerifyRequest;
import com.example.benchmanagement.dto.SectionAdditionalBenchSummaryDTO;
import com.example.benchmanagement.entity.AdditionalBenchPlan;
import com.example.benchmanagement.entity.AdditionalBenchPlanLog;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.AdditionalBenchPlanLogRepository;
import com.example.benchmanagement.repository.AdditionalBenchPlanRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdditionalBenchPlanServiceTest {

    @Mock
    private AdditionalBenchPlanRepository planRepository;
    @Mock
    private AdditionalBenchPlanLogRepository logRepository;
    @Mock
    private TreeNodeRepository treeNodeRepository;

    @InjectMocks
    private AdditionalBenchPlanService planService;

    private TreeNode district;
    private TreeNode section;

    @BeforeEach
    void setUp() {
        district = TreeNode.builder().id(10L).level(1).name("A街区").build();
        section = TreeNode.builder().id(20L).parentId(10L).level(2).name("主干道").build();
    }

    @Test
    void create_withoutSection_throws() {
        AdditionalBenchPlanRequest request = AdditionalBenchPlanRequest.builder()
                .districtId(10L)
                .planDate(LocalDate.now())
                .benchCount(5)
                .build();
        when(treeNodeRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(district));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> planService.createPlan(request));
        assertTrue(ex.getMessage().contains("路段"));
        verify(planRepository, never()).save(any());
    }

    @Test
    void create_zeroCount_throws() {
        AdditionalBenchPlanRequest request = AdditionalBenchPlanRequest.builder()
                .districtId(10L)
                .sectionId(20L)
                .planDate(LocalDate.now())
                .benchCount(0)
                .build();
        when(treeNodeRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(district));
        when(treeNodeRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(section));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> planService.createPlan(request));
        assertTrue(ex.getMessage().contains("大于0"));
    }

    @Test
    void create_validPlan_pendingAndTreeNodesFilled() {
        AdditionalBenchPlanRequest request = AdditionalBenchPlanRequest.builder()
                .districtId(10L)
                .sectionId(20L)
                .planDate(LocalDate.now())
                .benchCount(4)
                .build();
        when(treeNodeRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(district));
        when(treeNodeRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(section));
        when(treeNodeRepository.findAllActiveNodes()).thenReturn(List.of(district, section));
        when(planRepository.save(any(AdditionalBenchPlan.class))).thenAnswer(invocation -> {
            AdditionalBenchPlan saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        AdditionalBenchPlanDTO dto = planService.createPlan(request);

        assertEquals(1L, dto.getId());
        assertEquals("A街区", dto.getDistrictName());
        assertEquals("主干道", dto.getSectionName());
        assertEquals(4, dto.getBenchCount());
        assertEquals(0, dto.getVerifiedCount());
        assertEquals(4, dto.getPendingCount());
        assertEquals(AdditionalBenchPlanService.EFFECTIVE_STATUS_PENDING, dto.getEffectiveStatus());
        assertFalse(dto.getOverdue());
    }

    @Test
    void verify_partialCount_keepsPendingAndLeavesOperatorTrace() {
        AdditionalBenchPlan plan = AdditionalBenchPlan.builder()
                .id(2L).districtId(10L).sectionId(20L)
                .planDate(LocalDate.now()).benchCount(5).verifiedCount(0)
                .status(AdditionalBenchPlan.STATUS_PENDING)
                .build();
        when(planRepository.findById(2L)).thenReturn(Optional.of(plan));
        when(treeNodeRepository.findAllActiveNodes()).thenReturn(List.of(district, section));
        when(logRepository.findByPlanIdOrderByVerifiedAtDescIdDesc(2L)).thenAnswer(invocation -> {
            AdditionalBenchPlanLog log = plan.getLastVerifiedAt() == null ? null : AdditionalBenchPlanLog.builder()
                    .planId(2L).verifiedCount(2).beforeCount(0).afterCount(2)
                    .operator("李四").verifiedAt(plan.getLastVerifiedAt())
                    .build();
            return log == null ? List.of() : List.of(log);
        });

        AdditionalBenchPlanDTO dto = planService.verify(2L, AdditionalBenchVerifyRequest.builder()
                .verifiedCount(2)
                .operator(" 李四 ")
                .remark("上午投放")
                .build());

        assertEquals(2, dto.getVerifiedCount());
        assertEquals(3, dto.getPendingCount());
        assertEquals(AdditionalBenchPlanService.EFFECTIVE_STATUS_PENDING, dto.getEffectiveStatus());
        assertEquals("李四", dto.getLastVerifiedBy());
        assertNotNull(dto.getLastVerifiedAt());
        assertEquals(1, dto.getLogs().size());
        assertEquals("李四", dto.getLogs().get(0).getOperator());

        ArgumentCaptor<AdditionalBenchPlanLog> logCaptor = ArgumentCaptor.forClass(AdditionalBenchPlanLog.class);
        verify(logRepository).save(logCaptor.capture());
        assertEquals(2, logCaptor.getValue().getVerifiedCount());
        assertEquals(0, logCaptor.getValue().getBeforeCount());
        assertEquals(2, logCaptor.getValue().getAfterCount());
        verify(planRepository).save(plan);
    }

    @Test
    void verify_allCount_marksDeployed() {
        AdditionalBenchPlan plan = AdditionalBenchPlan.builder()
                .id(3L).districtId(10L).sectionId(20L)
                .planDate(LocalDate.now()).benchCount(3).verifiedCount(1)
                .status(AdditionalBenchPlan.STATUS_PENDING)
                .build();
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));
        when(treeNodeRepository.findAllActiveNodes()).thenReturn(List.of(district, section));
        when(logRepository.findByPlanIdOrderByVerifiedAtDescIdDesc(3L)).thenReturn(List.of());

        AdditionalBenchPlanDTO dto = planService.verify(3L, AdditionalBenchVerifyRequest.builder()
                .verifiedCount(2)
                .operator("王五")
                .build());

        assertEquals(AdditionalBenchPlan.STATUS_DEPLOYED, plan.getStatus());
        assertEquals(0, dto.getPendingCount());
        assertEquals(AdditionalBenchPlanService.EFFECTIVE_STATUS_DEPLOYED, dto.getEffectiveStatus());
    }

    @Test
    void verify_moreThanPending_throws() {
        AdditionalBenchPlan plan = AdditionalBenchPlan.builder()
                .id(4L).districtId(10L).sectionId(20L)
                .planDate(LocalDate.now()).benchCount(3).verifiedCount(2)
                .status(AdditionalBenchPlan.STATUS_PENDING)
                .build();
        when(planRepository.findById(4L)).thenReturn(Optional.of(plan));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> planService.verify(4L, AdditionalBenchVerifyRequest.builder()
                        .verifiedCount(2).operator("赵六").build()));
        assertTrue(ex.getMessage().contains("超出待投放数量"));
        verify(logRepository, never()).save(any());
    }

    @Test
    void sectionSummaries_includesPendingAndOverdueCount() {
        AdditionalBenchPlan pendingToday = AdditionalBenchPlan.builder()
                .sectionId(20L).planDate(LocalDate.now()).benchCount(5).verifiedCount(0)
                .status(AdditionalBenchPlan.STATUS_PENDING)
                .build();
        AdditionalBenchPlan overduePartial = AdditionalBenchPlan.builder()
                .sectionId(20L).planDate(LocalDate.now().minusDays(1)).benchCount(4).verifiedCount(1)
                .status(AdditionalBenchPlan.STATUS_PENDING)
                .build();
        AdditionalBenchPlan deployed = AdditionalBenchPlan.builder()
                .sectionId(20L).planDate(LocalDate.now().minusDays(2)).benchCount(2).verifiedCount(2)
                .status(AdditionalBenchPlan.STATUS_DEPLOYED)
                .build();
        when(planRepository.findBySectionIdInOrderByPlanDateDescIdDesc(List.of(20L, 21L)))
                .thenReturn(List.of(pendingToday, overduePartial, deployed));

        Map<Long, SectionAdditionalBenchSummaryDTO> summaries =
                planService.getSectionSummaries(List.of(20L, 21L));

        assertEquals(8L, summaries.get(20L).getPendingCount());
        assertEquals(1L, summaries.get(20L).getOverduePlanCount());
        assertEquals(0L, summaries.get(21L).getPendingCount());
        assertEquals(0L, summaries.get(21L).getOverduePlanCount());
    }
}
