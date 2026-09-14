package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 防滑垫领用/归还流水（一条领出或归还记录）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntiSlipMatRecordDTO {

    private Long id;

    private Long pointId;

    private String pointName;

    private String sectionName;

    private String districtName;

    /** 动作类型：1-领出，2-归还 */
    private Integer actionType;

    /** 动作中文：领出/归还 */
    private String actionTypeLabel;

    /** 本次数量：领出时为领出数量；归还时为本次归还总数 */
    private Integer quantity;

    /** 本次完好归还数量（仅归还时有值） */
    private Integer intactQuantity;

    /** 本次破损数量（仅归还时有值） */
    private Integer damagedQuantity;

    private LocalDateTime operatedAt;

    private String operator;

    private String remark;

    private LocalDateTime createdAt;
}
