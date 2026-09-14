package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.AntiSlipMatIssueRequest;
import com.example.benchmanagement.dto.AntiSlipMatRecordDTO;
import com.example.benchmanagement.dto.AntiSlipMatReturnRequest;
import com.example.benchmanagement.dto.PointAntiSlipMatDTO;
import com.example.benchmanagement.entity.AntiSlipMatRecord;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.AntiSlipMatRecordRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 防滑垫领用台账：按点位登记领出/归还（含破损），汇总各点位未还数量。
 * 点位台账汇总与街区树标记共用本服务同一套聚合口径，刷新后保持一致。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AntiSlipMatService {

    private final AntiSlipMatRecordRepository recordRepository;
    private final TreeNodeRepository treeNodeRepository;
    private final TreeNodeService treeNodeService;

    /**
     * 领出登记：点位必选、数量大于0；封闭点位禁止领出。
     */
    @Transactional
    public AntiSlipMatRecordDTO issue(AntiSlipMatIssueRequest request) {
        TreeNode point = requirePoint(request.getPointId());
        assertPointNotClosed(point);
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("领出数量必须为大于0的整数");
        }
        if (request.getOperatedAt() == null) {
            throw new IllegalArgumentException("领出时间不能为空");
        }

        AntiSlipMatRecord record = AntiSlipMatRecord.builder()
                .pointId(point.getId())
                .actionType(AntiSlipMatRecord.ACTION_ISSUE)
                .quantity(request.getQuantity())
                .damagedQuantity(0)
                .operatedAt(request.getOperatedAt())
                .operator(normalizeOperator(request.getOperator()))
                .remark(trimToNull(request.getRemark()))
                .build();
        AntiSlipMatRecord saved = recordRepository.save(record);
        log.info("防滑垫领出登记: pointId={}, quantity={}, operator={}",
                point.getId(), request.getQuantity(), saved.getOperator());
        return toRecordDTO(saved, getNodeMap());
    }

    /**
     * 归还登记：本次归还数量 = 完好归还 + 破损；归还数量不能超过当前未还数量。
     */
    @Transactional
    public AntiSlipMatRecordDTO recordReturn(AntiSlipMatReturnRequest request) {
        TreeNode point = requirePoint(request.getPointId());
        assertPointNotClosed(point);
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("归还数量必须为大于0的整数");
        }
        Integer damaged = request.getDamagedQuantity();
        if (damaged == null || damaged < 0) {
            throw new IllegalArgumentException("破损数量必须为大于等于0的整数");
        }
        if (damaged > request.getQuantity()) {
            throw new IllegalArgumentException("破损数量不能大于本次归还数量");
        }
        if (request.getOperatedAt() == null) {
            throw new IllegalArgumentException("归还时间不能为空");
        }

        int outstanding = outstandingOf(point.getId());
        if (outstanding <= 0) {
            throw new IllegalStateException(String.format(
                    "点位【%s】当前没有未归还的防滑垫，无需归还", point.getName()));
        }
        if (request.getQuantity() > outstanding) {
            throw new IllegalArgumentException(String.format(
                    "本次归还%d张超过该点位当前未还数量%d张，请核对后重新登记",
                    request.getQuantity(), outstanding));
        }

        AntiSlipMatRecord record = AntiSlipMatRecord.builder()
                .pointId(point.getId())
                .actionType(AntiSlipMatRecord.ACTION_RETURN)
                .quantity(request.getQuantity())
                .damagedQuantity(damaged)
                .operatedAt(request.getOperatedAt())
                .operator(normalizeOperator(request.getOperator()))
                .remark(trimToNull(request.getRemark()))
                .build();
        AntiSlipMatRecord saved = recordRepository.save(record);
        log.info("防滑垫归还登记: pointId={}, quantity={}, damaged={}, operator={}",
                point.getId(), request.getQuantity(), damaged, saved.getOperator());
        return toRecordDTO(saved, getNodeMap());
    }

    /**
     * 领用/归还流水，可按点位、动作类型、关键字（点位名/经办人/备注）筛选。
     */
    public List<AntiSlipMatRecordDTO> getRecords(Long pointId, Integer actionType, String keyword) {
        if (actionType != null
                && actionType != AntiSlipMatRecord.ACTION_ISSUE
                && actionType != AntiSlipMatRecord.ACTION_RETURN) {
            throw new IllegalArgumentException("动作类型只能为领出(1)或归还(2)");
        }
        List<AntiSlipMatRecord> all;
        if (pointId != null) {
            requirePoint(pointId);
            all = recordRepository.findByPointIdOrderByOperatedAtDescIdDesc(pointId);
        } else {
            all = recordRepository.findAllByOrderByOperatedAtDescIdDesc();
        }
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        Map<Long, TreeNode> nodeMap = getNodeMap();
        Map<Long, String> pointNames = new HashMap<>();
        return all.stream()
                .filter(r -> actionType == null || actionType.equals(r.getActionType()))
                .filter(r -> {
                    if (kw.isEmpty()) {
                        return true;
                    }
                    String pointName = pointNames.computeIfAbsent(r.getPointId(),
                            pid -> treeNodeRepository.findById(pid).map(TreeNode::getName).orElse(""));
                    return pointName.toLowerCase().contains(kw)
                            || (r.getOperator() != null && r.getOperator().toLowerCase().contains(kw))
                            || (r.getRemark() != null && r.getRemark().toLowerCase().contains(kw));
                })
                .map(r -> toRecordDTO(r, nodeMap))
                .toList();
    }

    /**
     * 单个点位的领用/归还流水（台账详情用）。
     */
    public List<AntiSlipMatRecordDTO> getRecordsByPoint(Long pointId) {
        requirePoint(pointId);
        Map<Long, TreeNode> nodeMap = getNodeMap();
        return recordRepository.findByPointIdOrderByOperatedAtDescIdDesc(pointId).stream()
                .map(r -> toRecordDTO(r, nodeMap))
                .toList();
    }

    /**
     * 点位领用台账：每个点位一行（含从未领用的点位，数量为0）。
     * outstanding=true 只保留未还清点位；可按街区/路段/点位范围筛选。
     */
    public List<PointAntiSlipMatDTO> getPointLedgers(Boolean outstanding, Long districtId,
                                                     Long sectionId, Long pointId) {
        List<TreeNode> points = treeNodeRepository.findByLevelAndIsDeletedFalse(3);
        Map<Long, TreeNode> nodeMap = getNodeMap();
        Map<Long, PointAntiSlipMatDTO> ledgerByPoint = new HashMap<>();
        for (TreeNode point : points) {
            ledgerByPoint.put(point.getId(), emptyLedger(point, nodeMap));
        }
        for (AntiSlipMatRecord record : recordRepository.findAllByOrderByOperatedAtDescIdDesc()) {
            PointAntiSlipMatDTO dto = ledgerByPoint.get(record.getPointId());
            // 外键保证流水点位存在；若点位已被删除则跳过，避免台账出现无法归类的行
            if (dto == null) {
                continue;
            }
            applyRecord(dto, record);
            // 流水按操作时间倒序返回，每点位第一次出现即最近一次操作
            if (dto.getLastOperatedAt() == null) {
                dto.setLastOperatedAt(record.getOperatedAt());
                dto.setLastOperator(record.getOperator());
            }
        }
        ledgerByPoint.values().forEach(this::finalizeLedger);

        return ledgerByPoint.values().stream()
                .filter(dto -> pointId == null || pointId.equals(dto.getPointId()))
                .filter(dto -> sectionId == null || sectionId.equals(dto.getSectionId()))
                .filter(dto -> districtId == null || districtId.equals(dto.getDistrictId()))
                .filter(dto -> outstanding == null || outstanding.equals(dto.getOutstanding()))
                .sorted(Comparator
                        .comparing((PointAntiSlipMatDTO d) -> sortOrderOf(nodeMap, d.getDistrictId()))
                        .thenComparing(d -> sortOrderOf(nodeMap, d.getSectionId()))
                        .thenComparing(d -> sortOrderOf(nodeMap, d.getPointId()))
                        .thenComparing(PointAntiSlipMatDTO::getPointId))
                .toList();
    }

    /**
     * 按点位id批量聚合领用台账（只含有点位流水的点位），供街区树批量填充未还标记，
     * 口径与点位领用台账完全一致。
     */
    public Map<Long, PointAntiSlipMatDTO> getLedgerMapByPointIds(List<Long> pointIds) {
        Map<Long, PointAntiSlipMatDTO> map = new HashMap<>();
        if (pointIds == null || pointIds.isEmpty()) {
            return map;
        }
        for (AntiSlipMatRecord record : recordRepository.findByPointIdInOrderByOperatedAtDescIdDesc(pointIds)) {
            PointAntiSlipMatDTO dto = map.get(record.getPointId());
            if (dto == null) {
                dto = PointAntiSlipMatDTO.builder()
                        .pointId(record.getPointId())
                        .issuedCount(0)
                        .returnedCount(0)
                        .intactCount(0)
                        .damagedCount(0)
                        .outstandingCount(0)
                        .hasRecord(false)
                        .build();
                map.put(record.getPointId(), dto);
            }
            applyRecord(dto, record);
            // 流水按操作时间倒序返回，每点位第一次出现即最近一次操作
            if (dto.getLastOperatedAt() == null) {
                dto.setLastOperatedAt(record.getOperatedAt());
                dto.setLastOperator(record.getOperator());
            }
        }
        map.values().forEach(this::finalizeLedger);
        return map;
    }

    /**
     * 删除点位前校验：仍有未还防滑垫的点位不允许删除，避免垫子在外却删掉点位。
     */
    public void validatePointCanDelete(Long pointId) {
        if (outstandingOf(pointId) > 0) {
            throw new IllegalStateException("该点位仍有未归还的防滑垫，请先收回并登记归还后再删除点位");
        }
    }

    private int outstandingOf(Long pointId) {
        int issued = 0;
        int returned = 0;
        for (AntiSlipMatRecord record : recordRepository.findByPointIdOrderByOperatedAtDescIdDesc(pointId)) {
            if (record.getActionType() == AntiSlipMatRecord.ACTION_ISSUE) {
                issued += record.getQuantity();
            } else if (record.getActionType() == AntiSlipMatRecord.ACTION_RETURN) {
                returned += record.getQuantity();
            }
        }
        return Math.max(0, issued - returned);
    }

    private PointAntiSlipMatDTO emptyLedger(TreeNode point, Map<Long, TreeNode> nodeMap) {
        TreeNode section = point.getParentId() != null ? nodeMap.get(point.getParentId()) : null;
        TreeNode district = section != null && section.getParentId() != null
                ? nodeMap.get(section.getParentId()) : null;
        return PointAntiSlipMatDTO.builder()
                .pointId(point.getId())
                .pointName(point.getName())
                .sectionId(section != null ? section.getId() : null)
                .sectionName(section != null ? section.getName() : "")
                .districtId(district != null ? district.getId() : null)
                .districtName(district != null ? district.getName() : "")
                .issuedCount(0)
                .returnedCount(0)
                .intactCount(0)
                .damagedCount(0)
                .outstandingCount(0)
                .outstanding(false)
                .hasRecord(false)
                .build();
    }

    /**
     * 把一条流水累加进点位台账；台账列表与街区树标记都走这里，保证两处同源。
     */
    private void applyRecord(PointAntiSlipMatDTO dto, AntiSlipMatRecord record) {
        dto.setHasRecord(true);
        if (record.getActionType() == AntiSlipMatRecord.ACTION_ISSUE) {
            dto.setIssuedCount(dto.getIssuedCount() + record.getQuantity());
        } else if (record.getActionType() == AntiSlipMatRecord.ACTION_RETURN) {
            dto.setReturnedCount(dto.getReturnedCount() + record.getQuantity());
            dto.setDamagedCount(dto.getDamagedCount()
                    + (record.getDamagedQuantity() != null ? record.getDamagedQuantity() : 0));
        }
    }

    private void finalizeLedger(PointAntiSlipMatDTO dto) {
        int intact = dto.getReturnedCount() - dto.getDamagedCount();
        dto.setIntactCount(Math.max(0, intact));
        int outstanding = Math.max(0, dto.getIssuedCount() - dto.getReturnedCount());
        dto.setOutstandingCount(outstanding);
        dto.setOutstanding(outstanding > 0);
    }

    private int sortOrderOf(Map<Long, TreeNode> nodeMap, Long nodeId) {
        TreeNode node = nodeId != null ? nodeMap.get(nodeId) : null;
        return node != null && node.getSortOrder() != null ? node.getSortOrder() : 0;
    }

    private TreeNode requirePoint(Long pointId) {
        if (pointId == null) {
            throw new IllegalArgumentException("点位不能为空，请先选择点位再提交");
        }
        TreeNode point = treeNodeRepository.findByIdAndIsDeletedFalse(pointId)
                .orElseThrow(() -> new IllegalArgumentException("点位不存在"));
        if (point.getLevel() != 3) {
            throw new IllegalArgumentException("防滑垫领用只能登记到点位(level=3)，请重新选择");
        }
        return point;
    }

    /**
     * 封闭期内点位禁止领用/归还防滑垫，到期或人工解封后恢复。
     */
    private void assertPointNotClosed(TreeNode point) {
        if (treeNodeService.isPointClosed(point.getId())) {
            throw new IllegalStateException(String.format(
                    "点位【%s】处于临时封闭期，封闭期内不办理防滑垫领用/归还，到期或人工解封后恢复",
                    point.getName()));
        }
    }

    private Map<Long, TreeNode> getNodeMap() {
        Map<Long, TreeNode> nodeMap = new HashMap<>();
        for (TreeNode node : treeNodeRepository.findAllActiveNodes()) {
            nodeMap.put(node.getId(), node);
        }
        return nodeMap;
    }

    private String normalizeOperator(String operator) {
        if (operator == null || operator.trim().isEmpty()) {
            return "system";
        }
        return operator.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AntiSlipMatRecordDTO toRecordDTO(AntiSlipMatRecord record, Map<Long, TreeNode> nodeMap) {
        TreeNode point = nodeMap.get(record.getPointId());
        TreeNode section = point != null && point.getParentId() != null
                ? nodeMap.get(point.getParentId()) : null;
        TreeNode district = section != null && section.getParentId() != null
                ? nodeMap.get(section.getParentId()) : null;
        boolean isReturn = record.getActionType() == AntiSlipMatRecord.ACTION_RETURN;
        return AntiSlipMatRecordDTO.builder()
                .id(record.getId())
                .pointId(record.getPointId())
                .pointName(point != null ? point.getName() : "已删除点位")
                .sectionName(section != null ? section.getName() : "")
                .districtName(district != null ? district.getName() : "")
                .actionType(record.getActionType())
                .actionTypeLabel(isReturn ? "归还" : "领出")
                .quantity(record.getQuantity())
                .intactQuantity(isReturn
                        ? record.getQuantity() - (record.getDamagedQuantity() != null ? record.getDamagedQuantity() : 0)
                        : null)
                .damagedQuantity(isReturn ? record.getDamagedQuantity() : null)
                .operatedAt(record.getOperatedAt())
                .operator(record.getOperator())
                .remark(record.getRemark())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
