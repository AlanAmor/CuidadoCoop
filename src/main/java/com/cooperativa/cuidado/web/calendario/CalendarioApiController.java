package com.cooperativa.cuidado.web.calendario;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class CalendarioApiController {

    private final CalendarioService calendarioService;

    public CalendarioApiController(CalendarioService calendarioService) {
        this.calendarioService = calendarioService;
    }

    @GetMapping("/api/calendario")
    public CalendarioRespuesta consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return calendarioService.consultar(desde, hasta);
    }
}
