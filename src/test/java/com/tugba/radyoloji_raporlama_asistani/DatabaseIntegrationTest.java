package com.tugba.radyoloji_raporlama_asistani;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

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
import com.tugba.radyoloji_raporlama_asistani.repository.AppUserRepository;
import com.tugba.radyoloji_raporlama_asistani.repository.ExamRepository;
import com.tugba.radyoloji_raporlama_asistani.repository.PatientRepository;
import com.tugba.radyoloji_raporlama_asistani.repository.ReportRepository;
import com.tugba.radyoloji_raporlama_asistani.repository.ResultCategoryRepository;

import jakarta.persistence.EntityManager;

/**
 * Gerçek bir PostgreSQL konteynerinde (Testcontainers) çalışır: Flyway migration'ları, seed verileri,
 * repository sorguları ve veritabanı kısıtları. Her test bir transaction içinde çalışır ve sonunda geri alınır.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class DatabaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ResultCategoryRepository resultCategoryRepository;

    // ---------- Flyway ve seed verileri ----------

    @Test
    void flyway_appliesAllSixMigrationsSuccessfully() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6");
    }

    @Test
    void seed_resultCategoriesAreLoaded() {
        assertThat(resultCategoryRepository.count()).isEqualTo(10);
        assertThat(resultCategoryRepository.findByCategoryName("Normal Bulgu")).isPresent();
        assertThat(resultCategoryRepository.findByCategoryName("Akut Kanama")).isPresent();
    }

    @Test
    void seed_demoPatientsAreLoaded() {
        assertThat(patientRepository.findAll()).extracting(Patient::getPatientFileNumber)
                .containsExactlyInAnyOrder("D-0001", "D-0002", "D-0003", "D-0004");
    }

    @Test
    void protocolSequence_returnsIncreasingValues() {
        long first = examRepository.nextProtocolSequence();
        long second = examRepository.nextProtocolSequence();

        assertThat(second).isEqualTo(first + 1);
    }

    // ---------- hasta sorguları ----------

    @Test
    void findByPatientFileNumber_findsExistingAndIgnoresUnknown() {
        assertThat(patientRepository.findByPatientFileNumber("D-0001")).isPresent()
                .get().extracting(Patient::getFullName).isEqualTo("Ayşe Örnek");
        assertThat(patientRepository.findByPatientFileNumber("D-9999")).isEmpty();
    }

    @Test
    void findByFullName_isCaseInsensitiveAndMatchesPartOfName() {
        assertThat(patientRepository.findByFullNameContainingIgnoreCase("MEHMET"))
                .extracting(Patient::getPatientFileNumber).containsExactly("D-0002");
        assertThat(patientRepository.findByFullNameContainingIgnoreCase("zey"))
                .extracting(Patient::getPatientFileNumber).containsExactly("D-0003");
        assertThat(patientRepository.findByFullNameContainingIgnoreCase("yok")).isEmpty();
    }

    @Test
    void searchByNameOrFileNumber_matchesEither() {
        assertThat(patientRepository
                .findByFullNameContainingIgnoreCaseOrPatientFileNumberContainingIgnoreCase("deneme", "D-0004"))
                .extracting(Patient::getPatientFileNumber).containsExactlyInAnyOrder("D-0002", "D-0004");
        assertThat(patientRepository
                .findByFullNameContainingIgnoreCaseOrPatientFileNumberContainingIgnoreCase("D-000", "D-000"))
                .hasSize(4);
    }

    // ---------- veritabanı kısıtları ----------

    @Test
    void duplicatePatientFileNumber_isRejectedByUniqueConstraint() {
        Patient duplicate = new Patient();
        duplicate.setFullName("Kopya Hasta");
        duplicate.setBirthDate(LocalDate.of(1990, 1, 1));
        duplicate.setGender(Gender.KADIN);
        duplicate.setPatientFileNumber("D-0001");
        duplicate.setNationalId("20000000001");

        assertThatThrownBy(() -> patientRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateUserEmail_isRejectedByUniqueConstraint() {
        newUser("ayse@ornek.com");

        assertThatThrownBy(() -> newUser("ayse@ornek.com")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateProtocolNumber_isRejectedByUniqueConstraint() {
        Patient patient = patientRepository.findByPatientFileNumber("D-0001").orElseThrow();
        newExam(patient, "P-TEST-000001");

        assertThatThrownBy(() -> newExam(patient, "P-TEST-000001"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- rapor kaydı ve sorguları ----------

    @Test
    void report_isStoredWithAllFieldsAndResultCategories() {
        Patient patient = patientRepository.findByPatientFileNumber("D-0001").orElseThrow();
        AppUser author = newUser("ayse@ornek.com");
        Exam exam = newExam(patient, "P-TEST-000002");
        ResultCategory category = resultCategoryRepository.findByCategoryName("Akut Kanama").orElseThrow();

        Report report = report(exam, patient, author, LocalDateTime.of(2026, 10, 20, 10, 0));
        report.setGeneratedFindings("Sağ frontalde hemoraji izlenmektedir.");
        report.setGeneratedResult("Hemoraji şüphesi. Acil klinik değerlendirme önerilir.");
        report.setUrgencyLevel(UrgencyLevel.KRITIK);
        report.setAiAssisted(true);
        report.getResultCategories().add(category);
        reportRepository.saveAndFlush(report);

        entityManager.clear();
        Report loaded = reportRepository.findById(report.getId()).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(ReportStatus.TASLAK);
        assertThat(loaded.getFindingsInput()).isEqualTo("Olağan Dışı Bulgular: sağ frontalde hemoraji");
        assertThat(loaded.getGeneratedResult()).isEqualTo("Hemoraji şüphesi. Acil klinik değerlendirme önerilir.");
        assertThat(loaded.getUrgencyLevel()).isEqualTo(UrgencyLevel.KRITIK);
        assertThat(loaded.isAiAssisted()).isTrue();
        assertThat(loaded.getCreatedBy().getEmail()).isEqualTo("ayse@ornek.com");
        assertThat(loaded.getResultCategories()).extracting(ResultCategory::getCategoryName)
                .containsExactly("Akut Kanama");
    }

    @Test
    void reportQueries_orderNewestFirstAndAnswerExistenceChecks() {
        Patient patient = patientRepository.findByPatientFileNumber("D-0001").orElseThrow();
        AppUser author = newUser("ayse@ornek.com");
        Exam exam = newExam(patient, "P-TEST-000003");
        Exam otherExam = newExam(patient, "P-TEST-000004");

        Report older = reportRepository.saveAndFlush(
                report(exam, patient, author, LocalDateTime.of(2026, 10, 20, 9, 0)));
        Report newer = reportRepository.saveAndFlush(
                report(otherExam, patient, author, LocalDateTime.of(2026, 10, 20, 11, 0)));

        assertThat(reportRepository.findByCreatedByIdOrderByCreatedAtDesc(author.getId()))
                .extracting(Report::getId).containsExactly(newer.getId(), older.getId());
        assertThat(reportRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId()))
                .extracting(Report::getId).containsExactly(newer.getId(), older.getId());
        assertThat(reportRepository.existsByExamId(exam.getId())).isTrue();
        assertThat(reportRepository.existsByExamId(-1L)).isFalse();

        Report correction = report(exam, patient, author, LocalDateTime.of(2026, 10, 20, 12, 0));
        correction.setPreviousRecord(older);
        reportRepository.saveAndFlush(correction);

        assertThat(reportRepository.existsByPreviousRecordId(older.getId())).isTrue();
        assertThat(reportRepository.existsByPreviousRecordId(newer.getId())).isFalse();
    }

    // ---------- yardımcı: test verisi üretir ----------

    private AppUser newUser(String email) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRole(Role.RADYOLOG);
        return appUserRepository.saveAndFlush(user);
    }

    private Exam newExam(Patient patient, String protocolNumber) {
        Exam exam = new Exam();
        exam.setPatient(patient);
        exam.setProtocolNumber(protocolNumber);
        exam.setExamType(ExamType.BT);
        exam.setAnatomicalRegion(AnatomicalRegion.KRANIAL);
        exam.setExamDateTime(LocalDateTime.of(2026, 10, 20, 8, 0));
        exam.setRequester("Acil Servis");
        exam.setFromEmergencyDepartment(true);
        return examRepository.saveAndFlush(exam);
    }

    private Report report(Exam exam, Patient patient, AppUser author, LocalDateTime createdAt) {
        Report report = new Report();
        report.setExam(exam);
        report.setPatient(patient);
        report.setCreatedBy(author);
        report.setStatus(ReportStatus.TASLAK);
        report.setFindingsInput("Olağan Dışı Bulgular: sağ frontalde hemoraji");
        report.setCreatedAt(createdAt);
        return report;
    }
}
