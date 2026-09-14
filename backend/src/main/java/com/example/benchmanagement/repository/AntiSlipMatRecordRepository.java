package com.example.benchmanagement.repository;

import com.example.benchmanagement.entity.AntiSlipMatRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AntiSlipMatRecordRepository extends JpaRepository<AntiSlipMatRecord, Long> {

    List<AntiSlipMatRecord> findAllByOrderByOperatedAtDescIdDesc();

    List<AntiSlipMatRecord> findByPointIdOrderByOperatedAtDescIdDesc(Long pointId);

    List<AntiSlipMatRecord> findByPointIdInOrderByOperatedAtDescIdDesc(List<Long> pointIds);
}
