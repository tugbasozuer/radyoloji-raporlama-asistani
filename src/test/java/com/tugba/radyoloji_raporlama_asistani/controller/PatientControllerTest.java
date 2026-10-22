package com.tugba.radyoloji_raporlama_asistani.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tugba.radyoloji_raporlama_asistani.config.SecurityConfig;
import com.tugba.radyoloji_raporlama_asistani.dto.PatientResponse;
import com.tugba.radyoloji_raporlama_asistani.exception.ResourceNotFoundException;
import com.tugba.radyoloji_raporlama_asistani.security.AppUserDetailsService;
import com.tugba.radyoloji_raporlama_asistani.security.JwtCookieService;
import com.tugba.radyoloji_raporlama_asistani.security.JwtService;
import com.tugba.radyoloji_raporlama_asistani.security.TokenBlacklistService;
import com.tugba.radyoloji_raporlama_asistani.service.PatientService;
import com.tugba.radyoloji_raporlama_asistani.service.ReportService;

@WebMvcTest(PatientController.class)
@Import(SecurityConfig.class)
class PatientControllerTest {

    private static final String USER = "ayse@ornek.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @MockitoBean
    private ReportService reportService;

    // SecurityConfig ve JwtAuthenticationFilter'ın ihtiyaç duyduğu bağımlılıklar (sahte)
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private JwtCookieService jwtCookieService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void search_withoutLogin_returns401() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void search_authenticated_returnsPatientList() throws Exception {
        when(patientService.search(null)).thenReturn(List.of(patient()));

        mockMvc.perform(get("/api/patients").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Deneme Hasta"))
                .andExpect(jsonPath("$[0].patientFileNumber").value("D-0001"))
                .andExpect(jsonPath("$[0].maskedNationalId").value("********901"));
    }

    @Test
    void search_withNameParameter_passesItToService() throws Exception {
        when(patientService.search("ay")).thenReturn(List.of());

        mockMvc.perform(get("/api/patients").param("name", "ay").with(user(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(patientService).search("ay");
    }

    @Test
    void getById_unknownPatient_returns404ProblemDetail() throws Exception {
        when(patientService.getById(99L)).thenThrow(new ResourceNotFoundException("Hasta bulunamadı: 99"));

        mockMvc.perform(get("/api/patients/99").with(user(USER)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Hasta bulunamadı: 99"));
    }

    @Test
    void getById_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/patients/abc").with(user(USER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReports_unknownPatient_returns404() throws Exception {
        when(reportService.listByPatient(99L)).thenThrow(new ResourceNotFoundException("Hasta bulunamadı: 99"));

        mockMvc.perform(get("/api/patients/99/reports").with(user(USER)))
                .andExpect(status().isNotFound());
    }

    private PatientResponse patient() {
        return new PatientResponse(1L, "Deneme Hasta", 46, "Kadın", "D-0001", "********901");
    }
}
