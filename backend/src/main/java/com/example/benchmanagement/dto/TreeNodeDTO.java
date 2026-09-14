package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeNodeDTO {

    private Long id;

    private Long parentId;

    @NotNull(message = "层级不能为空")
    private Integer level;

    @NotBlank(message = "名称不能为空")
    private String name;

    private Integer sortOrder;

    /** 可摆放长凳上限（仅点位 level=3 有效） */
    private Integer capacity;

    /** 当前占用数（仅点位 level=3 有效） */
    private Long occupiedCount;

    /** 剩余可摆放数（仅点位 level=3 有效） */
    private Long remainingCount;

    /** 点位容量状态：FULL-已满，NEARLY_FULL-将满，null-充足（仅点位 level=3 有效） */
    private String capacityStatus;

    /** 容量最近调整时间 */
    private java.time.LocalDateTime capacityUpdatedAt;

    /** 容量最近调整原因 */
    private String capacityUpdatedReason;

    /** 是否封闭中（仅点位 level=3 有效，已考虑到期自动解封） */
    private Boolean closed;

    /** 封闭开始时间 */
    private java.time.LocalDateTime closedStartAt;

    /** 封闭结束时间 */
    private java.time.LocalDateTime closedEndAt;

    /** 封闭原因 */
    private String closedReason;

    /** 最近一次夜间照明结论：1-完好，0-异常，null-未巡查（仅点位 level=3 有效） */
    private Integer lightingResult;

    /** 最近一次照明异常类型：缺灯/损坏（仅点位 level=3 有效） */
    private String lightingProblemType;

    /** 最近一次照明巡查时间（仅点位 level=3 有效） */
    private java.time.LocalDateTime lightingInspectedAt;

    /** 路段下最近一次照明结论为异常的点位数（仅路段 level=2 有效，与点位照明标记同源） */
    private Integer lightingAbnormalCount;

    /** 最近一次遮阳棚结论：1-完好，0-异常，null-未巡查（仅点位 level=3 有效） */
    private Integer sunshadeResult;

    /** 最近一次遮阳棚破损面积（平方米，仅点位 level=3 有效） */
    private java.math.BigDecimal sunshadeDamagedArea;

    /** 最近一次遮阳棚破损位置（仅点位 level=3 有效） */
    private String sunshadeDamagedLocation;

    /** 最近一次遮阳棚巡查时间（仅点位 level=3 有效） */
    private java.time.LocalDateTime sunshadeInspectedAt;

    /** 路段下最近一次遮阳棚结论为异常的点位数（仅路段 level=2 有效，与点位遮阳棚标记同源） */
    private Integer sunshadeAbnormalCount;

    /** 路段下各点位剩余容量加总（仅路段 level=2 有效；没有点位时为 null） */
    private Long capacityRemainingSum;

    /**
     * 路段容量告警：有点位、剩余容量加总低于阈值且至少存在一个已满/将满点位
     * （仅路段 level=2 有效；没有点位或没有已满/将满点位时恒为 false，保证下钻名单非空）
     */
    private Boolean capacityAlarm;

    /** 路段容量告警生效阈值（仅路段 level=2 有效，与告警判断同源） */
    private Integer capacityAlarmThreshold;

    /** 路段下已满点位数（仅路段 level=2 有效） */
    private Integer capacityFullCount;

    /** 路段下将满点位数（仅路段 level=2 有效） */
    private Integer capacityNearlyFullCount;

    /** 路段待投放加凳数量（含逾期未投放，仅路段 level=2 有效） */
    private Long additionalBenchPendingCount;

    /** 路段下逾期且仍未完全投放的加凳预案数（仅路段 level=2 有效） */
    private Integer additionalBenchOverduePlanCount;

    /** 点位即将到期的有效冠名数（仅点位 level=3 有效；没有时为0） */
    private Integer sponsorshipExpiringCount;

    /** 点位最早到期的冠名结束日期（仅点位 level=3 有效） */
    private java.time.LocalDate sponsorshipNearestEndDate;

    /** 点位最早到期的冠名剩余天数（仅点位 level=3 有效） */
    private Long sponsorshipNearestDaysRemaining;

    /** 点位最早到期的冠名商户（仅点位 level=3 有效） */
    private String sponsorshipNearestMerchantName;

    /** 点位最早到期的冠名文案（仅点位 level=3 有效） */
    private String sponsorshipNearestText;

    /** 路段下即将到期的有效冠名记录数（仅路段 level=2 有效；没有时为0） */
    private Integer sponsorshipExpiringSectionCount;

    /** 点位防滑垫领出合计（仅点位 level=3 有效；从未领用为0） */
    private Integer antiSlipMatIssuedCount;

    /** 点位防滑垫归还合计（完好归还 + 破损，仅点位 level=3 有效） */
    private Integer antiSlipMatReturnedCount;

    /** 点位防滑垫破损合计（仅点位 level=3 有效） */
    private Integer antiSlipMatDamagedCount;

    /** 点位防滑垫未还数量 = 领出 - 归还（仅点位 level=3 有效；大于0即未还清） */
    private Integer antiSlipMatOutstandingCount;

    /** 路段下防滑垫未还清点位数（仅路段 level=2 有效，与点位未还标记同源） */
    private Integer antiSlipMatOutstandingPointCount;

    /** 路段下防滑垫未还数量加总（仅路段 level=2 有效） */
    private Integer antiSlipMatOutstandingMatCount;

    private List<TreeNodeDTO> children;

    private String path;
}
