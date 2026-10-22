package com.tugba.radyoloji_raporlama_asistani.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tugba.radyoloji_raporlama_asistani.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

}
