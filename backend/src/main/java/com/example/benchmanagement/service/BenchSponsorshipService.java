package com.example.benchmanagement.service;

import com.example.benchmanagement.dto.BenchSponsorshipDTO;
import com.example.benchmanagement.dto.BenchSponsorshipRequest;
import com.example.benchmanagement.entity.BenchSponsorship;
import com.example.benchmanagement.entity.TreeNode;
import com.example.benchmanagement.repository.BenchSponsorshipRepository;
import com.example.benchmanagement.repository.TreeNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BenchSponsorshipService {

    public static final int EFFECTIVE_STATUS_PENDING = 1;
    public static final int EFFECTIVE_STATUS_ACTIVE = 2;
    public static final int EFFECTIVE_STATUS_EXPIRED = 3;
    public static final int EFFECTIVE_STATUS_EXPIRING_SOON = 4;

    /** 临期提醒窗口：结束日前3天（含结束日当天）开始在台账和街区树提示。 */
    public static final int EXPIRING_SOON_DAYS = 3;

    private final BenchSponsorshipRepository sponsorshipRepository;
    private final TreeNodeRepository treeNodeRepository;

    @Transactional
    public BenchSponsorshipDTO createSponsorship(BenchSponsorshipRequest request) {
        TreeNode point = validatePoint(request.getPointId());

        String merchantName = requireText(request.getMerchantName(), "请填写商户名称");
        String sponsorshipText = requireText(request.getSponsorshipText(), "请填写冠名文案");
        if (merchantName.length() > 100) {
            throw new IllegalArgumentException("商户名称不能超过100个字符");
        }
        if (sponsorshipText.length() > 200) {
            throw new IllegalArgumentException("冠名文案不能超过200个字符");
        }

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        if (startDate == null) {
            throw new IllegalArgumentException("请选择冠名开始日期");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("请选择冠名结束日期");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("冠名结束日期不能早于开始日期");
        }

        BenchSponsorship sponsorship = BenchSponsorship.builder()
                .pointId(point.getId())
                .merchantName(merchantName)
                .sponsorshipText(sponsorshipText)
                .startDate(startDate)
                .endDate(endDate)
                .build();
        BenchSponsorship saved = sponsorshipRepository.save(sponsorship);
        log.info("新建商户冠名记录: id={}, pointId={}, merchant={}, {} ~ {}",
                saved.getId(), point.getId(), merchantName, startDate, endDate);
        return toDTO(saved, getNodeMap());
    }

    /**
     * 冠名台账列表，可按街区、路段、点位、展示状态筛选。
     * 状态：1-待生效，2-生效中，3-已过期，4-即将到期（生效中且进入结束日前3天窗口）。
     * 过期和临期均由结束日期与当天动态比较，因此刷新页面或隔天打开状态会自动更新。
     */
    public List<BenchSponsorshipDTO> listSponsorships(Long districtId, Long sectionId,
                                                      Long pointId, Integer effectiveStatus) {
        if (effectiveStatus != null
                && effectiveStatus != EFFECTIVE_STATUS_PENDING
                && effectiveStatus != EFFECTIVE_STATUS_ACTIVE
                && effectiveStatus != EFFECTIVE_STATUS_EXPIRED
                && effectiveStatus != EFFECTIVE_STATUS_EXPIRING_SOON) {
            throw new IllegalArgumentException(
                    "冠名状态只能为待生效(1)、生效中(2)、已过期(3)或即将到期(4)");
        }
        List<BenchSponsorship> sponsorships = sponsorshipRepository.findAllByOrderByCreatedAtDescIdDesc();
        Map<Long, TreeNode> nodeMap = getNodeMap();

        return sponsorships.stream()
                .map(s -> toDTO(s, nodeMap))
                .filter(dto -> pointId == null || pointId.equals(dto.getPointId()))
                .filter(dto -> sectionId == null || sectionId.equals(dto.getSectionId()))
                .filter(dto -> districtId == null || districtId.equals(dto.getDistrictId()))
                .filter(dto -> effectiveStatus == null || effectiveStatus.equals(dto.getEffectiveStatus()))
                .toList();
    }

    /**
     * 查询点位上进入临期窗口的有效冠名，供街区树批量填充标记。
     * 同一点位可能存在多条记录，调用方按 endDate 取最早一条展示。
     */
    public List<BenchSponsorship> getExpiringSoonSponsorships(List<Long> pointIds, LocalDate today) {
        if (pointIds == null || pointIds.isEmpty()) {
            return List.of();
        }
        LocalDate effectiveToday = today != null ? today : LocalDate.now();
        return sponsorshipRepository.findExpiringSoonByPointIds(
                pointIds, effectiveToday, effectiveToday.plusDays(EXPIRING_SOON_DAYS));
    }

    /**
     * 删除点位前校验：只要登记过商户冠名（含已过期）就保留台账，不允许直接删除点位。
     */
    public void validatePointCanDelete(Long pointId) {
        if (sponsorshipRepository.existsByPointId(pointId)) {
            throw new IllegalStateException("该点位存在商户冠名台账记录，不能删除");
        }
    }

    private TreeNode validatePoint(Long pointId) {
        if (pointId == null) {
            throw new IllegalArgumentException("请选择冠名点位");
        }
        TreeNode point = treeNodeRepository.findByIdAndIsDeletedFalse(pointId)
                .orElseThrow(() -> new IllegalArgumentException("所选冠名点位不存在"));
        if (point.getLevel() != 3) {
            throw new IllegalArgumentException("冠名只能登记到点位(level=3)，请重新选择");
        }
        return point;
    }

    private Map<Long, TreeNode> getNodeMap() {
        Map<Long, TreeNode> nodeMap = new HashMap<>();
        for (TreeNode node : treeNodeRepository.findAllActiveNodes()) {
            nodeMap.put(node.getId(), node);
        }
        return nodeMap;
    }

    private BenchSponsorshipDTO toDTO(BenchSponsorship sponsorship, Map<Long, TreeNode> nodeMap) {
        TreeNode point = nodeMap.get(sponsorship.getPointId());
        TreeNode section = point != null && point.getParentId() != null
                ? nodeMap.get(point.getParentId()) : null;
        TreeNode district = section != null && section.getParentId() != null
                ? nodeMap.get(section.getParentId()) : null;

        LocalDate today = LocalDate.now();
        boolean expired = sponsorship.getEndDate() != null && sponsorship.getEndDate().isBefore(today);
        boolean active = !expired
                && sponsorship.getStartDate() != null && !sponsorship.getStartDate().isAfter(today);
        long daysRemaining = sponsorship.getEndDate() == null
                ? 0L : ChronoUnit.DAYS.between(today, sponsorship.getEndDate());
        boolean expiringSoon = active && daysRemaining >= 0 && daysRemaining <= EXPIRING_SOON_DAYS;
        int effectiveStatus = expired
                ? EFFECTIVE_STATUS_EXPIRED
                : expiringSoon ? EFFECTIVE_STATUS_EXPIRING_SOON
                : active ? EFFECTIVE_STATUS_ACTIVE : EFFECTIVE_STATUS_PENDING;

        return BenchSponsorshipDTO.builder()
                .id(sponsorship.getId())
                .pointId(sponsorship.getPointId())
                .pointName(point != null ? point.getName() : "已删除点位")
                .sectionId(section != null ? section.getId() : null)
                .sectionName(section != null ? section.getName() : "已删除路段")
                .districtId(district != null ? district.getId() : null)
                .districtName(district != null ? district.getName() : "已删除街区")
                .merchantName(sponsorship.getMerchantName())
                .sponsorshipText(sponsorship.getSponsorshipText())
                .startDate(sponsorship.getStartDate())
                .endDate(sponsorship.getEndDate())
                .effectiveStatus(effectiveStatus)
                .expiringSoon(expiringSoon)
                .daysRemaining(daysRemaining)
                .expiringSoonDays(EXPIRING_SOON_DAYS)
                .expired(expired)
                .createdAt(sponsorship.getCreatedAt())
                .build();
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
