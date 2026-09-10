package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.RepairOrderCompleteRequest;
import com.example.benchmanagement.dto.RepairOrderCreateRequest;
import com.example.benchmanagement.dto.RepairOrderDTO;
import com.example.benchmanagement.entity.Bench;
import com.example.benchmanagement.entity.BenchInspection;
import com.example.benchmanagement.entity.RepairOrder;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.BenchRepository;
import com.example.benchmanagement.repository.RepairOrderRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepairOrderService {

    private final RepairOrderRepository repairOrderRepository;
    private final BenchRepository benchRepository;
    private final TreeNodeRepository treeNodeRepository;

    public static final List<Integer> OPEN_STATUSES = List.of(
            RepairOrder.STATUS_PENDING, RepairOrder.STATUS_IN_PROGRESS);

    /**
     * 某长凳是否存在未完成（待处理/维修中）工单。
     */
    public long countOpenOrders(Long benchId) {
        return repairOrderRepository.countByBenchIdAndStatusIn(benchId, OPEN_STATUSES);
    }

    /**
     * 由异常巡检记录生成维修工单。
     */
    @Transactional
    public RepairOrder createFromInspection(BenchInspection inspection, String operator) {
        Bench bench = benchRepository.findById(inspection.getBenchId())
                .orElseThrow(() -> new IllegalArgumentException("长凳不存在: " + inspection.getBenchId()));
        assertNoOpenOrder(bench);

        RepairOrder order = RepairOrder.builder()
                .code(generateCode())
                .benchId(bench.getId())
                .inspectionId(inspection.getId())
                .problemType(inspection.getProblemType())
                .severity(inspection.getSeverity())
                .description(inspection.getDescription())
                .suggestion(inspection.getSuggestion())
                .status(RepairOrder.STATUS_PENDING)
                .createdBy(operator)
                .build();
        RepairOrder saved = repairOrderRepository.save(order);
        log.info("巡检异常生成维修工单: code={}, benchId={}, inspectionId={}",
                saved.getCode(), bench.getId(), inspection.getId());
        return saved;
    }

    @Transactional
    public RepairOrderDTO createOrder(RepairOrderCreateRequest request) {
        Bench bench = benchRepository.findById(request.getBenchId())
                .orElseThrow(() -> new IllegalArgumentException("长凳不存在"));
        assertNoOpenOrder(bench);

        RepairOrder order = RepairOrder.builder()
                .code(generateCode())
                .benchId(bench.getId())
                .problemType(request.getProblemType())
                .severity(request.getSeverity())
                .description(request.getDescription())
                .suggestion(request.getSuggestion())
                .status(RepairOrder.STATUS_PENDING)
                .createdBy("system")
                .build();
        RepairOrder saved = repairOrderRepository.save(order);
        log.info("手动创建维修工单: code={}, benchId={}", saved.getCode(), bench.getId());
        return toDTO(saved);
    }

    @Transactional
    public RepairOrderDTO startOrder(Long id) {
        RepairOrder order = getOrderOrThrow(id);
        if (order.getStatus() != RepairOrder.STATUS_PENDING) {
            throw new IllegalStateException("当前工单状态不允许开始维修（仅待处理工单可开始维修）");
        }
        order.setStatus(RepairOrder.STATUS_IN_PROGRESS);
        order.setStartedAt(LocalDateTime.now());
        RepairOrder saved = repairOrderRepository.save(order);
        log.info("工单开始维修: code={}", saved.getCode());
        return toDTO(saved);
    }

    @Transactional
    public RepairOrderDTO completeOrder(Long id, RepairOrderCompleteRequest request) {
        RepairOrder order = getOrderOrThrow(id);
        if (order.getStatus() != RepairOrder.STATUS_IN_PROGRESS) {
            throw new IllegalStateException("当前工单状态不允许完成维修（仅维修中工单可完成）");
        }
        if (request.getRepairResult() == null || request.getRepairResult().isBlank()) {
            throw new IllegalArgumentException("维修结果不能为空");
        }
        if (request.getCompletedAt() == null) {
            throw new IllegalArgumentException("维修完成时间不能为空");
        }

        order.setStatus(RepairOrder.STATUS_COMPLETED);
        order.setRepairResult(request.getRepairResult().trim());
        order.setCompletedAt(request.getCompletedAt());
        if (order.getStartedAt() == null) {
            order.setStartedAt(LocalDateTime.now());
        }
        RepairOrder saved = repairOrderRepository.save(order);
        log.info("工单完成维修: code={}", saved.getCode());
        return toDTO(saved);
    }

    @Transactional
    public RepairOrderDTO closeOrder(Long id) {
        RepairOrder order = getOrderOrThrow(id);
        if (order.getStatus() == RepairOrder.STATUS_CLOSED) {
            throw new IllegalStateException("工单已关闭，无需重复关闭");
        }
        if (order.getStatus() == RepairOrder.STATUS_IN_PROGRESS) {
            throw new IllegalStateException("维修中的工单不能直接关闭，请先完成维修");
        }
        // 待处理可直接关闭（无需维修）；已完成可关闭归档
        order.setStatus(RepairOrder.STATUS_CLOSED);
        order.setClosedAt(LocalDateTime.now());
        RepairOrder saved = repairOrderRepository.save(order);
        log.info("工单关闭: code={}", saved.getCode());
        return toDTO(saved);
    }

    public List<RepairOrderDTO> getOrders(Long status, Integer severity, String keyword) {
        List<RepairOrder> orders = repairOrderRepository.findAllByOrderByCreatedAtDescIdDesc();
        return orders.stream()
                .filter(o -> status == null || status.intValue() == o.getStatus())
                .filter(o -> severity == null || severity.equals(o.getSeverity()))
                .filter(o -> {
                    if (keyword == null || keyword.isBlank()) {
                        return true;
                    }
                    String kw = keyword.trim().toLowerCase();
                    Bench bench = benchRepository.findById(o.getBenchId()).orElse(null);
                    String code = bench != null ? bench.getCode().toLowerCase() : "";
                    return o.getCode().toLowerCase().contains(kw) || code.contains(kw);
                })
                .map(this::toDTO)
                .toList();
    }

    public RepairOrderDTO getOrderById(Long id) {
        return toDTO(getOrderOrThrow(id));
    }

    public List<RepairOrderDTO> getOrdersByBench(Long benchId) {
        return repairOrderRepository.findByBenchIdOrderByCreatedAtDescIdDesc(benchId)
                .stream().map(this::toDTO).toList();
    }

    private void assertNoOpenOrder(Bench bench) {
        long open = countOpenOrders(bench.getId());
        if (open > 0) {
            throw new IllegalStateException(String.format(
                    "长凳【%s】已有%d个未完成的维修工单（待处理/维修中），请先处理后再创建新工单",
                    bench.getCode(), open));
        }
    }

    private RepairOrder getOrderOrThrow(Long id) {
        return repairOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("维修工单不存在"));
    }

    private String generateCode() {
        String prefix = "RO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        for (int i = 0; i < 10; i++) {
            String code = prefix + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
            if (!repairOrderRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("工单号生成冲突，请稍后重试");
    }

    /**
     * 批量取长凳未完成工单数。
     */
    public Map<Long, Long> openOrderCountMap(List<Long> benchIds) {
        Map<Long, Long> map = new HashMap<>();
        for (Long benchId : benchIds) {
            map.put(benchId, repairOrderRepository.countByBenchIdAndStatusIn(benchId, OPEN_STATUSES));
        }
        return map;
    }

    public RepairOrderDTO toDTO(RepairOrder order) {
        Bench bench = benchRepository.findById(order.getBenchId()).orElse(null);
        Long nodeId = bench != null ? bench.getNodeId() : null;
        TreeNode point = nodeId != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(nodeId).orElse(null) : null;
        TreeNode section = point != null && point.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(point.getParentId()).orElse(null) : null;
        TreeNode district = section != null && section.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(section.getParentId()).orElse(null) : null;

        return RepairOrderDTO.builder()
                .id(order.getId())
                .code(order.getCode())
                .benchId(order.getBenchId())
                .benchCode(bench != null ? bench.getCode() : "")
                .inspectionId(order.getInspectionId())
                .problemType(order.getProblemType())
                .severity(order.getSeverity())
                .description(order.getDescription())
                .suggestion(order.getSuggestion())
                .status(order.getStatus())
                .repairResult(order.getRepairResult())
                .startedAt(order.getStartedAt())
                .completedAt(order.getCompletedAt())
                .closedAt(order.getClosedAt())
                .createdBy(order.getCreatedBy())
                .createdAt(order.getCreatedAt())
                .nodeName(point != null ? point.getName() : "")
                .sectionName(section != null ? section.getName() : "")
                .districtName(district != null ? district.getName() : "")
                .build();
    }
}
