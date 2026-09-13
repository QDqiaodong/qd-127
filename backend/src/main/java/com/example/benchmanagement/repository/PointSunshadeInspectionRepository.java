package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.PointSunshadeInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointSunshadeInspectionRepository extends JpaRepository<PointSunshadeInspection, Long> {

    List<PointSunshadeInspection> findAllByOrderByInspectedAtDescIdDesc();

    List<PointSunshadeInspection> findByPointIdOrderByInspectedAtDescIdDesc(Long pointId);

    List<PointSunshadeInspection> findByPointIdInOrderByInspectedAtDescIdDesc(List<Long> pointIds);
}
