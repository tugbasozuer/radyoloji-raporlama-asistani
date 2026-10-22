package com.tugba.radyoloji_raporlama_asistani.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tugba.radyoloji_raporlama_asistani.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientFileNumber(String patientFileNumber);

    List<Patient> findByFullNameContainingIgnoreCase(String name);

    List<Patient> findByFullNameContainingIgnoreCaseOrPatientFileNumberContainingIgnoreCase(String name, String fileNumber);

}
