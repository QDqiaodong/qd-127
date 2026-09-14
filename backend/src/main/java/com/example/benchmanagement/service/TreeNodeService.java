package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.CapacityAdjustRequest;
import com.example.benchmanagement.dto.NodeCapacityLogDTO;
import com.example.benchmanagement.dto.NodeClosureLogDTO;
import com.example.benchmanagement.dto.PointAntiSlipMatDTO;
import com.example.benchmanagement.dto.PointClosureRequest;
import com.example.benchmanagement.dto.PointReopenRequest;
import com.example.benchmanagement.dto.SectionAdditionalBenchSummaryDTO;
import com.example.benchmanagement.dto.SectionCapacityAlarmDTO;
import com.example.benchmanagement.dto.TreeNodeDTO;
import com.example.benchmanagement.entity.NodeCapacityLog;
import com.example.benchmanagement.entity.NodeClosureLog;
import com.example.benchmanagement.entity.BenchSponsorship;
import com.example.benchmanagement.entity.PointLightingInspection;
import com.example.benchmanagement.entity.PointSunshadeInspection;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.BenchRepository;
import com.example.benchmanagement.repository.NodeCapacityLogRepository;
import com.example.benchmanagement.repository.NodeClosureLogRepository;
import com.example.benchmanagement.repository.PointLightingInspectionRepository;
import com.example.benchmanagement.repository.PointSunshadeInspectionRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TreeNodeService {

    private final TreeNodeRepository treeNodeRepository;
    private final BenchRepository benchRepository;
    private final NodeCapacityLogRepository capacityLogRepository;
    private final NodeClosureLogRepository closureLogRepository;
    private final PointLightingInspectionRepository lightingInspectionRepository;
    private final PointSunshadeInspectionRepository sunshadeInspectionRepository;
    private final AdditionalBenchPlanService additionalBenchPlanService;
    private final BenchSponsorshipService benchSponsorshipService;
    private AntiSlipMatService antiSlipMatService;

    @Autowired
    public void setAntiSlipMatService(@Lazy AntiSlipMatService antiSlipMatService) {
        this.antiSlipMatService = antiSlipMatService;
    }

    public List<TreeNodeDTO> getTree() {
        List<TreeNode> allNodes = treeNodeRepository.findAllActiveNodes();
        Map<Long, TreeNodeDTO> nodeMap = new HashMap<>();
        List<TreeNodeDTO> rootNodes = new ArrayList<>();
        List<TreeNodeDTO> pointDtos = new ArrayList<>();

        for (TreeNode node : allNodes) {
            TreeNodeDTO dto = toDTO(node);
            if (node.getLevel() == 3) {
                pointDtos.add(dto);
            }
            nodeMap.put(node.getId(), dto);

            if (node.getParentId() == null) {
                rootNodes.add(dto);
            } else {
                TreeNodeDTO parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(dto);
                }
            }
        }

        fillPointStatus(pointDtos);
        fillSectionLightingAbnormalCount(nodeMap.values());
        fillSectionSunshadeAbnormalCount(nodeMap.values());
        fillSectionCapacityAlarm(nodeMap.values());
        fillSectionAdditionalBenchPlans(nodeMap.values());
        fillSectionSponsorshipExpiring(nodeMap.values());
        fillSectionAntiSlipMatOutstanding(nodeMap.values());

        rootNodes.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        for (TreeNodeDTO node : nodeMap.values()) {
            node.getChildren().sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        }

        return rootNodes;
    }

    public List<TreeNodeDTO> getNodesByLevel(Integer level) {
        List<TreeNode> nodes = treeNodeRepository.findByLevelAndIsDeletedFalse(level);
        List<TreeNodeDTO> dtos = nodes.stream().map(this::toDTO).toList();
        if (level == 3) {
            fillPointStatus(dtos);
        }
        if (level == 2) {
            fillSectionLightingAbnormalCountByQuery(dtos);
            fillSectionSunshadeAbnormalCountByQuery(dtos);
            fillSectionCapacityAlarmByQuery(dtos);
            fillSectionAdditionalBenchPlans(dtos);
            fillSectionSponsorshipExpiringByQuery(dtos);
            fillSectionAntiSlipMatOutstandingByQuery(dtos);
        }
        return dtos;
    }

    public List<TreeNodeDTO> getChildren(Long parentId) {
        List<TreeNode> nodes = treeNodeRepository.findByParentIdAndIsDeletedFalse(parentId);
        List<TreeNodeDTO> dtos = nodes.stream().map(this::toDTO).toList();
        List<TreeNodeDTO> pointDtos = dtos.stream().filter(d -> d.getLevel() == 3).toList();
        if (!pointDtos.isEmpty()) {
            fillPointStatus(pointDtos);
        }
        List<TreeNodeDTO> sectionDtos = dtos.stream().filter(d -> d.getLevel() == 2).toList();
        if (!sectionDtos.isEmpty()) {
            fillSectionLightingAbnormalCountByQuery(sectionDtos);
            fillSectionSunshadeAbnormalCountByQuery(sectionDtos);
            fillSectionCapacityAlarmByQuery(sectionDtos);
            fillSectionAdditionalBenchPlans(sectionDtos);
            fillSectionSponsorshipExpiringByQuery(sectionDtos);
            fillSectionAntiSlipMatOutstandingByQuery(sectionDtos);
        }
        return dtos;
    }

    @Transactional
    public TreeNodeDTO createNode(TreeNodeDTO dto) {
        validateNodeLevel(dto.getParentId(), dto.getLevel());

        if (dto.getParentId() != null) {
            TreeNode parent = treeNodeRepository.findByIdAndIsDeletedFalse(dto.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("父节点不存在"));
            if (parent.getLevel() >= 3) {
                throw new IllegalArgumentException("点位下不能再创建子节点");
            }
        }

        if (treeNodeRepository.existsByParentIdAndNameAndIsDeletedFalse(dto.getParentId(), dto.getName())) {
            throw new IllegalArgumentException("同级节点名称已存在");
        }

        Integer capacity = null;
        if (dto.getLevel() == 3) {
            capacity = dto.getCapacity() != null ? dto.getCapacity() : TreeNode.DEFAULT_CAPACITY;
            if (capacity < 0) {
                throw new IllegalArgumentException("容量必须为大于等于0的整数");
            }
        }

        TreeNode node = TreeNode.builder()
                .parentId(dto.getParentId())
                .level(dto.getLevel())
                .name(dto.getName())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .capacity(capacity)
                .build();

        TreeNode saved = treeNodeRepository.save(node);

        if (saved.getLevel() == 3) {
            NodeCapacityLog logEntry = NodeCapacityLog.builder()
                    .nodeId(saved.getId())
                    .oldCapacity(null)
                    .newCapacity(saved.getCapacity())
                    .occupiedCount(0)
                    .adjustReason("新建点位，初始化容量")
                    .adjustedBy("system")
                    .build();
            capacityLogRepository.save(logEntry);
            saved.setCapacityUpdatedAt(logEntry.getAdjustedAt());
            saved.setCapacityUpdatedReason(logEntry.getAdjustReason());
            treeNodeRepository.save(saved);
        }

        log.info("创建树形节点: id={}, name={}, level={}, capacity={}",
                saved.getId(), saved.getName(), saved.getLevel(), saved.getCapacity());
        return toDTOWithCapacity(saved);
    }

    @Transactional
    public TreeNodeDTO updateNode(Long id, TreeNodeDTO dto) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));

        if (dto.getName() != null && !dto.getName().equals(node.getName())) {
            if (treeNodeRepository.existsByParentIdAndNameAndIsDeletedFalse(node.getParentId(), dto.getName())) {
                throw new IllegalArgumentException("同级节点名称已存在");
            }
            node.setName(dto.getName());
        }

        if (dto.getSortOrder() != null) {
            node.setSortOrder(dto.getSortOrder());
        }

        TreeNode saved = treeNodeRepository.save(node);
        log.info("更新树形节点: id={}, name={}", saved.getId(), saved.getName());
        return toDTOWithCapacity(saved);
    }

    /**
     * 调整点位容量，保留调整时间与原因，并校验容量不能小于当前占用数。
     */
    @Transactional
    public TreeNodeDTO adjustCapacity(Long id, CapacityAdjustRequest request) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("点位不存在"));
        if (node.getLevel() != 3) {
            throw new IllegalArgumentException("只有点位(level=3)可以设置容量");
        }

        String reason = request.getAdjustReason() == null ? "" : request.getAdjustReason().trim();
        if (reason.isEmpty()) {
            throw new IllegalArgumentException("调整原因不能为空");
        }

        int newCapacity = request.getCapacity();
        int effectiveCapacity = node.getCapacity() != null ? node.getCapacity() : TreeNode.DEFAULT_CAPACITY;
        if (newCapacity == effectiveCapacity) {
            throw new IllegalArgumentException("新容量与当前容量一致，无需调整");
        }

        long occupied = benchRepository.countActiveByNodeId(id);
        if (newCapacity < occupied) {
            throw new IllegalArgumentException(String.format(
                    "容量调整失败：当前点位已摆放%d张在用长凳，容量不能低于当前占用数", occupied));
        }

        Integer oldCapacity = node.getCapacity();
        node.setCapacity(newCapacity);
        node.setCapacityUpdatedReason(reason);
        TreeNode saved = treeNodeRepository.save(node);

        NodeCapacityLog logEntry = NodeCapacityLog.builder()
                .nodeId(id)
                .oldCapacity(oldCapacity)
                .newCapacity(newCapacity)
                .occupiedCount((int) occupied)
                .adjustReason(reason)
                .adjustedBy("system")
                .build();
        capacityLogRepository.save(logEntry);

        saved.setCapacityUpdatedAt(logEntry.getAdjustedAt());
        treeNodeRepository.save(saved);

        log.info("调整点位容量: nodeId={}, oldCapacity={}, newCapacity={}, occupied={}",
                id, oldCapacity, newCapacity, occupied);
        return toDTOWithCapacity(saved);
    }

    public List<NodeCapacityLogDTO> getCapacityLogs(Long nodeId) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("点位不存在"));
        List<NodeCapacityLog> logs = capacityLogRepository.findByNodeIdOrderByAdjustedAtDesc(nodeId);
        return logs.stream()
                .map(l -> NodeCapacityLogDTO.builder()
                        .id(l.getId())
                        .nodeId(l.getNodeId())
                        .nodeName(node.getName())
                        .oldCapacity(l.getOldCapacity())
                        .newCapacity(l.getNewCapacity())
                        .occupiedCount(l.getOccupiedCount())
                        .adjustReason(l.getAdjustReason())
                        .adjustedAt(l.getAdjustedAt())
                        .adjustedBy(l.getAdjustedBy())
                        .build())
                .toList();
    }

    /**
     * 临时封闭点位：填写起止时间和原因；封闭期内禁止调入长凳、禁止以该点位发起巡检或执行待办任务。
     */
    @Transactional
    public TreeNodeDTO closePoint(Long id, PointClosureRequest request) {
        TreeNode node = requirePoint(id);
        if (isClosed(node)) {
            throw new IllegalStateException("点位已处于封闭状态，请勿重复封闭，如需调整请先解封");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = request.getStartAt();
        LocalDateTime endAt = request.getEndAt();
        if (endAt.isBefore(startAt) || endAt.isEqual(startAt)) {
            throw new IllegalArgumentException("封闭结束时间必须晚于开始时间");
        }
        if (endAt.isBefore(now)) {
            throw new IllegalArgumentException("封闭结束时间不能早于当前时间");
        }

        String reason = request.getReason().trim();
        node.setClosed(1);
        node.setClosedStartAt(startAt);
        node.setClosedEndAt(endAt);
        node.setClosedReason(reason);
        TreeNode saved = treeNodeRepository.save(node);

        closureLogRepository.save(NodeClosureLog.builder()
                .nodeId(id)
                .actionType(NodeClosureLog.ACTION_CLOSE)
                .closedStartAt(startAt)
                .closedEndAt(endAt)
                .closedReason(reason)
                .operatedBy("system")
                .build());

        log.info("点位临时封闭: nodeId={}, startAt={}, endAt={}, reason={}", id, startAt, endAt, reason);
        return toDTOWithCapacity(saved);
    }

    /**
     * 人工解封点位，解封原因记入台账；解封后恢复调入长凳与巡检。
     */
    @Transactional
    public TreeNodeDTO reopenPoint(Long id, PointReopenRequest request) {
        TreeNode node = requirePoint(id);
        if (!Integer.valueOf(1).equals(node.getClosed())) {
            throw new IllegalStateException("点位当前未处于封闭状态，无需解封");
        }

        String reason = request.getReason().trim();
        LocalDateTime startAt = node.getClosedStartAt();
        LocalDateTime endAt = node.getClosedEndAt();
        String closedReason = node.getClosedReason();

        node.setClosed(0);
        node.setClosedStartAt(null);
        node.setClosedEndAt(null);
        node.setClosedReason(null);
        TreeNode saved = treeNodeRepository.save(node);

        closureLogRepository.save(NodeClosureLog.builder()
                .nodeId(id)
                .actionType(NodeClosureLog.ACTION_REOPEN_MANUAL)
                .closedStartAt(startAt)
                .closedEndAt(endAt)
                .closedReason(closedReason)
                .reopenReason(reason)
                .operatedBy("system")
                .build());

        log.info("点位人工解封: nodeId={}, reason={}", id, reason);
        return toDTOWithCapacity(saved);
    }

    public List<NodeClosureLogDTO> getClosureLogs(Long nodeId) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("点位不存在"));
        return closureLogRepository.findByNodeIdOrderByOperatedAtDescIdDesc(nodeId).stream()
                .map(l -> toClosureLogDTO(l, node.getName()))
                .toList();
    }

    public List<NodeClosureLogDTO> getAllClosureLogs() {
        Map<Long, String> nodeNames = new HashMap<>();
        return closureLogRepository.findAllByOrderByOperatedAtDescIdDesc().stream()
                .map(l -> toClosureLogDTO(l, nodeNames.computeIfAbsent(l.getNodeId(), nid ->
                        treeNodeRepository.findById(nid).map(TreeNode::getName).orElse("已删除点位"))))
                .toList();
    }

    /**
     * 到期自动解封：把已过结束时间但仍标记封闭的点位解除封闭并登记台账。
     * 由定时任务及应用启动时调用。
     */
    @Transactional
    public int autoExpireClosedPoints() {
        LocalDateTime now = LocalDateTime.now();
        int expired = 0;
        for (TreeNode node : treeNodeRepository.findByLevelAndIsDeletedFalse(3)) {
            if (Integer.valueOf(1).equals(node.getClosed())
                    && node.getClosedEndAt() != null
                    && !node.getClosedEndAt().isAfter(now)) {
                LocalDateTime startAt = node.getClosedStartAt();
                LocalDateTime endAt = node.getClosedEndAt();
                String closedReason = node.getClosedReason();

                node.setClosed(0);
                node.setClosedStartAt(null);
                node.setClosedEndAt(null);
                node.setClosedReason(null);
                treeNodeRepository.save(node);

                closureLogRepository.save(NodeClosureLog.builder()
                        .nodeId(node.getId())
                        .actionType(NodeClosureLog.ACTION_REOPEN_AUTO)
                        .closedStartAt(startAt)
                        .closedEndAt(endAt)
                        .closedReason(closedReason)
                        .operatedBy("system")
                        .build());
                expired++;
                log.info("点位封闭到期自动解封: nodeId={}, endAt={}", node.getId(), endAt);
            }
        }
        return expired;
    }

    /**
     * 判断点位当前是否封闭（含到期自动解封判断）；到期但尚未落库时按未封闭处理。
     */
    public boolean isPointClosed(Long pointId) {
        return treeNodeRepository.findByIdAndIsDeletedFalse(pointId)
                .filter(this::isClosed)
                .isPresent();
    }

    private TreeNode requirePoint(Long id) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("点位不存在"));
        if (node.getLevel() != 3) {
            throw new IllegalArgumentException("只有点位(level=3)可以设置封闭");
        }
        return node;
    }

    /**
     * 封闭状态的实时判断：标记封闭且未到结束时间。
     */
    private boolean isClosed(TreeNode node) {
        if (!Integer.valueOf(1).equals(node.getClosed())) {
            return false;
        }
        return node.getClosedEndAt() == null || node.getClosedEndAt().isAfter(LocalDateTime.now());
    }

    private NodeClosureLogDTO toClosureLogDTO(NodeClosureLog l, String nodeName) {
        String label = switch (l.getActionType()) {
            case NodeClosureLog.ACTION_CLOSE -> "封闭";
            case NodeClosureLog.ACTION_REOPEN_MANUAL -> "人工解封";
            case NodeClosureLog.ACTION_REOPEN_AUTO -> "到期自动解封";
            default -> "未知";
        };
        return NodeClosureLogDTO.builder()
                .id(l.getId())
                .nodeId(l.getNodeId())
                .nodeName(nodeName)
                .actionType(l.getActionType())
                .actionTypeLabel(label)
                .closedStartAt(l.getClosedStartAt())
                .closedEndAt(l.getClosedEndAt())
                .closedReason(l.getClosedReason())
                .reopenReason(l.getReopenReason())
                .operatedAt(l.getOperatedAt())
                .operatedBy(l.getOperatedBy())
                .build();
    }

    @Transactional
    public void deleteNode(Long id) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));

        if (node.getLevel() < 3) {
            long childCount = treeNodeRepository.countByParentId(id);
            if (childCount > 0) {
                throw new IllegalArgumentException("该节点下存在子节点，无法删除");
            }
        }

        if (node.getLevel() == 2) {
            additionalBenchPlanService.validateSectionCanDelete(id);
        }

        if (node.getLevel() == 3) {
            benchSponsorshipService.validatePointCanDelete(id);
            antiSlipMatService.validatePointCanDelete(id);
            long occupied = benchRepository.countByNodeId(id);
            if (occupied > 0) {
                throw new IllegalArgumentException(String.format(
                        "该点位下仍有%d张长凳，无法删除", occupied));
            }
        }

        node.setIsDeleted(true);
        treeNodeRepository.save(node);
        log.info("删除树形节点: id={}, name={}", id, node.getName());
    }

    public TreeNodeDTO getNodeById(Long id) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));
        return toDTOWithCapacity(node);
    }

    private void validateNodeLevel(Long parentId, Integer level) {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("层级必须为1、2或3");
        }

        if (parentId == null) {
            if (level != 1) {
                throw new IllegalArgumentException("根节点层级必须为1");
            }
        } else {
            TreeNode parent = treeNodeRepository.findByIdAndIsDeletedFalse(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("父节点不存在"));
            if (level != parent.getLevel() + 1) {
                throw new IllegalArgumentException("子节点层级必须为父节点层级+1");
            }
        }
    }

    /**
     * 批量填充点位的占用/容量/封闭状态及最近一次夜间照明结论。
     */
    private void fillPointStatus(List<TreeNodeDTO> points) {
        fillCapacityStatus(points);
        fillLightingStatus(points);
        fillSunshadeStatus(points);
        fillPointSponsorshipExpiring(points);
        fillPointAntiSlipMat(points);
    }

    /**
     * 批量填充点位的占用数、剩余数、有效容量及封闭状态。
     */
    private void fillCapacityStatus(List<TreeNodeDTO> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        List<Long> pointIds = points.stream().map(TreeNodeDTO::getId).toList();
        Map<Long, TreeNode> nodeMap = new HashMap<>();
        for (TreeNode node : treeNodeRepository.findByIdsAndIsDeletedFalse(pointIds)) {
            nodeMap.put(node.getId(), node);
        }
        Map<Long, Long> countMap = new HashMap<>();
        for (Object[] row : benchRepository.countActiveByNodeIds(pointIds)) {
            countMap.put((Long) row[0], (Long) row[1]);
        }
        for (TreeNodeDTO point : points) {
            TreeNode node = nodeMap.get(point.getId());
            int capacity = point.getCapacity() != null ? point.getCapacity() : TreeNode.DEFAULT_CAPACITY;
            point.setCapacity(capacity);
            long occupied = countMap.getOrDefault(point.getId(), 0L);
            point.setOccupiedCount(occupied);
            long remaining = Math.max(0, capacity - occupied);
            point.setRemainingCount(remaining);
            point.setCapacityStatus(capacityStatusOf(remaining));
            boolean closed = node != null && isClosed(node);
            point.setClosed(closed);
            if (closed) {
                point.setClosedStartAt(node.getClosedStartAt());
                point.setClosedEndAt(node.getClosedEndAt());
                point.setClosedReason(node.getClosedReason());
            }
        }
    }

    /**
     * 点位容量状态：剩余0为已满，剩余小于等于将满阈值为将满，否则为充足（返回 null）。
     */
    private String capacityStatusOf(long remaining) {
        if (remaining == 0) {
            return TreeNode.CAPACITY_STATUS_FULL;
        }
        if (remaining <= TreeNode.POINT_NEARLY_FULL_THRESHOLD) {
            return TreeNode.CAPACITY_STATUS_NEARLY_FULL;
        }
        return null;
    }

    /**
     * 批量填充点位最近一次夜间照明结论（树标记与照明巡查记录同源，刷新后保持一致）。
     */
    private void fillLightingStatus(List<TreeNodeDTO> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        List<Long> pointIds = points.stream().map(TreeNodeDTO::getId).toList();
        Map<Long, PointLightingInspection> latestMap = new HashMap<>();
        for (PointLightingInspection inspection
                : lightingInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(pointIds)) {
            latestMap.putIfAbsent(inspection.getPointId(), inspection);
        }
        for (TreeNodeDTO point : points) {
            PointLightingInspection latest = latestMap.get(point.getId());
            if (latest != null) {
                point.setLightingResult(latest.getResult());
                point.setLightingProblemType(latest.getProblemType());
                point.setLightingInspectedAt(latest.getInspectedAt());
            }
        }
    }

    /**
     * 填充路段的照明异常点数：路段下最近一次照明结论为异常的点位数。
     * 直接统计树上已填充的点位照明标记，保证路段计数与点位标记同源一致。
     */
    private void fillSectionLightingAbnormalCount(java.util.Collection<TreeNodeDTO> nodes) {
        for (TreeNodeDTO node : nodes) {
            if (node.getLevel() != null && node.getLevel() == 2) {
                long count = node.getChildren().stream()
                        .filter(p -> Integer.valueOf(PointLightingInspection.RESULT_ABNORMAL)
                                .equals(p.getLightingResult()))
                        .count();
                node.setLightingAbnormalCount((int) count);
            }
        }
    }

    /**
     * 平铺返回路段列表时，按路段id批量查询点位最近一次照明结论并填充异常点数，
     * 口径与树上路段计数一致。
     */
    private void fillSectionLightingAbnormalCountByQuery(List<TreeNodeDTO> sections) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        List<Long> sectionIds = sections.stream().map(TreeNodeDTO::getId).toList();
        List<TreeNode> points = treeNodeRepository.findByParentIdInAndIsDeletedFalse(sectionIds);
        Map<Long, Integer> abnormalBySection = new HashMap<>();
        if (!points.isEmpty()) {
            List<Long> pointIds = points.stream().map(TreeNode::getId).toList();
            Map<Long, PointLightingInspection> latestMap = new HashMap<>();
            for (PointLightingInspection inspection
                    : lightingInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(pointIds)) {
                latestMap.putIfAbsent(inspection.getPointId(), inspection);
            }
            for (TreeNode point : points) {
                PointLightingInspection latest = latestMap.get(point.getId());
                if (latest != null && latest.getResult() == PointLightingInspection.RESULT_ABNORMAL) {
                    abnormalBySection.merge(point.getParentId(), 1, Integer::sum);
                }
            }
        }
        for (TreeNodeDTO section : sections) {
            section.setLightingAbnormalCount(abnormalBySection.getOrDefault(section.getId(), 0));
        }
    }

    /**
     * 批量填充点位最近一次遮阳棚结论（树标记与遮阳棚巡查记录同源，刷新后保持一致）。
     */
    private void fillSunshadeStatus(List<TreeNodeDTO> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        List<Long> pointIds = points.stream().map(TreeNodeDTO::getId).toList();
        Map<Long, PointSunshadeInspection> latestMap = new HashMap<>();
        for (PointSunshadeInspection inspection
                : sunshadeInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(pointIds)) {
            latestMap.putIfAbsent(inspection.getPointId(), inspection);
        }
        for (TreeNodeDTO point : points) {
            PointSunshadeInspection latest = latestMap.get(point.getId());
            if (latest != null) {
                point.setSunshadeResult(latest.getResult());
                point.setSunshadeDamagedArea(latest.getDamagedArea());
                point.setSunshadeDamagedLocation(latest.getDamagedLocation());
                point.setSunshadeInspectedAt(latest.getInspectedAt());
            }
        }
    }

    /**
     * 填充路段的遮阳棚异常点数：路段下最近一次遮阳棚结论为异常的点位数。
     * 直接统计树上已填充的点位遮阳棚标记，保证路段计数与点位标记同源一致。
     */
    private void fillSectionSunshadeAbnormalCount(java.util.Collection<TreeNodeDTO> nodes) {
        for (TreeNodeDTO node : nodes) {
            if (node.getLevel() != null && node.getLevel() == 2) {
                long count = node.getChildren().stream()
                        .filter(p -> Integer.valueOf(PointSunshadeInspection.RESULT_ABNORMAL)
                                .equals(p.getSunshadeResult()))
                        .count();
                node.setSunshadeAbnormalCount((int) count);
            }
        }
    }

    /**
     * 平铺返回路段列表时，按路段id批量查询点位最近一次遮阳棚结论并填充异常点数，
     * 口径与树上路段计数一致。
     */
    private void fillSectionSunshadeAbnormalCountByQuery(List<TreeNodeDTO> sections) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        List<Long> sectionIds = sections.stream().map(TreeNodeDTO::getId).toList();
        List<TreeNode> points = treeNodeRepository.findByParentIdInAndIsDeletedFalse(sectionIds);
        Map<Long, Integer> abnormalBySection = new HashMap<>();
        if (!points.isEmpty()) {
            List<Long> pointIds = points.stream().map(TreeNode::getId).toList();
            Map<Long, PointSunshadeInspection> latestMap = new HashMap<>();
            for (PointSunshadeInspection inspection
                    : sunshadeInspectionRepository.findByPointIdInOrderByInspectedAtDescIdDesc(pointIds)) {
                latestMap.putIfAbsent(inspection.getPointId(), inspection);
            }
            for (TreeNode point : points) {
                PointSunshadeInspection latest = latestMap.get(point.getId());
                if (latest != null && latest.getResult() == PointSunshadeInspection.RESULT_ABNORMAL) {
                    abnormalBySection.merge(point.getParentId(), 1, Integer::sum);
                }
            }
        }
        for (TreeNodeDTO section : sections) {
            section.setSunshadeAbnormalCount(abnormalBySection.getOrDefault(section.getId(), 0));
        }
    }

    /**
     * 填充路段的容量告警：直接汇总树上已填充的点位剩余容量，
     * 保证路段告警标记与点位容量标记同源一致。
     * 没有点位、或没有已满/将满点位的路段不出告警。
     */
    private void fillSectionCapacityAlarm(java.util.Collection<TreeNodeDTO> nodes) {
        for (TreeNodeDTO node : nodes) {
            if (node.getLevel() != null && node.getLevel() == 2) {
                applySectionCapacityStats(node, node.getChildren());
            }
        }
    }

    /**
     * 平铺返回路段列表时，按路段id批量查询点位占用并填充容量告警，
     * 口径与树上路段告警一致。
     */
    private void fillSectionCapacityAlarmByQuery(List<TreeNodeDTO> sections) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        List<Long> sectionIds = sections.stream().map(TreeNodeDTO::getId).toList();
        List<TreeNode> points = treeNodeRepository.findByParentIdInAndIsDeletedFalse(sectionIds);
        Map<Long, List<TreeNodeDTO>> pointsBySection = new HashMap<>();
        if (!points.isEmpty()) {
            List<TreeNodeDTO> pointDtos = points.stream().map(this::toDTO).toList();
            fillCapacityStatus(pointDtos);
            for (TreeNodeDTO point : pointDtos) {
                pointsBySection.computeIfAbsent(point.getParentId(), k -> new ArrayList<>()).add(point);
            }
        }
        for (TreeNodeDTO section : sections) {
            applySectionCapacityStats(section, pointsBySection.getOrDefault(section.getId(), List.of()));
        }
    }

    /**
     * 路段容量告警统一口径：有点位、剩余容量加总低于阈值且至少存在一个已满/将满点位时才告警。
     * 没有点位，或加总虽低于阈值但各点位均为充足状态的路段不出告警，
     * 保证树上告警标记点开后一定能在下钻的已满/将满名单中对上点位。
     */
    private void applySectionCapacityStats(TreeNodeDTO section, List<TreeNodeDTO> points) {
        section.setCapacityAlarmThreshold(TreeNode.SECTION_CAPACITY_ALARM_THRESHOLD);
        if (points == null || points.isEmpty()) {
            section.setCapacityRemainingSum(null);
            section.setCapacityAlarm(false);
            section.setCapacityFullCount(0);
            section.setCapacityNearlyFullCount(0);
            return;
        }
        long remainingSum = 0;
        int fullCount = 0;
        int nearlyFullCount = 0;
        for (TreeNodeDTO point : points) {
            long remaining = point.getRemainingCount() != null ? point.getRemainingCount() : 0;
            remainingSum += remaining;
            if (TreeNode.CAPACITY_STATUS_FULL.equals(point.getCapacityStatus())) {
                fullCount++;
            } else if (TreeNode.CAPACITY_STATUS_NEARLY_FULL.equals(point.getCapacityStatus())) {
                nearlyFullCount++;
            }
        }
        section.setCapacityRemainingSum(remainingSum);
        // 必须存在已满/将满点位才告警，否则会出现"路段有告警标记、下钻名单却为空"
        boolean hasFullOrNearlyFullPoint = fullCount + nearlyFullCount > 0;
        section.setCapacityAlarm(hasFullOrNearlyFullPoint
                && remainingSum < TreeNode.SECTION_CAPACITY_ALARM_THRESHOLD);
        section.setCapacityFullCount(fullCount);
        section.setCapacityNearlyFullCount(nearlyFullCount);
    }

    /**
     * 批量填充路段加凳待投放数量与逾期预案数；街区树标记和加凳预案列表共用同一服务口径。
     */
    private void fillSectionAdditionalBenchPlans(java.util.Collection<TreeNodeDTO> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        List<Long> sectionIds = nodes.stream()
                .filter(node -> node.getLevel() != null && node.getLevel() == 2)
                .map(TreeNodeDTO::getId)
                .toList();
        if (sectionIds.isEmpty()) {
            return;
        }
        Map<Long, SectionAdditionalBenchSummaryDTO> summaries =
                additionalBenchPlanService.getSectionSummaries(sectionIds);
        if (summaries == null) {
            return;
        }
        summaries.forEach((sectionId, summary) ->
                nodes.stream()
                        .filter(node -> sectionId.equals(node.getId()))
                        .findFirst()
                        .ifPresent(node -> {
                            node.setAdditionalBenchPendingCount(summary.getPendingCount());
                            node.setAdditionalBenchOverduePlanCount(summary.getOverduePlanCount().intValue());
                        }));
    }

    /**
     * 批量填充点位冠名临期提醒。标记来自冠名起止日期的动态推导，
     * 刷新页面或服务重启后仍会按当天日期重新计算并保留。
     */
    private void fillPointSponsorshipExpiring(List<TreeNodeDTO> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        List<Long> pointIds = points.stream().map(TreeNodeDTO::getId).toList();
        Map<Long, List<BenchSponsorship>> sponsorshipsByPoint = new HashMap<>();
        for (BenchSponsorship sponsorship
                : benchSponsorshipService.getExpiringSoonSponsorships(pointIds, today)) {
            sponsorshipsByPoint.computeIfAbsent(sponsorship.getPointId(), k -> new ArrayList<>()).add(sponsorship);
        }

        for (TreeNodeDTO point : points) {
            List<BenchSponsorship> expiringSponsorships =
                    sponsorshipsByPoint.getOrDefault(point.getId(), List.of());
            point.setSponsorshipExpiringCount(expiringSponsorships.size());
            if (expiringSponsorships.isEmpty()) {
                point.setSponsorshipNearestEndDate(null);
                point.setSponsorshipNearestDaysRemaining(null);
                point.setSponsorshipNearestMerchantName(null);
                point.setSponsorshipNearestText(null);
                continue;
            }

            BenchSponsorship nearest = expiringSponsorships.get(0);
            point.setSponsorshipNearestEndDate(nearest.getEndDate());
            point.setSponsorshipNearestDaysRemaining(ChronoUnit.DAYS.between(today, nearest.getEndDate()));
            point.setSponsorshipNearestMerchantName(nearest.getMerchantName());
            point.setSponsorshipNearestText(nearest.getSponsorshipText());
        }
    }

    /**
     * 街区树完整返回时，路段临期冠名数直接汇总其点位上已填充的临期标记，保证树节点同源。
     */
    private void fillSectionSponsorshipExpiring(java.util.Collection<TreeNodeDTO> nodes) {
        for (TreeNodeDTO node : nodes) {
            if (node.getLevel() != null && node.getLevel() == 2) {
                int count = node.getChildren().stream()
                        .map(TreeNodeDTO::getSponsorshipExpiringCount)
                        .filter(java.util.Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum();
                node.setSponsorshipExpiringSectionCount(count);
            }
        }
    }

    /**
     * 平铺返回路段时，按路段下点位数批量查询临期冠名并汇总记录数，
     * 口径与街区树完整返回一致。
     */
    private void fillSectionSponsorshipExpiringByQuery(List<TreeNodeDTO> sections) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        List<Long> sectionIds = sections.stream().map(TreeNodeDTO::getId).toList();
        List<TreeNode> points = treeNodeRepository.findByParentIdInAndIsDeletedFalse(sectionIds);
        Map<Long, Long> countBySection = new HashMap<>();
        if (!points.isEmpty()) {
            LocalDate today = LocalDate.now();
            List<Long> pointIds = points.stream().map(TreeNode::getId).toList();
            for (BenchSponsorship sponsorship
                    : benchSponsorshipService.getExpiringSoonSponsorships(pointIds, today)) {
                points.stream()
                        .filter(point -> point.getId().equals(sponsorship.getPointId()))
                        .findFirst()
                        .ifPresent(point ->
                                countBySection.merge(point.getParentId(), 1L, Long::sum));
            }
        }
        for (TreeNodeDTO section : sections) {
            section.setSponsorshipExpiringSectionCount(
                    countBySection.getOrDefault(section.getId(), 0L).intValue());
        }
    }

    /**
     * 批量填充点位防滑垫领出/归还/破损/未还汇总（树标记与领用台账同源，刷新后保持一致）。
     */
    private void fillPointAntiSlipMat(List<TreeNodeDTO> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        List<Long> pointIds = points.stream().map(TreeNodeDTO::getId).toList();
        Map<Long, PointAntiSlipMatDTO> ledgerMap = antiSlipMatService.getLedgerMapByPointIds(pointIds);
        for (TreeNodeDTO point : points) {
            PointAntiSlipMatDTO ledger = ledgerMap.get(point.getId());
            point.setAntiSlipMatIssuedCount(ledger != null ? ledger.getIssuedCount() : 0);
            point.setAntiSlipMatReturnedCount(ledger != null ? ledger.getReturnedCount() : 0);
            point.setAntiSlipMatDamagedCount(ledger != null ? ledger.getDamagedCount() : 0);
            point.setAntiSlipMatOutstandingCount(ledger != null ? ledger.getOutstandingCount() : 0);
        }
    }

    /**
     * 填充路段的防滑垫未还清点位数与未还数量加总：直接统计树上已填充的点位标记，
     * 保证路段标记、点位标记、领用台账三处同源一致。
     */
    private void fillSectionAntiSlipMatOutstanding(java.util.Collection<TreeNodeDTO> nodes) {
        for (TreeNodeDTO node : nodes) {
            if (node.getLevel() != null && node.getLevel() == 2) {
                int pointCount = 0;
                int matCount = 0;
                for (TreeNodeDTO point : node.getChildren()) {
                    int outstanding = point.getAntiSlipMatOutstandingCount() != null
                            ? point.getAntiSlipMatOutstandingCount() : 0;
                    if (outstanding > 0) {
                        pointCount++;
                        matCount += outstanding;
                    }
                }
                node.setAntiSlipMatOutstandingPointCount(pointCount);
                node.setAntiSlipMatOutstandingMatCount(matCount);
            }
        }
    }

    /**
     * 平铺返回路段时，按路段下点位批量查询防滑垫领用汇总并填充未还清点位数，
     * 口径与街区树完整返回一致。
     */
    private void fillSectionAntiSlipMatOutstandingByQuery(List<TreeNodeDTO> sections) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        List<Long> sectionIds = sections.stream().map(TreeNodeDTO::getId).toList();
        List<TreeNode> points = treeNodeRepository.findByParentIdInAndIsDeletedFalse(sectionIds);
        Map<Long, int[]> statsBySection = new HashMap<>();
        if (!points.isEmpty()) {
            List<Long> pointIds = points.stream().map(TreeNode::getId).toList();
            Map<Long, PointAntiSlipMatDTO> ledgerMap = antiSlipMatService.getLedgerMapByPointIds(pointIds);
            for (TreeNode point : points) {
                PointAntiSlipMatDTO ledger = ledgerMap.get(point.getId());
                int outstanding = ledger != null && ledger.getOutstandingCount() != null
                        ? ledger.getOutstandingCount() : 0;
                if (outstanding > 0) {
                    int[] stats = statsBySection.computeIfAbsent(point.getParentId(), k -> new int[2]);
                    stats[0]++;
                    stats[1] += outstanding;
                }
            }
        }
        for (TreeNodeDTO section : sections) {
            int[] stats = statsBySection.get(section.getId());
            section.setAntiSlipMatOutstandingPointCount(stats != null ? stats[0] : 0);
            section.setAntiSlipMatOutstandingMatCount(stats != null ? stats[1] : 0);
        }
    }

    /**
     * 路段容量告警下钻详情：剩余容量加总、生效阈值及已满/将满点位明细，
     * 与树上路段告警标记同源；alarm 为 true 时明细列表必非空。
     */
    public SectionCapacityAlarmDTO getSectionCapacityAlarm(Long sectionId) {
        TreeNode section = treeNodeRepository.findByIdAndIsDeletedFalse(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("路段不存在"));
        if (section.getLevel() != 2) {
            throw new IllegalArgumentException("只有路段(level=2)有容量告警");
        }

        List<TreeNodeDTO> pointDtos = treeNodeRepository.findByParentIdAndIsDeletedFalse(sectionId)
                .stream().map(this::toDTO).toList();
        fillCapacityStatus(pointDtos);

        TreeNodeDTO sectionDto = toDTO(section);
        applySectionCapacityStats(sectionDto, pointDtos);

        List<TreeNodeDTO> alarmPoints = pointDtos.stream()
                .filter(p -> p.getCapacityStatus() != null)
                .sorted(Comparator.comparing(TreeNodeDTO::getRemainingCount)
                        .thenComparing(TreeNodeDTO::getSortOrder))
                .toList();

        return SectionCapacityAlarmDTO.builder()
                .sectionId(section.getId())
                .sectionName(section.getName())
                .threshold(sectionDto.getCapacityAlarmThreshold())
                .remainingSum(sectionDto.getCapacityRemainingSum())
                .alarm(sectionDto.getCapacityAlarm())
                .fullCount(sectionDto.getCapacityFullCount())
                .nearlyFullCount(sectionDto.getCapacityNearlyFullCount())
                .points(alarmPoints)
                .build();
    }

    private TreeNodeDTO toDTO(TreeNode node) {
        return TreeNodeDTO.builder()
                .id(node.getId())
                .parentId(node.getParentId())
                .level(node.getLevel())
                .name(node.getName())
                .sortOrder(node.getSortOrder())
                .capacity(node.getCapacity())
                .capacityUpdatedAt(node.getCapacityUpdatedAt())
                .capacityUpdatedReason(node.getCapacityUpdatedReason())
                .closed(node.getLevel() == 3 && isClosed(node))
                .closedStartAt(node.getLevel() == 3 ? node.getClosedStartAt() : null)
                .closedEndAt(node.getLevel() == 3 ? node.getClosedEndAt() : null)
                .closedReason(node.getLevel() == 3 ? node.getClosedReason() : null)
                .children(new ArrayList<>())
                .build();
    }

    private TreeNodeDTO toDTOWithCapacity(TreeNode node) {
        TreeNodeDTO dto = toDTO(node);
        if (node.getLevel() == 3) {
            fillPointStatus(List.of(dto));
        }
        return dto;
    }
}
