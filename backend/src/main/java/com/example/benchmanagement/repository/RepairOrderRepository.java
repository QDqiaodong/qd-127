package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.RepairOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepairOrderRepository extends JpaRepository<RepairOrder, Long> {

    Optional<RepairOrder> findByCode(String code);

    List<RepairOrder> findAllByOrderByCreatedAtDescIdDesc();

    List<RepairOrder> findByBenchIdOrderByCreatedAtDescIdDesc(Long benchId);

    boolean existsByCode(String code);

    /**
     * 统计某长凳处于未完成状态（待处理/维修中）的工单数。
     */
    long countByBenchIdAndStatusIn(Long benchId, List<Integer> statuses);
}
