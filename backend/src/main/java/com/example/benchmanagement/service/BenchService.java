package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.BenchDTO;
import com.example.benchmanagement.dto.BenchChangeRequest;
import com.example.benchmanagement.dto.ChangeLogDTO;
import com.example.benchmanagement.entity.Bench;
import com.example.benchmanagement.entity.BenchChangeLog;
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

    private static final String REDIS_KEY_PREFIX = "bench:specs:";

    public List<BenchDTO> getAllBenches() {
        List<Bench> benches = benchRepository.findAll();
        return benches.stream().map(this::toDTO).toList();
    }

    public BenchDTO getBenchById(Long id) {
        Bench bench = benchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("长凳不存在"));
        return toDTO(bench);
    }

    public BenchDTO getBenchByCode(String code) {
        Bench bench = benchRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("长凳不存在"));
        return toDTO(bench);
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
        return benches.stream().map(this::toDTO).toList();
    }

    @Transactional
    public BenchDTO createBench(BenchDTO dto) {
        validateNodeLevel3(dto.getNodeId());

        if (benchRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("长凳编号已存在");
        }

        Bench bench = Bench.builder()
                .code(dto.getCode())
                .material(dto.getMaterial())
                .length(dto.getLength())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .nodeId(dto.getNodeId())
                .specsJson(dto.getSpecsJson())
                .status(dto.getStatus() != null ? dto.getStatus() : 1)
                .build();

        Bench saved = benchRepository.save(bench);
        cacheBenchSpecs(saved);
        log.info("创建长凳: id={}, code={}", saved.getId(), saved.getCode());
        return toDTO(saved);
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

        Bench saved = benchRepository.save(bench);
        cacheBenchSpecs(saved);
        log.info("更新长凳: id={}, code={}", saved.getId(), saved.getCode());
        return toDTO(saved);
    }

    @Transactional
    public void deleteBench(Long id) {
        Bench bench = benchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("长凳不存在"));

        benchRepository.delete(bench);
        redisTemplate.delete(REDIS_KEY_PREFIX + id);
        log.info("删除长凳: id={}, code={}", id, bench.getCode());
    }

    @Transactional
    public List<BenchDTO> changeBenchNode(BenchChangeRequest request) {
        validateNodeLevel3(request.getNewNodeId());

        List<BenchDTO> results = new ArrayList<>();
        for (Long benchId : request.getBenchIds()) {
            Bench bench = benchRepository.findById(benchId)
                    .orElseThrow(() -> new IllegalArgumentException("长凳不存在: " + benchId));

            if (!bench.getNodeId().equals(request.getNewNodeId())) {
                Long oldNodeId = bench.getNodeId();
                bench.setNodeId(request.getNewNodeId());
                benchRepository.save(bench);

                BenchChangeLog logEntry = BenchChangeLog.builder()
                        .benchId(benchId)
                        .oldNodeId(oldNodeId)
                        .newNodeId(request.getNewNodeId())
                        .changeReason(request.getChangeReason())
                        .changedBy("system")
                        .build();
                changeLogRepository.save(logEntry);

                cacheBenchSpecs(bench);
                results.add(toDTO(bench));
                log.info("变更长凳点位: benchId={}, oldNodeId={}, newNodeId={}", benchId, oldNodeId, request.getNewNodeId());
            }
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

    private void validateNodeLevel3(Long nodeId) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("点位不存在"));
        if (node.getLevel() != 3) {
            throw new IllegalArgumentException("长凳只能绑定到点位(level=3)");
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
        TreeNode point = treeNodeRepository.findByIdAndIsDeletedFalse(bench.getNodeId()).orElse(null);
        TreeNode section = point != null && point.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(point.getParentId()).orElse(null)
                : null;
        TreeNode district = section != null && section.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(section.getParentId()).orElse(null)
                : null;

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
