package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.PointSunshadeStatusDTO;
import com.example.benchmanagement.dto.SectionSunshadeSummaryDTO;
import com.example.benchmanagement.dto.SunshadeInspectionCreateRequest;
import com.example.benchmanagement.dto.SunshadeInspectionDTO;
import com.example.benchmanagement.entity.PointSunshadeInspection;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.PointSunshadeInspectionRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SunshadeInspectionService {

    private final PointSunshadeInspectionRepository sunshadeRepository;
    private final TreeNodeRepository treeNodeRepository;
    private final TreeNodeService treeNodeService;

    /**
     * 登记点位遮阳棚巡查：点位必选，异常（破损）时必须写清破损位置；封闭点位禁止登记。
     */
    @Transactional
    public SunshadeInspectionDTO createInspection(SunshadeInspectionCreateRequest request) {
        if (request.getPointId() == null) {
            throw new IllegalArgumentException("点位不能为空，请先选择点位再提交");
        }
        TreeNode point = treeNodeRepository.findByIdAndIsDeletedFalse(request.getPointId())
                .orElseThrow(() -> new IllegalArgumentException("点位不存在"));
        if (point.getLevel() != 3) {
            throw new IllegalArgumentException("遮阳棚巡查只能登记到点位(level=3)，请重新选择");
        }
        assertPointNotClosed(point);

        if (request.getInspectedAt() == null) {
            throw new IllegalArgumentException("巡查时间不能为空");
        }
        Integer result = request.getResult();
        if (result == null
                || (result != PointSunshadeInspection.RESULT_INTACT
                && result != PointSunshadeInspection.RESULT_ABNORMAL)) {
            throw new IllegalArgumentException("巡查结论只能为完好(1)或异常(0)");
        }
        BigDecimal damagedArea = request.getDamagedArea();
        if (damagedArea == null || damagedArea.signum() < 0) {
            throw new IllegalArgumentException("破损面积必须为大于等于0的数字");
        }
        String inspector = request.getInspector() == null ? "" : request.getInspector().trim();
        if (inspector.isEmpty()) {
            throw new IllegalArgumentException("巡查人不能为空");
        }

        boolean abnormal = result == PointSunshadeInspection.RESULT_ABNORMAL;
        String damagedLocation = trimToNull(request.getDamagedLocation());
        if (abnormal) {
            if (damagedLocation == null) {
                throw new IllegalArgumentException("遮阳棚异常时必须写清破损位置");
            }
        } else {
            // 完好时破损位置清空、破损面积归零
            damagedLocation = null;
            damagedArea = BigDecimal.ZERO;
        }

        PointSunshadeInspection inspection = PointSunshadeInspection.builder()
                .pointId(point.getId())
                .inspectedAt(request.getInspectedAt())
                .result(result)
                .damagedArea(damagedArea)
                .damagedLocation(damagedLocation)
                .description(trimToNull(request.getDescription()))
                .inspector(inspector)
                .build();
        PointSunshadeInspection saved = sunshadeRepository.save(inspection);
        log.info("保存遮阳棚巡查记录: pointId={}, result={}, damagedArea={}, inspector={}",
                point.getId(), result, damagedArea, inspector);
        return toDTO(saved);
    }

    /**
     * 遮阳棚巡查记录列表，可按点位、巡查结论、关键字（点位名/巡查人/破损位置）筛选。
     */
    public List<SunshadeInspectionDTO> getInspections(Long pointId, Integer result, String keyword) {
        List<PointSunshadeInspection> all;
        if (pointId != null) {
            requirePoint(pointId);
            all = sunshadeRepository.findByPointIdOrderByInspectedAtDescIdDesc(pointId);
        } else {
            all = sunshadeRepository.findAllByOrderByInspectedAtDescIdDesc();
        }
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        Map<Long, String> pointNames = new HashMap<>();
        return all.stream()
                .filter(i -> result == null || result.equals(i.getResult()))
                .filter(i -> {
                    if (kw.isEmpty()) {
                        return true;
                    }
                    String pointName = pointNames.computeIfAbsent(i.getPointId(), this::pointNameOf);
                    return pointName.toLowerCase().contains(kw)
                            || (i.getInspector() != null && i.getInspector().toLowerCase().contains(kw))
                            || (i.getDamagedLocation() != null && i.getDamagedLocation().toLowerCase().contains(kw));
                })
                .map(this::toDTO)
                .toList();
    }

    /**
     * 单个点位的遮阳棚巡查记录（点位详情用）。
     */
    public List<SunshadeInspectionDTO> getInspectionsByPoint(Long pointId) {
        requirePoint(pointId);
        return sunshadeRepository.findByPointIdOrderByInspectedAtDescIdDesc(pointId)
                .stream().map(this::toDTO).toList();
    }

    /**
     * 全部点位的遮阳棚状态（每点位取最近一次巡查结论）。
     * inspected=true 只看已巡查，inspected=false 单独筛出未巡查点位，null 返回全部；
     * sectionId 下钻单个路段的点位，result 按最近一次巡查结论筛选（1-完好，0-异常）。
     */
    public List<PointSunshadeStatusDTO> getPointStatuses(Boolean inspected, Long sectionId, Integer result) {
        List<TreeNode> points;
        if (sectionId != null) {
            TreeNode section = treeNodeRepository.findByIdAndIsDeletedFalse(sectionId)
                    .orElseThrow(() -> new IllegalArgumentException("路段不存在"));
            if (section.getLevel() != 2) {
                throw new IllegalArgumentException("只有路段(level=2)可以下钻查看点位遮阳棚状态");
            }
            points = treeNodeRepository.findByParentIdAndIsDeletedFalse(sectionId);
        } else {
            points = treeNodeRepository.findByLevelAndIsDeletedFalse(3);
        }
        if (result != null
                && result != PointSunshadeInspection.RESULT_INTACT
                && result != PointSunshadeInspection.RESULT_ABNORMAL) {
            throw new IllegalArgumentException("巡查结论只能为完好(1)或异常(0)");
        }
        if (points.isEmpty()) {
            return List.of();
        }
        List<Long> pointIds = points.stream().map(TreeNode::getId).toList();
        Map<Long, PointSunshadeInspection> latestMap = latestInspectionMap(pointIds);

        Map<Long, TreeNode> nodeMap = new HashMap<>();
        for (TreeNode node : treeNodeRepository.findAllActiveNodes()) {
            nodeMap.put(node.getId(), node);
        }

        List<PointSunshadeStatusDTO> statuses = new ArrayList<>();
        for (TreeNode point : points) {
            PointSunshadeInspection latest = latestMap.get(point.getId());
            boolean hasRecord = latest != null;
            if (inspected != null && inspected != hasRecord) {
                continue;
            }
            if (result != null && (!hasRecord || !result.equals(latest.getResult()))) {
                continue;
            }
            TreeNode section = point.getParentId() != null ? nodeMap.get(point.getParentId()) : null;
            TreeNode district = section != null && section.getParentId() != null
                    ? nodeMap.get(section.getParentId()) : null;
            statuses.add(PointSunshadeStatusDTO.builder()
                    .pointId(point.getId())
                    .pointName(point.getName())
                    .sectionName(section != null ? section.getName() : "")
                    .districtName(district != null ? district.getName() : "")
                    .inspected(hasRecord)
                    .latestResult(hasRecord ? latest.getResult() : null)
                    .damagedArea(hasRecord ? latest.getDamagedArea() : null)
                    .damagedLocation(hasRecord ? latest.getDamagedLocation() : null)
                    .inspector(hasRecord ? latest.getInspector() : null)
                    .latestInspectedAt(hasRecord ? latest.getInspectedAt() : null)
                    .build());
        }
        return statuses;
    }

    /**
     * 按路段汇总遮阳棚异常：异常点数、破损面积加总（平方米）。
     * 口径与树上点位遮阳棚标记一致（每点位取最近一次巡查结论）；
     * 没有异常点位的路段不出汇总。
     */
    public List<SectionSunshadeSummaryDTO> getSectionSummaries() {
        List<TreeNode> points = treeNodeRepository.findByLevelAndIsDeletedFalse(3);
        if (points.isEmpty()) {
            return List.of();
        }
        List<Long> pointIds = points.stream().map(TreeNode::getId).toList();
        Map<Long, PointSunshadeInspection> latestMap = latestInspectionMap(pointIds);

        Map<Long, TreeNode> nodeMap = new HashMap<>();
        for (TreeNode node : treeNodeRepository.findAllActiveNodes()) {
            nodeMap.put(node.getId(), node);
        }

        Map<Long, SectionSunshadeSummaryDTO> summaryBySection = new LinkedHashMap<>();
        for (TreeNode point : points) {
            PointSunshadeInspection latest = latestMap.get(point.getId());
            if (latest == null || latest.getResult() != PointSunshadeInspection.RESULT_ABNORMAL) {
                continue;
            }
            TreeNode section = point.getParentId() != null ? nodeMap.get(point.getParentId()) : null;
            if (section == null) {
                continue;
            }
            TreeNode district = section.getParentId() != null ? nodeMap.get(section.getParentId()) : null;
            SectionSunshadeSummaryDTO summary = summaryBySection.computeIfAbsent(section.getId(),
                    id -> SectionSunshadeSummaryDTO.builder()
                            .sectionId(section.getId())
                            .sectionName(section.getName())
                            .districtId(district != null ? district.getId() : null)
                            .districtName(district != null ? district.getName() : "")
                            .abnormalPointCount(0)
                            .damagedAreaSum(BigDecimal.ZERO)
                            .build());
            summary.setAbnormalPointCount(summary.getAbnormalPointCount() + 1);
            if (latest.getDamagedArea() != null) {
                summary.setDamagedAreaSum(summary.getDamagedAreaSum().add(latest.getDamagedArea()));
            }
            if (summary.getLatestInspectedAt() == null
                    || latest.getInspectedAt().isAfter(summary.getLatestInspectedAt())) {
                summary.setLatestInspectedAt(latest.getInspectedAt());
            }
        }

        // 按街区树顺序（街区排序号 → 路段排序号 → 路段id）输出，方便与树上标记逐条对照
        return summaryBySection.values().stream()
                .sorted(Comparator
                        .comparing((SectionSunshadeSummaryDTO s) -> sortOrderOf(nodeMap, s.getDistrictId()))
                        .thenComparing(s -> sortOrderOf(nodeMap, s.getSectionId()))
                        .thenComparing(SectionSunshadeSummaryDTO::getSectionId))
                .toList();
    }

    private int sortOrderOf(Map<Long, TreeNode> nodeMap, Long nodeId) {
        TreeNode node = nodeId != null ? nodeMap.get(nodeId) : null;
        return node != null && node.getSortOrder() != null ? node.getSortOrder() : 0;
    }

    /**
     * 批量取每个点位的最近一次遮阳棚巡查记录（街区树点位标记用）。
     */
    public Map<Long, PointSunshadeInspection> latestInspectionMap(List<Long> pointIds) {
        Map<Long, PointSunshadeInspection> map = new HashMap<>();
        if (pointIds == null || pointIds.isEmpty()) {
            return map;
        }
        for (PointSunshadeInspection inspection
                : sunshadeRepository.findByPointIdInOrderByInspectedAtDescIdDesc(pointIds)) {
            map.putIfAbsent(inspection.getPointId(), inspection);
        }
        return map;
    }

    private TreeNode requirePoint(Long pointId) {
        TreeNode point = treeNodeRepository.findByIdAndIsDeletedFalse(pointId)
                .orElseThrow(() -> new IllegalArgumentException("点位不存在"));
        if (point.getLevel() != 3) {
            throw new IllegalArgumentException("只有点位(level=3)有遮阳棚巡查记录");
        }
        return point;
    }

    /**
     * 封闭期内禁止往该点位登记遮阳棚巡查，到期或人工解封后恢复。
     */
    private void assertPointNotClosed(TreeNode point) {
        if (treeNodeService.isPointClosed(point.getId())) {
            String endAt = point.getClosedEndAt() != null ? point.getClosedEndAt().toString() : "未设置";
            throw new IllegalStateException(String.format(
                    "点位【%s】处于临时封闭期（截止%s），封闭期内禁止登记遮阳棚巡查，到期或人工解封后恢复",
                    point.getName(), endAt));
        }
    }

    private String pointNameOf(Long pointId) {
        return treeNodeRepository.findById(pointId).map(TreeNode::getName).orElse("");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SunshadeInspectionDTO toDTO(PointSunshadeInspection inspection) {
        TreeNode point = treeNodeRepository.findByIdAndIsDeletedFalse(inspection.getPointId()).orElse(null);
        TreeNode section = point != null && point.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(point.getParentId()).orElse(null) : null;
        TreeNode district = section != null && section.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(section.getParentId()).orElse(null) : null;
        return SunshadeInspectionDTO.builder()
                .id(inspection.getId())
                .pointId(inspection.getPointId())
                .pointName(point != null ? point.getName() : "")
                .sectionName(section != null ? section.getName() : "")
                .districtName(district != null ? district.getName() : "")
                .inspectedAt(inspection.getInspectedAt())
                .result(inspection.getResult())
                .damagedArea(inspection.getDamagedArea())
                .damagedLocation(inspection.getDamagedLocation())
                .description(inspection.getDescription())
                .inspector(inspection.getInspector())
                .createdAt(inspection.getCreatedAt())
                .build();
    }
}
