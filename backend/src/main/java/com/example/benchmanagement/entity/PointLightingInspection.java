package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_lighting_inspection")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointLightingInspection {

    /** 照明结论：完好 */
    public static final int RESULT_INTACT = 1;
    /** 照明结论：异常 */
    public static final int RESULT_ABNORMAL = 0;

    /** 异常类型：缺灯 */
    public static final String PROBLEM_MISSING_LAMP = "缺灯";
    /** 异常类型：损坏 */
    public static final String PROBLEM_DAMAGED = "损坏";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_id", nullable = false)
    private Long pointId;

    @Column(name = "inspected_at", nullable = false)
    private LocalDateTime inspectedAt;

    @Column(name = "result", nullable = false)
    private Integer result;

    @Column(name = "lamp_count", nullable = false)
    private Integer lampCount;

    @Column(name = "problem_type", length = 20)
    private String problemType;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "inspector", length = 50, nullable = false)
    private String inspector;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
