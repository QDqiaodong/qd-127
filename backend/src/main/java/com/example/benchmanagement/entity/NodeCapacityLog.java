package com.example.benchmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "node_capacity_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeCapacityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @Column(name = "old_capacity")
    private Integer oldCapacity;

    @Column(name = "new_capacity", nullable = false)
    private Integer newCapacity;

    @Column(name = "occupied_count")
    private Integer occupiedCount;

    @Column(name = "adjust_reason")
    private String adjustReason;

    @Column(name = "adjusted_at")
    private LocalDateTime adjustedAt;

    @Column(name = "adjusted_by")
    private String adjustedBy;

    @PrePersist
    public void prePersist() {
        this.adjustedAt = LocalDateTime.now();
        if (this.adjustedBy == null) {
            this.adjustedBy = "system";
        }
    }
}
