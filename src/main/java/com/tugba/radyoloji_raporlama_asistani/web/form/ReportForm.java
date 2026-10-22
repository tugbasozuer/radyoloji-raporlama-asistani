package com.tugba.radyoloji_raporlama_asistani.web.form;

import com.tugba.radyoloji_raporlama_asistani.entity.enums.AnatomicalRegion;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ExamType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportForm extends ReportEditForm {

    private Long patientId;

    @NotNull(message = "Tetkik türünü seçin.")
    private ExamType examType = ExamType.BT;

    @NotNull(message = "Anatomik bölgeyi seçin.")
    private AnatomicalRegion anatomicalRegion = AnatomicalRegion.KRANIAL;

    private boolean fromEmergencyDepartment;
}