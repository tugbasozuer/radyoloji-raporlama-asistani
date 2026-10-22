package com.tugba.radyoloji_raporlama_asistani.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tugba.radyoloji_raporlama_asistani.ai.FindingsParser;
import com.tugba.radyoloji_raporlama_asistani.config.SecurityConfig;
import com.tugba.radyoloji_raporlama_asistani.dto.PatientResponse;
import com.tugba.radyoloji_raporlama_asistani.dto.ReportCreateRequest;
import com.tugba.radyoloji_raporlama_asistani.dto.ReportResponse;
import com.tugba.radyoloji_raporlama_asistani.dto.ReportUpdateRequest;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.AnatomicalRegion;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ExamType;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ReportStatus;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UrgencyLevel;
import com.tugba.radyoloji_raporlama_asistani.exception.ConflictException;
import com.tugba.radyoloji_raporlama_asistani.exception.ForbiddenException;
import com.tugba.radyoloji_raporlama_asistani.exception.ResourceNotFoundException;
import com.tugba.radyoloji_raporlama_asistani.exception.TooManyRequestsException;
import com.tugba.radyoloji_raporlama_asistani.security.AppUserDetailsService;
import com.tugba.radyoloji_raporlama_asistani.security.JwtCookieService;
import com.tugba.radyoloji_raporlama_asistani.security.JwtService;
import com.tugba.radyoloji_raporlama_asistani.security.TokenBlacklistService;
import com.tugba.radyoloji_raporlama_asistani.service.PatientService;
import com.tugba.radyoloji_raporlama_asistani.service.ReportService;

@WebMvcTest(ReportPageController.class)
@Import(SecurityConfig.class)
class ReportPageControllerTest {

    private static final String USER = "ayse@ornek.com";
    private static final String OTHER = "baska@ornek.com";
    private static final String FINDINGS = "Olağan Dışı Bulgular: sağ akciğer alt lobda konsolidasyon";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private PatientService patientService;

    @MockitoBean
    private JwtCookieService jwtCookieService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    // ---------- GET /raporlar (Raporlarım) ----------

