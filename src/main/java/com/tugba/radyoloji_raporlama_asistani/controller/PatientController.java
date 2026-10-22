package com.tugba.radyoloji_raporlama_asistani.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tugba.radyoloji_raporlama_asistani.dto.PatientResponse;
import com.tugba.radyoloji_raporlama_asistani.dto.ReportResponse;
import com.tugba.radyoloji_raporlama_asistani.service.PatientService;
import com.tugba.radyoloji_raporlama_asistani.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final ReportService reportService;

    @GetMapping
    public List<PatientResponse> search(@RequestParam(required = false) String name) {
        return patientService.search(name);
    }

    @GetMapping("/{id}")
    public PatientResponse getById(@PathVariable Long id) {
        return patientService.getById(id);
    }

    @GetMapping("/{id}/reports")
    public List<ReportResponse> listReports(@PathVariable Long id) {
        return reportService.listByPatient(id);
    }
}