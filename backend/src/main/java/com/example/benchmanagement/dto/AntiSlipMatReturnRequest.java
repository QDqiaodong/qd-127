package com.example.benchmanagement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 防滑垫归还登记请求。本次归还数量 = 完好归还数量 + 破损数量。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntiSlipMatReturnRequest {

    @NotNull(message = "点位不能为空")
    private Long pointId;

    /** 本次归还总数（完好归还 + 破损，必须大于0） */
    @NotNull(message = "归还数量不能为空")
    @Positive(message = "归还数量必须大于0")
    private Integer quantity;

    /** 其中破损数量（不能大于本次归还数量，也不能超过当前未还数量） */
    @NotNull(message = "破损数量不能为空")
    private Integer damagedQuantity;

    @NotNull(message = "归还时间不能为空")
    private LocalDateTime operatedAt;

    private String operator;

    private String remark;
}
