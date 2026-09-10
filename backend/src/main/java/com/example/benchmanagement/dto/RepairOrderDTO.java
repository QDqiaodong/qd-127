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
public class RepairOrderDTO {

    private Long id;

    private String code;

    private Long benchId;

    private String benchCode;

    private Long inspectionId;

    private String problemType;

    private Integer severity;

    private String description;

    private String suggestion;

    /** 状态：1-待处理，2-维修中，3-已完成，4-已关闭 */
    private Integer status;

    private String repairResult;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime closedAt;

    private String createdBy;

    private LocalDateTime createdAt;

    private String districtName;

    private String sectionName;

    private String nodeName;

    private LocalDateTime inspectedAt;
}
