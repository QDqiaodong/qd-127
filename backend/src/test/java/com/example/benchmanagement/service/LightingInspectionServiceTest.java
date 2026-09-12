package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.LightingInspectionCreateRequest;
import com.example.benchmanagement.dto.LightingInspectionDTO;
import com.example.benchmanagement.dto.PointLightingStatusDTO;
import com.example.benchmanagement.entity.PointLightingInspection;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.PointLightingInspectionRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LightingInspectionServiceTest {

    @Mock
    private PointLightingInspectionRepository lightingRepository;
    @Mock
    private TreeNodeRepository treeNodeRepository;

    @InjectMocks
    private LightingInspectionService lightingInspectionService;

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

    private LightingInspectionCreateRequest intactRequest() {
        return LightingInspectionCreateRequest.builder()
                .pointId(30L)
                .inspectedAt(LocalDateTime.of(2026, 9, 12, 21, 30))
                .result(PointLightingInspection.RESULT_INTACT)
                .lampCount(4)
                .inspector("张三")
                .build();
    }

    private void stubPointHierarchy() {
        lenient().when(treeNodeRepository.findByIdAndIsDeletedFalse(30L)).thenReturn(Optional.of(pointA));
        lenient().when(treeNodeRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(section));
        lenient().when(treeNodeRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(district));
    }

    @Test
    void createWithoutPoint_throws() {
        LightingInspectionCreateRequest request = intactRequest();
        request.setPointId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> lightingInspectionService.createInspection(request));
        assertTrue(ex.getMessage().contains("点位不能为空"));
        verifyNoInteractions(lightingRepository);
    }

    @Test
    void createWithNonPointNode_throws() {
        when(treeNodeRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(section));
        LightingInspectionCreateRequest request = intactRequest();
        request.setPointId(20L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> lightingInspectionService.createInspection(request));
        assertTrue(ex.getMessage().contains("点位"));
        verifyNoInteractions(lightingRepository);
    }

    @Test
    void createAbnormalWithoutProblemType_throws() {
        stubPointHierarchy();
        LightingInspectionCreateRequest request = intactRequest();
        request.setResult(PointLightingInspection.RESULT_ABNORMAL);
        request.setProblemType(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> lightingInspectionService.createInspection(request));
        assertTrue(ex.getMessage().contains("缺灯"));
        assertTrue(ex.getMessage().contains("损坏"));
        verify(lightingRepository, never()).save(any());
    }

    @Test
    void createAbnormalWithBogusProblemType_throws() {
        stubPointHierarchy();
        LightingInspectionCreateRequest request = intactRequest();
        request.setResult(PointLightingInspection.RESULT_ABNORMAL);
        request.setProblemType("线路老化");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> lightingInspectionService.createInspection(request));
        assertTrue(ex.getMessage().contains("缺灯"));
        verify(lightingRepository, never()).save(any());
    }

    @Test
    void createAbnormalMissingLamp_savesWithProblemType() {
        stubPointHierarchy();
        when(lightingRepository.save(any())).thenAnswer(inv -> {
            PointLightingInspection entity = inv.getArgument(0);
            entity.setId(1L);
            return entity;
        });
        LightingInspectionCreateRequest request = intactRequest();
        request.setResult(PointLightingInspection.RESULT_ABNORMAL);
        request.setProblemType(PointLightingInspection.PROBLEM_MISSING_LAMP);
        request.setDescription("广场东侧缺2盏路灯");

        LightingInspectionDTO dto = lightingInspectionService.createInspection(request);

        ArgumentCaptor<PointLightingInspection> captor = ArgumentCaptor.forClass(PointLightingInspection.class);
        verify(lightingRepository).save(captor.capture());
        assertEquals(30L, captor.getValue().getPointId());
        assertEquals(PointLightingInspection.RESULT_ABNORMAL, captor.getValue().getResult());
        assertEquals("缺灯", captor.getValue().getProblemType());
        assertEquals(0, dto.getResult());
        assertEquals("缺灯", dto.getProblemType());
        assertEquals("点位A", dto.getPointName());
        assertEquals("主街路段", dto.getSectionName());
        assertEquals("中心街区", dto.getDistrictName());
    }

    @Test
    void createIntact_clearsProblemType() {
        stubPointHierarchy();
        when(lightingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LightingInspectionCreateRequest request = intactRequest();
        request.setProblemType("缺灯");

        LightingInspectionDTO dto = lightingInspectionService.createInspection(request);

        ArgumentCaptor<PointLightingInspection> captor = ArgumentCaptor.forClass(PointLightingInspection.class);
        verify(lightingRepository).save(captor.capture());
        assertNull(captor.getValue().getProblemType());
        assertEquals(1, dto.getResult());
        assertEquals(4, dto.getLampCount());
        assertEquals("张三", dto.getInspector());
    }

    @Test
    void createWithBlankInspector_throws() {
        stubPointHierarchy();
        LightingInspectionCreateRequest request = intactRequest();
        request.setInspector("  ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> lightingInspectionService.createInspection(request));
        assertTrue(ex.getMessage().contains("巡查人"));
        verify(lightingRepository, never()).save(any());
    }

    @Test
    void createWithNegativeLampCount_throws() {
        stubPointHierarchy();
        LightingInspectionCreateRequest request = intactRequest();
        request.setLampCount(-1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> lightingInspectionService.createInspection(request));
        assertTrue(ex.getMessage().contains("灯具数量"));
        verify(lightingRepository, never()).save(any());
    }

    @Test
    void pointStatuses_uninspectedFilter_singlesOutUninspected() {
        PointLightingInspection inspA = PointLightingInspection.builder()
                .id(1L).pointId(30L).result(PointLightingInspection.RESULT_INTACT)
                .lampCount(4).inspector("张三")
                .inspectedAt(LocalDateTime.of(2026, 9, 11, 22, 0))
                .build();
        when(treeNodeRepository.findByLevelAndIsDeletedFalse(3)).thenReturn(List.of(pointA, pointB));
        when(lightingRepository.findByPointIdInOrderByInspectedAtDescIdDesc(List.of(30L, 31L)))
                .thenReturn(List.of(inspA));
        when(treeNodeRepository.findAllActiveNodes())
                .thenReturn(List.of(district, section, pointA, pointB));

        List<PointLightingStatusDTO> uninspected = lightingInspectionService.getPointStatuses(false);
        assertEquals(1, uninspected.size());
        assertEquals(31L, uninspected.get(0).getPointId());
        assertFalse(uninspected.get(0).getInspected());
        assertNull(uninspected.get(0).getLatestResult());

        List<PointLightingStatusDTO> inspected = lightingInspectionService.getPointStatuses(true);
        assertEquals(1, inspected.size());
        assertEquals(30L, inspected.get(0).getPointId());
        assertTrue(inspected.get(0).getInspected());
        assertEquals(1, inspected.get(0).getLatestResult());
        assertEquals(4, inspected.get(0).getLampCount());
        assertEquals("张三", inspected.get(0).getInspector());
        assertEquals("中心街区", inspected.get(0).getDistrictName());

        assertEquals(2, lightingInspectionService.getPointStatuses(null).size());
    }

    @Test
    void latestInspectionMap_picksNewestPerPoint() {
        PointLightingInspection newer = PointLightingInspection.builder()
                .id(2L).pointId(30L).result(PointLightingInspection.RESULT_ABNORMAL)
                .lampCount(3).problemType("损坏").inspector("李四")
                .inspectedAt(LocalDateTime.of(2026, 9, 12, 21, 0))
                .build();
        PointLightingInspection older = PointLightingInspection.builder()
                .id(1L).pointId(30L).result(PointLightingInspection.RESULT_INTACT)
                .lampCount(4).inspector("张三")
                .inspectedAt(LocalDateTime.of(2026, 9, 10, 21, 0))
                .build();
        when(lightingRepository.findByPointIdInOrderByInspectedAtDescIdDesc(List.of(30L)))
                .thenReturn(List.of(newer, older));

        Map<Long, PointLightingInspection> map = lightingInspectionService.latestInspectionMap(List.of(30L));

        assertEquals(1, map.size());
        assertEquals(2L, map.get(30L).getId());
        assertEquals("损坏", map.get(30L).getProblemType());
    }
}
