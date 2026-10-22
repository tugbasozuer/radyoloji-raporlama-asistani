package com.tugba.radyoloji_raporlama_asistani.web;

import java.security.Principal;
import java.util.regex.Pattern;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tugba.radyoloji_raporlama_asistani.ai.FindingsParser;
import com.tugba.radyoloji_raporlama_asistani.dto.ReportCreateRequest;
import com.tugba.radyoloji_raporlama_asistani.dto.ReportResponse;
import com.tugba.radyoloji_raporlama_asistani.dto.ReportUpdateRequest;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.AnatomicalRegion;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ExamType;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ReportStatus;
import com.tugba.radyoloji_raporlama_asistani.service.PatientService;
import com.tugba.radyoloji_raporlama_asistani.service.ReportService;
import com.tugba.radyoloji_raporlama_asistani.web.form.ReportEditForm;
import com.tugba.radyoloji_raporlama_asistani.web.form.ReportForm;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ReportPageController {

    private final ReportService reportService;
    private final PatientService patientService;
    private static final Pattern LIST_PAGE = Pattern.compile("^/(raporlar|hastalar)(\\?[A-Za-z0-9=&%._~+-]*)?$");

    @GetMapping("/raporlar")
    public String myReports(
            @RequestParam(name = "durum", required = false) ReportStatus status,
            @RequestParam(name = "tetkik", required = false) ExamType examType,
            Principal principal, Model model) {
        model.addAttribute("reports", reportService.listMine(principal.getName(), status, examType));
        model.addAttribute("statuses", ReportStatus.values());
        model.addAttribute("examTypes", ExamType.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedExamType", examType);
        return "raporlar";
    }

    @GetMapping("/raporlar/yeni")
    public String newReportPage(@RequestParam(name = "hastaId") Long patientId, Model model) {
        ReportForm form = new ReportForm();
        form.setPatientId(patientId);
        form.setFindingsInput(FindingsParser.canonicalText(null));
        model.addAttribute("form", form);
        addNewFormAttributes(model, patientId);
        return "rapor-form";
    }

    @PostMapping("/raporlar")
    public String create(@Valid @ModelAttribute("form") ReportForm form, BindingResult result,
            Principal principal, Model model, RedirectAttributes redirectAttributes) {
        if (form.getPatientId() == null) {
            return "redirect:/hastalar";
        }
        requireFindings(form, result);
        if (result.hasErrors()) {
            addNewFormAttributes(model, form.getPatientId());
            return "rapor-form";
        }
        ReportResponse created = reportService.create(
                new ReportCreateRequest(form.getPatientId(), form.getExamType(), form.getAnatomicalRegion(),
                        form.isFromEmergencyDepartment(), form.getFindingsInput(), form.getResultInput(),
                        form.isNormalExam()),
                principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Rapor kaydedildi.");
        return "redirect:/raporlar/" + created.id();
    }

    @GetMapping("/raporlar/{id}/duzenle")
    public String editReportPage(@PathVariable Long id, Principal principal, Model model,
            RedirectAttributes redirectAttributes) {
        ReportResponse report = reportService.getById(id);
        if (!isEditable(report, principal)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bu rapor düzenlenemez.");
            return "redirect:/raporlar/" + id;
        }
        ReportEditForm form = new ReportEditForm();
        form.setFindingsInput(FindingsParser.canonicalText(report.findingsInput()));
        form.setResultInput(report.resultInput());
        form.setNormalExam(report.normalExam());
        model.addAttribute("form", form);
        addEditFormAttributes(model, report);
        return "rapor-form";
    }

    @PostMapping("/raporlar/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ReportEditForm form,
            BindingResult result, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        requireFindings(form, result);
        if (result.hasErrors()) {
            addEditFormAttributes(model, reportService.getById(id));
            return "rapor-form";
        }
        reportService.update(id,
                new ReportUpdateRequest(form.getFindingsInput(), form.getResultInput(), form.isNormalExam()),
                principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Rapor güncellendi.");
        return "redirect:/raporlar/" + id;
    }

    @GetMapping("/raporlar/{id}")
    public String reportDetail(@PathVariable Long id, Principal principal, Model model) {
        ReportResponse report = reportService.getById(id);
        boolean canEdit = isEditable(report, principal);
        model.addAttribute("report", report);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("approved", report.status() == ReportStatus.ONAYLANDI);
        model.addAttribute("canGenerate", canEdit && !report.normalExam() && !report.aiAssisted());
        model.addAttribute("approveBlocked",
                isBlank(report.generatedFindings()) || isBlank(report.generatedResult()));
        return "rapor-detay";
    }

    @PostMapping("/raporlar/{id}/yz-uret")
    public String generateWithAi(@PathVariable Long id, Principal principal,
            RedirectAttributes redirectAttributes) {
        ReportResponse report = reportService.generateWithAi(id, principal.getName());
        if (report.aiAssisted()) {
            redirectAttributes.addFlashAttribute("successMessage", "Rapor yapay zeka ile üretildi.");
        } else if (!report.normalExam()) {
            redirectAttributes.addFlashAttribute("warningMessage",
                    "Yapay zeka şu anda kullanılamadı; rapor kalıp cümlelerle üretildi. "
                            + "Birkaç dakika sonra tekrar deneyebilirsiniz.");
        }
        return "redirect:/raporlar/" + id;
    }

    @PostMapping("/raporlar/{id}/onayla")
    public String approve(@PathVariable Long id, Principal principal,
            RedirectAttributes redirectAttributes) {
        reportService.approve(id, principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Rapor onaylandı.");
        return "redirect:/raporlar/" + id;
    }

    @PostMapping("/raporlar/{id}/sil")
    public String delete(@PathVariable Long id,
            @RequestParam(name = "geri", required = false) String back,
            Principal principal, RedirectAttributes redirectAttributes) {
        reportService.delete(id, principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Rapor silindi.");
        return "redirect:" + listPageOrDefault(back);
    }

    @PostMapping("/raporlar/{id}/duzeltme")
    public String openCorrection(@PathVariable Long id, Principal principal,
            RedirectAttributes redirectAttributes) {
        ReportResponse correction = reportService.openCorrection(id, principal.getName());
        redirectAttributes.addFlashAttribute("successMessage",
                "Düzeltme için yeni bir taslak açıldı. Onaylı rapor olduğu gibi korunuyor.");
        return "redirect:/raporlar/" + correction.id();
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private static String listPageOrDefault(String back) {
        return back != null && LIST_PAGE.matcher(back).matches() ? back : "/raporlar";
    }

    private void addNewFormAttributes(Model model, Long patientId) {
        model.addAttribute("editMode", false);
        model.addAttribute("patient", patientService.getById(patientId));
        model.addAttribute("examTypes", ExamType.values());
        model.addAttribute("regions", AnatomicalRegion.values());
    }

    private void addEditFormAttributes(Model model, ReportResponse report) {
        model.addAttribute("editMode", true);
        model.addAttribute("report", report);
        model.addAttribute("patient", report.patient());
    }

    private boolean isEditable(ReportResponse report, Principal principal) {
        return report.status() == ReportStatus.TASLAK
                && report.createdByEmail().equals(principal.getName());
    }

    private void requireFindings(ReportEditForm form, BindingResult result) {
        boolean empty = FindingsParser.parse(form.getFindingsInput()).values().stream()
                .allMatch(String::isBlank);
        if (empty) {
            result.rejectValue("findingsInput", "empty", "Bulgular alanını doldurun.");
        }
    }
}