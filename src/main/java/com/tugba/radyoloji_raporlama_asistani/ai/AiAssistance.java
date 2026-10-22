package com.tugba.radyoloji_raporlama_asistani.ai;

import java.util.List;

public record AiAssistance(
        String findings,
        String result,
        List<String> categories) {
}