package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tree_node")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "level")
    private Integer level;

    @Column(name = "name")
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "capacity_updated_at")
    private LocalDateTime capacityUpdatedAt;

    @Column(name = "capacity_updated_reason")
    private String capacityUpdatedReason;

    /** 封闭状态：1-封闭中，0/null-未封闭（仅点位 level=3 使用） */
    @Column(name = "closed")
    private Integer closed;

    /** 封闭开始时间 */
    @Column(name = "closed_start_at")
    private LocalDateTime closedStartAt;

    /** 封闭结束时间（到期后自动解封） */
    @Column(name = "closed_end_at")
    private LocalDateTime closedEndAt;

    /** 封闭原因 */
    @Column(name = "closed_reason")
    private String closedReason;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static final int DEFAULT_CAPACITY = 10;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
