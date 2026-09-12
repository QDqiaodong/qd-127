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
public class AdditionalBenchPlanLogDTO {

    private Long id;
    private Long planId;
    private Integer verifiedCount;
    private Integer beforeCount;
    private Integer afterCount;
    private String operator;
    private String remark;
    private LocalDateTime verifiedAt;
}
