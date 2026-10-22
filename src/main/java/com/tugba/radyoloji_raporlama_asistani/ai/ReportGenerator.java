package com.tugba.radyoloji_raporlama_asistani.ai;

import com.tugba.radyoloji_raporlama_asistani.entity.Exam;

public interface ReportGenerator {

    GenerationResult generate(Exam exam,
            String findingsInput,
            String resultInput,
            boolean normalExam);
}
