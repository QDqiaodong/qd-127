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

    private List<TreeNodeDTO> children;

    private String path;
}
