package com.tugba.radyoloji_raporlama_asistani.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tugba.radyoloji_raporlama_asistani.dto.PatientResponse;
import com.tugba.radyoloji_raporlama_asistani.entity.Patient;
import com.tugba.radyoloji_raporlama_asistani.exception.ResourceNotFoundException;
import com.tugba.radyoloji_raporlama_asistani.repository.PatientRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public List<PatientResponse> search(String term) {
        List<Patient> patients;
        if (term == null || term.isBlank()) {
            patients = patientRepository.findAll();
        } else {
            String text = term.trim();
            patients = patientRepository
                    .findByFullNameContainingIgnoreCaseOrPatientFileNumberContainingIgnoreCase(text, text);
        }
        return patients.stream().map(PatientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse getById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hasta bulunamadı: " + id));
        return PatientResponse.from(patient);
    }
}
