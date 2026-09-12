package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.PointLevelingAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointLevelingAcceptanceRepository extends JpaRepository<PointLevelingAcceptance, Long> {

    List<PointLevelingAcceptance> findAllByOrderByAcceptedAtDescIdDesc();

    List<PointLevelingAcceptance> findByPointIdOrderByAcceptedAtDescIdDesc(Long pointId);

    List<PointLevelingAcceptance> findByPointIdInOrderByAcceptedAtDescIdDesc(List<Long> pointIds);

    /** 某点位是否存在未写回执的不合格验收（即该点位当前是否挂“待整改”）。 */
    boolean existsByPointIdAndResultAndRectifyStatus(Long pointId, Integer result, Integer rectifyStatus);
}
