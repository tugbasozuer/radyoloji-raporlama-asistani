package com.tugba.radyoloji_raporlama_asistani.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tugba.radyoloji_raporlama_asistani.entity.Exam;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    boolean existsByProtocolNumber(String protocolNumber);

    @Query(value = "SELECT nextval('protocol_number_seq')", nativeQuery = true)
    long nextProtocolSequence();

}
