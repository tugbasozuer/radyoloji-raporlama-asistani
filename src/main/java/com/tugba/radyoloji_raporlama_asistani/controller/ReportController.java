package com.tugba.radyoloji_raporlama_asistani.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tugba.radyoloji_raporlama_asistani.dto.ReportCreateRequest;
import com.tugba.radyoloji_raporlama_asistani.dto.ReportResponse;
import com.tugba.radyoloji_raporlama_asistani.dto.ReportUpdateRequest;
import com.tugba.radyoloji_raporlama_asistani.service.ReportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(@Valid @RequestBody ReportCreateRequest request, Principal principal) {
        return reportService.create(request, principal.getName());
    }

    @GetMapping("/mine")
    public List<ReportResponse> listMine(Principal principal) {
        return reportService.listMine(principal.getName());
    }

    @GetMapping("/{id}")
    public ReportResponse getById(@PathVariable Long id) {
        return reportService.getById(id);
    }

    @PutMapping("/{id}")
    public ReportResponse update(@PathVariable Long id,
            @Valid @RequestBody ReportUpdateRequest request, Principal principal) {
        return reportService.update(id, request, principal.getName());
    }

    @PostMapping("/{id}/generate-ai")
    public ReportResponse generateWithAi(@PathVariable Long id, Principal principal) {
        return reportService.generateWithAi(id, principal.getName());
    }

    @PostMapping("/{id}/approve")
    public ReportResponse approve(@PathVariable Long id, Principal principal) {
        return reportService.approve(id, principal.getName());
    }

    @PostMapping("/{id}/corrections")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse openCorrection(@PathVariable Long id, Principal principal) {
        return reportService.openCorrection(id, principal.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal principal) {
        reportService.delete(id, principal.getName());
    }
}