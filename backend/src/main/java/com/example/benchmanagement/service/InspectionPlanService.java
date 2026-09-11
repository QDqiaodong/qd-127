package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.BenchDTO;
import com.example.benchmanagement.dto.InspectionCreateRequest;
import com.example.benchmanagement.dto.InspectionDTO;
import com.example.benchmanagement.dto.InspectionItemRequest;
import com.example.benchmanagement.dto.InspectionPlanDTO;
import com.example.benchmanagement.dto.InspectionPlanDetailDTO;
import com.example.benchmanagement.dto.InspectionPlanRequest;
import com.example.benchmanagement.dto.InspectionTaskDTO;
import com.example.benchmanagement.dto.TaskExecuteRequest;
import com.example.benchmanagement.entity.Bench;
import com.example.benchmanagement.entity.InspectionPlan;
import com.example.benchmanagement.entity.InspectionTask;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.BenchRepository;
import com.example.benchmanagement.repository.InspectionPlanRepository;
import com.example.benchmanagement.repository.InspectionTaskRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InspectionPlanService {

    private final InspectionPlanRepository planRepository;
    private final InspectionTaskRepository taskRepository;
    private final TreeNodeRepository treeNodeRepository;
    private final BenchRepository benchRepository;
    private final BenchService benchService;
    private final InspectionService inspectionService;

    /**
     * 新建巡检计划：校验范围非空，启用时立即生成到期的待执行任务。
     */
    @Transactional
    public InspectionPlanDTO createPlan(InspectionPlanRequest request) {
        TreeNode scopeNode = validateScope(request.getScopeNodeId());
        validateCycleType(request.getCycleType());

        InspectionPlan plan = InspectionPlan.builder()
                .name(request.getName().trim())
                .scopeNodeId(scopeNode.getId())
                .cycleType(request.getCycleType())
                .planDate(request.getPlanDate())
                .inspector(request.getInspector().trim())
                .enabled(request.getEnabled() == null || request.getEnabled())
                .remark(trimToNull(request.getRemark()))
                .build();
        InspectionPlan saved = planRepository.save(plan);

        if (Boolean.TRUE.equals(saved.getEnabled())) {
            int created = generateDueTasks(saved);
            log.info("新建巡检计划: id={}, name={}, 生成{}个待执行任务", saved.getId(), saved.getName(), created);
        }
        return toPlanDTO(saved, taskRepository.findByPlanIdOrderByPlanDateDescIdDesc(saved.getId()));
    }

    /**
     * 编辑巡检计划：范围/周期/计划日期/检查人/启用状态可调整，
     * 已生成任务保留，后续按新配置生成。
     */
    @Transactional
    public InspectionPlanDTO updatePlan(Long id, InspectionPlanRequest request) {
        InspectionPlan plan = getPlan(id);
        TreeNode scopeNode = validateScope(request.getScopeNodeId());
        validateCycleType(request.getCycleType());

        plan.setName(request.getName().trim());
        plan.setScopeNodeId(scopeNode.getId());
        plan.setCycleType(request.getCycleType());
        plan.setPlanDate(request.getPlanDate());
        plan.setInspector(request.getInspector().trim());
        if (request.getEnabled() != null) {
            plan.setEnabled(request.getEnabled());
        }
        plan.setRemark(trimToNull(request.getRemark()));
        InspectionPlan saved = planRepository.save(plan);

        if (Boolean.TRUE.equals(saved.getEnabled())) {
            generateDueTasks(saved);
        }
        log.info("编辑巡检计划: id={}, name={}", saved.getId(), saved.getName());
        return toPlanDTO(saved, taskRepository.findByPlanIdOrderByPlanDateDescIdDesc(saved.getId()));
    }

    /**
     * 启用/停用巡检计划。停用后不再生成新任务；重新启用时补生成到期任务。
     */
    @Transactional
    public InspectionPlanDTO togglePlan(Long id) {
        InspectionPlan plan = getPlan(id);
        plan.setEnabled(!Boolean.TRUE.equals(plan.getEnabled()));
        InspectionPlan saved = planRepository.save(plan);

        int created = 0;
        if (Boolean.TRUE.equals(saved.getEnabled())) {
            created = generateDueTasks(saved);
        }
        log.info("切换巡检计划状态: id={}, enabled={}, 新生成{}个任务", saved.getId(), saved.getEnabled(), created);
        return toPlanDTO(saved, taskRepository.findByPlanIdOrderByPlanDateDescIdDesc(saved.getId()));
    }

    public List<InspectionPlanDTO> listPlans() {
        List<InspectionPlan> plans = planRepository.findAllByOrderByCreatedAtDescIdDesc();
        Map<Long, List<InspectionTask>> tasksByPlan = taskRepository.findAll().stream()
                .collect(Collectors.groupingBy(InspectionTask::getPlanId));
        return plans.stream()
                .map(p -> toPlanDTO(p, tasksByPlan.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    /**
     * 计划详情：计划信息（含已执行/待执行/逾期统计）及全部任务。
     */
    public InspectionPlanDetailDTO getPlanDetail(Long id) {
        InspectionPlan plan = getPlan(id);
        List<InspectionTask> tasks = taskRepository.findByPlanIdOrderByPlanDateDescIdDesc(plan.getId());
        return InspectionPlanDetailDTO.builder()
                .plan(toPlanDTO(plan, tasks))
                .tasks(tasks.stream().map(t -> toTaskDTO(t, plan.getName())).toList())
                .build();
    }

    /**
     * 任务列表：status 1-待执行（未逾期），2-已执行，3-逾期。
     */
    public List<InspectionTaskDTO> listTasks(Long planId, Integer status) {
        List<InspectionTask> tasks = planId != null
                ? taskRepository.findByPlanIdOrderByPlanDateDescIdDesc(planId)
                : taskRepository.findAllByOrderByPlanDateDescIdDesc();
        Map<Long, String> planNames = planRepository.findAll().stream()
                .collect(Collectors.toMap(InspectionPlan::getId, InspectionPlan::getName));
        return tasks.stream()
                .filter(t -> status == null || matchesStatus(t, status))
                .map(t -> toTaskDTO(t, planNames.getOrDefault(t.getPlanId(), "")))
                .toList();
    }

    /**
     * 执行任务时自动带入范围内全部长凳。
     */
    public List<BenchDTO> getTaskBenches(Long taskId) {
        InspectionTask task = getTask(taskId);
        return benchService.getBenchesByNode(task.getScopeNodeId());
    }

    /**
     * 执行巡检任务：先校验提交清单与任务范围内当前长凳集合完全一致
     * （少报、重复、包含已移出范围的长凳均阻止提交并列出原因），
     * 校验通过后才批量写入巡检记录并将任务置为已执行；
     * 整体处于同一事务中，任何失败都会回滚，不会留下部分数据。
     */
    @Transactional
    public List<InspectionDTO> executeTask(Long taskId, TaskExecuteRequest request) {
        InspectionTask task = getTask(taskId);
        if (task.getStatus() == null || task.getStatus() != InspectionTask.STATUS_PENDING) {
            throw new IllegalStateException("该任务已执行，请勿重复提交");
        }

        TreeNode scopeNode = treeNodeRepository.findByIdAndIsDeletedFalse(task.getScopeNodeId())
                .orElseThrow(() -> new IllegalArgumentException("任务巡检范围节点不存在或已被删除"));
        List<Long> pointIds = inspectionService.collectScopePointIds(scopeNode);
        List<Bench> benches = pointIds.isEmpty() ? List.of() : benchRepository.findByNodeIds(pointIds);
        if (benches.isEmpty()) {
            throw new IllegalArgumentException("该任务巡检范围内暂无长凳，无法执行");
        }

        validateTaskItems(benches, request.getItems());

        String inspector = (request.getInspector() == null || request.getInspector().isBlank())
                ? task.getInspector() : request.getInspector().trim();
        InspectionCreateRequest createRequest = InspectionCreateRequest.builder()
                .scopeNodeId(task.getScopeNodeId())
                .inspector(inspector)
                .items(request.getItems())
                .build();
        List<InspectionDTO> created = inspectionService.createInspection(createRequest, task.getId());

        task.setStatus(InspectionTask.STATUS_EXECUTED);
        task.setExecutedAt(LocalDateTime.now());
        task.setInspector(inspector);
        taskRepository.save(task);
        log.info("巡检任务执行完成: taskId={}, planId={}, 巡检记录{}条", task.getId(), task.getPlanId(), created.size());
        return created;
    }

    /**
     * 校验提交清单与任务范围内当前长凳集合完全一致：
     * 少报、重复提交、包含已移出范围（或已删除）的长凳时，
     * 汇总全部原因后抛出异常阻止提交。
     */
    private void validateTaskItems(List<Bench> scopeBenches, List<InspectionItemRequest> items) {
        Map<Long, Bench> scopeBenchMap = scopeBenches.stream()
                .collect(Collectors.toMap(Bench::getId, b -> b));
        List<Long> submittedIds = items == null ? List.of() : items.stream()
                .map(InspectionItemRequest::getBenchId)
                .filter(Objects::nonNull)
                .toList();

        // 查出提交长凳的当前信息，用于在提示中列出编号（已删除的长凳只显示ID）
        Map<Long, Bench> submittedBenchMap = new HashMap<>();
        if (!submittedIds.isEmpty()) {
            for (Bench bench : benchRepository.findAllById(submittedIds)) {
                submittedBenchMap.put(bench.getId(), bench);
            }
        }

        List<String> problems = new ArrayList<>();

        // 1) 重复提交
        Map<Long, Long> submitCounts = submittedIds.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        List<String> duplicated = submitCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> benchLabel(e.getKey(), submittedBenchMap.get(e.getKey())) + "（提交" + e.getValue() + "次）")
                .sorted()
                .toList();
        if (!duplicated.isEmpty()) {
            problems.add("重复提交的长凳：" + String.join("、", duplicated));
        }

        // 2) 不在任务当前范围内（已移出范围或已删除）
        List<String> outOfScope = submittedIds.stream()
                .distinct()
                .filter(id -> !scopeBenchMap.containsKey(id))
                .map(id -> {
                    Bench bench = submittedBenchMap.get(id);
                    return bench != null
                            ? benchLabel(id, bench) + "（已移出任务范围）"
                            : "ID=" + id + "（长凳不存在）";
                })
                .sorted()
                .toList();
        if (!outOfScope.isEmpty()) {
            problems.add("不在任务范围内的长凳：" + String.join("、", outOfScope));
        }

        // 3) 少报：范围内但未提交
        Set<Long> submittedSet = new HashSet<>(submittedIds);
        List<String> missing = scopeBenches.stream()
                .filter(b -> !submittedSet.contains(b.getId()))
                .map(b -> benchLabel(b.getId(), b))
                .sorted()
                .toList();
        if (!missing.isEmpty()) {
            problems.add("未提交巡检记录的长凳：" + String.join("、", missing));
        }

        if (!problems.isEmpty()) {
            throw new IllegalArgumentException(
                    "提交清单与任务范围内当前长凳不一致，已阻止提交：" + String.join("；", problems));
        }
    }

    private String benchLabel(Long benchId, Bench bench) {
        return bench != null ? "【" + bench.getCode() + "】" : "ID=" + benchId;
    }

    /**
     * 为全部启用中的计划生成到期（计划日期<=今天）的待执行任务，停用计划不生成。
     *
     * @return 新生成的任务数
     */
    @Transactional
    public int generateDueTasks() {
        int total = 0;
        for (InspectionPlan plan : planRepository.findByEnabledTrue()) {
            total += generateDueTasks(plan);
        }
        return total;
    }

    private int generateDueTasks(InspectionPlan plan) {
        if (plan.getPlanDate() == null || plan.getCycleType() == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        LocalDate date = plan.getPlanDate();
        int created = 0;
        while (!date.isAfter(today)) {
            if (!taskRepository.existsByPlanIdAndPlanDate(plan.getId(), date)) {
                taskRepository.save(InspectionTask.builder()
                        .planId(plan.getId())
                        .scopeNodeId(plan.getScopeNodeId())
                        .planDate(date)
                        .inspector(plan.getInspector())
                        .status(InspectionTask.STATUS_PENDING)
                        .build());
                created++;
            }
            date = nextDate(date, plan.getCycleType());
        }
        return created;
    }

    private LocalDate nextDate(LocalDate date, Integer cycleType) {
        return switch (cycleType) {
            case InspectionPlan.CYCLE_DAILY -> date.plusDays(1);
            case InspectionPlan.CYCLE_WEEKLY -> date.plusWeeks(1);
            case InspectionPlan.CYCLE_MONTHLY -> date.plusMonths(1);
            default -> throw new IllegalArgumentException("不支持的巡检周期: " + cycleType);
        };
    }

    private void validateCycleType(Integer cycleType) {
        if (cycleType == null
                || (cycleType != InspectionPlan.CYCLE_DAILY
                && cycleType != InspectionPlan.CYCLE_WEEKLY
                && cycleType != InspectionPlan.CYCLE_MONTHLY)) {
            throw new IllegalArgumentException("巡检周期只能为每天(1)、每周(2)或每月(3)");
        }
    }

    /**
     * 校验巡检范围：必须是街区/路段/点位，且范围内有点位、有长凳（空范围明确提示）。
     */
    private TreeNode validateScope(Long scopeNodeId) {
        TreeNode node = treeNodeRepository.findByIdAndIsDeletedFalse(scopeNodeId)
                .orElseThrow(() -> new IllegalArgumentException("巡检范围节点不存在"));
        if (node.getLevel() < 1 || node.getLevel() > 3) {
            throw new IllegalArgumentException("巡检范围必须是街区、路段或点位");
        }
        List<Long> pointIds = inspectionService.collectScopePointIds(node);
        if (pointIds.isEmpty()) {
            throw new IllegalArgumentException("所选范围下没有点位，无法保存巡检计划");
        }
        if (benchRepository.findByNodeIds(pointIds).isEmpty()) {
            throw new IllegalArgumentException("所选范围下暂无长凳，无法保存巡检计划");
        }
        return node;
    }

    private InspectionPlan getPlan(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("巡检计划不存在"));
    }

    private InspectionTask getTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("巡检任务不存在"));
    }

    private boolean isOverdue(InspectionTask task) {
        return task.getStatus() != null
                && task.getStatus() == InspectionTask.STATUS_PENDING
                && task.getPlanDate() != null
                && task.getPlanDate().isBefore(LocalDate.now());
    }

    private boolean matchesStatus(InspectionTask task, Integer status) {
        if (status == InspectionTask.STATUS_EXECUTED) {
            return task.getStatus() != null && task.getStatus() == InspectionTask.STATUS_EXECUTED;
        }
        if (status == InspectionTask.STATUS_OVERDUE) {
            return isOverdue(task);
        }
        return task.getStatus() != null
                && task.getStatus() == InspectionTask.STATUS_PENDING
                && !isOverdue(task);
    }

    private InspectionPlanDTO toPlanDTO(InspectionPlan plan, List<InspectionTask> tasks) {
        long executed = tasks.stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == InspectionTask.STATUS_EXECUTED)
                .count();
        long overdue = tasks.stream().filter(this::isOverdue).count();
        long pending = tasks.stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == InspectionTask.STATUS_PENDING)
                .count() - overdue;

        TreeNode scopeNode = treeNodeRepository.findByIdAndIsDeletedFalse(plan.getScopeNodeId()).orElse(null);
        return InspectionPlanDTO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .scopeNodeId(plan.getScopeNodeId())
                .scopeNodeName(scopeNode != null ? scopeNode.getName() : "")
                .scopePath(scopeNode != null ? buildScopePath(scopeNode) : "")
                .cycleType(plan.getCycleType())
                .planDate(plan.getPlanDate())
                .inspector(plan.getInspector())
                .enabled(plan.getEnabled())
                .remark(plan.getRemark())
                .executedCount(executed)
                .pendingCount(pending)
                .overdueCount(overdue)
                .createdAt(plan.getCreatedAt())
                .build();
    }

    private InspectionTaskDTO toTaskDTO(InspectionTask task, String planName) {
        TreeNode scopeNode = treeNodeRepository.findByIdAndIsDeletedFalse(task.getScopeNodeId()).orElse(null);
        return InspectionTaskDTO.builder()
                .id(task.getId())
                .planId(task.getPlanId())
                .planName(planName != null ? planName : "")
                .scopeNodeId(task.getScopeNodeId())
                .scopeNodeName(scopeNode != null ? scopeNode.getName() : "")
                .scopePath(scopeNode != null ? buildScopePath(scopeNode) : "")
                .planDate(task.getPlanDate())
                .inspector(task.getInspector())
                .status(task.getStatus())
                .overdue(isOverdue(task))
                .executedAt(task.getExecutedAt())
                .createdAt(task.getCreatedAt())
                .build();
    }

    private String buildScopePath(TreeNode node) {
        if (node.getLevel() == 1) {
            return node.getName();
        }
        TreeNode parent = node.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(node.getParentId()).orElse(null) : null;
        if (node.getLevel() == 2) {
            return (parent != null ? parent.getName() + " / " : "") + node.getName();
        }
        TreeNode district = parent != null && parent.getParentId() != null
                ? treeNodeRepository.findByIdAndIsDeletedFalse(parent.getParentId()).orElse(null) : null;
        return (district != null ? district.getName() + " / " : "")
                + (parent != null ? parent.getName() + " / " : "")
                + node.getName();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
