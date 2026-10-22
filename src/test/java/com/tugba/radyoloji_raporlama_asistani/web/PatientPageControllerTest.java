package com.tugba.radyoloji_raporlama_asistani.web;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tugba.radyoloji_raporlama_asistani.config.SecurityConfig;
import com.tugba.radyoloji_raporlama_asistani.dto.PatientResponse;
import com.tugba.radyoloji_raporlama_asistani.dto.ReportResponse;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.AnatomicalRegion;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ExamType;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ReportStatus;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UrgencyLevel;
import com.tugba.radyoloji_raporlama_asistani.security.AppUserDetailsService;
import com.tugba.radyoloji_raporlama_asistani.security.JwtCookieService;
import com.tugba.radyoloji_raporlama_asistani.security.JwtService;
import com.tugba.radyoloji_raporlama_asistani.security.TokenBlacklistService;
import com.tugba.radyoloji_raporlama_asistani.service.PatientService;
import com.tugba.radyoloji_raporlama_asistani.service.ReportService;

@WebMvcTest(PatientPageController.class)
@Import(SecurityConfig.class)
class PatientPageControllerTest {

    private static final String USER = "ayse@ornek.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtCookieService jwtCookieService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @Test
    void patients_withoutParameters_selectsFirstPatientAndLoadsItsReports() throws Exception {
        PatientResponse first = patient(1L, "Birinci Hasta", "D-0001");
        PatientResponse second = patient(2L, "İkinci Hasta", "D-0002");
        List<ReportResponse> reports = List.of(report(first));
        when(patientService.search(null)).thenReturn(List.of(first, second));
        when(reportService.listByPatient(1L)).thenReturn(reports);

        mockMvc.perform(get("/hastalar").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(view().name("hastalar"))
                .andExpect(model().attribute("patients", List.of(first, second)))
                .andExpect(model().attribute("selectedPatient", first))
                .andExpect(model().attribute("patientReports", reports))
                .andExpect(model().attribute("q", ""));
    }

    @Test
    void patients_withPatientId_selectsThatPatient() throws Exception {
        PatientResponse first = patient(1L, "Birinci Hasta", "D-0001");
        PatientResponse second = patient(2L, "İkinci Hasta", "D-0002");
        when(patientService.search(null)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/hastalar").param("hastaId", "2").with(user(USER)))
                .andExpect(model().attribute("selectedPatient", second));

        verify(reportService).listByPatient(2L);
    }

    @Test
    void patients_unknownPatientId_fallsBackToFirstPatient() throws Exception {
        PatientResponse first = patient(1L, "Birinci Hasta", "D-0001");
        when(patientService.search(null)).thenReturn(List.of(first));

        mockMvc.perform(get("/hastalar").param("hastaId", "99").with(user(USER)))
                .andExpect(model().attribute("selectedPatient", first));

        verify(reportService).listByPatient(1L);
    }

    @Test
    void patients_searchTerm_isPassedToServiceAndKeptInForm() throws Exception {
        PatientResponse match = patient(1L, "Ayşe Deneme", "D-0001");
        when(patientService.search("ay")).thenReturn(List.of(match));

        mockMvc.perform(get("/hastalar").param("q", "ay").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("q", "ay"))
                .andExpect(model().attribute("patients", List.of(match)));

        verify(patientService).search("ay");
    }

    @Test
    void patients_noSearchResult_showsEmptyListWithoutLookingUpReports() throws Exception {
        when(patientService.search("yok")).thenReturn(List.of());

        mockMvc.perform(get("/hastalar").param("q", "yok").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(view().name("hastalar"))
                .andExpect(model().attributeDoesNotExist("selectedPatient"))
                .andExpect(model().attribute("patientReports", List.of()));

        verify(reportService, never()).listByPatient(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void patients_withoutLogin_redirectsToLoginPage() throws Exception {
        mockMvc.perform(get("/hastalar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/giris"));
    }

    private PatientResponse patient(Long id, String name, String fileNumber) {
        return new PatientResponse(id, name, 46, "Kadın", fileNumber, "********901");
    }

    private ReportResponse report(PatientResponse patient) {
        return new ReportResponse(100L, patient, "P-2026-000001", ExamType.BT, AnatomicalRegion.TORAKS,
                LocalDateTime.of(2026, 10, 20, 10, 30), "Acil Servis", true, "bulgu girdisi", "sonuç girdisi", false,
                "Bulgu metni.", "Sonuç metni.", UrgencyLevel.RUTIN, ReportStatus.TASLAK, false, null,
                USER, USER, null, null, LocalDateTime.of(2026, 10, 20, 10, 30), null, List.of());
    }
}
