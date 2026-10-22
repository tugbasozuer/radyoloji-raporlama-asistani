package com.tugba.radyoloji_raporlama_asistani.ai;

import java.util.Map;

public interface AiAssistant {

    AiAssistance assist(String abnormalFindings,
            String resultText,
            Map<String, String> categories);
}