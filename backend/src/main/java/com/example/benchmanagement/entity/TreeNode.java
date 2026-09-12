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

    /** 路段容量告警阈值：路段下各点位剩余容量加总低于该值时，路段标记容量告警 */
    public static final int SECTION_CAPACITY_ALARM_THRESHOLD = 10;

    /** 点位将满阈值：剩余容量小于等于该值视为将满（剩余为0为已满） */
    public static final int POINT_NEARLY_FULL_THRESHOLD = 2;

    /** 点位容量状态：已满（剩余0） */
    public static final String CAPACITY_STATUS_FULL = "FULL";

    /** 点位容量状态：将满（剩余小于等于阈值） */
    public static final String CAPACITY_STATUS_NEARLY_FULL = "NEARLY_FULL";

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
