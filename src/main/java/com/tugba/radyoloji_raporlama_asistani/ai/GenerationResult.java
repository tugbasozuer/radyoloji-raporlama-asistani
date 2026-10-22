package com.tugba.radyoloji_raporlama_asistani.ai;

import java.util.Set;

import com.tugba.radyoloji_raporlama_asistani.entity.ResultCategory;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UrgencyLevel;

public record GenerationResult(
        String findings,
        String result,
        UrgencyLevel urgencyLevel,
        Set<ResultCategory> resultCategories,
        boolean aiAssisted) {

}
