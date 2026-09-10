package com.example.benchmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionDTO {

    private Long id;

    private Long benchId;

    private String benchCode;

    private Long scopeNodeId;

    private String scopeNodeName;

    private LocalDateTime inspectedAt;

    /** 巡检结果：1-正常，0-异常 */
    private Integer result;

    private String problemType;

    private Integer severity;

    private String description;

    private String suggestion;

    private String inspector;

    private Long repairOrderId;

    private String repairOrderCode;

    /** 当前工单状态（若已生成工单） */
    private Integer repairOrderStatus;

    private String districtName;

    private String sectionName;

    private String nodeName;

    private LocalDateTime createdAt;
}
