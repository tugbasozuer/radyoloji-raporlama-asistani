package com.tugba.radyoloji_raporlama_asistani.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
import com.tugba.radyoloji_raporlama_asistani.service.ReportService;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerTest {

    private static final String USER = "ayse@ornek.com";

    private static final String CREATE_JSON = """
            {
              "patientId": 1,
              "examType": "BT",
              "anatomicalRegion": "TORAKS",
              "fromEmergencyDepartment": true,
              "findingsInput": "Olağan Dışı Bulgular: sağ akciğer alt lobda konsolidasyon",
              "resultInput": "pnömoni ile uyumlu",
              "normalExam": false
            }
            """;

    private static final String UPDATE_JSON = """
            {
              "findingsInput": "Olağan Dışı Bulgular: sol akciğerde nodül",
              "resultInput": "ileri inceleme önerilir",
              "normalExam": false
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private JwtCookieService jwtCookieService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    // ---------- POST /api/reports ----------

    @Test
    void create_withoutLogin_returns401() throws Exception {
        mockMvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content(CREATE_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_validRequest_returns201AndPassesRequestAndUserToService() throws Exception {
        when(reportService.create(any(ReportCreateRequest.class), any())).thenReturn(report(ReportStatus.TASLAK));

        mockMvc.perform(post("/api/reports").with(user(USER))
                .contentType(MediaType.APPLICATION_JSON).content(CREATE_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("TASLAK"))
                .andExpect(jsonPath("$.protocolNumber").value("P-2026-000001"));

        ArgumentCaptor<ReportCreateRequest> captor = ArgumentCaptor.forClass(ReportCreateRequest.class);
        verify(reportService).create(captor.capture(), org.mockito.ArgumentMatchers.eq(USER));
        assertThat(captor.getValue().patientId()).isEqualTo(1L);
        assertThat(captor.getValue().examType()).isEqualTo(ExamType.BT);
        assertThat(captor.getValue().anatomicalRegion()).isEqualTo(AnatomicalRegion.TORAKS);
        assertThat(captor.getValue().fromEmergencyDepartment()).isTrue();
    }

    @Test
    void create_missingRequiredFields_returns400WithFieldErrors() throws Exception {
        String invalidJson = """
                {
                  "examType": "BT",
                  "anatomicalRegion": "TORAKS",
                  "fromEmergencyDepartment": false,
                  "findingsInput": "  ",
                  "normalExam": false
                }
                """;

        mockMvc.perform(post("/api/reports").with(user(USER))
                .contentType(MediaType.APPLICATION_JSON).content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.patientId").exists())
                .andExpect(jsonPath("$.errors.findingsInput").exists());
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/reports").with(user(USER))
                .contentType(MediaType.APPLICATION_JSON).content("{bozuk"))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET ----------

    @Test
    void listMine_authenticated_usesLoggedInUser() throws Exception {
        when(reportService.listMine(USER)).thenReturn(List.of(report(ReportStatus.TASLAK)));

        mockMvc.perform(get("/api/reports/mine").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    void getById_unknownReport_returns404() throws Exception {
        when(reportService.getById(999L)).thenThrow(new ResourceNotFoundException("Rapor bulunamadı: 999"));

        mockMvc.perform(get("/api/reports/999").with(user(USER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Rapor bulunamadı: 999"));
    }

    // ---------- PUT ----------

    @Test
    void update_validRequest_returns200() throws Exception {
        when(reportService.update(any(), any(ReportUpdateRequest.class), any()))
                .thenReturn(report(ReportStatus.TASLAK));

        mockMvc.perform(put("/api/reports/100").with(user(USER))
                .contentType(MediaType.APPLICATION_JSON).content(UPDATE_JSON))
                .andExpect(status().isOk());

        verify(reportService).update(org.mockito.ArgumentMatchers.eq(100L), any(ReportUpdateRequest.class),
                org.mockito.ArgumentMatchers.eq(USER));
    }

    @Test
    void update_reportOfAnotherUser_returns403() throws Exception {
        when(reportService.update(any(), any(ReportUpdateRequest.class), any()))
                .thenThrow(new ForbiddenException("Bu rapor üzerinde işlem yapma yetkiniz yok."));

        mockMvc.perform(put("/api/reports/100").with(user(USER))
                .contentType(MediaType.APPLICATION_JSON).content(UPDATE_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Bu rapor üzerinde işlem yapma yetkiniz yok."));
    }

    // ---------- POST /{id}/generate-ai ----------

    @Test
    void generateWithAi_success_returns200() throws Exception {
        when(reportService.generateWithAi(100L, USER)).thenReturn(report(ReportStatus.TASLAK));

        mockMvc.perform(post("/api/reports/100/generate-ai").with(user(USER)))
                .andExpect(status().isOk());
    }

    @Test
    void generateWithAi_rateLimitExceeded_returns429() throws Exception {
        when(reportService.generateWithAi(100L, USER))
                .thenThrow(new TooManyRequestsException("60 saniye içinde en fazla 5 YZ isteği gönderebilirsiniz."));

        mockMvc.perform(post("/api/reports/100/generate-ai").with(user(USER)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value("60 saniye içinde en fazla 5 YZ isteği gönderebilirsiniz."));
    }

    // ---------- POST /{id}/approve ----------

    @Test
    void approve_emptyGeneratedTexts_returns409() throws Exception {
        when(reportService.approve(100L, USER))
                .thenThrow(new ConflictException("Bulgular ve Sonuç dolu olmadan rapor onaylanamaz."));

        mockMvc.perform(post("/api/reports/100/approve").with(user(USER)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Bulgular ve Sonuç dolu olmadan rapor onaylanamaz."));
    }

    // ---------- POST /{id}/corrections ----------

    @Test
    void openCorrection_success_returns201() throws Exception {
        when(reportService.openCorrection(100L, USER)).thenReturn(report(ReportStatus.TASLAK));

        mockMvc.perform(post("/api/reports/100/corrections").with(user(USER)))
                .andExpect(status().isCreated());
    }

    @Test
    void openCorrection_alreadyOpened_returns409() throws Exception {
        when(reportService.openCorrection(100L, USER))
                .thenThrow(new ConflictException("Bu rapor için zaten bir düzeltme açılmış."));

        mockMvc.perform(post("/api/reports/100/corrections").with(user(USER)))
                .andExpect(status().isConflict());
    }

    // ---------- DELETE ----------

    @Test
    void delete_ownDraft_returns204() throws Exception {
        mockMvc.perform(delete("/api/reports/100").with(user(USER)))
                .andExpect(status().isNoContent());

        verify(reportService).delete(100L, USER);
    }

    @Test
    void delete_withoutLogin_returns401() throws Exception {
        mockMvc.perform(delete("/api/reports/100"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- beklenmeyen hata ----------

    @Test
    void unexpectedException_returns500WithoutLeakingDetails() throws Exception {
        when(reportService.getById(100L)).thenThrow(new IllegalStateException("veritabanı şifresi: gizli"));

        mockMvc.perform(get("/api/reports/100").with(user(USER)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Beklenmeyen bir hata oluştu."));
    }

    // ---------- yardımcı ----------

    private ReportResponse report(ReportStatus status) {
        PatientResponse patient = new PatientResponse(1L, "Deneme Hasta", 46, "Kadın", "D-0001", "********901");
        return new ReportResponse(100L, patient, "P-2026-000001", ExamType.BT, AnatomicalRegion.TORAKS,
                LocalDateTime.of(2026, 10, 20, 10, 30), "Acil Servis", true,
                "Olağan Dışı Bulgular: sağ akciğer alt lobda konsolidasyon", "pnömoni ile uyumlu", false,
                "Sağ akciğer alt lobda konsolidasyon izlenmektedir.", "", UrgencyLevel.RUTIN, status, false, null,
                USER, USER, null, null, LocalDateTime.of(2026, 10, 20, 10, 30), null, List.of());
    }
}
