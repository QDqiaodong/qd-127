package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.InspectionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionPlanRepository extends JpaRepository<InspectionPlan, Long> {

    List<InspectionPlan> findAllByOrderByCreatedAtDescIdDesc();

    List<InspectionPlan> findByEnabledTrue();
}
