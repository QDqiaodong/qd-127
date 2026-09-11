package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.BenchDTO;
import com.example.benchmanagement.dto.BenchChangeRequest;
import com.example.benchmanagement.dto.ChangeLogDTO;
import com.example.benchmanagement.entity.Bench;
import com.example.benchmanagement.entity.BenchChangeLog;
import com.example.benchmanagement.entity.BenchInspection;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.BenchChangeLogRepository;
import com.example.benchmanagement.repository.BenchRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BenchService {

    private final BenchRepository benchRepository;
    private final TreeNodeRepository treeNodeRepository;
    private final BenchChangeLogRepository changeLogRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final InspectionService inspectionService;
    private final RepairOrderService repairOrderService;
    private final TreeNodeService treeNodeService;

    private static final String REDIS_KEY_PREFIX = "bench:specs:";

    public List<BenchDTO> getAllBenches() {
        return getAllBenches(null, null);
    }

    /**
     * 查询全部长凳并附带最新巡检状态，可按巡检结果、严重程度筛选。
     *
     * @param inspectionResult 最新巡检结果：1-正常，0-异常；特殊值 2 表示从未巡检
     * @param severity         最新巡检严重程度：1-低，2-中，3-高
     */
    public List<BenchDTO> getAllBenches(Integer inspectionResult, Integer severity) {
        List<Bench> benches = benchRepository.findAll();

        List<Long> benchIds = benches.stream().map(Bench::getId).toList();
        Map<Long, BenchInspection> latestMap = inspectionService.latestInspectionMap(benchIds);
        Map<Long, Long> openOrderMap = repairOrderService.openOrderCountMap(benchIds);

        return benches.stream()
                .map(b -> toDTO(b, latestMap.get(b.getId()), openOrderMap.getOrDefault(b.getId(), 0L)))
                .filter(dto -> matchesInspectionFilter(dto, inspectionResult, severity))
                .toList();
    }

    private boolean matchesInspectionFilter(BenchDTO dto, Integer inspectionResult, Integer severity) {
        if (inspectionResult != null) {
            // 2 = 从未巡检
            if (inspectionResult == 2) {
                if (dto.getLatestInspectionResult() != null) {
                    return false;
                }
            } else if (!Objects.equals(dto.getLatestInspectionResult(), inspectionResult)) {
                return false;
            }
        }
        if (severity != null && !Objects.equals(dto.getLatestInspectionSeverity(), severity)) {
            return false;
        }
        return true;
    }

    public BenchDTO getBenchById(Long id) {
        Bench bench = benchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("长凳不存在"));
        Map<Long, BenchInspection> latestMap =
                inspectionService.latestInspectionMap(List.of(id));
        return toDTO(bench, latestMap.get(id), repairOrderService.countOpenOrders(id));
    }

    public BenchDTO getBenchByCode(String code) {
        Bench bench = benchRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("长凳不存在"));
        Map<Long, BenchInspection> latestMap =
                inspectionService.latestInspectionMap(List.of(bench.getId()));
        return toDTO(bench, latestMap.get(bench.getId()), repairOrderService.countOpenOrders(bench.getId()));
    }

    public List<BenchDTO> getBenchesByNode(Long nodeId) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("节点不存在"));

        List<Long> targetNodeIds;
        if (node.getLevel() == 3) {
            targetNodeIds = List.of(nodeId);
        } else if (node.getLevel() == 2) {
            targetNodeIds = treeNodeRepository.findByParentIdAndIsDeletedFalse(nodeId)
                    .stream().map(TreeNode::getId).toList();
        } else {
            List<TreeNode> sections = treeNodeRepository.findByParentIdAndIsDeletedFalse(nodeId);
            List<Long> sectionIds = sections.stream().map(TreeNode::getId).toList();
            targetNodeIds = treeNodeRepository.findByParentIdInAndIsDeletedFalse(sectionIds)
                    .stream().map(TreeNode::getId).toList();
        }

        List<Bench> benches = benchRepository.findByNodeIds(targetNodeIds);
        return mapBenchDTOs(benches);
    }

    /**
     * 按点位ID集合查询长凳（任务执行场景，封闭点位已在调用方剔除）。
     */
    public List<BenchDTO> getBenchesByNodeIds(List<Long> pointIds) {
        if (pointIds == null || pointIds.isEmpty()) {
            return List.of();
        }
        return mapBenchDTOs(benchRepository.findByNodeIds(pointIds));
    }

    private List<BenchDTO> mapBenchDTOs(List<Bench> benches) {
        List<Long> benchIds = benches.stream().map(Bench::getId).toList();
        Map<Long, BenchInspection> latestMap = inspectionService.latestInspectionMap(benchIds);
        Map<Long, Long> openOrderMap = repairOrderService.openOrderCountMap(benchIds);
        return benches.stream()
                .map(b -> toDTO(b, latestMap.get(b.getId()), openOrderMap.getOrDefault(b.getId(), 0L)))
                .toList();
    }

    @Transactional
    public BenchDTO createBench(BenchDTO dto) {
        TreeNode point = validateNodeLevel3(dto.getNodeId());
        assertPointNotClosed(point);

        if (benchRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("长凳编号已存在");
        }

        int status = dto.getStatus() != null ? dto.getStatus() : Bench.STATUS_NORMAL;
        // 只有在用长凳占用容量；直接建档为停用的长凳不占用容量
        if (status == Bench.STATUS_NORMAL) {
            assertCapacityAvailable(point, 1);
        }

        Bench bench = Bench.builder()
                .code(dto.getCode())
                .material(dto.getMaterial())
                .length(dto.getLength())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .nodeId(dto.getNodeId())
                .specsJson(dto.getSpecsJson())
                .status(status)
                .build();

        Bench saved = benchRepository.save(bench);
        cacheBenchSpecs(saved);
        log.info("创建长凳: id={}, code={}", saved.getId(), saved.getCode());
        return toDTO(saved, null, 0L);
    }

    @Transactional
    public BenchDTO updateBench(Long id, BenchDTO dto) {
        Bench bench = benchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("长凳不存在"));

        if (dto.getCode() != null && !dto.getCode().equals(bench.getCode())) {
            if (benchRepository.existsByCode(dto.getCode())) {
                throw new IllegalArgumentException("长凳编号已存在");
            }
            bench.setCode(dto.getCode());
        }

        if (dto.getMaterial() != null) {
            bench.setMaterial(dto.getMaterial());
        }
        if (dto.getLength() != null) {
            bench.setLength(dto.getLength());
        }
        if (dto.getWidth() != null) {
            bench.setWidth(dto.getWidth());
        }
        if (dto.getHeight() != null) {
            bench.setHeight(dto.getHeight());
        }
        if (dto.getStatus() != null) {
            bench.setStatus(dto.getStatus());
        }
        if (dto.getSpecsJson() != null) {
            bench.setSpecsJson(dto.getSpecsJson());
        }

        if (dto.getNodeId() != null && !dto.getNodeId().equals(bench.getNodeId())) {
            TreeNode targetPoint = validateNodeLevel3(dto.getNodeId());
            assertPointNotClosed(targetPoint);
            assertCapacityAvailable(targetPoint, 1);

            Long oldNodeId = bench.getNodeId();
            bench.setNodeId(dto.getNodeId());

            BenchChangeLog logEntry = BenchChangeLog.builder()
                    .benchId(id)
                    .oldNodeId(oldNodeId)
                    .newNodeId(dto.getNodeId())
                    .changeReason(dto.getChangeReason())
                    .changedBy("system")
                    .build();
            changeLogRepository.save(logEntry);
            log.info("单条变更长凳点位: benchId={}, oldNodeId={}, newNodeId={}", id, oldNodeId, dto.getNodeId());
        }

        Bench saved = benchRepository.save(bench);
        cacheBenchSpecs(saved);
        log.info("更新长凳: id={}, code={}", saved.getId(), saved.getCode());
        Map<Long, BenchInspection> latestMap =
                inspectionService.latestInspectionMap(List.of(id));
        return toDTO(saved, latestMap.get(id), repairOrderService.countOpenOrders(id));
    }

    @Transactional
    public void deleteBench(Long id) {
        Bench bench = benchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("长凳不存在"));

        long openOrders = repairOrderService.countOpenOrders(id);
        if (openOrders > 0) {
            throw new IllegalStateException(String.format(
                    "长凳【%s】仍有%d个未完成的维修工单（待处理/维修中），请先完成或关闭相关工单后再删除",
                    bench.getCode(), openOrders));
        }

        benchRepository.delete(bench);
        redisTemplate.delete(REDIS_KEY_PREFIX + id);
        log.info("删除长凳: id={}, code={}", id, bench.getCode());
    }

    @Transactional
    public List<BenchDTO> changeBenchNode(BenchChangeRequest request) {
        TreeNode targetPoint = validateNodeLevel3(request.getNewNodeId());
        assertPointNotClosed(targetPoint);

        if (request.getBenchIds() == null || request.getBenchIds().isEmpty()) {
            throw new IllegalArgumentException("长凳ID列表不能为空");
        }

        List<Bench> benches = new ArrayList<>();
        for (Long benchId : request.getBenchIds()) {
            Bench bench = benchRepository.findById(benchId)
                    .orElseThrow(() -> new IllegalArgumentException("长凳不存在: " + benchId));
            benches.add(bench);
        }

        // 已在目标点位上的长凳不重复占用容量
        List<Long> excludeBenchIds = benches.stream()
                .filter(b -> b.getNodeId().equals(request.getNewNodeId()))
                .map(Bench::getId)
                .toList();
        long incomingCount = benches.size() - excludeBenchIds.size();
        if (incomingCount > 0) {
            assertCapacityAvailable(targetPoint, incomingCount);
        }

        List<Long> resultBenchIds = benches.stream().map(Bench::getId).toList();
        Map<Long, BenchInspection> latestMap = inspectionService.latestInspectionMap(resultBenchIds);
        Map<Long, Long> openOrderMap = repairOrderService.openOrderCountMap(resultBenchIds);

        List<BenchDTO> results = new ArrayList<>();
        for (Bench bench : benches) {
            if (!bench.getNodeId().equals(request.getNewNodeId())) {
                Long oldNodeId = bench.getNodeId();
                bench.setNodeId(request.getNewNodeId());
                benchRepository.save(bench);

                BenchChangeLog logEntry = BenchChangeLog.builder()
                        .benchId(bench.getId())
                        .oldNodeId(oldNodeId)
                        .newNodeId(request.getNewNodeId())
                        .changeReason(request.getChangeReason())
                        .changedBy("system")
                        .build();
                changeLogRepository.save(logEntry);

                cacheBenchSpecs(bench);
                log.info("变更长凳点位: benchId={}, oldNodeId={}, newNodeId={}", bench.getId(), oldNodeId, request.getNewNodeId());
            }
            results.add(toDTO(bench, latestMap.get(bench.getId()), openOrderMap.getOrDefault(bench.getId(), 0L)));
        }
        return results;
    }

    public List<ChangeLogDTO> getChangeLogs(Long benchId) {
        List<BenchChangeLog> logs = changeLogRepository.findByBenchIdOrderByChangedAtDesc(benchId);
        return logs.stream().map(this::toChangeLogDTO).toList();
    }

    public List<ChangeLogDTO> getAllChangeLogs() {
        List<BenchChangeLog> logs = changeLogRepository.findAll();
        logs.sort((a, b) -> b.getChangedAt().compareTo(a.getChangedAt()));
        return logs.stream().map(this::toChangeLogDTO).toList();
    }

    public List<Map<String, Object>> exportBenchAssets(Long sectionId) {
        TreeNode section = treeNodeRepository.findByIdAndIsDeletedFalse(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("路段不存在"));

        if (section.getLevel() != 2) {
            throw new IllegalArgumentException("只能导出路段级别的资产");
        }

        List<TreeNode> points = treeNodeRepository.findByParentIdAndIsDeletedFalse(sectionId);
        List<Long> pointIds = points.stream().map(TreeNode::getId).toList();
        List<Bench> benches = benchRepository.findByNodeIds(pointIds);

        Map<Long, TreeNode> nodeMap = new HashMap<>();
        nodeMap.put(sectionId, section);
        points.forEach(p -> nodeMap.put(p.getId(), p));

        List<Map<String, Object>> assets = new ArrayList<>();
        for (Bench bench : benches) {
            TreeNode point = nodeMap.get(bench.getNodeId());
            Map<String, Object> asset = new LinkedHashMap<>();
            asset.put("长凳编号", bench.getCode());
            asset.put("材质", bench.getMaterial());
            asset.put("长度(cm)", bench.getLength());
            asset.put("宽度(cm)", bench.getWidth());
            asset.put("高度(cm)", bench.getHeight());
            asset.put("所属街区", section.getParentId() != null ? getNodeName(section.getParentId()) : "");
            asset.put("所属路段", section.getName());
            asset.put("所属点位", point != null ? point.getName() : "");
            asset.put("创建时间", bench.getCreatedAt());
            assets.add(asset);
        }
        return assets;
    }

    private TreeNode validateNodeLevel3(Long nodeId) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("点位不存在"));
        if (node.getLevel() != 3) {
            throw new IllegalArgumentException("长凳只能绑定到点位(level=3)");
        }
        return node;
    }

    /**
     * 封闭期内禁止往该点位调入（新增/移入）长凳。
     */
    private void assertPointNotClosed(TreeNode point) {
        if (treeNodeService.isPointClosed(point.getId())) {
            String endAt = point.getClosedEndAt() != null ? point.getClosedEndAt().toString() : "未设置";
            throw new IllegalStateException(String.format(
                    "点位【%s】处于临时封闭期（截止%s），封闭期内禁止调入长凳，到期或人工解封后恢复",
                    point.getName(), endAt));
        }
    }

    /**
     * 校验目标点位是否还有足够容量摆放 incoming 张在用长凳。
     * 占用数只统计状态为在用(1)的长凳，停用长凳不计入容量。
     *
     * @param point            目标点位
     * @param incoming         本次拟新增（含变更进入）的在用长凳数量
     */
    private void assertCapacityAvailable(TreeNode point, long incoming) {
        int capacity = point.getCapacity() != null ? point.getCapacity() : TreeNode.DEFAULT_CAPACITY;
        long occupied = benchRepository.countActiveByNodeId(point.getId());
        long available = capacity - occupied;
        if (occupied + incoming > capacity) {
            throw new IllegalArgumentException(String.format(
                    "点位【%s】容量不足：容量上限%d张，当前在用%d张，剩余%d个空位，本次需占用%d个，操作已阻止",
                    point.getName(), capacity, occupied, Math.max(0, available), incoming));
        }
    }

    private void cacheBenchSpecs(Bench bench) {
        String key = REDIS_KEY_PREFIX + bench.getId();
        redisTemplate.opsForZSet().removeRange(key, 0, -1);

        if (bench.getLength() != null) {
            redisTemplate.opsForZSet().add(key, "length:" + bench.getLength(), 1);
        }
        if (bench.getWidth() != null) {
            redisTemplate.opsForZSet().add(key, "width:" + bench.getWidth(), 2);
        }
        if (bench.getHeight() != null) {
            redisTemplate.opsForZSet().add(key, "height:" + bench.getHeight(), 3);
        }
    }

    private String getNodeName(Long nodeId) {
        return treeNodeRepository.findByIdAndIsDeletedFalse(nodeId)
                .map(TreeNode::getName)
                .orElse("");
    }

    private BenchDTO toDTO(Bench bench) {
        return toDTO(bench, null, 0L);
    }

    private BenchDTO toDTO(Bench bench, BenchInspection latestInspection, long openOrderCount) {
        TreeNode point = treeNodeRepository.findByIdAndIsDeletedFalse(bench.getNodeId()).orElse(null);
        TreeNode section = point != null && point.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(point.getParentId()).orElse(null)
                : null;
        TreeNode district = section != null && section.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(section.getParentId()).orElse(null)
                : null;

        boolean pointClosed = point != null && treeNodeService.isPointClosed(point.getId());

        return BenchDTO.builder()
                .id(bench.getId())
                .code(bench.getCode())
                .material(bench.getMaterial())
                .length(bench.getLength())
                .width(bench.getWidth())
                .height(bench.getHeight())
                .nodeId(bench.getNodeId())
                .specsJson(bench.getSpecsJson())
                .status(bench.getStatus())
                .nodeName(point != null ? point.getName() : "")
                .sectionName(section != null ? section.getName() : "")
                .districtName(district != null ? district.getName() : "")
                .latestInspectionAt(latestInspection != null ? latestInspection.getInspectedAt() : null)
                .latestInspectionResult(latestInspection != null ? latestInspection.getResult() : null)
                .latestInspectionSeverity(latestInspection != null ? latestInspection.getSeverity() : null)
                .latestProblemType(latestInspection != null ? latestInspection.getProblemType() : null)
                .openOrderCount(openOrderCount)
                .pointClosed(pointClosed)
                .pointClosedEndAt(pointClosed ? point.getClosedEndAt() : null)
                .pointClosedReason(pointClosed ? point.getClosedReason() : null)
                .build();
    }

    private ChangeLogDTO toChangeLogDTO(BenchChangeLog log) {
        Bench bench = benchRepository.findById(log.getBenchId()).orElse(null);
        TreeNode oldNode = treeNodeRepository.findByIdAndIsDeletedFalse(log.getOldNodeId()).orElse(null);
        TreeNode newNode = treeNodeRepository.findByIdAndIsDeletedFalse(log.getNewNodeId()).orElse(null);

        return ChangeLogDTO.builder()
                .id(log.getId())
                .benchId(log.getBenchId())
                .benchCode(bench != null ? bench.getCode() : "")
                .oldNodeId(log.getOldNodeId())
                .oldNodeName(oldNode != null ? oldNode.getName() : "")
                .newNodeId(log.getNewNodeId())
                .newNodeName(newNode != null ? newNode.getName() : "")
                .changeReason(log.getChangeReason())
                .changedAt(log.getChangedAt())
                .changedBy(log.getChangedBy())
                .build();
    }
}
