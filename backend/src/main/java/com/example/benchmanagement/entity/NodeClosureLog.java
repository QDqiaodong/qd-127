package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 点位封闭/解封台账。
 */
@Entity
@Table(name = "node_closure_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeClosureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    /** 动作类型：1-封闭，2-人工解封，3-到期自动解封 */
    @Column(name = "action_type", nullable = false)
    private Integer actionType;

    /** 封闭开始时间（封闭动作记录） */
    @Column(name = "closed_start_at")
    private LocalDateTime closedStartAt;

    /** 封闭结束时间（封闭动作记录的计划结束时间） */
    @Column(name = "closed_end_at")
    private LocalDateTime closedEndAt;

    /** 封闭原因 */
    @Column(name = "closed_reason")
    private String closedReason;

    /** 解封原因（人工解封时填写） */
    @Column(name = "reopen_reason")
    private String reopenReason;

    @Column(name = "operated_at")
    private LocalDateTime operatedAt;

    @Column(name = "operated_by")
    private String operatedBy;

    public static final int ACTION_CLOSE = 1;
    public static final int ACTION_REOPEN_MANUAL = 2;
    public static final int ACTION_REOPEN_AUTO = 3;

    @PrePersist
    public void prePersist() {
        this.operatedAt = LocalDateTime.now();
        if (this.operatedBy == null) {
            this.operatedBy = "system";
        }
    }
}
