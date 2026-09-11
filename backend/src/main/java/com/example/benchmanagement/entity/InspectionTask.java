package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 巡检任务：由巡检计划按周期生成，同一计划同一计划日期唯一。
 */
@Entity
@Table(name = "inspection_task",
        uniqueConstraints = @UniqueConstraint(name = "uk_plan_date", columnNames = {"plan_id", "plan_date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionTask {

    /** 任务状态：待执行 */
    public static final int STATUS_PENDING = 1;
    /** 任务状态：已执行 */
    public static final int STATUS_EXECUTED = 2;
    /** 查询用状态：逾期（由待执行且计划日期早于今天推导，不落库） */
    public static final int STATUS_OVERDUE = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    /** 生成任务时的巡检范围快照 */
    @Column(name = "scope_node_id", nullable = false)
    private Long scopeNodeId;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "inspector", length = 50)
    private String inspector;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = STATUS_PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
