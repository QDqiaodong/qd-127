package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "repair_order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairOrder {

    /** 工单状态：待处理 */
    public static final int STATUS_PENDING = 1;
    /** 工单状态：维修中 */
    public static final int STATUS_IN_PROGRESS = 2;
    /** 工单状态：已完成 */
    public static final int STATUS_COMPLETED = 3;
    /** 工单状态：已关闭 */
    public static final int STATUS_CLOSED = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "bench_id", nullable = false)
    private Long benchId;

    @Column(name = "inspection_id")
    private Long inspectionId;

    @Column(name = "problem_type", length = 50)
    private String problemType;

    @Column(name = "severity")
    private Integer severity;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "suggestion", length = 1000)
    private String suggestion;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "repair_result", length = 1000)
    private String repairResult;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

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
        if (this.createdBy == null || this.createdBy.isBlank()) {
            this.createdBy = "system";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
