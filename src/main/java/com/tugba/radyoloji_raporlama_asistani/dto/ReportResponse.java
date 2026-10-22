package com.tugba.radyoloji_raporlama_asistani.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.tugba.radyoloji_raporlama_asistani.entity.Exam;
import com.tugba.radyoloji_raporlama_asistani.entity.Report;
import com.tugba.radyoloji_raporlama_asistani.entity.ResultCategory;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.AnatomicalRegion;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ExamType;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ReportStatus;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UrgencyLevel;

public record ReportResponse(
        Long id,
        PatientResponse patient,
        String protocolNumber,
        ExamType examType,
        AnatomicalRegion anatomicalRegion,
        LocalDateTime examDateTime,
        String requester,
        boolean fromEmergencyDepartment,
        String findingsInput,
        String resultInput,
        boolean normalExam,
        String generatedFindings,
        String generatedResult,
        UrgencyLevel urgencyLevel,
        ReportStatus status,
        boolean aiAssisted,
        Long previousRecordId,
        String createdByEmail,
        String createdByName,
        String approvedByEmail,
        String approvedByName,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        List<String> resultCategories) {

    public static ReportResponse from(Report report) {
        Exam exam = report.getExam();
        return new ReportResponse(
                report.getId(),
                PatientResponse.from(report.getPatient()),
                exam.getProtocolNumber(),
                exam.getExamType(),
                exam.getAnatomicalRegion(),
                exam.getExamDateTime(),
                exam.getRequester(),
                exam.isFromEmergencyDepartment(),
                report.getFindingsInput(),
                report.getResultInput(),
                report.isNormalExam(),
                report.getGeneratedFindings(),
                report.getGeneratedResult(),
                report.getUrgencyLevel(),
                report.getStatus(),
                report.isAiAssisted(),
                report.getPreviousRecord() == null ? null : report.getPreviousRecord().getId(),
                report.getCreatedBy().getEmail(),
                report.getCreatedBy().getDisplayName(),
                report.getApprovedBy() == null ? null : report.getApprovedBy().getEmail(),
                report.getApprovedBy() == null ? null : report.getApprovedBy().getDisplayName(),
                report.getCreatedAt(),
                report.getApprovedAt(),
                report.getResultCategories().stream()
                        .map(ResultCategory::getCategoryName)
                        .sorted()
                        .toList());
    }
}
