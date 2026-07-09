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
public class ChangeLogDTO {

    private Long id;

    private Long benchId;

    private String benchCode;

    private Long oldNodeId;

    private String oldNodeName;

    private Long newNodeId;

    private String newNodeName;

    private String changeReason;

    private LocalDateTime changedAt;

    private String changedBy;
}
