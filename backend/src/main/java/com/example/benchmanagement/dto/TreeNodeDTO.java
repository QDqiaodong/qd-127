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

    private List<TreeNodeDTO> children;

    private String path;
}
