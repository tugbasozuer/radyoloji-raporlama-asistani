package com.tugba.radyoloji_raporlama_asistani.entity;

import java.time.LocalDateTime;

import com.tugba.radyoloji_raporlama_asistani.entity.enums.AnatomicalRegion;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ExamType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "exam")
@Entity
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false, unique = true)
    private String protocolNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamType examType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnatomicalRegion anatomicalRegion;

    @Column(nullable = false)
    private LocalDateTime examDateTime;

    @Column(nullable = false)
    private String requester;

    @Column(nullable = false)
    private boolean fromEmergencyDepartment;

}
