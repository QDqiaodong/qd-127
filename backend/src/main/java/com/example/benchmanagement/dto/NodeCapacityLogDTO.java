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
public class NodeCapacityLogDTO {

    private Long id;

    private Long nodeId;

    private String nodeName;

    private Integer oldCapacity;

    private Integer newCapacity;

    private Integer occupiedCount;

    private String adjustReason;

    private LocalDateTime adjustedAt;

    private String adjustedBy;
}
