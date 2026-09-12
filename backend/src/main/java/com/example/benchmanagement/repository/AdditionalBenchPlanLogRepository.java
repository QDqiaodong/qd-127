package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.AdditionalBenchPlanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdditionalBenchPlanLogRepository extends JpaRepository<AdditionalBenchPlanLog, Long> {

    List<AdditionalBenchPlanLog> findByPlanIdOrderByVerifiedAtDescIdDesc(Long planId);

    List<AdditionalBenchPlanLog> findByPlanIdInOrderByVerifiedAtDescIdDesc(List<Long> planIds);
}
