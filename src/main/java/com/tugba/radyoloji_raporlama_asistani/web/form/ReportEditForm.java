package com.tugba.radyoloji_raporlama_asistani.web.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportEditForm {

    private String findingsInput;

    private String resultInput;

    private boolean normalExam;
}