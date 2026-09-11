package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.InspectionCreateRequest;
import com.example.benchmanagement.dto.InspectionDTO;
import com.example.benchmanagement.dto.InspectionItemRequest;
import com.example.benchmanagement.entity.Bench;
import com.example.benchmanagement.entity.BenchInspection;
import com.example.benchmanagement.entity.RepairOrder;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.BenchInspectionRepository;
import com.example.benchmanagement.repository.BenchRepository;
import com.example.benchmanagement.repository.RepairOrderRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class InspectionService {

    private final BenchInspectionRepository inspectionRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final BenchRepository benchRepository;
    private final TreeNodeRepository treeNodeRepository;
    private final RepairOrderService repairOrderService;

    /**
     * 按街区/路段/点位发起巡检：保存范围内每张长凳的巡检记录，
     * 异常项可同步生成维修工单。
     */
    @Transactional
    public List<InspectionDTO> createInspection(InspectionCreateRequest request) {
        return createInspection(request, null);
    }

    /**
     * 发起巡检并关联来源巡检任务（任务派发场景）。
     * 先对全部巡检项做校验（长凳存在、在范围内、字段完整），
     * 全部通过后才批量写入；任一失败抛错回滚，不留下部分数据。
     */
    @Transactional
    public List<InspectionDTO> createInspection(InspectionCreateRequest request, Long taskId) {
        TreeNode scopeNode = treeNodeRepository.findByIdAndIsDeletedFalse(request.getScopeNodeId())
                .orElseThrow(() -> new IllegalArgumentException("巡检范围节点不存在"));
        if (scopeNode.getLevel() < 1 || scopeNode.getLevel() > 3) {
            throw new IllegalArgumentException("巡检范围必须是街区、路段或点位");
        }

        Set<Long> scopePointIds = new HashSet<>(collectScopePointIds(scopeNode));
        if (scopePointIds.isEmpty()) {
            throw new IllegalArgumentException("所选巡检范围下没有点位，无法发起巡检");
        }

        // 封闭点位禁止发起巡检；街区/路段范围下自动剔除封闭点位上的长凳
        assertScopeInspectable(scopeNode, scopePointIds);
        Set<Long> closedPointIds = new HashSet<>();
        for (TreeNode point : treeNodeRepository.findByIdsAndIsDeletedFalse(new ArrayList<>(scopePointIds))) {
            if (isPointClosed(point)) {
                closedPointIds.add(point.getId());
            }
        }
        if (!closedPointIds.isEmpty()) {
            scopePointIds.removeAll(closedPointIds);
            if (scopePointIds.isEmpty()) {
                throw new IllegalStateException("所选巡检范围内的点位均处于封闭期，无法发起巡检");
            }
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("巡检记录不能为空");
        }

        String inspector = (request.getInspector() == null || request.getInspector().isBlank())
                ? "system" : request.getInspector().trim();

        // 先逐项校验，全部通过后再写入，避免校验失败时已写入部分记录
        List<Bench> benches = new ArrayList<>();
        List<String> closedBenchCodes = new ArrayList<>();
        for (InspectionItemRequest item : request.getItems()) {
            Bench bench = benchRepository.findById(item.getBenchId())
                    .orElseThrow(() -> new IllegalArgumentException("长凳不存在: " + item.getBenchId()));
            if (!scopePointIds.contains(bench.getNodeId())) {
                if (closedPointIds.contains(bench.getNodeId())) {
                    closedBenchCodes.add(bench.getCode());
                } else {
                    throw new IllegalArgumentException(String.format(
                            "长凳【%s】不在所选巡检范围内", bench.getCode()));
                }
            } else {
                validateItem(item, item.getResult());
            }
            benches.add(bench);
        }
        if (!closedBenchCodes.isEmpty()) {
            throw new IllegalStateException("以下长凳所在点位处于临时封闭期，封闭期内禁止巡检："
                    + String.join("、", closedBenchCodes));
        }

        List<InspectionDTO> results = new ArrayList<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            InspectionItemRequest item = request.getItems().get(i);
            Bench bench = benches.get(i);
            int result = item.getResult();

            BenchInspection inspection = BenchInspection.builder()
                    .benchId(bench.getId())
                    .scopeNodeId(scopeNode.getId())
                    .inspectedAt(item.getInspectedAt())
                    .result(result)
                    .problemType(BenchInspection.RESULT_ABNORMAL == result ? trimToNull(item.getProblemType()) : null)
                    .severity(BenchInspection.RESULT_ABNORMAL == result ? item.getSeverity() : null)
                    .description(BenchInspection.RESULT_ABNORMAL == result ? trimToNull(item.getDescription()) : null)
                    .suggestion(trimToNull(item.getSuggestion()))
                    .inspector(inspector)
                    .taskId(taskId)
                    .build();
            BenchInspection saved = inspectionRepository.save(inspection);

            // 异常长凳可生成维修工单（同张长凳已有未完成工单则阻止）
            if (result == BenchInspection.RESULT_ABNORMAL
                    && Boolean.TRUE.equals(item.getCreateRepairOrder())) {
                RepairOrder order = repairOrderService.createFromInspection(saved, inspector);
                saved.setRepairOrderId(order.getId());
                inspectionRepository.save(saved);
            }

            log.info("保存巡检记录: benchId={}, result={}, scopeNodeId={}",
                    bench.getId(), result, scopeNode.getId());
            results.add(toDTO(saved));
        }
        return results;
    }

    public List<InspectionDTO> getInspections(Long nodeId, Integer result, Integer severity, String keyword) {
        List<BenchInspection> all;
        String scopeNodeName = null;
        if (nodeId != null) {
            TreeNode scopeNode = treeNodeRepository.findByIdAndIsDeletedFalse(nodeId)
                    .orElseThrow(() -> new IllegalArgumentException("节点不存在"));
            scopeNodeName = scopeNode.getName();
            List<Long> pointIds = collectScopePointIds(scopeNode);
            List<Long> benchIds = pointIds.isEmpty()
                    ? List.of()
                    : benchRepository.findByNodeIds(pointIds).stream().map(Bench::getId).toList();
            all = benchIds.isEmpty()
                    ? List.of()
                    : inspectionRepository.findByBenchIdInOrderByInspectedAtDescIdDesc(benchIds);
        } else {
            all = inspectionRepository.findAllByOrderByInspectedAtDescIdDesc();
        }

        final String finalScopeNodeName = scopeNodeName;
        return all.stream()
                .filter(i -> result == null || result.equals(i.getResult()))
                .filter(i -> severity == null || severity.equals(i.getSeverity()))
                .filter(i -> {
                    if (keyword == null || keyword.isBlank()) {
                        return true;
                    }
                    String kw = keyword.trim().toLowerCase();
                    Bench bench = benchRepository.findById(i.getBenchId()).orElse(null);
                    String code = bench != null ? bench.getCode().toLowerCase() : "";
                    String type = i.getProblemType() != null ? i.getProblemType().toLowerCase() : "";
                    return code.contains(kw) || type.contains(kw);
                })
                .map(i -> toDTO(i, finalScopeNodeName))
                .toList();
    }

    public InspectionDTO getInspectionById(Long id) {
        BenchInspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("巡检记录不存在"));
        return toDTO(inspection);
    }

    public List<InspectionDTO> getInspectionsByBench(Long benchId) {
        if (!benchRepository.existsById(benchId)) {
            throw new IllegalArgumentException("长凳不存在");
        }
        return inspectionRepository.findByBenchIdOrderByInspectedAtDescIdDesc(benchId)
                .stream().map(this::toDTO).toList();
    }

    /**
     * 批量取每排长凳的最近一次巡检记录。
     */
    public Map<Long, BenchInspection> latestInspectionMap(List<Long> benchIds) {
        Map<Long, BenchInspection> map = new HashMap<>();
        if (benchIds == null || benchIds.isEmpty()) {
            return map;
        }
        List<BenchInspection> list =
                inspectionRepository.findByBenchIdInOrderByInspectedAtDescIdDesc(benchIds);
        for (BenchInspection inspection : list) {
            map.putIfAbsent(inspection.getBenchId(), inspection);
        }
        return map;
    }

    private void validateItem(InspectionItemRequest item, int result) {
        if (result != BenchInspection.RESULT_NORMAL && result != BenchInspection.RESULT_ABNORMAL) {
            throw new IllegalArgumentException("巡检结果只能为正常(1)或异常(0)");
        }
        if (item.getInspectedAt() == null) {
            throw new IllegalArgumentException("检查时间不能为空");
        }
        if (result == BenchInspection.RESULT_ABNORMAL) {
            if (item.getProblemType() == null || item.getProblemType().isBlank()) {
                throw new IllegalArgumentException("巡检异常时必须选择问题类型");
            }
            if (item.getSeverity() == null) {
                throw new IllegalArgumentException("巡检异常时必须选择严重程度");
            }
            if (item.getSeverity() < 1 || item.getSeverity() > 3) {
                throw new IllegalArgumentException("严重程度只能为低(1)、中(2)、高(3)");
            }
            if (item.getDescription() == null || item.getDescription().isBlank()) {
                throw new IllegalArgumentException("巡检异常时必须填写问题描述");
            }
        }
    }

    /**
     * 收集范围节点（街区/路段/点位）下的全部点位ID。
     */
    public List<Long> collectScopePointIds(TreeNode node) {
        if (node.getLevel() == 3) {
            return List.of(node.getId());
        }
        List<TreeNode> children = treeNodeRepository.findByParentIdAndIsDeletedFalse(node.getId());
        List<Long> childIds = children.stream().map(TreeNode::getId).toList();
        if (node.getLevel() == 2) {
            return childIds;
        }
        // level=1 街区
        if (childIds.isEmpty()) {
            return List.of();
        }
        return treeNodeRepository.findByParentIdInAndIsDeletedFalse(childIds)
                .stream().map(TreeNode::getId).toList();
    }

    /**
     * 判断点位当前是否封闭（标记封闭且未到结束时间）。
     */
    public boolean isPointClosed(TreeNode point) {
        if (!Integer.valueOf(1).equals(point.getClosed())) {
            return false;
        }
        return point.getClosedEndAt() == null
                || point.getClosedEndAt().isAfter(java.time.LocalDateTime.now());
    }

    /**
     * 点位封闭期内禁止以该点位为范围发起巡检；
     * 街区/路段范围下若所有点位都在封闭，也无法发起。
     */
    private void assertScopeInspectable(TreeNode scopeNode, Set<Long> scopePointIds) {
        if (scopeNode.getLevel() == 3 && isPointClosed(scopeNode)) {
            throw new IllegalStateException(String.format(
                    "点位【%s】处于临时封闭期（截止%s），封闭期内禁止发起巡检，到期或人工解封后恢复",
                    scopeNode.getName(),
                    scopeNode.getClosedEndAt() != null ? scopeNode.getClosedEndAt() : "未设置"));
        }
        List<TreeNode> points = treeNodeRepository.findByIdsAndIsDeletedFalse(new ArrayList<>(scopePointIds));
        List<String> closedNames = points.stream()
                .filter(this::isPointClosed)
                .map(TreeNode::getName)
                .sorted()
                .toList();
        if (!closedNames.isEmpty() && closedNames.size() == points.size()) {
            throw new IllegalStateException("所选范围内的点位全部处于封闭期（"
                    + String.join("、", closedNames) + "），无法发起巡检");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private InspectionDTO toDTO(BenchInspection inspection) {
        return toDTO(inspection, null);
    }

    private InspectionDTO toDTO(BenchInspection inspection, String scopeNodeNameOverride) {
        Bench bench = benchRepository.findById(inspection.getBenchId()).orElse(null);
        Long nodeId = bench != null ? bench.getNodeId() : null;
        TreeNode point = nodeId != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(nodeId).orElse(null) : null;
        TreeNode section = point != null && point.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(point.getParentId()).orElse(null) : null;
        TreeNode district = section != null && section.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(section.getParentId()).orElse(null) : null;
        TreeNode scopeNode = inspection.getScopeNodeId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(inspection.getScopeNodeId()).orElse(null) : null;

        RepairOrder order = inspection.getRepairOrderId() != null
                ? repairOrderRepository.findById(inspection.getRepairOrderId()).orElse(null) : null;

        return InspectionDTO.builder()
                .id(inspection.getId())
                .benchId(inspection.getBenchId())
                .benchCode(bench != null ? bench.getCode() : "")
                .taskId(inspection.getTaskId())
                .scopeNodeId(inspection.getScopeNodeId())
                .scopeNodeName(scopeNodeNameOverride != null ? scopeNodeNameOverride
                        : scopeNode != null ? scopeNode.getName() : "")
                .inspectedAt(inspection.getInspectedAt())
                .result(inspection.getResult())
                .problemType(inspection.getProblemType())
                .severity(inspection.getSeverity())
                .description(inspection.getDescription())
                .suggestion(inspection.getSuggestion())
                .inspector(inspection.getInspector())
                .repairOrderId(inspection.getRepairOrderId())
                .repairOrderCode(order != null ? order.getCode() : null)
                .repairOrderStatus(order != null ? order.getStatus() : null)
                .nodeName(point != null ? point.getName() : "")
                .sectionName(section != null ? section.getName() : "")
                .districtName(district != null ? district.getName() : "")
                .createdAt(inspection.getCreatedAt())
                .build();
    }
}
