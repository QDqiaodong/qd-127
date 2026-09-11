package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.InspectionTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InspectionTaskRepository extends JpaRepository<InspectionTask, Long> {

    List<InspectionTask> findByPlanIdOrderByPlanDateDescIdDesc(Long planId);

    List<InspectionTask> findAllByOrderByPlanDateDescIdDesc();

    boolean existsByPlanIdAndPlanDate(Long planId, LocalDate planDate);
}
