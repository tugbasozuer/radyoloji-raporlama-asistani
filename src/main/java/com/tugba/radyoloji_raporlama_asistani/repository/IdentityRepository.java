package com.tugba.radyoloji_raporlama_asistani.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tugba.radyoloji_raporlama_asistani.entity.Identity;

public interface IdentityRepository extends JpaRepository<Identity, Long> {

    Optional<Identity> findByAppUserId(Long appUserId);

}
