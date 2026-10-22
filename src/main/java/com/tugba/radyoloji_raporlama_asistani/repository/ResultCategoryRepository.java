package com.tugba.radyoloji_raporlama_asistani.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tugba.radyoloji_raporlama_asistani.entity.ResultCategory;

public interface ResultCategoryRepository extends JpaRepository<ResultCategory, Long> {
    Optional<ResultCategory> findByCategoryName(String categoryName);
}
