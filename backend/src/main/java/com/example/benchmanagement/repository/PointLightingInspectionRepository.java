package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.PointLightingInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointLightingInspectionRepository extends JpaRepository<PointLightingInspection, Long> {

    List<PointLightingInspection> findAllByOrderByInspectedAtDescIdDesc();

    List<PointLightingInspection> findByPointIdOrderByInspectedAtDescIdDesc(Long pointId);

    List<PointLightingInspection> findByPointIdInOrderByInspectedAtDescIdDesc(List<Long> pointIds);
}
