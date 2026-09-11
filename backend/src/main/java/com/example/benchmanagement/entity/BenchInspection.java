package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bench_inspection")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchInspection {

    /** 巡检结果：正常 */
    public static final int RESULT_NORMAL = 1;
    /** 巡检结果：异常 */
    public static final int RESULT_ABNORMAL = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bench_id", nullable = false)
    private Long benchId;

    @Column(name = "scope_node_id")
    private Long scopeNodeId;

    @Column(name = "inspected_at", nullable = false)
    private LocalDateTime inspectedAt;

    @Column(name = "result", nullable = false)
    private Integer result;

    @Column(name = "problem_type", length = 50)
    private String problemType;

    @Column(name = "severity")
    private Integer severity;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "suggestion", length = 1000)
    private String suggestion;

    @Column(name = "inspector", length = 50)
    private String inspector;

    @Column(name = "repair_order_id")
    private Long repairOrderId;

    /** 来源巡检任务ID（由巡检计划任务派发执行时填写） */
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.inspector == null || this.inspector.isBlank()) {
            this.inspector = "system";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
