package com.tugba.radyoloji_raporlama_asistani.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.tugba.radyoloji_raporlama_asistani.entity.enums.ReportStatus;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UrgencyLevel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "report")
@Entity
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    // Doktorun girdisi: başlıkları sabit tek metin (Olağan Dışı Bulgular, İzlenmemiştir, ...)
    @Column(columnDefinition = "TEXT")
    private String findingsInput;

    // Doktorun girdisi: Sonuç / Öneriler serbest metni
    @Column(columnDefinition = "TEXT")
    private String resultInput;

    @Column(nullable = false)
    private boolean normalExam;

    @Column(columnDefinition = "TEXT")
    private String generatedFindings;

    @Column(columnDefinition = "TEXT")
    private String generatedResult;

    @Enumerated(EnumType.STRING)
    private UrgencyLevel urgencyLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(nullable = false)
    private boolean aiAssisted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_record_id")
    private Report previousRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private AppUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private AppUser approvedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToMany
    @JoinTable(name = "report_result_category", joinColumns = @JoinColumn(name = "report_id"), inverseJoinColumns = @JoinColumn(name = "result_category_id"))
    private Set<ResultCategory> resultCategories = new HashSet<>();
}
