package com.tugba.radyoloji_raporlama_asistani.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.Department;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UserTitle;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "identity")
@Entity
public class Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false, unique = true)
    private AppUser appUser;

    @Column(nullable = false)
    private String fullName;

    @Column(length = 30)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private UserTitle title;

    @Column(length = 50)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private Department department;

    @Column(nullable = false)
    private LocalDateTime createdAt;

}
