package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.TreeNodeDTO;
import com.example.benchmanagement.entity.BenchSponsorship;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreeNodeServiceSponsorshipTest {

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

    @Test
    void getTree_fillsPointAndSectionSponsorshipExpiringMarks() {
        TreeNode district = TreeNode.builder().id(10L).level(1).name("A街区").sortOrder(0).build();
        TreeNode section = TreeNode.builder().id(20L).parentId(10L).level(2).name("主干道").sortOrder(0).build();
        TreeNode pointA = TreeNode.builder().id(30L).parentId(20L).level(3).name("广场前")
                .sortOrder(0).capacity(10).closed(0).build();
        TreeNode pointB = TreeNode.builder().id(31L).parentId(20L).level(3).name("路口")
                .sortOrder(1).capacity(10).closed(0).build();

        when(treeNodeRepository.findAllActiveNodes()).thenReturn(List.of(district, section, pointA, pointB));
        when(treeNodeRepository.findByIdsAndIsDeletedFalse(anyList()))
                .thenReturn(List.of(pointA, pointB));
        when(benchRepository.countActiveByNodeIds(anyList())).thenReturn(List.of());
        when(lightingInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(anyList()))
                .thenReturn(List.of());
        when(sunshadeInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(anyList()))
                .thenReturn(List.of());
        when(additionalBenchPlanService.getSectionSummaries(anyList()))
                .thenReturn(java.util.Map.of());
        LocalDate today = LocalDate.now();
        when(benchSponsorshipService.getExpiringSoonSponsorships(anyList(), org.mockito.ArgumentMatchers.eq(today)))
                .thenReturn(List.of(
                        BenchSponsorship.builder().id(1L).pointId(30L).merchantName("张三奶茶")
                                .sponsorshipText("张三奶茶请你歇脚")
                                .startDate(today.minusDays(10)).endDate(today.plusDays(2)).build(),
                        BenchSponsorship.builder().id(2L).pointId(30L).merchantName("李四小店")
                                .sponsorshipText("李四小店陪你等")
                                .startDate(today.minusDays(10)).endDate(today.plusDays(3)).build()
                ));

        List<TreeNodeDTO> tree = treeNodeService.getTree();

        TreeNodeDTO sectionDto = tree.get(0).getChildren().get(0);
        TreeNodeDTO pointADto = sectionDto.getChildren().stream()
                .filter(point -> point.getId() == 30L).findFirst().orElseThrow();
        assertEquals(2, pointADto.getSponsorshipExpiringCount());
        assertEquals(today.plusDays(2), pointADto.getSponsorshipNearestEndDate());
        assertEquals(2L, pointADto.getSponsorshipNearestDaysRemaining());
        assertEquals("张三奶茶", pointADto.getSponsorshipNearestMerchantName());
        assertEquals(2, sectionDto.getSponsorshipExpiringSectionCount());
    }
}