    @Test
    void myReports_withoutFilters_listsOwnReports() throws Exception {
        List<ReportResponse> reports = List.of(report(ReportStatus.TASLAK, USER));
        when(reportService.listMine(USER, null, null)).thenReturn(reports);

        mockMvc.perform(get("/raporlar").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(view().name("raporlar"))
                .andExpect(model().attribute("reports", reports))
                .andExpect(model().attributeExists("statuses", "examTypes"));
    }

    @Test
    void myReports_withFilters_passesThemToService() throws Exception {
        when(reportService.listMine(USER, ReportStatus.TASLAK, ExamType.BT)).thenReturn(List.of());

        mockMvc.perform(get("/raporlar").param("durum", "TASLAK").param("tetkik", "BT").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedStatus", ReportStatus.TASLAK))
                .andExpect(model().attribute("selectedExamType", ExamType.BT));

        verify(reportService).listMine(USER, ReportStatus.TASLAK, ExamType.BT);
    }

    @Test
    void myReports_invalidFilterValue_showsBadRequestPage() throws Exception {
        mockMvc.perform(get("/raporlar").param("durum", "YOKBOYLE").with(user(USER)))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("hata"))
                .andExpect(model().attribute("title", "Geçersiz istek"));
    }

    // ---------- GET /raporlar/yeni ----------

    @Test
    void newReportPage_showsFormForSelectedPatient() throws Exception {
        when(patientService.getById(1L)).thenReturn(patient());

        mockMvc.perform(get("/raporlar/yeni").param("hastaId", "1").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(view().name("rapor-form"))
                .andExpect(model().attribute("editMode", false))
                .andExpect(model().attributeExists("form", "patient", "examTypes", "regions"));
    }

    @Test
    void newReportPage_withoutPatientId_showsBadRequestPage() throws Exception {
        mockMvc.perform(get("/raporlar/yeni").with(user(USER)))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("hata"));
    }

    // ---------- POST /raporlar (kaydet) ----------

    @Test
    void create_validForm_redirectsToDetailWithSuccessMessage() throws Exception {
        when(reportService.create(any(ReportCreateRequest.class), eq(USER)))
                .thenReturn(report(ReportStatus.TASLAK, USER));

        mockMvc.perform(post("/raporlar").with(user(USER))
                .param("patientId", "1").param("examType", "BT").param("anatomicalRegion", "TORAKS")
                .param("fromEmergencyDepartment", "true").param("findingsInput", FINDINGS)
                .param("resultInput", "pnömoni ile uyumlu"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raporlar/100"))
                .andExpect(flash().attribute("successMessage", "Rapor kaydedildi."));

        verify(reportService).create(any(ReportCreateRequest.class), eq(USER));
    }

    @Test
    void create_onlyEmptyHeadings_redisplaysFormWithFindingsError() throws Exception {
        when(patientService.getById(1L)).thenReturn(patient());

        mockMvc.perform(post("/raporlar").with(user(USER))
                .param("patientId", "1").param("examType", "BT").param("anatomicalRegion", "TORAKS")
                .param("findingsInput", FindingsParser.canonicalText(null)))
                .andExpect(status().isOk())
                .andExpect(view().name("rapor-form"))
                .andExpect(model().attributeHasFieldErrorCode("form", "findingsInput", "empty"));

        verify(reportService, never()).create(any(), any());
    }

    @Test
    void create_withoutPatientId_redirectsToPatientSelection() throws Exception {
        mockMvc.perform(post("/raporlar").with(user(USER))
                .param("examType", "BT").param("anatomicalRegion", "TORAKS").param("findingsInput", FINDINGS))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hastalar"));

        verify(reportService, never()).create(any(), any());
    }

    // ---------- GET /raporlar/{id}/duzenle ----------

    @Test
    void editPage_ownDraft_showsEditForm() throws Exception {
        when(reportService.getById(100L)).thenReturn(report(ReportStatus.TASLAK, USER));

        mockMvc.perform(get("/raporlar/100/duzenle").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(view().name("rapor-form"))
                .andExpect(model().attribute("editMode", true))
                .andExpect(model().attributeExists("form", "report", "patient"));
    }

    @Test
    void editPage_draftOfAnotherUser_redirectsToDetailWithError() throws Exception {
        when(reportService.getById(100L)).thenReturn(report(ReportStatus.TASLAK, OTHER));

        mockMvc.perform(get("/raporlar/100/duzenle").with(user(USER)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raporlar/100"))
                .andExpect(flash().attribute("errorMessage", "Bu rapor düzenlenemez."));
    }

    @Test
    void editPage_approvedReport_redirectsToDetailWithError() throws Exception {
        when(reportService.getById(100L)).thenReturn(report(ReportStatus.ONAYLANDI, USER));

        mockMvc.perform(get("/raporlar/100/duzenle").with(user(USER)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raporlar/100"))
                .andExpect(flash().attribute("errorMessage", "Bu rapor düzenlenemez."));
    }

    // ---------- POST /raporlar/{id} (güncelle) ----------

    @Test
    void update_validForm_redirectsToDetailWithSuccessMessage() throws Exception {
        when(reportService.update(eq(100L), any(ReportUpdateRequest.class), eq(USER)))
                .thenReturn(report(ReportStatus.TASLAK, USER));

        mockMvc.perform(post("/raporlar/100").with(user(USER))
                .param("findingsInput", FINDINGS).param("resultInput", "pnömoni ile uyumlu"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raporlar/100"))
                .andExpect(flash().attribute("successMessage", "Rapor güncellendi."));
    }

    @Test
    void update_onlyEmptyHeadings_redisplaysFormWithFindingsError() throws Exception {
        when(reportService.getById(100L)).thenReturn(report(ReportStatus.TASLAK, USER));

        mockMvc.perform(post("/raporlar/100").with(user(USER))
                .param("findingsInput", FindingsParser.canonicalText(null)))
                .andExpect(status().isOk())
                .andExpect(view().name("rapor-form"))
                .andExpect(model().attributeHasFieldErrorCode("form", "findingsInput", "empty"));

        verify(reportService, never()).update(any(), any(), any());
    }

    // ---------- GET /raporlar/{id} (rapor sayfası) ----------

    @Test
    void detail_ownDraftWithoutResult_allowsEditAndGenerateButBlocksApprove() throws Exception {
        when(reportService.getById(100L)).thenReturn(
                report(100L, ReportStatus.TASLAK, USER, false, false, "Bulgu metni.", ""));

        mockMvc.perform(get("/raporlar/100").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(view().name("rapor-detay"))
                .andExpect(model().attribute("canEdit", true))
                .andExpect(model().attribute("approved", false))
                .andExpect(model().attribute("canGenerate", true))
                .andExpect(model().attribute("approveBlocked", true));
    }

    @Test
    void detail_ownDraftWithBothTexts_allowsApprove() throws Exception {
        when(reportService.getById(100L)).thenReturn(
                report(100L, ReportStatus.TASLAK, USER, false, true, "Bulgu metni.", "Sonuç metni."));

        mockMvc.perform(get("/raporlar/100").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("approveBlocked", false));
    }

    @Test
    void detail_draftOfAnotherUser_isReadOnly() throws Exception {
        when(reportService.getById(100L)).thenReturn(
                report(100L, ReportStatus.TASLAK, OTHER, false, false, "Bulgu metni.", ""));

        mockMvc.perform(get("/raporlar/100").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canEdit", false))
                .andExpect(model().attribute("canGenerate", false));
    }

    @Test
    void detail_approvedReport_isReadOnly() throws Exception {
        when(reportService.getById(100L)).thenReturn(
                report(100L, ReportStatus.ONAYLANDI, USER, false, true, "Bulgu metni.", "Sonuç metni."));

        mockMvc.perform(get("/raporlar/100").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("approved", true))
                .andExpect(model().attribute("canEdit", false))
                .andExpect(model().attribute("canGenerate", false));
    }

    @Test
    void detail_normalExam_hidesGenerateButton() throws Exception {
        when(reportService.getById(100L)).thenReturn(
                report(100L, ReportStatus.TASLAK, USER, true, false, "Bulgu metni.", "Sonuç metni."));

        mockMvc.perform(get("/raporlar/100").with(user(USER)))
                .andExpect(model().attribute("canEdit", true))
                .andExpect(model().attribute("canGenerate", false));
    }

    @Test
    void detail_alreadyAiAssisted_hidesGenerateButton() throws Exception {
        when(reportService.getById(100L)).thenReturn(
                report(100L, ReportStatus.TASLAK, USER, false, true, "Bulgu metni.", "Sonuç metni."));

        mockMvc.perform(get("/raporlar/100").with(user(USER)))
                .andExpect(model().attribute("canGenerate", false));
    }

    @Test
    void detail_unknownReport_showsNotFoundPage() throws Exception {
        when(reportService.getById(999L)).thenThrow(new ResourceNotFoundException("Rapor bulunamadı: 999"));

        mockMvc.perform(get("/raporlar/999").with(user(USER)))
                .andExpect(status().isNotFound())
                .andExpect(view().name("hata"))
                .andExpect(model().attribute("title", "Bulunamadı"))
                .andExpect(model().attribute("message", "Rapor bulunamadı: 999"));
    }

    // ---------- POST /raporlar/{id}/yz-uret ----------

    @Test
    void generateWithAi_aiResult_flashesSuccess() throws Exception {
        when(reportService.generateWithAi(100L, USER)).thenReturn(
                report(100L, ReportStatus.TASLAK, USER, false, true, "Bulgu metni.", "Sonuç metni."));

        mockMvc.perform(post("/raporlar/100/yz-uret").with(user(USER)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raporlar/100"))
                .andExpect(flash().attribute("successMessage", "Rapor yapay zeka ile üretildi."));
    }

    @Test
    void generateWithAi_aiUnavailable_flashesFallbackWarning() throws Exception {
        when(reportService.generateWithAi(100L, USER)).thenReturn(
                report(100L, ReportStatus.TASLAK, USER, false, false, "Bulgu metni.", "Sonuç metni."));

        mockMvc.perform(post("/raporlar/100/yz-uret").with(user(USER)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raporlar/100"))
                .andExpect(flash().attribute("warningMessage",
                        "Yapay zeka şu anda kullanılamadı; rapor kalıp cümlelerle üretildi. "
                                + "Birkaç dakika sonra tekrar deneyebilirsiniz."));
    }

    @Test
    void generateWithAi_normalExam_flashesNothing() throws Exception {
        when(reportService.generateWithAi(100L, USER)).thenReturn(
                report(100L, ReportStatus.TASLAK, USER, true, false, "Bulgu metni.", "Sonuç metni."));

        mockMvc.perform(post("/raporlar/100/yz-uret").with(user(USER)))
                .andExpect(redirectedUrl("/raporlar/100"))
                .andExpect(flash().attributeCount(0));
    }

    @Test
    void generateWithAi_rateLimitExceeded_showsTooManyRequestsPage() throws Exception {
        when(reportService.generateWithAi(100L, USER))
                .thenThrow(new TooManyRequestsException("60 saniye içinde en fazla 5 YZ isteği gönderebilirsiniz."));

        mockMvc.perform(post("/raporlar/100/yz-uret").with(user(USER)))
                .andExpect(status().isTooManyRequests())
                .andExpect(view().name("hata"))
                .andExpect(model().attribute("title", "Çok fazla istek"));
    }

    // ---------- POST /raporlar/{id}/onayla ----------

    @Test
    void approve_success_flashesApprovedMessage() throws Exception {
        mockMvc.perform(post("/raporlar/100/onayla").with(user(USER)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raporlar/100"))
                .andExpect(flash().attribute("successMessage", "Rapor onaylandı."));

        verify(reportService).approve(100L, USER);
    }

    @Test
    void approve_emptyGeneratedTexts_showsConflictPage() throws Exception {
        when(reportService.approve(100L, USER))
                .thenThrow(new ConflictException("Bulgular ve Sonuç dolu olmadan rapor onaylanamaz."));

        mockMvc.perform(post("/raporlar/100/onayla").with(user(USER)))
                .andExpect(status().isConflict())
                .andExpect(view().name("hata"))
                .andExpect(model().attribute("title", "İşlem yapılamadı"));
    }

    // ---------- POST /raporlar/{id}/sil ----------

    @Test
    void delete_withoutBackParameter_redirectsToReportListWithMessage() throws Exception {
        mockMvc.perform(post("/raporlar/100/sil").with(user(USER)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raporlar"))
                .andExpect(flash().attribute("successMessage", "Rapor silindi."));

        verify(reportService).delete(100L, USER);
    }

    @Test
    void delete_withValidBackParameter_redirectsBackToThatListPage() throws Exception {
        mockMvc.perform(post("/raporlar/100/sil").param("geri", "/hastalar?hastaId=1&q=ay").with(user(USER)))
                .andExpect(redirectedUrl("/hastalar?hastaId=1&q=ay"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "https://example.com", "//evil.com", "javascript:alert(1)", "/baska-sayfa",
            "/raporlar/../giris", "/raporlar?x=<script>" })
    void delete_withUnsafeBackParameter_fallsBackToReportList(String unsafe) throws Exception {
        mockMvc.perform(post("/raporlar/100/sil").param("geri", unsafe).with(user(USER)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raporlar"));
    }

    @Test
    void delete_ofAnotherUsersReport_showsForbiddenPage() throws Exception {
        doThrow(new ForbiddenException("Bu rapor üzerinde işlem yapma yetkiniz yok."))
                .when(reportService).delete(100L, USER);

        mockMvc.perform(post("/raporlar/100/sil").with(user(USER)))
                .andExpect(status().isForbidden())
                .andExpect(view().name("hata"))
                .andExpect(model().attribute("title", "Yetkiniz yok"));
    }

    @Test
    void delete_withoutLogin_redirectsToLoginAndDeletesNothing() throws Exception {
        mockMvc.perform(post("/raporlar/100/sil"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/giris"));

        verifyNoInteractions(reportService);
    }

    // ---------- POST /raporlar/{id}/duzeltme ----------

    @Test
    void openCorrection_redirectsToNewDraftWithMessage() throws Exception {
        when(reportService.openCorrection(100L, USER)).thenReturn(
                report(101L, ReportStatus.TASLAK, USER, false, false, "Bulgu metni.", "Sonuç metni."));

        mockMvc.perform(post("/raporlar/100/duzeltme").with(user(USER)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raporlar/101"))
                .andExpect(flash().attribute("successMessage",
                        "Düzeltme için yeni bir taslak açıldı. Onaylı rapor olduğu gibi korunuyor."));
    }

    @Test
    void openCorrection_alreadyOpened_showsConflictPage() throws Exception {
        when(reportService.openCorrection(100L, USER))
                .thenThrow(new ConflictException("Bu rapor için zaten bir düzeltme açılmış."));

        mockMvc.perform(post("/raporlar/100/duzeltme").with(user(USER)))
                .andExpect(status().isConflict())
                .andExpect(view().name("hata"))
                .andExpect(model().attribute("message", "Bu rapor için zaten bir düzeltme açılmış."));
    }

    // ---------- yardımcı ----------

    private PatientResponse patient() {
        return new PatientResponse(1L, "Deneme Hasta", 46, "Kadın", "D-0001", "********901");
    }

    private ReportResponse report(ReportStatus status, String authorEmail) {
        return report(100L, status, authorEmail, false, false,
                "Sağ akciğer alt lobda konsolidasyon izlenmektedir.", "");
    }

    private ReportResponse report(Long id, ReportStatus status, String authorEmail, boolean normalExam,
            boolean aiAssisted, String generatedFindings, String generatedResult) {
        return new ReportResponse(id, patient(), "P-2026-000001", ExamType.BT, AnatomicalRegion.TORAKS,
                LocalDateTime.of(2026, 10, 20, 10, 30), "Acil Servis", true, FINDINGS, "pnömoni ile uyumlu",
                normalExam, generatedFindings, generatedResult, UrgencyLevel.RUTIN, status, aiAssisted, null,
                authorEmail, authorEmail, null, null, LocalDateTime.of(2026, 10, 20, 10, 30), null, List.of());
    }
}
