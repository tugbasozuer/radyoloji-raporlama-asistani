package com.tugba.radyoloji_raporlama_asistani.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tugba.radyoloji_raporlama_asistani.ai.AiReportGenerator;
import com.tugba.radyoloji_raporlama_asistani.ai.GenerationResult;
import com.tugba.radyoloji_raporlama_asistani.ai.ReportGenerator;
import com.tugba.radyoloji_raporlama_asistani.dto.ReportCreateRequest;
import com.tugba.radyoloji_raporlama_asistani.dto.ReportResponse;
import com.tugba.radyoloji_raporlama_asistani.dto.ReportUpdateRequest;
import com.tugba.radyoloji_raporlama_asistani.entity.AppUser;
import com.tugba.radyoloji_raporlama_asistani.entity.Exam;
import com.tugba.radyoloji_raporlama_asistani.entity.Patient;
import com.tugba.radyoloji_raporlama_asistani.entity.Report;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ExamType;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ReportStatus;
import com.tugba.radyoloji_raporlama_asistani.exception.ConflictException;
import com.tugba.radyoloji_raporlama_asistani.exception.ForbiddenException;
import com.tugba.radyoloji_raporlama_asistani.exception.ResourceNotFoundException;
import com.tugba.radyoloji_raporlama_asistani.repository.AppUserRepository;
import com.tugba.radyoloji_raporlama_asistani.repository.ExamRepository;
import com.tugba.radyoloji_raporlama_asistani.repository.PatientRepository;
import com.tugba.radyoloji_raporlama_asistani.repository.ReportRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ExamRepository examRepository;
    private final PatientRepository patientRepository;
    private final AppUserRepository appUserRepository;
    private final ReportGenerator reportGenerator;
    private final AiReportGenerator aiReportGenerator;
    private final RateLimitService rateLimitService;

    private static final String DEFAULT_REQUESTER = "Acil Servis";

    @Transactional
    public ReportResponse create(ReportCreateRequest reportCreateRequest, String userEmail) {

        Patient patient = patientRepository.findById(reportCreateRequest.patientId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Hasta bulunamadı: " + reportCreateRequest.patientId()));

        AppUser user = findUser(userEmail);

        Exam exam = new Exam();
        exam.setPatient(patient);
        exam.setProtocolNumber(generateProtocolNumber());
        exam.setExamType(reportCreateRequest.examType());
        exam.setAnatomicalRegion(reportCreateRequest.anatomicalRegion());
        exam.setExamDateTime(LocalDateTime.now());
        exam.setRequester(DEFAULT_REQUESTER);
        exam.setFromEmergencyDepartment(reportCreateRequest.fromEmergencyDepartment());
        examRepository.save(exam);

        Report report = new Report();
        report.setExam(exam);
        report.setPatient(patient);
        report.setFindingsInput(reportCreateRequest.findingsInput());
        report.setResultInput(reportCreateRequest.resultInput());
        report.setNormalExam(reportCreateRequest.normalExam());
        report.setStatus(ReportStatus.TASLAK);
        report.setCreatedBy(user);
        report.setCreatedAt(LocalDateTime.now());

        applyGeneration(report, reportGenerator.generate(exam, reportCreateRequest.findingsInput(),
                reportCreateRequest.resultInput(), reportCreateRequest.normalExam()));

        reportRepository.save(report);
        return ReportResponse.from(report);
    }

    private String generateProtocolNumber() {
        return String.format("P-%d-%06d", LocalDateTime.now().getYear(), examRepository.nextProtocolSequence());
    }

    @Transactional(readOnly = true)
    public ReportResponse getById(Long id) {
        return ReportResponse.from(findReport(id));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listMine(String userEmail) {
        AppUser user = findUser(userEmail);
        return reportRepository.findByCreatedByIdOrderByCreatedAtDesc(user.getId())
                .stream().map(ReportResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listMine(String userEmail, ReportStatus status, ExamType examType) {
        return listMine(userEmail).stream()
                .filter(report -> status == null || report.status() == status)
                .filter(report -> examType == null || report.examType() == examType)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listByPatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Hasta bulunamadı: " + patientId);
        }
        return reportRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream().map(ReportResponse::from).toList();
    }

    @Transactional
    public ReportResponse update(Long reportId, ReportUpdateRequest reportUpdateRequest, String userEmail) {
        Report report = findReport(reportId);
        requireCreator(report, userEmail);
        requireDraft(report);

        boolean unchanged = Objects.equals(report.getFindingsInput(), reportUpdateRequest.findingsInput())
                && Objects.equals(report.getResultInput(), reportUpdateRequest.resultInput())
                && report.isNormalExam() == reportUpdateRequest.normalExam();
        if (unchanged) {
            return ReportResponse.from(report);
        }

        report.setFindingsInput(reportUpdateRequest.findingsInput());
        report.setResultInput(reportUpdateRequest.resultInput());
        report.setNormalExam(reportUpdateRequest.normalExam());

        applyGeneration(report, reportGenerator.generate(
                report.getExam(),
                reportUpdateRequest.findingsInput(),
                reportUpdateRequest.resultInput(),
                reportUpdateRequest.normalExam()));

        return ReportResponse.from(report);
    }

    @Transactional
    public ReportResponse generateWithAi(Long reportId, String userEmail) {
        Report report = findReport(reportId);
        requireCreator(report, userEmail);
        requireDraft(report);

        if (report.isNormalExam() || report.isAiAssisted()) {
            return ReportResponse.from(report);
        }

        rateLimitService.checkAiGeneration(userEmail);

        applyGeneration(report, aiReportGenerator.generate(
                report.getFindingsInput(),
                report.getResultInput()));

        return ReportResponse.from(report);
    }

    @Transactional
    public void delete(Long reportId, String userEmail) {
        Report report = findReport(reportId);
        requireCreator(report, userEmail);
        requireDraft(report);

        Exam exam = report.getExam();
        reportRepository.delete(report);

        if (!reportRepository.existsByExamId(exam.getId())) {
            examRepository.delete(exam);
        }

    }

    @Transactional
    public ReportResponse approve(Long reportId, String userEmail) {
        Report report = findReport(reportId);
        requireCreator(report, userEmail);
        requireDraft(report);

        if (isBlank(report.getGeneratedFindings()) || isBlank(report.getGeneratedResult())) {
            throw new ConflictException("Bulgular ve Sonuç dolu olmadan rapor onaylanamaz.");
        }

        report.setStatus(ReportStatus.ONAYLANDI);
        report.setApprovedBy(findUser(userEmail));
        report.setApprovedAt(LocalDateTime.now());

        return ReportResponse.from(report);

    }

    @Transactional
    public ReportResponse openCorrection(Long id, String userEmail) {
        Report original = findReport(id);

        if (original.getStatus() != ReportStatus.ONAYLANDI) {
            throw new ConflictException("Yalnızca onaylanmış raporlar için düzeltme açılabilir.");
        }
        if (reportRepository.existsByPreviousRecordId(id)) {
            throw new ConflictException("Bu rapor için zaten bir düzeltme açılmış.");
        }

        Report correction = new Report();
        correction.setExam(original.getExam());
        correction.setPatient(original.getPatient());
        correction.setFindingsInput(original.getFindingsInput());
        correction.setResultInput(original.getResultInput());
        correction.setNormalExam(original.isNormalExam());
        correction.setGeneratedFindings(original.getGeneratedFindings());
        correction.setGeneratedResult(original.getGeneratedResult());
        correction.setUrgencyLevel(original.getUrgencyLevel());
        correction.setResultCategories(new HashSet<>(original.getResultCategories()));
        correction.setAiAssisted(original.isAiAssisted());
        correction.setStatus(ReportStatus.TASLAK);
        correction.setPreviousRecord(original);
        correction.setCreatedBy(findUser(userEmail));
        correction.setCreatedAt(LocalDateTime.now());

        reportRepository.save(correction);
        return ReportResponse.from(correction);
    }

    private AppUser findUser(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: " + email));

    }

    private void applyGeneration(Report report, GenerationResult generationResult) {
        report.setGeneratedFindings(generationResult.findings());
        report.setGeneratedResult(generationResult.result());
        report.setUrgencyLevel(generationResult.urgencyLevel());
        report.getResultCategories().clear();
        report.getResultCategories().addAll(generationResult.resultCategories());
        report.setAiAssisted(generationResult.aiAssisted());
    }

    private Report findReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapor bulunamadı: " + id));

    }

    private void requireCreator(Report report, String email) {
        if (!report.getCreatedBy().getEmail().equals(email)) {
            throw new ForbiddenException("Bu rapor üzerinde işlem yapma yetkiniz yok.");

        }
    }

    private void requireDraft(Report report) {
        if (report.getStatus() != ReportStatus.TASLAK) {
            throw new ConflictException("Yalnızca taslak raporlar üzerinde bu işlem yapılabilir.");

        }
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
