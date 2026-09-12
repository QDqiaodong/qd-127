package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.AdditionalBenchPlanDTO;
import com.example.benchmanagement.dto.AdditionalBenchPlanLogDTO;
import com.example.benchmanagement.dto.AdditionalBenchPlanRequest;
import com.example.benchmanagement.dto.AdditionalBenchVerifyRequest;
import com.example.benchmanagement.dto.SectionAdditionalBenchSummaryDTO;
import com.example.benchmanagement.entity.AdditionalBenchPlan;
import com.example.benchmanagement.entity.AdditionalBenchPlanLog;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.AdditionalBenchPlanLogRepository;
import com.example.benchmanagement.repository.AdditionalBenchPlanRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdditionalBenchPlanService {

    public static final int EFFECTIVE_STATUS_PENDING = 1;
    public static final int EFFECTIVE_STATUS_DEPLOYED = 2;
    public static final int EFFECTIVE_STATUS_OVERDUE = 3;

    private final AdditionalBenchPlanRepository planRepository;
    private final AdditionalBenchPlanLogRepository logRepository;
    private final TreeNodeRepository treeNodeRepository;

    @Transactional
    public AdditionalBenchPlanDTO createPlan(AdditionalBenchPlanRequest request) {
        TreeNode district = validateDistrict(request.getDistrictId());
        TreeNode section = validateSection(request.getSectionId(), district.getId());
        if (request.getBenchCount() == null || request.getBenchCount() <= 0) {
            throw new IllegalArgumentException("加凳数量必须大于0");
        }
        if (request.getStatus() != null && request.getStatus() != AdditionalBenchPlan.STATUS_PENDING) {
            throw new IllegalArgumentException("新建加凳预案只能为待投放，实际投放请通过核销登记");
        }

        AdditionalBenchPlan plan = AdditionalBenchPlan.builder()
                .districtId(district.getId())
                .sectionId(section.getId())
                .planDate(request.getPlanDate())
                .benchCount(request.getBenchCount())
                .verifiedCount(0)
                .status(AdditionalBenchPlan.STATUS_PENDING)
                .remark(trimToNull(request.getRemark()))
                .build();
        AdditionalBenchPlan saved = planRepository.save(plan);
        log.info("新建加凳预案: id={}, sectionId={}, planDate={}, count={}",
                saved.getId(), section.getId(), saved.getPlanDate(), saved.getBenchCount());
        return toDTO(saved, getNodeMap(), List.of());
    }

    @Transactional
    public AdditionalBenchPlanDTO verify(Long id, AdditionalBenchVerifyRequest request) {
        AdditionalBenchPlan plan = getPlan(id);
        if (plan.getStatus() != null && plan.getStatus() == AdditionalBenchPlan.STATUS_DEPLOYED) {
            throw new IllegalStateException("该加凳预案已全部投放并核销，不能重复核销");
        }

        int verifyCount = request.getVerifiedCount();
        int before = plan.getVerifiedCount() == null ? 0 : plan.getVerifiedCount();
        int after = before + verifyCount;
        if (after > plan.getBenchCount()) {
            throw new IllegalArgumentException(String.format(
                    "核销数量超出待投放数量：本次%d张，当前还可核销%d张",
                    verifyCount, plan.getBenchCount() - before));
        }

        String operator = request.getOperator().trim();
        LocalDateTime now = LocalDateTime.now();
        AdditionalBenchPlanLog verifyLog = AdditionalBenchPlanLog.builder()
                .planId(plan.getId())
                .verifiedCount(verifyCount)
                .beforeCount(before)
                .afterCount(after)
                .operator(operator)
                .remark(trimToNull(request.getRemark()))
                .verifiedAt(now)
                .build();
        logRepository.save(verifyLog);

        plan.setVerifiedCount(after);
        plan.setLastVerifiedBy(operator);
        plan.setLastVerifiedAt(now);
        if (after == plan.getBenchCount()) {
            plan.setStatus(AdditionalBenchPlan.STATUS_DEPLOYED);
        }
        AdditionalBenchPlan saved = planRepository.save(plan);
        log.info("加凳预案核销: planId={}, verifyCount={}, before={}, after={}, operator={}",
                id, verifyCount, before, after, operator);
        return getPlanDetail(saved.getId());
    }

    public List<AdditionalBenchPlanDTO> listPlans(Long districtId, Long sectionId, Integer effectiveStatus) {
        if (effectiveStatus != null
                && effectiveStatus != EFFECTIVE_STATUS_PENDING
                && effectiveStatus != EFFECTIVE_STATUS_DEPLOYED
                && effectiveStatus != EFFECTIVE_STATUS_OVERDUE) {
            throw new IllegalArgumentException("预案状态只能为待投放(1)、已投放(2)或逾期(3)");
        }
        List<AdditionalBenchPlan> plans = planRepository.findAllByOrderByPlanDateDescIdDesc();
        Map<Long, TreeNode> nodeMap = getNodeMap();
        Map<Long, List<AdditionalBenchPlanLog>> logsByPlan = getLogsByPlan(
                plans.stream().map(AdditionalBenchPlan::getId).toList());

        return plans.stream()
                .filter(p -> districtId == null || districtId.equals(p.getDistrictId()))
                .filter(p -> sectionId == null || sectionId.equals(p.getSectionId()))
                .map(p -> toDTO(p, nodeMap, logsByPlan.getOrDefault(p.getId(), List.of())))
                .filter(dto -> effectiveStatus == null || effectiveStatus.equals(dto.getEffectiveStatus()))
                .toList();
    }

    public AdditionalBenchPlanDTO getPlanDetail(Long id) {
        AdditionalBenchPlan plan = getPlan(id);
        List<AdditionalBenchPlanLog> logs = logRepository.findByPlanIdOrderByVerifiedAtDescIdDesc(id);
        return toDTO(plan, getNodeMap(), logs);
    }

    /**
     * 供街区树批量填充路段待投放数量和逾期标记，口径与预案列表完全一致。
     */
    public Map<Long, SectionAdditionalBenchSummaryDTO> getSectionSummaries(List<Long> sectionIds) {
        Map<Long, SectionAdditionalBenchSummaryDTO> result = new HashMap<>();
        if (sectionIds == null || sectionIds.isEmpty()) {
            return result;
        }

        LocalDate today = LocalDate.now();
        Map<Long, long[]> stats = new HashMap<>();
        for (AdditionalBenchPlan plan : planRepository.findBySectionIdInOrderByPlanDateDescIdDesc(sectionIds)) {
            if (!Integer.valueOf(AdditionalBenchPlan.STATUS_PENDING).equals(plan.getStatus())) {
                continue;
            }
            int pending = plan.getBenchCount() - (plan.getVerifiedCount() == null ? 0 : plan.getVerifiedCount());
            if (pending <= 0) {
                continue;
            }
            long[] values = stats.computeIfAbsent(plan.getSectionId(), key -> new long[2]);
            values[0] += pending;
            if (plan.getPlanDate() != null && plan.getPlanDate().isBefore(today)) {
                values[1]++;
            }
        }

        for (Long sectionId : sectionIds) {
            long[] values = stats.get(sectionId);
            result.put(sectionId, SectionAdditionalBenchSummaryDTO.builder()
                    .pendingCount(values == null ? 0L : values[0])
                    .overduePlanCount(values == null ? 0L : values[1])
                    .build());
        }
        return result;
    }

    /**
     * 删除路段前校验：只要曾提交过加凳预案（含已投放）就保留台账，不允许直接删除路段。
     */
    public void validateSectionCanDelete(Long sectionId) {
        if (planRepository.existsBySectionId(sectionId)) {
            throw new IllegalStateException("该路段存在节假日加凳预案或投放核销记录，不能删除");
        }
    }

    private AdditionalBenchPlan getPlan(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("加凳预案不存在"));
    }

    private TreeNode validateDistrict(Long districtId) {
        if (districtId == null) {
            throw new IllegalArgumentException("请选择街区");
        }
        TreeNode district = treeNodeRepository.findByIdAndIsDeletedFalse(districtId)
                .orElseThrow(() -> new IllegalArgumentException("所选街区不存在"));
        if (district.getLevel() != 1) {
            throw new IllegalArgumentException("请选择街区节点");
        }
        return district;
    }

    private TreeNode validateSection(Long sectionId, Long districtId) {
        if (sectionId == null) {
            throw new IllegalArgumentException("请选择投放路段");
        }
        TreeNode section = treeNodeRepository.findByIdAndIsDeletedFalse(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("所选投放路段不存在"));
        if (section.getLevel() != 2) {
            throw new IllegalArgumentException("投放位置必须是路段");
        }
        if (!districtId.equals(section.getParentId())) {
            throw new IllegalArgumentException("投放路段不属于所选街区，请重新选择");
        }
        return section;
    }

    private Map<Long, TreeNode> getNodeMap() {
        Map<Long, TreeNode> nodeMap = new HashMap<>();
        for (TreeNode node : treeNodeRepository.findAllActiveNodes()) {
            nodeMap.put(node.getId(), node);
        }
        return nodeMap;
    }

    private Map<Long, List<AdditionalBenchPlanLog>> getLogsByPlan(List<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<AdditionalBenchPlanLog>> result = new HashMap<>();
        for (AdditionalBenchPlanLog planLog : logRepository.findByPlanIdInOrderByVerifiedAtDescIdDesc(planIds)) {
            result.computeIfAbsent(planLog.getPlanId(), key -> new java.util.ArrayList<>()).add(planLog);
        }
        return result;
    }

    private AdditionalBenchPlanDTO toDTO(AdditionalBenchPlan plan,
                                         Map<Long, TreeNode> nodeMap,
                                         List<AdditionalBenchPlanLog> logs) {
        TreeNode district = nodeMap.get(plan.getDistrictId());
        TreeNode section = nodeMap.get(plan.getSectionId());
        int verified = plan.getVerifiedCount() == null ? 0 : plan.getVerifiedCount();
        int pending = Math.max(0, plan.getBenchCount() - verified);
        boolean deployed = Integer.valueOf(AdditionalBenchPlan.STATUS_DEPLOYED).equals(plan.getStatus());
        boolean overdue = !deployed && plan.getPlanDate() != null && plan.getPlanDate().isBefore(LocalDate.now());
        int effectiveStatus = deployed
                ? EFFECTIVE_STATUS_DEPLOYED
                : overdue ? EFFECTIVE_STATUS_OVERDUE : EFFECTIVE_STATUS_PENDING;

        return AdditionalBenchPlanDTO.builder()
                .id(plan.getId())
                .districtId(plan.getDistrictId())
                .districtName(district != null ? district.getName() : "已删除街区")
                .sectionId(plan.getSectionId())
                .sectionName(section != null ? section.getName() : "已删除路段")
                .planDate(plan.getPlanDate())
                .benchCount(plan.getBenchCount())
                .verifiedCount(verified)
                .pendingCount(pending)
                .status(plan.getStatus())
                .effectiveStatus(effectiveStatus)
                .overdue(overdue)
                .remark(plan.getRemark())
                .lastVerifiedBy(plan.getLastVerifiedBy())
                .lastVerifiedAt(plan.getLastVerifiedAt())
                .createdAt(plan.getCreatedAt())
                .logs(logs == null ? List.of() : logs.stream().map(this::toLogDTO)
                        .sorted(Comparator.comparing(AdditionalBenchPlanLogDTO::getVerifiedAt).reversed())
                        .toList())
                .build();
    }

    private AdditionalBenchPlanLogDTO toLogDTO(AdditionalBenchPlanLog logEntry) {
        return AdditionalBenchPlanLogDTO.builder()
                .id(logEntry.getId())
                .planId(logEntry.getPlanId())
                .verifiedCount(logEntry.getVerifiedCount())
                .beforeCount(logEntry.getBeforeCount())
                .afterCount(logEntry.getAfterCount())
                .operator(logEntry.getOperator())
                .remark(logEntry.getRemark())
                .verifiedAt(logEntry.getVerifiedAt())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
