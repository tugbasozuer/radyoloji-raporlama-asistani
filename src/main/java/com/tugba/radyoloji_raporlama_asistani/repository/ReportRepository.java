package com.tugba.radyoloji_raporlama_asistani.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tugba.radyoloji_raporlama_asistani.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    List<Report> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    boolean existsByPreviousRecordId(Long previousRecordId);

    boolean existsByExamId(Long examId);

}
