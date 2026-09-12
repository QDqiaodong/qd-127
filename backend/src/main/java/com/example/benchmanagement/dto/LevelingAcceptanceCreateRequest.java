package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelingAcceptanceCreateRequest {

    /** 点位必选，未选点位不能提交 */
    @NotNull(message = "点位不能为空")
    private Long pointId;

    /** 验收日期 */
    @NotNull(message = "验收日期不能为空")
    private LocalDateTime acceptedAt;

    /** 验收结论：1-合格，0-不合格 */
    @NotNull(message = "验收结论不能为空")
    private Integer result;

    @NotBlank(message = "验收人不能为空")
    private String acceptor;

    /** 验收说明（地面不平情况等，选填） */
    private String description;
}
