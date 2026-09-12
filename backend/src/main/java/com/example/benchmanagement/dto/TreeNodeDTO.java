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

    /** 路段下各点位剩余容量加总（仅路段 level=2 有效；没有点位时为 null） */
    private Long capacityRemainingSum;

    /** 路段容量告警：有点位且剩余容量加总低于阈值（仅路段 level=2 有效，没有点位时恒为 false） */
    private Boolean capacityAlarm;

    /** 路段容量告警生效阈值（仅路段 level=2 有效，与告警判断同源） */
    private Integer capacityAlarmThreshold;

    /** 路段下已满点位数（仅路段 level=2 有效） */
    private Integer capacityFullCount;

    /** 路段下将满点位数（仅路段 level=2 有效） */
    private Integer capacityNearlyFullCount;

    private List<TreeNodeDTO> children;

    private String path;
}
