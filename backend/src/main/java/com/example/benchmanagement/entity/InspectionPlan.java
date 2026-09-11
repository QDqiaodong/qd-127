package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 巡检计划：按街区/路段/点位配置周期、计划日期、检查人和启用状态。
 */
@Entity
@Table(name = "inspection_plan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionPlan {

    /** 巡检周期：每天 */
    public static final int CYCLE_DAILY = 1;
    /** 巡检周期：每周 */
    public static final int CYCLE_WEEKLY = 2;
    /** 巡检周期：每月 */
    public static final int CYCLE_MONTHLY = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "scope_node_id", nullable = false)
    private Long scopeNodeId;

    @Column(name = "cycle_type", nullable = false)
    private Integer cycleType;

    /** 首次计划日期，后续按周期顺延生成任务 */
    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "inspector", nullable = false, length = 50)
    private String inspector;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.enabled == null) {
            this.enabled = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
