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
public class LevelingAcceptanceDTO {

    private Long id;

    private Long pointId;

    private String pointName;

    private String sectionName;

    private String districtName;

    /** 验收日期 */
    private LocalDateTime acceptedAt;

    /** 验收结论：1-合格，0-不合格 */
    private Integer result;

    private String acceptor;

    private String description;

    /** 整改状态：0-待整改，1-已整改（合格记录恒为已整改） */
    private Integer rectifyStatus;

    /** 是否待整改（不合格且未写回执） */
    private Boolean pending;

    private String rectifyNote;

    private String rectifyBy;

    private LocalDateTime rectifiedAt;

    private LocalDateTime createdAt;
}
