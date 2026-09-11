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
public class NodeClosureLogDTO {

    private Long id;

    private Long nodeId;

    private String nodeName;

    /** 动作类型：1-封闭，2-人工解封，3-到期自动解封 */
    private Integer actionType;

    private String actionTypeLabel;

    private LocalDateTime closedStartAt;

    private LocalDateTime closedEndAt;

    private String closedReason;

    private String reopenReason;

    private LocalDateTime operatedAt;

    private String operatedBy;
}
