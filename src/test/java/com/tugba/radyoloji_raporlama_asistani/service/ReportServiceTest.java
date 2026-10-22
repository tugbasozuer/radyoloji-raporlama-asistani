package com.tugba.radyoloji_raporlama_asistani.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.tugba.radyoloji_raporlama_asistani.entity.ResultCategory;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.AnatomicalRegion;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ExamType;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.Gender;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ReportStatus;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.Role;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UrgencyLevel;
import com.tugba.radyoloji_raporlama_asistani.exception.ConflictException;
import com.tugba.radyoloji_raporlama_asistani.exception.ForbiddenException;
import com.tugba.radyoloji_raporlama_asistani.exception.ResourceNotFoundException;
import com.tugba.radyoloji_raporlama_asistani.exception.TooManyRequestsException;
import com.tugba.radyoloji_raporlama_asistani.repository.AppUserRepository;
import com.tugba.radyoloji_raporlama_asistani.repository.ExamRepository;
import com.tugba.radyoloji_raporlama_asistani.repository.PatientRepository;
import com.tugba.radyoloji_raporlama_asistani.repository.ReportRepository;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final String AUTHOR_EMAIL = "ayse@ornek.com";
    private static final String OTHER_EMAIL = "baska@ornek.com";
    private static final String FINDINGS_INPUT = "Olağan Dışı Bulgular: sağ akciğer alt lobda konsolidasyon";
    private static final String RESULT_INPUT = "pnömoni ile uyumlu";

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ReportGenerator reportGenerator;

    @Mock
    private AiReportGenerator aiReportGenerator;

    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private ReportService reportService;

    // ---------- create ----------

    @Test
    void create_validRequest_savesDraftWithTemplateGeneratedTexts() {
        Patient patient = patient();
        AppUser author = user(AUTHOR_EMAIL);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(appUserRepository.findByEmail(AUTHOR_EMAIL)).thenReturn(Optional.of(author));
        when(examRepository.nextProtocolSequence()).thenReturn(7L);
        when(reportGenerator.generate(any(Exam.class), eq(FINDINGS_INPUT), eq(RESULT_INPUT), eq(false)))
                .thenReturn(generation("Sağ akciğer alt lobda konsolidasyon izlenmektedir.", "", UrgencyLevel.RUTIN,
                        false));

        ReportResponse response = reportService.create(createRequest(false), AUTHOR_EMAIL);

        assertThat(response.status()).isEqualTo(ReportStatus.TASLAK);
        assertThat(response.generatedFindings()).isEqualTo("Sağ akciğer alt lobda konsolidasyon izlenmektedir.");
        assertThat(response.aiAssisted()).isFalse();
        assertThat(response.createdByEmail()).isEqualTo(AUTHOR_EMAIL);
        assertThat(response.protocolNumber())
                .isEqualTo(String.format("P-%d-%06d", LocalDateTime.now().getYear(), 7));

        ArgumentCaptor<Exam> examCaptor = ArgumentCaptor.forClass(Exam.class);
        verify(examRepository).save(examCaptor.capture());
        assertThat(examCaptor.getValue().getRequester()).isEqualTo("Acil Servis");
        assertThat(examCaptor.getValue().isFromEmergencyDepartment()).isTrue();

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getStatus()).isEqualTo(ReportStatus.TASLAK);
        assertThat(reportCaptor.getValue().getCreatedBy()).isSameAs(author);
        assertThat(reportCaptor.getValue().getPatient()).isSameAs(patient);
    }

    @Test
    void create_normalExam_passesFlagToGenerator() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient()));
        when(appUserRepository.findByEmail(AUTHOR_EMAIL)).thenReturn(Optional.of(user(AUTHOR_EMAIL)));
        when(examRepository.nextProtocolSequence()).thenReturn(8L);
        when(reportGenerator.generate(any(Exam.class), eq(FINDINGS_INPUT), eq(RESULT_INPUT), eq(true)))
                .thenReturn(generation("Normal bulgular.", "Akut patolojik bulgu saptanmamıştır.", UrgencyLevel.RUTIN,
                        false));

        reportService.create(createRequest(true), AUTHOR_EMAIL);

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().isNormalExam()).isTrue();
        assertThat(reportCaptor.getValue().getGeneratedResult()).isEqualTo("Akut patolojik bulgu saptanmamıştır.");
    }

    @Test
    void create_unknownPatient_throwsNotFoundAndSavesNothing() {
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.create(createRequest(false), AUTHOR_EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(examRepository, never()).save(any());
        verify(reportRepository, never()).save(any());
    }

    @Test
    void create_unknownUser_throwsNotFoundAndSavesNothing() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient()));
        when(appUserRepository.findByEmail(AUTHOR_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.create(createRequest(false), AUTHOR_EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(examRepository, never()).save(any());
        verify(reportRepository, never()).save(any());
    }

    // ---------- update ----------

    @Test
    void update_unchangedInputs_returnsEarlyAndKeepsAiFlag() {
        Report report = draftReport(AUTHOR_EMAIL);
        report.setAiAssisted(true);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        ReportResponse response = reportService.update(100L,
                new ReportUpdateRequest(FINDINGS_INPUT, RESULT_INPUT, false), AUTHOR_EMAIL);

        assertThat(response.aiAssisted()).isTrue();
        verifyNoInteractions(reportGenerator);
    }

    @Test
    void update_changedInputs_regeneratesWithTemplateAndResetsAiFlag() {
        Report report = draftReport(AUTHOR_EMAIL);
        report.setAiAssisted(true);
        report.setGeneratedResult("YZ ile yazılmış sonuç.");
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        String newFindings = "Olağan Dışı Bulgular: sol akciğerde nodül";
        when(reportGenerator.generate(report.getExam(), newFindings, RESULT_INPUT, false))
                .thenReturn(generation("Sol akciğerde nodül izlenmektedir.", "", UrgencyLevel.RUTIN, false));

        ReportResponse response = reportService.update(100L,
                new ReportUpdateRequest(newFindings, RESULT_INPUT, false), AUTHOR_EMAIL);

        assertThat(response.aiAssisted()).isFalse();
        assertThat(response.findingsInput()).isEqualTo(newFindings);
        assertThat(response.generatedFindings()).isEqualTo("Sol akciğerde nodül izlenmektedir.");
        assertThat(response.generatedResult()).isEmpty();
    }

    @Test
    void update_byAnotherUser_throwsForbidden() {
        when(reportRepository.findById(100L)).thenReturn(Optional.of(draftReport(AUTHOR_EMAIL)));

        assertThatThrownBy(() -> reportService.update(100L,
                new ReportUpdateRequest("başka bulgu", RESULT_INPUT, false), OTHER_EMAIL))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(reportGenerator);
    }

    @Test
    void update_approvedReport_throwsConflict() {
        Report report = draftReport(AUTHOR_EMAIL);
        report.setStatus(ReportStatus.ONAYLANDI);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.update(100L,
                new ReportUpdateRequest("başka bulgu", RESULT_INPUT, false), AUTHOR_EMAIL))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_unknownReport_throwsNotFound() {
        when(reportRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.update(100L,
                new ReportUpdateRequest(FINDINGS_INPUT, RESULT_INPUT, false), AUTHOR_EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- generateWithAi ----------

    @Test
    void generateWithAi_draft_checksRateLimitThenAppliesAiResult() {
        Report report = draftReport(AUTHOR_EMAIL);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(aiReportGenerator.generate(FINDINGS_INPUT, RESULT_INPUT)).thenReturn(generation(
                "Sağ akciğer alt lobda konsolidasyon izlenmektedir.",
                "Pnömoni ile uyumludur. Klinik korelasyon önerilir.", UrgencyLevel.ACIL, true));

        ReportResponse response = reportService.generateWithAi(100L, AUTHOR_EMAIL);

        assertThat(response.aiAssisted()).isTrue();
        assertThat(response.urgencyLevel()).isEqualTo(UrgencyLevel.ACIL);
        assertThat(response.generatedResult()).isEqualTo("Pnömoni ile uyumludur. Klinik korelasyon önerilir.");

        InOrder order = inOrder(rateLimitService, aiReportGenerator);
        order.verify(rateLimitService).checkAiGeneration(AUTHOR_EMAIL);
        order.verify(aiReportGenerator).generate(FINDINGS_INPUT, RESULT_INPUT);
    }

    @Test
    void generateWithAi_normalExam_skipsRateLimitAndAi() {
        Report report = draftReport(AUTHOR_EMAIL);
        report.setNormalExam(true);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        ReportResponse response = reportService.generateWithAi(100L, AUTHOR_EMAIL);

        assertThat(response.aiAssisted()).isFalse();
        verifyNoInteractions(rateLimitService, aiReportGenerator);
    }

    @Test
    void generateWithAi_alreadyAiAssisted_skipsRateLimitAndAi() {
        Report report = draftReport(AUTHOR_EMAIL);
        report.setAiAssisted(true);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        reportService.generateWithAi(100L, AUTHOR_EMAIL);

        verifyNoInteractions(rateLimitService, aiReportGenerator);
    }

    @Test
    void generateWithAi_rateLimitExceeded_doesNotCallAi() {
        Report report = draftReport(AUTHOR_EMAIL);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        doThrow(new TooManyRequestsException("çok sık")).when(rateLimitService).checkAiGeneration(AUTHOR_EMAIL);

        assertThatThrownBy(() -> reportService.generateWithAi(100L, AUTHOR_EMAIL))
                .isInstanceOf(TooManyRequestsException.class);

        verifyNoInteractions(aiReportGenerator);
        assertThat(report.isAiAssisted()).isFalse();
    }

    @Test
    void generateWithAi_byAnotherUser_throwsForbiddenWithoutUsingQuota() {
        when(reportRepository.findById(100L)).thenReturn(Optional.of(draftReport(AUTHOR_EMAIL)));

        assertThatThrownBy(() -> reportService.generateWithAi(100L, OTHER_EMAIL))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(rateLimitService, aiReportGenerator);
    }

    // ---------- approve ----------

    @Test
    void approve_validDraft_marksApprovedWithApproverAndTime() {
        Report report = draftReport(AUTHOR_EMAIL);
        report.setGeneratedFindings("Sağ akciğer alt lobda konsolidasyon izlenmektedir.");
        report.setGeneratedResult("Pnömoni ile uyumludur.");
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(appUserRepository.findByEmail(AUTHOR_EMAIL)).thenReturn(Optional.of(user(AUTHOR_EMAIL)));

        ReportResponse response = reportService.approve(100L, AUTHOR_EMAIL);

        assertThat(response.status()).isEqualTo(ReportStatus.ONAYLANDI);
        assertThat(response.approvedByEmail()).isEqualTo(AUTHOR_EMAIL);
        assertThat(response.approvedAt()).isNotNull();
    }

    @Test
    void approve_blankGeneratedResult_throwsConflictAndStaysDraft() {
        Report report = draftReport(AUTHOR_EMAIL);
        report.setGeneratedFindings("Sağ akciğer alt lobda konsolidasyon izlenmektedir.");
        report.setGeneratedResult("   ");
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.approve(100L, AUTHOR_EMAIL))
                .isInstanceOf(ConflictException.class);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.TASLAK);
    }

    @Test
    void approve_missingGeneratedFindings_throwsConflictAndStaysDraft() {
        Report report = draftReport(AUTHOR_EMAIL);
        report.setGeneratedFindings(null);
        report.setGeneratedResult("Pnömoni ile uyumludur.");
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.approve(100L, AUTHOR_EMAIL))
                .isInstanceOf(ConflictException.class);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.TASLAK);
    }

    @Test
    void approve_byAnotherUser_throwsForbidden() {
        Report report = draftReport(AUTHOR_EMAIL);
        report.setGeneratedResult("Pnömoni ile uyumludur.");
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.approve(100L, OTHER_EMAIL))
                .isInstanceOf(ForbiddenException.class);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.TASLAK);
    }

    @Test
    void approve_alreadyApproved_throwsConflict() {
        Report report = draftReport(AUTHOR_EMAIL);
        report.setStatus(ReportStatus.ONAYLANDI);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.approve(100L, AUTHOR_EMAIL))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- delete ----------

    @Test
    void delete_draftWithoutOtherReports_deletesReportAndItsExam() {
        Report report = draftReport(AUTHOR_EMAIL);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(reportRepository.existsByExamId(10L)).thenReturn(false);

        reportService.delete(100L, AUTHOR_EMAIL);

        verify(reportRepository).delete(report);
        verify(examRepository).delete(report.getExam());
    }

    @Test
    void delete_examStillUsedByAnotherReport_keepsExam() {
        Report report = draftReport(AUTHOR_EMAIL);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(reportRepository.existsByExamId(10L)).thenReturn(true);

        reportService.delete(100L, AUTHOR_EMAIL);

        verify(reportRepository).delete(report);
        verify(examRepository, never()).delete(any());
    }

    @Test
    void delete_byAnotherUser_throwsForbiddenAndDeletesNothing() {
        when(reportRepository.findById(100L)).thenReturn(Optional.of(draftReport(AUTHOR_EMAIL)));

        assertThatThrownBy(() -> reportService.delete(100L, OTHER_EMAIL))
                .isInstanceOf(ForbiddenException.class);

        verify(reportRepository, never()).delete(any());
        verify(examRepository, never()).delete(any());
    }

    @Test
    void delete_approvedReport_throwsConflictAndDeletesNothing() {
        Report report = draftReport(AUTHOR_EMAIL);
        report.setStatus(ReportStatus.ONAYLANDI);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> reportService.delete(100L, AUTHOR_EMAIL))
                .isInstanceOf(ConflictException.class);

        verify(reportRepository, never()).delete(any());
    }

    // ---------- openCorrection ----------

    @Test
    void openCorrection_approvedReport_createsDraftCopyLinkedToOriginal() {
        ResultCategory category = new ResultCategory();
        category.setCategoryName("Pnömoni");
        Report original = draftReport(AUTHOR_EMAIL);
        original.setStatus(ReportStatus.ONAYLANDI);
        original.setGeneratedFindings("Sağ akciğer alt lobda konsolidasyon izlenmektedir.");
        original.setGeneratedResult("Pnömoni ile uyumludur.");
        original.setUrgencyLevel(UrgencyLevel.ACIL);
        original.setAiAssisted(true);
        original.getResultCategories().add(category);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(original));
        when(reportRepository.existsByPreviousRecordId(100L)).thenReturn(false);
        when(appUserRepository.findByEmail(OTHER_EMAIL)).thenReturn(Optional.of(user(OTHER_EMAIL)));

        ReportResponse response = reportService.openCorrection(100L, OTHER_EMAIL);

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        Report correction = captor.getValue();
        assertThat(correction.getStatus()).isEqualTo(ReportStatus.TASLAK);
        assertThat(correction.getPreviousRecord()).isSameAs(original);
        assertThat(correction.getExam()).isSameAs(original.getExam());
        assertThat(correction.getCreatedBy().getEmail()).isEqualTo(OTHER_EMAIL);
        assertThat(correction.getGeneratedResult()).isEqualTo("Pnömoni ile uyumludur.");
        assertThat(correction.getUrgencyLevel()).isEqualTo(UrgencyLevel.ACIL);
        assertThat(correction.isAiAssisted()).isTrue();
        assertThat(correction.getResultCategories()).containsExactly(category);
        assertThat(response.previousRecordId()).isEqualTo(100L);
        assertThat(original.getStatus()).isEqualTo(ReportStatus.ONAYLANDI);
    }

    @Test
    void openCorrection_draftReport_throwsConflict() {
        when(reportRepository.findById(100L)).thenReturn(Optional.of(draftReport(AUTHOR_EMAIL)));

        assertThatThrownBy(() -> reportService.openCorrection(100L, AUTHOR_EMAIL))
                .isInstanceOf(ConflictException.class);

        verify(reportRepository, never()).save(any());
    }

    @Test
    void openCorrection_correctionAlreadyOpened_throwsConflict() {
        Report original = draftReport(AUTHOR_EMAIL);
        original.setStatus(ReportStatus.ONAYLANDI);
        when(reportRepository.findById(100L)).thenReturn(Optional.of(original));
        when(reportRepository.existsByPreviousRecordId(100L)).thenReturn(true);

        assertThatThrownBy(() -> reportService.openCorrection(100L, OTHER_EMAIL))
                .isInstanceOf(ConflictException.class);

        verify(reportRepository, never()).save(any());
    }

    @Test
    void openCorrection_unknownReport_throwsNotFound() {
        when(reportRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.openCorrection(100L, AUTHOR_EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- okuma ve listeleme ----------

    @Test
    void getById_existingReport_returnsResponse() {
        when(reportRepository.findById(100L)).thenReturn(Optional.of(draftReport(AUTHOR_EMAIL)));

        ReportResponse response = reportService.getById(100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.protocolNumber()).isEqualTo("P-2026-000001");
    }

    @Test
    void getById_unknownReport_throwsNotFound() {
        when(reportRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getById(100L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listByPatient_existingPatient_returnsReports() {
        when(patientRepository.existsById(1L)).thenReturn(true);
        when(reportRepository.findByPatientIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(draftReport(AUTHOR_EMAIL)));

        List<ReportResponse> reports = reportService.listByPatient(1L);

        assertThat(reports).hasSize(1);
    }

    @Test
    void listByPatient_unknownPatient_throwsNotFound() {
        when(patientRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> reportService.listByPatient(1L)).isInstanceOf(ResourceNotFoundException.class);

        verify(reportRepository, never()).findByPatientIdOrderByCreatedAtDesc(any());
    }

    @Test
    void listMine_withoutFilters_returnsAllOwnReports() {
        stubTwoReportsOfAuthor();

        assertThat(reportService.listMine(AUTHOR_EMAIL, null, null)).hasSize(2);
    }

    @Test
    void listMine_filteredByStatus_returnsOnlyMatchingReports() {
        stubTwoReportsOfAuthor();

        List<ReportResponse> reports = reportService.listMine(AUTHOR_EMAIL, ReportStatus.ONAYLANDI, null);

        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).status()).isEqualTo(ReportStatus.ONAYLANDI);
    }

    @Test
    void listMine_filteredByExamType_returnsOnlyMatchingReports() {
        stubTwoReportsOfAuthor();

        List<ReportResponse> reports = reportService.listMine(AUTHOR_EMAIL, null, ExamType.MR);

        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).examType()).isEqualTo(ExamType.MR);
    }

    @Test
    void listMine_filteredByStatusAndExamType_combinesBothFilters() {
        stubTwoReportsOfAuthor();

        assertThat(reportService.listMine(AUTHOR_EMAIL, ReportStatus.ONAYLANDI, ExamType.BT)).isEmpty();
    }

    // ---------- yardımcı metotlar: test verisi üretir ----------

    private ReportCreateRequest createRequest(boolean normalExam) {
        return new ReportCreateRequest(1L, ExamType.BT, AnatomicalRegion.TORAKS, true, FINDINGS_INPUT, RESULT_INPUT,
                normalExam);
    }

    private Patient patient() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setFullName("Deneme Hasta");
        patient.setBirthDate(LocalDate.of(1980, 5, 17));
        patient.setGender(Gender.KADIN);
        patient.setPatientFileNumber("D-0001");
        patient.setNationalId("12345678901");
        return patient;
    }

    private AppUser user(String email) {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRole(Role.RADYOLOG);
        return user;
    }

    // İki rapor: biri BT + TASLAK, diğeri MR + ONAYLANDI
    private void stubTwoReportsOfAuthor() {
        Report draftBt = draftReport(AUTHOR_EMAIL);
        Report approvedMr = draftReport(AUTHOR_EMAIL);
        approvedMr.setId(101L);
        approvedMr.setStatus(ReportStatus.ONAYLANDI);
        approvedMr.getExam().setExamType(ExamType.MR);
        when(appUserRepository.findByEmail(AUTHOR_EMAIL)).thenReturn(Optional.of(user(AUTHOR_EMAIL)));
        when(reportRepository.findByCreatedByIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(draftBt, approvedMr));
    }

    private Exam exam() {
        Exam exam = new Exam();
        exam.setId(10L);
        exam.setPatient(patient());
        exam.setProtocolNumber("P-2026-000001");
        exam.setExamType(ExamType.BT);
        exam.setAnatomicalRegion(AnatomicalRegion.TORAKS);
        exam.setExamDateTime(LocalDateTime.now());
        exam.setRequester("Acil Servis");
        exam.setFromEmergencyDepartment(true);
        return exam;
    }

    private Report draftReport(String creatorEmail) {
        Report report = new Report();
        report.setId(100L);
        report.setExam(exam());
        report.setPatient(patient());
        report.setCreatedBy(user(creatorEmail));
        report.setStatus(ReportStatus.TASLAK);
        report.setFindingsInput(FINDINGS_INPUT);
        report.setResultInput(RESULT_INPUT);
        report.setNormalExam(false);
        report.setGeneratedFindings("Eski bulgu metni.");
        report.setGeneratedResult("");
        report.setUrgencyLevel(UrgencyLevel.RUTIN);
        report.setAiAssisted(false);
        report.setCreatedAt(LocalDateTime.now());
        return report;
    }

    private GenerationResult generation(String findings, String result, UrgencyLevel urgency, boolean aiAssisted) {
        return new GenerationResult(findings, result, urgency, Set.of(), aiAssisted);
    }
}
