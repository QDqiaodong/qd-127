package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.LightingInspectionCreateRequest;
import com.example.benchmanagement.dto.LightingInspectionDTO;
import com.example.benchmanagement.dto.PointLightingStatusDTO;
import com.example.benchmanagement.entity.PointLightingInspection;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.PointLightingInspectionRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LightingInspectionService {

    private static final Set<String> PROBLEM_TYPES = Set.of(
            PointLightingInspection.PROBLEM_MISSING_LAMP,
            PointLightingInspection.PROBLEM_DAMAGED);

    private final PointLightingInspectionRepository lightingRepository;
    private final TreeNodeRepository treeNodeRepository;
    private final TreeNodeService treeNodeService;

    /**
     * 登记点位夜间照明巡查：点位必选，异常时必须点名缺灯或损坏；封闭点位禁止登记。
     */
    @Transactional
    public LightingInspectionDTO createInspection(LightingInspectionCreateRequest request) {
        if (request.getPointId() == null) {
            throw new IllegalArgumentException("点位不能为空，请先选择点位再提交");
        }
        TreeNode point = treeNodeRepository.findByIdAndIsDeletedFalse(request.getPointId())
                .orElseThrow(() -> new IllegalArgumentException("点位不存在"));
        if (point.getLevel() != 3) {
            throw new IllegalArgumentException("照明巡查只能登记到点位(level=3)，请重新选择");
        }
        assertPointNotClosed(point);

        if (request.getInspectedAt() == null) {
            throw new IllegalArgumentException("巡查时间不能为空");
        }
        Integer result = request.getResult();
        if (result == null
                || (result != PointLightingInspection.RESULT_INTACT
                && result != PointLightingInspection.RESULT_ABNORMAL)) {
            throw new IllegalArgumentException("照明结论只能为完好(1)或异常(0)");
        }
        if (request.getLampCount() == null || request.getLampCount() < 0) {
            throw new IllegalArgumentException("灯具数量必须为大于等于0的整数");
        }
        String inspector = request.getInspector() == null ? "" : request.getInspector().trim();
        if (inspector.isEmpty()) {
            throw new IllegalArgumentException("巡查人不能为空");
        }

        boolean abnormal = result == PointLightingInspection.RESULT_ABNORMAL;
        String problemType = trimToNull(request.getProblemType());
        if (abnormal) {
            if (problemType == null || !PROBLEM_TYPES.contains(problemType)) {
                throw new IllegalArgumentException("照明异常时必须点名异常类型：缺灯 或 损坏");
            }
        } else {
            problemType = null;
        }

        PointLightingInspection inspection = PointLightingInspection.builder()
                .pointId(point.getId())
                .inspectedAt(request.getInspectedAt())
                .result(result)
                .lampCount(request.getLampCount())
                .problemType(problemType)
                .description(trimToNull(request.getDescription()))
                .inspector(inspector)
                .build();
        PointLightingInspection saved = lightingRepository.save(inspection);
        log.info("保存照明巡查记录: pointId={}, result={}, lampCount={}, inspector={}",
                point.getId(), result, request.getLampCount(), inspector);
        return toDTO(saved);
    }

    /**
     * 照明巡查记录列表，可按点位、照明结论、关键字（点位名/巡查人/异常类型）筛选。
     */
    public List<LightingInspectionDTO> getInspections(Long pointId, Integer result, String keyword) {
        List<PointLightingInspection> all;
        if (pointId != null) {
            requirePoint(pointId);
            all = lightingRepository.findByPointIdOrderByInspectedAtDescIdDesc(pointId);
        } else {
            all = lightingRepository.findAllByOrderByInspectedAtDescIdDesc();
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
                            || (i.getProblemType() != null && i.getProblemType().toLowerCase().contains(kw));
                })
                .map(this::toDTO)
                .toList();
    }

    /**
     * 单个点位的照明巡查记录（点位详情用）。
     */
    public List<LightingInspectionDTO> getInspectionsByPoint(Long pointId) {
        requirePoint(pointId);
        return lightingRepository.findByPointIdOrderByInspectedAtDescIdDesc(pointId)
                .stream().map(this::toDTO).toList();
    }

    /**
     * 全部点位的照明状态（每点位取最近一次巡查结论）。
     * inspected=true 只看已巡查，inspected=false 单独筛出未巡查点位，null 返回全部。
     */
    public List<PointLightingStatusDTO> getPointStatuses(Boolean inspected) {
        List<TreeNode> points = treeNodeRepository.findByLevelAndIsDeletedFalse(3);
        if (points.isEmpty()) {
            return List.of();
        }
        List<Long> pointIds = points.stream().map(TreeNode::getId).toList();
        Map<Long, PointLightingInspection> latestMap = latestInspectionMap(pointIds);

        Map<Long, TreeNode> nodeMap = new HashMap<>();
        for (TreeNode node : treeNodeRepository.findAllActiveNodes()) {
            nodeMap.put(node.getId(), node);
        }

        List<PointLightingStatusDTO> statuses = new ArrayList<>();
        for (TreeNode point : points) {
            PointLightingInspection latest = latestMap.get(point.getId());
            boolean hasRecord = latest != null;
            if (inspected != null && inspected != hasRecord) {
                continue;
            }
            TreeNode section = point.getParentId() != null ? nodeMap.get(point.getParentId()) : null;
            TreeNode district = section != null && section.getParentId() != null
                    ? nodeMap.get(section.getParentId()) : null;
            statuses.add(PointLightingStatusDTO.builder()
                    .pointId(point.getId())
                    .pointName(point.getName())
                    .sectionName(section != null ? section.getName() : "")
                    .districtName(district != null ? district.getName() : "")
                    .inspected(hasRecord)
                    .latestResult(hasRecord ? latest.getResult() : null)
                    .lampCount(hasRecord ? latest.getLampCount() : null)
                    .problemType(hasRecord ? latest.getProblemType() : null)
                    .inspector(hasRecord ? latest.getInspector() : null)
                    .latestInspectedAt(hasRecord ? latest.getInspectedAt() : null)
                    .build());
        }
        return statuses;
    }

    /**
     * 批量取每个点位的最近一次照明巡查记录（街区树点位标记用）。
     */
    public Map<Long, PointLightingInspection> latestInspectionMap(List<Long> pointIds) {
        Map<Long, PointLightingInspection> map = new HashMap<>();
        if (pointIds == null || pointIds.isEmpty()) {
            return map;
        }
        for (PointLightingInspection inspection
                : lightingRepository.findByPointIdInOrderByInspectedAtDescIdDesc(pointIds)) {
            map.putIfAbsent(inspection.getPointId(), inspection);
        }
        return map;
    }

    private TreeNode requirePoint(Long pointId) {
        TreeNode point = treeNodeRepository.findByIdAndIsDeletedFalse(pointId)
                .orElseThrow(() -> new IllegalArgumentException("点位不存在"));
        if (point.getLevel() != 3) {
            throw new IllegalArgumentException("只有点位(level=3)有照明巡查记录");
        }
        return point;
    }

    /**
     * 封闭期内禁止往该点位登记照明巡查，到期或人工解封后恢复。
     */
    private void assertPointNotClosed(TreeNode point) {
        if (treeNodeService.isPointClosed(point.getId())) {
            String endAt = point.getClosedEndAt() != null ? point.getClosedEndAt().toString() : "未设置";
            throw new IllegalStateException(String.format(
                    "点位【%s】处于临时封闭期（截止%s），封闭期内禁止登记照明巡查，到期或人工解封后恢复",
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

    private LightingInspectionDTO toDTO(PointLightingInspection inspection) {
        TreeNode point = treeNodeRepository.findByIdAndIsDeletedFalse(inspection.getPointId()).orElse(null);
        TreeNode section = point != null && point.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(point.getParentId()).orElse(null) : null;
        TreeNode district = section != null && section.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(section.getParentId()).orElse(null) : null;
        return LightingInspectionDTO.builder()
                .id(inspection.getId())
                .pointId(inspection.getPointId())
                .pointName(point != null ? point.getName() : "")
                .sectionName(section != null ? section.getName() : "")
                .districtName(district != null ? district.getName() : "")
                .inspectedAt(inspection.getInspectedAt())
                .result(inspection.getResult())
                .lampCount(inspection.getLampCount())
                .problemType(inspection.getProblemType())
                .description(inspection.getDescription())
                .inspector(inspection.getInspector())
                .createdAt(inspection.getCreatedAt())
                .build();
    }
}
