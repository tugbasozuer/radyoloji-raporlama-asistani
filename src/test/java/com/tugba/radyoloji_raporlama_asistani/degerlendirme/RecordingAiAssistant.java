package com.tugba.radyoloji_raporlama_asistani.degerlendirme;

import java.util.Map;

import com.tugba.radyoloji_raporlama_asistani.ai.AiAssistance;
import com.tugba.radyoloji_raporlama_asistani.ai.AiAssistant;

/**
 * Gerçek AiAssistant'ı sarar ve her çağrıda üç şeyi kaydeder: modelin ham yanıtı (Java doğrulamasından önce),
 * yalnızca model çağrısının süresi ve varsa hata. AiReportGenerator hatayı yakalayıp yedek davranışa geçtiği için
 * bu bilgiler dışarıdan başka türlü öğrenilemez.
 */
final class RecordingAiAssistant implements AiAssistant {

    private final AiAssistant delegate;
    private AiAssistance lastRaw;
    private long lastNanos;
    private String lastError = "";

    RecordingAiAssistant(AiAssistant delegate) {
        this.delegate = delegate;
    }

    void reset() {
        lastRaw = null;
        lastNanos = 0;
        lastError = "";
    }

    @Override
    public AiAssistance assist(String abnormalFindings, String resultText, Map<String, String> categories) {
        reset();
        long start = System.nanoTime();
        try {
            lastRaw = delegate.assist(abnormalFindings, resultText, categories);
            return lastRaw;
        } catch (RuntimeException e) {
            lastError = e.toString();
            throw e;
        } finally {
            lastNanos = System.nanoTime() - start;
        }
    }

    AiAssistance lastRaw() {
        return lastRaw;
    }

    double lastMillis() {
        return lastNanos / 1_000_000.0;
    }

    String lastError() {
        return lastError;
    }
}
