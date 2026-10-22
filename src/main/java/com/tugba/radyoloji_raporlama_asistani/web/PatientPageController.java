package com.tugba.radyoloji_raporlama_asistani.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tugba.radyoloji_raporlama_asistani.dto.PatientResponse;
import com.tugba.radyoloji_raporlama_asistani.service.PatientService;
import com.tugba.radyoloji_raporlama_asistani.service.ReportService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PatientPageController {

    private final PatientService patientService;
    private final ReportService reportService;

    @GetMapping("/hastalar")
    public String patients(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "hastaId", required = false) Long patientId,
            Model model) {
        List<PatientResponse> patients = patientService.search(q);
        PatientResponse selected = patients.stream()
                .filter(patient -> patient.id().equals(patientId))
                .findFirst()
                .orElse(patients.isEmpty() ? null : patients.get(0));

        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("patients", patients);
        model.addAttribute("selectedPatient", selected);
        model.addAttribute("patientReports",
                selected == null ? List.of() : reportService.listByPatient(selected.id()));
        return "hastalar";
    }
}