package com.tugba.radyoloji_raporlama_asistani.entity;

import java.time.LocalDate;

import com.tugba.radyoloji_raporlama_asistani.entity.enums.Gender;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "patient")
@Entity
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING) 
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false, unique = true)
    private String patientFileNumber;

    @Column(nullable = false, unique = true)
    private String nationalId;

}
