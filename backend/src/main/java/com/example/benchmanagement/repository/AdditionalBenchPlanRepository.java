package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.AdditionalBenchPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdditionalBenchPlanRepository extends JpaRepository<AdditionalBenchPlan, Long> {

    List<AdditionalBenchPlan> findAllByOrderByPlanDateDescIdDesc();

    List<AdditionalBenchPlan> findByDistrictIdOrderByPlanDateDescIdDesc(Long districtId);

    List<AdditionalBenchPlan> findBySectionIdInOrderByPlanDateDescIdDesc(List<Long> sectionIds);

    boolean existsBySectionId(Long sectionId);
}
