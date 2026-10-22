package com.tugba.radyoloji_raporlama_asistani.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import com.tugba.radyoloji_raporlama_asistani.exception.ConflictException;
import com.tugba.radyoloji_raporlama_asistani.exception.ForbiddenException;
import com.tugba.radyoloji_raporlama_asistani.exception.ResourceNotFoundException;
import com.tugba.radyoloji_raporlama_asistani.exception.TooManyRequestsException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice(basePackages = "com.tugba.radyoloji_raporlama_asistani.web")
public class WebExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException e) {
        return errorPage(HttpStatus.NOT_FOUND, "Bulunamadı", e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ModelAndView handleForbidden(ForbiddenException e) {
        return errorPage(HttpStatus.FORBIDDEN, "Yetkiniz yok", e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ModelAndView handleConflict(ConflictException e) {
        return errorPage(HttpStatus.CONFLICT, "İşlem yapılamadı", e.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ModelAndView handleTooManyRequests(TooManyRequestsException e) {
        return errorPage(HttpStatus.TOO_MANY_REQUESTS, "Çok fazla istek", e.getMessage());
    }

    @ExceptionHandler({ MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class })
    public ModelAndView handleBadRequest(Exception e) {
        return errorPage(HttpStatus.BAD_REQUEST, "Geçersiz istek", "İstekte eksik ya da hatalı bilgi var.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception e) {
        log.error("Beklenmeyen hata", e);
        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen hata",
                "Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.");
    }

    private ModelAndView errorPage(HttpStatus status, String title, String message) {
        ModelAndView modelAndView = new ModelAndView("hata");
        modelAndView.setStatus(status);
        modelAndView.addObject("title", title);
        modelAndView.addObject("message", message);
        return modelAndView;
    }
}