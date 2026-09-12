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
public class LightingInspectionDTO {

    private Long id;

    private Long pointId;

    private String pointName;

    private String sectionName;

    private String districtName;

    private LocalDateTime inspectedAt;

    /** 照明结论：1-完好，0-异常 */
    private Integer result;

    private Integer lampCount;

    /** 异常类型：缺灯/损坏 */
    private String problemType;

    private String description;

    private String inspector;

    private LocalDateTime createdAt;
}
